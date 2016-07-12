package dk.dbc.dataio.jobstore.service.ejb;

import dk.dbc.dataio.commons.types.Chunk;
import dk.dbc.dataio.commons.types.Sink;
import dk.dbc.dataio.commons.types.interceptor.Stopwatch;
import dk.dbc.dataio.commons.utils.invariant.InvariantUtil;
import dk.dbc.dataio.jobstore.service.cdi.JobstoreDB;
import dk.dbc.dataio.jobstore.service.entity.ChunkEntity;
import dk.dbc.dataio.jobstore.service.entity.ConverterJSONBContext;
import dk.dbc.dataio.jobstore.service.entity.DependencyTrackingEntity;
import dk.dbc.dataio.jobstore.service.entity.DependencyTrackingEntity.ChunkProcessStatus;
import dk.dbc.dataio.jobstore.types.JobStoreException;
import dk.dbc.dataio.jsonb.JSONBException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Resource;
import javax.ejb.AsyncResult;
import javax.ejb.Asynchronous;
import javax.ejb.EJB;
import javax.ejb.SessionContext;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.LockModeType;
import javax.persistence.Query;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;

/**
 * Created by ja7 on 11-04-16.
 *
 * Handle Chunk Scheduling. Chunks Travels thu the ChunkProcessStatus stages.
 *
 * 2 Modes of operations i possible for Pr Sink and QueueType ( processing / Delivering )
 *
 *    DirectSubmit : When no Rate limiting is needed, Chunks messages is JMS queue
 *                   directly. we start in this mode
 *    BulkSubmit   : IF MAX_NUMBER_OF_.. chunks is in the jms queue, we switch to this mode.
 *                   And JobSchedulerBulkSubmitterBean handles the sending of JMS messages.
 *    transitionToDirectSubmit
 *    The Transition from BulkSubmit To DirectSuibmit mode must take at least 2 seconds to allow
 *    for alle chunks to be picked up by DirectSubmit.
 *
 *    Chunks i submitted directly to the JMS queue in the transistionToDirectSubmit mode.
 *    but the bulkSubmitter is also scanning for records to pickup Chunks added during mode Switch
 *
 *
 *
 * HACK:
 * Limits is pr jvm process.
 */
@Stateless
public class JobSchedulerBean {
    private static final Logger LOGGER = LoggerFactory.getLogger(JobSchedulerBean.class);

    enum QueueMode {
        directSubmit,  // In this mode the chunk is send to the JMS queue directly
        bulkSubmit, // In this mode the chunk is just added as ready for Processing/Delivering
        transitionToDirectSubmit // This is a transistional mode
    }


    // Max JMS Size pr Sink -- Test sizes overwritten for
    static int MAX_NUMBER_OF_CHUNKS_IN_DELIVERING_QUEUE_PER_SINK = 1000;
    static int MAX_NUMBER_OF_CHUNKS_IN_PROCESSING_QUEUE_PER_SINK = 1000;


    // Hash use to keep a count of pending jobs in JMS queues pr sink.
    // Small EJB violation for performance.
    // If the Application is run in multiple JVM's the limiter is pr jvm not pr application
    static final ConcurrentHashMap<Long, JobSchedulerPrSinkQueueStatuses> sinkStatusMap = new ConcurrentHashMap<>(16, 0.9F, 1);

    @Inject
    @JobstoreDB
    EntityManager entityManager;

    @EJB
    private JobSchedulerTransactionsBean jobSchedulerTransactionsBean;

    @Resource
    private SessionContext sessionContext;


    /**
     * ScheduleChunk
     * <p>
     * Passes given detection element and sink on to the
     * sequence analyser and notifies pipeline of next available workload (if any)
     *
     * @param chunk next chunk element to enter into sequence analysis
     * @param sink  sink associated with chunk
     * @throws NullPointerException if given any null-valued argument
     */
    @Stopwatch
    public void scheduleChunk(ChunkEntity chunk, Sink sink) {
        InvariantUtil.checkNotNullOrThrow(chunk, "chunk");
        InvariantUtil.checkNotNullOrThrow(sink, "sink");

        // Proxy to self to get new Transaction
        jobSchedulerTransactionsBean.persistDependencyEntity(chunk, sink );

        // Check before Submit to avoid unnecessary Async Call.

        if (getPrSinkStatusForSinkId(sink.getId()).isProcessingModeDirectSubmit()) {
            jobSchedulerTransactionsBean.submitToProcessingIfPossibleAsync(chunk, sink.getId());
        }
    }


