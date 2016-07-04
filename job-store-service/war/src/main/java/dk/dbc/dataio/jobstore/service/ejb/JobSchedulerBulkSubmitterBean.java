package dk.dbc.dataio.jobstore.service.ejb;

import dk.dbc.dataio.commons.types.interceptor.Stopwatch;
import dk.dbc.dataio.jobstore.service.cdi.JobstoreDB;
import dk.dbc.dataio.jobstore.service.entity.ChunkEntity;
import dk.dbc.dataio.jobstore.service.entity.DependencyTrackingEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.EJB;
import javax.ejb.Schedule;
import javax.ejb.Singleton;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.LockModeType;
import javax.persistence.Query;
import java.util.List;

/**
 * Created by ja7 on 03-07-16.
 *
 * Singleton Bean Responsible for handling Sink's in bulkProcessing Mode
 *
 *
 * TODO switch back from bulkMode to DirectSubmit Mode not handled
 */
@Singleton(name = "JobSchedulerBulkSubmitterBeanEJB")
public class JobSchedulerBulkSubmitterBean {
    private static final Logger LOGGER = LoggerFactory.getLogger(JobSchedulerBulkSubmitterBean.class);

    @Inject
    @JobstoreDB
    EntityManager entityManager;



    static Boolean bulkProcessingSchedulingRunning = new Boolean(false);
    static Boolean bulkDeliveringSchedulingRunning = new Boolean(false);

    @EJB
    JobSchedulerTransactionsBean jobSchedulerTransactionsBean;


    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    @Stopwatch
    void bulkScheduleToDeliveringForSink(long sinkId, JobSchedulerPrSinkQueueStatus sinkQueueStatus ) {

        int queuedToDelivering= sinkQueueStatus.jmsEnqueuedToDelivering.intValue();
        LOGGER.info("bulkScheduleToDeliveringForSink: {} / {}-{}", sinkId, queuedToDelivering, sinkQueueStatus.readyForDelivering.intValue() );

        if( queuedToDelivering <= JobSchedulerBean.MAX_NUMBER_OF_CHUNKS_IN_DELIVERING_QUEUE_PER_SINK) {
            LOGGER.info("Space for more jobs for delivering {}<{} ", queuedToDelivering, JobSchedulerBean.MAX_NUMBER_OF_CHUNKS_IN_DELIVERING_QUEUE_PER_SINK);
            Query query= entityManager.createQuery("select e from DependencyTrackingEntity e where e.sinkid=:sinkId and e.status=:state order by e.key.jobId, e.key.chunkId")
            .setParameter("sinkId", sinkId )
            .setParameter("state", DependencyTrackingEntity.ChunkProcessStatus.READY_TO_DELIVER)
            .setMaxResults(JobSchedulerBean.MAX_NUMBER_OF_CHUNKS_IN_DELIVERING_QUEUE_PER_SINK -queuedToDelivering + 1)
            .setLockMode( LockModeType.PESSIMISTIC_WRITE);
            List<DependencyTrackingEntity> chunks=query.getResultList();
            LOGGER.info(" found {} chunks ready for delivering max({})", chunks.size(), JobSchedulerBean.MAX_NUMBER_OF_CHUNKS_IN_DELIVERING_QUEUE_PER_SINK -queuedToDelivering + 10);
            for( DependencyTrackingEntity toSchedule : chunks ) {
                DependencyTrackingEntity.Key toScheduleKey=toSchedule.getKey();
                LOGGER.info(" Chunk ready to schedule {} for Delivering" ,toScheduleKey);

                jobSchedulerTransactionsBean.submitToDelivering( jobSchedulerTransactionsBean.getProcessedChunkFrom( toSchedule ), toSchedule, sinkQueueStatus);
            }
        }
        // Todo reset to directDelivery mode if ReadyToDeliver is low
    }

