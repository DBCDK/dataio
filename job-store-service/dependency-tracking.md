# Dependency Tracking

Dependency tracking is the mechanism that prevents chunks from being delivered to a sink out of
sequence, and prevents duplicate or orphaned processing when multiple service instances are running. 
The authoritative state lives in Hazelcast structures shared across all instances.

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
  and sink may pass until it has been delivered. Arranged by `barrierMatchKey`, see
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

`waitingOn` and `barrierMatchKey`, described through the rest of this document, currently enforce
both kinds of ordering at once. The gate and the watermark are the replacement, one for each kind,
and both are already live. Neither is load-bearing yet, because `waitingOn` still enforces everything
they do and more. It can be removed once the gate covers job ordering for every sink type and every
sink has migrated to the watermark.

## The per-job gate

The gate answers one question per chunk: may this chunk be delivered yet? It is a single boolean,
`gate_open`, on the chunk's `dependencytracking` row, and both dispatch paths refuse to send a chunk
whose gate is closed. A held chunk waits in `SCHEDULED_FOR_DELIVERY`, which has no capacity cap,
exactly as a `BLOCKED` chunk does.

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

### Who owns the gate columns

`dependencytracking` is a write-behind projection of the Hazelcast map, so ownership of its columns
is split, and getting this wrong is the main way to break the gate.

The MapStore owns **row lifecycle**. Rows appear and disappear on its own schedule, up to
`write-delay-seconds` (10 in production) after the corresponding map change. Nothing in the gate may
depend on a row existing at a particular moment.

Job-store owns the **gate column values**. `DependencyTrackingStore`'s upsert names five columns in
its `on conflict ... do update set` clause, and cannot clobber a column it does not name.
`is_termination` and `gate_open` are outside that list, which is what lets job-store write them in
plain synchronous SQL with no lag. **Never add those two columns to that clause**, which
`JobGateIT.mapStoreDoesNotClobberGateColumns` is there to enforce.

