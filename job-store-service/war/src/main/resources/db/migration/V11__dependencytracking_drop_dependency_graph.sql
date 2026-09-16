-- Removes the dependency graph: the waitingOn/matchKeys mechanism and the BLOCKED status.
--
-- Terminology is in job-store-service/dependency-tracking.md. What replaces this mechanism is
-- already live and has been since DI-3018 to DI-3020: the per-job gate orders whole jobs, and the
-- delivery watermark plus JMSXGroupID order versions of one record. This migration removes the
-- state the old mechanism kept.
--
-- Runs in a transaction, so a failure rolls back cleanly and Flyway re-runs this script on the next
-- startup with no manual cleanup. Nothing here is CONCURRENTLY, deliberately: dropping a column
-- takes an ACCESS EXCLUSIVE lock whatever else the script does, so there is nothing to be gained by
-- splitting it, and the lock is held only for a catalogue update rather than a table rewrite.

-- BLOCKED (value 3) is deleted from ChunkSchedulingStatus, so no row may be left holding it.
-- SCHEDULED_FOR_DELIVERY (value 7) is where such a chunk belongs: it is processed and awaiting
-- delivery, uncapped, and the bulk submitter takes chunks from there in dispatch order. A chunk
-- that still needs holding back is held by its gate, which is a separate column and untouched here.
-- ChunkSchedulingStatus.from maps a stray 3 the same way, for a row written by an instance running
-- the previous build during a rolling restart.
update dependencytracking set status = 7 where status = 3;

-- waitingon held the graph edges, matchkeys the sequence-analysis keys the edges were derived from.
-- Both are read by nothing after this. The GIN index on waitingon would be dropped by the column
-- drop below anyway, and is named explicitly only because the acceptance criteria name it.
--
-- matchkeys is dropped here rather than with the rest of sequence analysis, because what it held is
-- the scheduler's copy of the keys, not the source. chunk.sequenceanalysisdata is the source and
-- stays until DI-3022.
drop index if exists dependencytracking_waitingon_idx;
alter table dependencytracking drop column waitingon;
alter table dependencytracking drop column matchkeys;
