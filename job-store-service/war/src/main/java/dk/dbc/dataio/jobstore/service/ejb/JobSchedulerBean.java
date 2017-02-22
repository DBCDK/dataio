package dk.dbc.dataio.jobstore.service.ejb;

import dk.dbc.dataio.commons.types.Chunk;
import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.types.Sink;
import dk.dbc.dataio.commons.types.SinkContent;
import dk.dbc.dataio.commons.types.interceptor.Stopwatch;
import dk.dbc.dataio.commons.utils.invariant.InvariantUtil;
import dk.dbc.dataio.jobstore.service.cdi.JobstoreDB;
import dk.dbc.dataio.jobstore.service.entity.ChunkEntity;
import dk.dbc.dataio.jobstore.service.entity.ConverterJSONBContext;
import dk.dbc.dataio.jobstore.service.entity.DependencyTrackingEntity;
import dk.dbc.dataio.jobstore.service.entity.DependencyTrackingEntity.ChunkProcessStatus;
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
    static final ConcurrentHashMap<Long, JobSchedulerPrSinkQueueStatuses> sinkStatusMap = new ConcurrentHashMap<>(16, 0.9F, 1);

    @Inject
    @JobstoreDB
    EntityManager entityManager;

    @EJB
    protected JobSchedulerTransactionsBean jobSchedulerTransactionsBean;

    @EJB
    protected PgJobStoreRepository pgJobStoreRepository;


    /**
     * ScheduleChunk
     * <p>
     * Passes given detection element and sink on to the
     * sequence analyser and notifies pipeline of next available workload (if any)
     *
     * @param chunk next chunk element to enter into sequence analysis
     * @param sink  sink associated with chunk
     * @param dataSetId DataSet to be used by Tickle Sink Decadency Tracking
     * @throws NullPointerException if given any null-valued argument
     */
    @Stopwatch
    public void scheduleChunk(ChunkEntity chunk, Sink sink, long dataSetId) {
        InvariantUtil.checkNotNullOrThrow(chunk, "chunk");
        InvariantUtil.checkNotNullOrThrow(sink, "sink");

        int sinkId = (int) sink.getId();
        DependencyTrackingEntity e;
        String extraMatchKey=null;


        if ( sink.getContent().getSinkType() == SinkContent.SinkType.TICKLE && chunk.getKey().getId() == 0)
        {
            e = new DependencyTrackingEntity(chunk, sinkId, Long.toString(dataSetId));
        } else {
            e = new DependencyTrackingEntity(chunk, sinkId, null );
        }

        if ( sink.getContent().getSinkType() == SinkContent.SinkType.TICKLE && chunk.getKey().getId() > 0) {
            extraMatchKey = Long.toString(dataSetId);
        }

        jobSchedulerTransactionsBean.persistDependencyEntity(e, extraMatchKey);

        // Check before Submit to avoid unnecessary Async Call.

        if (getPrSinkStatusForSinkId(sink.getId()).isProcessingModeDirectSubmit()) {
            jobSchedulerTransactionsBean.submitToProcessingIfPossibleAsync(chunk, sink.getId());
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
     * @param chunkId id of job termination chunk.
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

        if( jobEndBarrierTrackingEntity.getStatus() == ChunkProcessStatus.READY_TO_DELIVER ) {
            JobSchedulerPrSinkQueueStatuses.QueueStatus sinkQueueStatus = getPrSinkStatusForSinkId(sinkId).deliveringStatus;
            sinkQueueStatus.readyForQueue.incrementAndGet();
        }

        jobSchedulerTransactionsBean.submitToDeliveringIfPossible(jobSchedulerTransactionsBean.getProcessedChunkFrom(jobEndBarrierTrackingEntity), jobEndBarrierTrackingEntity);
    }


    /**
     * Register Chunk Processing is Done.
     * Chunks not i state QUEUED_TO_PROCESS is ignored.
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
        } else {
            dependencyTrackingEntity.setStatus(ChunkProcessStatus.READY_TO_DELIVER );

            JobSchedulerPrSinkQueueStatuses.QueueStatus sinkQueueStatus = getPrSinkStatusForSinkId(sinkId).deliveringStatus;
            sinkQueueStatus.readyForQueue.incrementAndGet();
        }

        jobSchedulerTransactionsBean.submitToDeliveringIfPossible(chunk, dependencyTrackingEntity);
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
    @TransactionAttribute( TransactionAttributeType.REQUIRES_NEW )
    public Future<Integer> bulkScheduleToProcessingForSink(long sinkId, JobSchedulerPrSinkQueueStatuses.QueueStatus prSinkQueueStatus ) {
        LOGGER.info("bulkScheduleToProcessingForSink( {} / {}-{}", sinkId, prSinkQueueStatus.jmsEnqueued.intValue(), prSinkQueueStatus.readyForQueue);
        int chunksPushedToQueue=0;
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
                    chunksPushedToQueue++;
                }
            }

        } catch( Exception ex) {
            LOGGER.error("Error in bulk submit to Processing for sink {}", sinkId, ex);
        }
        return new AsyncResult<>(chunksPushedToQueue);
    }

    @Asynchronous
    @TransactionAttribute( TransactionAttributeType.REQUIRED )
    public Future<Integer> bulkScheduleToDeliveringForSink(long sinkId, JobSchedulerPrSinkQueueStatuses.QueueStatus sinkQueueStatus ) {

        int chunksPushedToQueue=0;
        try {

            int queuedToDelivering = sinkQueueStatus.jmsEnqueued.intValue();
            LOGGER.info("bulkScheduleToDeliveringForSink: {} / {}-{}", sinkId, queuedToDelivering, sinkQueueStatus.readyForQueue.intValue());
            int spaceInQueue = MAX_NUMBER_OF_CHUNKS_IN_DELIVERING_QUEUE_PER_SINK - queuedToDelivering;

            if (spaceInQueue > 0) {
                LOGGER.info("Space for more jobs for delivering {}<{} select limited to {}", queuedToDelivering, MAX_NUMBER_OF_CHUNKS_IN_DELIVERING_QUEUE_PER_SINK, spaceInQueue);

                Query query = entityManager.createQuery("select e from DependencyTrackingEntity e where e.sinkid=:sinkId and e.status=:state order by e.key.jobId, e.key.chunkId")
                        .setParameter("sinkId", sinkId)
                        .setParameter("state", DependencyTrackingEntity.ChunkProcessStatus.READY_TO_DELIVER)
                        .setMaxResults(spaceInQueue);

                List<DependencyTrackingEntity> chunks = query.getResultList();
                LOGGER.info(" found {} chunks ready for delivering max({})", chunks.size(), spaceInQueue);
                for (DependencyTrackingEntity toSchedule : chunks) {
                    DependencyTrackingEntity.Key toScheduleKey = toSchedule.getKey();
                    LOGGER.info(" Chunk ready to schedule {} for Delivering", toScheduleKey);
                    jobSchedulerTransactionsBean.submitToDeliveringNewTransaction(jobSchedulerTransactionsBean.getProcessedChunkFrom(toSchedule), sinkQueueStatus);
                    chunksPushedToQueue++;
                }
            }
        } catch( Exception ex) {
            LOGGER.error("Error in bulk submit to Delivering for sink {}", sinkId, ex);
        }
        return new AsyncResult<>(chunksPushedToQueue);
    }

    /**
     *  Reload and reset Counters form current Sink status.
     *  Set alle sinks to BulkMode to ensure progress on redeploy of service
     */
    @TransactionAttribute( TransactionAttributeType.REQUIRED )
    @Stopwatch
    public void loadSinkStatusOnBootstrap() {
        List<SinkIdStatusCountResult> res=entityManager.createNamedQuery("SinkIdStatusCount").getResultList();

        for( SinkIdStatusCountResult entry: res ) {
            JobSchedulerPrSinkQueueStatuses sinkQueueStatuses=getPrSinkStatusForSinkId( entry.sinkId );
            switch (entry.status) {
                case QUEUED_TO_PROCESS:
                    sinkQueueStatuses.processingStatus.jmsEnqueued.addAndGet( entry.count );
                    // intended fallthrough
                case READY_TO_PROCESS:
                    sinkQueueStatuses.processingStatus.readyForQueue.addAndGet(entry.count );
                    break;
                case QUEUED_TO_DELIVERY:
                    sinkQueueStatuses.deliveringStatus.jmsEnqueued.addAndGet( entry.count);
                    // intended fallthrough
                case READY_TO_DELIVER:
                    sinkQueueStatuses.deliveringStatus.readyForQueue.addAndGet( entry.count);
                    break;
                case BLOCKED: // blocked chunks is not counted
                    break;
            }
            sinkQueueStatuses.processingStatus.setMode(QueueSubmitMode.BULK);
            sinkQueueStatuses.deliveringStatus.setMode(QueueSubmitMode.BULK);
        }
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

    @SuppressWarnings("EjbClassBasicInspection")
    static JobSchedulerPrSinkQueueStatuses getPrSinkStatusForSinkId(long sinkId) {
        return sinkStatusMap.computeIfAbsent(sinkId, k -> new JobSchedulerPrSinkQueueStatuses());
    }

    // Helper method for Automatic Tests
    @SuppressWarnings("EjbClassBasicInspection")
    static void ForTesting_ResetPrSinkStatuses() {
        sinkStatusMap.replaceAll((k, v) -> new JobSchedulerPrSinkQueueStatuses());
    }

}
