-- Per-job gate for termination chunks. 
--
-- Terminology (termination chunk, barrier, gate, and why the flag is gate_open rather than
-- barrier_open) is in job-store-service/dependency-tracking.md under "Terminology".
--
-- Runs in a transaction, so a failure rolls back cleanly and Flyway re-runs this script on
-- the next startup with no manual cleanup.
--
-- Purely additive by design. Existing rows take is_termination = false and gate_open = true from
-- the defaults, so no gate holds an in-flight chunk back, and jobs partitioned in the window
-- between this migration and the gate logic going live are equally unaffected because nothing
-- writes the columns yet. The cross-job barrier consequently cannot see termination chunks that
-- predate the migration: ordering for those is held by waitingOn and barrierMatchKey until that
-- mechanism is removed, not by the gate. That is known and accepted.

-- is_termination marks the job's termination chunk. It is the discriminator the gate keys on, never
-- the counters below, because a job that is still partitioning and a job with genuinely zero data
-- chunks are indistinguishable by counter alone. gate_open is the flag the dispatch query filters
-- on: a termination chunk is withheld while it is false. Both live on the dependencytracking row
-- rather than on the job, which is what makes the gate structurally safe during partitioning - the
-- row does not exist until markJobAsPartitioned has run, so there is nothing to dispatch early no
-- matter what the counters say.
alter table dependencytracking add is_termination boolean default false not null;
alter table dependencytracking add gate_open boolean default true not null;

-- The per-job counters the gate compares. data_chunks_expected is the job's data-chunk count as
-- read in markJobAsPartitioned *before* it is scheduled. 
--
-- Deliberately not derived from numberofchunks. That column counts the termination chunk too, so
-- comparing against it would leave the gate one short forever, it doubles as the partitioning
-- resume cursor, and JobPurgeBean resets it to 0 on compaction.
alter table job add data_chunks_delivered integer default 0 not null;
alter table job add data_chunks_expected integer default 0 not null;
