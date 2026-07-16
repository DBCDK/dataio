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

An index on `(sinkid, status, priority, jobid, chunkid)` serves this without a sort.
Direct-mode dispatch (`READY_FOR_DELIVERY → QUEUED_FOR_DELIVERY`) applies the same
order when multiple chunks become ready simultaneously.

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

New per-job counters (on the `job` table or a dedicated small table):

```sql
delivered_data_chunks  INT  NOT NULL DEFAULT 0
total_data_chunks      INT  NOT NULL  -- set when the job is partitioned
```

The gate logic is owned by **job-store-service**: it runs in `chunkDeliveringDone(jobId,
chunkId)`, inside the same transaction as the `reportItemResult` call that completed the
chunk's DELIVERING phase. The scheduler-service only *reads* `gate_open` in its dispatch
query.

```
1. If job has a termination chunk AND gate_open = FALSE:
     atomically: INCREMENT delivered_data_chunks for this job
     if delivered_data_chunks == total_data_chunks:
         SET gate_open = TRUE for this job's termination chunk
         (scheduler picks it up on its next poll cycle)
```

**Cross-job submitter barrier.** When job B's per-job counter completes, check whether
any earlier job with the same submitter and same sink still has a termination chunk
present in `dependencytracking` — i.e. not yet delivered, regardless of its gate state
(an earlier termination chunk with an open gate may still be sitting in
`SCHEDULED_FOR_DELIVERY` or `QUEUED_FOR_DELIVERY`). If one exists, keep job B's gate
closed. Covered by one index seek on `(sinkid, submitter, is_termination)`.

**Re-trigger.** When a termination chunk is delivered (removed from `dependencytracking`),
re-evaluate the gates of later same-submitter jobs on the same sink: any job whose
`delivered_data_chunks == total_data_chunks` and which no longer has an earlier
undelivered termination chunk gets its gate opened. Without this step, a job whose
counter completed while an earlier termination chunk was pending would stay closed
forever.

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
| Barrier | TBD — no record key; `JMSXGroupID` not set |

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

- **Watermark key** = `RecordInfo.id` — the watermark tracks the highest-delivered
  version *per individual record*, not per group. Volume records from different jobs
  for the same physical volume are superseded correctly even though they share the
  constant correlationKey with all other hierarchy records.

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
sub-second window — a very rare occurrence, accepted as a known limitation. (The current
dependency-tracking system covers this case only when sequence analysis happens to emit
overlapping match keys for both versions.)

---

## Per-Item Dispatch

`SinkMessageProducerBean` iterates over `ItemEntity` rows for the chunk and sends one
JMS message per item:

```java
for (ItemEntity item : chunkItems) {
    Message msg = session.createTextMessage(serialize(item.getProcessingOutcome()));
    msg.setIntProperty(JMSHeader.jobId,    jobId);
    msg.setIntProperty(JMSHeader.chunkId,  chunkId);
    msg.setShortProperty(JMSHeader.itemId, item.getKey().getId());
    RecordInfo ri = item.getRecordInfo();
    if (ri != null && ri.getCorrelationKey() != null) {
        msg.setStringProperty("JMSXGroupID", ri.getCorrelationKey());
    }
    producer.send(msg);
}
```

Items with null `recordInfo` or null `correlationKey` receive no `JMSXGroupID` and are
distributed freely across consumers — they are processed unconditionally anyway.

`RecordInfo` is already stored per `ItemEntity`; no new schema is required. The
`JMSHeader.itemId` constant does not exist today and must be added to `commons/types`
(`JMSHeader.java`). It identifies the individual item within the chunk: it is part of
the `(jobId, chunkId, itemId)` version tuple compared against the watermark before
delivery, and of the result-reporting endpoint path
`POST /v1/jobs/{jobId}/chunks/{chunkId}/items/{itemId}/delivering`. No chunk-size
header is needed: job-store detects chunk completion from its own phase counters, and
since the broker distributes items by record group — not by chunk — no single consumer
is guaranteed to see all items of a chunk anyway.

---

## Artemis Broker Configuration

Per sink queue in `broker.xml`:

```xml
<address name="Sink.{name}">
  <anycast>
    <queue name="Sink.{name}"
           group-rebalance="true"
           group-rebalance-pause-dispatch="true"
           group-first-key="JMSXGroupFirstForConsumer">
      <group-buckets>1048576</group-buckets>
    </queue>
  </anycast>
</address>
```

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
CREATE TABLE sink_record_delivery_watermark (
    sink_id    INT         NOT NULL,
    record_key VARCHAR     NOT NULL,
    job_id     INT         NOT NULL,
    chunk_id   INT         NOT NULL,
    item_id    SMALLINT    NOT NULL,
    PRIMARY KEY (sink_id, record_key)
);
```

### Upsert on delivery

`reportItemResult` atomically advances the watermark when the incoming version is newer:

```sql
INSERT INTO sink_record_delivery_watermark
       (sink_id, record_key, job_id, chunk_id, item_id)
