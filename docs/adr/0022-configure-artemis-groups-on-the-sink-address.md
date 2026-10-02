# 22. Configure Artemis group settings on the shared `sink` address

Date: 2026-08-23

## Status

Accepted

Implemented under DI-2999.

## Context

Delivery ordering now depends on broker message grouping (ADR 0010), which needs configuration to
stay true across scaling and to bound its memory cost.

Sink queues are not declared in the broker configuration. Each sink resolves its queue name at
runtime from an environment variable and the broker auto-creates the queue on first connect, so a
new sink is deployed without touching broker config and the settings cannot be attached to
individual queue elements.

The queue names turn out not to be addresses. `sink::dummy` is Artemis fully qualified queue name
syntax, meaning queue `dummy` on address `sink`, so every sink queue lives on one address.

## Decision

Attach the group settings to one exact-match address setting for `sink`: bucket count, rebalance,
rebalance pause-dispatch and first-key. No wildcard and no list of queue names.

Drop and re-create the existing sink queues once, while drained, so the defaults apply to them.

Leave the processor and dead-letter addresses untouched.

## Consequences

One rule covers every current and future sink. Bucket tables are per queue, so each sink queue still
gets its own despite sharing an address.

The configuration does not create the ordering guarantee. Measured against a real broker with the
settings absent, groups were still pinned to one consumer, per-group order was still preserved, and
two messages of a group were never in flight together. What it adds is that a late-joining consumer
receives work at all, which without rebalance took none of the load, that the handover rebalance
introduces is safe because dispatch is held until in-flight messages are acknowledged, and that
group tracking is bounded rather than growing with every record id ever seen. Missing configuration
therefore means correct but idle replicas and growing broker memory, not misordered deliveries.

The rebalance pause is also what lets the watermark be read without a local cache (ADR 0011): by the
time dispatch resumes, every prior delivery is in PostgreSQL.

Scoping to sinks is deliberate. Rebalance fires on every consumer add or remove whether or not the
queue has groups, so applied to the processor addresses it would pause dispatch on every processor
pod restart for no benefit.

Re-creating the queues is the part that is not a configuration-only change. Address-setting defaults
apply when a queue is created, so deploying against a broker whose sink queues already exist changes
nothing, silently, while the file reads correctly. The pause-dispatch attribute cannot be set on an
existing queue at all, since no management overload accepts it, so the management API and CLI can
repair only three of the four. The re-creation relies on auto-delete staying off, or an empty
re-created queue is removed again on the next restart.

A bucket count of roughly a million keeps false collisions under one percent at the expected ten
thousand simultaneously active groups, for eight megabytes per queue, and a false collision only
serialises two unrelated records against each other. A group timeout is not used as a memory
alternative: if processing exceeds it, the broker can expire the group and reassign it before the
first item is acknowledged, putting two items for one record in flight together.

Two operational consequences of the naming. Management and JMX names use the real address and queue,
never the combined form, so lookups keyed on the combined name fail. Destructive operations must be
given the combined form, since a bare queue name is ambiguous across addresses and has been observed
removing queues of the same name on more than one address.

The first-key setting is retained without being consumed. Nothing reads the header it produces and
the protocol does not need it, since a sink checks the watermark per item and keeps no per-group
state, but it costs one boolean header and is a useful signal when debugging group handover. It
marks a consumer handover, not the first version of a record, so it is not a substitute for any part
of the watermark comparison.
