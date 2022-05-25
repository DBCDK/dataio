package dk.dbc.dataio.jobstore.service.ejb;

import dk.dbc.commons.jsonb.JSONBContext;
import dk.dbc.dataio.commons.time.StopWatch;
import dk.dbc.dataio.commons.types.Chunk;
import dk.dbc.dataio.commons.types.ConsumedMessage;
import dk.dbc.dataio.commons.types.exceptions.InvalidMessageException;
import dk.dbc.dataio.commons.types.exceptions.ServiceException;
import dk.dbc.dataio.sink.types.AbstractSinkMessageConsumerBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.ActivationConfigProperty;
import javax.ejb.MessageDriven;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * Created by ja7 on 06-05-16.
 * <p>
 * Test message comsumer bean.
 */
@MessageDriven(activationConfig = {
        @ActivationConfigProperty(propertyName = "destinationLookup", propertyValue = "jms/dataio/sinks"),
        @ActivationConfigProperty(propertyName = "destinationType", propertyValue = "javax.jms.Queue"),}
)
public class TestSinkMessageConsumerBean extends AbstractSinkMessageConsumerBean {

    private static final Logger LOGGER = LoggerFactory.getLogger(TestSinkMessageConsumerBean.class);

    private static final List<Chunk> chunksReceived = new ArrayList<>();
    private static final Semaphore processBlocker = new Semaphore(0);

    JSONBContext jsonbContext = new JSONBContext();

    @SuppressWarnings("EjbClassWarningsInspection")
    static void waitForDeliveringOfChunks(String message, int numberOfChunksToWaitFor) throws Exception {
        StopWatch timer = new StopWatch();
        if (!processBlocker.tryAcquire(numberOfChunksToWaitFor, 10, TimeUnit.SECONDS)) {
            throw new Exception("Unittest Errors unable to acquire " + numberOfChunksToWaitFor + " in 10 Seconds : " + message);
        }
        LOGGER.info("Waiting in took waitForDeliveringOfChunks {}  {} ms", numberOfChunksToWaitFor, timer.getElapsedTime());
    }


    @Override
    public void handleConsumedMessage(ConsumedMessage consumedMessage) throws InvalidMessageException, ServiceException {
        final Chunk chunk = unmarshallPayload(consumedMessage);
        LOGGER.info("Handled chunk {}/{}", chunk.getJobId(), chunk.getChunkId());
        synchronized (chunksReceived) {
            try {
                chunksReceived.add(chunk);
                TestJobStoreConnection.sendChunkToJobstoreAsType(chunk, Chunk.Type.DELIVERED);
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
