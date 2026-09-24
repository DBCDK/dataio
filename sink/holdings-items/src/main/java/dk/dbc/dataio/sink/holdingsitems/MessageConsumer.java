package dk.dbc.dataio.sink.holdingsitems;

import dk.dbc.commons.addi.AddiReader;
import dk.dbc.commons.addi.AddiRecord;
import dk.dbc.commons.jsonb.JSONBException;
import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.types.ConsumedMessage;
import dk.dbc.dataio.commons.types.Diagnostic;
import dk.dbc.dataio.jobstore.types.ItemDeliveryResult;
import dk.dbc.dataio.jse.artemis.common.jms.SinkMessageConsumerAdapter;
import dk.dbc.dataio.jse.artemis.common.service.ServiceHub;
import dk.dbc.solrdocstore.connector.SolrDocStoreConnector;
import dk.dbc.solrdocstore.connector.SolrDocStoreConnectorException;
import dk.dbc.solrdocstore.connector.SolrDocStoreConnectorUnexpectedStatusCodeException;
import dk.dbc.solrdocstore.connector.model.HoldingsItems;
import dk.dbc.solrdocstore.connector.model.Status;
import org.eclipse.microprofile.metrics.Tag;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

public class MessageConsumer extends SinkMessageConsumerAdapter {
    private final SolrDocStoreConnector solrDocStoreConnector;
    private final HoldingsItemsUnmarshaller holdingsItemsUnmarshaller;
    private static final String QUEUE = SinkConfig.QUEUE.fqnAsQueue();
    private static final String ADDRESS = SinkConfig.QUEUE.fqnAsAddress();

    public MessageConsumer(ServiceHub serviceHub, SolrDocStoreConnector solrDocStoreConnector, HoldingsItemsUnmarshaller holdingsItemsUnmarshaller) {
        super(serviceHub);
        this.solrDocStoreConnector = solrDocStoreConnector;
        this.holdingsItemsUnmarshaller = holdingsItemsUnmarshaller;
    }

    /**
     * Sends a successfully processed item's holdings to solr-doc-store, and passes any other item
     * through as ignored
     * <p>
     * An item the processor failed or ignored is reported as ignored rather than as delivered, so
     * that it counts towards the job's ignored items and advances no delivery watermark for a
     * record nothing was sent for.
     *
     * @throws SolrDocStoreConnectorException if the call to solr-doc-store fails, which rolls the
     *                                        JMS session back and has the item delivered again later
     */
    @Override
    protected ItemDeliveryResult deliverItem(ConsumedMessage message, ChunkItem item)
            throws SolrDocStoreConnectorException {
        switch (item.getStatus()) {
            case FAILURE:
                return ItemDeliveryResult.of(ItemDeliveryResult.Status.IGNORED,
                        outcome(item)
                                .withStatus(ChunkItem.Status.IGNORE)
                                .withData("Failed by processor"));
            case IGNORE:
                return ItemDeliveryResult.of(ItemDeliveryResult.Status.IGNORED,
                        outcome(item)
                                .withStatus(ChunkItem.Status.IGNORE)
                                .withData("Ignored by processor"));
            default:
                return deliverToSolrDocStore(item);
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
     * Delivers the holdings of one item, reporting content solr-doc-store will not accept as failed
     * <p>
     * Content that is not a readable Addi record, or not the holdings items JSON it must contain, is
     * failed rather than retried, and so is an item solr-doc-store rejected with a status code. A
     * failed item advances no delivery watermark, leaving a later version of the record free to
     * reach the target.
     */
    private ItemDeliveryResult deliverToSolrDocStore(ChunkItem item) throws SolrDocStoreConnectorException {
        try {
            return ItemDeliveryResult.of(ItemDeliveryResult.Status.DELIVERED,
                    outcome(item)
                            .withStatus(ChunkItem.Status.SUCCESS)
                            .withData(deliverHoldingsItems(item)));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ItemDeliveryResult.of(ItemDeliveryResult.Status.FAILED,
                    outcome(item)
                            .withStatus(ChunkItem.Status.FAILURE)
                            .withDiagnostics(new Diagnostic(Diagnostic.Level.FATAL, e.getMessage(), e))
                            .withData(e.getMessage()));
        }
    }

    /**
     * Creates the delivering outcome item of a delivered item, without the status and data naming
     * what became of it
     */
    private ChunkItem outcome(ChunkItem item) {
        return new ChunkItem()
                .withId(item.getId())
                .withTrackingId(item.getTrackingId())
                .withType(ChunkItem.Type.STRING)
                .withEncoding(StandardCharsets.UTF_8);
    }

    private String deliverHoldingsItems(ChunkItem chunkItem) throws SolrDocStoreConnectorException {
        StringBuilder resultStatus = new StringBuilder();
        AddiReader addiReader = new AddiReader(new ByteArrayInputStream(chunkItem.getData()));
        boolean hasError = false;
        try {
            while (addiReader.hasNext()) {
                AddiRecord addiRecord = addiReader.next();

                List<HoldingsItems> holdingsItemsList = holdingsItemsUnmarshaller.unmarshall(
                        addiRecord.getContentData(), chunkItem.getTrackingId());
                for (HoldingsItems holdingsItems : holdingsItemsList) {
                    try {
                        Status status = callSetHoldings(holdingsItems);
                        resultStatus.append(String.format("%s:%d consumer service response - %s\n",
                                holdingsItems.getBibliographicRecordId(), holdingsItems.getAgencyId(), status == null ? "<null>" : status.getText()));
                    } catch (SolrDocStoreConnectorUnexpectedStatusCodeException e) {
                        String error = "status code " + e.getStatusCode();
                        if (e.getStatus() != null) {
                            error = e.getStatus().getText();
                        }
                        resultStatus.append(String.format("%s:%d consumer service response - %s\n",
                                holdingsItems.getBibliographicRecordId(), holdingsItems.getAgencyId(), error));

                        hasError = true;
                    }
                }
            }
        } catch (IOException | JSONBException e) {
            throw new IllegalArgumentException("Invalid chunk item", e);
        }

        if (hasError) {
            throw new IllegalStateException(resultStatus.toString());
        }
        return resultStatus.toString();
    }

    private Status callSetHoldings(HoldingsItems holdingsItems) throws SolrDocStoreConnectorException {
        Instant start = Instant.now();
        Status status = null;
        try {
            status = solrDocStoreConnector.setHoldings(holdingsItems);
            return status;
        } finally {
            Tag success = new Tag("success", status == null ? "false" : status.getOk().toString());
            Metric.SET_HOLDINGS_REQUESTS.timer(success).update(Duration.between(start, Instant.now()));
        }
    }
}
