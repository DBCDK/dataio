package dk.dbc.dataio.jobstore.ejb;

import dk.dbc.dataio.commons.types.Chunk;
import dk.dbc.dataio.commons.types.Sink;
import dk.dbc.dataio.commons.utils.invariant.InvariantUtil;
import dk.dbc.dataio.jobstore.types.JobStoreException;
import dk.dbc.dataio.sequenceanalyser.SequenceAnalyser;
import dk.dbc.dataio.sequenceanalyser.naive.ChunkIdentifier;
import dk.dbc.dataio.sequenceanalyser.naive.NaiveSequenceAnalyser;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.ConcurrencyManagement;
import javax.ejb.ConcurrencyManagementType;
import javax.ejb.EJB;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

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

    ConcurrentHashMap<String, SequenceAnalyser> sequenceAnalysers = new ConcurrentHashMap<>(16, 0.9F, 1);
    ConcurrentHashMap<ChunkIdentifier, Sink> toSinkMapping = new ConcurrentHashMap<>(16, 0.9F, 1);

    @EJB
    JobProcessorMessageProducerBean jobProcessorMessageProducerBean;

    @EJB
    JobStoreBean jobStoreBean;

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
        final List<ChunkIdentifier> inactiveIndependentChunks;
        LOGGER.info("Scheduling chunk.id {} of job.id {}", chunk.getChunkId(), chunk.getJobId());
        toSinkMapping.put(new ChunkIdentifier(chunk.getJobId(), chunk.getChunkId()), sink);
        final String lockObject = getLockObject(String.valueOf(sink.getId()));
        synchronized(lockObject) {
            final SequenceAnalyser sequenceAnalyser = getSequenceAnalyser(lockObject);
            sequenceAnalyser.addChunk(chunk);
            inactiveIndependentChunks = sequenceAnalyser.getInactiveIndependentChunks();
        }
        notifyJobProcessorOfWorkloadAvailable(inactiveIndependentChunks);
    }

    /**
     * Forces sequence analyser release of the chunk identified by given
     * job and chunk IDs due to the chunk no longer being present in the
     * pipeline and notifies pipeline of next available workload (if any)
     * @param jobId identifier of job containing chunk
     * @param chunkId identifier of chunk in containing job
     */
    public void releaseChunk(long jobId, long chunkId) {
        final List<ChunkIdentifier> inactiveIndependentChunks;
        LOGGER.info("Releasing chunk.id {} of job.id {}", chunkId, jobId);
        final ChunkIdentifier chunkIdentifier = new ChunkIdentifier(jobId, chunkId);
        final Sink sink = toSinkMapping.get(chunkIdentifier);
        if (sink != null) {
            final String lockObject = getLockObject(String.valueOf(sink.getId()));
            synchronized (lockObject) {
                final SequenceAnalyser sequenceAnalyser = getSequenceAnalyser(lockObject);
                sequenceAnalyser.deleteAndReleaseChunk(chunkIdentifier);
                inactiveIndependentChunks = sequenceAnalyser.getInactiveIndependentChunks();
            }
            notifyJobProcessorOfWorkloadAvailable(inactiveIndependentChunks);
            toSinkMapping.remove(chunkIdentifier);
        }
    }

    String getLockObject(String id) {
        // Add namespace to given string to avoid global locking issues.
        // Use intern() method to get reference from String pool, so
        // that the returned string can be used as a monitor object in
        // a synchronized block.
        return (this.getClass().getName() + "." + id).intern();
    }

    private void notifyJobProcessorOfWorkloadAvailable(List<ChunkIdentifier> chunkIdentifiers) {
        for (final ChunkIdentifier chunkIdentifier : chunkIdentifiers) {
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

    @SuppressFBWarnings({"AT_OPERATION_SEQUENCE_ON_CONCURRENT_ABSTRACTION"})
    private SequenceAnalyser getSequenceAnalyser(String lockObject) {
        SequenceAnalyser sequenceAnalyser = sequenceAnalysers.get(lockObject);
        if (sequenceAnalyser == null) {
            sequenceAnalyser = new NaiveSequenceAnalyser();
            sequenceAnalysers.put(lockObject, sequenceAnalyser);
        }
        return sequenceAnalyser;
    }
}