VALUES (?, ?, ?, ?, ?)
ON CONFLICT (sink_id, record_key) DO UPDATE
  SET job_id   = EXCLUDED.job_id,
      chunk_id = EXCLUDED.chunk_id,
      item_id  = EXCLUDED.item_id
  WHERE (EXCLUDED.job_id, EXCLUDED.chunk_id, EXCLUDED.item_id)
      > (sink_record_delivery_watermark.job_id,
         sink_record_delivery_watermark.chunk_id,
         sink_record_delivery_watermark.item_id)
```

### REST endpoints

**Read watermark (called by sink before each delivery):**

```
GET /v1/sinks/{sinkId}/watermarks/{recordKey}
→ 200 {"watermark": {"jobId": 1234567, "chunkId": 42, "itemId": 3}}
→ 200 {"watermark": null}          (no watermark exists for this record key)
```

Backed by a direct PostgreSQL PK read: `(sinkId, recordKey)` is the primary key; the
lookup is O(1). No Hazelcast. If throughput under per-delivery call rate becomes a
measured bottleneck, an in-process cache or Hazelcast IMap can be added later without
changing the correctness argument.

**Call-rate impact.** Today a sink makes one HTTP call per chunk (`addChunk`, 10 items).
The new protocol makes one GET plus one POST per *item* — a ~20× increase in HTTP calls
to job-store-service, plus one watermark upsert per item on PostgreSQL. At a sink
throughput of 1 000 items/s this is 2 000 requests/s against the job-store fleet.
Capacity-plan job-store-service instances and their connection pools accordingly. If
needed, a batched read (`GET /v1/sinks/{sinkId}/watermarks?keys=k1,k2,…`) lets a sink
prefetch watermarks for all items of a chunk in one round trip without changing the
correctness argument.

**Report item result (called by sink after delivery):**

```
POST /v1/jobs/{jobId}/chunks/{chunkId}/items/{itemId}/delivering
Content-Type: application/json

{ "sinkId": 42, "recordKey": "...", "status": "DELIVERED" | "SKIPPED" | "FAILED" }
```

Implementation (one transaction):

```
1. Write deliveredResult to ItemEntity.deliveringOutcome
2. Increment State.Phase.DELIVERING counters on ItemEntity and ChunkEntity
3. If status == DELIVERED AND recordKey != null:
       DB upsert watermark (using conditional upsert above)
4. If ChunkEntity.state passes State.phaseDone(DELIVERING):
       call chunkDeliveringDone()  → per-job counter + gate logic
```

**Idempotency:** if `ItemEntity.deliveringOutcome` is already set, return 200 and take
no further action. This handles crash-then-redelivery between step 3 and `session.commit()`.

### Version comparison

Versions are compared as `(jobId, chunkId, itemId)` tuples — lexicographically on the
sink side (compare `jobId` first, then `chunkId`, then `itemId`) and as a native row
comparison in the SQL upsert above. No bit-packing into a `long`: a packed encoding
would impose hard bit-width limits (e.g. 16 bits caps `chunkId` at 65 535, which
million-record jobs exceed) and risks diverging from the SQL tuple comparison. The
tuple is carried as three plain integer fields in the REST payloads.

---

## Sink Delivery Protocol

### Per-item delivery sequence

```
1. Receive item message (session not yet committed)
2. key = getRecordKey(message)
3. if key != null:
       watermark = GET /v1/sinks/{sinkId}/watermarks/{key}
       incoming  = (jobId, chunkId, itemId)
       if watermark != null AND incoming < watermark:   // lexicographic tuple compare
           POST reportItemResult(... SKIPPED)
           session.commit()
           return
       // incoming == watermark: exact retransmit, always deliver (idempotent re-delivery)
4. deliver item to target system
5. POST /v1/jobs/{jobId}/chunks/{chunkId}/items/{itemId}/delivering  (reportItemResult)
6. session.commit()   →   JMS ACK
```

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

### Sink framework (`MessageConsumerAdapter`)

Subclasses implement two methods:

```java
protected abstract String getRecordKey(Message message);
protected abstract ItemStatus deliverItem(Message message) throws Exception;
```

The framework handles the watermark check and result reporting around `deliverItem`. No
lock is required: the broker guarantees that same-key items are never processed
concurrently across threads or pods.

`ServiceHub` gains a `WatermarkServiceConnector`. `MessageConsumerApp` is structurally
unchanged.

### `WatermarkServiceConnector` (job-store-service-connector)

```java
public record Watermark(int jobId, int chunkId, short itemId)
        implements Comparable<Watermark> {
    // compareTo: jobId, then chunkId, then itemId
}

