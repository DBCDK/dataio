# 7. Serialise every gate decision on a per-scope advisory lock

Date: 2026-09-02

## Status

Accepted

Implemented under DI-3019.

## Context

The gate is decided at three sites and re-evaluated by a fourth, and under `READ COMMITTED` two of
them can miss each other. One site reads "an earlier job still holds a barrier" while another
transaction is lifting exactly that barrier and running the re-trigger. The reader still sees the
old value and writes a closed gate, while the re-trigger's scan cannot see the reader's
not-yet-committed row. Both decline, and the gate is closed with nothing left to open it.

The dependency graph did not need serialising this way. Its blocking state lived in a map whose
entry processors made each mutation atomic for one key, and a chunk was released by removing the key
it waited on rather than by reading another job's state. A gate decision reads one job's rows and
writes another chunk's, so no single-key atomicity covers it.

A job row lock does not help, because the two transactions lock different jobs' rows.

A single conditional statement looks like it would remove the need for a lock and does not. One
statement takes one snapshot at its start, so a re-trigger committing between that snapshot and the
insert is still read as unlifted while its update has already run and missed the not-yet-inserted
row. Same lost wakeup.

## Decision

Serialise every decision about whether a gate opens on `pg_advisory_xact_lock` over
`(sinkid, submitter)`, the barrier scope. Every sink type with a termination chunk takes it, not
only the full-width ones.

Fix the lock order as job row, then the barrier scope, then `dependencytracking` rows, and forbid
any transaction from waiting for a job row while holding the advisory lock. The re-trigger therefore
marks the barrier lifted before taking the lock, and the termination row's insert takes the lock
after the job row lock it needs anyway.

Make the data chunk's gate write its own `REQUIRES_NEW` transaction taking no job row lock at all,
rather than joining the partitioning transaction it is called from.

## Consequences

A transaction-scoped advisory lock releases at commit, so it is safe behind a connection pool where
a session-scoped one would not be, and it serialises across service instances as a database lock
rather than as anything the application has to arrange. The scope is the one the jobqueue already
partitions in, one job at a time in job id order, and that shared choice is what lets a data chunk's
gate be decided once at insert.

The ordering rule is forced by the delivery site, which cannot avoid taking the job row first
because its increment locks that row until commit and only then can the counters be compared. The
inversion it rules out is reachable rather than academic: a job's last data chunk and its own
termination chunk can be acknowledged concurrently.

The boundary for the data chunk's gate is the part most easily undone by accident. Its caller's
transaction is the one partitioning opened, which for a large job stays open for minutes, and the
insert of that job's own termination chunk runs in a nested `REQUIRES_NEW` transaction. Taking the
lock in the caller's transaction would have that nested call ask for the same lock on a different
connection and wait for a transaction that cannot commit until the call returns. PostgreSQL sees no
cycle, because the wait is on an EJB call rather than on a database lock, so nothing is detected and
nothing times out. It is an undetectable hang, not a deadlock, and it would hit every job on a
full-width sink.

Committing that row in its own transaction also makes it visible to dispatch from the moment
`scheduleChunk` returns rather than at the end of partitioning.

The barrier read is split in two to keep the common case cheap: an unlocked read first, and the lock
and the write only if something is blocking. That is sound in one direction only, and it is the one
it is used in. "Nothing is blocking" cannot go stale into "blocking", because an earlier job's
barrier comes into existence while that job partitions and the jobqueue finished it first.
"Blocking" can go stale the other way, and that is exactly the answer that goes on to take the lock
and read again. This depends on `READ COMMITTED` taking a fresh snapshot per statement.

`SELECT ... FOR UPDATE` on the earlier job's row would serialise the same paths without an advisory
lock. It is not used, because it works only while every writer goes through a path that touches
those rows, and the advisory lock does not depend on that.

The delivery path takes a `dependencytracking` row lock before the job row, which reads as an
inversion and is not: the row it deletes is always gate-open, and every gate write under the
advisory lock matches closed gates, so the two row sets are disjoint. `removeJobId` is the
exception, since it deletes all of a job's rows whatever their gate, and it runs in its own
transaction for that reason.
