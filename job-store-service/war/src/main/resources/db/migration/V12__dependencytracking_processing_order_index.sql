-- Index backing ordered processing dispatch, and the removal of the one it supersedes.
--
-- Both statements are CONCURRENTLY, so a rolling deploy does not take an ACCESS EXCLUSIVE lock on a
-- table the previous instance is still writing. Flyway also refuses to mix a transactional statement
-- into a non-transactional script, so the drop has no choice either way.
--
-- RECOVERY IF A BUILD FAILS, as V9. A failure cannot be rolled back and leaves an INVALID index that
-- the "if not exists" guard will skip rather than rebuild, plus a failed row in Flyway's schema
-- version table that aborts the next startup. Clear both by hand and restart.

-- Serves the processing candidate query, see DependencyTrackingRepository.findProcessingCandidates.
-- sinkid and status are equality predicates, so the ordered tail is exactly the ORDER BY.
--
-- dependencytracking_delivery_order_index cannot serve it. That index has gate_open as its third key
-- column, and the processing phase must not filter on the gate: under the full barrier width a
-- queued job's data chunks sit at gate_open = FALSE while still needing to be processed. Leaving it
-- unconstrained breaks the ordered tail and the planner sorts.
create index concurrently if not exists dependencytracking_processing_order_index
    on dependencytracking (sinkid, status, priority desc, jobid, chunkid);

-- Now a strict leading prefix of both ordered indexes, so a prefix scan of either serves whatever it
-- served. Dropping it also takes a quarter off the index churn every status change causes, see
-- job-store-service/dependency-tracking.md under "Write volume".
drop index concurrently if exists dependencytracking_sinkid_status_index;
