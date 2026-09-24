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

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
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
        submitForSinks(dependencyTrackingService.getActiveSinks(SCHEDULED_FOR_DELIVERY), SCHEDULED_FOR_DELIVERY);
    }


    @Schedule(second = "*/1", minute = "*", hour = "*", persistent = false)
    @Stopwatch
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void bulkScheduleChunksForProcessing() {
        if(Hazelcast.isSlave()) return;
        submitForSinks(dependencyTrackingService.getActiveSinks(SCHEDULED_FOR_PROCESSING), SCHEDULED_FOR_PROCESSING);
    }

    /**
     * Dispatches for the sinks the table says hold parked chunks, once a minute.
     * <p>
     * The dispatch sweeps above ask {@code getActiveSinks}, which reads the sink chunk counts. A
     * count that has lost a delta reports a sink as holding nothing parked, and no other timer
     * looks at a {@code SCHEDULED_*} chunk, so that sink stops dispatching entirely: its chunks
     * wait for the next hourly recount, and because the direct paths stand down for the head of a
     * sink's parked queue, every chunk partitioned afterwards parks behind them. Reading the table
     * bounds that to a minute whatever the counts hold.
     * <p>
     * Once a minute rather than on the dispatch tick because the query filters on status alone and
     * neither ordered index leads with it, see
     * {@link DependencyTrackingRepository#distinctSinkIdsWithStatus}. It costs nothing when the
     * counts are right: {@link #doBulkJmsQueueSubmit} keeps one dispatch in flight per sink and
     * phase, and a sink with no candidates returns on its capacity read.
     * <p>
     * This is a singleton with the default write lock, so the two scans hold off the dispatch
     * sweeps for as long as they take. That is the reason to keep the interval at a minute rather
     * than shorten it: the cost is paid against dispatch latency, not in the background.
     */
    @Schedule(second = "0", minute = "*", hour = "*", persistent = false)
    @Stopwatch
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void sweepSinksWithParkedChunks() {
        if (Hazelcast.isSlave()) {
            return;
        }
        for (ChunkSchedulingStatus phase : List.of(SCHEDULED_FOR_PROCESSING, SCHEDULED_FOR_DELIVERY)) {
            submitForSinks(dependencyTrackingService.findSinksWithChunksIn(phase), phase);
        }
    }

    private void submitForSinks(Set<Integer> sinkIds, ChunkSchedulingStatus phase) {
        sinkIds.forEach(sinkId -> {
            try {
                doBulkJmsQueueSubmit(sinkId, phase);
            } catch (Exception e) {
                LOGGER.error("Error in {} for sink {}", phase, sinkId, e);
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
