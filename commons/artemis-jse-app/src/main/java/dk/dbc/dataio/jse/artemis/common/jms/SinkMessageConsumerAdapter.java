package dk.dbc.dataio.jse.artemis.common.jms;

import dk.dbc.commons.jsonb.JSONBContext;
import dk.dbc.commons.jsonb.JSONBException;
import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.types.ConsumedMessage;
import dk.dbc.dataio.commons.types.exceptions.InvalidMessageException;
import dk.dbc.dataio.commons.types.jms.JMSHeader;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnectorException;
import dk.dbc.dataio.jobstore.types.ItemDeliveryResult;
import dk.dbc.dataio.jobstore.types.Watermark;
import dk.dbc.dataio.jse.artemis.common.ItemDeliveryException;
import dk.dbc.dataio.jse.artemis.common.Metric;
import dk.dbc.dataio.jse.artemis.common.service.ServiceHub;
import dk.dbc.log.DBCTrackedLogContext;

import java.util.Optional;

import static dk.dbc.dataio.jse.artemis.common.Metric.ATag.destination;
import static dk.dbc.dataio.jse.artemis.common.Metric.ATag.status;

/**
 * Base class for sinks delivering one item at a time, handling the delivery watermark
 * check and the result reporting around each item so that a sink implements nothing but
 * {@link #deliverItem(ConsumedMessage, ChunkItem)}
 * (see docs/chunk-scheduling-redesign.md).
 * <p>
 * The sequence per item is: read the watermark, skip the item when an equal-or-newer
 * version of the same record has already been delivered, deliver, report the result, and
 * only then let the JMS session commit. Reporting before the commit is what makes a crash
 * between the two harmless: the message is redelivered and the idempotency guard on the
 * report endpoint absorbs the duplicate.
 * <p>
 * Every result names the watermark row it concerns, whatever its outcome, and the delivery
 * endpoint advances that row only for a DELIVERED one. A null key means the item has no
 * watermark row at all, never that this particular outcome must not advance it.
 * <p>
 * No delivery state is held locally. Every decision reads the shared watermark, since a
 * per pod cache cannot see what other pods delivered after it was populated, and the
 * broker's {@code JMSXGroupID} grouping guarantees that two versions of the same record
 * are never processed concurrently anyway.
 * <p>
 * Sinks with no bibliographic record identity to protect, such as those aggregating a
 * whole job before delivering anything, opt out with {@link #usesDeliveryWatermark()}.
 * <p>
 * The watermark key is taken from {@link JMSHeader#recordKey} rather than re-derived from
 * the delivered content, and no sink is given a say in it: {@code RecordInfo} whitespace
 * normalizes the record ID, so a key composed from raw record bytes can differ from the
 * one job-store composed and compares against. Nothing would report that mismatch - the
 * watermark lookup would simply never match, and stale delivery detection would stop
 * working for that record.
 * <p>
 * {@link MessageConsumerAdapter} is deliberately left as the base for consumers of whole
 * chunks, which job-processor and dlq-errorhandler remain.
 */
public abstract class SinkMessageConsumerAdapter extends MessageConsumerAdapter {
    private final JSONBContext jsonbContext = new JSONBContext();

    public SinkMessageConsumerAdapter(ServiceHub serviceHub) {
        super(serviceHub);
    }

    @Override
    public final void handleConsumedMessage(ConsumedMessage consumedMessage) throws InvalidMessageException {
        String payloadType = JMSHeader.payload.getHeader(consumedMessage, String.class);
        if (!JMSHeader.ITEM_PAYLOAD_TYPE.equals(payloadType)) {
            throw new InvalidMessageException(String.format("Message<%s> payload type %s != %s",
                    consumedMessage.getMessageId(), payloadType, JMSHeader.ITEM_PAYLOAD_TYPE));
        }
        Watermark incoming = new Watermark(
                header(consumedMessage, JMSHeader.jobId, Integer.class),
                header(consumedMessage, JMSHeader.chunkId, Long.class).intValue(),
                header(consumedMessage, JMSHeader.itemId, Short.class));
        long sinkId = header(consumedMessage, JMSHeader.sinkId, Long.class);
        ChunkItem item = unmarshallItem(consumedMessage);

        // A null record key means no watermark row to check and none to advance, either
        // because this sink opted out of the watermark, or because the item has no record
        // identity, as the job termination item has not.
        String recordKey = usesDeliveryWatermark()
                ? JMSHeader.recordKey.getHeader(consumedMessage, String.class)
                : null;

        ItemDeliveryResult result = deliverUnlessSuperseded(consumedMessage, item, incoming, sinkId, recordKey);
        report(result.withWatermarkKey(sinkId, recordKey), incoming);
    }

