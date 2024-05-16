package dk.dbc.dataio.jobstore.service.ejb;

import dk.dbc.dataio.commons.types.interceptor.Stopwatch;
import dk.dbc.dataio.jobstore.distributed.ChunkSchedulingStatus;
import dk.dbc.dataio.jobstore.distributed.JobSchedulerSinkStatus;
import dk.dbc.dataio.jobstore.service.dependencytracking.DependencyTrackingService;
import jakarta.ejb.EJB;
import jakarta.ejb.Schedule;
import jakarta.ejb.Singleton;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Future;

import static dk.dbc.dataio.jobstore.distributed.QueueSubmitMode.BULK;
import static dk.dbc.dataio.jobstore.distributed.QueueSubmitMode.DIRECT;
import static dk.dbc.dataio.jobstore.distributed.QueueSubmitMode.TRANSITION_TO_DIRECT;

/**
 * Created by ja7 on 03-07-16.
 * <p>
 * Singleton Bean Responsible for handling Sink's in bulkProcessing Mode
 * </p>
 * TODO switch back from bulkMode to DirectSubmit Mode not handled
 */
@Singleton
public class JobSchedulerBulkSubmitterBean {
    private static final Logger LOGGER = LoggerFactory.getLogger(JobSchedulerBulkSubmitterBean.class);
    @Inject
    DependencyTrackingService dependencyTrackingService;

    @EJB
    JobSchedulerBean jobSchedulerBean;

    @Schedule(second = "*/1", minute = "*", hour = "*", persistent = false)
    @Stopwatch
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void bulkScheduleChunksForDelivering() {
        dependencyTrackingService.getSinkStatusMap().forEach((sinkId, sinkQueueStatus) -> {
            try {
                JobSchedulerSinkStatus.QueueStatus queueStatus = sinkQueueStatus.getDeliveringStatus();

                if (queueStatus.getMode() == DIRECT) return;

                LOGGER.debug("prSink Delivering QueueMode for sink {} is {}", sinkId, queueStatus.getMode());
                doBulkJmsQueueSubmit(sinkId, queueStatus, ChunkSchedulingStatus.READY_FOR_DELIVERY);
            } catch (Exception e) {
                LOGGER.error("Error in sink for sink {}", sinkId, e);
            }
        });
    }


    @Schedule(second = "*/1", minute = "*", hour = "*", persistent = false)
    @Stopwatch
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void bulkScheduleChunksForProcessing() {
        dependencyTrackingService.getSinkStatusMap().forEach((sinkId, sinkQueueStatus) -> {
            try {
                JobSchedulerSinkStatus.QueueStatus queueStatus = sinkQueueStatus.getProcessingStatus();

                if (queueStatus.getMode() == DIRECT) return;

                LOGGER.debug("prSink Processing QueueMode for sink {} is {}", sinkId, queueStatus.getMode());
                doBulkJmsQueueSubmit(sinkId, queueStatus, ChunkSchedulingStatus.READY_FOR_PROCESSING);
            } catch (Exception e) {
                LOGGER.error("Error in Processing for sink {}", sinkId, e);
            }
        });
    }

    private void doBulkJmsQueueSubmit(Integer sinkId, JobSchedulerSinkStatus.QueueStatus queueStatus, ChunkSchedulingStatus phase) {
        int ready = dependencyTrackingService.getCount(sinkId, phase);
        int mark = ChunkSchedulingStatus.getTransitionToDirectMark();
        LOGGER.debug("prSink {} queue test {} < {} -> {} ", phase, ready, mark, ready < mark);
        if (queueStatus.getMode() == BULK && ready < mark) {
            LOGGER.debug("prSink {} Queue starting switch to DirectMode", phase);
            queueStatus.setMode(TRANSITION_TO_DIRECT);
            queueStatus.bulkToDirectCleanUpPushes = 0;
        }


        // If no Async Future exists call async
        if (queueStatus.lastAsyncPushResult == null) {
            queueStatus.lastAsyncPushResult = doAsyncBulkScheduleCallForPhase(sinkId, queueStatus, phase);
            return;
        }

        // A Future is present, if not done check again on next schedule
        if (!queueStatus.lastAsyncPushResult.isDone()) return;
        ready = dependencyTrackingService.getCount(sinkId, phase);
        LOGGER.debug("{} Async Result for sink {}, ready({})", phase, sinkId, ready);
        int lastAsyncPushedToQueue = -1;
        try {
            lastAsyncPushedToQueue = queueStatus.lastAsyncPushResult.get();
        } catch (Throwable e) {
            LOGGER.error("Exception thrown by Async Push to {} ", phase, e);
        }
        queueStatus.lastAsyncPushResult = null;

        if (queueStatus.getMode() == TRANSITION_TO_DIRECT) {
            queueStatus.bulkToDirectCleanUpPushes++;
        }


        // Check of done transition to directMode is complete

        if (queueStatus.bulkToDirectCleanUpPushes > 2 && lastAsyncPushedToQueue == 0) {
            int count = dependencyTrackingService.statusCount(sinkId, phase);
            if (count == 0) {
                LOGGER.info("prSink {} {} queue switched to {}", sinkId, phase, DIRECT);
                queueStatus.setMode(DIRECT);
            } else {
                LOGGER.info("prSink {} {} queue NOT switched to {} {} chunks found waiting", sinkId, phase, DIRECT, count);
            }
        }
    }

    private Future<Integer> doAsyncBulkScheduleCallForPhase(Integer sinkId, JobSchedulerSinkStatus.QueueStatus queueStatus, ChunkSchedulingStatus phase) {
        switch (phase) {
            case READY_FOR_PROCESSING:
                return jobSchedulerBean.bulkScheduleToProcessingForSink(sinkId);
            case READY_FOR_DELIVERY:
                return jobSchedulerBean.bulkScheduleToDeliveringForSink(sinkId, queueStatus);
            default:
                throw new IllegalArgumentException("Unknown Phase " + phase);
        }
    }
}
