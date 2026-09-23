# 5. Make barrier width a property of the sink type

Date: 2026-09-07

## Status

Accepted

Implemented under DI-3076.

## Context

The `waitingOn` barrier was full width. Every data chunk of a later same-submitter job on the same
sink waited for the earlier job's termination chunk, so nothing at all from job B reached the sink
before job A's job-end had been delivered.

The per-job gate (ADR 0004) is termination width. Only termination chunks are gated, so the
guarantee shrinks to "job B's job-end does not reach the sink before job A's". For ordering two
versions of one record that is deliberate and sufficient, since the broker grouping and the
watermark cover it. It is not sufficient for job-end work whose correctness depends on no later
job's data having landed yet.

Tickle in `TOTAL` mode is exactly that shape. A total ingest expresses "everything the source no
longer sends is gone" by marking every record in the dataset when a job's first item arrives, and
deleting everything still marked when its job-end arrives. Neither step is scoped to a batch, but a
batch belongs to one job. Under termination width, job B's first item starts a second pass over the
same dataset while job A's job-end is still gated: B's mark undoes what A just wrote, A's delete
then removes everything B has not yet rewritten, and the dataset ends up silently short.

Marcconv and periodic-jobs finalize by job id and deliver one job's records, so termination width is
enough for them.

## Decision

Make width a property of the sink type. `JobSchedulerBean.REQUIRES_FULL_WIDTH_BARRIER` holds the
full-width types, currently `TICKLE` alone, as a subset of `REQUIRES_TERMINATION_CHUNK`. For those
types a data chunk is inserted with its gate closed when an earlier job in the same barrier scope
still holds an unlifted barrier.

Keep the set in code next to `REQUIRES_TERMINATION_CHUNK` rather than adding a field to
`SinkContent`, and carry it on no column.

Read the set in exactly one place, the data chunk's insert. Run the re-trigger that reopens data
chunks for every sink type rather than consulting the set again.

## Consequences

Scoping tickle's mark and sweep to a batch instead was considered and rejected. Total-ingest delete
detection is inherently dataset-wide, `tickle-repo-api` is shared with the tickle harvester and
other consumers, and the change would move a correctness guarantee out of the scheduler, where every
other cross-job ordering rule lives, into one sink.

Width follows from what the sink implementation does at job end, not from an operator choice, which
is why it is not configuration. `SinkContent` is cached per job, so a new field there would need a
defensible default for every cached sink already written.

No column is needed because the sink type is fixed per sink and every gate statement is already
scoped to one sink and submitter.

Running the re-trigger unconditionally is identical in effect, since only the data chunk's insert
ever closes a data chunk's gate, and it keeps the sink type off the delivery path entirely. It also
reopens a row closed by an earlier deployment, or by a sink whose type has since changed.

A closed gate becomes job-sized rather than one synthetic chunk per job, so the direct dispatch path
has to filter on the gate rather than merely order by the dispatch key. Full width holds delivery
only: chunks are still processed and accumulate in an uncapped state, so a large tickle job queued
behind another is no more expensive to hold than it was.
