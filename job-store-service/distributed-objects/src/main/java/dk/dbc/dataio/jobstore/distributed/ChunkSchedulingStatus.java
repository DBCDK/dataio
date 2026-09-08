package dk.dbc.dataio.jobstore.distributed;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Where one chunk stands in the scheduler, and which states it may move to from there.
 * <p>
 * A chunk goes through two phases, processing and delivery, and each phase has the same three
 * states. The name prefix says which of the three, and that is the distinction to read first:
 * <ul>
 *   <li><b>{@code READY_*}</b> - may be dispatched, and dispatch is attempted straight away by
 *       whoever put the chunk in this state.</li>
 *   <li><b>{@code SCHEDULED_*}</b> - should be dispatched but could not be, so the chunk waits here
 *       for the once-a-second sweep in {@code JobSchedulerBulkSubmitterBean} to try again. Not
 *       capped, so this is where backlog accumulates.</li>
 *   <li><b>{@code QUEUED_*}</b> - the JMS message has been sent and the reply is outstanding. Capped
 *       per sink, and that cap is the reason the parked state exists.</li>
 * </ul>
 * Delivery may additionally be held back by the chunk's gate, but that is a column on the chunk's
 * row rather than a status of its own: a gated chunk waits in {@link #SCHEDULED_FOR_DELIVERY} until
 * the gate opens.
 * <p>
 * The enum order is the order a chunk passes through. {@link #value}, which is what
 * {@code dependencytracking.status} stores, is not in that order, because the parked states were
 * added later and took the next free numbers. Value 3 is unused, see {@link #from}.
 */
public enum ChunkSchedulingStatus {
    /**
     * Registered by the partitioner, nothing sent yet. Every chunk starts here, see
     * {@link DependencyTracking}'s field initialiser.
     */
    READY_FOR_PROCESSING(1, 6, 2),

    /**
     * Waiting for room in the sink's processor queue, which was full when the chunk was ready, see
     * {@code JobSchedulerTransactionsBean.submitToProcessingIfPossible}.
     */
    SCHEDULED_FOR_PROCESSING(6, 2),

    /**
     * Sent to the processor, awaiting the processed chunk back. A chunk still here after
     * {@code PROCESSOR_TIMEOUT} is counted stale and sent again, once, by
     * {@code AdminBean.updateStaleChunks}.
     */
    QUEUED_FOR_PROCESSING(2, 1000, SCHEDULED_FOR_PROCESSING, 4),

    /**
     * Processed, so delivery is attempted at once. A chunk that has not moved on within five minutes
     * is parked in {@link #SCHEDULED_FOR_DELIVERY} by {@code AdminBean.updateStaleChunks}, so a
     * failed attempt cannot leave it here for good.
     */
    READY_FOR_DELIVERY(4, 7, 5),

    /**
     * Waiting to be delivered: the sink's delivery queue was full, the chunk's gate is closed, or a
     * dispatch attempt failed and left the chunk to be retried. The sweep takes chunks from here in
     * dispatch order, {@code (priority DESC, jobId ASC, chunkId ASC)}, see
     * {@code DeliveryDispatchRepository}.
     */
    SCHEDULED_FOR_DELIVERY(7, 5),

    /**
     * Sent to the sink, awaiting the delivery result. A chunk still here an hour later is counted
     * stale and sent again, once. Nothing follows it: the entry is removed from dependency tracking
     * when the sink reports the chunk delivered.
     */
    QUEUED_FOR_DELIVERY(5, 1000, SCHEDULED_FOR_DELIVERY);

    /**
     * Value of the deleted {@code BLOCKED} status, which
     * {@code V11__dependencytracking_drop_dependency_graph.sql} migrates to
     * {@link #SCHEDULED_FOR_DELIVERY}.
     */
    private static final int BLOCKED_VALUE = 3;


    static int transitionToDirectMark = 50;
    /** Stored in {@code dependencytracking.status}. */
    public final Integer value;
    /** How many chunks of one sink may hold this status at once, null where the status is uncapped. */
    Integer max;

    /** Where a chunk goes when it has held this status too long to be plausible, null where staleness has no meaning. */
    public final ChunkSchedulingStatus resend;
    /** {@link #value}s of the statuses a chunk may move to from this one, see {@link #isValidStatusChange}. */
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

    /**
     * Resolves a stored {@code dependencytracking.status} value.
     * <p>
     * Value 3 was {@code BLOCKED} and no longer has a constant. The migration rewrites every stored
     * 3, so the mapping here covers only a row written by an instance running the previous build
     * during a rolling restart, where the alternative is a null status on the scheduling path. Drop
     * it once no deployed build can write the value.
     *
     * @param value stored status value
     * @return the matching status, or null if the value is not one this enum knows
     */
    public static ChunkSchedulingStatus from(int value) {
        if (value == BLOCKED_VALUE) {
            return SCHEDULED_FOR_DELIVERY;
        }
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