    @Schedule(second="*/1", minute="*",hour="*", persistent=false)
    @Stopwatch
    @TransactionAttribute( TransactionAttributeType.REQUIRED)
    void bulkScheduleChunksForDelivering() {

        synchronized (bulkDeliveringSchedulingRunning) {
            LOGGER.info("bulkDeliveringScheduling Active ");
            if (bulkDeliveringSchedulingRunning == true) return;
            bulkDeliveringSchedulingRunning = true;
        }
        try {
            JobSchedulerBean.sinkStatusMap.forEach( Long.MAX_VALUE,  (sinkId, sinkQueueStatus) -> {
                LOGGER.info("PrSink {} Delivering Mode is {}", sinkId, sinkQueueStatus.getDeliveringMode());
                if(sinkQueueStatus.isDeliveringModeDirectSubmit() ) return;
                bulkScheduleToDeliveringForSink( sinkId, sinkQueueStatus );
            });
        } finally {
            synchronized (bulkDeliveringSchedulingRunning) {
                LOGGER.info("bulkDeliveringScheduling DONE ");
                bulkDeliveringSchedulingRunning = false;
            }
        }
    }

    @Stopwatch
    void bulkScheduleToProcessingForSink(long sinkId, JobSchedulerPrSinkQueueStatus prSinkQueueStatus ) {
        LOGGER.info("bulkScheduleToProcessingForSink( {} / {}-{}", sinkId, prSinkQueueStatus.jmsEnqueuedToProcessing.intValue(), prSinkQueueStatus.readyForDelivering);
        try {
            int queuedToProcessing = prSinkQueueStatus.jmsEnqueuedToProcessing.intValue();
            LOGGER.info("bulkScheduleToProcessing: prSinkQueueStatus.jmsEnqueuedToProcessing: {}", queuedToProcessing);
            if (queuedToProcessing < JobSchedulerBean.MAX_NUMBER_OF_CHUNKS_IN_PROCESSING_QUEUE_PER_SINK) {
                LOGGER.info("Space for more jobs to Processing {} < {}", queuedToProcessing, JobSchedulerBean.MAX_NUMBER_OF_CHUNKS_IN_PROCESSING_QUEUE_PER_SINK);
                Query query = entityManager.createQuery("select e from DependencyTrackingEntity e where e.sinkid=:sinkId and e.status=:state order by e.key.jobId, e.key.chunkId")
                        .setParameter("sinkId", sinkId)
                        .setParameter("state", DependencyTrackingEntity.ChunkProcessStatus.READY_TO_PROCESS)
                        .setMaxResults(JobSchedulerBean.MAX_NUMBER_OF_CHUNKS_IN_PROCESSING_QUEUE_PER_SINK - queuedToProcessing + 1)
                        .setLockMode(LockModeType.PESSIMISTIC_WRITE);


                List<DependencyTrackingEntity> chunks = query.getResultList();


                LOGGER.info(" found {} chunks ready for delivering max({})", chunks.size(), JobSchedulerBean.MAX_NUMBER_OF_CHUNKS_IN_DELIVERING_QUEUE_PER_SINK - queuedToProcessing + 10);
                for (DependencyTrackingEntity toSchedule : chunks) {
                    DependencyTrackingEntity.Key toScheduleKey = toSchedule.getKey();
                    LOGGER.info(" Chunk ready to schedule {} to Processing", toScheduleKey);
                    ChunkEntity ch = entityManager.find(ChunkEntity.class, new ChunkEntity.Key(toScheduleKey.getChunkId(), toScheduleKey.getJobId()));
                    jobSchedulerTransactionsBean.submitToProcessing(toSchedule, ch, prSinkQueueStatus);
                }
            }

        } catch( Exception ex) {
            LOGGER.error("Error in bulkProcessing", ex);
        }
        // Todo: fallback to directSchedulingMode
    }

    @Schedule(second = "*/1", minute = "*", hour = "*", persistent = false)
    @Stopwatch
    void bulkScheduleChunksForProcessing() {
        synchronized (bulkProcessingSchedulingRunning) {
            LOGGER.info("bulkProcessingScheduling Active ");
            if (bulkProcessingSchedulingRunning == true) return;
            bulkProcessingSchedulingRunning = true;
        }
        try {
            JobSchedulerBean.sinkStatusMap.forEach( Long.MAX_VALUE,  (sinkId, sinkQueueStatus) -> {
                LOGGER.info("prSink {} Processing Mode is {}", sinkId, sinkQueueStatus.getProcessingMode());
                if( sinkQueueStatus.isProcessingModeDirectSubmit() ) return;
                bulkScheduleToProcessingForSink( sinkId, sinkQueueStatus);

            });
        } finally {
            synchronized (bulkProcessingSchedulingRunning) {
                LOGGER.info("bulkProcessingScheduling DONE ");
                bulkProcessingSchedulingRunning = false;
            }
        }
    }
}
