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
import dk.dbc.dataio.jse.artemis.common.service.ServiceHub;
import dk.dbc.dataio.sink.vip.connector.VipCoreConnector;
import dk.dbc.dataio.sink.vip.connector.VipCoreConnectorException;
import dk.dbc.dataio.sink.vip.connector.VipCoreConnectorUnexpectedStatusCodeException;
import dk.dbc.log.DBCTrackedLogContext;
import jakarta.ws.rs.client.ClientBuilder;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

public class VipMessageConsumer extends MessageConsumerAdapter {
    private static final String QUEUE = SinkConfig.QUEUE.fqnAsQueue();
    private static final String ADDRESS = SinkConfig.QUEUE.fqnAsAddress();
    private final ConfigBean configBean;
    private VipSinkConfig config;
    private VipCoreConnector vipCoreConnector;
    private final JSONBContext jsonbContext = new JSONBContext();

    public VipMessageConsumer(ServiceHub serviceHub, ConfigBean configBean) {
        super(serviceHub);
        this.configBean = configBean;
    }

    @Override
    public void handleConsumedMessage(ConsumedMessage consumedMessage) throws InvalidMessageException {
        Chunk chunk = unmarshallPayload(consumedMessage);
        refreshState(configBean.getConfig(consumedMessage));
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
                    vipLoad(chunkItem);
                    return result
                            .withStatus(ChunkItem.Status.SUCCESS)
                            .withData("Loaded");
            }
        } catch (Exception e) {
            if (e instanceof VipCoreConnectorUnexpectedStatusCodeException) {
                Optional<VipCoreConnector.Error> error =
                        ((VipCoreConnectorUnexpectedStatusCodeException) e).getError();
                if (error.isPresent()) {
                    String errorMessage = e.getMessage() + " - " + error.get();
                    return result
                            .withStatus(ChunkItem.Status.FAILURE)
                            .withDiagnostics(new Diagnostic(Diagnostic.Level.FATAL, errorMessage, e))
                            .withData(errorMessage);
                }
            }
            return result
                    .withStatus(ChunkItem.Status.FAILURE)
                    .withDiagnostics(new Diagnostic(Diagnostic.Level.FATAL, e.getMessage(), e))
                    .withData(e.getMessage());
        }
    }

    private void vipLoad(ChunkItem chunkItem) throws VipCoreConnectorException, IOException, JSONBException {
        AddiReader addiReader = new AddiReader(new ByteArrayInputStream(chunkItem.getData()));
        while (addiReader.hasNext()) {
            AddiRecord addiRecord = addiReader.next();
            AddiMetaData addiMetaData = jsonbContext.unmarshall(StringUtil.asString(addiRecord.getMetaData()), AddiMetaData.class);
            vipCoreConnector.vipload(addiMetaData.format(), StringUtil.asString(addiRecord.getContentData()));
        }
    }

    private synchronized void refreshState(VipSinkConfig latestConfig) {
        if (!latestConfig.equals(config)) {
            config = latestConfig;
            vipCoreConnector = createVipCoreConnector(config);
        }
    }

    private VipCoreConnector createVipCoreConnector(VipSinkConfig config) {
        if (vipCoreConnector != null) {
            vipCoreConnector.close();
        }
        return new VipCoreConnector(ClientBuilder.newClient(), config.getEndpoint());
    }
}
