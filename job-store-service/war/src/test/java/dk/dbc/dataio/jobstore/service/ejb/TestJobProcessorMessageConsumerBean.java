package dk.dbc.dataio.jobstore.service.ejb;

import dk.dbc.commons.jsonb.JSONBContext;
import dk.dbc.commons.jsonb.JSONBException;
import dk.dbc.dataio.commons.time.StopWatch;
import dk.dbc.dataio.commons.types.Chunk;
import dk.dbc.dataio.commons.types.ConsumedMessage;
import dk.dbc.dataio.commons.types.exceptions.InvalidMessageException;
import dk.dbc.dataio.commons.types.interceptor.Stopwatch;
import dk.dbc.dataio.commons.types.jms.JmsConstants;
import dk.dbc.dataio.commons.utils.service.AbstractMessageConsumerBean;
import dk.dbc.dataio.jobstore.types.JobStoreException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.ActivationConfigProperty;
import javax.ejb.MessageDriven;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * Handles Chunk messages received from the job-store
 * Test Job Chunk Processor
 */
@MessageDriven(activationConfig = {
        @ActivationConfigProperty(propertyName = "destinationLookup", propertyValue = "jms/dataio/processor"),
        @ActivationConfigProperty(propertyName = "destinationType", propertyValue = "javax.jms.Queue"),}
)
public class TestJobProcessorMessageConsumerBean extends AbstractMessageConsumerBean {
    private static final Logger LOGGER = LoggerFactory.getLogger(TestJobProcessorMessageConsumerBean.class);

    private static final List<Chunk> chunksReceived = new ArrayList<>();
    private static final Semaphore processBlocker = new Semaphore(0);

    JSONBContext jsonbContext = new JSONBContext();


    @SuppressWarnings("EjbClassWarningsInspection")
    static void waitForProcessingOfChunks(String message, int numberOfChunksToWaitFor) throws Exception {
        StopWatch timer = new StopWatch();
        if (!processBlocker.tryAcquire(numberOfChunksToWaitFor, 20, TimeUnit.SECONDS)) {
            throw new Exception("Unittest Errors unable to acquire " + numberOfChunksToWaitFor + " in 10 Seconds :" + message);
        }
        LOGGER.info("Waiting in took waitForProcessingOfChunks {} {} ms", numberOfChunksToWaitFor, timer.getElapsedTime());
    }

    /**
     * Processes Chunk received in consumed message
     *
     * @param consumedMessage message to be handled
     * @throws InvalidMessageException if message payload can not be unmarshalled to chunk instance
     */
    @Stopwatch
    synchronized public void handleConsumedMessage(ConsumedMessage consumedMessage) throws InvalidMessageException {
        try {
            final Chunk chunk = jsonbContext.unmarshall(consumedMessage.getMessagePayload(), Chunk.class);
            LOGGER.info("Received chunk {}/{}", chunk.getJobId(), chunk.getChunkId());
            confirmLegalChunkTypeOrThrow(chunk, Chunk.Type.PARTITIONED);
            process(chunk);
        } catch (JSONBException e) {
            throw new InvalidMessageException(String.format("Message<%s> payload was not valid Chunk type %s",
                    consumedMessage.getMessageId(), consumedMessage.getHeaderValue(JmsConstants.CHUNK_PAYLOAD_TYPE, String.class)), e);
        } catch (JobStoreException e) {
            throw new InvalidMessageException(String.format("Message<%s> Failed in JobStore %s",
                    consumedMessage.getMessageId(), e.getMessage(), e));
        }
    }

    private void process(Chunk chunk) throws JSONBException, JobStoreException {
        synchronized (chunksReceived) {
            try {
                chunksReceived.add(chunk);

                TestJobStoreConnection.sendChunkToJobstoreAsType(chunk, Chunk.Type.PROCESSED);

            } finally {
                processBlocker.release();
            }
        }
    }


    @SuppressWarnings("EjbClassWarningsInspection")
    public static void reset() {
        synchronized (chunksReceived) {
            chunksReceived.clear();
            processBlocker.drainPermits();
        }
    }

    @SuppressWarnings("EjbClassWarningsInspection")
    public static int getChunksReceivedCount() {
        synchronized ((chunksReceived)) {
            return chunksReceived.size();
        }
    }
}
