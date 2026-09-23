# 3. Take dispatch candidates from an ordered SQL query

Date: 2026-09-03

## Status

Accepted

Implemented under DI-3020.

## Context

With the dependency graph gone (ADR 0002), nothing holds a chunk back to keep it behind an older
one, so the order chunks are dispatched in becomes the order they reach the sink in. That order is a
correctness property rather than a fairness one. Every record of a MARC hierarchy carries the same
correlation key, so the broker delivers them in the order they were sent, and the watermark cannot
help because a head and its volumes are different records with different keys.

The bulk scheduler took its candidates from a Hazelcast `PagingPredicate`, which truncated to an
arbitrary page and could not see the gate columns at all. A high-priority chunk could sit behind
lower-priority ones for as long as the queue stood at its cap, which defeated the priority override
for live head and section records at the hop where it is cheapest to honour.

Both phases also have a second dispatcher. The direct paths send a chunk the instant it becomes
ready, which is what keeps latency low on a quiet sink. Free capacity says the sink has room, not
that this chunk is entitled to it.

## Decision

Take candidates for both phases from an ordered SQL query over `dependencytracking`, ordered by
`priority DESC, jobid ASC, chunkid ASC` and limited to the sink's free queue slots. The delivery
query additionally filters on `gate_open`.

Give each phase its own index, shaped so the equality predicates form a prefix and the ordered
columns follow exactly, so rows come back in order from the scan and the limit reads no further.
Keep `gate_open` out of the processing query and out of its index: a gate holds back delivery and
nothing else, and under full barrier width a queued job's data chunks are gated while still needing
to be processed normally.

Make both direct paths read the head of the same order at `LIMIT 1` and stand down when that head
outranks the chunk they hold, parking it for the sweep.

## Consequences

The priority inversion at the processing hop is fixed rather than left to the broker.

Without the rank guard the order holds only over chunks that happen to be parked. The sweep fills
every free slot each tick, so between ticks the only free slots are those freed by completions, and
a job partitioning right now takes them on arrival. Where a partitioner emits chunks at least as
fast as chunks complete, the sweep dispatches nothing for as long as the burst lasts, and a volume
overtakes its head.

The guard costs an idle sink one index probe, answered from the index's first entry, and it is
placed last of the dispatch guards so that is all it costs.

The two indexes cannot be collapsed into one. `gate_open` sits third in the delivery index, and with
no equality predicate on it the ordered tail no longer follows the equality columns, so the planner
sorts.

The ordering is index-backed whatever the vacuum state, which is what these indexes are for. Whether
the scan is index-only is decided by the visibility map and mostly will not be, because the table
churns. Budget one heap buffer per candidate returned.

The planner prefers a narrower index and a sort until a sink has enough queued chunks for the sort
to dominate, so `EXPLAIN` against a freshly seeded table proves nothing.
