# 18. Count each item delivery once, and complete a job without an "already done" guard

Date: 2026-08-18

## Status

Accepted

Implemented under DI-2997.

## Context

A delivery report is redelivered by design: the sink acknowledges its message only after the report
returns, so a crash in between has the message redelivered and the report repeated. The obvious
guard is to return early when the item's delivering outcome is already set.

That check is not lock-protected on its own, and redelivery is not always sequential. The broker's
failure detection can fire while the original consumer thread is still alive and mid-flight on its
own report, producing two genuinely concurrent calls for the same item. Both can see the outcome
unset and proceed.

Double counting here is not a stall. Each call adds exactly one, and the phase closes when the
running total matches the partitioning total, so the phase still closes on an exact match, just one
item too early. Downstream work fires and the job can complete while an item was never delivered at
all, with nothing in the job state recording the omission.

## Decision

Re-check idempotency after acquiring the chunk's exclusive lock, and treat that second check as the
correctness guard. Keep the unlocked check at the top as a fast path for the common case, a
genuinely already-committed replay. Lock chunk then item, matching the bulk path's existing order.

Return from the report whether the chunk's delivering phase is done as of right now, rather than
whether this particular call completed it.

Detect job completion from the item alone, with no guard asking whether the job was already done.

## Consequences

The two concurrent calls serialise at the chunk lock, and whichever runs second re-reads the outcome
fresh and sees the first call's committed write.

A snapshot return value keeps the losing call reporting accurate current status rather than an
unconditional false, and it makes recovery self-healing across the gap between the job-store
transaction committing and the gate call that follows it outside that transaction. If the pod dies
in that gap the sink never gets its response, never acknowledges, and the redelivery re-evaluates
and reports current status, so the gate call simply happens again. An event-based return value would
have that redelivery report that nothing happened, stranding the chunk by a different route. The
gate call already tolerates being made on an unknown or completed chunk, so a redundant one is
harmless.

A "was the job already done" guard on completion is both unnecessary and harmful. Unnecessary,
because every read and write of job state happens under the job row lock, so two concurrent item
deliveries for one job serialise there whatever chunks they belong to. Harmful, because a job with a
termination chunk closes its delivering phase when the last data item reports, before the
termination item has reported at all, so the guard would see the job as complete by the time the
termination item arrives and suppress the completion block entirely. Such jobs would never get a
completion time, never emit the completion notification and never set the fatal-error flag for a
failed termination item.

Once-only is instead guaranteed by the completion test's own two clauses. Without a termination
chunk, the item count equals the partitioning total and exactly one item closes the phase. With one,
the creation of the termination chunk bumps the item count by one while passing zeroed counters, so
the first clause is false for every data item and only the termination item selects itself. This
mirrors the bulk path's pair of clauses, which exist for the same reason.

`PgJobStore_AddItemDeliveredIT` pins all four cases.
