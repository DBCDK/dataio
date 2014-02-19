package dk.dbc.dataio.jobprocessor.ejb;

import dk.dbc.dataio.commons.types.Chunk;
import dk.dbc.dataio.commons.types.ChunkResult;
import dk.dbc.dataio.commons.types.NewJob;
import dk.dbc.dataio.commons.utils.invariant.InvariantUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.Asynchronous;
import javax.ejb.EJB;
import javax.ejb.LocalBean;
import javax.ejb.Stateless;

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

    public void process(NewJob newJob) throws NullPointerException {
        InvariantUtil.checkNotNullOrThrow(newJob, "newJob");

        final long numberOfChunks = newJob.getChunkCount();
        LOGGER.info("Processing job {} with {} chunks", newJob.getJobId(), numberOfChunks);

        for (long i = 1; i <= numberOfChunks; i++) {
            try {
                final Chunk chunk = jobStoreServiceConnector.getChunk(newJob.getJobId(), i);
                final ChunkResult processorResult = chunkProcessor.process(chunk);
                jobStoreMessageProducer.send(processorResult);
                sinkMessageProducer.send(processorResult, newJob.getSink());
            } catch (Exception e) {
                LOGGER.error("Exception during chunk processing", e);
            }
        }
    }
}
