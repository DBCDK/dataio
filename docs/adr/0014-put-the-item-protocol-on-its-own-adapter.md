# 14. Put the item protocol on a separate sink adapter, with an opt-out

Date: 2026-08-31

## Status

Accepted

Implemented under DI-3002.

## Context

The watermark check and the result report bracket every delivery, and repeating them in fifteen
sinks would mean fifteen chances to get the protocol wrong. They belong in the shared sink
framework.

`MessageConsumerAdapter` is not the right place for them. Two of its subclasses consume whole chunks
permanently and will never deliver an item: `job-processor2`'s consumer, which still receives chunk
messages, and `dlq-errorhandler`, which handles both shapes.

Not every sink has per-record supersession to enforce either. `periodic-jobs` and `marcconv` treat
an item as input to work carried out when the job ends rather than as a record delivered to a target
on its own, so there is no "newer version already delivered" question to ask about them.

## Decision

Add `SinkMessageConsumerAdapter` as a subclass. It reads the watermark key off the message, performs
the check, calls one abstract `deliverItem`, and reports the result. `handleConsumedMessage` is
final there.

Have `deliverItem` return an `ItemDeliveryResult` carrying the verdict and the item, with the sink
building its half and the framework adding the watermark key.

Give it `usesDeliveryWatermark()`, defaulting to true, for the sinks that opt out.

Put the two watermark calls on the existing `JobStoreServiceConnector` rather than adding a
connector of their own.

Pass `ConsumedMessage` and the unmarshalled item to `deliverItem` rather than the raw JMS message.

## Consequences

Putting the protocol on the shared base would leave both chunk consumers inheriting a `deliverItem`
that is meaningless for them and could never be made abstract. With the split, it is abstract from
the start, the fifteen sinks extend the new class, and a sink cannot forget the protocol because it
does not implement the method that runs it.

The watermark key is read once, by the framework, so no sink names one (ADR 0012), and no lock is
needed around the check because the broker guarantees same-key items are never processed
concurrently.

An opted-out sink skips the read, delivers every item unconditionally and advances nothing, which is
the same path an item with no record key already takes, so the rest of the protocol needs no second
branch. What it does not skip is reporting: the delivering counters and the per-job gate are driven
by those reports, and a job whose items are never reported never completes.

Opting out is safe for these two sinks because their job-end work runs against complete data by
construction. `deliverItem` commits its own transaction and returns before the result is reported,
so a reported item is one whose writes are durable, and the termination chunk is released only once
every data chunk has reported. The chunk protocol had this the other way round, reporting before
committing, which `periodic-jobs` covered with a fixed sleep before finalizing.

A separate watermark connector was weighed and rejected. Putting the calls on the connector every
sink already holds needs no change to `ServiceHub` and no second connector to wire in.

`ConsumedMessage` is what validation already produces on the path to the handler, it carries every
property plus the body, and it needs no JMS provider in a test. The body is unmarshalled once, by
the framework, so the sink does not parse it a second time.

A message the broker gives up on is reported per item as well, by `dlq-errorhandler`, which fails
the item on the job's behalf. No sink reports an item the broker stopped delivering, so without that
branch the counters would never reach their total and the job would hang unfinished with nothing
saying why.
