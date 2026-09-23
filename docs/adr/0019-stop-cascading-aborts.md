# 19. Stop cascading aborts to dependent jobs

Date: 2026-09-08

## Status

Accepted

Implemented under DI-3021.

## Context

Aborting a job aborted every job that depended on it. The abort path asked the dependency table for
the distinct jobs whose `waitingOn` named a chunk of the aborted job and recursed into each, with a
loop-detection set to stop it coming back round.

The reason was sound while the graph existed. A chunk blocked on a chunk of an aborted job would
never be unblocked, because the delivery that would have cleared it never happens, so the dependent
job would stall for good. Aborting it too was the lesser evil.

## Decision

Remove the cascade with the graph, and do not replace it. Abort one job.

## Consequences

Nothing holds a later job back that way any more. The only cross-job hold left is the per-job gate,
and the abort path lifts the aborted job's barrier and re-triggers the jobs queued behind it, which
releases them rather than aborting them (ADR 0006).

This is a deliberate behaviour change and an improvement: aborting one job stops taking unrelated
later jobs with it. The abort entry point returns one job rather than a stream, which is visible to
its callers.