    /**
     * Register Chunk Processing is Done.
     * Chunks not i state QUEUED_TO_PROCESS is ignored.
     *
     * @param chunk Chunk completed from processing
     * @throws JobStoreException if Unable to Load Chunk
     */
    @Stopwatch
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void chunkProcessingDone(Chunk chunk) throws JobStoreException {
        final DependencyTrackingEntity.Key key = new DependencyTrackingEntity.Key(chunk.getJobId(), chunk.getChunkId());
        DependencyTrackingEntity dependencyTrackingEntity = entityManager.find(DependencyTrackingEntity.class, key, LockModeType.PESSIMISTIC_WRITE);

        if (dependencyTrackingEntity == null) {
            LOGGER.info("chunkProcessingDone called with unknown Chunk {} - Assuming it is already completed ", key);
            return;
        }

        if (dependencyTrackingEntity.getStatus() != ChunkProcessStatus.QUEUED_TO_PROCESS) {
            LOGGER.info("chunkProcessingDone called with chunk not in state QUEUED_TO_PROCESS {} was {} ", key, dependencyTrackingEntity.getStatus());
            return;
        }

        int sinkId = dependencyTrackingEntity.getSinkid();
        JobSchedulerPrSinkQueueStatuses.QueueStatus prSinkQueueStatus = getPrSinkStatusForSinkId(sinkId).processingStatus;
        prSinkQueueStatus.jmsEnqueued.decrementAndGet();
        prSinkQueueStatus.readyForQueue.decrementAndGet();
        LOGGER.info("chunkProcessingDone: prSinkQueueStatus.jmsEnqueuedToProcessing: {}", prSinkQueueStatus.jmsEnqueued.intValue());

        if (dependencyTrackingEntity.getWaitingOn().size() != 0) {
            dependencyTrackingEntity.setStatus(ChunkProcessStatus.BLOCKED);
            LOGGER.debug("chunk {} blocked by {} ", key, dependencyTrackingEntity.getWaitingOn());
        }
        else {
            jobSchedulerTransactionsBean.submitToDeliveringIfPossible(chunk, dependencyTrackingEntity);
        }
    }



    /**
     * Register a chunk as Delivered, and remove it from dependency tracking.
     * <p>
     * If called Multiple times with the same chunk, or chunk not in QUEUED_TO_DELIVERY the chunk is ignored
     *
     * @param chunk Chunk Done
     * @throws JobStoreException on failure to queue other chunks
     */
    @Stopwatch
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void chunkDeliveringDone(Chunk chunk) throws JobStoreException {
        final DependencyTrackingEntity.Key key = new DependencyTrackingEntity.Key(chunk.getJobId(), chunk.getChunkId());
        DependencyTrackingEntity doneChunk = entityManager.find(DependencyTrackingEntity.class, key, LockModeType.PESSIMISTIC_WRITE);

        if (doneChunk == null) {
            LOGGER.info("chunkDeliveringDone called with unknown Chunk {} - Assuming it is already completed ", key);
            return;
        }
        if (doneChunk.getStatus() != ChunkProcessStatus.QUEUED_TO_DELIVERY) {
            LOGGER.info("chunkDeliveringDone called with chunk {}, not in state QUEUED_TO_DELIVERY {} -- chunk Ignored", key, doneChunk.getStatus());
            return;
        }

        // Decrement early to make space for in queue.  -- most important when queue size is 1, when unit testing

        long doneChunkSinkId = doneChunk.getSinkid();
        DependencyTrackingEntity.Key doneChunkKey = doneChunk.getKey();
        entityManager.remove(doneChunk);

        JobSchedulerPrSinkQueueStatuses.QueueStatus sinkQueueStatus = getPrSinkStatusForSinkId(doneChunkSinkId).deliveringStatus;

        sinkQueueStatus.jmsEnqueued.decrementAndGet();
        sinkQueueStatus.readyForQueue.decrementAndGet();

        List<DependencyTrackingEntity.Key> chunksWaitingForMe = findChunksWaitingForMe(doneChunkKey);

        for (DependencyTrackingEntity.Key blockChunkKey : chunksWaitingForMe) {
            DependencyTrackingEntity blockedChunk = entityManager.find(DependencyTrackingEntity.class, blockChunkKey, LockModeType.PESSIMISTIC_WRITE);

            blockedChunk.getWaitingOn().remove(doneChunkKey);

            if (blockedChunk.getWaitingOn().size() == 0) {
                if (blockedChunk.getStatus() == ChunkProcessStatus.BLOCKED) {
                    blockedChunk.setStatus(ChunkProcessStatus.READY_TO_DELIVER);
                    sinkQueueStatus.readyForQueue.incrementAndGet();
                    if (sinkQueueStatus.isDirectSubmitMode()) {
                        jobSchedulerTransactionsBean.submitToDeliveringIfPossible(jobSchedulerTransactionsBean.getProcessedChunkFrom(blockedChunk), blockedChunk);
                    }
                }
            }
        }
    }

