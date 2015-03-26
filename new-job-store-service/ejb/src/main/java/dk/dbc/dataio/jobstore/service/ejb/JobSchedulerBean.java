package dk.dbc.dataio.jobstore.service.ejb;

import dk.dbc.dataio.commons.time.StopWatch;
import dk.dbc.dataio.commons.types.ExternalChunk;
import dk.dbc.dataio.commons.types.Sink;
import dk.dbc.dataio.commons.utils.invariant.InvariantUtil;
import dk.dbc.dataio.jobstore.service.ejb.monitoring.SequenceAnalyserMonitorBean;
import dk.dbc.dataio.jobstore.service.ejb.monitoring.SequenceAnalyserMonitorMXBean;
import dk.dbc.dataio.jobstore.service.ejb.monitoring.SequenceAnalyserMonitorSample;
import dk.dbc.dataio.jobstore.types.JobStoreException;
import dk.dbc.dataio.sequenceanalyser.ChunkIdentifier;
import dk.dbc.dataio.sequenceanalyser.CollisionDetectionElement;
import dk.dbc.dataio.sequenceanalyser.SequenceAnalyser;
import dk.dbc.dataio.sequenceanalyser.naive.NaiveSequenceAnalyser;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.ConcurrencyManagement;
import javax.ejb.ConcurrencyManagementType;
import javax.ejb.EJB;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This Enterprise Java Bean (EJB) is responsible for chunk scheduling
 * via sequence analysis and for notifying the dataIO pipeline components of
 * available chunks.
 */
@Singleton
@Startup
@ConcurrencyManagement(ConcurrencyManagementType.BEAN)
public class JobSchedulerBean {
    private static final Logger LOGGER = LoggerFactory.getLogger(JobSchedulerBean.class);

    ConcurrentHashMap<String, SequenceAnalyserComposite> sequenceAnalysers = new ConcurrentHashMap<>(16, 0.9F, 1);
    ConcurrentHashMap<ChunkIdentifier, Sink> toSinkMapping = new ConcurrentHashMap<>(16, 0.9F, 1);

    @EJB
    PgJobStore jobStoreBean;

    @EJB
    SequenceAnalyserMonitorBean sequenceAnalyserMonitorBean;

    @EJB
    JobProcessorMessageProducerBean jobProcessorMessageProducerBean;

    /**
     * Passes given chunk collision detection element and sink on to the
     * sequence analyser and notifies pipeline of next available workload (if any)
     * @param chunkCDE next chunk collision detection element to enter into sequence analysis
     * @param sink sink associated with chunk
     * @throws NullPointerException if given any null-valued argument
     * @throws JobStoreException if unable to setup monitoring
     */
    public void scheduleChunk(CollisionDetectionElement chunkCDE, Sink sink) throws NullPointerException, JobStoreException {
        final StopWatch stopWatch = new StopWatch();
        try {
            InvariantUtil.checkNotNullOrThrow(chunkCDE, "cde");
            InvariantUtil.checkNotNullOrThrow(sink, "sink");
            final ChunkIdentifier chunkIdentifier = chunkCDE.getIdentifier();
            LOGGER.info("Scheduling chunk.id {} of job.id {}", chunkIdentifier.getChunkId(), chunkIdentifier.getJobId());
            toSinkMapping.put(chunkIdentifier, sink);
            final String lockObject = getLockObject(String.valueOf(sink.getId()));
            final List<ChunkIdentifier> workload;
            synchronized (lockObject) {
                final SequenceAnalyserComposite sac = getSequenceAnalyserComposite(lockObject, sink.getContent().getName());
                sac.sequenceAnalyser.addChunk(chunkCDE);
                updateMonitor(sac, sac.sequenceAnalyser.isHead(chunkIdentifier));
                workload = sac.sequenceAnalyser.getInactiveIndependentChunks();
            }
            publishWorkload(workload);
        } finally {
            LOGGER.debug("Operation took {} milliseconds", stopWatch.getElapsedTime());
        }
    }

    /**
     * Forces sequence analyser release of the identified chunk due to the
     * chunk no longer being present in the pipeline and notifies pipeline of
     * next available workload (if any)
     * @param chunkIdentifier chunk identifier
     * @throws JobStoreException if unable to setup monitoring
     */
    public void releaseChunk(ChunkIdentifier chunkIdentifier) throws JobStoreException {
        final StopWatch stopWatch = new StopWatch();
        try {
            if (chunkIdentifier != null) {
                LOGGER.info("Releasing chunk.id {} of job.id {}", chunkIdentifier.getChunkId(), chunkIdentifier.getJobId());
                List<ChunkIdentifier> workload;
                final Sink sink = toSinkMapping.get(chunkIdentifier);
                if (sink != null) {
                    final String lockObject = getLockObject(String.valueOf(sink.getId()));
                    synchronized (lockObject) {
                        final SequenceAnalyserComposite sac = getSequenceAnalyserComposite(lockObject, sink.getContent().getName());
                        workload = releaseAndReturnWorkload(sac, chunkIdentifier);
                    }
                    publishWorkload(workload);
                    toSinkMapping.remove(chunkIdentifier);
                } else {
                    // Somehow the toSinkMapping has gone out of sync with reality,
                    // so try to release chunk from all sequence analysers
                    for (Map.Entry<String, SequenceAnalyserComposite> entry : sequenceAnalysers.entrySet()) {
                        synchronized (entry.getKey()) {
                            workload = releaseAndReturnWorkload(entry.getValue(), chunkIdentifier);
                        }
                        publishWorkload(workload);
                    }
                }
            }
        } finally {
            LOGGER.debug("Operation took {} milliseconds", stopWatch.getElapsedTime());
        }
    }

