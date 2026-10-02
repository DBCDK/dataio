# 12. Compose the watermark key in job-store as an opaque agency-qualified token

Date: 2026-08-24

## Status

Accepted

Implemented under DI-3000.

## Context

The watermark is keyed per record (ADR 0011), so something has to say which record an item is for.

Leaving it to each sink looks reasonable, on the assumption that a sink can parse its own record's
identity back out of the content it delivers as reliably as job-store does. It cannot in general.
`RecordInfo`'s constructor strips whitespace from the id, so job-store's id, and therefore the key
it compares against, is normalised while the raw bytes in the payload are not. A sink re-deriving
the key would read `" 4 2 "` where job-store stored `"42"`, the lookup would silently never match,
and stale-delivery detection would quietly stop working for that record with no error anywhere.

A record id is also only unique within the agency that assigned it. The existing dependency-tracking
system encoded that one level removed, scoping a match key to a submitter, so two agencies' records
sharing an id simply did not interact.

## Decision

Compose the key in job-store, in the one place that already carries the record's id onto the
message, as `<agencyId>:<RecordInfo id>`, and carry it as a message header.

Treat it as a single opaque token everywhere downstream: one column in the watermark table's primary
key, one query parameter, one connector argument. Read it in the sink framework rather than per
sink.

Keep the sink dimension out of the token. A watermark row is identified by the pair
`(sink_id, record_key)`, and the sink half stays a separate value at every hop.

Take the agency from the job specification's submitter id, which is the business library number
rather than any database key.

## Consequences

Every sink compares the identical, canonically normalised key job-store itself would compare
against, and the class of bugs that comes from deriving one value two different ways is gone. Adding
a new sink no longer means getting record-key derivation right for it.

The agency qualification is what stops a wrong answer rather than an unhelpful one. Without it the
table's primary key would assert stronger uniqueness than the data has, and two unrelated records
from two agencies sharing an id would collide into one row, with the older job's record wrongly
reported as superseded. That is the opposite of the harmless over-grouping accepted for the
correlation key (ADR 0010), which is why only one of the two carries the agency.

Composition is injective without escaping, because the agency is a decimal number whose string form
cannot contain a colon, so the first colon is always the delimiter even though the record id is
arbitrary harvested data.

An explicit agency column in the primary key would make the dimension visible in the schema and give
a plain index seek for "every row for agency X". It was rejected because it turns "which dimensions
make a watermark key" from a job-store concern into a sink framework concern too: the REST path, the
connector signatures and every key read would need a second parameter. If ad hoc queries across
agencies turn out to be needed, add denormalised columns alongside the opaque key rather than
restructuring the key.

The sink dimension is a separate column where the agency dimension is folded into the token, and the
asymmetry is deliberate rather than an oversight. The agency qualifies the record's identity, since
a record id means nothing without knowing who assigned it, so it belongs inside the value that names
the record. The sink is not a property of the record at all. One record delivered to rawrepo and to
tickle has two independent delivery histories, and what one sink holds says nothing about what the
other does, so the sink names which history is being asked about rather than which record.

The sink half therefore travels on its own the whole way: `JMSHeader.sinkId` on the message, a path
segment in `GET /sinks/{sinkId}/watermarks`, and its own argument in `getWatermark(sinkId,
recordKey)` and `withWatermarkKey(sinkId, recordKey)`. A sink reads its own id off the message and
composes nothing, so the token is the same string whichever sink handles the item.

Keeping the token opaque also keeps sinks from depending on its structure by accident. Every
semantic the framework knows about the key's internals is one some sink's delivery code could come
to rely on.

The key travels as a query parameter rather than a path segment, because a record id can contain a
slash or a percent. A literal slash breaks route matching outright and returns a silent 404
indistinguishable from "no watermark exists", and a percent-encoded one is rejected or normalised by
some servlet container configurations before it reaches application code.
