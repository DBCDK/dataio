# 6. Answer the cross-job barrier from a nullable flag on `job`

Date: 2026-09-02

## Status

Accepted

Implemented under DI-3019.

## Context

A job's termination chunk holds back later jobs from the same submitter on the same sink until it
has been delivered. The gate needs to ask, for a given job, whether any earlier job in that scope
still holds such a barrier.

The dependency graph never asked it. A barrier match key went into every later chunk's `waitingOn`
set when the chunk was scheduled, so the question answered itself as the set emptied, and nothing
had to look at another job's state. The gate has to ask it explicitly, which makes where the answer
is read from a decision.

The obvious formulation is presence: does an earlier termination row still exist in
`dependencytracking`. It is the wrong one. Delivery is not the only thing that removes a chunk's
row. `JobPurgeBean` compacts old jobs and the abort and recheck paths drop a job's rows wholesale,
none of them timed by anything the barrier controls.

The damage a stale presence read does is not a late open. The re-trigger that re-evaluates later
jobs is edge-triggered, firing once from the removal of the earlier termination chunk, which is
precisely the instant at which a read of that row is most likely to be wrong. The single evaluation
designed to notice the blocker is gone is the one guaranteed to read that it is still there. It
declines, and there is no poll and no retry behind it. A stale read here cancels the open rather
than delaying it.

## Decision

Add `termination_barrier_lifted` to the `job` table and answer the cross-job check from it.

Make it nullable with no default, carrying three meanings: `NULL` for a job that has no termination
chunk and so never imposes a barrier, `FALSE` while the barrier holds, `TRUE` once it is lifted.
Write `FALSE` in the same transaction that inserts the termination row, and `TRUE` in the
transaction that removes it, guarded on the flag actually standing.

Name it for the barrier rather than for delivery, and write the predicate as `IS FALSE` rather than
as a negation.

## Consequences

The barrier stays answerable after the chunk row is gone, which is what the recovery sweep needs
(ADR 0008), and it is independent of purge and deletion timing.

Only a minority of sink types get a termination chunk, so `NULL` is the common case by a wide
margin. `NOT NULL DEFAULT FALSE` would assert that every job in the table holds later jobs back,
saved from doing damage only by a second predicate elsewhere, and relying on a second predicate to
neutralise a wrong default is how the next query written against the column gets it wrong. `NOT NULL
DEFAULT TRUE` is not wrong that way, but it conflates "never had a barrier" with "had one, now
lifted", which is a conflation `data_chunks_expected = 0` already forces the design to reason around
(ADR 0004). A nullable column costs nothing and keeps the lifecycle monotonic, `NULL` to `FALSE` to
`TRUE`, where raising the barrier is a transition rather than a toggle away from a default that
already claimed it was lifted.

The permissive default is safe here for one specific reason: the `FALSE` write and the termination
row's insert are the same transaction. A job cannot end up with a termination row and an unset
barrier, and if that transaction rolls back there is no row to block on either.

Raising the barrier any earlier gains nothing and costs two things. The cross-job check joins the
job to its `is_termination` row, so a job carrying `FALSE` with no such row holds nothing back and
an early write would sit inert until the row arrived anyway. Worse, the recovery sweep reads exactly
that combination, `FALSE` with no termination row, as a job whose row was removed without a lift
(ADR 0008). Every job still partitioning would land in the set the sweep repairs, and have its
barrier lifted and its scope re-triggered while it was still being created.

Whether a job raises a barrier at all is also not known earlier. The sink type is, but a job that
leaves partitioning through an early return or is aborted part way never gets a termination chunk,
and an early `FALSE` would leave it holding a barrier with nothing able to lift it. `NULL` to
`FALSE` at the insert is what keeps that case out of the column.

There is no window to cover in the meantime. Within a barrier scope the jobqueue partitions one job
at a time in job id order, so no later job has chunks to hold back until the earlier one is fully
partitioned, which is after both the termination row and the flag are written. The late write
depends on that invariant, as the data chunk's gate does for the same reason (ADR 0005), so
relaxing it would mean revisiting both.

Abort is the other cause of a lift, and the abort path lifts the flag too. A flag called
`delivered` would have to be set untruthfully there or left false, and left false it would strand
every later job on that submitter. Lifting is guarded on the flag standing because the abort and recheck paths reach any job
whose rows they remove, not only the minority holding a barrier, and an unguarded write would turn
`NULL` into `TRUE` and lose the distinction. The guarded statement's row count also answers whether
there was a barrier at all, so the rest of the work is skipped when there was not.

Both predicates are correct as written, since a negation over `NULL` yields `NULL` and excludes the
row anyway. `IS FALSE` states the intent without asking the reader to work through three-valued
logic.
