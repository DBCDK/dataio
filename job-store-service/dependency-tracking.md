# Dependency Tracking

Dependency tracking is the mechanism that prevents chunks from being delivered to a sink out of sequence, and prevents duplicate or orphaned processing when multiple service instances are running. The authoritative state lives in Hazelcast structures shared across all instances.

## Terminology

Three words are easy to conflate, and two of them face opposite ways. A fourth, width,
says how far the barrier reaches:

- **Termination chunk** - the object. A synthetic chunk appended to a job for the sink types that
  require job-level ordering (`createJobTerminationChunkEntity`, `REQUIRES_TERMINATION_CHUNK`).
- **Barrier** - what a termination chunk imposes on *other* chunks: nothing from the same submitter
  and sink may pass until it has been delivered. Arranged by `barrierMatchKey`, see
  [Barrier chunks](#barrier-chunks) below.
- **Gate** - what is imposed on *the termination chunk itself*: it is withheld until its own job's
  data chunks are acknowledged and no earlier job with the same submitter on the same sink still
  holds a barrier that has not been lifted. Carried by `dependencytracking.gate_open`, against the
  counters `job.data_chunks_expected` and `job.data_chunks_delivered`, with the cross-job half
  answered from `job.termination_barrier_lifted`. That flag is named for the barrier and not for
  delivery because delivery is only its usual cause: aborting a job lifts the barrier too, and its
  termination chunk was never delivered. It is nullable on purpose, `NULL` for the majority of jobs
  that have no termination chunk at all, `FALSE` while the barrier holds, `TRUE` once it is lifted.
  Query it as `IS FALSE`, never as `NOT ...`.
- **Barrier width** - how much of a later job the barrier holds back. Today it is full
  width: every data chunk of a later same-submitter job on the same sink waits for the
  earlier job's termination chunk, not just that job's own termination chunk. The redesign
  narrows this to termination chunks only, except for sink types whose job-end work is not
  scoped to its own job (`TICKLE`, whose total-batch `mark`/`sweep` is dataset-wide), which
  keep the current width. See `docs/chunk-scheduling-redesign.md`, "Barrier Width -
  Per-Sink-Type Job Isolation".

So a termination chunk both imposes a barrier on others and is held by a gate of its own. That is
why the flag is `gate_open` and not `barrier_open`: it says this chunk may now be dispatched, not
that the barrier it imposes on others has been lifted.

Ownership of those four columns is split, and the split matters. `job.data_chunks_expected`,
`job.data_chunks_delivered` and `job.termination_barrier_lifted` are ordinary job-store state on a
table job-store owns outright. `dependencytracking.is_termination` and `gate_open` sit on a table
whose rows are inserted and deleted by the Hazelcast MapStore on a write-behind schedule, and are
job-store's only because `DependencyTrackingStore`'s `UPSERT` does not name them in its
`on conflict ... do update set` clause. **They must never be added to that clause.** The cross-job
half of the gate lives on `job` for exactly this reason: it is the half that would otherwise have
had to read row presence, which job-store does not control. See
`docs/chunk-scheduling-redesign.md`, "Who writes the gate columns before Phase 9".

The gate's evaluation rules are set out in `docs/chunk-scheduling-redesign.md`, "Barrier Chunks —
Per-Job Gate". The ordering described in the rest of this document is enforced through `waitingOn`
and `barrierMatchKey`.

**A closed gate actually withholds the chunk.** Both dispatch paths read `gate_open` and neither
will send a chunk whose gate is closed, so the column decides delivery rather than merely recording
a decision taken elsewhere. A chunk held by it waits in `SCHEDULED_FOR_DELIVERY`, which has no
capacity cap, exactly as a `BLOCKED` chunk does today. **A gate that closes and is never reopened
is a job that never completes**, which is why every removal of a termination row has to lift the
barrier and re-trigger, and why the `recheckBlocks` sweep exists as a backstop.

**Source of truth is split until the Hazelcast map goes.** Delivery order and `gate_open` are read
from PostgreSQL; `status` is read from the map, because the table's copy of it is written write-behind
by the MapStore and lags by up to `write-delay-seconds`. The bulk sweep therefore takes an ordered
candidate list from SQL and re-checks each candidate against the map before dispatching it. See
`docs/chunk-scheduling-redesign.md`, "Delivery Ordering", for why the query cannot be a Hazelcast
predicate and what the split costs.

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

For sink types that require strict job-level ordering (MARCCONV, PERIODIC_JOBS, TICKLE), a synthetic **termination chunk** is appended at the end of each job. Its `barrierMatchKey` is the submitter ID, so it explicitly waits for all prior chunks from the same submitter that are still in flight. Future jobs from the same submitter then wait for this termination chunk, enforcing job-level ordering at the sink. Note that this holds back *every* chunk of a future job, not only its termination chunk, because `scheduleChunk` passes the barrier key to `findChunksToWaitFor` for every chunk it schedules. See **Barrier width** under [Terminology](#terminology).

## Unblocking — the `RemoveWaitingOn` EntryProcessor

`chunkDeliveringDone` first ignores the call outright if the chunk has no tracking entry (already
completed) or is not in `QUEUED_FOR_DELIVERY`. Otherwise:

1. The completed chunk's entry is removed from the IMap.
2. The per-job gate is advanced for the chunk's job: a data chunk is counted, a termination chunk
   lifts its job's barrier and re-evaluates the jobs queued behind it. This is synchronous SQL in
   the caller's transaction and is independent of the `waitingOn` unblocking below, which is what
   still enforces ordering. See "Terminology" above.
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
  - `recheckBlocks()` (hourly) drops trackers for jobs that are gone or already completed, and releases chunks left `BLOCKED` on dependencies that no longer exist.
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
| `war/src/main/java/.../ejb/JobGateBean.java` | Per-job gate, delivery side: counts data chunks, lifts barriers, re-triggers later jobs |
| `war/src/main/java/.../ejb/JobGateRepository.java` | The gate's synchronous SQL, and the barrier-scope advisory lock |
| `war/src/main/java/.../ejb/DeliveryDispatchRepository.java` | Ordered delivery candidates and the gate check both dispatch paths read |
| `war/src/main/java/.../ejb/JobSchedulerBulkSubmitterBean.java` | Per-second bulk submission of `SCHEDULED_FOR_*` chunks to the JMS queues |
| `war/src/main/java/.../rs/AdminBean.java` | Scheduled recovery tasks: stale chunks, blocked rechecks, job completion |
