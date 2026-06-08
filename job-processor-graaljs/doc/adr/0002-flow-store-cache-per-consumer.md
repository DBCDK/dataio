# 2. Flow store cache per consumer

Date: 2026-06-08

## Status

Accepted

## Context

Each consumer thread runs JavaScript business logic by calling `GraalJsScript.invoke()`, which executes 
inside a GraalVM `Context`. GraalVM contexts are not thread-safe, so each consumer thread needs its own compiled
`GraalJsScript`. Since all threads could process the same flow, a shared synchronized cache
would serialize all JS execution — defeating the point of multiple consumer threads.

## Decision

Each consumer thread owns its own `FlowCache` (keyed by `flowId.flowVersion`), giving a
lock-free JS execution path.

## Consequences

Memory scales with threads × distinct active flows, which is expected to be negligible in practice.
