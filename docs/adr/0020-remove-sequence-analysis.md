# 20. Remove sequence analysis, and leave the flow-store field in place

Date: 2026-09-09

## Status

Accepted

Implemented under DI-3022.

## Context

Every chunk carried a `SequenceAnalysisData`, the full set of record keys its items covered, stored
as a column on the chunk row and copied into the scheduler's own row. Its sole consumer was the call
that built the dependency graph, which ADR 0002 removed. Nothing has read the keys since.

A key generator interface, its single implementation, a per-chunk option on `SinkContent` that
steered the computation, and the plumbing that carried the option down into partitioning all exist
to produce that one value.

## Decision

Remove all of it: the key set and its converter, the key generator interface and its implementation,
the option on `SinkContent`, the per-record key set accessor, and the plumbing between them. Drop
the chunk column in the same migration.

Keep `getCorrelationKey()`, which becomes the only key job-store derives from a record.

Leave `sequenceAnalysisOption` in the sink definitions already stored in flow-store, and make no
migration there.

## Consequences

No key is computed per chunk and nothing in the scheduler holds a set of them. The option's removal
also takes away a job lookup performed once per chunk during partitioning purely to read it.

The per-record key set accessor is removed rather than reduced to a no-argument method, which would
have no caller.

Flow-store needs no change because it stores a sink definition as the JSON it was posted as and
unmarshals only to validate, and the type ignores unknown properties, so an existing stored
definition and one posted by a client still sending the field both keep validating. The member
survives in the stored content and stops being read. Stripping it from live flow configuration would
be a write against every sink definition for no reader's benefit.

Dropping the column exposes one window per deploy. It is `NOT NULL` and the previous build names it
in every chunk insert, so an instance still partitioning when the migration runs fails its next
chunk transaction. Nothing is lost, because the interrupted-partitioning reset returns the queue
entry to waiting and partitioning resumes from the job's chunk count, but that reset runs at
instance startup rather than on a timer, so the deploy belongs at a quiet partitioning moment.

Splitting the drop across two releases, one relaxing the constraint and a later one dropping the
column, was considered and rejected. It closes a one-window exposure at the price of leaving a dead
column in the schema across a release boundary, and the earlier migration in this series took the
same window for the two columns it dropped.
