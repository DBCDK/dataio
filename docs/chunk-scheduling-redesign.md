# Chunk Scheduling — Design

Replaces the dependency-tracking mechanism and chunk-level sink dispatch with:

- Ordered delivery using `(priority DESC, jobId ASC, chunkId ASC)` scheduling
- Per-item JMS dispatch with `JMSXGroupID` for per-record broker serialisation
- Durable delivery watermark in PostgreSQL, read via REST before every delivery
- No per-sink inflight map; no Hazelcast

---

## Why Replace Dependency Tracking

The current system builds a dependency graph for each new chunk by scanning all
in-flight chunks for overlapping sequence-analysis keys (`matchKeys`). Any chunk whose
keys overlap with an earlier in-flight chunk is placed in `BLOCKED` status until that
earlier chunk is fully delivered. This prevents an older version of a record from
overwriting a newer version that was delivered first.

Problems with this approach:

- **Quadratic graph construction** — each new chunk scans all in-flight chunks per sink.
  Under high load (thousands of in-flight chunks) this is the dominant CPU consumer.
- **Hazelcast dependency** — the dependency graph lives in a distributed Hazelcast map
  with complex entry processors, GIN-indexed JSONB columns, and aggregator logic that
  must be rebuilt from scratch on every restart.
- **Priority boosting cascade** — a high-priority chunk propagates its priority backward
  through the dependency chain; deep chains trigger recursive map mutations across
  cluster nodes.
- **Thundering-herd unblocking** — delivering one chunk may unblock hundreds of chunks
  simultaneously, each triggering its own delivery attempt in separate EJB transactions.

The new design eliminates the dependency graph entirely. Ordering and supersession
detection replace it.

---

## Threading Reality at Sinks

`MessageConsumerApp` spawns `CONSUMER_THREADS` threads (configurable via env var). Most
sinks run with `CONSUMER_THREADS > 1`. Each thread owns its own `JMSConsumer` on the
sink's queue and processes one item at a time. With N threads and M pod replicas, up to
M × N items are in-flight simultaneously.

**FIFO queue order ≠ delivery order.** With concurrent threads, thread A may pick up
an item from job=50 while thread B picks up an item from job=100 for the same record.
Either may reach the target first. This is the race the watermark check resolves.

---

## Scheduler Changes

### State Machine

Remove `BLOCKED`. The new state machine:

```
READY_FOR_PROCESSING
        │
        ▼
SCHEDULED_FOR_PROCESSING   ← queue full (bulk mode)
        │
        ▼
QUEUED_FOR_PROCESSING (max=1000)  ── JMS → job-processor
        │
        ▼
READY_FOR_DELIVERY
        │
        ▼
SCHEDULED_FOR_DELIVERY     ← queue full; termination chunks held here until gate fires
        │
        ▼
QUEUED_FOR_DELIVERY (max=1000)   ── JMS → sink
        │
        ▼
     (removed from scheduler)
```

`ChunkSchedulingStatus.BLOCKED` (value 3) is deleted. Existing DB rows in `BLOCKED`
are migrated to `SCHEDULED_FOR_DELIVERY`.

### Delivery Ordering

The bulk scheduler fills `QUEUED_FOR_DELIVERY` slots with:

```sql
SELECT jobid, chunkid
  FROM dependencytracking
 WHERE sinkid = ?
   AND status = SCHEDULED_FOR_DELIVERY
   AND gate_open = TRUE
 ORDER BY priority DESC, jobid ASC, chunkid ASC
 LIMIT ?
```