    String getLockObject(String id) {
        // Add namespace to given string to avoid global locking issues.
        // Use intern() method to get reference from String pool, so
        // that the returned string can be used as a monitor object in
        // a synchronized block.
        return (this.getClass().getName() + "." + id).intern();
    }

    private void publishWorkload(List<ChunkIdentifier> chunkIdentifiers) {
        for (final ChunkIdentifier chunkIdentifier : chunkIdentifiers) {
            try {
                final ExternalChunk chunk = jobStoreBean.getChunk(ExternalChunk.Type.PARTITIONED,
                        (int) chunkIdentifier.getJobId(), (int) chunkIdentifier.getChunkId());
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
            } catch (NullPointerException e) {
                LOGGER.error("Unable to retrieve chunk.id {} for job.id {}",
                        chunkIdentifier.chunkId, chunkIdentifier.jobId, e);
            }
        }
    }

    @SuppressFBWarnings({"AT_OPERATION_SEQUENCE_ON_CONCURRENT_ABSTRACTION"})
    private SequenceAnalyserComposite getSequenceAnalyserComposite(String lockObject, String localName) throws JobStoreException {
        SequenceAnalyserComposite sequenceAnalyserComposite = sequenceAnalysers.get(lockObject);
        if (sequenceAnalyserComposite == null) {
            try {
                // Register new monitor in JMX with initial sample
                sequenceAnalyserMonitorBean.registerInJmx(localName);
            } catch (IllegalStateException e) {
                throw new JobStoreException("Monitoring error", e);
            }
            sequenceAnalyserMonitorBean.getMBeans().get(localName).setSample(
                    new SequenceAnalyserMonitorSample(0, new Date().getTime()));
            sequenceAnalyserComposite = new SequenceAnalyserComposite(
                    new NaiveSequenceAnalyser(), sequenceAnalyserMonitorBean.getMBeans().get(localName));
            sequenceAnalysers.put(lockObject, sequenceAnalyserComposite);
        }
        return sequenceAnalyserComposite;
    }

    private List<ChunkIdentifier> releaseAndReturnWorkload(SequenceAnalyserComposite sac, ChunkIdentifier chunkIdentifier) {
        final boolean isHead = sac.sequenceAnalyser.isHead(chunkIdentifier);
        sac.sequenceAnalyser.deleteAndReleaseChunk(chunkIdentifier);
        updateMonitor(sac, isHead);
        return sac.sequenceAnalyser.getInactiveIndependentChunks();
    }

    private void updateMonitor(SequenceAnalyserComposite sac, boolean isHead) {
        if (isHead) {
            // Specified chunk is at the head of the sequence analysis "queue", so
            // we update the sample with the current "queue" size and "now" timestamp
            sac.sequenceAnalyserMonitorMXBean.setSample(
                    new SequenceAnalyserMonitorSample(sac.sequenceAnalyser.size(), new Date().getTime()));
        } else {
            // Specified chunk is not at the head of the sequence analysis "queue", so
            // we update the sample with the current "queue" size and keep the old timestamp
            // since head of the "queue" still remains
            final long headOfQueueMonitoringStartTime = sac.sequenceAnalyserMonitorMXBean.getSample()
                    .getHeadOfQueueMonitoringStartTime();
            sac.sequenceAnalyserMonitorMXBean.setSample(
                    new SequenceAnalyserMonitorSample(sac.sequenceAnalyser.size(), headOfQueueMonitoringStartTime));
        }
    }

    static class SequenceAnalyserComposite {
        public final SequenceAnalyser sequenceAnalyser;
        public final SequenceAnalyserMonitorMXBean sequenceAnalyserMonitorMXBean;

        public SequenceAnalyserComposite(SequenceAnalyser sequenceAnalyser, SequenceAnalyserMonitorMXBean sequenceAnalyserMonitorMXBean) {
            this.sequenceAnalyser = sequenceAnalyser;
            this.sequenceAnalyserMonitorMXBean = sequenceAnalyserMonitorMXBean;
        }
    }
}
