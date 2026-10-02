# 13. Report a delivery under one of four verdicts

Date: 2026-09-03

## Status

Accepted

Implemented under DI-3017.

## Context

Per-item dispatch moved where the delivering counters come from. The chunk path read them off each
item's `ChunkItem.Status` as the chunk was recorded, mapping failure to failed, ignore to ignored
and success to succeeded. The per-item path has one report per item, and that report also decides
whether the record's watermark advances.

Sinks routinely do not send an item. A processing outcome that was itself a failure or an ignore is
passed through, and the framework itself skips an item the watermark says has been overtaken.

With three values, such an item has to be reported as delivered.

## Decision

Report each item under one of four verdicts: `DELIVERED` for an item sent to the target, `SUPERSEDED`
for one not sent because a newer version of the record already was, `IGNORED` for one not sent
because there was nothing to send, and `FAILED` for one attempted and rejected in a way retrying
will not fix.

Count `DELIVERED` as succeeded, `FAILED` as failed, and both `SUPERSEDED` and `IGNORED` as ignored.
Advance the watermark on `DELIVERED` alone.

Take the verdict from the returned result rather than deriving it from the returned
`ChunkItem.Status`.

Let only the framework return `SUPERSEDED`.

## Consequences

Reporting a not-sent item as delivered would be wrong twice over. It counts the item as succeeded,
overstating what reached the target in the completion mail that prints the succeeded count. And it
advances the record's watermark, claiming a version as delivered that never was, so a genuinely
older version arriving afterwards is judged stale and skipped, leaving the target with neither. Both
halves follow from the verdict, so one value fixes both.

Returning delivered with an ignore item, the nearest thing the three-value form allows, preserves
the item's visible outcome, which is stored verbatim, and neither its contribution to the counters
nor its effect on the watermark.

`SUPERSEDED` and `IGNORED` are indistinguishable to job-store. They are two values because they are
two different answers to "why is this record not at the target", which is the question asked when
investigating one. The split by author is documented rather than enforced: a sink does not read the
watermark and so cannot detect supersession, but a sink returning it wrongly produces the same
counter and the same watermark behaviour as `IGNORED`, so the mistake is misleading in metrics and
harmless in effect, and enforcing it would add a failure mode to catch something that cannot corrupt
anything.

The verdict is deliberately not derived from the returned `ChunkItem.Status`. Each verdict means one
specific thing for the delivering phase, where `ChunkItem.Status` is a general-purpose outcome
reused across partitioning, processing and delivering, and coupling them would tie job-store's
counting to whatever a sink's status happens to be for unrelated reasons. The item itself still
travels with the verdict, because job-store stores it verbatim and the status alone leaves the sink
no way to say what it delivered.

Whether `FAILED` should advance the watermark was weighed and answered no. That leaves one case
open: a newer version is dispatched first, fails at the target, and an older version behind it then
passes the check and is delivered, regressing the target with no newer write coming. Advancing on
failure would suppress the older delivery instead, leaving the target unchanged and the failure
visible in the job state. It remains a defensible alternative if that case is ever observed to
matter.

A failure the target will not recover from has to be returned as `FAILED` rather than thrown.
Throwing rolls the session back and has the item redelivered.
