package dk.dbc.dataio.jobstore.service.ejb;

import dk.dbc.dataio.commons.types.Chunk;
import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.types.Priority;
import dk.dbc.dataio.commons.types.Sink;
import dk.dbc.dataio.commons.types.SinkContent;
import dk.dbc.dataio.commons.types.interceptor.Stopwatch;
import dk.dbc.dataio.commons.utils.invariant.InvariantUtil;
import dk.dbc.dataio.jobstore.service.cdi.JobstoreDB;
import dk.dbc.dataio.jobstore.service.entity.ChunkEntity;
import dk.dbc.dataio.jobstore.service.entity.ConverterJSONBContext;
import dk.dbc.dataio.jobstore.service.entity.DependencyTrackingEntity;
import dk.dbc.dataio.jobstore.service.entity.DependencyTrackingEntity.ChunkSchedulingStatus;
import dk.dbc.dataio.jobstore.service.entity.JobEntity;
import dk.dbc.dataio.jobstore.service.entity.SinkIdStatusCountResult;
import dk.dbc.dataio.jobstore.types.JobStoreException;
import dk.dbc.dataio.jobstore.types.State;
import dk.dbc.dataio.jsonb.JSONBException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.AsyncResult;
import javax.ejb.Asynchronous;
import javax.ejb.EJB;
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
 *    DIRECT       : When no Rate limiting is needed, Chunks messages is JMS queue
 *                   directly. we start in this mode
 *    BULK        : IF MAX_NUMBER_OF_.. chunks is in the jms queue, we switch to this mode.
 *                   And JobSchedulerBulkSubmitterBean handles the sending of JMS messages.
 *    TRANSITION_TO_DIRECT
 *    The Transition from BulkSubmit To Direct submit mode must take at least 2 seconds to allow
 *    for all chunks to be picked up by Direct Submit.
 *
 *    Chunks i submitted directly to the JMS queue in the transitionToDirectSubmit mode.
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

    enum QueueSubmitMode {
        DIRECT,  // In this mode the chunk is send to the JMS queue directly
        BULK, // In this mode the chunk is just added as ready for Processing/Delivering
        TRANSITION_TO_DIRECT // This is a transitional mode
    }

    // Max JMS Size pr Sink -- Test sizes overwritten for
    @SuppressWarnings("EjbClassWarningsInspection")
    static int MAX_NUMBER_OF_CHUNKS_IN_DELIVERING_QUEUE_PER_SINK = 1000;
    @SuppressWarnings("EjbClassWarningsInspection")
    static int MAX_NUMBER_OF_CHUNKS_IN_PROCESSING_QUEUE_PER_SINK = 1000;

    // if Number of Chunks in JMS Quque
    @SuppressWarnings("EjbClassWarningsInspection")
    static int TRANSITION_TO_DIRECT_MARK = 50;


    // Hash use to keep a count of pending jobs in JMS queues pr sink.
    // Small EJB violation for performance.
    // If the Application is run in multiple JVM's the limiter is pr jvm not pr application
    static final ConcurrentHashMap<Long, JobSchedulerSinkStatus> sinkStatusMap = new ConcurrentHashMap<>(16, 0.9F, 1);

    @Inject
    @JobstoreDB
    EntityManager entityManager;

    @EJB
    protected JobSchedulerTransactionsBean jobSchedulerTransactionsBean;

    @EJB
    protected PgJobStoreRepository pgJobStoreRepository;

    public JobSchedulerBean withEntityManager(EntityManager entityManager) {
        this.entityManager = entityManager;
        return this;
    }

    /**
     * ScheduleChunk
     * <p>
     * Passes given detection element and sink on to the
     * sequence analyser and notifies pipeline of next available workload (if any)
     *
     * @param chunk next chunk element to enter into sequence analysis
     * @param sink  sink associated with chunk
     * @param priority chunk priority
     * @param dataSetId DataSet to be used by Tickle Sink dependency Tracking
     * @throws NullPointerException if given any null-valued argument
     */
    @Stopwatch
    public void scheduleChunk(ChunkEntity chunk, Sink sink, Priority priority, long dataSetId) {
        InvariantUtil.checkNotNullOrThrow(chunk, "chunk");
        InvariantUtil.checkNotNullOrThrow(sink, "sink");
        InvariantUtil.checkNotNullOrThrow(priority, "priority");
        int sinkId = (int) sink.getId();

        DependencyTrackingEntity e;
        if (sink.getContent().getSinkType() == SinkContent.SinkType.TICKLE && chunk.getKey().getId() == 0) {
            e = new DependencyTrackingEntity(chunk, sinkId, Long.toString(dataSetId));
        } else {
            e = new DependencyTrackingEntity(chunk, sinkId, null );
        }

        String extraMatchKey = null;
        if (sink.getContent().getSinkType() == SinkContent.SinkType.TICKLE && chunk.getKey().getId() > 0) {
            extraMatchKey = Long.toString(dataSetId);
        }

        e.setPriority(priority.getValue());

        jobSchedulerTransactionsBean.persistDependencyEntity(e, extraMatchKey);

        // check before submit to avoid unnecessary Async call.
        if (getSinkStatus(sink.getId()).isProcessingModeDirectSubmit()) {
            jobSchedulerTransactionsBean.submitToProcessingIfPossibleAsync(chunk, sink.getId(), e.getPriority());
        }
    }


    /**
     * If job is TICKLE scheduled Add Special Barrier Chunk with one Special JobTermination Record
     *
     * @param jobEntity the job.
     * @throws JobStoreException on createJobTerminationChunkEntity errors
     */
    @Stopwatch
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void markJobPartitioned(JobEntity jobEntity) throws JobStoreException {
        
        if( jobEntity.getNumberOfChunks() == 0) return;
        if( jobEntity.getNumberOfChunks() == 1 && jobEntity.hasFatalError() ) {
            // on FatalErrors and only One Chunk.. The chunk is properly not send.
            // But check The DB for 0 succeeded from partitioning
            JobEntity dbJobEntity = entityManager.find(JobEntity.class, jobEntity.getId());
            entityManager.refresh( dbJobEntity );
            if (dbJobEntity.getState().getPhase(State.Phase.PARTITIONING).getSucceeded() == 0) return;
        }

        ChunkItem.Status terminationStatus = ChunkItem.Status.SUCCESS;
        if (jobEntity.hasFatalDiagnostics()) {
            terminationStatus = ChunkItem.Status.FAILURE;
        }


        markJobPartitioned(jobEntity.getId(),jobEntity.getCachedSink().getSink(),jobEntity.getNumberOfChunks(), jobEntity.lookupDataSetId(), terminationStatus );
    }
    


    /**
     * If job is TICKLE scheduled Add Special Barrier Chunk with one Special JobTermination Record
     *
     * @param jobId jobId,
     * @param sink sinkId,
     * @param chunkId id of job termination chunk
     * @param dataSetId DataSetId to be used for Tickle sink.
     * @param ItemStatus  status for tickle termination item
     * @throws JobStoreException on createJobTerminationChunkEntity errors
     */
    void markJobPartitioned(int jobId, Sink sink, int chunkId, long dataSetId, ChunkItem.Status ItemStatus) throws JobStoreException {
        if( sink.getContent().getSinkType() != SinkContent.SinkType.TICKLE) return;
        int sinkId = (int) sink.getId();


        ChunkEntity chunkEntity=pgJobStoreRepository.createJobTerminationChunkEntity( jobId, chunkId, "dummyDatafileId", ItemStatus);

        DependencyTrackingEntity jobEndBarrierTrackingEntity= new DependencyTrackingEntity(chunkEntity, sinkId, String.valueOf(dataSetId));


        jobSchedulerTransactionsBean.persistJobTerminationDependencyEntity(jobEndBarrierTrackingEntity);

        if( jobEndBarrierTrackingEntity.getStatus() == ChunkSchedulingStatus.READY_FOR_DELIVERY) {
            JobSchedulerSinkStatus.QueueStatus sinkQueueStatus = getSinkStatus(sinkId).deliveringStatus;
            sinkQueueStatus.ready.incrementAndGet();
        }

        jobSchedulerTransactionsBean.submitToDeliveringIfPossible(jobSchedulerTransactionsBean.getProcessedChunkFrom(jobEndBarrierTrackingEntity), jobEndBarrierTrackingEntity);
    }


    /**
     * Register Chunk Processing is Done.
     * Chunks not i state QUEUED_FOR_PROCESSING is ignored.
     *
     * @param chunk Chunk completed from processing
     */
    @Stopwatch
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void chunkProcessingDone(Chunk chunk) {
        final DependencyTrackingEntity.Key key = new DependencyTrackingEntity.Key(chunk.getJobId(), chunk.getChunkId());
        DependencyTrackingEntity dependencyTrackingEntity = entityManager.find(DependencyTrackingEntity.class, key, LockModeType.PESSIMISTIC_WRITE);

        if (dependencyTrackingEntity == null) {
            LOGGER.info("chunkProcessingDone called with unknown Chunk {} - Assuming it is already completed ", key);
            return;
        }

        if (dependencyTrackingEntity.getStatus() != ChunkSchedulingStatus.QUEUED_FOR_PROCESSING) {
            LOGGER.info("chunkProcessingDone called with chunk not in state QUEUED_FOR_PROCESSING {} was {} ", key, dependencyTrackingEntity.getStatus());
            return;
        }

        int sinkId = dependencyTrackingEntity.getSinkid();
        JobSchedulerSinkStatus.QueueStatus prSinkQueueStatus = getSinkStatus(sinkId).processingStatus;
        prSinkQueueStatus.enqueued.decrementAndGet();
        prSinkQueueStatus.ready.decrementAndGet();
        LOGGER.info("chunkProcessingDone: prSinkQueueStatus.jmsEnqueuedToProcessing: {}", prSinkQueueStatus.enqueued.intValue());

        if (dependencyTrackingEntity.getWaitingOn().size() != 0) {
            dependencyTrackingEntity.setStatus(ChunkSchedulingStatus.BLOCKED);
            LOGGER.debug("chunk {} blocked by {} ", key, dependencyTrackingEntity.getWaitingOn());
        } else {
            dependencyTrackingEntity.setStatus(ChunkSchedulingStatus.READY_FOR_DELIVERY);

            JobSchedulerSinkStatus.QueueStatus sinkQueueStatus = getSinkStatus(sinkId).deliveringStatus;
            sinkQueueStatus.ready.incrementAndGet();
        }

        jobSchedulerTransactionsBean.submitToDeliveringIfPossible(chunk, dependencyTrackingEntity);
    }



    /**
     * Register a chunk as Delivered, and remove it from dependency tracking.
     * <p>
     * If called Multiple times with the same chunk, or chunk not in QUEUED_FOR_DELIVERY the chunk is ignored
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
        if (doneChunk.getStatus() != ChunkSchedulingStatus.QUEUED_FOR_DELIVERY) {
            LOGGER.info("chunkDeliveringDone called with chunk {}, not in state QUEUED_FOR_DELIVERY {} -- chunk Ignored", key, doneChunk.getStatus());
            return;
        }

        // Decrement early to make space for in queue.  -- most important when queue size is 1, when unit testing

        long doneChunkSinkId = doneChunk.getSinkid();
        DependencyTrackingEntity.Key doneChunkKey = doneChunk.getKey();
        entityManager.remove(doneChunk);

        JobSchedulerSinkStatus.QueueStatus sinkQueueStatus = getSinkStatus(doneChunkSinkId).deliveringStatus;

        sinkQueueStatus.enqueued.decrementAndGet();
        sinkQueueStatus.ready.decrementAndGet();

        List<DependencyTrackingEntity.Key> chunksWaitingForMe = findChunksWaitingForMe(doneChunkKey);

        for (DependencyTrackingEntity.Key blockChunkKey : chunksWaitingForMe) {
            DependencyTrackingEntity blockedChunk = entityManager.find(DependencyTrackingEntity.class, blockChunkKey, LockModeType.PESSIMISTIC_WRITE);

            blockedChunk.getWaitingOn().remove(doneChunkKey);

            if (blockedChunk.getWaitingOn().size() == 0) {
                if (blockedChunk.getStatus() == ChunkSchedulingStatus.BLOCKED) {
                    blockedChunk.setStatus(ChunkSchedulingStatus.READY_FOR_DELIVERY);
                    sinkQueueStatus.ready.incrementAndGet();
                    if (sinkQueueStatus.isDirectSubmitMode()) {
                        jobSchedulerTransactionsBean.submitToDeliveringIfPossible(jobSchedulerTransactionsBean.getProcessedChunkFrom(blockedChunk), blockedChunk);
                    }
                }
            }
        }
    }

    @Asynchronous
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public Future<Integer> bulkScheduleToProcessingForSink(long sinkId, JobSchedulerSinkStatus.QueueStatus queueStatus ) {
        int chunksPushedToQueue = 0;
        try {
            final int ready = queueStatus.ready.intValue();
            final int enqueued = queueStatus.enqueued.intValue();
            LOGGER.info("bulk scheduling for processing - sink {} enqueued={} ready={}", sinkId, enqueued, ready);

            final int spaceLeftInQueue = JobSchedulerBean.MAX_NUMBER_OF_CHUNKS_IN_PROCESSING_QUEUE_PER_SINK - enqueued;
            if (spaceLeftInQueue > 0) {
                LOGGER.info("bulk scheduling for processing - sink {} has space left in queue for {} chunks", sinkId, spaceLeftInQueue);

                final List<DependencyTrackingEntity> chunks = entityManager
                        .createNamedQuery(DependencyTrackingEntity.BY_SINKID_AND_STATE_QUERY, DependencyTrackingEntity.class)
                        .setParameter("sinkId", sinkId)
                        .setParameter("state", ChunkSchedulingStatus.READY_FOR_PROCESSING)
                        .setMaxResults(spaceLeftInQueue)
                        .getResultList();

                LOGGER.info("bulk scheduling for processing - found {} chunks ready for processing for sink {}", chunks.size(), sinkId);
                for (DependencyTrackingEntity toSchedule : chunks) {
                    final DependencyTrackingEntity.Key toScheduleKey = toSchedule.getKey();
                    LOGGER.info("bulk scheduling for processing - chunk {} to be scheduled for processing for sink {}", toScheduleKey, sinkId);
                    final ChunkEntity chunk = entityManager.find(ChunkEntity.class, new ChunkEntity.Key(toScheduleKey.getChunkId(), toScheduleKey.getJobId()));
                    jobSchedulerTransactionsBean.submitToProcessing(chunk, queueStatus, toSchedule.getPriority());
                    chunksPushedToQueue++;
                }
            }
        } catch(Exception ex) {
            LOGGER.error("Error in bulk scheduling for processing for sink {}", sinkId, ex);
        }
        return new AsyncResult<>(chunksPushedToQueue);
    }

    @Asynchronous
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public Future<Integer> bulkScheduleToDeliveringForSink(long sinkId, JobSchedulerSinkStatus.QueueStatus queueStatus ) {
        int chunksPushedToQueue = 0;
        try {
            final int ready = queueStatus.ready.intValue();
            final int enqueued = queueStatus.enqueued.intValue();
            LOGGER.info("bulk scheduling for delivery - sink {} enqueued={} ready={}", sinkId, enqueued, ready);

            final int spaceLeftInQueue = MAX_NUMBER_OF_CHUNKS_IN_DELIVERING_QUEUE_PER_SINK - enqueued;
            if (spaceLeftInQueue > 0) {
                LOGGER.info("bulk scheduling for delivery - sink {} has space left in queue for {} chunks", sinkId, spaceLeftInQueue);

                final List<DependencyTrackingEntity> chunks = entityManager
                        .createNamedQuery(DependencyTrackingEntity.BY_SINKID_AND_STATE_QUERY, DependencyTrackingEntity.class)
                        .setParameter("sinkId", sinkId)
                        .setParameter("state", ChunkSchedulingStatus.READY_FOR_DELIVERY)
                        .setMaxResults(spaceLeftInQueue)
                        .getResultList();

                LOGGER.info("bulk scheduling for delivery - found {} chunks ready for processing for sink {}", chunks.size(), sinkId);
                for (DependencyTrackingEntity toSchedule : chunks) {
                    final DependencyTrackingEntity.Key toScheduleKey = toSchedule.getKey();
                    LOGGER.info("bulk scheduling for delivery - chunk {} to be scheduled for delivery for sink {}", toScheduleKey, sinkId);
                    jobSchedulerTransactionsBean.submitToDeliveringNewTransaction(
                            jobSchedulerTransactionsBean.getProcessedChunkFrom(toSchedule), queueStatus);
                    chunksPushedToQueue++;
                }
            }
        } catch(Exception ex) {
            LOGGER.error("Error in bulk scheduling for delivery for sink {}", sinkId, ex);
        }
        return new AsyncResult<>(chunksPushedToQueue);
    }

    /**
     *  Reload and reset counters for sinks
     *  Set all sinks to BULK mode to ensure progress on redeploy of service
     */
    @Stopwatch
    @TransactionAttribute( TransactionAttributeType.REQUIRED )
    public void loadSinkStatusOnBootstrap() {
        final List<SinkIdStatusCountResult> initialCounts = entityManager
                .createNamedQuery(DependencyTrackingEntity.SINKID_STATUS_COUNT_QUERY, SinkIdStatusCountResult.class)
                .getResultList();

        for (SinkIdStatusCountResult entry: initialCounts) {
            final JobSchedulerSinkStatus sinkStatus = getSinkStatus(entry.sinkId);
            switch (entry.status) {
                case QUEUED_FOR_PROCESSING:
                    sinkStatus.processingStatus.enqueued.addAndGet(entry.count);
                    // intended fallthrough
                case READY_FOR_PROCESSING:
                    sinkStatus.processingStatus.ready.addAndGet(entry.count);
                    break;
                case QUEUED_FOR_DELIVERY:
                    sinkStatus.deliveringStatus.enqueued.addAndGet(entry.count);
                    // intended fallthrough
                case READY_FOR_DELIVERY:
                    sinkStatus.deliveringStatus.ready.addAndGet(entry.count);
                    break;
                case BLOCKED: // blocked chunks are not counted
                    break;
            }
            sinkStatus.processingStatus.setMode(QueueSubmitMode.BULK);
            sinkStatus.deliveringStatus.setMode(QueueSubmitMode.BULK);
        }
    }

    List<DependencyTrackingEntity.Key> findChunksWaitingForMe(DependencyTrackingEntity.Key key) throws JobStoreException {
        try {
            String keyAsJson = ConverterJSONBContext.getInstance().marshall(key);

            Query query = entityManager.createNativeQuery("select jobid, chunkid from dependencyTracking where waitingOn @> '[" + keyAsJson + "]' ORDER BY jobid, chunkid ", DependencyTrackingEntity.KEY_RESULT);
            return query.getResultList();

        } catch (JSONBException e) {
            LOGGER.error("Unable to serialize DependencyTrackingKey to JSON in JobSchedulerBean", e);
            throw new JobStoreException("Unable to serialize DependencyTrackingKey to JSON", e);
        }

    }

    @SuppressWarnings("EjbClassBasicInspection")
    static JobSchedulerSinkStatus getSinkStatus(long sinkId) {
        return sinkStatusMap.computeIfAbsent(sinkId, k -> new JobSchedulerSinkStatus());
    }

    // Helper method for Automatic Tests
    @SuppressWarnings("EjbClassBasicInspection")
    static void resetAllSinkStatuses() {
        sinkStatusMap.replaceAll((k, v) -> new JobSchedulerSinkStatus());
    }
}
