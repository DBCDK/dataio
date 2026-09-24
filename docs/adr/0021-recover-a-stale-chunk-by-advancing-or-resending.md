# 21. Recover a stale chunk by advancing it or resending it, on a per-phase budget

Date: 2026-09-22

## Status

Accepted

## Context

A chunk can be left behind by a crash or a lost JMS message, and with the dependency graph gone
nothing else is watching it. The bulk submitters read only the scheduled statuses, so a chunk left
in either ready or queued state is the recovery sweep's business alone.

Resending is not always the right repair. A chunk whose phase has actually finished is waiting on a
completion call that never ran, and resending it puts duplicate traffic on a processor or a sink
that may be the thing that is down. Resending without limit does the same indefinitely.

Under per-item dispatch a resend sends every item of the chunk again, which the item-level
idempotency guard absorbs (ADR 0018).

## Decision

Deal with a stale chunk in two steps. Where the chunk row's own state says the phase has already
finished, run the completion call again rather than resending. Resend only a chunk whose work really
is outstanding, up to a configurable limit per phase, clearing the count as the chunk enters the
delivery half so that what processing spent is not charged to delivery.

Advance each chunk in its own transaction.

Distinguish a duplicate from a chunk left stuck at the point where a completion call finds nothing
to change, and count and log the second.

Give the two ready states different staleness windows, ten minutes for processing and five for
delivery.

Make the sweeps reachable on demand.

## Consequences

Re-running a completion call is idempotent and sends nothing outside job-store, so it is attempted
on every sweep with no limit. Resending is bounded, and a retry sets the modification time, so a
chunk becomes stale again only once that is older than the phase's timeout and the retries are
already a timeout apart. A chunk that has used its budget is logged once at error and counted, and
no sweep touches it again, so its job cannot complete without an operator retransmitting it. That is
the deliberate end of the ladder rather than an oversight.

Per-chunk transactions mean a chunk that throws costs only itself and is logged, rather than rolling
back a sweep that would otherwise never complete again.

Telling a duplicate from a stuck chunk is what makes the second visible where it happens. A row that
is gone or already past the phase is an acknowledgement of something the instance knew. A row still
waiting for the phase that has just completed is a chunk nothing will move on, and it is logged with
the status the row holds and counted.

The two ready windows differ on purpose. Five minutes on the delivery side covers a real round trip
to a sink. The processing side's attempt is an in-memory asynchronous invocation fired as the
chunk's row commits, so it is milliseconds in health, and its window is set by the opposite risk: a
large partitioning burst queues those invocations, and a sweep firing while they drain hands the
same chunks to the bulk submitter and leaves every queued invocation to find its chunk already
claimed.

The ready-for-processing state had no recovery at all before this. It is held only between a chunk's
row committing and the asynchronous dispatch running, but that invocation is in-memory, so a crash
in the window stranded the chunk. The delivery side had exactly this rescue, which is what marks the
omission as an oversight rather than a decision.

The sweeps are reachable on demand because an operator with a stranded chunk should not wait out a
timer. The stale sweep and the parked-sink sweep run together there, since an operator wants both.
Each sweep's dispatch is asynchronous and runs in its own transaction, so a chunk the call rescues
is sent shortly after it returns rather than during it.
