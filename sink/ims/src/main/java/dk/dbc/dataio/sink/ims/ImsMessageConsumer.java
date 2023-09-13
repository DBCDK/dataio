package dk.dbc.dataio.sink.ims;

import dk.dbc.dataio.commons.types.Chunk;
import dk.dbc.dataio.commons.types.ConsumedMessage;
import dk.dbc.dataio.commons.types.ImsSinkConfig;
import dk.dbc.dataio.commons.types.exceptions.InvalidMessageException;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnector;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnectorException;
import dk.dbc.dataio.jse.artemis.common.jms.MessageConsumerAdapter;
import dk.dbc.dataio.jse.artemis.common.service.ServiceHub;
import dk.dbc.dataio.sink.ims.connector.ImsServiceConnector;
import dk.dbc.oss.ns.updatemarcxchange.UpdateMarcXchangeResult;
import jakarta.xml.ws.WebServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.List;

public class ImsMessageConsumer extends MessageConsumerAdapter {
    private static final Logger LOGGER = LoggerFactory.getLogger(ImsMessageConsumer.class);
    private static final String QUEUE = SinkConfig.QUEUE.fqnAsQueue();
    private static final String ADDRESS = SinkConfig.QUEUE.fqnAsAddress();
    private final MarcXchangeRecordUnmarshaller marcXchangeRecordUnmarshaller = new MarcXchangeRecordUnmarshaller();
    private final JobStoreServiceConnector jobStoreServiceConnector;
    private final ImsConfig imsConfig;
    private ImsSinkConfig config;
    private ImsServiceConnector connector;

    public ImsMessageConsumer(ServiceHub serviceHub, ImsConfig imsConfig) {
        super(serviceHub);
        jobStoreServiceConnector = serviceHub.jobStoreServiceConnector;
        this.imsConfig = imsConfig;
    }

    @Override
    public void handleConsumedMessage(ConsumedMessage consumedMessage) throws InvalidMessageException {
        Chunk chunk = unmarshallPayload(consumedMessage);

        ImsSinkConfig latestConfig = imsConfig.getConfig(consumedMessage);
        if (!latestConfig.equals(config)) {
            LOGGER.debug("Updating connector");
            connector = getImsServiceConnector(latestConfig);
            config = latestConfig;
        }

        try {
            long imsRequestStartTime = System.currentTimeMillis();
            SinkResult sinkResult = new SinkResult(chunk, marcXchangeRecordUnmarshaller);
            if (!sinkResult.getMarcXchangeRecords().isEmpty()) {
                List<UpdateMarcXchangeResult> marcXchangeResults = connector.updateMarcXchange(
                        String.format("%d-%d", chunk.getJobId(), chunk.getChunkId()), sinkResult.getMarcXchangeRecords());
                sinkResult.update(marcXchangeResults);
            }
            Metric.REQUEST_DURATION.simpleTimer().update(Duration.ofMillis(System.currentTimeMillis() - imsRequestStartTime));
            addChunkToJobStore(sinkResult.toChunk());
        } catch (WebServiceException e) {
            Metric.IMS_FAILURES.counter().inc();
            throw new RuntimeException("WebServiceException caught when handling chunk " + chunk.getJobId() + "/" + chunk.getChunkId(), e);
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

    private ImsServiceConnector getImsServiceConnector(ImsSinkConfig config) {
        return new ImsServiceConnector(config.getEndpoint());
    }

    private void addChunkToJobStore(Chunk outcome) {
        try {
            jobStoreServiceConnector.addChunkIgnoreDuplicates(outcome, outcome.getJobId(), outcome.getChunkId());
        } catch (JobStoreServiceConnectorException e) {
            throw new RuntimeException("Error in communication with job-store", e);
        }
    }
}
