package dk.dbc.dataio.sink.ims;

import dk.dbc.commons.metricshandler.MetricsHandlerBean;
import dk.dbc.dataio.commons.types.Chunk;
import dk.dbc.dataio.commons.types.ConsumedMessage;
import dk.dbc.dataio.commons.types.ImsSinkConfig;
import dk.dbc.dataio.commons.types.exceptions.InvalidMessageException;
import dk.dbc.dataio.commons.types.interceptor.Stopwatch;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnectorException;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnectorUnexpectedStatusCodeException;
import dk.dbc.dataio.commons.utils.jobstore.ejb.JobStoreServiceConnectorBean;
import dk.dbc.dataio.jobstore.types.JobError;
import dk.dbc.dataio.sink.ims.connector.ImsServiceConnector;
import dk.dbc.dataio.sink.types.AbstractSinkMessageConsumerBean;
import dk.dbc.dataio.sink.types.SinkException;
import dk.dbc.oss.ns.updatemarcxchange.UpdateMarcXchangeResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.ActivationConfigProperty;
import javax.ejb.EJB;
import javax.ejb.MessageDriven;
import javax.inject.Inject;
import javax.xml.ws.WebServiceException;
import java.time.Duration;
import java.util.List;

@MessageDriven(name = "imsListener", activationConfig = {
        // Please see the following url for a explanation of the available settings.
        // The message selector variable is defined in the dataio-secrets project
        // https://activemq.apache.org/activation-spec-properties
        @ActivationConfigProperty(propertyName = "destination", propertyValue = "${ENV=QUEUE}"),
        @ActivationConfigProperty(propertyName = "useJndi", propertyValue = "false"),
        @ActivationConfigProperty(propertyName = "destinationType", propertyValue = "javax.jms.Queue"),
        @ActivationConfigProperty(propertyName = "resourceAdapter", propertyValue = "artemis"),
        @ActivationConfigProperty(propertyName = "initialRedeliveryDelay", propertyValue = "5000"),
        @ActivationConfigProperty(propertyName = "redeliveryBackOffMultiplier", propertyValue = "4"),
        @ActivationConfigProperty(propertyName = "maximumRedeliveries", propertyValue = "3"),
        @ActivationConfigProperty(propertyName = "redeliveryUseExponentialBackOff", propertyValue = "true"),
        @ActivationConfigProperty(propertyName = "MaxSession", propertyValue = "4")
})
public class ImsMessageProcessorBean extends AbstractSinkMessageConsumerBean {
    private static final Logger LOGGER = LoggerFactory.getLogger(ImsMessageProcessorBean.class);

    @EJB
    JobStoreServiceConnectorBean jobStoreServiceConnectorBean;
    @EJB
    ImsConfigBean imsConfigBean;

    final MarcXchangeRecordUnmarshaller marcXchangeRecordUnmarshaller = new MarcXchangeRecordUnmarshaller();

    private ImsSinkConfig config;
    private ImsServiceConnector connector;

    @Inject
    MetricsHandlerBean metricsHandler;

    @Stopwatch
    @Override
    public void handleConsumedMessage(ConsumedMessage consumedMessage) throws SinkException, InvalidMessageException, NullPointerException, WebServiceException {

        final Chunk chunk;
        try {
            chunk = unmarshallPayload(consumedMessage);
            LOGGER.info("Received chunk {}/{}", chunk.getJobId(), chunk.getChunkId());
        } catch (Exception e) {
            LOGGER.info("Caught exception when trying to unmarshall message payload {}", e);
            throw e;
        }

        final ImsSinkConfig latestConfig = imsConfigBean.getConfig(consumedMessage);
        if (!latestConfig.equals(config)) {
            LOGGER.debug("Updating connector");
            connector = getImsServiceConnector(latestConfig);
            config = latestConfig;
        }

        try {
            long imsRequestStartTime = System.currentTimeMillis();
            final SinkResult sinkResult = new SinkResult(chunk, marcXchangeRecordUnmarshaller);
            if (!sinkResult.getMarcXchangeRecords().isEmpty()) {
                final List<UpdateMarcXchangeResult> marcXchangeResults = connector.updateMarcXchange(
                        String.format("%d-%d", chunk.getJobId(), chunk.getChunkId()), sinkResult.getMarcXchangeRecords());
                sinkResult.update(marcXchangeResults);
            }
            metricsHandler.update(ImsTimerMetrics.REQUEST_DURATION, Duration.ofMillis(System.currentTimeMillis() - imsRequestStartTime));
            addChunkToJobStore(sinkResult.toChunk());
        } catch (WebServiceException e) {
            LOGGER.error("WebServiceException caught when handling chunk {}/{}", chunk.getJobId(), chunk.getChunkId(), e);
            metricsHandler.increment(ImsCounterMetrics.IMS_FAILURES);
            throw e;
        } catch (Exception e) {
            LOGGER.error("Caught unhandled exception {}", e);
            metricsHandler.increment(ImsCounterMetrics.UNHANDLED_EXCEPTIONS);
            throw e;
        }
    }

    private ImsServiceConnector getImsServiceConnector(ImsSinkConfig config) {
        return new ImsServiceConnector(config.getEndpoint());
    }

    private void addChunkToJobStore(Chunk outcome) throws SinkException {
        try {
            jobStoreServiceConnectorBean.getConnector().addChunkIgnoreDuplicates(outcome, outcome.getJobId(), outcome.getChunkId());
        } catch (JobStoreServiceConnectorException e) {
            logJobStoreError(e);
            // Throw SinkException to force transaction rollback
            throw new SinkException("Error in communication with job-store", e);
        }
    }

    private void logJobStoreError(JobStoreServiceConnectorException e) {
        if (e instanceof JobStoreServiceConnectorUnexpectedStatusCodeException) {
            final JobError jobError = ((JobStoreServiceConnectorUnexpectedStatusCodeException) e).getJobError();
            if (jobError != null) {
                LOGGER.error("job-store returned error: {}", jobError.getDescription());
            }
        }
    }
}
