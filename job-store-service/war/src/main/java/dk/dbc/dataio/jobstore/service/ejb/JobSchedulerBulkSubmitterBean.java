package dk.dbc.dataio.jobstore.service.ejb;

import dk.dbc.dataio.commons.types.interceptor.Stopwatch;
import dk.dbc.dataio.jobstore.service.cdi.JobstoreDB;
import static dk.dbc.dataio.jobstore.service.ejb.JobSchedulerBean.MAX_NUMBER_OF_CHUNKS_IN_DELIVERING_QUEUE_PER_SINK;
import static dk.dbc.dataio.jobstore.service.ejb.JobSchedulerBean.MAX_NUMBER_OF_CHUNKS_IN_PROCESSING_QUEUE_PER_SINK;
import static dk.dbc.dataio.jobstore.service.ejb.JobSchedulerBean.QueueMode.bulkSubmit;
import static dk.dbc.dataio.jobstore.service.ejb.JobSchedulerBean.QueueMode.directSubmit;
import static dk.dbc.dataio.jobstore.service.ejb.JobSchedulerBean.QueueMode.transitionToDirectSubmit;
import static dk.dbc.dataio.jobstore.service.ejb.JobSchedulerBean.sinkStatusMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.EJB;
import javax.ejb.Schedule;
import javax.ejb.Singleton;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;
import javax.persistence.EntityManager;

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
    JobSchedulerBean jobSchedulerBean;

    @Schedule(second="*/1", minute="*",hour="*", persistent=false)
    @Stopwatch
    @TransactionAttribute( TransactionAttributeType.REQUIRED)
    void bulkScheduleChunksForDelivering() {

        sinkStatusMap.forEach( Long.MAX_VALUE,  (sinkId, sinkQueueStatus) -> {
            JobSchedulerPrSinkQueueStatuses.QueueStatus queueStatus =sinkQueueStatus.deliveringStatus;
            LOGGER.info("prSink Delivering QueueMode for sink {} is {}", sinkId, queueStatus.getMode());

            if( queueStatus.getMode() == directSubmit) return;

            doBulkJmsQueueSubmit(sinkId, queueStatus, ProcessingOrDelivering.Delivering, MAX_NUMBER_OF_CHUNKS_IN_PROCESSING_QUEUE_PER_SINK);
        });
    }


    @Schedule(second = "*/1", minute = "*", hour = "*", persistent = false)
    @Stopwatch
    @TransactionAttribute( TransactionAttributeType.REQUIRED )
    void bulkScheduleChunksForProcessing() {
        sinkStatusMap.forEach(Long.MAX_VALUE, (sinkId, sinkQueueStatus) -> {
            JobSchedulerPrSinkQueueStatuses.QueueStatus queueStatus =sinkQueueStatus.processingStatus;
            LOGGER.info("prSink Processing QueueMode for sink {} is {}", sinkId, queueStatus.getMode());

            if (queueStatus.getMode() == directSubmit) return;

            doBulkJmsQueueSubmit(sinkId, queueStatus, ProcessingOrDelivering.Processing, MAX_NUMBER_OF_CHUNKS_IN_DELIVERING_QUEUE_PER_SINK);


        });
    }

    // Enum used for Logging and Knowning which phase of the system we are jmsBulkQueuing for
    private enum ProcessingOrDelivering {
        Processing,
        Delivering
    }


    private void doBulkJmsQueueSubmit(Long sinkId, JobSchedulerPrSinkQueueStatuses.QueueStatus queueStatus, ProcessingOrDelivering phase, int maxNumberOfChunksInQueuePerSink) {
        LOGGER.info("psSink {} queue test {} < {} -> {} ", phase, queueStatus.readyForQueue.intValue(), ( maxNumberOfChunksInQueuePerSink / 2 ),queueStatus.readyForQueue.intValue() < ( MAX_NUMBER_OF_CHUNKS_IN_PROCESSING_QUEUE_PER_SINK / 2 ));
        if( (queueStatus.getMode() == bulkSubmit) &&
            (queueStatus.readyForQueue.intValue() < ( MAX_NUMBER_OF_CHUNKS_IN_PROCESSING_QUEUE_PER_SINK / 3 )) )
        {
            LOGGER.info("psSink {} Queue starting switch to DirectMode", phase);
            queueStatus.setMode( transitionToDirectSubmit );
            queueStatus.bulkToDirectCleanUpPushes = 0;
        }


        // If no Async Future exists call async
        if( queueStatus.lastAsyncPushResult==null ) {
            switch (phase) {
                case Processing:
                    queueStatus.lastAsyncPushResult = jobSchedulerBean.bulkScheduleToProcessingForSink( sinkId, queueStatus);
                    break;
                case Delivering:
                    queueStatus.lastAsyncPushResult = jobSchedulerBean.bulkScheduleToDeliveringForSink( sinkId, queueStatus);
                    break;
            }
            return ;
        }

        LOGGER.info("{} Async Result for sink {}, readyForQueue({}), jmsEnqueued({})", phase, sinkId, queueStatus.readyForQueue.intValue(), queueStatus.jmsEnqueued.intValue());
        // A Future is present
        // If not done wait
        int lastAsyncPushedToQueue=0;
        if( !queueStatus.lastAsyncPushResult.isDone() ) return;
        try {
            lastAsyncPushedToQueue=queueStatus.lastAsyncPushResult.get();
        } catch (Throwable e) {
            LOGGER.error("Exception thrown by Async Push to {} ", phase, e);
        }
        queueStatus.lastAsyncPushResult=null;

        if( queueStatus.getMode() == transitionToDirectSubmit ) {
            queueStatus.bulkToDirectCleanUpPushes++;
        }


        // Check of done traisition to directMode is complete

        if(( queueStatus.bulkToDirectCleanUpPushes > 2) && lastAsyncPushedToQueue == 0)   {
            LOGGER.info("prSink {} {} queue switched to {}",sinkId, phase, directSubmit);
            queueStatus.setMode( directSubmit );
        }
    }
}
