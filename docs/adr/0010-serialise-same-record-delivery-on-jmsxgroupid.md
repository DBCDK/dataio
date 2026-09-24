# 10. Serialise same-record delivery on `JMSXGroupID`, with one group per hierarchy

Date: 2026-08-13

## Status

Accepted

Implemented under DI-2993, DI-2994 and DI-3059.

## Context

Queue order is not delivery order. A sink runs several consumer threads per pod across several pods,
so one thread can pick up an item from job 50 while another picks up an item for the same record
from job 100, and either can reach the target first. That race is what `BLOCKED` prevented by
keeping only one chunk per record in flight (ADR 0002).

Artemis pins a message group to one consumer and delivers the group in send order, which removes the
race for any two items that share a group id.

MARC hierarchies need more than same-record ordering. For live records the parent must arrive at the
target before the child, and for delete-marked records the child must arrive before the parent, so
two different records have to be ordered against each other. The watermark cannot express that: a
head and its volumes are different records with different keys.

## Decision

Add `correlationKey` to `RecordInfo`, derived from the record's own type, and set it as
`JMSXGroupID` on each item message. A standalone record takes its own id. Every record that is part
of a hierarchy, head, section or volume alike, takes one shared constant, so all of them serialise
into a single broker group per sink queue and the dispatch order (ADR 0003) decides which arrives
first.

Give a chunk containing a live head or section record the highest scheduler priority, so the head
reaches the sink before a volume from a newer job. Chunks containing delete-marked head or section
records keep their priority.

Leave the correlation key un-qualified by agency.

## Consequences

Hierarchical records are delivered one at a time per sink. They are a small fraction of the
workload, and the trade buys something substantial: the key derives from the record's own type
alone, so no section-to-head resolution is needed anywhere and the partitioners need no hierarchy
lookups and no mapping table. Detecting a live head or section is the same, the record's own type
and delete flags.

Should the single group ever become a measured bottleneck, grouping can be refined to the head
record's id without changing the protocol, at the price of re-introducing section-to-head resolution
in the partitioners. The constant has to be one that cannot collide with a real record id.

Over-grouping only costs latency, which is why the correlation key is not agency qualified. Two
agencies' records sharing a literal id are serialised against each other unnecessarily, the same
benign collision already accepted for the broker's bucket hashing. The watermark key needs the
agency dimension for the opposite reason (ADR 0012).

One gap is accepted. Broker serialisation covers two versions of a record only while they carry the
same correlation key, so a record that switches between standalone and hierarchical type between
jobs lands in two groups and can be processed concurrently on two pods. Both can then read a
watermark older than both versions and both deliver. It needs the same record in flight in two jobs,
a type change between them, and a sub-second interleave.

The semantics are an Artemis extension. JMS 2.0 names the property without mandating consumer
pinning or serial dispatch, so replacing the broker means verifying the replacement provides
equivalent grouping before relying on ordering.
