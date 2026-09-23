# 11. Order record versions with a shared delivery watermark

Date: 2026-08-14

## Status

Accepted

Implemented under DI-2995, DI-2996 and DI-2998.

## Context

Broker grouping (ADR 0010) stops two versions of one record from being processed concurrently. It
does not decide which of them should win, and it does not cover every path: a group's messages are
delivered in send order, but a redelivery, a stale-chunk resend or a record that changed correlation
key between jobs can still present an older version after a newer one has landed.

Something has to hold, per record and per sink, the newest position already delivered, and it has to
be visible to every consumer thread on every pod.

The obvious place for it is the sink itself, a map per pod loaded from the database at startup.

## Decision

Keep it in PostgreSQL, one row per sink and record key naming the newest job, chunk and item already
delivered for that record. A sink reads the row before each delivery, skips the item when the row
already names an equal-or-newer position, delivers otherwise, and reports the outcome. Job-store
advances the row when it records a `DELIVERED` result, in an upsert whose conflict clause compares
the incoming triple against the stored one and does nothing unless it is strictly greater.

Read it per delivery over HTTP. No local cache in the sink.

Compare versions as a `(jobId, chunkId, itemId)` tuple, lexicographically on the sink side and as a
native row comparison in SQL. No bit-packing into a single number.

Deliver an exact retransmit rather than skipping it.

## Consequences

Two threads racing on one record converge on the newer of them whichever order they arrive in, and a
restarted sink picks up mid-job without replaying anything.

The per-pod map was rejected for two reasons, and the second has no fix. Loading it means reading
potentially millions of rows at startup. And after a rebalance the new pod's map reflects only what
was written before that pod started, so deliveries made by other pods since then are invisible, with
no sweep or notification that closes the gap without leaving a residual window.

Reading the shared store per delivery is correct without a local cache because the broker holds
dispatch until in-flight messages are acknowledged during a rebalance (ADR 0022), so the store is
current at the moment dispatch resumes.

The read is a primary-key lookup, so it is O(1) per delivery, at the call-rate cost recorded in ADR
0009. An in-process cache or a distributed map can be added later if throughput makes it necessary,
without changing the correctness argument.

A packed encoding would impose hard bit-width limits, sixteen bits caps chunk ids at 65 535 and
million-record jobs exceed that, and it risks diverging from the SQL comparison. Three plain integer
fields in the payloads cost nothing.

An exact retransmit is the stale-recovery case, where the same item is dispatched again after a
crash. Skipping it would suppress a delivery that may never have reached the target.

This is the only thing ordering versions of one record. A sink that neither reads nor advances a
watermark row has nothing protecting the records it delivers from being written out of order, which
is why taking part is mandatory and opting out is deliberate (ADR 0014).
