-- Indexes backing the per-job gate columns added in V8 and ordered delivery dispatch. 
--
-- Created concurrently: a rolling deploy runs this while the previous instance still writes
-- dependencytracking on every chunk transition, so a plain CREATE INDEX would take an ACCESS
-- EXCLUSIVE lock and stall live scheduling.
--
-- RECOVERY IF A BUILD FAILS. CONCURRENTLY cannot run inside a transaction, so Flyway runs this whole
-- script non-transactionally and a failure cannot be rolled back. Two things are then left behind:
-- an INVALID index, which the "if not exists" guard below will silently skip rather than rebuild,
-- and a failed row in Flyway's schema version table, which makes the next startup abort with
-- "Detected failed migration to version 9" instead of retrying. Both have to be cleared by hand,
-- then restart the service so Flyway re-runs this migration.

-- Cross-job submitter barrier. Serves the check "does an earlier job with the same submitter, on
-- the same sink, still hold an undelivered termination chunk":
--
--   select 1 from dependencytracking
--    where sinkid = ? and submitter = ? and is_termination and jobid < ? limit 1
--
-- Partial on purpose. dependencytracking is upserted on every chunk transition, and only
-- termination rows are ever read through this index, so leaving the data-chunk rows out keeps it
-- off the hot write path entirely. jobid is a key column so the "earlier job" comparison is part of
-- the range scan rather than a heap filter.
create index concurrently if not exists dependencytracking_barrier_index
    on dependencytracking (sinkid, submitter, jobid) where is_termination;

-- Ordered delivery dispatch. Serves the bulk-scheduler query:
--
--   select jobid, chunkid from dependencytracking
--    where sinkid = ? and status = ? and gate_open
--    order by priority desc, jobid asc, chunkid asc
--    limit ?
--
-- sinkid, status and gate_open are all equality predicates, so the ordered suffix
-- (priority desc, jobid, chunkid) is exactly the ORDER BY and no sort is needed - but only because
-- the index carries the desc direction on priority. since priority is stored with higher = more 
-- important (Priority.HIGH(7)/NORMAL(4)/LOW(1)). 
--
-- Do not expect the planner to choose this index at low row counts. Measured on a freshly seeded
-- table, it prefers a bitmap scan of the older, narrower dependencytracking_sinkid_status_index
-- followed by a sort, since sorting a hundred rows is cheaper than a wider index. The shape only
-- pays off once a sink has enough queued chunks that the sort dominates. If the dispatch query is
-- later seen sorting in production, that is a statistics and volume question, not a reason to
-- change the index.
create index concurrently if not exists dependencytracking_delivery_order_index
    on dependencytracking (sinkid, status, gate_open, priority desc, jobid, chunkid);
