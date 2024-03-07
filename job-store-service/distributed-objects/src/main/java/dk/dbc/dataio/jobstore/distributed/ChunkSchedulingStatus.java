package dk.dbc.dataio.jobstore.distributed;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

public enum ChunkSchedulingStatus {
    READY_FOR_PROCESSING(1),   // chunk is ready for processing
    QUEUED_FOR_PROCESSING(2, 1000, READY_FOR_PROCESSING),  // chunk is sent to processor JMS queue
    BLOCKED(3),                // chunk is waiting for other chunk(s) to return from sink
    READY_FOR_DELIVERY(4),     // chunk is ready for delivery
    QUEUED_FOR_DELIVERY(5, 1000, READY_FOR_DELIVERY);     // chunk is sent to sink JMS queue

    public final Integer value;
    public final Integer capacity;

    public final ChunkSchedulingStatus resend;
    private static final Map<Integer, ChunkSchedulingStatus> VALUE_MAP = Arrays.stream(values()).collect(Collectors.toMap(c -> c.value, c -> c));

    ChunkSchedulingStatus(Integer value) {
        this.value = value;
        this.capacity = null;
        resend = null;
    }

    ChunkSchedulingStatus(Integer value, Integer capacity, ChunkSchedulingStatus resend) {
        this.value = value;
        this.capacity = capacity;
        this.resend = resend;
    }

    public static ChunkSchedulingStatus from(int value) {
        return VALUE_MAP.get(value);
    }
}
