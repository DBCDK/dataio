# 8. Back the gate with an idempotent sweep, reachable on demand

Date: 2026-09-07

## Status

Accepted

Implemented under DI-3076.

## Context

A gate that closes and is never reopened is a job that never completes. There is one way that
happens: a termination row removed without its barrier being lifted. The abort path, the recheck
path and `JobPurgeBean` all remove rows, and the re-trigger is edge-triggered, so a lift that fails
or is skipped on any of those paths leaves the job reading as still blocking with nothing left to
fire on.

Widening the gate to full width (ADR 0005) adds the same exposure for data chunks, in two reachable
cases rather than one. The two halves are one mechanism: without the sweep, widening the gate
strands jobs.

The equivalent case under the dependency graph was stale keys left in a later job's `waitingOn`, and
the hourly `recheckBlocks` sweep is what released them.

## Decision

Extend that same hourly sweep to the gate. It lifts the barrier of any job left holding one with no
termination row, then opens any gate closed with no earlier unlifted barrier, requiring additionally
for a termination chunk that its own job's data chunks are delivered.

Derive that last condition from the absence of the job's data-chunk rows rather than from
`data_chunks_delivered`.

Sweep one barrier scope at a time, each in its own transaction under that scope's advisory lock.

Expose the same sweep at `POST dependency/gate_sweep`.

## Consequences

An hour bounds the damage of any path that is ever added and misses the lock or the lift, which is
what makes the sweep the backstop rather than the mechanism. An hour is also a long time to hold a
job that cannot complete, which is why it is reachable on demand. The call runs exactly what the
hourly pass runs, in the same order, and is safe at any time: it only ever opens a gate whose reason
to be shut is already gone.

Opening a termination chunk's gate on the earlier barrier alone would dispatch it while its own data
chunks were still in flight, which is what the per-job gate exists to prevent in the first place.

The sweep reads "its own data chunks are delivered" from the absence of the job's data-chunk rows
rather than from `data_chunks_delivered`. The rows are the more direct evidence, the check costs one
indexed probe either way, and a sweep whose job is to repair gate state does not want a counter
deciding its verdict.

Per-scope transactions keep the advisory lock held per scope rather than for the whole sweep, so an
on-demand call does not stall delivery acknowledgements across every scope it visits.

The sweep needs no knowledge of sink type. Only the data chunk's insert closes a data chunk's gate
and it runs only for full-width sinks, so the existence of such a row is the answer.
