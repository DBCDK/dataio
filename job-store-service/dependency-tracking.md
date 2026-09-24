# Dependency Tracking

Dependency tracking is the mechanism that prevents chunks from being delivered to a sink out of
sequence, and prevents duplicate or orphaned processing when multiple service instances are running. 
The authoritative state is the `dependencytracking` table in PostgreSQL. Every row is written
synchronously, in the transaction that decides its contents, so a row read back is what the last
committed writer put there.

Two mechanisms do the ordering, and between them they are the whole of it: the **per-job gate**
orders whole jobs, and the **delivery watermark** orders versions of one record.

The decisions behind this mechanism, and the alternatives weighed against each of them, are
recorded as architecture decision records in `docs/adr`.

## Terminology

Two different things get ordered here, and they have separate vocabularies and separate mechanisms.
**Whole jobs** are ordered against each other, so that one job's end-of-job work does not run while
another job's data is still arriving. **Versions of a single record** are ordered against each other,
so that an older version does not overwrite a newer one at the sink.

Ordering between jobs. Three of these words are easy to conflate, and two of them face opposite
ways. Two more, scope and width, say where the barrier applies and how much of a later job it holds,
and the last is the mechanism that lets a held chunk go:

- **Termination chunk** - the object. A synthetic chunk appended to a job for the sink types that
  require job-level ordering (`createJobTerminationChunkEntity`, `REQUIRES_TERMINATION_CHUNK`).
