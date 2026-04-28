# Dependency Tracking

Dependency tracking is the mechanism that prevents chunks from being delivered to a sink out of sequence, and prevents duplicate or orphaned processing when multiple service instances are running. The authoritative state lives in three Hazelcast IMaps shared across all instances.

## Hazelcast state

| IMap | Key | Value | Purpose |
|---|---|---|---|
| `DEPENDENCY_TRACKING` | `TrackingKey` (jobId + chunkId) | `DependencyTracking` | One entry per active chunk |
| `SINK_STATUS` | sinkId | `Map<ChunkSchedulingStatus, Integer>` | Cached per-sink scheduling counters |
| `LAST_TRACKER` | `WaitFor` | `TrackingKey` | Fast-path index for sequencing (opt-in via `WAIT_FOR_TRACKING_ENABLED`) |

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

For sink types that require strict job-level ordering (MARCCONV, PERIODIC_JOBS, TICKLE), a synthetic **termination chunk** is appended at the end of each job. Its `barrierMatchKey` is the submitter ID, so it explicitly waits for all prior chunks from the same submitter that are still in flight. Future jobs from the same submitter then wait for this termination chunk, enforcing job-level ordering at the sink.

## Unblocking — the `RemoveWaitingOn` EntryProcessor

When `chunkDeliveringDone` fires:

1. The completed chunk's entry is removed from the IMap.
2. `removeFromWaitingOn` runs `RemoveWaitingOn` as a Hazelcast `executeOnEntries` across all entries whose `waitingOn` contains this key. Hazelcast executes this atomically on whichever node owns each partition.
3. `RemoveWaitingOn.process()` removes the key from `waitingOn`. If the set becomes empty and status is `BLOCKED`, it transitions to `READY_FOR_DELIVERY` and returns a `StatusChangeEvent`.
4. Each newly unblocked chunk is handed to `attemptToUnblockChunk` in a **separate transaction** to avoid exhausting the JMS connection pool.

## Multi-instance safety

- The IMap is distributed across all Hazelcast cluster members (one per Payara instance). Each `TrackingKey` is owned by exactly one partition/node.
- EntryProcessors (`RemoveWaitingOn`, `UpdateStatus`, `UpdateCounter`, `UpdatePriority`) execute **on the owning node**, so mutations are atomic and require no network round-trip.
- `modify()` uses `tryLock` (2-minute timeout) for cases that need a read-modify-write.
- The `SINK_STATUS` counters IMap is maintained via `UpdateCounter` EntryProcessors, keeping per-sink scheduling counts consistent without full scans.
- **Reaper tasks** (`@Schedule` in `JobSchedulerBean`) find chunks stuck in `QUEUED_FOR_PROCESSING` or `QUEUED_FOR_DELIVERY` beyond `PROCESSOR_TIMEOUT` (default 1 hour) and re-enqueue them, recovering from crashes or lost JMS messages.

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
