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
  holds one. Carried by `dependencytracking.gate_open`, against the counters
  `job.data_chunks_expected` and `job.data_chunks_delivered`.
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

The gate's evaluation rules are set out in `docs/chunk-scheduling-redesign.md`, "Barrier Chunks —
Per-Job Gate". The ordering described in the rest of this document is enforced through `waitingOn`
and `barrierMatchKey`.

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
2. `removeFromWaitingOn` runs `RemoveWaitingOn` as a Hazelcast `executeOnEntries` across all entries whose `waitingOn` contains this key. Hazelcast executes this atomically on whichever node owns each partition.
3. `RemoveWaitingOn.process()` removes the key from `waitingOn`. If the set becomes empty and status is `BLOCKED`, it transitions to `READY_FOR_DELIVERY` and returns a `StatusChangeEvent`.
4. Each newly unblocked chunk is handed to `attemptToUnblockChunk` in a **separate transaction** to avoid exhausting the JMS connection pool.

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
| `war/src/main/java/.../ejb/JobSchedulerBulkSubmitterBean.java` | Per-second bulk submission of `SCHEDULED_FOR_*` chunks to the JMS queues |
| `war/src/main/java/.../rs/AdminBean.java` | Scheduled recovery tasks: stale chunks, blocked rechecks, job completion |
