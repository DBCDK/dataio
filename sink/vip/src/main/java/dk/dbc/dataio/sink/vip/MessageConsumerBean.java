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
import dk.dbc.dataio.commons.utils.lang.StringUtil;
import dk.dbc.dataio.jse.artemis.common.jms.MessageConsumerAdapter;
import dk.dbc.dataio.sink.vip.connector.VipCoreConnector;
import dk.dbc.dataio.sink.vip.connector.VipCoreConnectorException;
import dk.dbc.dataio.sink.vip.connector.VipCoreConnectorUnexpectedStatusCodeException;
import dk.dbc.httpclient.HttpClient;
import dk.dbc.log.DBCTrackedLogContext;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

public class MessageConsumerBean extends MessageConsumerAdapter {
    private static final Logger LOGGER = LoggerFactory.getLogger(MessageConsumerBean.class);
    private ConfigBean configBean;
    private VipSinkConfig config;
    private VipCoreConnector vipCoreConnector;
    private JSONBContext jsonbContext = new JSONBContext();

    @Override
    public void handleConsumedMessage(ConsumedMessage consumedMessage) throws InvalidMessageException {
        final Chunk chunk = unmarshallPayload(consumedMessage);
        refreshState(configBean.getConfig(consumedMessage));
        sendResultToJobStore(handleChunk(chunk));
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