public interface WatermarkServiceConnector {
    Optional<Watermark> getWatermark(long sinkId, String recordKey);
    void reportItemResult(long sinkId, int jobId, int chunkId, short itemId,
                          String recordKey, ItemStatus status);
}
```

HTTP implementation following the existing connector pattern.

---

## Operational Resilience

### Sink crash recovery

| Failure point | Behaviour | Recovery |
|---|---|---|
| Before step 4 (deliver) | Message rolls back; redelivered to next available group consumer | No side effects |
| After step 4, before step 5 (reportItemResult) | Message rolls back; redelivered | Duplicate target call. Idempotent targets (rawrepo, solr-doc-store) absorb it. Same exposure as current `addChunkIgnoreDuplicates` retry |
| After step 5, before step 6 (session.commit) | Message rolls back; redelivered | `reportItemResult` called again; idempotency guard sees `deliveringOutcome` already set; ignored cleanly |

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
| job-store-service (N instances) | All inbound REST requests: job submission, partitioning callbacks (`/chunks/{id}/processed`), item delivery results (`/items/{id}/delivering`), watermark reads. Writes item/chunk state, the watermark, the per-job counters (`delivered_data_chunks`), and `gate_open` to PostgreSQL. |
| scheduler-service (1 instance) | Continuously reads `dependencytracking` from PostgreSQL, advances chunk `status` through the state machine, fires JMS dispatch. Holds in-memory `SINK_STATUS` counters. |

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
`delivered_data_chunks`, and `gate_open` (the gate logic runs in the `reportItemResult`
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
| `SequenceAnalysisData` / `SequenceAnalysisOption` | Removed |
| `DefaultKeyGenerator` | Removed |
| `ChunkEntity.sequenceAnalysisData` column | Migration drops it |
| `SinkContent.SequenceAnalysisOption` | Removed |
| `dependencytracking.matchkeys` (GIN-indexed text[]) | Migration drops it |
| `dependencytracking.waitingon` (GIN-indexed int[]) | Migration drops it |

`ItemEntity.recordInfo` already holds the record key per item. `SinkMessageProducerBean`
reads `RecordInfo.getCorrelationKey()` directly when building item messages.

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
| `SINK_STATUS` Hazelcast counters | Moved to scheduler-service as JVM `ConcurrentHashMap<Integer, AtomicInteger>` |
| `ChunkSchedulingStatus.BLOCKED` (value 3) | Deleted; rows migrated to `SCHEDULED_FOR_DELIVERY` |

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

- Flyway migration (purely additive): add `is_termination`, `gate_open` columns to
  `dependencytracking`; add `delivered_data_chunks`, `total_data_chunks` counters to
  the `job` table; add the delivery ordering index and the
  `(sinkid, submitter, is_termination)` barrier index
- Implement the per-job gate logic in `chunkDeliveringDone` (runs alongside the
  existing `waitingOn` mechanism — redundant but harmless)
- Update the bulk-scheduler ordering query to `(priority DESC, jobid ASC, chunkid ASC)`
  with the `gate_open` filter
- Dependency tracking, `BLOCKED` and sequence analysis remain fully active
- `SINK_STATUS` remains a distributed Hazelcast map: it is mutated by callback
  handling on any job-store instance (see [Counter Management](#counter-management));
  it moves to a JVM map at scheduler extraction (Phase 11)

### Phase 2 — Add `correlationKey` to `RecordInfo`

- Add `correlationKey` field to `RecordInfo`; derivation logic on `MarcRecordInfo`
  (record type alone: standalone → own id, head/section/volume → the shared hierarchy
  constant — no section-to-head resolution, no partitioner changes)
- Add priority-override logic for chunks containing live head/section records

### Phase 3 — Watermark table and endpoints (job-store-service)

- Flyway migration: create `sink_record_delivery_watermark`
- Add `GET /v1/sinks/{sinkId}/watermarks/{recordKey}` endpoint
- Add `POST /v1/jobs/{jobId}/chunks/{chunkId}/items/{itemId}/delivering` endpoint
- Add watermark upsert to `reportItemResult` write path
- Integration tests against real PostgreSQL

### Phase 4 — Connector (job-store-service-connector)

- `Watermark` record, `WatermarkServiceConnector` interface and HTTP implementation
- Unit tests with WireMock

### Phase 5 — Broker configuration

- Add `group-rebalance`, `group-rebalance-pause-dispatch`, `group-first-key`,
  `group-buckets=1048576` to each sink queue in `broker.xml`
- Configuration-only change; deploy and verify before Phase 6

### Phase 6 — Per-item dispatch (`SinkMessageProducerBean`)

- Add `itemId` constant to `JMSHeader` (`commons/types`)
- Iterate items; set `JMSXGroupID` from `RecordInfo.getCorrelationKey()`; send one
  message per item
- Requires Phase 5 broker config to be live
- Coexists with chunk-level dependency tracking: the graph still decides *when* a
  chunk is dispatched; this phase only changes *how* (N item messages instead of one
  chunk message)

### Phase 7 — Sink framework (`commons/artemis-jse-app`)

- `MessageConsumerAdapter`: watermark check + result reporting before and after `deliverItem`
- `ServiceHub`: add `WatermarkServiceConnector`
- Abstract `getRecordKey(Message)` and `deliverItem(Message)` for subclasses

### Phase 8+ — Individual sink migrations (one PR per sink)

- Implement `getRecordKey(Message)` and `deliverItem(Message)` per sink
- Remove old chunk-aggregation logic
- Coordinate with Phase 6/7 release to ensure backward compatibility or migrate all
  sinks in one release

### Phase 9 — Remove dependency tracking (job-store-service)

Precondition: all sinks are live on the per-item + watermark protocol (Phase 8
complete) — see ordering constraint 1 above.

- Delete `BLOCKED` state from `ChunkSchedulingStatus`; migrate existing `BLOCKED`
  rows to `SCHEDULED_FOR_DELIVERY`
- Remove `DEPENDENCY_TRACKING` and `LAST_TRACKER` Hazelcast maps and all entry processors
- Remove `findChunksToWaitFor`, `trackChunksToWaitFor`, `optimizeDependencies`,
  `boostPriorities`, `removeFromWaitingOn`, `addDependencies`, fan-out loop
- Remove `LastTrackerMap` and `BlockedCounter` aggregators
- Flyway migration: drop `waitingon`, `matchkeys` columns and their GIN indexes
- Hazelcast itself stays for now: the `isMaster()`/`isSlave()` guards, `ABORTED_JOBS`,
  and `executeOnMaster` partitioning routing are still in use until Phase 11

### Phase 10 — Sequence analysis removal (job-store-service)

- Remove `SequenceAnalysisData`, `SequenceAnalysisOption`, `DefaultKeyGenerator`
- Remove `SinkContent.SequenceAnalysisOption`
- Flyway migration: drop `sequenceAnalysisData` column from `chunk` table

### Phase 11 — Extract scheduler-service, drop Hazelcast

- Create new `scheduler-service` Maven module (Payara Micro, same tech stack)
- Move `JobSchedulerBean`, `JobSchedulerTransactionsBean`, `JobSchedulerBulkSubmitterBean`,
  `AdminBean` (scheduling parts), and `SinkMessageProducerBean` into the new service
- Scheduler-service connects to the same PostgreSQL instance as job-store-service
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
| Same record, priority inversion | BLOCKED guarantees serial delivery | Broker delivers higher-priority item first; watermark check skips stale item |
| Termination chunk dispatched before all data chunks delivered | Impossible (BLOCKED) | Per-job counter gate: termination held until all `chunkDeliveringDone()` fired |
| Cross-job termination ordering (same submitter, same sink) | Termination BLOCKED on prior-job termination | Gate checks no earlier same-submitter termination pending |
| Exact retransmit (stale recovery) | `addChunkIgnoreDuplicates` | `incoming == watermark` → always deliver (idempotent re-delivery) |
| Pod crash, stale watermark after rebalance | Hazelcast MapStore reloads from PostgreSQL | No local cache to become stale; `group-rebalance-pause-dispatch` ensures watermark is current before any post-rebalance dispatch |
| Live head/section before volume delivery | Dependency tracking + barrier | Constant hierarchy group serialises all hierarchy records; priority override ensures head chunk dispatched first |

---

## Open Questions

1. **Barrier records (`correlationKey`)**: the `correlationKey` for barrier/termination
   records is not yet defined. Since barrier records carry no record ID, they receive no
   `JMSXGroupID` and are distributed freely — this is acceptable, but the field mapping
   should be documented explicitly.

2. **Watermark table growth**: the table grows with the number of unique record keys
   delivered per sink. A retention policy (e.g., prune rows not updated within a
   configurable window) reduces growth at the cost of a small correctness risk for
   records not seen recently. Define the retention policy before the table reaches
   operational scale.

3. **Should FAILED advance the watermark?** Currently only `DELIVERED` does. In the
   priority-inversion case a newer version can be dispatched first, fail at the target,
   and then an older version behind it passes the watermark check and is delivered —
   the target regresses to old data with no newer write coming. Advancing the watermark
   on `FAILED` (recording the latest *attempted* version) would suppress the older
   delivery instead, leaving the target unchanged and the failure visible in the job
   state. Both behaviours are defensible; decide and document before Phase 3
   (watermark table and endpoints).
