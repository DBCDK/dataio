# 4. Hold a termination chunk with a per-job counter gate

Date: 2026-09-02

## Status

Accepted

Implemented under DI-3018, DI-3019 and DI-3049.

## Context

Sink types that do end-of-job work append a synthetic termination chunk to each job, and it must not
reach the sink before every data chunk of that job has been delivered. The dependency graph did this
with a `waitingOn` set naming the chunks the termination chunk was held behind, which is the
mechanism ADR 0002 removes.

The obvious replacement is a counter: hold the termination chunk until as many data chunks have been
acknowledged as the job has. Partitioning and delivery overlap, which is what makes it harder than
it sounds. Chunks are scheduled from inside the partitioning loop, so a job's data chunks are
processed and delivered while its later chunks are still being created, and the job's total is not
known until partitioning ends.

## Decision

Carry the verdict as `gate_open` on the chunk's `dependencytracking` row, and decide it from
`data_chunks_delivered` and `data_chunks_expected` on the `job` row. Termination chunks are inserted
with the gate closed. Both dispatch paths refuse a chunk whose gate is closed, and a held chunk
waits in `SCHEDULED_FOR_DELIVERY`, which is uncapped.

Evaluate a job's own gate at two sites, not one: when a data chunk is delivered, and when the
termination chunk is inserted. Write `data_chunks_expected` in the transaction that inserts the
termination row, passing the count down rather than writing it in the caller.

Condition the delivery count on the `DELETE` of the chunk's row, in the same transaction, so the row
removal is the token that makes the count once-only.

Keep `gate_open` `NOT NULL DEFAULT TRUE`, and let only a write that means to close a gate touch the
column.

## Consequences

Both evaluation sites are needed in the general case rather than one covering for the other. A job
whose data chunks all finish before partitioning ends leaves no delivery to fire the delivery-side
site, so the insert is the only place its gate can be decided. A job with no data
chunks at all is that same case rather than a special one.

The delivery count is unconditional and lands in one atomic statement,
`data_chunks_delivered = data_chunks_delivered + 1`. Conditioning it on the job already having a
termination chunk would drop every chunk delivered before that chunk existed, which for a fast sink
on a large file is most of the job, and writing it as a read followed by a write would let a
concurrent update be lost. Either shape would leave the counter permanently below the total with no
delivery left to arrive.

The chunk row's `DELETE` is what makes the count once-only, and it guards the more damaging of the
two hazards. The delivery callback is invoked again by every redelivery, and the broker's failure
detection can produce two concurrent calls for one chunk, so a count taken per call would run twice.
Twice counted it still reaches the total exactly, but while data chunks are in flight, which
dispatches the termination chunk ahead of them and runs job-end work against an incomplete set. The
`DELETE` returns a row to exactly one caller whatever the interleaving, and the count commits in that
same transaction.

The counters are compared with `>=` rather than `==`, and that is defensive only. It mitigates
neither hazard above, since a double count reaches the total exactly and a lost update stays below
it. What it covers is the total being revised or backfilled by something other than the increment.

`data_chunks_expected = 0` carries two opposite meanings, the migration default on jobs that predate
the gate and a genuine job with no data chunks. The gate keys on `is_termination` to tell them
apart, and reads the counter only as a total to compare against.

The count runs in the outer callback transaction rather than the one that writes the item result, so
it commits before the response is written and no sink can acknowledge a message whose count has not
committed.

What the gate does not do is order two versions of one record. It orders whole jobs and nothing
finer (ADR 0011).
