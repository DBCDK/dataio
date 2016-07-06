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
@Singleton
public class JobSchedulerBulkSubmitterBean {
    private static final Logger LOGGER = LoggerFactory.getLogger(JobSchedulerBulkSubmitterBean.class);

    @Inject
    @JobstoreDB
    EntityManager entityManager;

    @EJB
    JobSchedulerTransactionsBean jobSchedulerTransactionsBean;


    private void bulkScheduleToDeliveringForSink(long sinkId, JobSchedulerPrSinkQueueStatuses.QueueStatus sinkQueueStatus ) {

        int queuedToDelivering= sinkQueueStatus.jmsEnqueued.intValue();
        LOGGER.info("bulkScheduleToDeliveringForSink: {} / {}-{}", sinkId, queuedToDelivering, sinkQueueStatus.readyForQueue.intValue() );
        int spaceInQueue = JobSchedulerBean.MAX_NUMBER_OF_CHUNKS_IN_DELIVERING_QUEUE_PER_SINK - queuedToDelivering;

        if( spaceInQueue > 0 ) {
            LOGGER.info("Space for more jobs for delivering {}<{} select limited to {}", queuedToDelivering, JobSchedulerBean.MAX_NUMBER_OF_CHUNKS_IN_DELIVERING_QUEUE_PER_SINK, spaceInQueue);

            Query query= entityManager.createQuery("select e from DependencyTrackingEntity e where e.sinkid=:sinkId and e.status=:state order by e.key.jobId, e.key.chunkId")
            .setParameter("sinkId", sinkId )
            .setParameter("state", DependencyTrackingEntity.ChunkProcessStatus.READY_TO_DELIVER)
            .setMaxResults(spaceInQueue);

            List<DependencyTrackingEntity> chunks=query.getResultList();
            LOGGER.info(" found {} chunks ready for delivering max({})", chunks.size(), spaceInQueue );
            for( DependencyTrackingEntity toSchedule : chunks ) {
                DependencyTrackingEntity.Key toScheduleKey=toSchedule.getKey();
                LOGGER.info(" Chunk ready to schedule {} for Delivering" ,toScheduleKey);
                jobSchedulerTransactionsBean.submitToDeliveringNewTransaction( jobSchedulerTransactionsBean.getProcessedChunkFrom( toSchedule ), sinkQueueStatus);
            }
        }
        // Todo reset to directDelivery mode if ReadyToDeliver is low
    }

    @Schedule(second="*/1", minute="*",hour="*", persistent=false)
    @Stopwatch
    @TransactionAttribute( TransactionAttributeType.REQUIRED)
    void bulkScheduleChunksForDelivering() {

        JobSchedulerBean.sinkStatusMap.forEach( Long.MAX_VALUE,  (sinkId, sinkQueueStatus) -> {
            LOGGER.info("prSink Delivering Mode for sink {} is {}", sinkId, sinkQueueStatus.deliveringStatus.getMode());
            if(sinkQueueStatus.isDeliveringModeDirectSubmit() ) return;
            bulkScheduleToDeliveringForSink( sinkId, sinkQueueStatus.deliveringStatus );
        });
    }

    private void bulkScheduleToProcessingForSink(long sinkId, JobSchedulerPrSinkQueueStatuses.QueueStatus prSinkQueueStatus ) {
        LOGGER.info("bulkScheduleToProcessingForSink( {} / {}-{}", sinkId, prSinkQueueStatus.jmsEnqueued.intValue(), prSinkQueueStatus.readyForQueue);
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
        JobSchedulerBean.sinkStatusMap.forEach(Long.MAX_VALUE, (sinkId, sinkQueueStatus) -> {
            LOGGER.info("prSink Processing Mode for sink {} is {}", sinkId, sinkQueueStatus.processingStatus.getMode());
            if (sinkQueueStatus.isProcessingModeDirectSubmit()) return;
            bulkScheduleToProcessingForSink(sinkId, sinkQueueStatus.processingStatus);

        });
    }
}