    @Asynchronous
    @TransactionAttribute( TransactionAttributeType.REQUIRED )
    public Future<Integer> bulkScheduleToProcessingForSink(long sinkId, JobSchedulerPrSinkQueueStatuses.QueueStatus prSinkQueueStatus ) {
        LOGGER.info("bulkScheduleToProcessingForSink( {} / {}-{}", sinkId, prSinkQueueStatus.jmsEnqueued.intValue(), prSinkQueueStatus.readyForQueue);
        int ChunksPushedToQueue=0;
        try {
            int queuedToProcessing = prSinkQueueStatus.jmsEnqueued.intValue();
            LOGGER.info("bulkScheduleToProcessing: prSinkQueueStatus.jmsEnqueuedToProcessing: {}", queuedToProcessing);
            final int spaceInQueue = JobSchedulerBean.MAX_NUMBER_OF_CHUNKS_IN_PROCESSING_QUEUE_PER_SINK - queuedToProcessing;

            if ( spaceInQueue > 0 ) {
                LOGGER.info("Space for more jobs to Processing {} < {}", queuedToProcessing, JobSchedulerBean.MAX_NUMBER_OF_CHUNKS_IN_PROCESSING_QUEUE_PER_SINK);

                Query query = entityManager.createQuery("select e from DependencyTrackingEntity e where e.sinkid=:sinkId and e.status=:state order by e.key.jobId, e.key.chunkId")
                        .setParameter("sinkId", sinkId)
                        .setParameter("state", DependencyTrackingEntity.ChunkProcessStatus.READY_TO_PROCESS)
                        .setMaxResults(spaceInQueue);

                List<DependencyTrackingEntity> chunks = query.getResultList();
                LOGGER.info(" found {} chunks ready for delivering max({})", chunks.size(), spaceInQueue);

                for (DependencyTrackingEntity toSchedule : chunks) {
                    DependencyTrackingEntity.Key toScheduleKey = toSchedule.getKey();
                    LOGGER.info(" Chunk ready to schedule {} to Processing", toScheduleKey);
                    ChunkEntity ch = entityManager.find(ChunkEntity.class, new ChunkEntity.Key(toScheduleKey.getChunkId(), toScheduleKey.getJobId()));
                    jobSchedulerTransactionsBean.submitToProcessing( ch, prSinkQueueStatus);
                    ChunksPushedToQueue++;
                }
            }

        } catch( Exception ex) {
            LOGGER.error("Error in bulkProcessing", ex);
        }
        return new AsyncResult<>(ChunksPushedToQueue);
    }


    List<DependencyTrackingEntity.Key> findChunksWaitingForMe(DependencyTrackingEntity.Key key) throws JobStoreException {
        try {
            String keyAsJson = ConverterJSONBContext.getInstance().marshall(key);

            Query query = entityManager.createNativeQuery("select jobid, chunkid from dependencyTracking where waitingOn @> '[" + keyAsJson + "]' ORDER BY jobid, chunkid ", "JobIdChunkIdResult");
            return query.getResultList();

        } catch (JSONBException e) {
            LOGGER.error("Unable to serialize DependencyTrackingKey to JSON in JobSchedulerBean", e);
            throw new JobStoreException("Unable to serialize DependencyTrackingKey to JSON", e);
        }

    }

    static JobSchedulerPrSinkQueueStatuses getPrSinkStatusForSinkId(long sinkId) {
        return sinkStatusMap.computeIfAbsent(sinkId, k -> new JobSchedulerPrSinkQueueStatuses());
    }

    // Helper method for Automatic Tests
    static void ForTesting_ResetPrSinkStatuses() {
        sinkStatusMap.replaceAll((k, v) -> new JobSchedulerPrSinkQueueStatuses());
    }

}
