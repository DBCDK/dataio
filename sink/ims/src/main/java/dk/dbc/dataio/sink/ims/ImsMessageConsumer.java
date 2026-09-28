package dk.dbc.dataio.sink.ims;

import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.types.ConsumedMessage;
import dk.dbc.dataio.commons.types.ImsSinkConfig;
import dk.dbc.dataio.commons.types.ObjectFactory;
import dk.dbc.dataio.commons.types.jms.JMSHeader;
import dk.dbc.dataio.jobstore.types.ItemDeliveryResult;
import dk.dbc.dataio.jse.artemis.common.jms.SinkMessageConsumerAdapter;
import dk.dbc.dataio.jse.artemis.common.service.ServiceHub;
import dk.dbc.dataio.sink.ims.connector.ImsServiceConnector;
import dk.dbc.oss.ns.updatemarcxchange.MarcXchangeRecord;
import dk.dbc.oss.ns.updatemarcxchange.UpdateMarcXchangeResult;
import dk.dbc.oss.ns.updatemarcxchange.UpdateMarcXchangeStatusEnum;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.ws.WebServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.List;

public class ImsMessageConsumer extends SinkMessageConsumerAdapter {
    private static final Logger LOGGER = LoggerFactory.getLogger(ImsMessageConsumer.class);
    private static final String QUEUE = SinkConfig.QUEUE.fqnAsQueue();
    private static final String ADDRESS = SinkConfig.QUEUE.fqnAsAddress();
    private final MarcXchangeRecordUnmarshaller marcXchangeRecordUnmarshaller = new MarcXchangeRecordUnmarshaller();
    private final ImsConfig imsConfig;
    private ImsSinkConfig config;
    private ImsServiceConnector connector;

    public ImsMessageConsumer(ServiceHub serviceHub, ImsConfig imsConfig) {
        super(serviceHub);
        this.imsConfig = imsConfig;
    }

    /**
     * Sends a successfully processed item's record to the IMS web service, and passes any other
     * item through as ignored
     * <p>
     * An item the processor failed or ignored is reported as ignored rather than as delivered, so
     * that it counts towards the job's ignored items and advances no delivery watermark for a
     * record nothing was sent for.
     */
    @Override
    protected ItemDeliveryResult deliverItem(ConsumedMessage message, ChunkItem item) {
        ImsServiceConnector imsServiceConnector = refreshState(imsConfig.getConfig(message));
        switch (item.getStatus()) {
            case SUCCESS:
                return deliverToIms(imsServiceConnector, requestTrackingId(message), item);
            case FAILURE:
                return ItemDeliveryResult.of(ItemDeliveryResult.Status.IGNORED,
                        passedThroughItem(item, "Failed by processor"));
            case IGNORE:
                return ItemDeliveryResult.of(ItemDeliveryResult.Status.IGNORED,
                        passedThroughItem(item, "Ignored by processor"));
            default:
                throw new IllegalStateException("Unknown chunk item status: " + item.getStatus());
        }
    }

    @Override
    public String getQueue() {
        return QUEUE;
    }

    @Override
    public String getAddress() {
        return ADDRESS;
    }

    /**
     * Sends one record to the IMS web service and turns its answer into a delivery result
     * <p>
     * A {@link WebServiceException} is left to propagate so that the item is redelivered rather
     * than failed. The connector retries one for a minute before giving up, which makes it an IMS
     * outage rather than a rejection of this record, and an outage is what redelivery is for.
     */
    private ItemDeliveryResult deliverToIms(ImsServiceConnector imsServiceConnector, String trackingId, ChunkItem item) {
        MarcXchangeRecord marcXchangeRecord;
        try {
            marcXchangeRecord = marcXchangeRecordUnmarshaller.toMarcXchangeRecord(item);
        } catch (JAXBException e) {
            return failedItem(item, "Error occurred while unmarshalling JAXBElement", e);
        }
        long requestStartTime = System.currentTimeMillis();
        try {
            List<UpdateMarcXchangeResult> results =
                    imsServiceConnector.updateMarcXchange(trackingId, List.of(marcXchangeRecord));
            return interpret(item, results);
        } catch (WebServiceException e) {
            Metric.IMS_FAILURES.counter().inc();
            throw new RuntimeException("WebServiceException caught when handling item " + trackingId, e);
        } finally {
            Metric.REQUEST_DURATION.timer().update(
                    Duration.ofMillis(System.currentTimeMillis() - requestStartTime));
        }
    }

