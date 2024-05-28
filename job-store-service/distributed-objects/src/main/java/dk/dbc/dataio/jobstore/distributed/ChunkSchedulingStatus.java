package dk.dbc.dataio.jobstore.distributed;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public enum ChunkSchedulingStatus {
    READY_FOR_PROCESSING(1, 6, 2),   // chunk is ready for processing
    SCHEDULED_FOR_PROCESSING(6, 2),   // chunk is ready for processing
    QUEUED_FOR_PROCESSING(2, 1000, SCHEDULED_FOR_PROCESSING, 3, 4),  // chunk is sent to processor JMS queue
    BLOCKED(3, 7),                // chunk is waiting for other chunk(s) to return from sink
    READY_FOR_DELIVERY(4, 7, 5),     // chunk is ready for delivery
    SCHEDULED_FOR_DELIVERY(7, 5),     // chunk is ready for delivery
    QUEUED_FOR_DELIVERY(5, 1000, SCHEDULED_FOR_DELIVERY);     // chunk is sent to sink JMS queue


    static int transitionToDirectMark = 50;
    public final Integer value;
    Integer max;

    public final ChunkSchedulingStatus resend;
    private final int[] canChangeTo;
    private Set<ChunkSchedulingStatus> validStatusChanges;
    private static final Map<Integer, ChunkSchedulingStatus> VALUE_MAP = Arrays.stream(values()).collect(Collectors.toMap(c -> c.value, c -> c));

    ChunkSchedulingStatus(Integer value, int... canChangeTo) {
        this(value, null, null, canChangeTo);
    }

    ChunkSchedulingStatus(Integer value, Integer max, ChunkSchedulingStatus resend, int... canChangeTo) {
        this.value = value;
        this.max = max;
        this.resend = resend;
        this.canChangeTo = canChangeTo;
    }

    public static int getTransitionToDirectMark() {
        return transitionToDirectMark;
    }

    public Integer getMax() {
        return max;
    }

    public static ChunkSchedulingStatus from(int value) {
        return VALUE_MAP.get(value);
    }

    private Set<ChunkSchedulingStatus> validStatusChanges() {
        if(validStatusChanges == null) {
            validStatusChanges = IntStream.of(canChangeTo).mapToObj(ChunkSchedulingStatus::from).collect(Collectors.toSet());
        }
        return validStatusChanges;
    }

    public ChunkSchedulingStatus[] getValidStatusChanges() {
        return validStatusChanges().toArray(new ChunkSchedulingStatus[0]);
    }

    public boolean isValidStatusChange(ChunkSchedulingStatus status) {
        return validStatusChanges().contains(status);
    }

    public boolean isInvalidStatusChange(ChunkSchedulingStatus status) {
        return !validStatusChanges().contains(status);
    }
}
