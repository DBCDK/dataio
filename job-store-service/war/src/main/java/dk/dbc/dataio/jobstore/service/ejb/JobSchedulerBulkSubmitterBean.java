package dk.dbc.dataio.jobstore.service.ejb;

import dk.dbc.dataio.commons.types.interceptor.Stopwatch;
import dk.dbc.dataio.jobstore.service.cdi.JobstoreDB;
import static dk.dbc.dataio.jobstore.service.ejb.JobSchedulerBean.MAX_NUMBER_OF_CHUNKS_IN_DELIVERING_QUEUE_PER_SINK;
import static dk.dbc.dataio.jobstore.service.ejb.JobSchedulerBean.MAX_NUMBER_OF_CHUNKS_IN_PROCESSING_QUEUE_PER_SINK;
import static dk.dbc.dataio.jobstore.service.ejb.JobSchedulerBean.QueueMode.directSubmit;
import static dk.dbc.dataio.jobstore.service.ejb.JobSchedulerBean.QueueMode.transitionToDirectSubmit;
import static dk.dbc.dataio.jobstore.service.ejb.JobSchedulerBean.sinkStatusMap;
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


    @EJB // TODO be removed when Delivering Modes is set for
    JobSchedulerTransactionsBean jobSchedulerTransactionsBean;

    @EJB
    JobSchedulerBean jobSchedulerBean;


    private void bulkScheduleToDeliveringForSink(long sinkId, JobSchedulerPrSinkQueueStatuses.QueueStatus sinkQueueStatus ) {

        int queuedToDelivering= sinkQueueStatus.jmsEnqueued.intValue();
        LOGGER.info("bulkScheduleToDeliveringForSink: {} / {}-{}", sinkId, queuedToDelivering, sinkQueueStatus.readyForQueue.intValue() );
        int spaceInQueue = MAX_NUMBER_OF_CHUNKS_IN_DELIVERING_QUEUE_PER_SINK - queuedToDelivering;

        if( spaceInQueue > 0 ) {
            LOGGER.info("Space for more jobs for delivering {}<{} select limited to {}", queuedToDelivering, MAX_NUMBER_OF_CHUNKS_IN_DELIVERING_QUEUE_PER_SINK, spaceInQueue);

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

        sinkStatusMap.forEach( Long.MAX_VALUE,  (sinkId, sinkQueueStatus) -> {
            LOGGER.info("prSink Delivering Mode for sink {} is {}", sinkId, sinkQueueStatus.deliveringStatus.getMode());
            if(sinkQueueStatus.isDeliveringModeDirectSubmit() ) return;
            bulkScheduleToDeliveringForSink( sinkId, sinkQueueStatus.deliveringStatus );
        });
    }


    @Schedule(second = "*/1", minute = "*", hour = "*", persistent = false)
    @Stopwatch
    void bulkScheduleChunksForProcessing() {
        sinkStatusMap.forEach(Long.MAX_VALUE, (sinkId, sinkQueueStatus) -> {
            JobSchedulerPrSinkQueueStatuses.QueueStatus queueStatus =sinkQueueStatus.processingStatus;
            LOGGER.info("prSink Processing Mode for sink {} is {}", sinkId, queueStatus.getMode());

            // Ignore queues in DirectSubmitMode
            if (queueStatus.isDirectSubmitMode()) return;

            LOGGER.info("psSink processing queue test {} < {} -> {} ",queueStatus.readyForQueue.intValue(), ( MAX_NUMBER_OF_CHUNKS_IN_PROCESSING_QUEUE_PER_SINK / 2 ),queueStatus.readyForQueue.intValue() < ( MAX_NUMBER_OF_CHUNKS_IN_PROCESSING_QUEUE_PER_SINK / 2 ));
            if( queueStatus.readyForQueue.intValue() < ( MAX_NUMBER_OF_CHUNKS_IN_PROCESSING_QUEUE_PER_SINK / 2 )) {
                LOGGER.info("psSink processing Queue starting switch to DirectMode");
                queueStatus.setMode( transitionToDirectSubmit );
                queueStatus.bulkToDirectCleanUpPushes = 0;
            }


            // If no Async Future exists call async
            if( queueStatus.lastAsyncPushResult==null ) {
                queueStatus.lastAsyncPushResult = jobSchedulerBean.bulkScheduleToProcessingForSink(sinkId, sinkQueueStatus.processingStatus);
                return ;
            }
            LOGGER.info("Processing Async Result for sink {}, readyForQueue({}), jmsEnqueued({})", sinkId, queueStatus.readyForQueue.intValue(), queueStatus.jmsEnqueued.intValue());
            // A Future is present
            // If not done wait
            int lastAsyncPushedToQueue=0;
            if( !queueStatus.lastAsyncPushResult.isDone() ) return;
            try {
                lastAsyncPushedToQueue=queueStatus.lastAsyncPushResult.get();
            } catch (Throwable e) {
                LOGGER.error("Exception thrown by Async Push ",e);
            }
            queueStatus.lastAsyncPushResult=null;

            if( queueStatus.getMode() == transitionToDirectSubmit ) {
                queueStatus.bulkToDirectCleanUpPushes++;
            }


            // Check of done traisition to directMode is complete

            if(( queueStatus.bulkToDirectCleanUpPushes > 2) && lastAsyncPushedToQueue == 0)   {
                queueStatus.setMode( directSubmit );
            }


        });
    }
}
