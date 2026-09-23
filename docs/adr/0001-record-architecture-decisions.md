# 1. Record architecture decisions

Date: 2026-09-23

## Status

Accepted

## Context

The chunk scheduling redesign replaced the mechanism that orders chunk delivery across the whole of
dataio. Its reasoning lives in two design documents, `docs/chunk-scheduling-redesign.md` and
`job-store-service/dependency-tracking.md`, which describe how the system works rather than why each
choice was made over the alternatives that were considered and rejected. Those documents are kept in
the present tense and are rewritten as the system changes, so an argument recorded in them is lost
the moment the mechanism it justifies is described differently.

`job-processor-graaljs/doc/adr` already records decisions this way for that component.

## Decision

We will use Architecture Decision Records, as [described by Michael
Nygard](http://thinkrelevance.com/blog/2011/11/15/documenting-architecture-decisions), for decisions
that span more than one component of dataio, keeping them in `docs/adr`.

A record states the decision, the alternatives weighed against it and what the choice costs. The
design documents keep the description of the mechanism, and neither duplicates the other.

## Consequences

See Michael Nygard's article, linked above. For a lightweight ADR toolset, see Nat Pryce's
[adr-tools](https://github.com/npryce/adr-tools).

Records 0002 to 0022 are drawn from the two design documents named above. Each states the behaviour
that is in place and what it replaced, and is dated at the change that put it there.
