package dk.dbc.dataio.jobstore.ejb;

import dk.dbc.dataio.commons.types.Chunk;
import dk.dbc.dataio.commons.types.Sink;
import dk.dbc.dataio.commons.utils.invariant.InvariantUtil;
import dk.dbc.dataio.jobstore.types.JobStoreException;
import dk.dbc.dataio.sequenceanalyser.SequenceAnalyser;
import dk.dbc.dataio.sequenceanalyser.naive.ChunkIdentifier;
import dk.dbc.dataio.sequenceanalyser.naive.NaiveSequenceAnalyser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.ejb.ConcurrencyManagement;
import javax.ejb.ConcurrencyManagementType;
import javax.ejb.EJB;
import javax.ejb.Singleton;
import javax.ejb.Startup;

/**
 * This Enterprise Java Bean (EJB) is responsible for chunk scheduling
 * via sequence analysis and for notifying dataio pipeline components of
 * available chunks.
 */
@Singleton
@Startup
@ConcurrencyManagement(ConcurrencyManagementType.BEAN)
public class JobSchedulerBean {
    private static final Logger LOGGER = LoggerFactory.getLogger(JobSchedulerBean.class);

    SequenceAnalyser sequenceAnalyser;

    @EJB
    JobProcessorMessageProducerBean jobProcessorMessageProducerBean;

    @EJB
    JobStoreBean jobStoreBean;

    @PostConstruct
    public void initialise() {
        sequenceAnalyser = new NaiveSequenceAnalyser();
    }

    /**
     * Passes given chunk and sink on to the sequence analyser and notifies
     * pipeline of next available workload (if any)
     * @param chunk next chunk to enter into sequence analysis
     * @param sink  sink associated with chunk
     * @throws NullPointerException if given any null-valued argument
     */
    public void scheduleChunk(Chunk chunk, Sink sink) throws NullPointerException {
        InvariantUtil.checkNotNullOrThrow(chunk, "chunk");
        InvariantUtil.checkNotNullOrThrow(sink, "sink");
        LOGGER.info("Scheduling chunk.id {} of job.id {}", chunk.getChunkId(), chunk.getJobId());
        sequenceAnalyser.addChunk(chunk, sink);
        notifyWorkloadAvailable();
    }

    /**
     * Forces sequence analyser release of the chunk identified by given
     * job and chunk IDs due to the chunk no longer being present in the
     * pipeline and notifies pipeline of next available workload (if any)
     * @param jobId identifier of job containing chunk
     * @param chunkId identifier of chunk in containing job
     */
    public void releaseChunk(long jobId, long chunkId) {
        LOGGER.info("Releasing chunk.id {} of job.id {}", chunkId, jobId);
        final ChunkIdentifier chunkIdentifier = new ChunkIdentifier(jobId, chunkId);
        sequenceAnalyser.deleteAndReleaseChunk(chunkIdentifier);
        notifyWorkloadAvailable();
    }

    private void notifyWorkloadAvailable() {
        for (final ChunkIdentifier chunkIdentifier : sequenceAnalyser.getInactiveIndependentChunks()) {
            try {
                final Chunk chunk = jobStoreBean.getJobStore().getChunk(chunkIdentifier.jobId, chunkIdentifier.chunkId);
                if (chunk == null) {
                    LOGGER.error("Unable to locate chunk.id {} for job.id {}",
                            chunkIdentifier.chunkId, chunkIdentifier.jobId);
                } else {
                    try {
                        jobProcessorMessageProducerBean.send(chunk);
                    } catch (JobStoreException e) {
                        LOGGER.error("Unable to send notification for chunk.id {} for job.id {}",
                                chunkIdentifier.chunkId, chunkIdentifier.jobId, e);
                    }
                }
            } catch (JobStoreException e) {
                LOGGER.error("Unable to retrieve chunk.id {} for job.id {}",
                        chunkIdentifier.chunkId, chunkIdentifier.jobId, e);
            }
        }
    }
}