    /**
     * Turns the web service's answer to a single record request into this item's delivery result
     * <p>
     * One request carries one record, so anything other than a single result about that record is
     * the service answering a question that was not asked, and the item is failed rather than
     * given an outcome taken from the wrong answer.
     */
    private ItemDeliveryResult interpret(ChunkItem item, List<UpdateMarcXchangeResult> results) {
        if (results.size() != 1) {
            return failedItem(item, String.format(
                    "Item failed due to webservice returning %d updateMarcXchangeResults when 1 was expected.",
                    results.size()), null);
        }
        UpdateMarcXchangeResult result = results.get(0);
        String recordId = result.getMarcXchangeRecordId();
        if (recordId == null) {
            return failedItem(item, buildItemData(result,
                    "Item failed due to webservice returning updateMarcXchangeResult with record id null."), null);
        }
        if (!recordId.equals(String.valueOf(item.getId()))) {
            return failedItem(item, buildItemData(result, String.format(
                    "Item failed due to webservice returning updateMarcXchangeResult for record id %s when %d was expected.",
                    recordId, item.getId())), null);
        }
        String itemData = buildItemData(result, null);
        if (result.getUpdateMarcXchangeStatus() == UpdateMarcXchangeStatusEnum.OK) {
            return ItemDeliveryResult.of(ItemDeliveryResult.Status.DELIVERED,
                    ChunkItem.successfulChunkItem()
                            .withId(item.getId())
                            .withTrackingId(item.getTrackingId())
                            .withType(ChunkItem.Type.STRING)
                            .withData(itemData));
        }
        return failedItem(item, itemData, null);
    }

    /**
     * Creates the delivering outcome of an item the processor failed or ignored, which this sink
     * sends nothing for
     */
    private ChunkItem passedThroughItem(ChunkItem item, String reason) {
        return ChunkItem.ignoredChunkItem()
                .withId(item.getId())
                .withTrackingId(item.getTrackingId())
                .withType(ChunkItem.Type.STRING)
                .withData(reason);
    }

    /**
     * Creates the failed delivery result of an item, carrying the reason both as the outcome data
     * and as a fatal diagnostic
     *
     * @param cause exception the failure came from, or null when the reason is the web service's
     *              own answer rather than a thrown one
     */
    private ItemDeliveryResult failedItem(ChunkItem item, String message, Throwable cause) {
        return ItemDeliveryResult.of(ItemDeliveryResult.Status.FAILED,
                ChunkItem.failedChunkItem()
                        .withId(item.getId())
                        .withTrackingId(item.getTrackingId())
                        .withType(ChunkItem.Type.STRING)
                        .withData(message)
                        .withDiagnostics(cause == null
                                ? ObjectFactory.buildFatalDiagnostic(message)
                                : ObjectFactory.buildFatalDiagnostic(message, cause)));
    }

    /**
     * Renders the web service's answer as the outcome data of an item, optionally prefixed by what
     * this sink found wrong with it
     */
    private String buildItemData(UpdateMarcXchangeResult result, String errorMessage) {
        StringBuilder stringBuilder = new StringBuilder();
        if (errorMessage != null) {
            stringBuilder.append(errorMessage).append(" -> ");
        }
        stringBuilder.append("[Status: ").append(result.getUpdateMarcXchangeStatus().value()).append("]");
        String updateMarcXchangeMessage = result.getUpdateMarcXchangeMessage();
        if (updateMarcXchangeMessage != null && !updateMarcXchangeMessage.isEmpty()) {
            stringBuilder.append(", [Message: ").append(updateMarcXchangeMessage).append("]");
        }
        return stringBuilder.toString();
    }

    /**
     * The id this sink gives the IMS web service to identify the request by, naming the item the
     * request carries
     * <p>
     * A request carries one item, so the id names that item within the job. The three headers are
     * read unguarded because the item delivery protocol has already rejected a message missing any
     * of them as invalid.
     */
    private String requestTrackingId(ConsumedMessage message) {
        return String.format("%d-%d-%d",
                JMSHeader.jobId.getHeader(message, Integer.class),
                JMSHeader.chunkId.getHeader(message, Long.class),
                JMSHeader.itemId.getHeader(message, Short.class));
    }

    /**
     * Returns the connector for the current sink configuration, rebuilding it when the
     * configuration has changed
     * <p>
     * The connector is returned rather than only assigned so that one delivery uses one connector
     * throughout. Both fields are written here alone, which with more than one consumer thread is
     * what keeps a delivery from seeing a half updated pair.
     */
    private synchronized ImsServiceConnector refreshState(ImsSinkConfig latestConfig) {
        if (!latestConfig.equals(config)) {
            LOGGER.debug("Updating connector");
            connector = createConnector(latestConfig);
            config = latestConfig;
        }
        return connector;
    }

    ImsServiceConnector createConnector(ImsSinkConfig imsSinkConfig) {
        return new ImsServiceConnector(imsSinkConfig.getEndpoint());
    }
}
