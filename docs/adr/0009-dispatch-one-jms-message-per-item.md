# 9. Dispatch one JMS message per item

Date: 2026-08-26

## Status

Accepted

Implemented under DI-3000 and DI-3001.

## Context

Job-store sent one JMS message per chunk, carrying up to ten items, and the sink delivered the whole
chunk and reported one result for it.

Neither half of the replacement for the dependency graph works at that granularity. The broker
serialises on a group id carried by a message, and a chunk holds items for up to ten different
records, so a chunk message has no single group to belong to. The watermark compares one record's
position against what has already been delivered for that record, which is a per-item question.

## Decision

Send one message per item. The body is the item's processing outcome alone, so every sink sees one
shape, and the message carries `itemId` and `recordKey` alongside the identifiers a chunk message
already carries.

Carry every routing and validation header the chunk message carries onto each item message. Give
item messages a payload type of their own rather than reusing the chunk's.

Send the items of a chunk in ascending `itemId` order, from a query that states that order, through
a single `JMSContext`.

Set `trackingId` from the item's own tracking id rather than from the chunk's.

## Consequences

The header list is the existing contract rather than a new one, and the point of restating it is
that the per-item send path is a second producer that has to satisfy it. The failure modes are
silent rather than loud: a missing payload type has the message discarded with a warning and no
retry, and a missing sink or flow-binder reference is an NPE on every message in the sinks that
unbox it.

A distinct payload type does not let a sink built for the chunk protocol cope with an item message.
No sink handles both shapes (ADR 0023). What it buys is a diagnostic naming the actual cause, rather
than a Jackson error about invalid chunk JSON.

Send order becomes a contract. Two items of one chunk can share a correlation key, as two versions
of one record or as any two records of one hierarchy, and the broker delivers a group in send order,
so sending out of order would have the watermark judge the newer version stale. Ascending item id
has always been what the chunk query produced, as an incidental property of how it built a chunk
rather than as a stated one.

One `JMSContext` makes the enlisted XA session show either all of a chunk's items or none, so a
partially dispatched chunk is not a state a sink can observe.

The item's own tracking id is what ties one record's log lines together across harvester, job-store,
processor and sink, and it already exists for that purpose. The chunk's tracking id restates two
numbers the message already carries, which was harmless while one message meant one chunk and
becomes a run of identical lines naming neither the item nor the record once it is repeated per
item.

HTTP call volume against job-store rises by roughly twenty times, one read and one write per item
where there was one write per chunk, plus one watermark upsert per item on PostgreSQL. Job-store
instances and their connection pools have to be capacity-planned for it. A batched watermark read
is available if it is ever needed and changes no part of the correctness argument.

The abort path is untouched. It sends a chunk-level message handled before validation, and its
discard check keys on job id alone, so it filters individual item messages with no change.