    /**
     * Delivers the item unless the watermark row it belongs to already names an
     * equal-or-newer version of the same record
     *
     * @param recordKey watermark key of the item, or null when no watermark row applies to
     *                  it, in which case the item is delivered unconditionally
     * @return outcome to report, without the watermark key the caller adds to it
     */
    private ItemDeliveryResult deliverUnlessSuperseded(ConsumedMessage message, ChunkItem item, Watermark incoming,
                                                       long sinkId, String recordKey) {
        if (recordKey == null) {
            return deliver(message, item);
        }
        Watermark delivered = getWatermark(sinkId, recordKey);
        if (delivered != null && incoming.compareTo(delivered) < 0) {
            return ItemDeliveryResult.of(ItemDeliveryResult.Status.SKIPPED,
                    supersededItem(item, incoming, delivered));
        }
        return deliver(message, item);
    }

    /**
     * Delivers a single item to the target system of this sink
     * <p>
     * Called only for items the delivery watermark has not already superseded. Throwing
     * rolls the JMS session back and has the item redelivered, so a failure the target
     * will not recover from on its own must be returned as
     * {@link ItemDeliveryResult.Status#FAILED} rather than thrown, or the item is retried
     * for as long as the broker allows.
     *
     * @param message message carrying the item, for the sinks reading their own headers
     *                off it
     * @param item    processing outcome to deliver, unmarshalled from the message body
     * @return outcome of the delivery, as {@link ItemDeliveryResult#of(ItemDeliveryResult.Status, ChunkItem)}.
     * The returned chunk item is stored verbatim as the item's delivering outcome
     * @throws Exception when the delivery attempt should be retried
     */
    protected abstract ItemDeliveryResult deliverItem(ConsumedMessage message, ChunkItem item) throws Exception;

    /**
     * Whether deliveries from this sink are subject to the delivery watermark
     * <p>
     * Overridden to false by sinks with no per-record supersession to enforce, typically
     * those where an item is not a record delivered to a target on its own, but input to
     * work carried out when the job ends. Such a sink delivers every item unconditionally
     * and never advances a watermark, but still reports a result per item, since the phase
     * counters and the per-job gate in job-store are driven by those reports.
     *
     * @return true to check and advance the watermark, false to opt out entirely
     */
    protected boolean usesDeliveryWatermark() {
        return true;
    }

    private ItemDeliveryResult deliver(ConsumedMessage message, ChunkItem item) {
        try {
            DBCTrackedLogContext.setTrackingId(item.getTrackingId());
            return deliverItem(message, item);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new ItemDeliveryException("Error while delivering item " + item.getId(), e);
        } finally {
            DBCTrackedLogContext.remove();
        }
    }

    private Watermark getWatermark(long sinkId, String recordKey) {
        try {
            Optional<Watermark> watermark = jobStoreServiceConnector.getWatermark(Math.toIntExact(sinkId), recordKey);
            return watermark.orElse(null);
        } catch (RuntimeException | JobStoreServiceConnectorException e) {
            throw new ItemDeliveryException("Error while reading watermark for record " + recordKey, e);
        }
    }

    private void report(ItemDeliveryResult result, Watermark incoming) {
        try {
            jobStoreServiceConnector.addItemDelivered(result, incoming.jobId(), incoming.chunkId(), incoming.itemId());
        } catch (RuntimeException | JobStoreServiceConnectorException e) {
            throw new ItemDeliveryException(String.format("Error while reporting %s result for item %d/%d/%d",
                    result.status(), incoming.jobId(), incoming.chunkId(), incoming.itemId()), e);
        }
        Metric.dataio_item_delivery_count.counter(destination.is(getFQN()), status.is(result.status().name())).inc();
    }

    /**
     * The delivering outcome recorded for an item a newer version of the same record has
     * already superseded, synthesized here so that every sink reports supersession the
     * same way, and so that the job view names the version that won.
     */
    private ChunkItem supersededItem(ChunkItem item, Watermark incoming, Watermark delivered) {
        return new ChunkItem()
                .withId(incoming.itemId())
                .withStatus(ChunkItem.Status.IGNORE)
                .withType(ChunkItem.Type.STRING)
                .withTrackingId(item.getTrackingId())
                .withData(String.format("Item was skipped, version %d/%d/%d of this record is already delivered",
                        delivered.jobId(), delivered.chunkId(), delivered.itemId()));
    }

    private ChunkItem unmarshallItem(ConsumedMessage message) throws InvalidMessageException {
        try {
            return jsonbContext.unmarshall(message.getMessagePayload(), ChunkItem.class);
        } catch (JSONBException e) {
            throw new InvalidMessageException(String.format("Message<%s> payload was not a valid ChunkItem",
                    message.getMessageId()), e);
        }
    }

    /**
     * Reads a header the item delivery protocol can not proceed without, as an invalid
     * message rather than as the NullPointerException unboxing a missing header would
     * otherwise raise. An invalid message is discarded with a warning, where the
     * NullPointerException would have the message redelivered for as long as the broker
     * allows, and it can never become valid.
     */
    private <T> T header(ConsumedMessage message, JMSHeader header, Class<T> type) throws InvalidMessageException {
        T value = header.getHeader(message, type);
        if (value == null) {
            throw new InvalidMessageException(String.format("Message<%s> has no %s property",
                    message.getMessageId(), header.name));
        }
        return value;
    }
}
