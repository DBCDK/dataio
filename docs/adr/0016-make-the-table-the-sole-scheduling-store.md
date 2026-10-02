# 16. Make `dependencytracking` the sole scheduling store

Date: 2026-09-09

## Status

Accepted

Implemented under DI-3021.

## Context

Chunk scheduling state lived in a distributed Hazelcast map, with the `dependencytracking` table as
a write-behind projection of it. The map was authoritative, the table lagged by up to the configured
write delay, and row lifecycle belonged to the MapStore on its own schedule.

The lag was not free. The bulk delivery sweep could not trust the table's `status`, so it
over-fetched past the free slots and re-checked every candidate against the map. Ownership of the
gate columns was split, surviving only because the projection's conflict clause named neither of
them. The gate's correctness depended on a row removal and a counter increment committing together,
and the map's mutations were not enrolled in the JTA transaction, so a rollback after a removal
reverted the increment without restoring the row.

Once the ordered dispatch query had to filter on a column (ADR 0003), no map predicate could serve
it at all.

## Decision

Delete the map and the projection. Row lifecycle and `status` become synchronous SQL in a
`DependencyTrackingRepository`, every statement running in its caller's transaction on its caller's
connection.

Make every status change one conditional `UPDATE` whose predicate on the target row decides whether
the move happens, and keep that predicate on the target row rather than in the CTE that supplies the
prior status for the counters.

Make the delivery acknowledgement one conditional `DELETE ... RETURNING`, which is both the guard
and the once-only token for the gate's count.

Insert a chunk's row at the site that owns the transaction boundary it needs, carrying its gate
verdict in the insert itself.

Keep the per-sink status counters in the distributed map, maintained from the SQL write sites and
rebuilt at startup, hourly and on demand.

## Consequences

A row read back is what the last committed writer put there, so a candidate the dispatch query
returns is a chunk genuinely waiting, and the over-fetch and the re-check go with the lag they
compensated for. No column is off limits to any statement, so the split ownership of the gate
columns collapses and the convention that survives is the simple one: an unwritten gate is an open
gate.

The removal and the count now commit together, which closes the window where a rollback after the
removal left the chunk uncounted with its row gone.

Under `READ COMMITTED` PostgreSQL re-evaluates a conditional update's predicate against the newest
row version when it finds the row concurrently updated, so two instances racing to advance one chunk
produce exactly one success. A read followed by a write is not sufficient, and this is not
theoretical: two of the callback paths are called concurrently from separate instances.

The CTE is the easy way to write that statement wrong. It is evaluated once against the
transaction's snapshot, so a decision predicate moved into it is frozen at the value both racers
read and both succeed.

`DependencyTrackingService` becomes read-locked rather than serialised, or the conversion would turn
a serialised sequence of microsecond map operations into a serialised sequence of database calls.

`removeJobId` becomes `REQUIRES_NEW`, or the hourly recheck hangs undetectably on its own nested
transactions (ADR 0007).

Write volume becomes the thing to watch. A chunk takes three status updates on the direct path and
up to five when it parks, plus an insert and a delete, and every one of them is a non-HOT update
because `status` is a key column of both ordered indexes. The table therefore carries no index that
is a leading prefix of another, and a per-table autovacuum scale factor well below the default. The
stale-chunk query is left unindexed on purpose, as the cheaper side of that trade.

The counters stay in a distributed map because any instance can handle the callback that changes
one, so a per-instance map would diverge. Hazelcast therefore remains for them, for the aborted-jobs
set and for the cluster-membership guards, holding no chunk state.
