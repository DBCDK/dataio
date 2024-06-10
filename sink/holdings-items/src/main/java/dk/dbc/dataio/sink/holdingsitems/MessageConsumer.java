package dk.dbc.dataio.sink.holdingsitems;

import dk.dbc.commons.addi.AddiReader;
import dk.dbc.commons.addi.AddiRecord;
import dk.dbc.commons.jsonb.JSONBException;
import dk.dbc.dataio.commons.types.Chunk;
import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.types.ConsumedMessage;
import dk.dbc.dataio.commons.types.Diagnostic;
import dk.dbc.dataio.commons.types.exceptions.InvalidMessageException;
import dk.dbc.dataio.jse.artemis.common.jms.MessageConsumerAdapter;
import dk.dbc.dataio.jse.artemis.common.service.ServiceHub;
import dk.dbc.log.DBCTrackedLogContext;
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

public class MessageConsumer extends MessageConsumerAdapter {
    private final SolrDocStoreConnector solrDocStoreConnector;
    private final HoldingsItemsUnmarshaller holdingsItemsUnmarshaller;
    private static final String QUEUE = SinkConfig.QUEUE.fqnAsQueue();
    private static final String ADDRESS = SinkConfig.QUEUE.fqnAsAddress();

    public MessageConsumer(ServiceHub serviceHub, SolrDocStoreConnector solrDocStoreConnector, HoldingsItemsUnmarshaller holdingsItemsUnmarshaller) {
        super(serviceHub);
        this.solrDocStoreConnector = solrDocStoreConnector;
        this.holdingsItemsUnmarshaller = holdingsItemsUnmarshaller;
    }

    @Override
    public void handleConsumedMessage(ConsumedMessage consumedMessage) throws InvalidMessageException {
        Chunk chunk = unmarshallPayload(consumedMessage);
        sendResultToJobStore(handleChunk(chunk));
    }

    @Override
    public String getQueue() {
        return QUEUE;
    }

    @Override
    public String getAddress() {
        return ADDRESS;
    }

    Chunk handleChunk(Chunk chunk) {
        Chunk result = new Chunk(chunk.getJobId(), chunk.getChunkId(), Chunk.Type.DELIVERED);
        try {
            for (ChunkItem chunkItem : chunk.getItems()) {
                DBCTrackedLogContext.setTrackingId(chunkItem.getTrackingId());
                result.insertItem(handleChunkItem(chunkItem));
            }
        } finally {
            DBCTrackedLogContext.remove();
        }
        return result;
    }

    private ChunkItem handleChunkItem(ChunkItem chunkItem) {
        ChunkItem result = new ChunkItem()
                .withId(chunkItem.getId())
                .withTrackingId(chunkItem.getTrackingId())
                .withType(ChunkItem.Type.STRING)
                .withEncoding(StandardCharsets.UTF_8);
        try {
            switch (chunkItem.getStatus()) {
                case FAILURE:
                    return result
                            .withStatus(ChunkItem.Status.IGNORE)
                            .withData("Failed by processor");
                case IGNORE:
                    return result
                            .withStatus(ChunkItem.Status.IGNORE)
                            .withData("Ignored by processor");
                default:
                    return result
                            .withStatus(ChunkItem.Status.SUCCESS)
                            .withData(deliverHoldingsItems(chunkItem));
            }
        } catch (IllegalArgumentException | IllegalStateException e) {
            return result
                    .withStatus(ChunkItem.Status.FAILURE)
                    .withDiagnostics(new Diagnostic(Diagnostic.Level.FATAL, e.getMessage(), e))
                    .withData(e.getMessage());
        } catch (SolrDocStoreConnectorException e) {
            throw new RuntimeException(e);
        }
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
