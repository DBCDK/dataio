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
total_data_chunks      INT  NOT NULL DEFAULT 0
```

`total_data_chunks` is the job's data-chunk count, meaning `numberOfChunks` as read in
`markJobAsPartitioned` *before* `createAndScheduleTerminationChunk` runs.
`createJobTerminationChunkEntity` increments `numberOfChunks` itself, so a read taken
after it returns is one too high, the counter can never reach the total, and the gate
stays closed forever. For a job that has a termination chunk the value therefore equals
that chunk's `chunkId`, which makes it cheap to assert. The column needs a default
because the migration runs against a non-empty `job` table, and it is written on every
partitioned job, including those that leave `markJobAsPartitioned` through its early
returns without a termination chunk.

A job can have zero data chunks. `addAndScheduleEmptyJob` calls `markJobAsPartitioned`
with `numberOfChunks = 0`, and partitioning an empty data file reaches the same state.
Such a job still gets a termination chunk, at `chunkId = 0`. Its gate must be evaluated
when that chunk is inserted, subject to the same cross-job barrier as any other, because
no data chunk will ever be delivered to trigger the increment. Opening the gate only from
`chunkDeliveringDone` strands these jobs permanently.

Note that `total_data_chunks = 0` therefore carries two opposite meanings, and the gate
must not try to tell them apart by the counter. It is the value the migration's default
leaves on every job that predates the gate, including any job partitioned between the
migration and the gate logic going live, which must be ignored entirely. And it is the
legitimate value for a job with genuinely zero data chunks, whose gate must be evaluated
at once. The flag that separates them is `is_termination` on the job's `dependencytracking`
row, so the gate keys on that and treats the counter only as a total to compare against.
The overlap is benign in one direction only: pre-gate jobs already carry `gate_open = TRUE`
from the default, so mistakenly opening their gate is a no-op. Mistakenly ignoring an empty
job strands it.

The gate logic is owned by **job-store-service**: it runs in `chunkDeliveringDone(jobId,
chunkId)`. The scheduler-service only *reads* `gate_open` in its dispatch query.

```
1. If job has a termination chunk AND gate_open = FALSE:
     if this call is the one that removed the chunk's dependencytracking row:
         INCREMENT delivered_data_chunks for this job
         if delivered_data_chunks >= total_data_chunks:
             SET gate_open = TRUE for this job's termination chunk
             (scheduler picks it up on its next poll cycle)
```

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
double increment makes `delivered_data_chunks` overtake the number of data chunks that
have actually been delivered. Each call adds exactly 1, so the counter still reaches
`total_data_chunks` exactly, just too early, and the gate opens while data chunks are
still outstanding - precisely what the barrier exists to prevent. The termination chunk
is dispatched ahead of them, and job-end work such as marcconv's `ConversionFinalizer`
or `PeriodicJobs*FinalizerBean` runs against incomplete data. This is the same shape of
failure as the item counters under [Same-item concurrent redelivery](
#same-item-concurrent-redelivery-job-store-service): premature completion, not a stall.

The hazard that *does* strand the gate permanently is the opposite one. A non-atomic
read-modify-write increment can lose an update, leaving the counter one short of
`total_data_chunks` with no further chunk to deliver, so the gate never opens. That is
why the increment must be a single atomic statement
(`SET delivered_data_chunks = delivered_data_chunks + 1`) rather than a read followed by
a write.

The removal of the chunk's `dependencytracking` row is the natural token, since exactly
one concurrent caller can perform it. The increment must therefore be conditioned on
having won that removal, not on a preceding read of the row - the current
`get`-then-`remove` sequence in `chunkDeliveringDone` is a non-atomic check-then-act that
two callers can both pass. Harmless for the idempotent work that follows it today, not
harmless for a counter. See [Phase 1](#phase-1--gate-and-ordered-dispatch-job-store-service)
for the required signature change. The `>=` in the pseudocode above is defensive only:
it does not mitigate either hazard above, since double counting reaches the total exactly
and a lost update leaves the counter below it forever. It guards only against the counter
being pushed above `total_data_chunks` by something other than the increment, such as a
backfill or a revised total.

Note that full transactional atomicity between the increment and the row removal only
arrives once `dependencytracking` is read and written directly in PostgreSQL. While it
remains a Hazelcast map with a Postgres MapStore, map mutations are not enrolled in the
JTA transaction, so a rollback after the removal reverts the increment without restoring
the row, leaving the chunk uncounted and unrecoverable. That window is pre-existing (it
already applies to the unblock cascade), but the gate is the first logic whose
correctness depends on closing it.

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
sub-second window — a very rare occurrence, accepted as a known limitation. (The current
dependency-tracking system covers this case only when sequence analysis happens to emit
overlapping match keys for both versions.)

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

The body is the item's processing outcome alone. `ItemEntity` also carries a
`nextProcessingOutcome`, populated only for acceptance-test runs, and `sink/diff` is its
sole consumer. It is deliberately not carried onto the item message: the diff sink is
being deprecated, so it does not shape the wire format, and adding a wrapper body for it
would put a second shape in front of every other sink.

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
`Chunk` rather than as a stated contract. Under per-item dispatch it is load-bearing.

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
watermark collision actively produces a wrong `SKIPPED` verdict for an unrelated
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
#sink-framework-messageconsumeradapter) below) entirely up to each sink, on the
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
older of the two jobs would be judged stale and wrongly reported `SKIPPED`. Composing
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
sink (see [Sink framework](#sink-framework-messageconsumeradapter) below): every
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
`SKIPPED`) leaves `last_modified` untouched. See [Retention Policy](#retention-policy)
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

{ "sinkId": 42, "recordKey": "...", "status": "DELIVERED" | "SKIPPED" | "FAILED" }
```