An index on `(sinkid, status, gate_open, priority DESC, jobid, chunkid)` makes the sorting
index-backed: the equality prefix pins one contiguous range, the ordered suffix is the `ORDER BY`
itself, and the scan yields rows in order without a sort step. Both details matter.
`priority DESC` has to be in the index: with equality on `sinkid` and `status`, an all-ASC
index yields `priority ASC, jobid ASC, chunkid ASC` forward and all three DESC backward,
and neither matches the `ORDER BY`, so the planner adds a sort and the `LIMIT` stops being
cheap. `gate_open` is a third equality column rather than a partial `WHERE gate_open`
predicate, which keeps the ordered suffix intact, makes the filter an index condition
instead of a heap recheck, and leaves the index usable by a query looking for closed gates.
Direct-mode dispatch (`READY_FOR_DELIVERY → QUEUED_FOR_DELIVERY`) applies the same `gate_open`
filter, see [Barrier Width](#barrier-width--per-sink-type-job-isolation), and holds to the same
order. It has no batch to sort: since the dependency graph was removed there is no site where
several chunks become ready at once, the cascade in `chunkDeliveringDone` that used to hand each
newly unblocked chunk to its own transaction having gone with `waitingOn`. **The order it has to
respect is against the chunks already parked**, not against a batch of its own.

Free capacity says the sink has room, not that this chunk is the one entitled to it. So before
sending, the direct path reads the head of the parked queue with the same query at `LIMIT 1` and
stands down when that head outranks the chunk it holds, parking it in `SCHEDULED_FOR_DELIVERY` for
the sweep. The ordering index answers the read from its first entry, and it is the last of the
three guards, so an idle sink pays one index probe and keeps the latency direct mode exists for.

Without it the order holds only over chunks that happen to be parked. The sweep fills every free
slot each tick, so between two ticks the only slots available are those freed by completions, and a
job partitioning right now takes them on arrival. Where a partitioner emits chunks at least as fast
as chunks complete, the next tick finds no capacity and the sweep dispatches nothing, for as long
as the burst lasts. That is not a fairness question, see [Mapping](#mapping): a hierarchy is one
broker group delivered in send order, so a volume that overtakes its head reaches the sink
referencing a record that is not there yet.

**Why the query is SQL.** The reason is not the index. `gate_open` is a column and no Hazelcast
predicate could see it, so while the map existed a `PagingPredicate` with a comparator would have
given the ordering and still not the filter. The table is now the only store, so the question no
longer arises and a candidate the query returns is a chunk genuinely awaiting delivery with an open
gate.

Historical, and the reason the delivery half was converted first: until Phase 9 the table was a
write-behind projection of the map, so `status` lagged by up to `write-delay-seconds`. The bulk sweep
compensated by over-fetching past the free slots and re-checking each candidate against the map, both
of which end with the projection. They are removed in Phase 9's third PR.

### Processing Ordering

The processing half is ordered the same way, by the same shape of query:

```sql
SELECT jobid, chunkid, priority
  FROM dependencytracking
 WHERE sinkid = ?
   AND status = SCHEDULED_FOR_PROCESSING
 ORDER BY priority DESC, jobid ASC, chunkid ASC
 LIMIT ?
```

Backed by `dependencytracking_processing_order_index`, `(sinkid, status, priority desc, jobid,
chunkid)`. **The delivery index cannot serve this**, because it carries `gate_open` as its third key
column: with no equality predicate on it the ordered tail no longer follows the equality columns and
the planner adds a sort. As with the delivery index, the planner only prefers it once a sink has
enough queued chunks that the sort dominates, so verify with `EXPLAIN` on a realistic backlog rather
than on a fresh table.

Measured, the ordering is index-backed whatever the vacuum state, and that is what these two indexes
are for: with 300 000 rows queued for one sink the planner takes an ordered index scan either way and
the `LIMIT` stops it early, so a sweep costs the free queue slots rather than the backlog.

What vacuum state decides is whether the scan is *index-only*. The same `LIMIT 1000` costs 12
buffers fully all-visible, 674 after one percent of rows have been updated at random, and 1011 with a
cold visibility map. The decay is steep because the table holds about 106 rows per heap page, so
updating a fraction `f` of rows at random clears the all-visible bit on `1 - (1-f)^106` of the pages,
65 percent at `f = 0.01`. **No attainable `autovacuum_vacuum_scale_factor` keeps these scans
index-only**, since its trigger is a dead-tuple count and by the time it fires most pages have lost
the bit. The per-table setting is justified by dead tuples and bloat, not by the visibility map, and
the steady-state budget is one heap buffer per candidate returned.

**The gate predicate is deliberately absent**, and this query must not share a statement with
`findDeliveryCandidates`. A gate holds back delivery and nothing else. Under full barrier width a
queued job's data chunks sit at `gate_open = FALSE` while still needing to be processed normally, so
a gate filter here would stop the processing of exactly the chunks the barrier assumes get
processed.

The query returns `priority`, which is what `submitToProcessing` needs, so there is no per-chunk
lookup. There is no over-fetch and no re-check against a map either, because the table is the only
source of `status`.

**This is what fixes a priority inversion at the first hop.** The `PagingPredicate` it replaces
truncated to an arbitrary page, so a `HIGH` priority chunk could sit behind lower-priority ones for
as long as `QUEUED_FOR_PROCESSING` stood at its cap. That defeated the priority override for live
head and section records where it was cheapest to honour, and left the broker to mitigate it: once a
message is enqueued `submitToProcessing` has passed the chunk's priority as the JMS priority, but
nothing ordered chunks waiting for a queue slot. The inversion predates the ordered delivery query
and was not made worse by it, only more conspicuous.

Direct dispatch to processing carries the same guard as the delivery half, for the same reason and
at the same cost, one `LIMIT 1` read of the query above. It matters at this hop too because the
priority override only reaches delivery through processing: a head chunk that cannot get a
processor slot is not a delivery candidate either, so leaving the first hop unguarded would let a
partitioning burst hold back the very chunks the override raises to `HIGH`.

### Aborting no longer cascades

Aborting a job used to abort every job that depended on it. `findDependingJobs` asked
`dependencytracking` for the distinct jobs whose `waitingon` named a chunk of the aborted job, and
`abortDependingJobs` recursed into each, with a loop-detection set to stop it coming back round.

The reason was sound while the graph existed: a chunk `BLOCKED` on a chunk of an aborted job would
never be unblocked, because the delivery that would have cleared it never happens, so the dependent
job would stall for good. Aborting it too was the lesser evil.

Nothing holds a later job back that way any more. The only cross-job hold left is the per-job gate,
and the abort path already lifts the aborted job's barrier and re-triggers the jobs queued behind it,
which **releases** them rather than aborting them. So the cascade is removed rather than replaced,
and `PgJobStore.abortJob` returns one job instead of a stream.

This is a deliberate behaviour change and an improvement: aborting one job stops taking unrelated
later jobs with it.

### Barrier Chunks — Per-Job Gate

Termination chunks (chunkId = numberOfDataChunks) must not be dispatched until all data
chunks of the same job have had their delivery acknowledged. This replaces the `waitingOn`
dependency set with a simple counter.

New columns on `dependencytracking`:

```sql
is_termination  BOOLEAN  NOT NULL DEFAULT FALSE
gate_open       BOOLEAN  NOT NULL DEFAULT TRUE
```

Termination chunks are inserted with `gate_open = FALSE`.

Termination chunk, barrier and gate are three distinct things, and the last two face opposite
ways. See `job-store-service/dependency-tracking.md`, "Terminology", for the distinction and for
why the flag is `gate_open` rather than `barrier_open`.

New per-job counters, on the `job` table:

```sql
data_chunks_delivered  INT  NOT NULL DEFAULT 0
data_chunks_expected   INT  NOT NULL DEFAULT 0
```

`data_chunks_expected` is the job's data-chunk count, meaning `numberOfChunks` as read in
`markJobAsPartitioned` *before* `createAndScheduleTerminationChunk` runs.
`createJobTerminationChunkEntity` increments `numberOfChunks` itself, so a read taken
after it returns is one too high, `data_chunks_delivered` can never reach it, and the gate
stays closed forever. For a job that has a termination chunk the value therefore equals
that chunk's `chunkId`, which makes it cheap to assert. The column needs a default
because the migration runs against a non-empty `job` table, and because a job that never
gets a termination chunk never gets the write either: the value is written where the
termination row is inserted, which is a no-op for a job that leaves `markJobAsPartitioned`
through one of its early returns, or whose sink type takes no termination chunk. Those jobs
keep 0, which is harmless, since the column is only ever read for a job that has one. Their
total chunk count remains `numberofchunks`.

That value is *read* in `markJobAsPartitioned` but must be *written* one level down, inside
`createJobTerminationChunkEntity`, passed in as a parameter rather than re-read there. See **Partitioning and
delivery overlap** below for why: the two methods run in separate transactions, and a write
left in the outer one is invisible to the gate evaluation in the inner one.

A job can have zero data chunks. `addAndScheduleEmptyJob` calls `markJobAsPartitioned`
with `numberOfChunks = 0`, and partitioning an empty data file reaches the same state.
Such a job still gets a termination chunk, at `chunkId = 0`. Its gate must be evaluated
when that chunk is inserted, subject to the same cross-job barrier as any other, because
no data chunk will ever be delivered to trigger the increment. Opening the gate only from
`chunkDeliveringDone` strands these jobs permanently. This is not a special case for empty
jobs: any job whose data chunks all finish delivering before partitioning ends has the same
missing trigger, so insert-time evaluation is the general rule. See **Partitioning and
delivery overlap** below.

Note that `data_chunks_expected = 0` therefore carries two opposite meanings, and the gate
must not try to tell them apart by the counter. It is the value the migration's default
leaves on every job that predates the gate, including any job partitioned between the
migration and the gate logic going live, which must be ignored entirely. And it is the
legitimate value for a job with genuinely zero data chunks, whose gate must be evaluated
at once. The flag that separates them is `is_termination` on the job's `dependencytracking`
row, so the gate keys on that and treats the counter only as a total to compare against.
The overlap is benign in one direction only: pre-gate jobs already carry `gate_open = TRUE`
from the default, so mistakenly opening their gate is a no-op. Mistakenly ignoring an empty
job strands it.

The gate logic is owned by **job-store-service**. A job's own gate has two evaluation
sites, not one: `chunkDeliveringDone(jobId, chunkId)` and the insert of the termination
chunk itself. A third site, the cross-job re-trigger described at the end of this section,
evaluates *other* jobs' gates. The scheduler-service only *reads* `gate_open` in its
dispatch query.

```
A. On data-chunk delivery - chunkDeliveringDone(jobId, chunkId):

   if this call is the one that removed the chunk's dependencytracking row
   and the chunk is not the job's termination chunk:

       INCREMENT data_chunks_delivered for this job
           -- unconditional: not guarded by the existence of a termination
           -- chunk, and not by gate_open. See Partitioning and delivery
           -- overlap below.

       if the job has a termination chunk with gate_open = FALSE
          and data_chunks_delivered >= data_chunks_expected
          and no earlier job with the same submitter on this sink still has
              an undelivered termination chunk:

           SET gate_open = TRUE for this job's termination chunk
           (scheduler picks it up on its next poll cycle)

   else, the chunk removed was the job's own termination chunk:

       SET job.termination_barrier_lifted = TRUE
       run the cross-job re-trigger below

B. On termination-chunk insert - createJobTerminationChunkEntity(jobId, chunkId = N, ...),
   in the one transaction that inserts the row, under the job row lock it already holds
   and under the barrier-scope advisory lock:

       SET data_chunks_expected = N
           -- N passed in from markJobAsPartitioned, never re-read here

       SET job.termination_barrier_lifted = FALSE
           -- this job now imposes a barrier. The column is NULL until here, so this is
           -- the one place a barrier comes into existence, and it happens in the same
           -- transaction as the is_termination row below.

       INSERT the dependencytracking row with is_termination = TRUE and
       gate_open = (data_chunks_delivered >= N
                    and no earlier job with the same submitter on this sink
                        has an unlifted termination barrier)
```

Site B is what covers a job whose data chunks all finished before partitioning ended, site A
what covers the ordinary case where they finish after. Both apply the cross-job barrier, and
both are no-ops for a job that has no termination chunk at all.

Site A has to know whether the chunk it just removed was the termination chunk, so that the
job's barrier does not count itself. That falls out of the removal token, because
`DependencyTrackingService.remove(TrackingKey)` returns the removed `DependencyTracking`
rather than void: the winning caller gets the proof it won *and* the `is_termination`
flag of the entry it removed, in one call and with no extra read. The flag is a field on that
object as well as a column on the row, which is sound for a value decided when the row is created
and never changed afterwards. See [Who writes the gate columns](#barrier-chunks--per-job-gate).

**Who writes the gate columns.** `dependencytracking` is job-store's outright. One writer,
`DependencyTrackingRepository`, whose every statement runs in its caller's transaction on its
caller's connection, so no column is off limits to any statement and a committed write is visible to
every reader at once.

One convention governs the gate columns, and it is the whole of it: **`gate_open` is
`NOT NULL DEFAULT TRUE`, and only a write that means to close a gate touches the column.** That is
what makes site B and site C correct with a single statement each, a data chunk on a non-full-width
sink getting no gate write at all, and it is why the two inserts that create a row take the verdict
as a value rather than writing it afterwards. A closed gate has nowhere to be recorded until the row
exists, so a gate closed in a second statement is a gate closed too late.

*Historical, up to Phase 9.* The table used to be a write-behind projection of the Hazelcast map, so
ownership was split: the MapStore owned row lifecycle on its own schedule, and job-store owned the
two gate column values because `DependencyTrackingStore`'s `on conflict ... do update set` clause
named neither and could not clobber a column it did not name. Two rules followed and both have
lapsed. "Never add those columns to the `do update set` clause" went with the clause. "A missing row
is an open gate" went with the MapStore's ownership of row lifecycle: the row now exists from the
moment the chunk can be dispatched, so absence is no longer a case any reader has to interpret. What
survives unchanged is the `NOT NULL DEFAULT TRUE` convention above, which never depended on the
projection.

Site B has to run after the termination chunk's own `chunk` row exists.
`dependencytracking_jobid_fkey` is a foreign key on `(jobid, chunkid)` into `chunk`, so the insert
fails outright otherwise. That falls out of the existing order in
`createJobTerminationChunkEntity`, which persists and flushes the chunk before it takes the job row
lock, but it is the reason the gate write belongs at the end of that method rather than at its top.

And the cross-job barrier answers from `job.termination_barrier_lifted` rather than from row
*presence*, which keeps it independent of when a row is deleted and leaves it answerable once the row
is gone. See **Cross-job submitter barrier** below.

**Partitioning and delivery overlap.** Chunks are scheduled from inside the partitioning
loop - `partitionJobIntoChunksAndItems` calls `jobSchedulerBean.scheduleChunk` per chunk - so
data chunks are processed and delivered while later chunks of the same job are still being
created. `PgJobStore` says so at the point where it marks the job partitioned: "Due to
asynchronous operations processing and delivering phases may complete before partitioning is
marked as done."

For the whole of that window the two counters read `k delivered of 0 expected`, because
`data_chunks_expected` is not written until partitioning ends. Any check that trusts the
counters alone concludes the job is complete from the first delivery onward. What actually
holds the barrier shut is not the counters but the fact that `gate_open` lives on the
termination chunk's `dependencytracking` row, and that row does not exist until
`markJobAsPartitioned` runs after the loop. Until then there is nothing to dispatch. This is
the same reason the gate keys on `is_termination` rather than on `data_chunks_expected = 0`.

Three consequences, none of them optional:

1. **`data_chunks_expected` is written in the transaction that inserts the termination
   chunk.** `markJobAsPartitioned` is `@REQUIRES_NEW`, and `createJobTerminationChunkEntity`
   is a *nested* `@REQUIRES_NEW` that loads its own `JobEntity` under `PESSIMISTIC_WRITE`. A
   value written on `markJobAsPartitioned`'s own instance is still uncommitted and therefore
   invisible to that inner transaction. A gate evaluated there would read the column's
   default `0`, conclude `data_chunks_delivered >= 0`, and open the gate while every data
   chunk of the job is still outstanding - and `createAndScheduleTerminationChunk` calls
   `submitToDeliveringIfPossible` immediately afterwards. Pass N down as a parameter and
   write it where the row is inserted, so the expected value, `is_termination` and the gate
   verdict are decided together under one lock.

2. **The increment is unconditional.** Guarding it on "the job has a termination chunk" or
   on `gate_open = FALSE` loses every chunk delivered before the termination chunk existed.
   The counter would then be permanently short of `data_chunks_expected` once that value is
   written, with no further delivery left to arrive, and the gate would never open. For a
   fast sink on a large data file that is most of the job. The only condition on the
   increment is the once-only removal token below.

3. **The gate is evaluated at insert as well as on delivery.** If all N data chunks are
   delivered before `markJobAsPartitioned` runs, no further `chunkDeliveringDone` fires and a
   gate evaluated only there is never evaluated at all.

**Which transaction the increment belongs to.** Not the job-store transaction that wrote
the item's delivery result. `chunkDeliveringDone` is invoked from `JobsBean` *after*
`PgJobStore.addItemDelivered` (`REQUIRES_NEW`) has committed, deliberately so, and the
increment runs in that outer `JobsBean` transaction. See "Why `addItemDelivered`'s return
value is a snapshot, not an event" under [Same-item concurrent redelivery](
#same-item-concurrent-redelivery-job-store-service).

Two orderings make that safe. The chunk's DELIVERING write is already committed when the
increment runs, so the increment can never count a chunk whose phase is not actually
done. And `JobsBean` is `@Stateless` with the default `REQUIRED`, so its transaction
commits before JAX-RS writes the response, meaning no sink can ACK a message whose
increment has not committed. A crash in the gap between the two commits is recovered by
the same mechanism as everything else on this path: the sink never gets its 200, never
ACKs, and on redelivery `addItemDelivered`'s fast path reports DELIVERING done again, so
`JobsBean` calls `chunkDeliveringDone` again.

Moving the increment into the job-store transaction would gain none of this and would
hold the chunk, item and job row locks across the gate update, which is exactly what
keeping `chunkDeliveringDone` outside that transaction avoids.

**The increment must be once-only per chunk.** This is the constraint that actually
matters, and it is about the *token*, not the transaction boundary. `chunkDeliveringDone`
is called more than once for the same chunk by design - every redelivery re-triggers it,
and the false-failure-detection race can produce two genuinely concurrent calls. A
double increment makes `data_chunks_delivered` overtake the number of data chunks that
have actually been delivered. Each call adds exactly 1, so the counter still reaches
`data_chunks_expected` exactly, just too early, and the gate opens while data chunks are
still outstanding - precisely what the barrier exists to prevent. The termination chunk
is dispatched ahead of them, and job-end work such as marcconv's `ConversionFinalizer`
or `PeriodicJobs*FinalizerBean` runs against incomplete data. This is the same shape of
failure as the item counters under [Same-item concurrent redelivery](
#same-item-concurrent-redelivery-job-store-service): premature completion, not a stall.

The hazard that *does* strand the gate permanently is the opposite one. A non-atomic
read-modify-write increment can lose an update, leaving the counter one short of
`data_chunks_expected` with no further chunk to deliver, so the gate never opens. That is
why the increment must be a single atomic statement
(`SET data_chunks_delivered = data_chunks_delivered + 1`) rather than a read followed by
a write.

The removal of the chunk's `dependencytracking` row is the token, since exactly one
concurrent caller can perform it, and the increment is conditioned on having won that
removal rather than on a preceding read of the row. `chunkDeliveringDone` still reads the
entry first, to answer "is this chunk in `QUEUED_FOR_DELIVERY`" and to turn an unknown chunk
away, but that read is a filter and not the token: `get`-then-`remove` is a non-atomic
check-then-act that two callers can both pass. Harmless for the idempotent work that follows
it, not harmless for a counter. See [Phase 1](#phase-1--gate-and-ordered-dispatch-job-store-service)
for the signature. The `>=` in the pseudocode above is defensive only:
it does not mitigate either hazard above, since double counting reaches the total exactly
and a lost update leaves the counter below it forever. It guards only against the counter
being pushed above `data_chunks_expected` by something other than the increment, such as a
backfill or a revised total.

The increment and the row's removal are transactionally atomic from Phase 9 onwards, which is what
makes the `>=` defensive rather than load-bearing. Until then the row lived in a Hazelcast map whose
mutations were not enrolled in the JTA transaction, so a rollback after the removal reverted the
increment without restoring the row and the chunk was uncounted and unrecoverable. The gate was the
first logic whose correctness depended on closing that window.

**Cross-job submitter barrier.** When job B's per-job counter completes, check whether
any earlier job with the same submitter and same sink still has an undelivered termination
chunk, regardless of its gate state (an earlier termination chunk with an open gate may still
be sitting in `SCHEDULED_FOR_DELIVERY` or `QUEUED_FOR_DELIVERY`). If one exists, keep job B's
gate closed.

```sql
SELECT 1 FROM dependencytracking d JOIN job j ON j.id = d.jobid
 WHERE d.sinkid = ? AND d.submitter = ? AND d.jobid < ? AND d.is_termination
   AND j.termination_barrier_lifted IS FALSE
 LIMIT 1
```

Covered by one index seek on `(sinkid, submitter, jobid) WHERE is_termination` plus a
primary-key probe per candidate row, and the candidate set is the earlier termination chunks for
one `(sinkid, submitter)` still present in the table, so typically zero or one. The index is
partial on purpose: `dependencytracking` is upserted on every chunk transition, and holding only
termination rows keeps it off that hot path entirely. `jobid` is a key column so the "earlier
job" comparison is part of the range scan rather than a heap filter. Note that a partial index
does not supersede the existing `dependencytracking_sinkid_submitter_index`, which still covers
non-termination rows.

**Why the check is not "a row is present".** Presence is the obvious formulation and it is still not
the right one. Delivery is not the only thing that removes a chunk's row: `JobPurgeBean` compacts old
jobs and the abort and recheck paths drop a job's rows wholesale, none of them timed by anything the
barrier controls, and the flag is what keeps the barrier answerable once the row is gone. Before
Phase 9 there was a second reason: row deletion belonged to the MapStore and landed up to
`write-delay-seconds` after the chunk left the map, so the table showed a row for a termination chunk
that had been delivered and the barrier answered "still blocked".

That looks like the safe direction, a gate that opens late rather than early, and it is not. The
re-trigger below is edge-triggered: it fires once, from the removal of the earlier termination
chunk, which is precisely the instant at which the table is most stale about that very row. The
single evaluation designed to notice that the blocker is gone is the one guaranteed to read that
it is still there. It declines, and there is no poll and no retry behind it. The gate stays closed
permanently and every job behind it on that submitter stalls with it. A stale read here does not
delay the open, it cancels it.

So the barrier narrows by a flag job-store controls outright:

```sql
ALTER TABLE job ADD termination_barrier_lifted BOOLEAN;
```

**Nullable, with no default.** The column has three meanings and they are all worth keeping apart:

| Value | Meaning |
|---|---|
| `NULL` | this job has no termination chunk, so it never imposes a barrier |
| `FALSE` | it has one and the barrier is not lifted, so later jobs are held |
| `TRUE` | the barrier has been lifted, by delivery or by abort |

Only `REQUIRES_TERMINATION_CHUNK` sink types get a termination chunk, so `NULL` is the common case
by a wide margin. A `NOT NULL DEFAULT FALSE` column would assert that every job in the table is
holding later jobs back, saved from doing damage only by the `d.is_termination` predicate in the
query above, and relying on a second predicate to neutralise a wrong default is how the next query
written against this column gets it wrong. `NOT NULL DEFAULT TRUE` is not wrong in that way, but it
conflates "never had a barrier" with "had one, now lifted", which is the same conflation
`data_chunks_expected = 0` already forces this design to reason around, see the note under
[Barrier Chunks](#barrier-chunks--per-job-gate) on that column's two opposite meanings. There is no
reason to repeat it for the third flag when a nullable column costs nothing.

The lifecycle then reads monotonically, `NULL → FALSE → TRUE`, where creation of the barrier is a
distinct transition rather than a toggle away from a default that already claimed it was lifted.
`SELECT id FROM job WHERE termination_barrier_lifted IS FALSE` is the set of jobs currently
blocking, and `IS NOT NULL` is the set that ever used a termination chunk, which stays answerable
after the chunk rows are gone.

**Write the predicate as `IS FALSE`, never as `NOT ...`.** Both are correct, since `NOT NULL`
evaluates to `NULL` and a `NULL` job is therefore excluded from the barrier, but `IS FALSE` states
the intent without asking the reader to reason through three-valued logic.

`FALSE` is written at site B, the one place a barrier comes into existence, and `TRUE` in the
transaction that lifts it. The obvious objection to any permissive default is that a missed write
fails open, and for a barrier protecting tickle's dataset-wide `mark`/`sweep` a fail-open is data
loss where a fail-closed is only a stall. It does not apply here, because the `FALSE` write and the
`is_termination` row's `INSERT` are the same transaction at site B. A job cannot end up with a
termination row and an unset barrier, and if that transaction rolls back there is no row to block
on either. The migration also stays inert in the sense DI-3018 cared about: existing rows take
`NULL` and hold nothing back.

**The name is `termination_barrier_lifted`, not `..._delivered`, because delivery is only the
usual cause.** The flag answers "does this job's termination chunk still hold back later jobs",
which is the *barrier* in the sense `job-store-service/dependency-tracking.md` defines under
Terminology: what a termination chunk imposes on other chunks, as opposed to the `gate_open` that
is imposed on the termination chunk itself. Abort is the other cause, and it must lift the barrier
too, see **Every removal of a termination row has to re-trigger** under [Barrier Width](
#barrier-width--per-sink-type-job-isolation). A flag called `delivered` would either be set
untruthfully there or left false, and left false it strands every later job on that submitter.

On `job` rather than on `dependencytracking` because it is a per-job fact belonging beside the
counters it is evaluated with, because `job` carries no dependency on the `do update set`
constraint above, and because it stays answerable after the chunk row is gone, which the
`recheckBlocks` gate sweep needs. The flag remains useful after Phase 9 for that last reason, so
it stays.

This check holds back job B's *termination chunk* only. Job B's data chunks are not gated,
which is a deliberate narrowing of what the `waitingOn` barrier does today and is safe for
some sink types but not for all of them. See [Barrier Width](
#barrier-width--per-sink-type-job-isolation).

**Re-trigger.** When a termination chunk is delivered (removed from `dependencytracking`),
re-evaluate the gates of later same-submitter jobs on the same sink: any job that still has
a termination chunk with `gate_open = FALSE`, whose
`data_chunks_delivered >= data_chunks_expected`, and which no longer has an earlier
undelivered termination chunk, gets its gate opened. Without this step, a job whose
counter completed while an earlier termination chunk was pending would stay closed
forever. The `gate_open = FALSE` and termination-chunk conditions are what keep this from
touching a job that is still partitioning. Such a job is a candidate for the scan - same
submitter, same sink, higher job id - and its counters satisfy the comparison for the whole
partitioning window, since `data_chunks_expected` is still on the migration default and
`data_chunks_delivered >= 0` holds however many chunks it has delivered so far. Note this is
wider than it was with `==`, which matched only before the job's first delivery. What
excludes it is that it has no termination row yet.

**Lost-wakeup race between site B and the re-trigger.** Site B reads "an earlier job has an
undelivered termination chunk" while another transaction is marking that same chunk delivered and
running the re-trigger. Under `READ COMMITTED` the reader still sees the old flag value and
inserts `gate_open = FALSE`, while the re-trigger's scan cannot see the not-yet-committed new row.
Both decline, and the gate is left closed with nothing to open it. The job row lock site B already
holds does not help, because the re-trigger locks a *different* job's row.

Serialise the two on the barrier scope: take `pg_advisory_xact_lock` over `(sinkid, submitter)` in
site B and in the transaction that marks a termination chunk delivered and runs the re-trigger.
Every sink type with a termination chunk takes it, not only the full-width ones. The scope is the
one the jobqueue already serialises partitioning on, and the transaction-scoped variant releases
at commit, so it is safe behind a connection pool where a session-scoped lock would not be. The
same lock covers site C once [Barrier Width](#barrier-width--per-sink-type-job-isolation) adds it,
which is why that section describes the race only in terms of the site it adds.

**Lock ordering: job row first, then the barrier scope.** All three sites take at most one job row
lock, then the advisory lock, then `dependencytracking` rows. **No transaction may wait for a job
row while holding the advisory lock.** Site A's data-chunk branch cannot avoid taking the job row
first, because its `+ 1` increment locks that row until commit and only then can the counters be
compared, so the other two sites have to follow it rather than the other way round. Both can:

- The re-trigger writes `termination_barrier_lifted = TRUE` *before* taking the advisory lock. The
  mark still commits atomically with the scan, and it is the scan that has to be mutually exclusive
  with another transaction's barrier read, not the mark. Whichever of the two commits first, the
  other sees it: a barrier read taken before the marking transaction commits reads the old flag but
  is then followed by that transaction's scan finding the row it inserted, and a barrier read taken
  after it commits reads the lifted flag directly.
- Site B takes the advisory lock *after* the `PESSIMISTIC_WRITE` on the job row that it needs
  anyway, and still decides the whole verdict under both.

The inversion this rules out is reachable, not academic. A job's last data chunk and its own
termination chunk can be acknowledged concurrently: `optimizeDependencies` prunes the termination
chunk's `waitingOn` down to the chunks that transitively cover the rest, so it is dispatched when
those clear rather than when every data chunk of its job has been acknowledged. The data chunk's
transaction would then hold the job row and want the barrier scope while the termination chunk's
transaction held the barrier scope and wanted the same job row.

A `SELECT ... FOR UPDATE` on the earlier job's row would serialise the same two paths without an
advisory lock. It is not used, because it only works while every writer goes through a path that
touches those rows, and the advisory lock does not depend on that.

### Barrier Width — Per-Sink-Type Job Isolation

The gate above narrows what a termination chunk holds back, and one sink type depends on
the width it had before.

Today's `waitingOn` barrier is **full width**. `JobSchedulerBean.scheduleChunk` passes
`barrierMatchKey` to `findChunksToWaitFor` for every chunk it schedules
(`JobSchedulerBean.java:167-173`, `DependencyTrackingService.java:388`), so every *data*
chunk of a later same-submitter job on the same sink waits for the earlier job's
termination chunk. The guarantee is "nothing at all from job B reaches the sink before
job A's job-end has been delivered".

The gate as specified is **termination width**. Only termination chunks are inserted with
`gate_open = FALSE`, data chunks take the `TRUE` default, and the cross-job check is
consulted only when a termination chunk's own gate is evaluated. The guarantee shrinks to
"job B's job-end does not reach the sink before job A's". For same-record ordering that is
deliberate and sufficient, since `JMSXGroupID` plus the watermark cover it. It is not
sufficient for job-end work whose correctness depends on no *later* job's data having
landed yet.

**The tickle case.** `tickle-repo` in `TOTAL` mode is exactly that shape:

- `TickleRepo.createBatch` runs the dataset-wide `Record.mark`, setting every `ACTIVE`
  record in the dataset to `RESET`. It runs when the first item of a job reaches the sink.
- `TickleRepo.closeBatch`, called from `TickleMessageConsumer.handleJobEnd` on the
  termination chunk, runs the dataset-wide `Record.sweep`, setting every record still
  `RESET` to `DELETED`. This is how a total ingest expresses "everything the source no
  longer sends is gone".
- Batches are per job: `batch.batchKey` is unique and holds the job id.

Neither query is scoped by batch, so two open batches on one dataset are destructive in
both directions. Under termination width, job B's first delivered item creates and marks a
second batch while job A's termination chunk is still gated. B's mark resets the records A
has just written, and A's later sweep deletes every record B has not yet rewritten.
Records carried by both jobs survive, records carried by only one are deleted, and the
dataset is left silently short. Full width makes that overlap structurally impossible,
which is why nothing in the tickle sink guards against it.

`marcconv` and `periodic-jobs` do not have this shape. `ConversionFinalizer` deletes
conversion blocks and params by `jobId` and uploads one file per job, the
`PeriodicJobs*FinalizerBean`s deliver one job's record set, and ordering between the
job-end events themselves is still held by the cross-job check. Termination width is
enough for them.

**Per-sink-type toggle.** Barrier width therefore becomes a property of the sink type, a
subset of the types that get a termination chunk at all:

```java
// JobSchedulerBean
private static final Set<SinkType> REQUIRES_TERMINATION_CHUNK =
        Set.of(MARCCONV, PERIODIC_JOBS, TICKLE);

// Sink types whose job-end work is not scoped to its own job, so a later job's data
// must not reach the sink until the earlier job's termination chunk is delivered.
private static final Set<SinkType> REQUIRES_FULL_WIDTH_BARRIER = Set.of(TICKLE);
```

Kept in code next to `REQUIRES_TERMINATION_CHUNK` rather than added as a `SinkContent`
field. Width follows from what the sink implementation does at job end, not from an
operator choice, and `SinkContent` is cached per job (`jobEntity.getCachedSink()`), so a
new field would need a defensible default for every cached sink already written. The flag
is read in Java from the cached sink, so no column on `dependencytracking` is needed to
carry it: the sink type is fixed per `sinkid`, and every statement below is already scoped
to one `(sinkid, submitter)`.

The toggle adds a third evaluation site, alongside A and B above:

```
C. On data-chunk insert - scheduleChunk(chunk, job), only for a sink type in
   REQUIRES_FULL_WIDTH_BARRIER, in its own transaction under the barrier-scope
   advisory lock and under no job row lock at all:

       if an earlier job with the same submitter on this sink still has an
          unlifted termination barrier:

           INSERT the dependencytracking row with is_termination = FALSE and
           gate_open = FALSE

       otherwise write nothing
```

Two things the shape of that is deliberate about. It asks
`job.termination_barrier_lifted` rather than whether a `dependencytracking` row is present, for the
reason **Cross-job submitter barrier** gives above: a barrier that answers from presence stops being
answerable the moment the row is purged or dropped. And it decides the gate *in the insert that
creates the chunk's row*, rather than writing the gate afterwards, since a closed gate has nowhere to
be recorded until the row exists. Site B does the same for the termination chunk, which is why it
creates the row whichever way its verdict falls.

Insert time alone is enough on this path, and for a reason outside the gate: the jobqueue
partitions jobs with the same submitter on the same sink strictly one at a time in job id
order (`NQ_FIND_BY_SINK_AND_AVAILABLE_SUBMITTER`), which is the same `(sinkid, submitter)`
scope the barrier uses. When job B's chunks are inserted, job A is fully partitioned, so
A's barrier either stands or has already been lifted. There is no window like
the one that forces site B to exist for a job's own gate. If that jobqueue invariant is
ever relaxed, this site needs the same treatment the termination gate already has.

**Which transaction site C runs in.** Sites A and B are each reached in a transaction opened for
that one piece of work, so the lock *ordering* rule above is the whole of what they have to observe.
Site C is not like that, and the difference decides its implementation.

```
PgJobStore.partitionNextJobForSinkIfAvailable   @Asynchronous, no tx annotation -> REQUIRED
  +- handlePartitioning                          plain self-call, same transaction
      +- self().partition                        @TransactionAttribute(SUPPORTS) -> joins it
          +- partitionJobIntoChunksAndItems       private, same transaction
          |    +- jobStoreRepository.createChunkEntity   REQUIRES_NEW  <- escapes it, per chunk
          |    +- jobSchedulerBean.scheduleChunk         @EJB, REQUIRED -> joins it
          +- jobSchedulerBean.markJobAsPartitioned       REQUIRES_NEW  <- site B, escapes it
```

The transaction `scheduleChunk` joins is open for the whole of the job's partitioning, which for a
large tickle job is minutes. `createChunkEntity`'s own comment is the evidence: it is `REQUIRES_NEW`
"to enable external visibility of job creation progress", which is only meaningful if an outer
transaction is holding everything else invisible. `scheduleChunk` gets away with joining it today
because it does no database work at all, handing the chunk to Hazelcast, which is not JTA-enlisted.

Taking the advisory lock there would hold it for the whole of partitioning, and
`markJobAsPartitioned` is `REQUIRES_NEW`, so site B would then ask for the same lock on a different
connection and wait for a transaction that cannot commit until that call returns. **PostgreSQL sees
no lock cycle, because the wait is on an EJB call rather than on a database lock, so nothing is
detected and nothing times out.** It is an undetectable hang, not a deadlock, and it would hit every
job on a full-width sink. Holding the lock that long would also block site A and the re-trigger for
the deliveries of the job ahead, which run in the JAX-RS callback transaction with a sink waiting on
the response.

So the rule for site C is a boundary rather than an ordering: **it evaluates and writes in its own
transaction, and takes no job row lock at all.** The lock is then held for one short transaction per
closed chunk, and the row is committed by the time `scheduleChunk` returns rather than at the end of
partitioning, so it is visible to dispatch for the whole window it is needed in. That the outer
transaction holds no job row lock during the loop is not an assumption: site B takes
`PESSIMISTIC_WRITE` on that job's row from a nested `REQUIRES_NEW` transaction today and does not
hang.

The barrier read can be split in two, which is what keeps the common case cheap. An unlocked read
first, in the caller's transaction, and the lock and the write only if it says something is
blocking. That is sound in one direction only, and it is the direction it is used in: a "nothing is
blocking" answer cannot go stale into "blocking", since a barrier for an earlier job in this scope
only comes into existence at that job's site B and the jobqueue finished partitioning it first. A
"blocking" answer can go stale the other way, and that is precisely the answer that goes on to take
the lock and read again. Note this depends on READ COMMITTED taking a fresh snapshot per statement;
under REPEATABLE READ the read would be pinned to the opening snapshot of a transaction that spans
the whole job.

A single conditional statement, `INSERT ... SELECT ... WHERE EXISTS (barrier check) ON CONFLICT ...`,
looks like it would remove the need for the lock and does not. One statement takes one snapshot at
its start, so a re-trigger committing between that snapshot and the insert is still read as
unlifted, while its `UPDATE` has already run and missed the not-yet-inserted row. Both decline,
which is the same lost wakeup.

The re-trigger widens to match. On removal of a termination chunk, for a full-width sink
type, later same-submitter jobs need their data-chunk gates opened too:

```sql
UPDATE dependencytracking d
   SET gate_open = TRUE
 WHERE d.sinkid = ? AND d.submitter = ? AND d.jobid > ?
   AND NOT d.gate_open
   AND NOT d.is_termination
   AND NOT EXISTS (SELECT 1
                     FROM dependencytracking e
                     JOIN job j ON j.id = e.jobid
                    WHERE e.sinkid = d.sinkid
                      AND e.submitter = d.submitter
                      AND e.is_termination
                      AND e.jobid < d.jobid
                      AND j.termination_barrier_lifted IS FALSE)
```

The `NOT EXISTS` is what keeps three or more queued jobs correct: delivering job A's
termination chunk releases job B's data chunks, and job C's stay closed behind job B's
still unlifted barrier. It is one probe of the partial
`(sinkid, submitter, jobid) WHERE is_termination` index per candidate row, plus a primary-key probe
into `job`, and it joins `job` for the same reason site C's own predicate does.

**The statement is run for every sink type, not only the full-width ones.** Only site C ever closes
a data chunk's gate and it runs only for `REQUIRES_FULL_WIDTH_BARRIER`, so for any other sink type
this matches nothing. Making it unconditional is what keeps the sink type out of the delivery path
entirely: `chunkDeliveringDone` holds a `DependencyTracking` and would otherwise have to load a
`JobEntity` per delivered termination chunk purely to ask what kind of sink it was, and the abort
and recheck paths would each need the same. `REQUIRES_FULL_WIDTH_BARRIER` is therefore read in exactly
one place, `scheduleChunk`. It also reopens a row closed by an earlier deployment, or by a sink
whose type has since changed.

**Direct-mode dispatch must filter on `gate_open` too**, not only order by the dispatch
key. With termination width that mattered for one synthetic chunk per job. With full width
a closed gate is the normal state of every data chunk of a queued job, so a direct
`READY_FOR_DELIVERY → QUEUED_FOR_DELIVERY` transition that skips the check would deliver
exactly what the barrier is holding.

**Every removal of a termination row has to lift the barrier and re-trigger, not just
delivery.** `chunkDeliveringDone` is the normal path. `JobsBean.abortJob` (`JobsBean.java:115`)
and `AdminBean.recheckBlocks` (`AdminBean.java:132`) both drop a job's rows through
`removeJobId`. Under full width, an aborted job whose termination row disappears without a
re-trigger strands every later same-submitter job's data chunks permanently.

Both paths must `SET job.termination_barrier_lifted = TRUE` *before* running the re-trigger, and
not merely run it. Dropping the rows is what takes away the edge a lift would otherwise have fired
on, and the re-trigger is edge-triggered, so a pass that fires without lifting the flag reads the
aborted job as still blocking, declines, and never fires again. That is the same failure the flag exists to prevent, arriving by the abort path instead of
the delivery path. It is also why the flag is named for the barrier rather than for delivery: an
aborted termination chunk was never delivered, but its barrier is genuinely lifted.

**The lift is guarded on `IS FALSE`, and the guard is not cosmetic.** These two call sites reach
*any* job whose rows are being removed, not only the minority that hold a barrier. An unguarded
`UPDATE job SET termination_barrier_lifted = TRUE WHERE id = ?` would rewrite `NULL` to `TRUE` on
every job that never had a termination chunk, collapsing "never raised a barrier" into "raised one,
now lifted" and undoing the whole point of the nullable column. Guarded, the statement's row count
also answers whether this job was holding a barrier at all, so the re-trigger and its scan can be
skipped entirely when it was not.

Today the equivalent case is stale keys left in a later job's `waitingOn`, and the hourly
`recheckBlocks` sweep is what releases them, so the gate belongs in that same sweep. That sweep is
also the backstop for the race below, and it is not an afterthought to the width change: without it,
widening the gate strands jobs in two reachable cases, so the two halves are one mechanism.

Four requirements, and the third is the one that is easy to get wrong:

1. **Open a closed gate when no earlier job in its scope holds an unlifted barrier.** For a data
   chunk that is the whole condition, since a data chunk has no counter of its own.
2. **For a termination chunk, additionally require the job's own data chunks to be delivered.**
   Opened on the earlier barrier alone it would be dispatched while its own job's data chunks were
   still in flight, which is what the per-job gate exists to prevent in the first place.
3. **Derive "its own data chunks are delivered" from state that survives a rollback, never from
   `data_chunks_delivered`.** `chunkDeliveringDone` removes the chunk's map entry *before* the gate
   work and outside the JTA transaction, so anything throwing afterwards rolls the increment back
   while the removal stands, and on redelivery `chunkDeliveringDone` finds no tracker and returns at
   once. The counter is then permanently one short with no delivery left to arrive. A sweep reading
   that counter cannot repair the one failure it is there for. The absence of `dependencytracking`
   rows without `is_termination` for the job is the reading that survives, since the removal is the
   half that stood; the DELIVERING phase counters on `job.state` are the alternative, committed by
   `addItemDelivered` in its own `REQUIRES_NEW` transaction before the window opens.
4. **Lift the barrier of a job standing with `termination_barrier_lifted = FALSE` and no
   `is_termination` row**, and run the re-trigger afterwards. This is the same loss on the
   termination branch, where the flag is never written and every later job on that submitter stalls.
   It also catches a job whose rows were dropped by a path that could not resolve its scope.

The window this covers opens when [Phase 1](#phase-1--gate-and-ordered-dispatch-job-store-service)
starts filtering dispatch on `gate_open`, and closes at Phase 9, where the row removal becomes
transactional. The sweep has to cover the interval between the two.

The sweep needs no knowledge of sink type: a closed data-chunk row can only have been written by
site C, which runs only for a full-width sink, so its existence is the answer. It works per barrier
scope, taking each scope's advisory lock, so it never opens a gate on a barrier reading that another
transaction is in the middle of changing.

**Lost-wakeup race between site C and the re-trigger.** Site C reads "an earlier
termination row exists" while another transaction is removing that row and running the
re-trigger. Under `READ COMMITTED` the reader still sees the committed row and inserts
`gate_open = FALSE`, while the re-trigger's `UPDATE` cannot see the not-yet-committed new
row. The chunk is then closed with nothing left to open it. Two guards, and the first is
the real one:

- Serialise the two on the barrier scope: take `pg_advisory_xact_lock` over
  `(sinkid, submitter)` in site C as well. The lock itself is not new here. Site B and the
  removal plus re-trigger transaction already take it for every sink type with a termination
  chunk, because the same race exists between those two, see **Lost-wakeup race between site B
  and the re-trigger** under [Barrier Chunks](#barrier-chunks--per-job-gate). This section only
  adds site C to the set of places that take it.
- The `recheckBlocks` gate sweep above bounds the damage to one sweep interval if a path
  is ever added that misses the lock.

Full width holds delivery only, exactly as `BLOCKED` does today. Chunks are still
processed and accumulate in `SCHEDULED_FOR_DELIVERY`, which has no cap, so a large tickle
job queued behind another is no more expensive to hold than it is now.

**Changes nothing until Phase 9.** Until `waitingOn` is deleted it still enforces full
width for every sink type, so the gate being narrower has no observable effect. The flag
and site C can therefore land inert in Phase 1 and be verified against the `waitingOn`
behaviour they replace, but they must be live before Phase 9 removes the mechanism they
are replacing.

Scoping the tickle sink's `mark`/`sweep` to a batch instead was considered and rejected:
total-ingest delete detection is inherently dataset-wide, `tickle-repo-api` is shared with
the tickle harvester and other consumers, and the change would move a correctness
guarantee out of the scheduler, where every other cross-job ordering rule lives, into one
sink.

### Counter Management

`SINK_STATUS` Hazelcast counters are replaced by a `ConcurrentHashMap<Integer, AtomicInteger>`
in the scheduler service JVM. On startup the map is rebuilt from a
`COUNT(*) GROUP BY sinkid, status` query over `dependencytracking`. No distributed
synchronisation is required because the scheduler is a single-instance service
(see [Scheduler as a Standalone Service](#scheduler-as-a-standalone-service)).

This replacement happens at scheduler extraction (Phase 11), not before. Until then
the counters must remain a distributed map: counter mutations are triggered by
`chunkProcessingDone`/`chunkDeliveringDone`, which run on whichever job-store
instance receives the REST callback (`JobsBean`), so a per-instance JVM map in the
multi-instance job-store would diverge.

---

## Record Identity — `correlationKey`

A `correlationKey` field is added to `dk.dbc.dataio.jobstore.types.RecordInfo`. Note
that plain `RecordInfo` carries only `id` and `pid`; the record type information needed
to derive the key (`type`, `delete`) lives on the `MarcRecordInfo` subclass — the
derivation logic belongs there. Non-MARC `RecordInfo` instances use `correlationKey = id`.

### Mapping

| Record type | `correlationKey` |
|---|---|
| Standalone | `RecordInfo.id` |
| Head | shared hierarchy constant (e.g. `__hierarchy__`) |
| Section | shared hierarchy constant |
| Volume | shared hierarchy constant |
| Barrier | `null` — no record key; `JMSXGroupID` not set (see [Open Questions](#open-questions) §1) |

Head/Section/Volume hierarchies are a small but important part of workloads. For live
records the parent must arrive at the target before the child. For delete-marked records
the child must arrive before the parent. **All** records that are part of a hierarchy
share one constant `correlationKey` and are therefore serialised into a single broker
group per sink queue: the scheduler's `(priority DESC, jobId ASC, chunkId ASC)` ordering
ensures the right item arrives first within the serialised group — for every hierarchy
at once.

This deliberately trades throughput for simplicity: hierarchical records are delivered
one at a time per sink, which is acceptable because they are a small fraction of the
workload. In return, no section-to-head resolution is required anywhere — the key
derives from the record's own type alone, so data partitioners need no hierarchy
lookups and no section-to-head mapping table. Should the single group ever become a
measured bottleneck, grouping can later be refined to the head record's id
(per-hierarchy groups) without changing the protocol; that refinement re-introduces
section-to-head resolution in the partitioners. The constant must be chosen so it
cannot collide with a real record id.

### Priority Override for Live Head/Section Records

A chunk containing a **live** head or section record is given the highest scheduler
priority. This ensures the head arrives before any volume record referencing it, even
if the volume came from a newer higher-priority job.

Chunks containing **delete-marked** head or section records keep their original priority.

Detecting a live head/section record requires only the record's own `type` and `delete`
flags — no hierarchy resolution is involved.

### JMSXGroupID vs. Watermark Key

- **`JMSXGroupID`** = `RecordInfo.correlationKey` — used for broker serialisation.
  All head/section/volume records share the constant group ID and are therefore
  processed serially by the broker.

- **Watermark key** = `<agencyId>:RecordInfo.id`, not bare `RecordInfo.id` — the
  watermark tracks the highest-delivered version *per individual record*, not per
  group, but `RecordInfo.id` alone is only meaningful within the agency that
  assigned it (see "Why `recordKey` is carried on the message" under Per-Item
  Dispatch, and the `WaitFor(sinkId, submitter, matchKey)` precedent it follows —
  `submitter` there holds the same agencyId value). Volume records from different jobs
  for the same physical volume are superseded correctly even though they share the
  constant correlationKey with all other hierarchy records — and two different
  agencies' records that happen to share a bare id are *not* wrongly superseded
  against each other, because the agency qualification keeps their watermark rows
  distinct.

**Known gap — correlationKey changes between jobs.** Broker serialisation only covers
two versions of the same record if they carry the same `correlationKey`. With the
constant hierarchy key, changed placement *within* the hierarchy space (a volume moved
to a different head) no longer changes the key and is fully covered. The remaining gap
is a record that switches between standalone and hierarchical *type* between jobs — a
standalone record re-catalogued as a volume, or vice versa: the two versions land in
different broker groups and can be processed concurrently on different pods. The window
between the watermark GET and the target delivery is then a read-then-act race: both
pods can read a watermark older than both versions and both deliver, and the older
version may reach the target last. This requires the same record to be in-flight in two
jobs *and* to have changed type between them *and* the deliveries to interleave within a
sub-second window — a very rare occurrence, accepted as a known limitation.

---

## Per-Item Dispatch

`SinkMessageProducerBean` iterates over `ItemEntity` rows for the chunk and sends one
JMS message per item:

```java
// job is the JobEntity already in scope in SinkMessageProducerBean.send(Chunk, JobEntity, int)
final long agencyId = job.getSpecification().getSubmitterId();
final String recordKeyPrefix = agencyId + ":";
for (ItemEntity item : chunkItems) {
    Message msg = session.createTextMessage(serialize(item.getProcessingOutcome()));

    // routing and validation headers, carried unchanged from the chunk message
    // (see "Headers on an item message" below - these are not optional)
    JMSHeader.payload.addHeader(msg, JMSHeader.ITEM_PAYLOAD_TYPE);
    JMSHeader.sinkId.addHeader(msg, sinkReference.getId());
    JMSHeader.sinkVersion.addHeader(msg, sinkReference.getVersion());
    if (flowBinderReference != null) {
        JMSHeader.flowBinderId.addHeader(msg, flowBinderReference.getId());
        JMSHeader.flowBinderVersion.addHeader(msg, flowBinderReference.getVersion());
    }
    JMSHeader.trackingId.addHeader(msg, chunk.getTrackingId());

    // item identity
    msg.setIntProperty(JMSHeader.jobId,    jobId);
    msg.setIntProperty(JMSHeader.chunkId,  chunkId);
    msg.setShortProperty(JMSHeader.itemId, item.getKey().getId());
    RecordInfo ri = item.getRecordInfo();
    if (ri != null) {
        if (ri.getId() != null) {
            msg.setStringProperty(JMSHeader.recordKey, recordKeyPrefix + ri.getId());
        }
        if (ri.getCorrelationKey() != null) {
            msg.setStringProperty("JMSXGroupID", ri.getCorrelationKey());
        }
    }
    producer.send(msg);
}
```

The sample is illustrative rather than literal in two respects. Headers are set through
`JMSHeader.<name>.addHeader(message, value)` rather than the raw `setXxxProperty` calls,
and `chunkId` is a **long** property, not an int: that is what `MessageIdentifiers
.addIdentifiers` writes on the chunk message today, consumers read it back through
`getObjectProperty` and unbox it, so an item message has to carry the identical property
type. `jobId` is an int and `itemId` a short, as shown.

The body is the item's processing outcome alone, so every sink sees one shape.

### Items of a chunk are sent in ascending `itemId` order

Two items of the same chunk can share a `correlationKey`, either as two versions of one
record or as any two records of one hierarchy, since every hierarchy record carries the
same `MarcRecordInfo.HIERARCHY_CORRELATION_KEY`. The broker serialises a group in the
order its messages were sent, so sending out of order would let a lower
`(jobId, chunkId, itemId)` version arrive after a higher one, and the watermark would
then judge the higher one stale.

The producer therefore requires its input to be ordered by ascending item id, and gets it
from a single `ORDER BY item_id ASC` query. This has always been the order
`PgJobStoreRepository.getChunk` produced, but as an incidental property of how it built a
`Chunk` rather than as a stated contract. Under per-item dispatch the watermark depends on
that order, so it has to become one.

All item messages of a chunk are produced through one `JMSContext`, so the enlisted XA
session makes either all of them or none of them visible to the sink. A partially
dispatched chunk is therefore not a state the sink can observe.

### Headers on an item message

Splitting one chunk message into N item messages does not change what the sink framework
reads off a message *before* any per-sink code runs, so every header the chunk message
carries today has to be carried by each item message. `MessageConsumer.onMessage` and
`MessageConsumer.validateMessage` (`commons/artemis-jse-app`) enforce most of this
already, and the failure modes are silent rather than loud:

| Header | Read by | Consequence if absent |
|---|---|---|
| `payload` | `validateMessage` | `InvalidMessageException`, and `onMessage` catches it and **discards the message with a warning**. Not a rollback, so the item is lost with no retry |
| `jobId` | `onMessage`, unboxed to `int` | NPE inside `onMessage`, rethrown as `IllegalStateException`, transaction rolls back and the message is redelivered forever |
| `chunkId` | `onMessage` (logging), result reporting | Item cannot be attributed to a chunk when reporting delivery |
| `trackingId` | `onMessage` (logging) | Loses the correlation id that ties log lines across components together |
| `sinkId`, `sinkVersion` | config refresh in `ims`, `vip`, `dpf`, `openupdate`, `rawrepo-update-v3`, each unboxed to `long` | NPE on every message, so those five sinks stop delivering entirely |
| `flowBinderId`, `flowBinderVersion` | queue-provider lookup in `dpf` (`ConfigBean`) and `openupdate` (`UpdateMessageConsumer`), each unboxed to `long` | NPE in those two sinks whenever the job has a flow-binder reference |

None of these are new requirements. They are the existing contract, and the point of
listing them is that the per-item send path is a second producer that has to satisfy the
same contract as `createMessage` does today.

**A new payload type.** `payload` cannot keep the value `CHUNK_PAYLOAD_TYPE`, because
`unmarshallPayload` accepts that value and then parses the body as a `Chunk`. An item
body is a single `ChunkItem`, so a sink that has not yet been migrated would fail inside
Jackson with a message about invalid `Chunk` JSON, which says nothing about the actual
cause. A new constant `JMSHeader.ITEM_PAYLOAD_TYPE = "Item"` alongside
`CHUNK_PAYLOAD_TYPE` makes the same situation produce the explicit
`payload type Item != Chunk` diagnostic instead. Either way the message is discarded,
so this is about diagnosability during rollout, not about tolerating an un-migrated
sink. Both halves of the rollout still have to ship together, as
[Phase 8+](#phase-8--individual-sink-migrations-one-pr-per-sink) requires.

**The abort path is untouched.** `AbstractMessageProducer.sendAbort` sends a chunk-level
message with `payload = ABORT_PAYLOAD_TYPE` and `abortId`, handled by `onMessage` before
the `jobId` read and before validation. It stays as it is. The `ABORTED_JOBS` discard
check in `onMessage` keys on `jobId` alone and therefore filters individual item
messages of an aborted job with no change.

`agencyId` is read via `job.getSpecification().getSubmitterId()` — `JobSpecification`
has no separate `agencyId` accessor, but `submitterId` *is* the agency/library number in
this domain: the RR harvester populates a job's submitter number directly from each
record's own `RecordId.getAgencyId()` (`HarvestOperation.java`:
`.withSubmitterNumber(recordId.getAgencyId())`), and gatekeeper/CLI tooling treats the
two as interchangeable (`JobStoreSearcher.java`: `.put("submitterId",
Integer.valueOf(args.getString("agency")))`). "agencyId" is used here as the more
precise domain name for that same value; nothing new is being read.

**Do not confuse this with `Submitter.id`.** flow-store-service's `Submitter` type has
two differently-scoped numeric fields that are easy to mix up precisely because they
sit right next to each other: `Submitter.id` is an autogenerated flow-store database
primary key, with no meaning outside that table, while `SubmitterContent.number` is the
actual business/library number. `JobSpecification.submitterId` is the latter, not the
former — `JobSpecification.java` carries an explicit comment on the getter saying so
("Submitter id represents the unique submitter number and not the id generated by the
system when a new submitter is created"), and every producer of it in this codebase
(the harvester, the transfile parser, gatekeeper, the CLI tool above) writes the raw
business number with no flow-store round-trip to resolve a `Submitter` entity first.
flow-store-service itself exposes `GET .../submitters/searches/number/{number}` as a
*separate* endpoint specifically to resolve a business number to a `Submitter` (and
its distinct DB `id`) — the existence of that lookup is itself evidence the two are not
interchangeable in general; `JobSpecification.submitterId` happens to already be on the
business-number side of that lookup, so no resolution step is needed here.

This is also the same field `JobSchedulerBean.scheduleChunk()` already reads to
populate `DependencyTracking.submitter` (`(int) job.getSpecification().getSubmitterId()`,
see `JobSchedulerBean.java`), so both the old dependency-tracking key and the new
watermark key are sourced from the one place the agency is actually known.
`getSubmitterId()` returns `long`; its decimal string form can never contain `:`, so
`recordKeyPrefix + id` is injective — distinct `(agencyId, id)` pairs cannot produce
the same composed string, even though `id` itself is "effectively arbitrary" (see
below) and could in principle contain a colon. The first `:` in the composed string is
therefore always the unambiguous delimiter, without needing to escape or reject colons
in `id`.

Items with null `recordInfo` or null `correlationKey` receive no `JMSXGroupID` and are
distributed freely across consumers — they are processed unconditionally anyway.
`correlationKey` (`JMSXGroupID`) is not agency-qualified — see [JMSXGroupID vs.
Watermark Key](#jmsxgroupid-vs-watermark-key): two different agencies' records sharing
a literal id can end up serialised into the same broker group unnecessarily, which is
the same benign "false collision" cost already accepted for `group-buckets` hashing,
not a correctness issue. Only the watermark key needs the agency dimension, because
unlike `JMSXGroupID` grouping — where over-grouping only costs a little latency — a
watermark collision actively produces a wrong `SUPERSEDED` verdict for an unrelated
record (see the invariant discussion below).

`RecordInfo` is already stored per `ItemEntity`; no new schema is required. The
`JMSHeader.itemId` and `JMSHeader.recordKey` constants do not exist today and must be
added to `commons/types` (`JMSHeader.java`). `itemId` identifies the individual item
within the chunk: it is part of the `(jobId, chunkId, itemId)` version tuple compared
against the watermark before delivery, and of the result-reporting endpoint path
`POST /jobs/{jobId}/chunks/{chunkId}/items/{itemId}/delivered`. No chunk-size
header is needed: job-store detects chunk completion from its own phase counters, and
since the broker distributes items by record group — not by chunk — no single consumer
is guaranteed to see all items of a chunk anyway.

**Why `recordKey` is carried on the message instead of re-derived by the sink.** An
earlier version of this design left `getRecordKey(Message)` (see [Sink framework](
#sink-framework-sinkmessageconsumeradapter) below) entirely up to each sink, on the
assumption that any sink can parse its own record's identity back out of the delivered
content (e.g. MARC field 001 §a) as reliably as job-store did. It cannot, in general:
`RecordInfo`'s constructor applies `StringUtil.removeWhitespace(id)`
(`RecordInfo.java`), so job-store's own `id` — and therefore the watermark key that
must match it — is whitespace-normalised, while the raw bytes in the delivered payload
are not guaranteed to be. A sink re-deriving the key from raw content would read
`" 4 2 "` where job-store stores `"42"`; the two never compare equal, the watermark
lookup silently never matches, and stale-delivery detection quietly stops working for
that record with no error anywhere. Putting job-store's already-normalised `RecordInfo
.getId()` directly on the message removes the re-derivation step (and the class of bugs
that come with re-deriving the same value two different ways) entirely — every sink
reads the identical, canonically-normalised key job-store itself would compare against.

**The key is agency-qualified, and sinks must treat it as opaque.** `RecordInfo.id`
is only meaningful within the agency that assigned it — the existing dependency-
tracking system already encodes this, one level removed: `WaitFor(sinkId, submitter,
matchKey)` scopes a match key to a submitter, and `submitter` there is populated from
the same `job.getSpecification().getSubmitterId()` value this document calls `agencyId`
(see above), so two agencies' records that happen to carry the same id simply don't
interact under `WaitFor`. `sink_record_delivery_watermark`'s primary key
`(sink_id, record_key)` has no such qualifier — if `record_key` were the bare
`RecordInfo.id`, it would assert a *stronger* uniqueness than the data actually has:
two unrelated records from two agencies that happen to share an id would collide into
one watermark row, and — unlike `WaitFor`'s "simply don't interact" failure mode — the
older of the two jobs would be judged stale and wrongly reported `SUPERSEDED`. Composing
`recordKey` as `<agencyId>:<id>` (see the code sample above) restores the missing
dimension and makes the watermark's uniqueness claim match `RecordInfo.id`'s actual
scope, using the exact same value the dependency-tracking system already derives from
`job.getSpecification().getSubmitterId()`.

The composed string is deliberately kept a single opaque token, not an
`(agencyId, id)` pair split across two columns/properties/parameters. Everything
downstream — the `sink_record_delivery_watermark.record_key` column, the
`GET /sinks/{sinkId}/watermarks?recordKey={recordKey}` query, and the
`WatermarkServiceConnector` signatures (`getWatermark(long sinkId, String recordKey)`,
`reportItemResult(..., String recordKey, ...)`, see below) — stays exactly as specced,
because the composition happens once, in job-store, at the same place that already
carries `RecordInfo.id` onto the message. This matters specifically because
`getRecordKey(Message)` deliberately lives on `MessageConsumerAdapter` itself, not per
sink (see [Sink framework](#sink-framework-sinkmessageconsumeradapter) below): every
semantic the sink framework has to know about the key's internal structure is a
semantic some sink's `deliverItem` could still end up depending on by accident. An
opaque pass-through token has no internal structure for a sink to depend on in the
first place — the same rationale as carrying the id itself instead of re-deriving it,
one dimension further.

---

## Artemis Broker Configuration

Sink queues are **not declared** in `broker.xml`. Each sink resolves its queue name at
runtime from its `QUEUE` environment variable (for example `sink::dummy`,
`sink::update/fbstest/validate-only`), and the queue is auto-created by the broker on
first connect. A new sink is deployed without touching broker config, so the group
settings cannot be attached to individual `<queue>` elements.

### Queue naming is FQQN, so all sinks share one address

`sink::dummy` is not an address called `sink::dummy`. `::` is Artemis Fully Qualified
Queue Name syntax, so it means **queue `dummy` on address `sink`**. Every sink queue
therefore lives on the single address `sink`:

| `QUEUE` value | Address | Queue |
|---|---|---|
| `sink::dummy` | `sink` | `dummy` |
| `sink::update/fbstest/validate-only` | `sink` | `update/fbstest/validate-only` |
| `processor::business` | `processor` | `business` |
| `DLQ::DLQ` | `DLQ` | `DLQ` |

Address settings match on the address, so one exact-match rule covers every current and
future sink, with no wildcard and no list of queue names:

```xml
<address-settings>
   <address-setting match="sink">
      <default-group-rebalance>true</default-group-rebalance>
      <default-group-rebalance-pause-dispatch>true</default-group-rebalance-pause-dispatch>
      <default-group-first-key>JMSXGroupFirstForConsumer</default-group-first-key>
      <default-group-buckets>1048576</default-group-buckets>
   </address-setting>
   ...
</address-settings>
```

Verified against Artemis 2.42.0: queues reached through `sink::*` receive all four
settings, while `processor`, `processor-graaljs` and `DLQ` are untouched, since they are
separate addresses. `group-buckets` is a per-queue attribute, so each of the 36 sink
queues gets its own bucket table despite sharing an address.

Scoping the settings to sinks is deliberate rather than cosmetic. `group-rebalance` fires
on every consumer add or remove regardless of whether the queue has any groups, and
combined with `group-rebalance-pause-dispatch` it pauses dispatch until in-flight
messages are acked. Applied to the processor addresses that would mean a dispatch pause on
every processor pod restart for no benefit.

Two consequences of FQQN worth knowing when operating the broker:

- Management resource names and JMX object names use the **real** address and queue, so it
  is `address="sink",queue="dummy"`, never `address="sink::dummy"`. Per-queue management
  lookups keyed on `queue.sink::dummy` fail.
- Destructive operations must be given the FQQN form. `destroyQueue("sink::vip")` removes
  exactly that queue, whereas the bare queue name is ambiguous across addresses and was
  observed removing queues of the same name on more than one address.

### What these settings do and do not provide

Measured against a real broker with and without the settings: per-group serialisation is
provided by `JMSXGroupID` itself and is on by default. With all four settings absent,
groups were still pinned to one consumer, per-group order was still preserved, and two
messages of a group were never in flight together.

The ordering guarantee that replaces `BLOCKED` therefore comes from Phase 6 setting
`JMSXGroupID`, not from this configuration. What the configuration adds:

| Setting | What it adds |
|---|---|
| `group-buckets` | Bounds group-tracking memory. The default of -1 tracks every group id ever seen, which is unbounded when the id is a record id |
| `group-rebalance` | Lets a consumer that joins later receive work at all. Without it a late-joining consumer took 0 of 480 messages while the others carried the load |
| `group-rebalance-pause-dispatch` | Makes the handover that `group-rebalance` introduces safe, by holding dispatch until in-flight messages are acked |
| `group-first-key` | Nothing that is consumed, see below |

In short, this phase does not create the guarantee. It keeps the guarantee true across
scaling events and pod restarts, and bounds its memory cost. That distinction matters when
reasoning about what breaks if the configuration is missing: deliveries stay correctly
serialised, but new sink replicas sit idle and broker memory grows with the number of
distinct record ids seen.

### `group-first-key` is set but not consumed

`group-first-key=JMSXGroupFirstForConsumer` makes the broker set a
`JMSXGroupFirstForConsumer=true` header on the first message of a group dispatched to a
given consumer, so a consumer can detect that it has just been handed that group. The key
name comes from the Artemis documentation example.

Nothing in dataio reads this header, and the delivery protocol does not need it. A sink
resolves the record key and checks it against the watermark per item, so it never depends
on knowing whether it has just acquired a group, and per-record state lives in PostgreSQL
rather than in the pod. It is retained because it is named in the DI-2999 acceptance
criteria, costs one boolean header, and is a useful signal when debugging group handover.

Phase 7 therefore does not need to expose it through `MessageConsumerAdapter`. If a later
change does come to depend on it, note that it marks a *consumer handover*, not the first
version of a record, so it is not a substitute for any part of the watermark comparison.

### Existing queues do not inherit these settings

`default-*` address settings are applied **when a queue is created**. A queue that already
exists keeps its stored attributes across a broker restart, so deploying the config above
against a broker whose sink queues already exist changes nothing, silently. The config
file reads correctly while every queue still reports `group-buckets=-1` and
`group-rebalance=false`.

`group-rebalance-pause-dispatch` also cannot be set on an existing queue at all.
`QueueControl` exposes `isGroupRebalancePauseDispatch()` for reading, but no
`ActiveMQServerControl.updateQueue` overload accepts it, so the management API and the
`artemis queue update` CLI can repair only three of the four attributes.

Existing sink queues must therefore be **dropped and re-created** once, while drained, so
that the address-setting defaults apply. Verified against 2.42.0: after a drop, the
auto-created replacement carries all four attributes, and they persist across a
subsequent broker restart. This relies on `auto-delete-queues` remaining `false` in
`broker.xml`, otherwise an empty re-created queue is removed again on the next restart.

Because `JMSXGroupID` is only set from Phase 6 onwards, this recreation is behaviourally
inert and can be done well ahead of any delivery-path change.

### `group-buckets`

Without `group-buckets`, Artemis tracks one entry per unique group ID seen — unbounded
memory growth. With `group-buckets=N`, Artemis uses a fixed-size hash table of N slots:
`slot = hash(groupId) % N`. Memory is O(N) regardless of how many unique record IDs
have ever been seen.

Peak simultaneously active groups = `QUEUED_FOR_DELIVERY` limit × items per chunk =
1 000 × 10 = **10 000**. False-collision fraction ≈ K/N for K ≪ N:

| N | False-collision fraction | Broker memory |
|---|---|---|
| 65 536 | ~15% | 512 KB |
| 262 144 | ~3.8% | 2 MB |
| **1 048 576** | **~0.95%** | **8 MB** |
| 4 194 304 | ~0.24% | 32 MB |

**Use `group-buckets=1048576`** — < 1% false collision, 8 MB per queue. Use a power of
two to avoid modulo bias. A false collision only serialises two unrelated records against
each other (one extra message-latency), which is imperceptible.

**Do not use `group-timeout`** as a memory-management alternative. If processing time
exceeds the timeout, Artemis may expire the group and reassign it to a different consumer
before the first item is ACKed, allowing two items for the same record to be active
concurrently.

### `group-rebalance` and `group-rebalance-pause-dispatch`

`group-rebalance=true` clears all bucket assignments when a consumer is added.

`group-rebalance-pause-dispatch=true` pauses all dispatch until every in-flight message
is ACKed before proceeding. Since the watermark is written to PostgreSQL inside
`reportItemResult` (which happens before `session.commit()`), the watermark store is
fully up-to-date by the time dispatch resumes after rebalancing.

This broker-side guarantee is what allows the watermark check to be correct without a
local inflight cache: by the time a pod reads a watermark after a rebalance, all prior
deliveries are reflected in PostgreSQL.

For `CONSUMER_CLOSED` (pod crash): `group-rebalance` does not trigger. The crashed pod's
unACKed messages are redelivered. Because those messages were never ACKed, `reportItemResult`
was never called for them — the watermark reflects only what was committed, which is
correct.

### `JMSXGroupID` semantics are Artemis-specific

Consumer-pinning and serial-dispatch semantics for `JMSXGroupID` are an Artemis
(and HornetQ) extension. The JMS 2.0 spec names the property but does not mandate these
semantics. If the broker is ever replaced, verify the replacement provides equivalent
group serialisation before relying on ordering correctness.

---

## Delivery Watermark (job-store-service)

### Table

```sql
-- record_key is the opaque, job-store-composed "<agencyId>:<RecordInfo.id>" string
-- (see "Why recordKey is carried on the message" under Per-Item Dispatch) — not a bare
-- record id. The agency qualification lives inside the string, not as a separate
-- column, so this table's shape does not change from an earlier, unqualified design.
CREATE TABLE sink_record_delivery_watermark (
    sink_id       INT          NOT NULL,
    record_key    VARCHAR      NOT NULL,
    job_id        INT          NOT NULL,
    chunk_id      INT          NOT NULL,
    item_id       SMALLINT     NOT NULL,
    last_modified TIMESTAMPTZ  NOT NULL DEFAULT now(),
    PRIMARY KEY (sink_id, record_key)
);
```

`last_modified` exists solely to drive retention pruning (see [Retention Policy](
#retention-policy) below) — it plays no role in the version comparison itself, which
is done purely on `(job_id, chunk_id, item_id)`. It is set on first insert, and
refreshed only when the `ON CONFLICT DO UPDATE` branch actually fires — that branch
is gated by the same `WHERE (...) > (...)` version-advance guard as the rest of the
row, so `last_modified` advances exactly when the watermark itself advances, **not**
on every delivery attempt for the record. A delivery that the `WHERE` clause rejects
(an exact retransmit where `incoming == watermark`, or an older version that gets
`SUPERSEDED`) leaves `last_modified` untouched. See [Retention Policy](#retention-policy)
for why this matters.

An explicit `agency_id INT` column in the primary key (rather than folding it into
`record_key`) would make the dimension visible in the schema and give ops a plain index
seek for "every watermark row for agency X". It was not chosen here because it turns
"which dimensions make a watermark key" from a job-store-only concern into a sink
framework concern too — the REST path, the connector method signatures, and every
`getRecordKey(Message)` implementation would need a second parameter, whereas the
opaque token keeps every one of those exactly as specced (see above). If ad hoc queries
like "every watermark row for record 12345678 across agencies" turn out to be needed
in practice, add denormalised `agency_id`/`record_id` columns alongside the opaque
`record_key` PK for that querying, rather than restructuring the key itself.

### Upsert on delivery

`reportItemResult` atomically advances the watermark when the incoming version is newer:

```sql
INSERT INTO sink_record_delivery_watermark
       (sink_id, record_key, job_id, chunk_id, item_id, last_modified)
VALUES (?, ?, ?, ?, ?, now())
ON CONFLICT (sink_id, record_key) DO UPDATE
  SET job_id        = EXCLUDED.job_id,
      chunk_id      = EXCLUDED.chunk_id,
      item_id       = EXCLUDED.item_id,
      last_modified = EXCLUDED.last_modified
  WHERE (EXCLUDED.job_id, EXCLUDED.chunk_id, EXCLUDED.item_id)
      > (sink_record_delivery_watermark.job_id,
         sink_record_delivery_watermark.chunk_id,
         sink_record_delivery_watermark.item_id)
```

### REST endpoints

**Read watermark (called by sink before each delivery):**

```
GET /sinks/{sinkId}/watermarks?recordKey={recordKey}
→ 200 {"watermark": {"jobId": 1234567, "chunkId": 42, "itemId": 3}}
→ 200 {"watermark": null}          (no watermark exists for this record key)
```

`recordKey` is a query parameter, not a path segment. `RecordInfo.id` is arbitrary
harvested data — `RecordInfo`'s constructor only normalises whitespace (see above), so
nothing rules out `/` or `%` inside it. A path segment containing a literal `/` breaks
route matching outright (silent 404, indistinguishable from "no watermark exists" — the
same silent-wrong-answer class the whitespace discussion above worries about), and a
percent-encoded `%2F` is rejected or normalised by some servlet container
configurations before it ever reaches application code. A query parameter value has no
such restriction and round-trips any byte sequence through standard URL encoding.

Backed by a direct PostgreSQL PK read: `(sinkId, recordKey)` is the primary key; the
lookup is O(1). No Hazelcast. If throughput under per-delivery call rate becomes a
measured bottleneck, an in-process cache or Hazelcast IMap can be added later without
changing the correctness argument.

**Call-rate impact.** Today a sink makes one HTTP call per chunk (`addChunk`, 10 items).
The new protocol makes one GET plus one POST per *item* — a ~20× increase in HTTP calls
to job-store-service, plus one watermark upsert per item on PostgreSQL. At a sink
throughput of 1 000 items/s this is 2 000 requests/s against the job-store fleet.
Capacity-plan job-store-service instances and their connection pools accordingly. If
needed, a batched read (`GET /sinks/{sinkId}/watermarks?keys=k1,k2,…`) lets a sink
prefetch watermarks for all items of a chunk in one round trip without changing the
correctness argument.

**Report item result (called by sink after delivery):**

```
POST /jobs/{jobId}/chunks/{chunkId}/items/{itemId}/delivered
Content-Type: application/json

{ "sinkId": 42, "recordKey": "...", "status": "DELIVERED" | "SUPERSEDED" | "IGNORED" | "FAILED" }
```

Same path as the existing `GET .../items/{itemId}/delivered` (which reads back
`ItemEntity.deliveringOutcome`) — POST writes the delivery result, GET reads it, one
resource. This also matches the existing bulk `POST /jobs/{jobId}/chunks/{chunkId}/delivered`
endpoint's naming: "delivered" there already covers `FAILURE`/`IGNORE` item outcomes
within the chunk, not just success, so the same word covering `SUPERSEDED`/`IGNORED`/`FAILED` at
item granularity here is consistent, not a stretch.

Implementation (one transaction):

```
1. Write deliveredResult to ItemEntity.deliveringOutcome
2. Increment State.Phase.DELIVERING counters on ItemEntity, ChunkEntity and JobEntity
3. If this item completes the job (see below):
       set fatal-error flag when it is a FAILED termination item,
       set timeOfCompletion, add the JOB_COMPLETED notification
4. If status == DELIVERED AND recordKey != null:
       DB upsert watermark (using conditional upsert above)
5. If ChunkEntity.state passes State.phaseDone(DELIVERING):
       call chunkDeliveringDone()  → per-job counter + gate logic
```

Steps 1 to 4 are the job-store transaction (`PgJobStore.addItemDelivered`,
`REQUIRES_NEW`). Step 5 runs in `JobsBean` *after* that transaction commits, driven by
the method's boolean return value - deliberately outside it, so the chunk, item and job
row locks are released before `chunkDeliveringDone`'s unbounded unblock cascade and its
JMS sends begin. See "Why `addItemDelivered`'s return value is a snapshot, not an event"
under [Same-item concurrent redelivery](#same-item-concurrent-redelivery-job-store-service).

Step 3's "completes the job" test is `itemCompletesJob`, the single-item counterpart to
the bulk path's `chunkCompletesJob`: all job phases done, **and** either
`numberOfItems == the job's PARTITIONING counter total` or this item is the termination
item. Both clauses are needed, and no additional "was the job already done" guard may
be added - see [Same-item concurrent redelivery](#same-item-concurrent-redelivery-job-store-service)
for why such a guard would
silently break every job that has a termination chunk.

**Idempotency:** if `ItemEntity.deliveringOutcome` is already set, return 200 and take
no further action. This handles crash-then-redelivery between step 4 and `session.commit()`.

### Version comparison

Versions are compared as `(jobId, chunkId, itemId)` tuples — lexicographically on the
sink side (compare `jobId` first, then `chunkId`, then `itemId`) and as a native row
comparison in the SQL upsert above. No bit-packing into a `long`: a packed encoding
would impose hard bit-width limits (e.g. 16 bits caps `chunkId` at 65 535, which
million-record jobs exceed) and risks diverging from the SQL tuple comparison. The
tuple is carried as three plain integer fields in the REST payloads.

### Retention Policy

`sink_record_delivery_watermark` grows with the number of unique record keys ever
delivered per sink, and is never pruned by the upsert itself. A nightly job removes
rows whose watermark has not *advanced* — not merely rows with no delivery activity
at all, see the note on `last_modified` under [Delivery Watermark](
#delivery-watermark-job-store-service) — for longer than a configurable retention window:

- `WatermarkPurgeBean.purgeStaleWatermarks()` deletes rows whose `last_modified` is
  older than `now() - retention`, in one bulk statement.
- `ScheduledWatermarkPurgeBean` fires the purge once daily (`@Schedule(hour = "3")`),
  mirroring the existing `ScheduledJobPurgeBean` → `JobPurgeBean` pair used for job
  purging, including the same `Hazelcast.isSlave()` master-only guard.
- The retention window is a MicroProfile Config property, `WATERMARK_RETENTION`
  (`java.time.Duration`, default `P90D`), following the same idiom as
  `PROCESSOR_TIMEOUT`.

**Accepted correctness trade-off.** A record whose watermark has not *advanced* for
longer than the retention window loses its row — this is a stronger condition than
"no delivery at all". An exact retransmit (`incoming == watermark`, always delivered
per [Per-item delivery sequence](#per-item-delivery-sequence)) or an older version
that gets `SUPERSEDED` both leave `last_modified` untouched, so a record can keep
receiving delivery attempts indefinitely without its row ever refreshing. If a
genuinely older version of that record then arrives after the row is pruned, it is
no longer caught by the version check and is delivered instead of skipped.

This remains acceptable, but for a narrower reason than "no delivery in months is
irrelevant": under normal operation a record simply stops generating any traffic
once its current version has been delivered — no new messages exist for it until a
later job supersedes it — so "time since last watermark advance" and "time since
last delivery attempt" coincide for the overwhelming majority of records. The two
diverge only under redelivery/retry scenarios — crash-then-redelivery, or
`AdminBean`'s stale-chunk re-dispatch (see [Stale recovery](#stale-recovery)) — and
those resolve within the same operational incident (minutes to hours), not months
later, so they don't meaningfully extend the pruning window in practice.

The residual case this doesn't cover: a chunk stuck being redelivered against a
persistently failing target for longer than the retention window, without ever
being superseded by a newer job, would have its watermark row pruned mid-incident.
A chunk retried for 90+ days is itself an operational anomaly that monitoring should
already be surfacing independently of this trade-off, so it is accepted as a known,
narrow limitation rather than grounds for a different design — but the retention
window should be chosen with comfortable margin over any expected stuck-retry
duration, not just normal job turnaround.

---

## Sink Delivery Protocol

### Per-item delivery sequence

```
1. Receive item message (session not yet committed)
2. key = getRecordKey(message)
3. if key != null:
       watermark = GET /sinks/{sinkId}/watermarks/{key}
       incoming  = (jobId, chunkId, itemId)
       if watermark != null AND incoming < watermark:   // lexicographic tuple compare
           POST reportItemResult(... SUPERSEDED)
           session.commit()
           return
       // incoming == watermark: exact retransmit, always deliver (idempotent re-delivery)
4. deliver item to target system
5. POST /jobs/{jobId}/chunks/{chunkId}/items/{itemId}/delivered  (reportItemResult)
6. session.commit()   →   JMS ACK
```

Every result reports the watermark row it concerns, whatever its status, and only a
`DELIVERED` one advances that row (see [Upsert on delivery](#upsert-on-delivery)). A null
`recordKey` therefore means the item has no watermark row at all - a barrier item, or a
sink that has opted out - and never "this outcome must not advance the watermark".

Step 5 before step 6 preserves the existing crash-safety guarantee: if the pod dies
between steps 5 and 6, the message is redelivered and the idempotency guard in step 5
handles the duplicate.

### No local inflight map in sinks

The original redesign maintained a per-pod `ConcurrentHashMap<String, Long>` loaded from
the watermark table at startup. This was rejected because:

1. **Bulk startup load**: potentially millions of rows — unacceptable heap and startup time.
2. **Cross-pod staleness**: after a rebalance, the new pod's in-memory map reflects only
   what was written before the pod started; deliveries made by other pods since then are
   invisible. No sweep or notification mechanism can close this gap without residual
   correctness windows.

The correct solution is no local cache: every delivery decision reads from the shared
authoritative store. The `group-rebalance-pause-dispatch=true` broker setting guarantees
that the store is fully up-to-date at the moment dispatch resumes after any rebalancing
event.

### Sink framework (`SinkMessageConsumerAdapter`)

The protocol lives on a new `SinkMessageConsumerAdapter extends MessageConsumerAdapter`,
not on `MessageConsumerAdapter` itself. Two of that class's subclasses consume whole
chunks permanently and will never deliver an item: `job-processor2`'s
`JobStoreMessageConsumer` (`JobProcessorMessageProducerBean` keeps sending
`CHUNK_PAYLOAD_TYPE`) and `dlq-errorhandler`'s `DLQMessageConsumer` (which handles both,
see [Dead item messages](#dead-item-messages)). Putting the item protocol on the shared
base would leave both inheriting a `deliverItem` that is meaningless for them and could
never be made abstract. With the split, the 15 sinks extend the new class, `deliverItem`
is abstract from the start, and `handleConsumedMessage` is final there.

Subclasses implement one method:

```java
protected abstract ItemDeliveryResult deliverItem(ConsumedMessage message, ChunkItem item)
        throws Exception;
```

`ItemDeliveryResult`, rather than its `Status` alone, because job-store stores
`deliveryResult.chunkItem()` verbatim as `ItemEntity.deliveringOutcome` — the status
alone leaves the sink no way to say what it delivered. The sink builds its half with
`ItemDeliveryResult.of(status, chunkItem)` and the framework adds the half it owns with
`withWatermarkKey(sinkId, recordKey)` - the pair being the primary key of
`sink_record_delivery_watermark` - so no sink names a watermark key. The status is
deliberately not derived from the returned `ChunkItem.Status`: each status value means one
specific thing for the delivering phase (see [The four verdicts](#the-four-verdicts)
below), where `ChunkItem.Status` is a general-purpose outcome reused across partitioning,
processing and delivering. Coupling the two would tie job-store's counting to whatever a
sink's `ChunkItem.Status` happens to be for unrelated reasons.

#### The four verdicts

The returned `ChunkItem` is stored verbatim and feeds no counter, so the status is the only
thing deciding how job-store counts the item and whether the record's watermark moves:

| `Status` | Meaning | DELIVERING counter | Watermark | Returned by |
|---|---|---|---|---|
| `DELIVERED` | sent to the target | succeeded | advances | the sink |
| `SUPERSEDED` | not sent, a newer version of the record already was | ignored | untouched | the framework only |
| `IGNORED` | not sent, there was nothing to send | ignored | untouched | the sink |
| `FAILED` | attempted, rejected in a way retrying will not fix | failed | untouched | the sink |

`IGNORED` is what a sink returns where it would previously have put an `IGNORE` item in a
delivered chunk, typically for an item whose processing outcome was itself a failure or an
ignore. It exists because the counters changed source: the chunk-level path read them off
each item's `ChunkItem.Status` (`PgJobStoreRepository.setItemStateOnChunkItemFromStatus`,
`FAILURE → failed`, `IGNORE → ignored`, `SUCCESS → succeeded`), and the per-item path reads
them off the verdict (`PgJobStore.deliveryStatusAsStateChange`). Without a fourth value such
an item has to be reported `DELIVERED`, which counts it as succeeded — overstating what
reached the target in a `job_completed` mail that prints
`state.states.DELIVERING.succeeded` as "heraf ok" — and, worse, advances the record's
watermark, claiming a version as delivered that never was. A genuinely older version
arriving afterwards is then judged stale and superseded, leaving the target with neither.
Both halves follow from the verdict, so one value fixes both.

`SUPERSEDED` and `IGNORED` are indistinguishable to job-store, which counts both as ignored
and advances neither watermark. They are two values because they are two different answers
to "why is this record not at the target", which is the question asked when investigating
one, and because they have different authors: only `SinkMessageConsumerAdapter` returns
`SUPERSEDED`, since a sink does not read the watermark and therefore cannot detect
supersession. That split is documented rather than enforced — a sink returning it wrongly
produces the same counter and the same watermark behaviour as `IGNORED`, so the mistake is
misleading in metrics and harmless in effect, and enforcing it would add a failure mode to
catch something that cannot corrupt anything.

An earlier version of this document said instead that "a sink that wants the old ignore
semantics returns `DELIVERED` with an `IGNORE` item". That preserves the item's visible
outcome, which is stored verbatim, and not its contribution to the counters or its effect
on the watermark, which is why it was replaced by a verdict of its own.

The framework takes `ConsumedMessage` rather than the raw `jakarta.jms.Message`: it is
what `validateMessage` already produces on the path to `handleConsumedMessage`, it
carries every JMS property plus the body, and it needs no JMS provider in a test. The
body is unmarshalled once, by the framework, and passed alongside the message, so the
sink does not parse it a second time.

Throwing from `deliverItem` rolls the session back and has the item redelivered, so a
failure the target will not recover from must be *returned* as `FAILED` rather than
thrown.

The watermark key is **not** left to subclasses — `handleConsumedMessage` reads
`JMSHeader.recordKey` straight off the message (`null` if absent, e.g. for a
barrier/termination item whose `RecordInfo.getId()` was null at dispatch time — see
[Open Questions](#open-questions) §1). No sink re-derives this key from delivered
content; see the rationale in
[Per-Item Dispatch](#per-item-dispatch) above. This also means adding a new sink no
longer requires getting record-key derivation right per sink — one less thing for
Phase 8+ migrations to get wrong.

The framework handles the watermark check and result reporting around `deliverItem`. No
lock is required: the broker guarantees that same-key items are never processed
concurrently across threads or pods.

`MessageConsumerApp` and `ServiceHub` are structurally unchanged: the watermark calls
went onto the existing `JobStoreServiceConnector` that `ServiceHub` already carries (see
below), so there is no second connector to wire in.

#### Watermark opt-out

Not every sink has per-record supersession to enforce. `periodic-jobs` and `marcconv`
treat an item as input to work carried out when the job ends rather than as a record
delivered to a target on its own, so there is no "newer version already delivered"
question to ask about them. They override:

```java
protected boolean usesDeliveryWatermark() {   // default true
    return false;
}
```

An opted-out sink skips the watermark GET, delivers every item unconditionally, and never
advances a watermark — the record key is left `null` for it, which is the same path an
item with no record key already takes, so the rest of the protocol needs no second branch.

What it does **not** skip is result reporting: every item is still reported individually,
because the DELIVERING phase counters and the per-job gate are driven by those reports,
and a job whose items are never reported never completes.

##### Job-end work runs against complete data by construction

An aggregating sink's job-end work — the `PeriodicJobs*FinalizerBean`s, marcconv's
`ConversionFinalizer` — reads what every preceding item of the job persisted, so it is
only correct if all of those writes are committed before it starts. The per-item protocol
gives that for free, from the order in which one item is handled: `deliverItem` commits
its own transaction and returns, and `SinkMessageConsumerAdapter` reports the result only
afterwards. A reported item is therefore an item whose writes are durable, and the
termination chunk is released only once every data chunk of the job has reported (by
`waitingOn` today, by `gate_open` once the dispatch filter lands — both driven by
`chunkDeliveringDone`, which fires when a chunk's last item result is committed).

The chunk protocol had this the other way round: each sink called
`sendResultToJobStore(result)` *before* committing its own transaction, so job-store could
see a chunk as delivered while that chunk's data was still uncommitted, and the
termination chunk could be released against an incomplete set. `periodic-jobs` covered
that window with a fixed five second sleep before finalizing, removed in DI-3015 along
with the ordering problem it guessed at. `marcconv` has the same shape and the same
argument applies to it.

### Watermark calls (`job-store-service-connector`)

An earlier version of this document specified a separate `WatermarkServiceConnector`.
What shipped instead puts both calls on the existing `JobStoreServiceConnector`, which
every sink already holds through `ServiceHub`:

```java
public record Watermark(int jobId, int chunkId, short itemId)
        implements Comparable<Watermark> {
    // compareTo: jobId, then chunkId, then itemId, agreeing with the SQL row comparison
}

Optional<Watermark> getWatermark(int sinkId, String recordKey);
void addItemDelivered(ItemDeliveryResult itemDeliveryResult, int jobId, int chunkId, short itemId);
```

`ItemDeliveryResult(long sinkId, String recordKey, Status status, ChunkItem chunkItem)`
carries what the report endpoint needs, so the result item travels with the verdict
rather than as a separate argument.

---

## Operational Resilience

### Sink crash recovery

| Failure point | Behaviour | Recovery |
|---|---|---|
| Before step 4 (deliver) | Message rolls back; redelivered to next available group consumer | No side effects |
| After step 4, before step 5 (reportItemResult) | Message rolls back; redelivered | Duplicate target call. Idempotent targets (rawrepo, solr-doc-store) absorb it. Same exposure as current `addChunkIgnoreDuplicates` retry |
| After step 5, before step 6 (session.commit) | Message rolls back; redelivered | `reportItemResult` called again; idempotency guard sees `deliveringOutcome` already set; ignored cleanly |

### Same-item concurrent redelivery (job-store-service)

The table above assumes redelivery is sequential: the prior attempt's session has
rolled back or timed out *before* the broker hands the message to a second consumer.
That assumption can break under a false failure-detection positive - the broker's
heartbeat/session timeout fires (e.g. during a long GC pause on the sink) while the
original consumer's thread is still alive and mid-flight on its own `reportItemResult`
call - producing two genuinely concurrent calls for the *same* item, not two
sequential ones.

`addItemDelivered`'s idempotency check (`ItemEntity.deliveringOutcome != null`) is not
lock-protected on its own, so both concurrent calls can see it unset and proceed. A
naive implementation would then double-count that item's contribution to the chunk's
and job's DELIVERING counters. A duplicate on an *already-closed* phase is a plain
no-op (see `State.updateStateElement`), but one that lands while the phase is still
open makes the running total overtake the number of items that have actually reported.
Each call adds exactly 1, and `State.phaseDone` compares the running total against the
PARTITIONING total, so the phase still closes on an exact match - just one item too
early. `chunkDeliveringDone` then fires, downstream chunks unblock, and the job can
complete while an item was never delivered at all. Nothing in the job state records the
missing delivery, which makes this worse than a stall rather than a milder version of
one.

The fix is a second, authoritative idempotency re-check, taken only after acquiring
the chunk's exclusive lock (`getExclusiveAccessFor`) - so the two concurrent calls
serialise there, and whichever runs second re-reads `deliveringOutcome` fresh and
observes the first call's already-committed write. The unlocked check at the top of
`addItemDelivered` is kept purely as a fast path for the overwhelmingly common case
(a genuinely already-committed replay), not as the correctness guard. Locking order
is chunk, then item - matching `PgJobStoreRepository.updateChunkItemEntities`'s
existing chunk-then-item order for the bulk path, so no new deadlock risk is
introduced.

**Why `addItemDelivered`'s return value is a snapshot, not an event.** It reports
"is the chunk's DELIVERING phase done as of right now", not "did this specific call
complete it". This matters for two independent reasons:

1. It keeps the same-item race above correct: the call that loses the race and
   returns early through either idempotency check still needs to report accurate,
   current chunk status rather than an unconditional `false`.
2. It makes recovery self-healing across the gap between the job-store transaction
   committing and `JobsBean` invoking `jobSchedulerBean.chunkDeliveringDone(...)`
   afterwards (deliberately outside that transaction - see "Delivery Watermark").
   If the pod dies in that gap, the sink never gets its response, the message is
   never ACKed, and it gets redelivered. Under an event-based return value, that
   redelivery would hit the idempotency guard and report nothing happened, so the
   gate notification would never fire again - stranding the chunk exactly as above,
   just via a different trigger. A snapshot return value means every redelivery,
   however it arises, re-evaluates and reports current status, and `JobsBean` can
   simply call `chunkDeliveringDone(...)` again whenever it reports done -
   `JobSchedulerBean.chunkDeliveringDone` already tolerates being called on an
   unknown or already-completed tracking key (it logs and returns), so a redundant
   call is harmless. This mirrors the legacy `addChunkDelivered` path, which already
   calls `chunkDeliveringDone` unconditionally rather than trying to suppress
   duplicates itself.

**Why job completion needs no equivalent "already done" guard.** The completion block
in `addItemDelivered` (fatal-error flag, `timeOfCompletion`, the `JOB_COMPLETED`
notification) must run exactly once per job. It is tempting to reach for the same
transition-detection shape as the chunk-level fix above - snapshot whether the job was
already done *before* applying this call's delta, and fire only on the not-done to done
edge. That is both unnecessary and actively harmful here.

It is unnecessary because there is no job-level analogue of the same-item race. Every
read and write of job state in `addItemDelivered` happens after
`getExclusiveAccessFor(JobEntity.class, jobId)`, so two concurrent item deliveries for
one job - whether from the same chunk or two different chunks - serialise at exactly
that point. Chunk locks being independent of each other is true but irrelevant: the
job row lock is what orders the completion check.

Once-only is instead guaranteed by `itemCompletesJob`'s own two clauses, given how
termination chunks are accounted for:

- **Job without a termination chunk** (the common case): `numberOfItems` equals the
  job's `PARTITIONING` counter total, so the first clause selects the single item whose
  delta closes the job's DELIVERING phase. No item reports after it, because the
  item-level idempotency check guarantees each item contributes exactly one delta.
- **Job with a termination chunk** (less common, but it happens):
  `createJobTerminationChunkEntity` bumps `jobEntity.numberOfItems` by one while
  passing `updateJobEntityState` a `StateChange` with zeroed counters, so
  `numberOfItems` is the `PARTITIONING` total **+ 1**. The first clause is therefore
  false for every data item, and only the termination item selects itself, through the
  `isTerminationItem` clause. This mirrors the bulk path's
  `chunkCompletesJob(...)`/`chunk.isTerminationChunk()` pair, whose `||` clause exists
  for precisely this reason.

And it is harmful because of that second case. The job's DELIVERING phase closes when
its delivering total reaches its `PARTITIONING` total (`State.phaseDone`), which the
last **data** item achieves - before the termination item has reported at all. A
"was the job already done" guard therefore sees the job as complete by the time the
termination item arrives and suppresses the completion block entirely, so such jobs
would never get `timeOfCompletion`, never emit `JOB_COMPLETED`, and never set the
fatal-error flag for a failed termination item. `PgJobStore_AddItemDeliveredIT`
pins all four cases, including this one.

### Dead item messages

A message that exhausts its delivery attempts is moved by the broker to `DLQ::DLQ`, where
`dlq-errorhandler` fails it on the job's behalf. Under per-item delivery that message is
an item rather than a chunk, so the handler branches on `JMSHeader.payload` and reports
the dead item with
`addItemDelivered(ItemDeliveryResult.of(FAILED, deadItem).withWatermarkKey(sinkId, recordKey), …)`
instead of `addChunk`.

This is what keeps the failure visible rather than silent. Job completion is driven by
one report per item, and no sink will ever report an item the broker has given up on, so
without this branch the item's `deliveringOutcome` stays null, the DELIVERING counters
never reach their total, and the job hangs unfinished with nothing saying why.

The reported `recordKey` is whatever the message carries, as on every other reporting
path: a `FAILED` result never advances a watermark, but that is the endpoint's rule to
apply, and a null key means the item has no record identity at all rather than that this
outcome must not advance anything. A dead termination item keeps `ChunkItem.Type.JOB_END`,
recognised the way `Chunk.isTerminationChunk` recognises it, so it still reaches the
fatal-error branch in `addItemDelivered`.

### Job-store restart

All scheduling state lives in PostgreSQL (`dependencytracking`, `item`, `chunk` tables).
job-store-service carries no in-memory scheduling state and no Hazelcast dependency —
it is immediately consistent the moment it accepts requests.

### Scheduler-service restart

On startup the scheduler-service rebuilds its `SINK_STATUS` counters from a
`COUNT(*) GROUP BY sinkid, status` query over `dependencytracking` and begins the poll
loop. No durable state is lost; the next poll cycle picks up exactly where the previous
instance left off.

### Broker restart

Artemis group-bucket slot assignments are in-memory and reset on restart. Durable
messages survive. After restart, the first message for each bucket pins to a new consumer
via round-robin; subsequent messages for that bucket go to the same consumer. Since the
assignment is hash-deterministic (`hash(correlationKey) % N`), all messages for a given
record hierarchy route to the same bucket index after restart. Serialisation within a
bucket is re-established from the first post-restart message for that bucket.

### Stale recovery

`AdminBean.updateStaleChunks()` finds `QUEUED_FOR_DELIVERY` rows whose
`timeOfLastModification` is older than a configured threshold and moves them back to
`SCHEDULED_FOR_DELIVERY` for re-dispatch. With per-item dispatch, re-dispatch sends all
N item messages for the chunk. Items whose `deliveringOutcome` is already written receive
a duplicate `reportItemResult` call; the idempotency guard ignores it. Items still
missing their outcome are processed normally.

---

## Scheduler as a Standalone Service

The scheduling logic (`JobSchedulerBean`, `JobSchedulerBulkSubmitterBean`, `AdminBean`)
is extracted from job-store-service into its own dedicated service.

**Division of responsibility:**

| Layer | Handles |
|---|---|
| job-store-service (N instances) | All inbound REST requests: job submission, partitioning callbacks (`/chunks/{id}/processed`), item delivery results (`/items/{id}/delivered`), watermark reads. Writes item/chunk state, the watermark, the per-job counters (`data_chunks_delivered`, `data_chunks_expected`), `job.termination_barrier_lifted`, and `gate_open` to PostgreSQL. Owns the whole per-job gate, `JobGateBean` and `JobGateRepository`, and owns `dependencytracking` row lifecycle: it inserts the row when partitioning creates the chunk and deletes it on delivery. |
| scheduler-service (1 instance) | Continuously reads `dependencytracking` from PostgreSQL, advances chunk `status` through the state machine, fires JMS dispatch. Writes `status` and nothing else on the row, inserting and deleting none. Reads `gate_open` in its dispatch query and never writes it. Holds in-memory `SINK_STATUS` counters. |

**Where the per-job gate lands.** The gate stays job-store's across the extraction, and the two
beans that implement it, `JobGateBean` and `JobGateRepository`, stay in job-store-service. Site B
holds them there outright: `PgJobStoreRepository.createJobTerminationChunkEntity` evaluates the
gate inside the partitioning transaction, under the job row lock it already holds, and partitioning
does not move. Three consequences for the extraction:

- **Site A has to be re-anchored before `JobSchedulerBean` moves.** `JobGateBean.advanceGateState`
  is called today from `JobSchedulerBean.chunkDeliveringDone`, which this phase moves wholesale.
  The call belongs on `JobsBean`'s delivery callback path instead, which is also where the
  transaction requirement puts it: the increment must run in the `JobsBean` transaction rather than
  the `REQUIRES_NEW` one that writes the item result, so it commits before JAX-RS writes the
  response and no sink can acknowledge a message whose increment has not committed. Re-anchoring is
  independent of the extraction and can land in any earlier phase.
- **The delivery-side `DELETE` stays with the increment.** Once the removal of the chunk's
  `dependencytracking` row is the once-only token the increment is conditioned on (DI-3049), the
  two have to commit together, so job-store issues that `DELETE ... RETURNING is_termination` even
  though scheduler-service owns every other write to `dependencytracking.status`. The column sets
  stay disjoint. What this adds to job-store's side is row lifecycle on one path, not a second
  writer of any column the scheduler writes.
- **The `recheckBlocks` gate sweep stays in job-store-service.** It writes `gate_open`, so moving
  it along with the rest of `AdminBean` would put a gate writer in scheduler-service and make the
  table above wrong. It is left behind as an admin endpoint, alongside `JobsBean.abortJob`, which
  is the other non-delivery gate writer and does not move either.

The barrier scope lock needs no attention here. `pg_advisory_xact_lock` over `(sinkid, submitter)`
is a database lock, so it serialises across two services exactly as it serialises across job-store
instances today. That is an argument for keeping one owner of the gate SQL rather than duplicating
`JobGateRepository.barrierLockKey` in two modules, not a reason to revisit the locking.

**Why this eliminates leader election entirely:**

The existing scheduler is guarded by `if(isMaster()) { ... }` (currently using
`Hazelcast.isMaster()`) to ensure only one job-store-service instance drives scheduling.
Extracting the scheduler into its own single-instance service makes that guard redundant —
there is only ever one scheduler. No Hazelcast, no advisory lock, no distributed consensus
needed for scheduling.

**Interaction model:**

The scheduler service polls PostgreSQL for chunks ready to advance. Triggered transitions
(e.g. a chunk becoming `READY_FOR_DELIVERY` after a processor callback, or a termination
gate opening) are also picked up by the poll loop — the scheduler is never called
synchronously by job-store-service. The poll interval is configurable; a short interval
(e.g. 1 s) is equivalent to the existing `@Schedule(second="*/1")` timer.

The scheduler writes `status` back to PostgreSQL and sends JMS messages.
job-store-service writes `deliveringOutcome`, `state` counters, the watermark,
`data_chunks_delivered`, and `gate_open` (the gate logic runs in the `reportItemResult`
transaction — see [Barrier Chunks](#barrier-chunks--per-job-gate)). These are distinct
column sets; no write conflicts arise.

**Accepted trade-off — loss of direct-mode dispatch latency.** Today
`JobSchedulerBean.chunkProcessingDone()` calls `submitToDeliveringIfPossible()`
synchronously (`JobSchedulerBean.java:311`), so a chunk moves toward delivery the
instant the processor callback lands. With a polling scheduler every state transition
waits for the next poll tick — up to ~2 s of added latency per chunk across the two
dispatch hops. This is irrelevant for pipelined throughput (the poll batch keeps the
queues full) but noticeable for small latency-sensitive jobs. If it matters in practice,
PostgreSQL `LISTEN/NOTIFY` can wake the scheduler on state writes without reintroducing
any synchronous coupling.

**Hazelcast fate:**

Removing `DEPENDENCY_TRACKING`, `LAST_TRACKER`, and `SINK_STATUS` eliminates most — but
not all — Hazelcast usage in job-store-service. The remaining usages must each be
resolved before the dependency can be dropped:

| Remaining usage | Where | Replacement |
|---|---|---|
| `ABORTED_JOBS` distributed set | `JobsBean` | Column on the `job` table (or small `abortedjobs` table); it is checked per chunk callback, so a DB read per callback is acceptable — or cache with short TTL |
| `Hazelcast.executeOnMaster(new RemotePartitioning(sink))` | `PgJobStore.java:216-217` | Partitioning no longer routes to a master node. Decide its new home: run on whichever job-store instance receives the request (partitioning writes only DB state, so any instance works), or move to scheduler-service |
| `isSlave()` guard | `ScheduledJobPurgeBean` | Move the scheduled purge to scheduler-service (single instance, no guard needed) |
| `isSlave()` guards | `BootstrapBean`, `AdminBean` cache/admin operations | Scheduling-related parts move to scheduler-service; per-instance cache flushes lose the guard (each instance flushes its own cache) |
| `isMaster()` / `isSlave()` scheduling guards | `JobSchedulerBean`, `JobSchedulerBulkSubmitterBean` | Deleted — the single-instance scheduler-service needs no guard |

Once these are resolved, the Hazelcast dependency is dropped from job-store-service
entirely, and scheduler-service never introduces it.

---

## Sequence Analysis Removal

`SequenceAnalysisData` per chunk (the full set of record keys, stored as `matchKeys`)
was the sole input to `addAndBuildDependencies()`. That call is removed.

| Artefact | Fate |
|---|---|
| `SequenceAnalysisData` | Removed |
| `SequenceAnalysisDataConverter` | Removed, with both its `<class>` entries in `persistence.xml` |
| `SinkContent.SequenceAnalysisOption` | Removed, together with the field, the constructor parameter, the accessor and the field's part in `equals`/`hashCode` |
| `KeyGenerator` and `DefaultKeyGenerator` | Removed. The interface goes with its only implementation, its only caller being the key set |
| `ChunkEntity.sequenceAnalysisData` column | Dropped by `V13` |
| `PgJobStoreRepository.createChunkEntity`'s `KeyGenerator` parameter | Removed, with `PartitioningParam.keyGenerator` and its accessor, which supplied the argument |
| `ChunkItemEntities.keys`, `getSequenceAnalysisData`, `getSequenceAnalysisOption` | Removed. The last was a `JobEntity` lookup per chunk during partitioning, purely to read the option |
| `RecordInfo.getKeys` and the `MarcRecordInfo` override | Removed rather than reduced to a no-argument method, which would have no caller |
| `dependencytracking.matchkeys` (jsonb) | Dropped by `V11`, in Phase 9 rather than here: it held the scheduler's copy of the keys, not the source |
| `dependencytracking.waitingon` (jsonb, GIN-indexed) | Dropped by `V11`, in Phase 9 |

`ItemEntity.recordInfo` already holds the record key per item. `SinkMessageProducerBean`
reads `RecordInfo.getCorrelationKey()` directly when building item messages. That method is
what survives on the producer side: after this removal it is the only key job-store derives
from a record, and there is no chunk-level key set at all.

Flow-store needs no change and no migration. `SinksBean` stores the sink definition as the
JSON string it was posted as and unmarshalls to `SinkContent` only to validate, and
`SinkContent` carries `@JsonIgnoreProperties(ignoreUnknown = true)`, so an existing stored
definition and one posted by a client still sending `sequenceAnalysisOption` both keep
validating. The member survives in `sinks.content` and stops being read. Stripping it from
live flow configuration would be a write against every sink definition for no reader's
benefit, so it is deliberately left in place.

---

## Removed Components

| Component | Reason |
|---|---|
| `DependencyTracking.waitingOn` | Dependency graph removed |
| `DependencyTracking.matchKeys` / `waitFor` | Sequence analysis no longer drives scheduling |
| `LAST_TRACKER` Hazelcast map | Used only by `trackChunksToWaitFor` |
| `DEPENDENCY_TRACKING` Hazelcast map | Replaced by PostgreSQL-only dependency table |
| `RemoveWaitingOn` entry processor | Unblocking removed |
| `AddTerminationWaitingOn` entry processor | Replaced by per-job counter |
| `UpdatePriority` entry processor | Priority boosting removed |
| `DependencyTrackingService.findChunksToWaitFor()` | Removed |
| `DependencyTrackingService.trackChunksToWaitFor()` | Removed |
| `DependencyTrackingService.optimizeDependencies()` | Removed |
| `DependencyTrackingService.boostPriorities()` | Removed |
| `DependencyTrackingService.removeFromWaitingOn()` | Removed |
| `JobSchedulerTransactionsBean.addDependencies()` | Removed |
| `JobSchedulerBean.chunkDeliveringDone()` fan-out loop | Replaced by counter increment |
| `LastTrackerMap` aggregator | Removed |
| `BlockedCounter` aggregator | Removed |
| `DependencyTrackingStore` | The write-behind projection itself. Row lifecycle is synchronous SQL in `DependencyTrackingRepository` |
| `UpdateStatus` entry processor | Replaced by one conditional `UPDATE` whose predicate sits on the target row |
| `StatusCounter`, `SinkStatusCounter` and `JobCounter` aggregators | Replaced by `COUNT(*) GROUP BY` and `COUNT(DISTINCT jobid)` |
| `TrackingKeySer`, `StatusChangeSer`, `UpdateStatusSer` | Their types stopped crossing the wire |
| `DependencyTrackingService.find`, `findDependencies`, `makeDependencyPredicate`, `modify`, `reload` | The map's candidate and mutation machinery |
| `dependency/reload` endpoint | With no map to reload only the recount is left, which `status/sinks/recount` already offers |
| `DependencyTracking(ResultSet)` and `DependencyTracking.resend()` | The MapStore's row mapper, and the retry the statement now carries |
| `staleCandidateSlack`, `isStillAwaitingDelivery`, `DeliveryDispatchStaleStatusIT` | Compensated for the write-behind lag |
| `DeliveryDispatchRepository.hasClosedGate` | The direct path reads `gate_open` off the row it already holds |
| `dependencytracking_sinkid_status_index` | A leading prefix of both ordered indexes (`V12`) |
| `SINK_STATUS` Hazelcast counters | Phase 11: moved to scheduler-service as JVM `ConcurrentHashMap<Integer, AtomicInteger>`. Phase 9 keeps the map and changes only where it is maintained from |
| `ChunkSchedulingStatus.BLOCKED` (value 3) | Deleted; rows migrated to `SCHEDULED_FOR_DELIVERY` |
| `JobSchedulerBean.updateSinks` and the `chunks.blocked` metric | Counted a state that no longer exists |
| `DependencyTrackingService.recheckBlocks`, `find`, `findChunksWaitingForMe`, `findJobBarrier` | Served the graph |
| `PgJobStoreRepository.findDependingJobs` and the abort cascade | See [Aborting no longer cascades](#aborting-no-longer-cascades) |
| `dependency/check_blocked` endpoint | Its subject is gone |
| `SequenceAnalysisData` and `SequenceAnalysisDataConverter` | Nothing has read the per-chunk key set since the graph went |
| `SinkContent.SequenceAnalysisOption` | It steered a computation that no longer happens |
| `KeyGenerator` and `DefaultKeyGenerator` | The interface had one implementation and one caller, both the key set's |
| `RecordInfo.getKeys` / `MarcRecordInfo.getKeys` | `getCorrelationKey()` is the surviving per-record key |
| `PartitioningParam.keyGenerator`, `ChunkItemEntities.keys`, `PgJobStoreRepository.getSequenceAnalysisData` and `getSequenceAnalysisOption` | The plumbing between the option and the column |
| `chunk.sequenceanalysisdata` | Dropped by `V13` |

---

## Migration Path

Each phase is a standalone PR targeting ≤ 500 lines of change. Exception: Phase 11
(scheduler extraction) is mostly *moved* code plus new module scaffolding and will
exceed that limit; its genuinely new logic (the poll loop) stays small.

Two ordering constraints shape the sequence:

1. **Dependency tracking is removed only after every sink is live on the per-item +
   watermark protocol.** Until then, `BLOCKED` is the sole record-level ordering
   guarantee — queue order is not delivery order with concurrent consumers (see
   [Threading Reality at Sinks](#threading-reality-at-sinks)) — and removing it
   earlier would open a window where an older record version can overwrite a newer
   one at the target.
2. **Dependency tracking is removed before the scheduler is extracted** — extracting
   first would mean moving the Hazelcast maps, entry processors, and aggregators into
   the new service only to delete them in the next phase.

### Phase 1 — Gate and ordered dispatch (job-store-service)

- **DI-3018** Flyway migration (purely additive): add `is_termination`, `gate_open` columns to
  `dependencytracking`; add `data_chunks_delivered`, `data_chunks_expected` counters to
  the `job` table; add the delivery ordering index
  `(sinkid, status, gate_open, priority DESC, jobid, chunkid)` and the partial barrier
  index `(sinkid, submitter, jobid) WHERE is_termination`
- **DI-3019** Implement the per-job gate logic in `chunkDeliveringDone` (runs alongside the
  existing `waitingOn` mechanism — redundant but harmless). Includes a second Flyway migration
  adding `job.termination_barrier_lifted`, since the cross-job barrier cannot answer from row
  presence while the MapStore owns row lifecycle. Job-store writes `is_termination` and
  `gate_open` in synchronous SQL and performs the termination row's own `INSERT`, see [Who writes
  the gate columns](#barrier-chunks--per-job-gate). Takes
  `pg_advisory_xact_lock` over `(sinkid, submitter)` at site B and in the removal plus re-trigger
  transaction
- **DI-3049** Change `DependencyTrackingService.remove(TrackingKey)` to return the removed
  `DependencyTracking`, so the caller learns whether *this* call
  performed the removal, and condition the `data_chunks_delivered` increment on it. Carry
  `is_termination` on `DependencyTracking` as well, so the caller that won the removal reads the
  branch off the entry it was handed rather than querying the row it has just removed. That is the
  shape Phase 9 arrives at anyway, once the row is deleted synchronously and a read after the
  removal would return nothing.
  It already computes this and discards it: `dependencyTracker.remove(key)` returns the
  previous value atomically per key and the method checks `removed == null`, but returns
  `void`, so no caller can learn it won the race. Without this the increment sits behind
  `chunkDeliveringDone`'s non-atomic `get`-then-`remove` check and can double-count,
  opening the gate before all data chunks are delivered, see [Barrier Chunks](
  #barrier-chunks--per-job-gate)
- Increment `data_chunks_delivered` with a single atomic statement, never a read followed
  by a write: a lost update leaves the counter permanently below `data_chunks_expected` and
  the gate never opens
- Increment unconditionally, and write `data_chunks_expected` in the transaction that
  inserts the termination chunk, passing the count down into
  `createJobTerminationChunkEntity` rather than writing it in `markJobAsPartitioned`.
  Evaluate the gate at that insert as well as in `chunkDeliveringDone`. All three follow
  from partitioning and delivery overlapping, see [Barrier Chunks](
  #barrier-chunks--per-job-gate)
- Add `REQUIRES_FULL_WIDTH_BARRIER` (`TICKLE` only) and gate the data chunks of a later
  same-submitter job on the same sink behind an earlier undelivered termination chunk for
  those sink types, widen the re-trigger to open them, and re-trigger on every removal of
  a termination row including the abort path. Inert until Phase 9, because `waitingOn`
  still enforces full width for every sink type until then, but required before it. No
  schema change, the `gate_open` column and both indexes from DI-3018 already carry it.
  See [Barrier Width](#barrier-width--per-sink-type-job-isolation)
- **DI-3076** Extend the hourly `recheckBlocks` sweep to open a gate closed with no earlier
  *unlifted termination barrier*, requiring for a termination chunk that its own job's data chunks
  are delivered, derived from state that survives a rollback rather than from
  `data_chunks_delivered`, and lift the barrier of a job left with one and no `is_termination` row.
  Lands as one change together with the width toggle, site C, the widened re-trigger and the abort
  path above: the sweep is what keeps widening the gate from stranding jobs, so splitting the two
  would open that gap deliberately. See [Barrier Width](#barrier-width--per-sink-type-job-isolation)
- **DI-3020** Update the bulk-scheduler ordering query to
  `(priority DESC, jobid ASC, chunkid ASC)` with the `gate_open` filter, and filter on `gate_open`
  in the direct dispatch path as well, parking a held-back chunk in `SCHEDULED_FOR_DELIVERY` rather
  than leaving it in `READY_FOR_DELIVERY`. The query has to be issued
  as SQL against PostgreSQL: the bulk scheduler picks candidates through a Hazelcast
  `PagingPredicate` today (`DependencyTrackingService.find`), which cannot see the gate columns at
  all. Because the table's `status` is written write-behind, the sweep re-checks each candidate
  against the map and over-fetches by the `QUEUED_FOR_DELIVERY` cap; both end at Phase 9, see
  [Delivery Ordering](#delivery-ordering). Adds no Hazelcast code, and removes one use of the map
  from the dispatch path
- Dependency tracking, `BLOCKED` and sequence analysis remain fully active
- Accepted for the duration: the cross-job submitter barrier cannot see termination chunks
  that predate the migration, since they carry `is_termination = FALSE` from the column
  default. A later same-submitter job's gate can therefore open while an older termination
  chunk is still undelivered. Ordering for those chunks is still held by `waitingOn` and
  their `barrierMatchKey` until Phase 9, so the gate being wrong here changes no behaviour.
  Backfilling the flag was considered and rejected: the gate ignores pre-migration jobs
  either way, and it would turn a purely additive migration into one that rewrites live
  scheduling rows
- `SINK_STATUS` remains a distributed Hazelcast map: it is mutated by callback
  handling on any job-store instance (see [Counter Management](#counter-management));
  it moves to a JVM map at scheduler extraction (Phase 11)

### Phase 2 — Add `correlationKey` to `RecordInfo`

- Add `correlationKey` field to `RecordInfo`; derivation logic on `MarcRecordInfo`
  (record type alone: standalone → own id, head/section/volume → the shared hierarchy
  constant — no section-to-head resolution, no partitioner changes)
- Add priority-override logic for chunks containing live head/section records
- Follow-up (separate story): construct the termination marker item with a `null` id
  (today it wrongly gets a correlationKey via the literal id `"End Item"`) — the
  watermark half of this follows automatically once `JMSHeader.recordKey` (Phase 6/7)
  is in place, no separate sink-side change needed — see [Open Questions](#open-questions) §1

### Phase 3 — Watermark table and endpoints (job-store-service)

- Flyway migration: create `sink_record_delivery_watermark`
- Add `GET /sinks/{sinkId}/watermarks?recordKey={recordKey}` endpoint
- Add `POST /jobs/{jobId}/chunks/{chunkId}/items/{itemId}/delivered` endpoint
  (same path as the existing `GET` that reads `ItemEntity.deliveringOutcome` back)
- Add watermark upsert to `reportItemResult` write path
- Integration tests against real PostgreSQL

### Phase 4 — Connector (job-store-service-connector)

- `Watermark` record, `WatermarkServiceConnector` interface and HTTP implementation
- Unit tests with WireMock

### Phase 5 — Broker configuration

- Add one `<address-setting match="sink">` carrying `default-group-rebalance`,
  `default-group-rebalance-pause-dispatch`, `default-group-first-key` and
  `default-group-buckets=1048576`. All sink queues share the address `sink`, so no
  wildcard is needed
- Drop and re-create the existing sink queues, drained, so the defaults take effect. This
  is **not** a configuration-only change, see
  [Artemis Broker Configuration](#artemis-broker-configuration)
- Align the Artemis image used by integration tests with the version staging and
  production run, so tests exercise the same broker and the same config
- Deploy and verify before Phase 6. Verification has to read the four attributes back per
  queue and exercise `JMSXGroupID` serialisation against a real broker, because a correct
  config file is not evidence that the running queues carry the settings

### Phase 6 — Per-item dispatch (`SinkMessageProducerBean`)

- Add `itemId` and `recordKey` constants to `JMSHeader` (`commons/types`) - done under
  DI-3000, ahead of the rest of this phase
- Add the `ITEM_PAYLOAD_TYPE` constant to `JMSHeader` and set it as `payload` on every
  item message, so an item body is never handed to `unmarshallPayload`'s `Chunk` parse
- Send the items of a chunk in ascending `itemId` order, through a single `JMSContext`
  (see [Items of a chunk are sent in ascending `itemId` order](
  #items-of-a-chunk-are-sent-in-ascending-itemid-order))
- Carry every routing and validation header from the chunk message onto each item
  message (`payload`, `jobId`, `chunkId`, `trackingId`, `sinkId`, `sinkVersion`, and
  `flowBinderId`/`flowBinderVersion` when the job has a flow-binder reference) - see
  [Headers on an item message](#headers-on-an-item-message) for what each one breaks
  when it is missing
- Iterate items; set `JMSHeader.recordKey` from `<agencyId>:RecordInfo.getId()`
  (the job's `job.getSpecification().getSubmitterId()`, not `RecordInfo.getId()` alone —
  see "Why `recordKey` is carried on the message" under Per-Item Dispatch) and
  `JMSXGroupID` from `RecordInfo.getCorrelationKey()`; send one message per item
- Requires Phase 5 broker config to be live
- Coexists with chunk-level dependency tracking: the graph still decides *when* a
  chunk is dispatched; this phase only changes *how* (N item messages instead of one
  chunk message)

### Phase 7 — Sink framework (`commons/artemis-jse-app`)

- New `SinkMessageConsumerAdapter extends MessageConsumerAdapter`: watermark check +
  result reporting before and after `deliverItem`, `handleConsumedMessage` final. The base
  class is left alone for the two consumers that stay on whole chunks (see
  [Sink framework](#sink-framework-sinkmessageconsumeradapter))
- The watermark key read once in `SinkMessageConsumerAdapter.handleConsumedMessage` from
  `JMSHeader.recordKey` — not abstract, not per-sink
- Abstract `deliverItem(ConsumedMessage, ChunkItem)` for subclasses, returning an
  `ItemDeliveryResult`
- `usesDeliveryWatermark()` for the sinks that opt out of watermark filtering (see
  [Watermark opt-out](#watermark-opt-out))
- `Watermark implements Comparable<Watermark>`, and `ItemDeliveryResult.of` /
  `withWatermarkKey` to split what the sink decides from what the framework fills in
- `dlq-errorhandler`: fail dead item messages per item, so a job whose item the broker
  gave up on can still complete (see [Dead item messages](#dead-item-messages))
- No `ServiceHub` change: the watermark calls live on the `JobStoreServiceConnector` it
  already carries

### Phase 8+ — Individual sink migrations (one PR per sink)

- Change `extends MessageConsumerAdapter` to `extends SinkMessageConsumerAdapter` and
  implement `deliverItem(ConsumedMessage, ChunkItem)` per sink
- Remove old chunk-aggregation logic, including each sink's `handleConsumedMessage`
- Translate each sink's "pass a failed or ignored processing outcome through as ignored"
  branch into an `IGNORED` verdict, not a `DELIVERED` one with an `IGNORE` item, so the
  DELIVERING counters keep the meaning the chunk path gave them and the record's watermark
  is not advanced by an item that was never sent (see [The four verdicts](#the-four-verdicts)).
  The first migration, `dummy` under DI-3017, added the verdict and is the worked example
- `periodic-jobs` and `marcconv` additionally override `usesDeliveryWatermark()`
- Deployment is big-bang: job-store and every sink go live together against drained,
  re-created queues, so no sink needs to handle both payload types during a rollout
  window. Until the last of these merges, master builds but is not deployable

### Phase 9 — Remove dependency tracking (job-store-service)

Precondition: all sinks are live on the per-item + watermark protocol (Phase 8
complete) — see ordering constraint 1 above. Second precondition:
`REQUIRES_FULL_WIDTH_BARRIER` is live and verified, since `waitingOn` is what enforces
full barrier width until this phase deletes it, see [Barrier Width](
#barrier-width--per-sink-type-job-isolation).

Split into three PRs, since the whole phase is far past the 500 line guideline. See
`.claude/plans/DI-3021-remove-dependency-graph.md`.

**PR 1, remove the dependency graph and keep Hazelcast.** Done.

- Delete `BLOCKED` state from `ChunkSchedulingStatus`; migrate existing `BLOCKED`
  rows to `SCHEDULED_FOR_DELIVERY` (`V11`)
- Remove the `LAST_TRACKER` Hazelcast map, the `RemoveWaitingOn`, `AddTerminationWaitingOn` and
  `UpdatePriority` entry processors, and the `LastTrackerMap` and `BlockedCounter` aggregators
- Remove `findChunksToWaitFor`, `trackChunksToWaitFor`, `optimizeDependencies`,
  `boostPriorities`, `removeFromWaitingOn`, `addDependencies`, fan-out loop
- Remove `DependencyTracking.waitingOn`, `matchKeys` and `waitFor`, and the `WaitFor` type
- Flyway migration: drop the `waitingon` and `matchkeys` columns and the GIN index on `waitingon`
  (`matchkeys` never had one)
- The abort cascade goes with the graph, see [Aborting no longer cascades](#aborting-no-longer-cascades)

**PR 2, make `dependencytracking` the sole store.** Done.

- Remove the `DEPENDENCY_TRACKING` map and `DependencyTrackingStore`; row lifecycle and `status`
  become synchronous SQL in job-store, in a new `DependencyTrackingRepository`
- `setValidatedStatus` becomes one conditional `UPDATE` whose predicate on the target row decides
  whether the move happens, replacing the `UpdateStatus` entry processor. A `MATERIALIZED` CTE
  alongside it supplies the prior status for the counters and nothing else, and the decision
  predicate must never move into it
- The chunk's row is `DELETE`d in the same transaction as the `data_chunks_delivered` increment,
  which makes the returned row the once-only token DI-3049 introduced and closes the window where a
  failure after the removal left the count lost and the gate shut
- `DependencyTrackingService` becomes `@Lock(READ)`. Without it the conversion would turn a
  serialised sequence of microsecond map operations into a serialised sequence of database calls
- Row lifecycle moves to the site that owns the transaction boundary it needs: a data chunk's row is
  inserted by `JobGateBean.insertDataChunkRow` carrying its gate verdict, and the termination row by
  `JobGateRepository.insertTerminationRow`, which becomes the only insert of it
- `removeJobId` becomes `REQUIRES_NEW`, or `AdminBean.recheckBlocks` hangs undetectably on its own
  nested transactions
- `SINK_STATUS` stays a Hazelcast map, maintained from the new SQL write sites and rebuilt at
  startup, hourly from `recheckBlocks`, and on demand from `COUNT(*) GROUP BY sinkid, status`. It
  becomes a JVM map in DI-3024
- Ordered processing-phase candidate query and its index (`V12`), see
  [Processing Ordering](#processing-ordering). The same migration drops
  `dependencytracking_sinkid_status_index`, now a leading prefix of both ordered indexes
- `DeliveryDispatchStaleStatusIT` goes here rather than with PR 3: its subject is the MapStore's
  write-behind lag, which no longer exists

**PR 3, simplify both dispatch paths.** Done. The delivery candidate query is limited to the free
queue slots, `staleCandidateSlack` and `isStillAwaitingDelivery` are gone with the over-fetch they
compensated for, and `hasClosedGate` is gone with the second statement it cost: both direct-path gate
checks read `gate_open` off the `DependencyTracking` the path already holds.
- `gate_open` becomes a field on `DependencyTracking`, which the standing rule used to forbid. The
  rule's premise was that the object was a cached map value written behind a projection, so a copy of
  a column written by four sites could be stale. It is now a detached snapshot of a `SELECT`, read
  and used inside one transaction, exactly like `status`
- It also picks up one thing that is not part of this phase's criteria: a chunk stranded in
  `READY_FOR_PROCESSING` had no recovery. That status is held only between a chunk's row committing
  and the asynchronous dispatch attempt running, but an EJB asynchronous invocation is in-memory, so
  a crash in that window stranded the chunk. The bulk sweep takes only `SCHEDULED_FOR_PROCESSING` and
  the stale sweep covered every other status but this one. `READY_FOR_DELIVERY` has exactly this
  rescue, which is what marks the omission as an oversight rather than a decision. It predates the
  phase. `AdminBean.updateStaleChunks` now pushes a chunk stale in `READY_FOR_PROCESSING` for ten
  minutes to `SCHEDULED_FOR_PROCESSING`, with the same validated status change the delivery side
  uses. Ten rather than the delivery side's five: that window covers a round trip to a sink, whereas
  this one is milliseconds in health and is sized instead to sit above the asynchronous-call backlog
  a large partitioning burst produces, since a sweep firing into that backlog hands the same chunks
  to the bulk submitter
- The gate needs no change here. `dependencytracking` becomes job-store's outright, so the
  split ownership described under [Who writes the gate columns](
  #barrier-chunks--per-job-gate) collapses and the `do update set` constraint on
  `is_termination` and `gate_open` lapses with the MapStore. Site B's `INSERT` becomes the only
  insert rather than a race-free duplicate of one. `job.termination_barrier_lifted` stays:
  row presence becomes trustworthy, but the flag keeps the barrier independent of deletion and
  purge timing, and the `recheckBlocks` gate sweep reads it after the chunk row is gone
- Hazelcast itself stays for now: the `isMaster()`/`isSlave()` guards, `ABORTED_JOBS`,
  and `executeOnMaster` partitioning routing are still in use until Phase 11

### Phase 10 — Sequence analysis removal (job-store-service)

Precondition: Phase 9, which removed the graph that read the keys. Nothing has read them
since, so this phase changes no delivery ordering. One PR, planned in
`.claude/plans/DI-3022-remove-sequence-analysis.md`.

- **DI-3022** Remove the eleven artefacts listed under [Sequence Analysis Removal](
  #sequence-analysis-removal): the key set, its converter, the key generator interface and
  its implementation, the option on `SinkContent`, `RecordInfo.getKeys` and the plumbing
  between them
- Flyway migration `V13` drops `chunk.sequenceanalysisdata`
- Accepted for the duration of one deploy: the column is `NOT NULL` and the previous build
  names it in every `INSERT INTO chunk`, so an instance still partitioning when `V13` runs
  fails its next chunk transaction. Nothing is lost, since
  `BootstrapBean.resetJobsInterruptedDuringPartitioning` returns the job queue entry to
  `WAITING` and partitioning resumes from `job.numberofchunks`, but that reset runs at
  instance startup rather than on a timer, so the deploy belongs at a quiet partitioning
  moment. Splitting the drop across two releases, `V13` relaxing `NOT NULL` and a later
  migration dropping the column, was considered and rejected: it closes a one-window
  exposure at the price of leaving a dead column in the schema across a release boundary,
  and `V11` took the same window for `waitingon` and `matchkeys`
- Flow-store keeps `sequenceAnalysisOption` in its stored sink definitions, see
  [Sequence Analysis Removal](#sequence-analysis-removal)

### Phase 11 — Extract scheduler-service, drop Hazelcast

- Create new `scheduler-service` Maven module (Payara Micro, same tech stack)
- Move `JobSchedulerBean`, `JobSchedulerTransactionsBean`, `JobSchedulerBulkSubmitterBean`,
  `AdminBean` (scheduling parts), and `SinkMessageProducerBean` into the new service
- Scheduler-service connects to the same PostgreSQL instance as job-store-service
- `JobGateBean` and `JobGateRepository` stay in job-store-service, held there by the gate's
  evaluation site inside `createJobTerminationChunkEntity`. Re-anchor
  `JobGateBean.advanceGateState` from `JobSchedulerBean.chunkDeliveringDone` to `JobsBean`'s
  delivery callback path if it has not already moved in an earlier phase, so the gate does not
  travel with the scheduler. See [Where the per-job gate lands](
  #scheduler-as-a-standalone-service)
- Job-store-service inserts the `dependencytracking` row for every chunk, in the partitioning
  transaction that creates the chunk, and scheduler-service picks it up on its next poll. Together
  with the `DELETE` below that gives job-store the row lifecycle and scheduler-service `status`
  and dispatch, which is the inverse of the split that held while the table was a projection, see
  [Who writes the gate columns](#barrier-chunks--per-job-gate)
- Split `JobSchedulerBean.scheduleChunk` rather than moving it. It is called synchronously per
  chunk from `partitionJobIntoChunksAndItems`, which stays in job-store-service, so moving it
  whole would need exactly the synchronous call into the scheduler that the interaction model
  rules out. By this phase the method is down to building the row and one
  `submitToProcessingIfPossibleAsync`: the row insert stays in job-store-service, the dispatch
  attempt is dropped in favour of the poll loop, which is the direct-mode latency already accepted
  above. Site C's gate verdict travels with the insert, since it is the same statement
- Delete the rank guard on both direct dispatch paths along with the paths themselves. It exists
  only because two things dispatch, so with one dispatcher running one ordered query the ordering
  holds by construction. Whatever wakes that dispatcher, a shorter poll interval or `LISTEN/NOTIFY`,
  must not reintroduce a path that sends without consulting the ordered query
- Leave the delivery-side `DELETE` of the chunk's `dependencytracking` row in job-store-service,
  in the same transaction as the `data_chunks_delivered` increment it is the once-only token for
- Leave the `recheckBlocks` gate sweep in job-store-service when `AdminBean`'s scheduling parts
  move, so scheduler-service holds no write of `gate_open` at all
- Replace `@Schedule` EJB timers with a poll loop; delete the `isMaster()`/`isSlave()`
  scheduling guards (single instance, no guard needed)
- Replace `SINK_STATUS` Hazelcast counters with a JVM
  `ConcurrentHashMap<Integer, AtomicInteger>` in scheduler-service, rebuilt on
  startup from `COUNT(*) GROUP BY sinkid, status` (safe only now — single instance)
- Resolve remaining Hazelcast usages (see [Hazelcast fate](#scheduler-as-a-standalone-service)):
  `ABORTED_JOBS` → `job` table column; `executeOnMaster` partitioning routing → run on
  the receiving instance; `ScheduledJobPurgeBean` → move to scheduler-service
- Drop the Hazelcast dependency from job-store-service; scheduler-service never adds it
- Integration test: scheduler-service starts, picks up a chunk from DB, dispatches to JMS

---

## Correctness Summary

| Scenario | Current | New |
|---|---|---|
| Same record, concurrent threads, single pod | BLOCKED: one chunk in-flight per record | Broker serialises same-correlationKey items to one thread; no concurrent access |
| Same record, multiple pods, any submitter | BLOCKED: guaranteed pre-delivery ordering | `JMSXGroupID = correlationKey` — serial delivery across all pods |
| Same record, priority inversion | BLOCKED guarantees serial delivery | Broker delivers higher-priority item first; watermark check supersedes stale item |
| Termination chunk dispatched before all data chunks delivered | Impossible (BLOCKED) | Per-job counter gate: termination held until all `chunkDeliveringDone()` fired |
| Cross-job termination ordering (same submitter, same sink) | Termination BLOCKED on prior-job termination | Gate checks no earlier same-submitter termination pending |
| Cross-job data ordering (same submitter, same sink) | All of job B BLOCKED on job A's termination | Not held, except for sink types in `REQUIRES_FULL_WIDTH_BARRIER` |
| Overlapping `TOTAL` batches in one tickle dataset | Impossible: full-width barrier holds job B's data chunks | `REQUIRES_FULL_WIDTH_BARRIER` gates job B's data chunks on job A's termination chunk |
| Exact retransmit (stale recovery) | `addChunkIgnoreDuplicates` | `incoming == watermark` → always deliver (idempotent re-delivery) |
| Pod crash, stale watermark after rebalance | Hazelcast MapStore reloads from PostgreSQL | No local cache to become stale; `group-rebalance-pause-dispatch` ensures watermark is current before any post-rebalance dispatch |
| Live head/section before volume delivery | Dependency tracking + barrier | Constant hierarchy group serialises all hierarchy records; priority override ensures head chunk dispatched first |
| Lower-ranked chunk arrives while higher-ranked chunks are parked | Direct dispatch takes any free slot, so a partitioning burst streams past the backlog | Both direct paths read the head of the parked queue and park themselves when it outranks them |
| Job completion fires exactly once, incl. jobs with a termination chunk | One `addChunk` call per chunk; `chunkCompletesJob` | One `addItemDelivered` call per item under the job row lock; `itemCompletesJob` selects the last data item, or the termination item when `numberOfItems == PARTITIONING total + 1` |

---

## Open Questions

1. **Barrier records (`correlationKey` and watermark key)** — resolved, two independent
   gaps closed by two independent changes:

   **(a) JMSXGroupID / broker serialisation.** The termination/barrier marker item is
   currently constructed as a plain `RecordInfo("End Item")`
   (`PgJobStoreRepository.java:400`) — the sole construction site for this sentinel.
   `RecordInfo.getCorrelationKey()` defaults to `id`, which would wrongly give every
   termination item across every job the same literal correlation key (`"End Item"`),
   serialising all of them into one broker group instead of leaving them unkeyed.

   Two ways to fix this were considered:
   - Introduce a `BarrierRecordInfo extends RecordInfo` sibling class overriding
     `getCorrelationKey()` to return `null`. **Rejected**: `RecordInfo` is polymorphically
     typed (`@JsonTypeInfo(use = Id.CLASS, ...)`), persisted per item as `jsonb` via
     `RecordInfoConverter`, and exposed over REST via `ItemInfoSnapshot.getRecordInfo()`.
     A new subclass changes the `@class` discriminator written into both the DB column
     and the REST payload; an instance still running the older `types` jar that doesn't
     know the new class name throws deserializing it, and rows/payloads written after
     the new class is in use become unreadable again on rollback. Not something to
     introduce without explicit deployment-ordering discipline — likely not worth it
     here.
   - Construct the termination item as `new RecordInfo(null)` instead of
     `new RecordInfo("End Item")`. **Preferred**: `getCorrelationKey()` already returns
     `id`, so a null `id` already yields `correlationKey == null` — "Barrier → no key" —
     with no new class, no discriminator change, no migration. Trade-off: loses the
     human-readable `"End Item"` label wherever this item's `RecordInfo.getId()` is
     surfaced today (e.g. `JobExporter`/`JobRerunnerBean` record-id exports,
     `ItemInfoSnapshot` over REST) — confirm nothing downstream assumes a non-null id
     for that row before making the change.

   **(b) Watermark.** The watermark key is `<agencyId>:RecordInfo.id`, not
   `correlationKey` (see "JMSXGroupID vs. Watermark Key" above) — the two are
   deliberately different so that head/section/volume records can share one
   correlationKey without sharing a watermark row — so fixing (a) alone does not fix
   this half. This half is resolved by the `JMSHeader.recordKey` message property
   introduced in [Per-Item Dispatch](#per-item-dispatch): it is set from
   `<agencyId>:RecordInfo.getId()`, so once (a)'s change lands (`id == null` for the
   termination item), the whole composed key is skipped and `getRecordKey(Message)` —
   now a single
   framework method on `MessageConsumerAdapter`, not a per-sink implementation, see
   [Sink framework](#sink-framework-sinkmessageconsumeradapter) — reads no property, returns
   `null`, and the framework skips the watermark check entirely for that item. No
   per-sink logic needs to "recognise" a termination message; every sink gets this for
   free from the same message-property mechanism that also fixes the record-key
   whitespace-normalisation bug. Before this fix landed, every job's termination item
   shared the literal watermark key `"End Item"`, so delivering job 1000's termination
   item would have made job 999's look stale and get it wrongly reported `SUPERSEDED` —
   not a theoretical risk, since `PeriodicJobs*FinalizerBean` and marcconv's
   `ConversionFinalizer` do real delivery work on job end.

   Both halves are a separate follow-up from the base `correlationKey` field/derivation
   work (`RecordInfo`/`MarcRecordInfo` standalone/head/section/volume rules) — (a) is a
   `job-store-service` construction-site change, (b) rides along with the Phase 6/7
   `JMSHeader.recordKey` work once it lands.

2. **Watermark table growth** — resolved, see [Retention Policy](#retention-policy):
   `last_modified` on `sink_record_delivery_watermark`, a nightly `WatermarkPurgeBean`
   purge, and a configurable `WATERMARK_RETENTION` window (default `P90D`).

3. **Should FAILED advance the watermark?** — resolved for DI-2997 (Phase 3's write
   endpoint): **no, only `DELIVERED` does**, matching the "Upsert on delivery"
   pseudocode above (`If status == DELIVERED AND recordKey != null`) as-is. The
   priority-inversion case this leaves unresolved — a newer version is dispatched
   first, fails at the target, and an older version behind it then passes the
   watermark check and is delivered, regressing the target to old data with no newer
   write coming — is accepted as a known limitation rather than solved here. Advancing
   the watermark on `FAILED` (recording the latest *attempted* version) would suppress
   the older delivery instead, leaving the target unchanged and the failure visible in
   the job state, and remains a defensible alternative if the priority-inversion case
   is later observed to matter in practice — just not adopted now.
