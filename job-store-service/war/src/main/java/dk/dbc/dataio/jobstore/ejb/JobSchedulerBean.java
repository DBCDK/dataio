package dk.dbc.dataio.jobstore.ejb;

import dk.dbc.dataio.commons.types.Chunk;
import dk.dbc.dataio.commons.types.Sink;
import dk.dbc.dataio.commons.utils.invariant.InvariantUtil;
import dk.dbc.dataio.jobstore.types.JobStoreException;
import dk.dbc.dataio.sequenceanalyser.SequenceAnalyser;
import dk.dbc.dataio.sequenceanalyser.naive.ChunkIdentifier;

import javax.annotation.PostConstruct;
import javax.ejb.ConcurrencyManagement;
import javax.ejb.ConcurrencyManagementType;
import javax.ejb.EJB;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.util.ArrayList;
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
    SequenceAnalyser sequenceAnalyser;
    ConcurrentHashMap<ChunkIdentifier, Chunk> workRemaining;

    @EJB
    JobProcessorMessageProducerBean jobProcessorMessageProducer;

    @PostConstruct
    public void initialise() {
        sequenceAnalyser = new SequenceAnalyserImpl();
        workRemaining = new ConcurrentHashMap<>();
    }

    /**
     * Passes given chunk and sink on to the sequence analyser and notifies
     * pipeline of next available workload (if any)
     * @param chunk next chunk to enter into sequence analysis
     * @param sink  sink associated with chunk
     * @throws NullPointerException if given any null-valued argument
     * @throws JobStoreException on failure to notify
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void scheduleChunk(Chunk chunk, Sink sink) throws NullPointerException, JobStoreException {
        InvariantUtil.checkNotNullOrThrow(chunk, "chunk");
        InvariantUtil.checkNotNullOrThrow(sink, "sink");
        workRemaining.put(getChunkIdentifierFor(chunk), chunk);
        sequenceAnalyser.addChunk(chunk, sink);
        notifyWorkloadAvailable();
    }

    /**
     * Forces sequence analyser release of the chunk identified by given
     * job and chunk IDs due to the chunk no longer being present in the
     * pipeline and notifies pipeline of next available workload (if any)
     * @param jobId identifier of job containing chunk
     * @param chunkId identifier of chunk in containing job
     * @throws JobStoreException on failure to notify
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void releaseChunk(long jobId, long chunkId) throws JobStoreException {
        final ChunkIdentifier chunkIdentifier = new ChunkIdentifier(jobId, chunkId);
        workRemaining.remove(chunkIdentifier);
        sequenceAnalyser.deleteAndReleaseChunk(chunkIdentifier);
        notifyWorkloadAvailable();
    }

    private ChunkIdentifier getChunkIdentifierFor(Chunk chunk) {
        return new ChunkIdentifier(chunk.getJobId(), chunk.getChunkId());
    }

    private void notifyWorkloadAvailable() throws JobStoreException {
        for (final ChunkIdentifier chunkIdentifier : sequenceAnalyser.getInactiveIndependentChunks()) {
            jobProcessorMessageProducer.send(workRemaining.get(chunkIdentifier));
        }
    }

    /* Temporary sequence analyser implementation
     */
    private class SequenceAnalyserImpl implements SequenceAnalyser {
        private final List<ChunkIdentifier> active;
        private final List<ChunkIdentifier> ready;

        private SequenceAnalyserImpl() {
            active = new ArrayList<>();
            ready = new ArrayList<>();
        }

        @Override
        public synchronized void addChunk(Chunk chunk, Sink sink) {
            ready.add(getChunkIdentifierFor(chunk));
        }

        @Override
        public synchronized void deleteAndReleaseChunk(ChunkIdentifier identifier) {
            for(ChunkIdentifier chunkIdentifier : active) {
                if (chunkIdentifier.equals(identifier)) {
                    active.remove(identifier);
                    break;
                }
            }
        }

        @Override
        public synchronized List<ChunkIdentifier> getInactiveIndependentChunks() {
            final List<ChunkIdentifier> identifiers = new ArrayList<>();
            for (final ChunkIdentifier chunkIdentifier : ready) {
                active.add(chunkIdentifier);
                identifiers.add(chunkIdentifier);
            }
            for (final ChunkIdentifier chunkIdentifier : identifiers) {
                ready.remove(chunkIdentifier);
            }
            return identifiers;
        }

        @Deprecated
        @Override
        public void activateChunk(ChunkIdentifier identifier) {
        }

        @Override
        public synchronized int size() {
            return  active.size() + ready.size();
        }
    }
}