Same path as the existing `GET .../items/{itemId}/delivered` (which reads back
`ItemEntity.deliveringOutcome`) — POST writes the delivery result, GET reads it, one
resource. This also matches the existing bulk `POST /jobs/{jobId}/chunks/{chunkId}/delivered`
endpoint's naming: "delivered" there already covers `FAILURE`/`IGNORE` item outcomes
within the chunk, not just success, so the same word covering `SKIPPED`/`FAILED` at
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
that gets `SKIPPED` both leave `last_modified` untouched, so a record can keep
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
           POST reportItemResult(... SKIPPED)
           session.commit()
           return
       // incoming == watermark: exact retransmit, always deliver (idempotent re-delivery)
4. deliver item to target system
5. POST /jobs/{jobId}/chunks/{chunkId}/items/{itemId}/delivered  (reportItemResult)
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

Subclasses implement one method:

```java
protected abstract ItemDeliveryResult.Status deliverItem(Message message) throws Exception;
```

`getRecordKey(Message message)` is **not** left to subclasses — it is a final method on
`MessageConsumerAdapter` itself, reading `JMSHeader.recordKey` straight off the message
(`null` if absent, e.g. for a barrier/termination item whose `RecordInfo.getId()` was
null at dispatch time — see [Open Questions](#open-questions) §1). No sink re-derives
this key from delivered content; see the rationale in [Per-Item Dispatch](
#per-item-dispatch) above. This also means adding a new sink no longer requires getting
record-key derivation right per sink — one less thing for Phase 8+ migrations to get
wrong.

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
                          String recordKey, ItemDeliveryResult.Status status);
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
| job-store-service (N instances) | All inbound REST requests: job submission, partitioning callbacks (`/chunks/{id}/processed`), item delivery results (`/items/{id}/delivered`), watermark reads. Writes item/chunk state, the watermark, the per-job counters (`delivered_data_chunks`), and `gate_open` to PostgreSQL. |
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
- Change `DependencyTrackingService.remove(TrackingKey)` to report whether *this* call
  performed the removal, and condition the `delivered_data_chunks` increment on it.
  It already computes this and discards it: `dependencyTracker.remove(key)` returns the
  previous value atomically per key and the method checks `removed == null`, but returns
  `void`, so no caller can learn it won the race. Without this the increment sits behind
  `chunkDeliveringDone`'s non-atomic `get`-then-`remove` check and can double-count,
  opening the gate before all data chunks are delivered, see [Barrier Chunks](
  #barrier-chunks--per-job-gate)
- Increment `delivered_data_chunks` with a single atomic statement, never a read followed
  by a write: a lost update leaves the counter permanently below `total_data_chunks` and
  the gate never opens
- Update the bulk-scheduler ordering query to `(priority DESC, jobid ASC, chunkid ASC)`
  with the `gate_open` filter
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

- `MessageConsumerAdapter`: watermark check + result reporting before and after `deliverItem`
- `getRecordKey(Message)` implemented once on `MessageConsumerAdapter` itself, reading
  `JMSHeader.recordKey` — not abstract, not per-sink (see [Sink framework](
  #sink-framework-messageconsumeradapter))
- `ServiceHub`: add `WatermarkServiceConnector`
- Abstract `deliverItem(Message)` for subclasses

### Phase 8+ — Individual sink migrations (one PR per sink)

- Implement `deliverItem(Message)` per sink
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
   [Sink framework](#sink-framework-messageconsumeradapter) — reads no property, returns
   `null`, and the framework skips the watermark check entirely for that item. No
   per-sink logic needs to "recognise" a termination message; every sink gets this for
   free from the same message-property mechanism that also fixes the record-key
   whitespace-normalisation bug. Before this fix landed, every job's termination item
   shared the literal watermark key `"End Item"`, so delivering job 1000's termination
   item would have made job 999's look stale and get it wrongly reported `SKIPPED` —
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