- **Barrier** - what a termination chunk imposes on *other* chunks: nothing from the same submitter
  and sink may pass until it has been delivered. Enforced by the gate, see
  [Barrier chunks](#barrier-chunks) below.
- **Barrier scope** - the `(sinkid, submitter)` pair a barrier applies within, and the unit almost
  everything about the barrier is expressed in. Jobs are ordered against each other only when they
  agree on both, so an earlier job on the same sink for a different submitter is not a blocker and
  neither is the same submitter on another sink. It is also the unit of serialization: every decision
  about whether a gate opens is taken under `pg_advisory_xact_lock` over that pair, which is what
  keeps a barrier read and a barrier lift from missing each other. The scope is not arbitrary. It is
  the one the jobqueue already partitions in, strictly one job at a time in job id order
  (`NQ_FIND_BY_SINK_AND_AVAILABLE_SUBMITTER`), and that shared choice is what lets a data chunk's
  gate be decided once when the chunk is inserted.
- **Gate** - what is imposed on a chunk *held back by* a barrier, as opposed to the barrier it may
  itself impose on others. A termination chunk is gated until its own job's data chunks are
  acknowledged and no earlier job in the same scope holds an unlifted barrier. On a full-width sink
  type a *data* chunk is gated too, on the second of those conditions alone. Carried by
  `dependencytracking.gate_open`, see [The per-job gate](#the-per-job-gate) below.
- **Barrier width** - how much of a later job the barrier holds back, either every chunk of it or
  only its termination chunk. See [Barrier width per sink type](#barrier-width-per-sink-type).
- **Re-trigger** - the pass that re-evaluates the gates of *later* jobs in the scope, run whenever a
  job's barrier is lifted (`JobGateBean.liftBarrierAndRetrigger`). It is what a held chunk depends on
  to ever be released: a chunk gated while an earlier barrier stood has already had its own
  evaluation run and decline, and nothing else would look at it again. The re-trigger is
  **edge-triggered**, firing once at the instant the barrier is lifted, and two things follow from
  that. Every path that removes a termination chunk has to lift the barrier, delivery being only the
  usual one, and a stale read here cancels an open rather than delaying it, because there is no
  second edge to come. It releases both kinds of held chunk: the termination chunks of later jobs one
  at a time, since each additionally needs its own job's data chunks accounted for, and their data
  chunks in a single statement, since for those the earlier barrier is the whole condition. It stops
  by itself, because opening a gate does not lift the barrier that chunk's own job imposes, so only
  the next eligible job is released and the rest stay behind it.

So a termination chunk both imposes a barrier on others and is held by a gate of its own, and the two
point in opposite directions. That is why the flag is `gate_open` and not `barrier_open`: it says
this chunk may now be dispatched, not that the barrier it imposes on others has been lifted. Every
gated chunk has a gate; only a termination chunk raises a barrier. For the same reason the flag recording that a
barrier is gone is `termination_barrier_lifted` and not `..._delivered`: delivery is its usual cause
but not its only one, since aborting a job lifts the barrier of a termination chunk that was never
delivered.

Ordering between versions of one record. The last two are the pair to be careful with, since they
look interchangeable and are not:

- **Watermark** - the newest position already delivered for one record at one sink, held in
  `sink_record_delivery_watermark`. A sink reads it before delivering and skips anything it has
  already passed. See [Delivery watermark](#delivery-watermark) below.
- **`recordKey`** - what a watermark row is keyed by, `<agencyId>:<record id>`, agency qualified
  because a record id is only unique within the agency that assigned it.
- **Correlation key** - what the broker groups on, carried in `JMSXGroupID`, and deliberately *not*
  agency qualified. Using one where the other belongs is silent: too coarse a correlation key only
  costs latency, but too coarse a `recordKey` skips an unrelated record's delivery as superseded.

Both are derived per item from that item's `RecordInfo`, the correlation key by
`RecordInfo.getCorrelationKey()` and the `recordKey` by `SinkMessageProducerBean` composing the
agency onto `RecordInfo.getId()`. `getCorrelationKey()` is the only key job-store derives from a
record: no key is computed per chunk, and nothing in the scheduler holds a set of them.

## The per-job gate

The gate answers one question per chunk: may this chunk be delivered yet? It is a single boolean,
`gate_open`, on the chunk's `dependencytracking` row, and both dispatch paths refuse to send a chunk
whose gate is closed. A held chunk waits in `SCHEDULED_FOR_DELIVERY`, which has no capacity cap.

**Which chunks are gated depends on the sink type.** A termination chunk is always gated. A data
chunk is gated only on the sink types that need the full barrier width, currently tickle alone, and
on every other sink type a data chunk is dispatchable as soon as it has been processed. See
[Barrier width per sink type](#barrier-width-per-sink-type).

**Two conditions decide a gate, and which apply depends on the chunk.** A termination chunk needs
both: every data chunk of its own job acknowledged, counted with `job.data_chunks_delivered` against
`job.data_chunks_expected`, and no earlier job from the same submitter on the same sink still holding
a barrier. A data chunk needs only the second, since it has no count of its own to wait for. That
second condition is read from `job.termination_barrier_lifted` on the earlier jobs, a nullable flag
whose three values matter: `NULL` for the majority of jobs that never had a termination chunk,
`FALSE` while the barrier holds, `TRUE` once it is lifted. Query it as `IS FALSE`, never as
`NOT ...`.

It is called a *per-job* gate because the verdict is a property of the job rather than of the
individual chunk: every data chunk of a queued job gets the same answer, and the facts it is decided
from all live on `job`. The column has to sit on the chunk row because that is what the dispatch
query filters on.

**A gate that closes and is never reopened is a job that never completes.** That single fact explains
most of the design below: every path that removes a termination chunk has to lift its barrier, and an
hourly sweep exists to catch the paths that somehow did not.

### Who writes the table

`dependencytracking` is job-store's outright. One writer, `DependencyTrackingRepository`, whose
every statement runs in its caller's transaction on its caller's connection. No column is off limits
to any statement, and a committed write is immediately visible to every reader.

**An unwritten gate is an open gate.** `gate_open` is `NOT NULL DEFAULT TRUE`, and only a write that
means to close a gate touches the column. That is what makes a data chunk on a non-full-width sink
correct without any gate write at all.

**The cross-job check asks `job.termination_barrier_lifted`, never row presence.** Delivery is not
the only thing that removes a chunk's row: `JobPurgeBean` compacts old jobs and the abort path drops
a job's rows wholesale, neither of them timed by anything the barrier controls. The flag keeps the
barrier independent of all of that, and it stays answerable once the chunk row is gone, which is what
the sweep needs. See [When a gate is left closed](#when-a-gate-is-left-closed).

### Where the gate is decided

| Site | When | Decides |
|---|---|---|
| A | a data chunk is delivered (`JobGateBean.advanceGateState`) | counts the delivery, then opens its job's termination chunk if that was the last one |
| B | the termination chunk is inserted (`createJobTerminationChunkEntity`) | that chunk's initial gate, and writes `data_chunks_expected` |
| C | a data chunk is inserted (`scheduleChunk`) | that chunk's gate, carried by the insert itself, and only for full-width sink types |
| re-trigger | a job's barrier is lifted (`JobGateBean.liftBarrierAndRetrigger`) | the gates of *later* jobs in the same scope |

A job's own gate needs both A and B because partitioning and delivery overlap: chunks are scheduled
from inside the partitioning loop, so a job's data chunks can all be delivered before its termination
chunk exists. Then no delivery is left to fire site A, and only the insert can open the gate. A job
with no data chunks at all is the same case.

Five things about those sites are easy to get wrong:

- **`data_chunks_expected` is written at site B, not by its caller.** The two run in separate
  transactions, so a value written by the caller is uncommitted and invisible to the verdict. Site B
  would read `0`, conclude the job was complete, and open the gate with every data chunk still
  outstanding. The count is passed down as a parameter instead. It is the chunk count taken *before*
  the termination chunk is created, since creating it bumps the total and a later reading is one too
  high for `data_chunks_delivered` to ever reach.
- **The delivery count is incremented unconditionally, in one statement.** Conditional on the job
  having a termination chunk, it would lose every chunk delivered before that chunk existed. As a
  read-then-write, it could lose an update and leave the counter permanently one short. Either way
  the gate never opens.
- **Site A runs once per chunk, and what makes that true is the `DELETE`.** `chunkDeliveringDone`
  is called again by every redelivery, and the broker's failure detection can produce two genuinely
  concurrent calls for one chunk, so the count hangs off the one thing only one caller can do:
  delete the row. `DELETE ... WHERE jobid = ? AND chunkid = ? AND status = QUEUED_FOR_DELIVERY
  RETURNING ...` returns a row to exactly one caller whatever the interleaving, because the second
  transaction waits on the row lock and then finds nothing to delete. A read followed by a delete
  would be a check-then-act two callers can both pass. Counted twice, the counter still lands on
  `data_chunks_expected` exactly, only while a data chunk is still in flight, so the failure would be
  a job that reads as complete rather than one that stalls, which is the worse of the two. See
  [Delivery acknowledgement](#delivery-acknowledgement).
- **`data_chunks_expected = 0` means two opposite things**: the migration default on jobs that
  predate the gate, which must be ignored, and a genuine job with no data chunks, whose gate must be
  decided at once. `is_termination` is what tells them apart, so the gate keys on that and uses the
  counter only as a total to compare against.
- **Site C has no delivery-side counterpart**, and the reason is outside the gate. The jobqueue
  partitions jobs from one submitter on one sink strictly one at a time in job id order
  (`NQ_FIND_BY_SINK_AND_AVAILABLE_SUBMITTER`), the same scope the barrier uses, so by the time this
  job's chunks are inserted the earlier job is fully partitioned and its barrier either stands or is
  already lifted. If that jobqueue guarantee is ever relaxed, site C needs a second site too.
- **Lifting a barrier is guarded on it actually standing.** `termination_barrier_lifted` is nullable
  with three meanings: `NULL` for the majority of jobs that never had a termination chunk, `FALSE`
  while the barrier holds, `TRUE` once lifted. The abort and recheck paths call the re-trigger for
  any job they remove, so an unguarded write would turn `NULL` into `TRUE` and lose the distinction.
  The guarded update's row count also says whether there was a barrier at all, so the rest of the
  work is skipped when there was not.

**Delivery is only the usual way a barrier is lifted.** `JobsBean.abortJob` and
`AdminBean.recheckBlocks` both drop a job's rows through `removeJobId`, and both must lift and
re-trigger. Skipping it leaves the job holding a barrier with no termination row left to fire a lift
on, and the re-trigger is edge-triggered, so nothing would ever look again and every later job from
that submitter stalls.

### Barrier width per sink type

Width is how much of a later job the barrier holds back.

The gate is **termination width** by default: only job B's job-end waits for job A's. For
ordering two versions of one record that is deliberate and sufficient, since `JMSXGroupID` and the
[delivery watermark](#delivery-watermark) already cover it. It is not sufficient when a sink's
job-end work depends on no later job's data having landed yet.

Tickle is that case, and it is the only one. A tickle total ingest expresses "everything the source
no longer sends is gone" by marking every record in the dataset when a job's first item arrives, and
deleting everything still marked when its job-end arrives. Neither step is scoped to the job, but a
batch belongs to one job. Under termination width, job B's first item would start a second pass over
the same dataset while job A's job-end was still waiting: B's mark undoes what A just wrote, A's
delete then removes everything B has not yet rewritten, and the dataset ends up silently short with
no error anywhere. Full width makes that overlap impossible, which is why nothing in the tickle sink
guards against it.

Marcconv and periodic-jobs finalize by job id and deliver one job's records, so termination width is
enough for them.

Width is therefore a property of the sink type. `JobSchedulerBean.REQUIRES_FULL_WIDTH_BARRIER` holds
the full-width types, currently `TICKLE` alone, and is a subset of `REQUIRES_TERMINATION_CHUNK`. It
lives in code rather than on `SinkContent` because it follows from what the sink implementation does
at job end, not from an operator choice, and because `SinkContent` is cached per job, so a new field
would need a defensible default for every cached sink already written. No column carries it: the sink
type is fixed per sink, and every gate statement is already scoped to one sink and submitter.

The set is read in exactly one place, site C. The re-trigger reopens data chunks for every sink type
rather than consulting it, which is identical in effect since only site C ever closes a data chunk's
gate, and it keeps the sink type off the delivery path.

Full width holds delivery only. Chunks are still processed and simply accumulate, so a large tickle
job queued behind another is no more expensive to hold than it is today. It does mean a closed gate
is job-sized rather than one synthetic chunk per job.

### Locking

All the sites serialise on one advisory lock, `pg_advisory_xact_lock` over `(sinkid, submitter)`,
which is the scope the jobqueue already serialises partitioning on. Every sink type with a
termination chunk takes it, not only the full-width ones.

It exists for a lost wakeup. Under READ COMMITTED one site can read "an earlier job still holds a
barrier" while another transaction is lifting exactly that barrier and running the re-trigger. The
reader still sees the old value and writes a closed gate; the re-trigger cannot see the reader's
uncommitted row. Both decline, and the gate is closed with nothing left to open it.

**Lock ordering: the job row first, then the barrier scope, then `dependencytracking` rows. No
transaction may wait for a job row while holding the advisory lock.** Site A cannot avoid taking the
job row first, because its increment locks that row until commit, so the others follow it. Hence the
re-trigger marks the barrier lifted *before* taking the advisory lock, and site B takes the lock only
after the job row lock it needs anyway. Both still decide under the lock, which is what matters. The
inversion this rules out is reachable, since a job's last data chunk and its own termination chunk
can be acknowledged at the same time.

**Site C is a boundary rather than an ordering, and this is the part most easily undone by
accident.** It is called from `scheduleChunk`, whose transaction is the one partitioning opened and
which therefore stays open for the whole job. Taking the advisory lock there would hold it for the
entire partitioning, and the insert of that job's own termination chunk runs in a nested
`REQUIRES_NEW` transaction, so it would ask for the same lock on a different connection and wait for
a transaction that cannot commit until the call returns. **PostgreSQL detects nothing, because the
wait is on an EJB call rather than a database lock.** It is an undetectable hang, not a deadlock, and
it would hit every job on a full-width sink.

`JobGateBean.insertDataChunkRow` is therefore `REQUIRES_NEW` and takes no job row lock at
all. It is also where the chunk's row is inserted, so the gate verdict and the row arrive in one
statement and there is never an instant where the row exists with a gate that should be closed. The lock is then held briefly, once per closed chunk, and the row is committed by the time
`scheduleChunk` returns rather than at the end of partitioning, so dispatch can see it throughout the
window it matters. `scheduleChunk` also reads the barrier once without the lock and only enters that
transaction when something is blocking. That shortcut is safe in one direction only, which is the one
it is used in: "nothing is blocking" cannot become "blocking", since an earlier job's barrier is
created while that job partitions and the jobqueue finished it first. "Blocking" can go stale the
other way, and that is exactly the answer that goes on to take the lock and read again.

**The delivery path takes a `dependencytracking` row lock before the job row**, because the `DELETE`
that acts as its once-only token runs first. Read against the ordering above that looks like an
inversion, and it is not, because the two row sets are disjoint. The deleted row is always
`gate_open = TRUE`: a chunk reaches `QUEUED_FOR_DELIVERY` only through a dispatch path that refuses a
closed gate. Every gate write taken under the advisory lock matches closed gates, filtering
`NOT gate_open`. So no transaction holding one wants the other.

`removeJobId` is the exception, and it is why that statement runs in its own transaction. It deletes
all of a job's rows whatever their gate, so its row set does overlap the sweep's. Left in the
caller's transaction it produces the undetectable hang rather than a deadlock:
`AdminBean.recheckBlocks` would hold those row locks while nesting `sweepScope`, which asks for the
same rows on a second connection and blocks, and the outer transaction cannot commit until the
nested call returns. `REQUIRES_NEW` releases the locks before anything nested asks for them. Aborting
a job holds its lift and its delete in one transaction on purpose, so there the worst case is a
detected deadlock against a concurrent sweep, and one of the two is rolled back and retried.

### When a gate is left closed

There is one way a gate outlives its reason to be shut, and it is a job that never completes: **a
termination row removed without its barrier being lifted.** `JobsBean.abortJob`,
`AdminBean.recheckBlocks` and `JobPurgeBean` all remove rows, and a lift that fails or is skipped on
any of those paths leaves the job reading as still blocking with nothing left to fire on.

A lost delivery count would be a second, and the transaction is what rules it out. The row's deletion
and the increment commit together, so either the chunk is counted and its row gone, or neither
happened and the row is still there for the sink's redelivery to count. See
[Delivery acknowledgement](#delivery-acknowledgement).

`AdminBean.recheckBlocks` sweeps hourly, which bounds the damage to one sweep interval. It lifts the barrier of any job left holding one with no termination row, then opens any
gate closed with no earlier unlifted barrier, requiring additionally for a termination chunk that its
own job's data chunks are delivered. Opened on the earlier barrier alone, a termination chunk would
be dispatched while its own data chunks were still in flight.

An hour is a long time to hold a job that cannot complete, so the same sweep is reachable on demand
at `POST dependency/gate_sweep`. It runs exactly what the hourly pass runs, in the same order, and is
safe to call at any time: it only ever opens a gate whose reason to be shut is already gone. Each
barrier scope is swept in its own transaction, so the advisory lock is held per scope rather than for
the whole sweep, which is what keeps an on-demand call from stalling delivery acknowledgements across
every scope it visits.

**The sweep reads that last condition from the absence of the job's data-chunk rows rather than from
`data_chunks_delivered`.** The rows are the more direct evidence, and the check costs one indexed
probe either way.

The sweep needs no knowledge of sink type, since only site C closes a data chunk's gate and it runs
only for full-width sinks, so the existence of such a row is the answer. It works one barrier scope
at a time under that scope's lock.

### The dispatch queries

Both phases take candidates from an ordered SQL query over `dependencytracking`, and both are served
by an index shaped to answer them without a sort.

**Delivery**, `DeliveryDispatchRepository.findDeliveryCandidates`:

```sql
SELECT jobid, chunkid, priority FROM dependencytracking
 WHERE sinkid = ? AND status = SCHEDULED_FOR_DELIVERY AND gate_open
 ORDER BY priority DESC, jobid, chunkid
 LIMIT ?
```

Backed by `dependencytracking_delivery_order_index`, `(sinkid, status, gate_open, priority desc,
jobid, chunkid)`. The first three are equality predicates and the rest is exactly the `ORDER BY`, so
rows come back in order from the scan itself and the `LIMIT` reads no further. `priority desc` has to
be in the index, since an all-ascending index yields neither the required order forwards nor
backwards.

**Processing**, the same shape without the gate:

```sql
SELECT jobid, chunkid, priority FROM dependencytracking
 WHERE sinkid = ? AND status = SCHEDULED_FOR_PROCESSING
 ORDER BY priority DESC, jobid, chunkid
 LIMIT ?
```

Backed by `dependencytracking_processing_order_index`, `(sinkid, status, priority desc, jobid,
chunkid)`. **The delivery index cannot serve it**: `gate_open` is its third key column, and with no
equality predicate on that column the ordered tail no longer follows the equality columns, so the
planner sorts.

**The gate predicate is absent from the processing query on purpose, and the two must not share a
statement.** A gate holds back delivery and nothing else. Under full barrier width a queued job's
data chunks sit at `gate_open = FALSE` while still needing to be processed normally, so a gate filter
here would stop the processing of exactly the chunks the barrier assumes get processed.

Each query is limited to the free slots in the sink's queue, and a candidate needs no further check
before it is dispatched. The table is the only source of `status`, so a candidate is a chunk that is
genuinely waiting, and the order it comes back in is the order to dispatch in.

**Both queries serve the direct paths too, at `LIMIT 1`.** A direct dispatch has no batch to order,
but it does have to place itself against the chunks already parked, so it reads the head of this
order and stands down when that head outranks it. Both queries return `priority` alongside the key,
because that comparison is on all three ordering keys. On the processing side it does double duty,
saving `submitToProcessing` a per-chunk read for the JMS priority. `DispatchOrder.outranks` is
the Java statement of the same three keys, and the two have to be changed together.

For both indexes: do not expect the planner to choose them at low row counts. It prefers a narrower
index and a sort until a sink has enough queued chunks for the sort to dominate, so verify with
`EXPLAIN` against a realistic backlog rather than a freshly seeded table. That is a small-set
artifact rather than a risk, since sorting a few thousand rows is a couple of milliseconds.

**The ordering is index-backed whatever the vacuum state, and that is the part that matters.** At a
realistic backlog the planner takes an ordered index scan either way and the `LIMIT` stops it early,
so the cost of a sweep is bounded by the queue's free slots rather than by the size of the backlog.
That is what these two indexes buy and it does not depend on vacuuming.

What vacuum state decides is whether the scan is *index-only*, and on this table it mostly will not
be. Measured on 300 000 rows queued for one sink, a `LIMIT 1000` costs 12 buffers with the table
fully all-visible, 674 after one percent of rows have been updated at random, and 1011 with a cold
visibility map. The decay is that steep because the table holds about 106 rows per heap page, so
updating a fraction `f` of rows at random clears the all-visible bit on `1 - (1-f)^106` of the pages,
which is 65 percent at `f = 0.01`. **No attainable
`autovacuum_vacuum_scale_factor` keeps these scans index-only**, since the trigger is a dead-tuple
count and by the time it fires most pages have lost the bit. The scale factor below is therefore
justified by dead tuples and bloat, as it says, and not by the visibility map.

So budget one heap buffer per candidate returned. At the `QUEUED_FOR_*` cap of 1000 that is roughly
1000 buffer hits and well under a millisecond, per sink per sweep.

## Delivery watermark

The gate orders whole jobs. The watermark is the other half of the picture: it stops an *older*
version of a single record from overwriting a newer one at the sink.

The problem it solves is that queue order is not delivery order. A sink runs several consumer
threads per pod across several pods, so one thread can pick up an item from job 50 while another
picks up an item for the same record from job 100, and either can reach the target first. No amount
of care about the order items are *sent* in fixes that.

So instead of ordering the sends, the sink checks before each delivery. `sink_record_delivery_watermark`
holds one row per `(sink_id, record_key)` naming the newest `(job_id, chunk_id, item_id)` already
delivered for that record. A sink reads the row, skips the item if the row already names an
equal-or-newer position, delivers otherwise, and reports the outcome. Job-store advances the row when
it records the delivery, and the upsert only moves it forward: the conflict clause compares the
incoming triple against the stored one and does nothing if it is not strictly greater. Two threads
racing on one record therefore converge on the newer of them whichever order they arrive in.

The state is shared rather than held in each sink, which is what makes it work across threads and
pods, and what lets a restarted sink pick up mid-job without replaying anything.

**Two different keys travel with each item, and confusing them causes silent damage.**
`recordKey` is the watermark key, composed in exactly one place, `SinkMessageProducerBean`, as
`<agencyId>:<RecordInfo id>`. It is composed rather than re-derived by each sink from the delivered
bytes because `RecordInfo` normalizes whitespace in the ID, so a sink deriving its own key would
produce one that never matches and stale-delivery detection would quietly stop working. It is agency
qualified because a record ID is only unique within the agency that assigned it. `JMSXGroupID`
carries the correlation key instead, which is what the broker serializes on, and is deliberately
*not* agency qualified: grouping two agencies' records that share a literal ID only costs a little
latency, whereas conflating them in a watermark key would skip an unrelated record's delivery as
superseded.

An item with no record, the job termination item above all, gets neither header. It is distributed
freely by the broker and ignored by the watermark, which is right: it is a per-job barrier, not a
bibliographic record. A sink that must see a whole job before delivering anything opts out entirely
via `usesDeliveryWatermark()`, and then neither reads nor advances a row, though it still reports a
result per item.

Rows are per record and never deleted on delivery, so they accumulate. `WatermarkPurgeBean` prunes
rows untouched for longer than `WATERMARK_RETENTION` (default 90 days) nightly.

**This is the only thing ordering versions of one record.** A sink that does not take part neither
reads nor advances a watermark row, so nothing protects the records it delivers from being written
out of order. Every sink receiving traffic must therefore implement the protocol, which
`SinkMessageConsumerAdapter` provides, or opt out through `usesDeliveryWatermark()` because it sees a
whole job before delivering anything.

## Hazelcast state

Two structures, and neither holds chunk state.

| Structure | Key | Value | Purpose |
|---|---|---|---|
| `SINK_STATUS` | sinkId | `Map<ChunkSchedulingStatus, Integer>` | Per-sink scheduling counters, read for the queue caps |
| `ABORTED_JOBS` | - | ISet of jobId | Jobs being aborted, checked on the scheduling path |

The counters are distributed because any job-store instance can handle the callback that changes
one. Each SQL write that changes a chunk's status applies its own delta through the
`UpdateCounter` entry processor, and the map is rebuilt at startup, hourly, and on demand at
`GET status/sinks/recount` from `SELECT sinkid, status, COUNT(*) ... GROUP BY sinkid, status`.

**The counters can drift, and two things keep that tolerable.** The delta is not part of the
transaction that changed the row, so the two can disagree. A transaction that does not commit has
its delta reversed from a transaction synchronisation, which covers the rollback case without
waiting for a rebuild. The delta is still applied as the statement returns rather than on commit,
because `capacity` is read by concurrent dispatchers in between and a delta they cannot see is a
queue slot handed out twice, so a rebuild whose read runs while another transaction is open still
replaces the counters with a census that does not include it.

**A counter that is wrong cannot hide work.** `getActiveSinks` answers from the counters, so it is
what the once-a-second dispatch sweeps ask, and a sink whose count says zero would otherwise stop
dispatching entirely until the next rebuild: nothing else reads a `SCHEDULED_*` chunk, and because
the direct paths stand down for the head of a sink's parked queue, every chunk partitioned
afterwards parks behind the one left behind. `JobSchedulerBulkSubmitterBean.sweepSinksWithParkedChunks`
therefore asks the table once a minute, through
`DependencyTrackingRepository.distinctSinkIdsWithStatus`, and dispatches for whatever it names.
That bounds the delay to one sweep whatever the counters hold. It runs once a minute rather than on
the dispatch tick because the query filters on status alone and neither ordered index leads with
it.

What the rebuild had to correct is counted in `dataio_sink_status_counter_drift` and the sink and
status of each is logged, since the repair is otherwise silent.

The counters feed the queue caps, which are backpressure rather than correctness, so drift costs
throughput until the next rebuild. They move to a plain JVM map in DI-3024, once the scheduler is a
single instance.

## The `DependencyTracking` record

A detached snapshot of one row, read by a `SELECT` and used within the reading transaction. It holds:

- **`key`** — `(jobId, chunkId)`, the unique identity
- **`sinkId`** — which sink this chunk is destined for
- **`submitter`** — the submitter ID (used for barrier scoping)
- **`status`** — current scheduling status (see lifecycle below)
- **`termination`** — whether this chunk is its job's termination chunk, from `is_termination`
- **`gateOpen`** — whether this chunk may be dispatched, from `gate_open`
- **`priority`**, **`lastModified`**, **`retries`**

`gateOpen` is here so the direct dispatch path can read the gate off the row it already fetches
instead of paying for a second statement. Every field is a snapshot: a setter on this object writes
to a local copy and changes no row, so the table remains the answer to every question about a
chunk.

## Chunk lifecycle

```
READY_FOR_PROCESSING
       │
       ▼  (bulk submitter picks it up)
SCHEDULED_FOR_PROCESSING
       │
       ▼  (enqueued to Artemis processor queue)
QUEUED_FOR_PROCESSING
       │
       ▼  (chunkProcessingDone)
READY_FOR_DELIVERY
       │
       ├──► SCHEDULED_FOR_DELIVERY   (queue full, or the chunk's gate is closed)
       │             │
       ▼             ▼
   QUEUED_FOR_DELIVERY
       │
       ▼  (chunkDeliveringDone deletes the row)
```

A chunk held back by its gate waits in `SCHEDULED_FOR_DELIVERY`, which is one reason that state is
uncapped. Status value 3 is unused, see `ChunkSchedulingStatus.from`.

`QUEUED_FOR_PROCESSING` and `QUEUED_FOR_DELIVERY` each have a cap of 1000 entries per sink, used for backpressure against the Artemis queues.

Both edges into `QUEUED_FOR_DELIVERY` are gated, and neither dispatches a chunk with
`gate_open = FALSE`:

- **bulk** — `JobSchedulerBean.bulkScheduleToDeliveringForSink` takes candidates from
  `DeliveryDispatchRepository.findDeliveryCandidates`, which filters on the gate in SQL, so every
  candidate it returns is dispatchable, see [The dispatch queries](#the-dispatch-queries).
- **direct** — `submitToDeliveringIfPossible` reads `gate_open` off the row it fetches for the
  capacity check and, when the gate is closed, parks the chunk in `SCHEDULED_FOR_DELIVERY` so the
  bulk sweep picks it up once the gate opens. `submitToDelivering` reads it again immediately before
  the status change, which is the choke point every path funnels through.

Both edges also hold to `(priority DESC, jobid ASC, chunkid ASC)`, and the direct one needs a guard
to do it. Capacity tells it the sink has room, not that this chunk is the one entitled to the room,
so it reads the head of the parked queue and parks itself when that head outranks it. The same guard
sits on the direct edge into `QUEUED_FOR_PROCESSING`.

Order is a correctness property on the delivery edge, not a fairness one. Every record of a MARC
hierarchy carries the same `correlationKey`, so the broker puts them in one group and delivers them
in the order they were sent, and the watermark cannot help because a head and its volumes are
different records with different keys. Dispatch order is the only thing deciding which reaches the
sink first.

## Barrier chunks

For sink types that require strict job-level ordering (MARCCONV, PERIODIC_JOBS, TICKLE), a synthetic
**termination chunk** is appended at the end of each job, carrying the job's end-of-job work to the
sink. It raises a barrier: no later job from the same submitter on the same sink may pass it, and how
much of that later job is held back depends on the sink type, see
[Barrier width per sink type](#barrier-width-per-sink-type).

The barrier is enforced entirely by the gate, and it holds back only what each sink type needs. It
orders whole jobs and nothing finer: for ordering two versions of one record see
[Delivery watermark](#delivery-watermark).

## Delivery acknowledgement

One statement does the work of the guard and the token together:

```sql
DELETE FROM dependencytracking
 WHERE jobid = ? AND chunkid = ? AND status = QUEUED_FOR_DELIVERY
RETURNING sinkid, submitter, priority, is_termination, status
```

1. A returned row means this caller is the one that acknowledged the chunk. Nothing returned means
   another caller already did, or the chunk was never in `QUEUED_FOR_DELIVERY`, and the call ends
   there. Which of the two it was costs one extra probe on that path, and only on that path.
2. The caller holding the returned row advances the per-job gate: a data chunk is counted, a
   termination chunk lifts its job's barrier and re-evaluates the jobs queued behind it, opening both
   the termination chunks and the data chunks of later jobs in the scope. See
   [The per-job gate](#the-per-job-gate).

**Both are in one transaction, and that is the point.** The delete is the once-only token for the
count, so a count that commits without its delete would be countable twice, and a delete that
commits without its count is the failure described under
[When a gate is left closed](#when-a-gate-is-left-closed). Sharing the transaction makes both
impossible: the sink's redelivery finds the row exactly when the count did not happen.

Nothing else follows, and there is no fan-out. No other chunk is waiting on this one, because nothing
records that one chunk waits on another. A chunk held by its gate is released by the re-trigger in
step 2 or by the sweep, both of which work from the gate columns.

## Multi-instance safety

Every job-store instance runs the same code against one database, so nothing here may depend on
being the only writer. Three mechanisms carry that, and each is a property of one SQL statement:

- **A conditional `UPDATE` for every status change.** `setValidatedStatus` is one statement,
  `UPDATE ... SET status = ? WHERE jobid = ? AND chunkid = ? AND status IN (<legal predecessors>)`,
  and whether it returns a row says whether this caller made the move. Under READ COMMITTED
  PostgreSQL re-evaluates that predicate against the newest row version when it finds the row
  concurrently updated, so two callers racing to advance one chunk produce exactly one success. **A
  read followed by a write is not sufficient**, and this is not a theoretical concern:
  `chunkProcessingDone` rejecting a chunk that has moved on, and `submitToDelivering` rejecting a
  chunk already queued, are both this check, and both are called concurrently from separate
  instances.

  The statement carries a `MATERIALIZED` CTE alongside it, and only to read the *prior* status for
  the sink counters, since `UPDATE ... RETURNING` returns post-update values. **The decision
  predicate must stay on the target row and never move into that CTE**: a CTE is evaluated once
  against the transaction's snapshot, so a predicate there would be frozen at the value both racers
  read and both would succeed. That is the check-then-act this bullet rules out, and it is the easy
  way to write this statement wrong. The predecessor sets are computed by inverting
  `ChunkSchedulingStatus.canChangeTo` rather than written out a second time.
- **A conditional `DELETE` for the acknowledgement**, which is the once-only token for the delivery
  count. See [Delivery acknowledgement](#delivery-acknowledgement).
- **The barrier scope's advisory lock** for everything that decides whether a gate opens. See
  [Locking](#locking).

The stale-chunk retry is the same shape. `AdminBean.resendIfNeeded` picks candidates from the stale
query, and the conditions that make the retry once-only, `retries < 1` and the status having a
successor at all, sit in the retry statement's own `WHERE` clause rather than in the caller.

- **Scheduled tasks run on one instance only.** Every `@Schedule` method on this path opens with
  `if (Hazelcast.isSlave()) return;`, so the recovery work below happens once per cluster rather than
  once per instance. Those guards are deleted when the scheduler becomes a single-instance service.
- **Recovery tasks** live in `AdminBean` (`rs` package):
  - `updateStaleChunks()` (every minute) re-drives chunks left behind by crashes or lost JMS messages. Entries stale in `READY_FOR_PROCESSING` for more than 10 minutes are pushed to `SCHEDULED_FOR_PROCESSING` and entries stale in `READY_FOR_DELIVERY` for more than 5 minutes to `SCHEDULED_FOR_DELIVERY`, both with a validated status change so a chunk that moved on between the query and the write is left alone; entries stale in `QUEUED_FOR_DELIVERY` beyond 1 hour, and in `QUEUED_FOR_PROCESSING` beyond `PROCESSOR_TIMEOUT` (default `PT1H`), are dealt with in two steps. A chunk whose phase has already finished, which the chunk row's own state answers, has its completion call run again rather than being resent: `chunkProcessingDone` or `chunkDeliveringDone`, both idempotent and neither sending anything outside job-store, so this is attempted on every sweep with no limit. Each runs in its own transaction through `JobSchedulerBean.advanceCompletedChunk`, so a chunk that throws is logged at error and costs only itself rather than rolling back the sweep that would otherwise never complete again. Only a chunk whose work really is outstanding is resent, up to `CHUNK_RESEND_LIMIT` times per phase (default `3`), the count being cleared as the chunk enters the delivery half so that what processing spent is not charged to delivery, which bounds the duplicate traffic a resend puts on a processor or a sink that is down. A retry sets `lastmodified`, so a chunk becomes stale again only once that is older than the phase's timeout and the retries are already a timeout apart. A chunk that has used them all is logged at error once and counted in `dataio_chunks_retries_exhausted`: no sweep will touch it again, so its job cannot complete without an operator retransmitting it. It also maintains the per-sink stale-chunk metric. Reachable on demand at `POST dependency/stale_sweep`, which runs this and `sweepSinksWithParkedChunks` together, since an operator with a stranded chunk wants both. Each sweep's dispatch is asynchronous and runs in its own transaction, so a chunk the call rescues is sent shortly after it returns rather than during it.

    A chunk that gets stuck is also reported where that happens. `chunkProcessingDone` and `chunkDeliveringDone` both re-read the row when their status change matches nothing, and tell a duplicate apart from a chunk left behind: a row that is gone or already past the phase is an acknowledgement of something the instance knew, and a row still waiting for the phase that has just completed is a chunk nothing will move on. The second is logged at warning with the status the row holds and counted in `dataio_stuck_chunks`, tagged `state=processing` or `state=delivering`.

    The two `READY_*` windows differ on purpose. A chunk holds either status for the length of one dispatch attempt and no longer, and the bulk submitters read only the `SCHEDULED_*` statuses, so this sweep is the only thing watching. Five minutes on the delivery side covers a real round trip to a sink. The processing side's attempt is an EJB asynchronous invocation fired as the chunk's row commits, so it is milliseconds in health, and its window is set by the opposite risk: a large partitioning burst queues those invocations, and a sweep firing while they drain hands the same chunks to the bulk submitter and leaves every queued invocation to find its chunk already claimed.
  - `recheckBlocks()` (hourly) drops the rows of jobs that are gone or already completed, lifting the barrier of each so the jobs queued behind it are released, and recounts the sink status map, counting what it had to correct in `dataio_sink_status_counter_drift`. It then sweeps the gate: it lifts the barrier of any job left with one and no `is_termination` row, and opens any gate closed with no earlier unlifted barrier, requiring for a termination chunk that its own job's data chunks are delivered.
  - `completeFinishedJobs()` (hourly) closes jobs whose work finished without the completion being recorded.

## Write volume

Every status change is its own `UPDATE`. A chunk passes through three of them on the direct path and
up to five when it parks in either `SCHEDULED_*` state, plus an insert and a delete.

**Each of those writes is a non-HOT update, and that is the part that costs.** `status` is a key
column of both ordered indexes, and updating an indexed column rules out a heap-only tuple, which
means a new index entry in *every* index on the table rather than only in the ones containing
`status`. The table therefore carries no index that is a leading prefix of another: `(sinkid,
status)` alone would be one, and every query it would serve is served by a prefix scan of an ordered
index instead.

The dead tuple rate rises by the same factor, and the table's live size is bounded by in-flight
chunks while its churn is not, so it carries a per-table `autovacuum_vacuum_scale_factor` well below
the 0.2 default. Fillfactor is not worth tuning here, since its benefit is to HOT updates and HOT is
ruled out.

**The stale-chunk query is the one read deliberately left unindexed.**
`AdminBean.updateStaleChunks` asks `WHERE status = ? AND lastmodified < ?` four times a minute, and
no index leads with `status`, so each is a sequential scan. That is the cheaper side of the trade
above: an index for it would be a fourth entry written on every status change. Scoping the query per
sink would let the ordered indexes serve it and is what to reach for if the scan ever shows up in
`pg_stat_statements`.

## Key files

| File | Role |
|---|---|
| `distributed-objects/src/main/java/.../DependencyTracking.java` | Snapshot of one row |
| `distributed-objects/src/main/java/.../ChunkSchedulingStatus.java` | Status enum with valid transitions and capacity limits |
| `war/src/main/java/.../ejb/DependencyTrackingRepository.java` | Every statement against the table: insert, status change, delete, the ordered processing query |
| `war/src/main/java/.../dependencytracking/DependencyTrackingService.java` | Facade over the repository, and the `SINK_STATUS` counters |
| `war/src/main/java/.../dependencytracking/Hazelcast.java` | Cluster membership helpers, and the two remaining structures |
| `war/src/main/java/.../ejb/JobSchedulerBean.java` | Primary caller; owns the scheduling logic |
| `war/src/main/java/.../ejb/JobGateBean.java` | Per-job gate: counts data chunks, lifts barriers, re-triggers later jobs, closes a data chunk's gate at schedule time, and owns the hourly gate sweep |
| `war/src/main/java/.../ejb/JobGateRepository.java` | The gate's synchronous SQL, and the barrier-scope advisory lock |
| `war/src/main/java/.../ejb/DeliveryDispatchRepository.java` | The ordered delivery candidate query |
| `war/src/main/java/.../ejb/SinkMessageProducerBean.java` | Composes the `recordKey` and `JMSXGroupID` headers each item carries |
| `war/src/main/java/.../rs/WatermarksBean.java` | The watermark read endpoint sinks call before each delivery |
| `war/src/main/java/.../ejb/WatermarkPurgeBean.java` | Nightly pruning of watermark rows past `WATERMARK_RETENTION` |
| `commons/artemis-jse-app/.../jms/SinkMessageConsumerAdapter.java` | Sink-side half of the watermark protocol, with the opt-out |
| `war/src/main/java/.../ejb/JobSchedulerBulkSubmitterBean.java` | Per-second bulk submission of `SCHEDULED_FOR_*` chunks to the JMS queues, and the minutely sweep of the sinks the table says hold them |
| `war/src/main/java/.../rs/AdminBean.java` | Scheduled recovery tasks: stale chunks, row rechecks, the gate sweep, job completion |
| `war/src/main/java/.../ejb/JobsBean.java` | Delivery callbacks, and the abort path that has to lift a job's barrier |
