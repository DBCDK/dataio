package dk.dbc.dataio.jobstore.service.ejb;

import dk.dbc.dataio.commons.types.interceptor.Stopwatch;
import dk.dbc.dataio.jobstore.distributed.ChunkSchedulingStatus;
import dk.dbc.dataio.jobstore.service.dependencytracking.DependencyTrackingService;
import dk.dbc.dataio.jobstore.service.dependencytracking.Hazelcast;
import jakarta.ejb.EJB;
import jakarta.ejb.Schedule;
import jakarta.ejb.Singleton;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;

import static dk.dbc.dataio.jobstore.distributed.ChunkSchedulingStatus.SCHEDULED_FOR_DELIVERY;
import static dk.dbc.dataio.jobstore.distributed.ChunkSchedulingStatus.SCHEDULED_FOR_PROCESSING;

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
    private final Map<BulkSchedulerKey, Future<Integer>> bulkFutures = new ConcurrentHashMap<>();

    @EJB
    JobSchedulerBean jobSchedulerBean;

    @Schedule(second = "*/1", minute = "*", hour = "*", persistent = false)
    @Stopwatch
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void bulkScheduleChunksForDelivering() {
        if(Hazelcast.isSlave()) return;
        dependencyTrackingService.getActiveSinks(SCHEDULED_FOR_DELIVERY).forEach(sinkId-> {
            try {
                doBulkJmsQueueSubmit(sinkId, SCHEDULED_FOR_DELIVERY);
            } catch (Exception e) {
                LOGGER.error("Error in sink for sink {}", sinkId, e);
            }
        });
    }


    @Schedule(second = "*/1", minute = "*", hour = "*", persistent = false)
    @Stopwatch
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void bulkScheduleChunksForProcessing() {
        if(Hazelcast.isSlave()) return;
        dependencyTrackingService.getActiveSinks(SCHEDULED_FOR_PROCESSING).forEach(sinkId-> {
            try {
                doBulkJmsQueueSubmit(sinkId, SCHEDULED_FOR_PROCESSING);
            } catch (Exception e) {
                LOGGER.error("Error in Processing for sink {}", sinkId, e);
            }
        });
    }

    private void doBulkJmsQueueSubmit(Integer sinkId, ChunkSchedulingStatus phase) {
        BulkSchedulerKey key = new BulkSchedulerKey(sinkId, phase);
        Future<Integer> future = bulkFutures.get(key);
        if (future == null || future.isDone()) {
            bulkFutures.put(key, doAsyncBulkScheduleCallForPhase(sinkId, phase));
        }
    }

    private Future<Integer> doAsyncBulkScheduleCallForPhase(Integer sinkId, ChunkSchedulingStatus phase) {
        switch (phase) {
            case SCHEDULED_FOR_PROCESSING:
                return jobSchedulerBean.bulkScheduleToProcessingForSink(sinkId);
            case SCHEDULED_FOR_DELIVERY:
                return jobSchedulerBean.bulkScheduleToDeliveringForSink(sinkId);
            default:
                throw new IllegalArgumentException("Unknown Phase " + phase);
        }
    }

    private static class BulkSchedulerKey {
        public final Integer sinkId;
        public final ChunkSchedulingStatus status;

        public BulkSchedulerKey(Integer sinkId, ChunkSchedulingStatus status) {
            this.sinkId = sinkId;
            this.status = status;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            BulkSchedulerKey that = (BulkSchedulerKey) o;
            return Objects.equals(sinkId, that.sinkId) && status == that.status;
        }

        @Override
        public int hashCode() {
            return Objects.hash(sinkId, status);
        }
    }
}
