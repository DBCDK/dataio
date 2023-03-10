package dk.dbc.dataio.sink.holdingsitems;

import dk.dbc.commons.addi.AddiReader;
import dk.dbc.commons.addi.AddiRecord;
import dk.dbc.commons.jsonb.JSONBException;
import dk.dbc.commons.metricshandler.MetricsHandlerBean;
import dk.dbc.dataio.commons.types.Chunk;
import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.types.ConsumedMessage;
import dk.dbc.dataio.commons.types.Diagnostic;
import dk.dbc.dataio.commons.types.exceptions.InvalidMessageException;
import dk.dbc.dataio.sink.holdingsitems.metrics.CounterMetrics;
import dk.dbc.dataio.sink.holdingsitems.metrics.SimpleTimerMetrics;
import dk.dbc.dataio.sink.types.AbstractSinkMessageConsumerBean;
import dk.dbc.dataio.sink.types.SinkException;
import dk.dbc.log.DBCTrackedLogContext;
import dk.dbc.solrdocstore.connector.SolrDocStoreConnector;
import dk.dbc.solrdocstore.connector.SolrDocStoreConnectorException;
import dk.dbc.solrdocstore.connector.SolrDocStoreConnectorUnexpectedStatusCodeException;
import dk.dbc.solrdocstore.connector.model.HoldingsItems;
import dk.dbc.solrdocstore.connector.model.Status;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.ActivationConfigProperty;
import javax.ejb.MessageDriven;
import javax.inject.Inject;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;

@MessageDriven(name = "holdingsItemsListener", activationConfig = {
        // Please see the following url for a explanation of the available settings.
        // The message selector variable is defined in the dataio-secrets project
        // https://activemq.apache.org/activation-spec-properties
        @ActivationConfigProperty(propertyName = "destination", propertyValue = "jms/dataio/sinks"),
        @ActivationConfigProperty(propertyName = "useJndi", propertyValue = "true"),
        @ActivationConfigProperty(propertyName = "destinationType", propertyValue = "javax.jms.Queue"),
        @ActivationConfigProperty(propertyName = "resourceAdapter", propertyValue = "artemis"),
        @ActivationConfigProperty(propertyName = "messageSelector", propertyValue = "resource = '${ENV=MESSAGE_NAME_FILTER}'"),
        @ActivationConfigProperty(propertyName = "initialRedeliveryDelay", propertyValue = "5000"),
        @ActivationConfigProperty(propertyName = "redeliveryBackOffMultiplier", propertyValue = "4"),
        @ActivationConfigProperty(propertyName = "maximumRedeliveries", propertyValue = "3"),
        @ActivationConfigProperty(propertyName = "redeliveryUseExponentialBackOff", propertyValue = "true"),
        @ActivationConfigProperty(propertyName = "MaxSession", propertyValue = "4")
})
public class MessageConsumerBean extends AbstractSinkMessageConsumerBean {
    private static final Logger LOGGER = LoggerFactory.getLogger(MessageConsumerBean.class);

    @Inject
    SolrDocStoreConnector solrDocStoreConnector;
    @Inject
    MetricsHandlerBean metricsHandler;
    @Inject
    HoldingsItemsUnmarshaller holdingsItemsUnmarshaller;

    @Override
    public void handleConsumedMessage(ConsumedMessage consumedMessage) throws InvalidMessageException, SinkException {
        try {
            final Chunk chunk = unmarshallPayload(consumedMessage);
            LOGGER.info("Received chunk {}/{}", chunk.getJobId(), chunk.getChunkId());
            metricsHandler.increment(CounterMetrics.CHUNK_ITEMS, chunk.size());

            uploadChunk(handleChunk(chunk));
        } catch (Exception any) {
            metricsHandler.increment(CounterMetrics.UNHANDLED_EXCEPTIONS);
            throw any;
        }
    }

    Chunk handleChunk(Chunk chunk) throws SinkException {
        final Chunk result = new Chunk(chunk.getJobId(), chunk.getChunkId(), Chunk.Type.DELIVERED);
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

    private ChunkItem handleChunkItem(ChunkItem chunkItem) throws SinkException {
        final ChunkItem result = new ChunkItem()
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
            throw new SinkException(e);
        }
    }

    private String deliverHoldingsItems(ChunkItem chunkItem) throws SolrDocStoreConnectorException {
        final StringBuilder resultStatus = new StringBuilder();
        final AddiReader addiReader = new AddiReader(new ByteArrayInputStream(chunkItem.getData()));
        boolean hasError = false;
        try {
            while (addiReader.hasNext()) {
                final AddiRecord addiRecord = addiReader.next();

                final List<HoldingsItems> holdingsItemsList = holdingsItemsUnmarshaller.unmarshall(
                        addiRecord.getContentData(), chunkItem.getTrackingId());
                for (HoldingsItems holdingsItems : holdingsItemsList) {
                    try {
                        final Status status = callSetHoldings(holdingsItems);
                        resultStatus.append(String.format("%s:%d consumer service response - %s\n",
                                holdingsItems.getBibliographicRecordId(), holdingsItems.getAgencyId(), status.getText()));
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
        long requestStartTime = System.currentTimeMillis();
        try {
            return solrDocStoreConnector.setHoldings(holdingsItems);
        } finally {
            metricsHandler.update(SimpleTimerMetrics.SET_HOLDINGS_REQUESTS,
                    Duration.ofMillis(System.currentTimeMillis() - requestStartTime));
        }
    }
}
