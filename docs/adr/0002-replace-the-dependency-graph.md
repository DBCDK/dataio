# 2. Replace the dependency graph with ordered dispatch and supersession detection

Date: 2026-08-13

## Status

Accepted

## Context

Delivering an older version of a record after a newer one leaves the target holding stale data, and
job-end work that runs while another job's data is still arriving acts on an incomplete set. Both
were prevented by a dependency graph. Each new chunk was scanned against every in-flight chunk on
its sink for overlapping sequence-analysis keys, and a chunk whose keys overlapped an earlier one
was held in `BLOCKED` until that earlier chunk had been delivered.

The mechanism worked and cost more every year:

- Graph construction is quadratic. Each new chunk scans all in-flight chunks for its sink, and under
  a few thousand in-flight chunks that scan is the dominant CPU consumer in job-store-service.
- It is the reason job-store-service depends on Hazelcast. The graph lives in a distributed map with
  custom entry processors, aggregators and GIN-indexed JSONB columns, rebuilt from scratch on every
  restart.
- Priority propagates backwards along the chain, so one high-priority chunk triggers recursive map
  mutations across cluster nodes for as deep as the chain runs.
- Delivering one chunk can unblock hundreds at once, each taking its own EJB transaction and its own
  delivery attempt.

Every one of those costs is paid to enforce two guarantees that have nothing else in common: the
ordering of whole jobs against each other, and the ordering of two versions of one record.

## Decision

Remove the dependency graph, and enforce the two guarantees separately.

Order whole jobs with a per-job gate, a boolean on the chunk's row decided from counters on the job
(ADR 0004). Order versions of one record by serialising same-record deliveries at the broker and
checking a durable watermark before each delivery, so a delivery that has already been overtaken is
skipped rather than held back (ADR 0010, ADR 0011).

Dispatch order becomes an ordered SQL query rather than a graph traversal (ADR 0003).

## Consequences

Scheduling a chunk costs a constant amount of work. Nothing scans the in-flight set, no chunk holds
a key set, and nothing propagates priority along a chain.

Ordering moves from "hold the chunk back" to "check before acting", which moves part of the
correctness into the sinks. Every sink that receives traffic has to take part in the watermark
protocol or opt out deliberately (ADR 0014), and a sink that does neither delivers unordered with
nothing to notice.

Supersession is visible where holding back was not. A skipped delivery is reported and counted
(ADR 0013), where a blocked chunk simply waited.

`BLOCKED`, `waitingOn`, `matchKeys` and the sequence analysis that fed them all go, along with the
abort cascade that existed to keep jobs from stalling behind an aborted one (ADR 0019, ADR 0020).

Because the guarantee now depends on every sink taking part, job-store and the sinks are one
deployable unit (ADR 0023).
