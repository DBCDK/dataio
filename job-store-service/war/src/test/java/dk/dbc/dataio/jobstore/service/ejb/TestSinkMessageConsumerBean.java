package dk.dbc.dataio.jobstore.service.ejb;

import dk.dbc.dataio.commons.types.Chunk;
import dk.dbc.dataio.commons.types.ConsumedMessage;
import dk.dbc.dataio.commons.types.exceptions.InvalidMessageException;
import dk.dbc.dataio.commons.types.exceptions.ServiceException;
import dk.dbc.dataio.commons.utils.service.AbstractSinkMessageConsumerBean;
import dk.dbc.dataio.jsonb.JSONBContext;
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
 */
@MessageDriven(activationConfig = {
        @ActivationConfigProperty(propertyName = "destinationLookup", propertyValue = "jms/dataio/sinks"),
        @ActivationConfigProperty(propertyName = "destinationType", propertyValue = "javax.jms.Queue"), }
)
public class TestSinkMessageConsumerBean extends AbstractSinkMessageConsumerBean {

    private static final Logger LOGGER = LoggerFactory.getLogger(TestJobProcessorMessageConsumerBean.class);

     static private List<Chunk> chunksRetrived =new ArrayList<>();
     static Semaphore processBlocker=new Semaphore(0);

     JSONBContext jsonbContext = new JSONBContext();

     public static List<Chunk> getChunksRetrived() {
         return chunksRetrived;
     }

     static void waitForDeliveringOfChunks(int numberOfChunksToWaitFor) throws Exception {
         if( ! processBlocker.tryAcquire( numberOfChunksToWaitFor, 10, TimeUnit.SECONDS ) ) {
             throw new Exception("Unittest Errors unable to Aacquire "+ numberOfChunksToWaitFor + " in 10 Seconds");
         }
     }

    @Override
    public void handleConsumedMessage(ConsumedMessage consumedMessage) throws InvalidMessageException, ServiceException {
        final Chunk processedChunk = unmarshallPayload(consumedMessage);
        LOGGER.info(" Chunk {}-{} handled in Sink", processedChunk.getJobId(), processedChunk.getChunkId() );
        synchronized (chunksRetrived) {
            chunksRetrived.add( processedChunk);
            processBlocker.release();
        }

    }

    public static void reset() {
        synchronized ( chunksRetrived ) {
            chunksRetrived.clear();
            processBlocker.drainPermits();
        }
    }
}
