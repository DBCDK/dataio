# 17. Take job, chunk and item entities out of the shared cache

Date: 2026-09-17

## Status

Accepted

## Context

PostgreSQL holds the scheduling state, and a JPA read does not necessarily reach it. The persistence
unit runs under `shared-cache-mode DISABLE_SELECTIVE`, so an entity that does not opt out is held in
EclipseLink's shared identity map. That map belongs to one server session, so it is per instance,
and every read path goes through it. A native query with an entity result class hands back the
cached instance and discards the row it just fetched, and `find` does not reach the database at all
on a hit.

Writes are unaffected, because they take a pessimistic lock and so force a read. Reads are what
diverge. One instance handles the call that completes a job, and only that instance's cache learns
of it.

Cross-instance invalidation is configured, through Payara's Hazelcast publishing transport. It rides
on Payara's data grid, which is a different Hazelcast instance from the one job-store-service starts
for itself, and both halves of its bus are guarded on that grid being enabled with no else branch
and no logging. With the grid disabled, or enabled but not clustered, every coordination message is
dropped silently and EclipseLink never inspects the result. The transport cannot report its own
failure, so no log confirms it is working.

## Decision

Mark `JobEntity`, `ChunkEntity` and `ItemEntity` not cacheable, alongside `JobQueueEntity` and
`WatermarkEntity`. Leave `FlowCacheEntity` and `SinkCacheEntity` cached.

Depend on no cache coordination anywhere in the design.

## Consequences

A read of one of those rows is current on any instance, which is what keeps job status, the hourly
sweeps and the per-item delivery dispatch consistent across instances. The aborted-jobs check that
later replaces the distributed set with a column relies on the same property.

The listings pay nothing for it. Their `SELECT *` runs either way and only object building changes.
The two cache entities stay cached because they are written once and read many times.

Two limits survive. The persistence context still serves the instance it loaded, so a native
statement against a row stays invisible to an entity read earlier in the same transaction, which is
why the gate reads its counter with its own SQL. And a read that is current is still a read:
anything that reads and then acts remains check-then-act, and where that matters the guard is a
lock, not the cache.

`SharedCacheStalenessIT` covers all three entities, modelling a second instance with a second entity
manager factory under its own session name.
