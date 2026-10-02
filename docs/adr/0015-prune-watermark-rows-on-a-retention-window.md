# 15. Prune watermark rows on a retention window

Date: 2026-08-14

## Status

Accepted

Implemented under DI-2995.

## Context

`sink_record_delivery_watermark` gains a row the first time a record is delivered to a sink and
never loses one, because the upsert only ever advances a row. It therefore grows with the number of
distinct records ever delivered per sink, which over the life of the system is every record dataio
has ever handled.

## Decision

Carry `last_modified` on the row, refreshed only when the watermark itself advances, and delete
rows older than a configurable retention window nightly. The window is a MicroProfile Config
property defaulting to ninety days.

Mirror the existing scheduled job purge in structure, including its master-only guard.

## Consequences

`last_modified` plays no part in the version comparison, which is done purely on the position
triple. It exists to drive pruning, which is why it is gated by the same version-advance guard as
the rest of the row: a delivery the guard rejects, an exact retransmit or an older version that is
superseded, leaves it untouched.

That makes the pruning condition "has not advanced" rather than "has seen no traffic", which is the
stronger condition and the one that carries the trade-off. A record can keep receiving delivery
attempts indefinitely without its row refreshing, and if a genuinely older version arrives after the
row is pruned it is delivered instead of skipped.

This is acceptable for a narrower reason than "no delivery in months is irrelevant". Under normal
operation a record stops generating traffic once its current version has been delivered, since no
new message exists for it until a later job supersedes it, so time since last advance and time since
last attempt coincide for the overwhelming majority of records. They diverge only under redelivery
and retry, which resolve within the same operational incident rather than months later.

One residual case is not covered: a chunk retried against a persistently failing target for longer
than the window, never superseded by a newer job, has its row pruned mid-incident. A chunk retried
for ninety days is an operational anomaly monitoring should already surface, so it is accepted as a
narrow limitation rather than grounds for a different design. The window should be chosen with
comfortable margin over any expected stuck-retry duration, not just over normal job turnaround.
