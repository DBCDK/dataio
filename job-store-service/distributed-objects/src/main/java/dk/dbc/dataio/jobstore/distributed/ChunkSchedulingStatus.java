package dk.dbc.dataio.jobstore.distributed;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

public enum ChunkSchedulingStatus {
    READY_FOR_PROCESSING(1, s -> s.getProcessingStatus().ready),   // chunk is ready for processing
    QUEUED_FOR_PROCESSING(2, READY_FOR_PROCESSING, s -> s.getProcessingStatus().enqueued),  // chunk is sent to processor JMS queue
    BLOCKED(3, null),                // chunk is waiting for other chunk(s) to return from sink
    READY_FOR_DELIVERY(4, s -> s.getDeliveringStatus().ready),     // chunk is ready for delivery
    QUEUED_FOR_DELIVERY(5, READY_FOR_DELIVERY, s -> s.getDeliveringStatus().enqueued);     // chunk is sent to sink JMS queue

    public final Integer value;
    public final ChunkSchedulingStatus resend;
    private static final Map<Integer, ChunkSchedulingStatus> VALUE_MAP = Arrays.stream(values()).collect(Collectors.toMap(c -> c.value, c -> c));
    private final Function<JobSchedulerSinkStatus, AtomicInteger> counter;

    ChunkSchedulingStatus(Integer value, Function<JobSchedulerSinkStatus, AtomicInteger> counter) {
        this.value = value;
        this.counter = counter;
        resend = null;
    }

    ChunkSchedulingStatus(Integer value, ChunkSchedulingStatus resend, Function<JobSchedulerSinkStatus, AtomicInteger> counter) {
        this.value = value;
        this.resend = resend;
        this.counter = counter;
    }

    public void incSinkStatusCount(JobSchedulerSinkStatus status) {
        if (counter != null) counter.apply(status).incrementAndGet();
    }

    public void decSinkStatusCount(JobSchedulerSinkStatus status) {
        if (counter != null) counter.apply(status).decrementAndGet();
    }

    public static ChunkSchedulingStatus from(int value) {
        return VALUE_MAP.get(value);
    }
}
