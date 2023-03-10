package dk.dbc.dataio.sink.vip;

import dk.dbc.commons.addi.AddiReader;
import dk.dbc.commons.addi.AddiRecord;
import dk.dbc.commons.jsonb.JSONBContext;
import dk.dbc.commons.jsonb.JSONBException;
import dk.dbc.dataio.commons.types.AddiMetaData;
import dk.dbc.dataio.commons.types.Chunk;
import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.types.ConsumedMessage;
import dk.dbc.dataio.commons.types.Diagnostic;
import dk.dbc.dataio.commons.types.VipSinkConfig;
import dk.dbc.dataio.commons.types.exceptions.InvalidMessageException;
import dk.dbc.dataio.commons.types.interceptor.Stopwatch;
import dk.dbc.dataio.commons.utils.lang.StringUtil;
import dk.dbc.dataio.sink.types.AbstractSinkMessageConsumerBean;
import dk.dbc.dataio.sink.types.SinkException;
import dk.dbc.dataio.sink.vip.connector.VipCoreConnector;
import dk.dbc.dataio.sink.vip.connector.VipCoreConnectorException;
import dk.dbc.dataio.sink.vip.connector.VipCoreConnectorUnexpectedStatusCodeException;
import dk.dbc.httpclient.HttpClient;
import dk.dbc.log.DBCTrackedLogContext;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.ActivationConfigProperty;
import javax.ejb.EJB;
import javax.ejb.MessageDriven;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

@MessageDriven(name = "vipListener", activationConfig = {
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

    @EJB
    ConfigBean configBean;

    private VipSinkConfig config;
    private VipCoreConnector vipCoreConnector;
    private JSONBContext jsonbContext = new JSONBContext();

    @Override
    @Stopwatch
    public void handleConsumedMessage(ConsumedMessage consumedMessage)
            throws InvalidMessageException, SinkException {
        final Chunk chunk = unmarshallPayload(consumedMessage);
        LOGGER.info("Received chunk {}/{}", chunk.getJobId(), chunk.getChunkId());

        refreshState(configBean.getConfig(consumedMessage));

        uploadChunk(handleChunk(chunk));
    }

    Chunk handleChunk(Chunk chunk) {
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

    private ChunkItem handleChunkItem(ChunkItem chunkItem) {
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
                    vipload(chunkItem);
                    return result
                            .withStatus(ChunkItem.Status.SUCCESS)
                            .withData("Loaded");
            }
        } catch (Exception e) {
            if (e instanceof VipCoreConnectorUnexpectedStatusCodeException) {
                final Optional<VipCoreConnector.Error> error =
                        ((VipCoreConnectorUnexpectedStatusCodeException) e).getError();
                if (error.isPresent()) {
                    final String errorMessage = e.getMessage() + " - " + error.get().toString();
                    return result
                            .withStatus(ChunkItem.Status.FAILURE)
                            .withDiagnostics(new Diagnostic(Diagnostic.Level.FATAL, errorMessage, e))
                            .withData(errorMessage);
                }
            }
            return result
                    .withStatus(ChunkItem.Status.FAILURE)
                    .withDiagnostics(new Diagnostic(
                            Diagnostic.Level.FATAL, e.getMessage(), e))
                    .withData(e.getMessage());
        }
    }

    private void vipload(ChunkItem chunkItem) throws VipCoreConnectorException, IOException, JSONBException {
        final AddiReader addiReader = new AddiReader(new ByteArrayInputStream(chunkItem.getData()));
        while (addiReader.hasNext()) {
            final AddiRecord addiRecord = addiReader.next();
            final AddiMetaData addiMetaData = jsonbContext.unmarshall(
                    StringUtil.asString(addiRecord.getMetaData()), AddiMetaData.class);
            vipCoreConnector.vipload(addiMetaData.format(),
                    StringUtil.asString(addiRecord.getContentData()));
        }
    }

    private void refreshState(VipSinkConfig latestConfig) {
        if (!latestConfig.equals(config)) {
            config = latestConfig;
            vipCoreConnector = createVipCoreConnector(config);
        }
    }

    private VipCoreConnector createVipCoreConnector(VipSinkConfig config) {
        if (vipCoreConnector != null) {
            vipCoreConnector.close();
        }
        return new VipCoreConnector(
                HttpClient.newClient(
                        new ClientConfig()
                                .register(new JacksonFeature())),
                config.getEndpoint());
    }
}
