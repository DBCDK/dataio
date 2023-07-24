package dk.dbc.dataio.jobstore.service.ejb;

import dk.dbc.dataio.commons.types.interceptor.Stopwatch;
import dk.dbc.dataio.jobstore.service.cdi.JobstoreDB;
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
import java.util.concurrent.Future;

import static dk.dbc.dataio.jobstore.service.ejb.JobSchedulerBean.QueueSubmitMode.BULK;
import static dk.dbc.dataio.jobstore.service.ejb.JobSchedulerBean.QueueSubmitMode.DIRECT;
import static dk.dbc.dataio.jobstore.service.ejb.JobSchedulerBean.QueueSubmitMode.TRANSITION_TO_DIRECT;
import static dk.dbc.dataio.jobstore.service.ejb.JobSchedulerBean.sinkStatusMap;

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
    @JobstoreDB
    EntityManager entityManager;

    @EJB
    JobSchedulerBean jobSchedulerBean;

    @Schedule(second = "*/1", minute = "*", hour = "*", persistent = false)
    @Stopwatch
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void bulkScheduleChunksForDelivering() {

        sinkStatusMap.forEach((sinkId, sinkQueueStatus) -> {
            try {
                JobSchedulerSinkStatus.QueueStatus queueStatus = sinkQueueStatus.deliveringStatus;

                if (queueStatus.getMode() == DIRECT) return;

                LOGGER.debug("prSink Delivering QueueMode for sink {} is {}", sinkId, queueStatus.getMode());
                doBulkJmsQueueSubmit(sinkId, queueStatus, ProcessingOrDelivering.Delivering);
            } catch (Exception e) {
                LOGGER.error("Error in sink for sink {}", sinkId, e);
            }
        });
    }


    @Schedule(second = "*/1", minute = "*", hour = "*", persistent = false)
    @Stopwatch
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void bulkScheduleChunksForProcessing() {
        sinkStatusMap.forEach((sinkId, sinkQueueStatus) -> {
            try {
                JobSchedulerSinkStatus.QueueStatus queueStatus = sinkQueueStatus.processingStatus;

                if (queueStatus.getMode() == DIRECT) return;

                LOGGER.debug("prSink Processing QueueMode for sink {} is {}", sinkId, queueStatus.getMode());
                doBulkJmsQueueSubmit(sinkId, queueStatus, ProcessingOrDelivering.Processing);
            } catch (Exception e) {
                LOGGER.error("Error in Processing for sink {}", sinkId, e);
            }
        });
    }

    // Enum used for Logging and Knowing which phase of the system we are jmsBulkQueuing for
    private enum ProcessingOrDelivering {
        Processing,
        Delivering
    }


    private void doBulkJmsQueueSubmit(Long sinkId, JobSchedulerSinkStatus.QueueStatus queueStatus, ProcessingOrDelivering phase) {
        LOGGER.debug("prSink {} queue test {} < {} -> {} ", phase, queueStatus.ready.intValue(), JobSchedulerBean.TRANSITION_TO_DIRECT_MARK, queueStatus.ready.intValue() < (JobSchedulerBean.TRANSITION_TO_DIRECT_MARK));
        if (queueStatus.getMode() == BULK && queueStatus.ready.intValue() < JobSchedulerBean.TRANSITION_TO_DIRECT_MARK) {
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

        LOGGER.debug("{} Async Result for sink {}, ready({}), enqueued({})", phase, sinkId, queueStatus.ready.intValue(), queueStatus.enqueued.intValue());
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
            Query q = entityManager.createQuery("select count(dt) from DependencyTrackingEntity dt where dt.status=:statusPhase and dt.sinkid=:sinkid")
                    .setParameter("sinkid", sinkId)
                    .setParameter("statusPhase", getReadyForPhase(phase));
            Long count = (Long) q.getSingleResult();
            if (count == 0) {
                LOGGER.info("prSink {} {} queue switched to {}", sinkId, phase, DIRECT);
                queueStatus.setMode(DIRECT);
            } else {
                LOGGER.info("prSink {} {} queue NOT switched to {} {} chunks found waiting", sinkId, phase, DIRECT, count);
            }
        }
    }

    private Future<Integer> doAsyncBulkScheduleCallForPhase(Long sinkId, JobSchedulerSinkStatus.QueueStatus queueStatus, ProcessingOrDelivering phase) {
        switch (phase) {
            case Processing:
                return jobSchedulerBean.bulkScheduleToProcessingForSink(sinkId, queueStatus);
            case Delivering:
                return jobSchedulerBean.bulkScheduleToDeliveringForSink(sinkId, queueStatus);
            default:
                throw new IllegalArgumentException("Unknown Phase " + phase);
        }
    }

    private DependencyTrackingEntity.ChunkSchedulingStatus getReadyForPhase(ProcessingOrDelivering phase) {
        switch (phase) {
            case Processing:
                return DependencyTrackingEntity.ChunkSchedulingStatus.READY_FOR_PROCESSING;
            case Delivering:
                return DependencyTrackingEntity.ChunkSchedulingStatus.READY_FOR_DELIVERY;
            default:
                throw new IllegalArgumentException("Unknown Phase " + phase);
        }

    }
}
