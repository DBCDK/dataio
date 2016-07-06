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


    private void bulkScheduleToDeliveringForSink(long sinkId, JobSchedulerPrSinkQueueStatus sinkQueueStatus ) {

        int queuedToDelivering= sinkQueueStatus.jmsEnqueuedToDelivering.intValue();
        LOGGER.info("bulkScheduleToDeliveringForSink: {} / {}-{}", sinkId, queuedToDelivering, sinkQueueStatus.readyForDelivering.intValue() );
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
                jobSchedulerTransactionsBean.submitToDelivering( jobSchedulerTransactionsBean.getProcessedChunkFrom( toSchedule ), toSchedule, sinkQueueStatus);
            }
        }
        // Todo reset to directDelivery mode if ReadyToDeliver is low
    }

    @Schedule(second="*/1", minute="*",hour="*", persistent=false)
    @Stopwatch
    @TransactionAttribute( TransactionAttributeType.REQUIRED)
    void bulkScheduleChunksForDelivering() {

        JobSchedulerBean.sinkStatusMap.forEach( Long.MAX_VALUE,  (sinkId, sinkQueueStatus) -> {
            LOGGER.info("prSink Delivering Mode for sink {} is {}", sinkId, sinkQueueStatus.getProcessingMode());
            if(sinkQueueStatus.isDeliveringModeDirectSubmit() ) return;
            bulkScheduleToDeliveringForSink( sinkId, sinkQueueStatus );
        });
    }

    private void bulkScheduleToProcessingForSink(long sinkId, JobSchedulerPrSinkQueueStatus prSinkQueueStatus ) {
        LOGGER.info("bulkScheduleToProcessingForSink( {} / {}-{}", sinkId, prSinkQueueStatus.jmsEnqueuedToProcessing.intValue(), prSinkQueueStatus.readyForDelivering);
        try {
            int queuedToProcessing = prSinkQueueStatus.jmsEnqueuedToProcessing.intValue();
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
        JobSchedulerBean.sinkStatusMap.forEach(Long.MAX_VALUE, (sinkId, sinkQueueStatus) -> {
            LOGGER.info("prSink Processing Mode for sink {} is {}", sinkId, sinkQueueStatus.getProcessingMode());
            if (sinkQueueStatus.isProcessingModeDirectSubmit()) return;
            bulkScheduleToProcessingForSink(sinkId, sinkQueueStatus);

        });
    }
}
