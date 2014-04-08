package dk.dbc.dataio.jobprocessor.ejb;

import dk.dbc.dataio.commons.types.Chunk;
import dk.dbc.dataio.commons.types.ChunkResult;
import dk.dbc.dataio.commons.types.NewJob;
import dk.dbc.dataio.commons.types.Sink;
import dk.dbc.dataio.commons.utils.invariant.InvariantUtil;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnectorException;
import dk.dbc.dataio.commons.utils.jobstore.ejb.JobStoreServiceConnectorBean;
import dk.dbc.dataio.jobprocessor.exception.JobProcessorException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.Asynchronous;
import javax.ejb.EJB;
import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;

@Stateless
@Asynchronous
@LocalBean
public class JobProcessorBean {

    private static final Logger LOGGER = LoggerFactory.getLogger(JobProcessorBean.class);

    @EJB
    JobStoreServiceConnectorBean jobStoreServiceConnector;

    @EJB
    JobStoreMessageProducerBean jobStoreMessageProducer;

    @EJB
    SinkMessageProducerBean sinkMessageProducer;

    @EJB
    ChunkProcessorBean chunkProcessor;

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void processChunk(long jobId, long chunkId, Sink sink) throws NullPointerException, JobProcessorException {
        try {
            // todo: What happens to the original transaction in the MDB if this transaction fails and rolls back?
            // Will the MDB transaction also roll-back? Or will it continue?
            // If the MDB transaction does not roll back, then we should carefully think about
            // moving the .getChunk() call into the MDB, in order to ensure that errors with retrieving
            // chunks from the jobstore puts the message back on the queue.
            LOGGER.info("TAROK: Requesting chunk #{}", chunkId);
            final Chunk chunk = jobStoreServiceConnector.getChunk(jobId, chunkId);
            LOGGER.info("TAROK: Recieved chunk #{}", chunkId);
            final ChunkResult processorResult = chunkProcessor.process(chunk);
            jobStoreMessageProducer.send(processorResult);
            sinkMessageProducer.send(processorResult, sink);
        } catch (JobStoreServiceConnectorException e) {
            throw new JobProcessorException("Exception caught while fetching and processing chunk in jobProcessor", e);
        }
    }
}