`is_termination` is also a field on `DependencyTracking`, and that is not a contradiction. The clause
still does not name it, so nothing about who writes the column changes, and a copy on the map value
cannot go stale for a value decided when the row is created and never changed. A delivery reads the
branch off the entry it removed rather than querying a row it has just removed, see
[Where the gate is decided](#where-the-gate-is-decided). **`gate_open` gets no such field.** Four
sites write it over a chunk's life, so a copy would be a second answer to a question with one, and a
stale open gate dispatches a job's end-of-job work ahead of the data it summarises.

Three consequences follow:

- **A writer that closes a gate creates the row itself**, rather than waiting for the MapStore.
  Waiting means the update matches nothing and is lost, and the MapStore's later insert takes
  `gate_open`'s default of `TRUE`, dispatching the very chunk that should have been held. Such an
  insert must also supply `sinkid`, `matchkeys` and `submitter`, which the conflict clause would
  never repair.
- **A missing row means an open gate.** Between `scheduleChunk` and the next flush a chunk has no
  row at all, so reading absence as "closed" would stall everything. Absence is safe to read as open
  because only a writer that creates the row can close a gate.
- **The cross-job check asks the flag, never row presence.** A delivered termination chunk's row
  lingers for the delete delay, and the re-trigger below fires at exactly that instant, so a
  presence check would be evaluated when the table is at its most stale about the very row it asks
  after. It would see the blocker as still there, decline, and never fire again. A stale read there
  does not delay the gate opening, it cancels it.

### Where the gate is decided

| Site | When | Decides |
|---|---|---|
| A | a data chunk is delivered (`JobGateBean.advanceGateState`) | counts the delivery, then opens its job's termination chunk if that was the last one |
| B | the termination chunk is inserted (`createJobTerminationChunkEntity`) | that chunk's initial gate, and writes `data_chunks_expected` |
| C | a data chunk is inserted (`scheduleChunk`) | that chunk's gate, and only for full-width sink types |
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
- **Site A runs once per chunk, and what makes that true is the removal.** `chunkDeliveringDone` is
  called again by every redelivery, and the broker's failure detection can produce two genuinely
  concurrent calls for one chunk, so the count hangs off the one thing only one caller can do:
  `DependencyTrackingService.remove` hands the removed entry to whichever caller removed it and null
  to every other. The read of the entry before it is a filter on status, not the token, since
  `get`-then-`remove` is a check-then-act two callers can both pass. Counted twice, the counter still
  lands on `data_chunks_expected` exactly, only while a data chunk is still in flight, so the failure
  is a job that reads as complete rather than one that stalls. The rest of `chunkDeliveringDone` runs
  for every caller: `removeFromWaitingOn` reports only the entries it changed, so the caller that
  lost the removal finds nothing left to unblock.
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
re-trigger. Skipping it leaves the removed job reading as still blocking for the whole delete delay,
which is the window the re-trigger fires in, so it would decline and never fire again, stalling every
later job from that submitter.

### Barrier width per sink type

Width is how much of a later job the barrier holds back.

`waitingOn` makes it **full width** for every sink type: `scheduleChunk` passes the submitter's
`barrierMatchKey` to `findChunksToWaitFor` for every chunk, so nothing at all from job B reaches the
sink before job A's job-end has been delivered.

The gate is narrower by default, **termination width**: only job B's job-end waits for job A's. For
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

Until `waitingOn` is removed it enforces full width for everything anyway, so the gate being narrower
for marcconv and periodic-jobs has no observable effect yet.

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

`JobGateBean.closeDataChunkGateIfBlocked` is therefore `REQUIRES_NEW` and takes no job row lock at
all. The lock is then held briefly, once per closed chunk, and the row is committed by the time
`scheduleChunk` returns rather than at the end of partitioning, so dispatch can see it throughout the
window it matters. `scheduleChunk` also reads the barrier once without the lock and only enters that
transaction when something is blocking. That shortcut is safe in one direction only, which is the one
it is used in: "nothing is blocking" cannot become "blocking", since an earlier job's barrier is
created while that job partitions and the jobqueue finished it first. "Blocking" can go stale the
other way, and that is exactly the answer that goes on to take the lock and read again.

### When a gate is left closed

Two ways a gate outlives its reason to be shut, each of them a job that never completes:

- A termination row removed without its barrier being lifted.
- A lost delivery count. `chunkDeliveringDone` removes the chunk's map entry *before* the gate work,
  and that removal is not part of the transaction. Anything failing afterwards rolls back the count
  while the removal stands, and on redelivery `chunkDeliveringDone` finds no tracker and returns
  immediately. The counter is then permanently one short with no delivery left to arrive. The same
  loss on the termination branch never lifts the barrier, stalling every later job from that
  submitter.

That second one is inherent while `dependencytracking` is a Hazelcast map, and it is accepted rather
than overlooked. The removal is what both the count's once-only property and a redelivery's
early return hang off, so no choice of marker closes the window: it closes when the row is deleted in
the same transaction as the count, which is where the map goes away. Accepting it buys the far worse
failure being gone. A count that is lost leaves a job visibly stuck and swept within the hour, while
a count taken twice reaches the total early and lets end-of-job work run on data that never arrived,
with nothing in the job state saying so.

`AdminBean.recheckBlocks` sweeps for both hourly, which bounds the damage to one sweep interval. It
lifts the barrier of any job left holding one with no termination row, then opens any gate closed
with no earlier unlifted barrier, requiring additionally for a termination chunk that its own job's
data chunks are delivered. Opened on the earlier barrier alone, a termination chunk would be
dispatched while its own data chunks were still in flight.

An hour is a long time to hold a job that cannot complete, so the same sweep is reachable on demand
at `POST dependency/gate_sweep`. It runs exactly what the hourly pass runs, in the same order, and is
safe to call at any time: it only ever opens a gate whose reason to be shut is already gone. Each
barrier scope is swept in its own transaction, so the advisory lock is held per scope rather than for
the whole sweep, which is what keeps an on-demand call from stalling delivery acknowledgements across
every scope it visits.

**The sweep reads that last condition from the absence of the job's data-chunk rows, never from
`data_chunks_delivered`.** The counter is exactly what the second failure destroys, so a sweep
reading it could not repair the case it exists for. The row removal is the half that survives.

The sweep needs no knowledge of sink type, since only site C closes a data chunk's gate and it runs
only for full-width sinks, so the existence of such a row is the answer. It works one barrier scope
at a time under that scope's lock.

### Why the dispatch query is SQL

Delivery order and `gate_open` are read from PostgreSQL, while `status` is read from the map, because
the table's copy of it is written write-behind and lags. The bulk sweep therefore takes an ordered
candidate list from SQL and re-checks each candidate against the map before dispatching it.

The query cannot be a Hazelcast predicate, and the reason is the ownership split above: `gate_open`
is a column on the table and deliberately not a field on `DependencyTracking`, so no predicate can
see it. Putting it on the map value to make one possible is what would give a chunk two answers to
whether it may be dispatched, one of them written behind the other's back.

## Delivery watermark

The gate orders whole jobs. The watermark is the other half of the picture: it stops an *older*
version of a single record from overwriting a newer one at the sink. That is the job `waitingOn` and
sequence analysis do today, and the watermark is what replaces them.

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

**Status.** Everything above is built and live on the job-store side: the table, the forward-only
upsert on delivery, the read endpoint, the purge, and both JMS headers on every item sent.
`SinkMessageConsumerAdapter` implements the sink half. **No production sink consumes it yet**, only
tests, so the watermark is not currently preventing anything in production. `waitingOn` and `BLOCKED`
are still what actually enforce record ordering, which is why they are still here. Each sink
migration moves one sink onto the watermark, and `waitingOn` can only be removed once every sink has
moved.

## Hazelcast state

| IMap | Key | Value | Purpose |
|---|---|---|---|
| `DEPENDENCY_TRACKING` | `TrackingKey` (jobId + chunkId) | `DependencyTracking` | One entry per active chunk |
| `SINK_STATUS` | sinkId | `Map<ChunkSchedulingStatus, Integer>` | Cached per-sink scheduling counters |
| `LAST_TRACKER` | `WaitFor` | `TrackingKey` | Fast-path index for sequencing (opt-in via `WAIT_FOR_TRACKING_ENABLED`, default off) |
| `ABORTED_JOBS` | - | ISet of jobId | Jobs being aborted, checked on the scheduling and unblocking paths |

## The `DependencyTracking` record

Each entry holds:

- **`key`** — `(jobId, chunkId)`, the unique identity
- **`sinkId`** — which sink this chunk is destined for
- **`submitter`** — the submitter ID (used for barrier scoping)
- **`status`** — current scheduling status (see lifecycle below)
- **`matchKeys`** — string keys derived from sequence analysis data plus an optional barrier key; used to find chunks this one must sequence after
- **`waitFor`** — indexed form of matchKeys as `WaitFor(sinkId, submitter, key)` tuples, used for Hazelcast predicate queries
- **`waitingOn`** — set of `TrackingKey`s this chunk is currently blocked by
- **`termination`** - whether this chunk is its job's termination chunk, mirroring the
  `is_termination` column. The column is the authority and this is read back from it whenever an
  entry is loaded, which is sound because the value is decided when the row is created and never
  changes. It is here so that the caller who removes an entry on delivery can tell the two branches
  of the gate apart from the entry it was handed, see [Where the gate is decided](#where-the-gate-is-decided).
  **`gate_open` has no counterpart here, and must not get one**, see below
- **`priority`**, **`lastModified`**, **`retries`**

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
READY_FOR_DELIVERY ──────────────────────────────┐
       │                                          │
       │  (waitingOn non-empty)          (no blocking deps)
       ▼                                          │
    BLOCKED                                       │
       │  (RemoveWaitingOn clears last dep)        │
       └──────────────► READY_FOR_DELIVERY ◄──────┘
                               │
               (bulk: SCHEDULED_FOR_DELIVERY)
                               │
               (direct: QUEUED_FOR_DELIVERY)
                               │
                (chunkDeliveringDone removes entry)
```

`QUEUED_FOR_PROCESSING` and `QUEUED_FOR_DELIVERY` each have a cap of 1000 entries per sink, used for backpressure against the Artemis queues.

Both edges into `QUEUED_FOR_DELIVERY` are gated, and neither dispatches a chunk with
`gate_open = FALSE`:

- **bulk** — `JobSchedulerBean.bulkScheduleToDeliveringForSink` takes candidates from
  `DeliveryDispatchRepository.findDeliveryCandidates`, an SQL query ordered by
  `(priority DESC, jobid ASC, chunkid ASC)` with the gate filter in its `WHERE` clause, then
  re-checks each candidate's status against the map.
- **direct** — `submitToDeliveringIfPossible` checks the gate after the capacity check and, when it
  is closed, parks the chunk in `SCHEDULED_FOR_DELIVERY` so the bulk sweep picks it up once the gate
  opens. `submitToDelivering` checks again immediately before the status change, which is the choke
  point every path funnels through.

## How dependency relationships are built

When `scheduleChunk` is called, `addAndBuildDependencies` runs:

1. **Sequence analysis** — the chunk carries `sequenceAnalysisData` (typically bibliographic record IDs). These become `matchKeys`.
2. **`findChunksToWaitFor`** queries the IMap for any currently active chunks for the same sink+submitter that share one or more matchKeys — i.e., chunks whose records overlap with this one that must be delivered first.
3. **`optimizeDependencies`** prunes transitive redundancy: if chunk B already waits for A, and a new chunk would wait for both A and B, only B is kept.
4. The resulting `waitingOn` set is stored. A non-empty set means the chunk will enter `BLOCKED` after processing completes (processing itself still proceeds).
5. **Priority boost** — if the new chunk has higher priority than anything it is waiting on (cross-job), those predecessors get their priorities raised transitively via the `UpdatePriority` EntryProcessor.

## Barrier chunks

For sink types that require strict job-level ordering (MARCCONV, PERIODIC_JOBS, TICKLE), a synthetic **termination chunk** is appended at the end of each job. Its `barrierMatchKey` is the submitter ID, so it explicitly waits for all prior chunks from the same submitter that are still in flight. Future jobs from the same submitter then wait for this termination chunk, enforcing job-level ordering at the sink. Note that this holds back *every* chunk of a future job, not only its termination chunk, because `scheduleChunk` passes the barrier key to `findChunksToWaitFor` for every chunk it schedules.

The gate reproduces that width without `waitingOn`, but only for the sink types that need it, which
is what lets `waitingOn` eventually be deleted without taking tickle's dataset-wide guarantee with
it. See [Barrier width per sink type](#barrier-width-per-sink-type), and
[Delivery watermark](#delivery-watermark) for the record-level ordering that replaces the rest.

## Unblocking — the `RemoveWaitingOn` EntryProcessor

`chunkDeliveringDone` first ignores the call outright if the chunk has no tracking entry (already
completed) or is not in `QUEUED_FOR_DELIVERY`. Otherwise:

1. The completed chunk's entry is removed from the IMap.
2. The per-job gate is advanced for the chunk's job: a data chunk is counted, a termination chunk
   lifts its job's barrier and re-evaluates the jobs queued behind it, opening both the termination
   chunks and the data chunks of later jobs in the scope. This is synchronous SQL in the caller's
   transaction, independent of the `waitingOn` unblocking below, which is what still enforces
   ordering. See [The per-job gate](#the-per-job-gate).
3. `removeFromWaitingOn` runs `RemoveWaitingOn` as a Hazelcast `executeOnEntries` across all entries whose `waitingOn` contains this key. Hazelcast executes this atomically on whichever node owns each partition.
4. `RemoveWaitingOn.process()` removes the key from `waitingOn`. If the set becomes empty and status is `BLOCKED`, it transitions to `READY_FOR_DELIVERY` and returns a `StatusChangeEvent`.
5. Each newly unblocked chunk is handed to `attemptToUnblockChunk` in a **separate transaction** to avoid exhausting the JMS connection pool, in `(priority DESC, jobId ASC, chunkId ASC)` order. The order matters because any one of them can take the last free slot of `QUEUED_FOR_DELIVERY`, so it decides which reach the sink now and which wait for the next sweep.

## Multi-instance safety

- The IMap is distributed across all Hazelcast cluster members (one per Payara instance). Each `TrackingKey` is owned by exactly one partition/node.
- EntryProcessors (`RemoveWaitingOn`, `UpdateStatus`, `UpdateCounter`, `UpdatePriority`) execute **on the owning node**, so mutations are atomic and require no network round-trip.
- `modify()` uses `tryLock` (2-minute timeout) for cases that need a read-modify-write.
- The `SINK_STATUS` counters IMap is maintained via `UpdateCounter` EntryProcessors, keeping per-sink scheduling counts consistent without full scans.
- **Scheduled tasks run on one instance only.** Every `@Schedule` method on this path opens with `if (Hazelcast.isSlave()) return;`, so the recovery work below happens once per cluster rather than once per instance.
- **Recovery tasks** live in `AdminBean` (`rs` package), not in `JobSchedulerBean`, whose only `@Schedule` method is `updateSinks()`:
  - `updateStaleChunks()` (every minute) re-drives chunks left behind by crashes or lost JMS messages. Entries stale in `READY_FOR_DELIVERY` for more than 5 minutes are pushed to `SCHEDULED_FOR_DELIVERY`; entries stale in `QUEUED_FOR_DELIVERY` beyond 1 hour, and in `QUEUED_FOR_PROCESSING` beyond `PROCESSOR_TIMEOUT` (default `PT1H`), are resent. It also maintains the per-sink stale-chunk metric.
  - `recheckBlocks()` (hourly) drops trackers for jobs that are gone or already completed, lifting the barrier of each so the jobs queued behind it are released, and releases chunks left `BLOCKED` on dependencies that no longer exist. It then sweeps the gate: it lifts the barrier of any job left with one and no `is_termination` row, and opens any gate closed with no earlier unlifted barrier, requiring for a termination chunk that its own job's data chunks are delivered. That last condition is read from the absence of the job's data-chunk rows and never from `job.data_chunks_delivered`, because the counter is exactly what a rollback in `chunkDeliveringDone` loses, so a sweep reading it could not repair the failure it exists for.
  - `completeFinishedJobs()` (hourly) closes jobs whose work finished without the completion being recorded.

## Key files

| File | Role |
|---|---|
| `distributed-objects/src/main/java/.../DependencyTracking.java` | Per-chunk state object |
| `distributed-objects/src/main/java/.../ChunkSchedulingStatus.java` | Status enum with valid transitions and capacity limits |
| `distributed-objects/src/main/java/.../hz/processor/` | EntryProcessors for atomic in-place mutations |
| `distributed-objects/src/main/java/.../hz/aggregator/` | Aggregators for cluster-wide stats |
| `war/src/main/java/.../dependencytracking/DependencyTrackingService.java` | Singleton facade — primary API for all tracking operations |
| `war/src/main/java/.../dependencytracking/Hazelcast.java` | IMap initializer and cluster membership helpers |
| `war/src/main/java/.../ejb/JobSchedulerBean.java` | Primary caller; owns the scheduling and unblocking logic |
| `war/src/main/java/.../ejb/JobGateBean.java` | Per-job gate: counts data chunks, lifts barriers, re-triggers later jobs, closes a data chunk's gate at schedule time, and owns the hourly gate sweep |
| `war/src/main/java/.../ejb/JobGateRepository.java` | The gate's synchronous SQL, and the barrier-scope advisory lock |
| `war/src/main/java/.../ejb/DeliveryDispatchRepository.java` | Ordered delivery candidates and the gate check both dispatch paths read |
| `war/src/main/java/.../ejb/SinkMessageProducerBean.java` | Composes the `recordKey` and `JMSXGroupID` headers each item carries |
| `war/src/main/java/.../rs/WatermarksBean.java` | The watermark read endpoint sinks call before each delivery |
| `war/src/main/java/.../ejb/WatermarkPurgeBean.java` | Nightly pruning of watermark rows past `WATERMARK_RETENTION` |
| `commons/artemis-jse-app/.../jms/SinkMessageConsumerAdapter.java` | Sink-side half of the watermark protocol, with the opt-out |
| `war/src/main/java/.../ejb/JobSchedulerBulkSubmitterBean.java` | Per-second bulk submission of `SCHEDULED_FOR_*` chunks to the JMS queues |
| `war/src/main/java/.../rs/AdminBean.java` | Scheduled recovery tasks: stale chunks, blocked rechecks, the gate sweep, job completion |
| `war/src/main/java/.../ejb/JobsBean.java` | Delivery callbacks, and the abort path that has to lift a job's barrier |
