package dk.dbc.dataio.jobstore.service.ejb;

import dk.dbc.dataio.commons.types.Priority;
import dk.dbc.dataio.jobstore.distributed.TrackingKey;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

/**
 * The three keys of the dispatch order, and that they are consulted in the right sequence.
 * <p>
 * The integration tests prove the direct paths apply this. Here it is only the comparison, which is
 * where an inverted sign or a reordered key would hide.
 */
class DispatchOrderTest {
    private static final TrackingKey JOB_1_CHUNK_0 = new TrackingKey(1, 0);
    private static final TrackingKey JOB_1_CHUNK_1 = new TrackingKey(1, 1);
    private static final TrackingKey JOB_2_CHUNK_0 = new TrackingKey(2, 0);

    @Test
    void higherPriorityWins() {
        assertThat(DispatchOrder.outranks(Priority.HIGH.getValue(), JOB_2_CHUNK_0,
                Priority.NORMAL.getValue(), JOB_1_CHUNK_0), is(true));
        assertThat(DispatchOrder.outranks(Priority.NORMAL.getValue(), JOB_1_CHUNK_0,
                Priority.HIGH.getValue(), JOB_2_CHUNK_0), is(false));
    }

    /**
     * Priority is consulted first, so a higher priority beats a lower job id rather than the other
     * way round.
     */
    @Test
    void priorityBeatsJobId() {
        assertThat(DispatchOrder.outranks(Priority.LOW.getValue(), JOB_1_CHUNK_0,
                Priority.NORMAL.getValue(), JOB_2_CHUNK_0), is(false));
    }

    @Test
    void lowerJobIdWinsAtEqualPriority() {
        assertThat(DispatchOrder.outranks(Priority.NORMAL.getValue(), JOB_1_CHUNK_0,
                Priority.NORMAL.getValue(), JOB_2_CHUNK_0), is(true));
        assertThat(DispatchOrder.outranks(Priority.NORMAL.getValue(), JOB_2_CHUNK_0,
                Priority.NORMAL.getValue(), JOB_1_CHUNK_0), is(false));
    }

    @Test
    void lowerChunkIdWinsWithinAJob() {
        assertThat(DispatchOrder.outranks(Priority.NORMAL.getValue(), JOB_1_CHUNK_0,
                Priority.NORMAL.getValue(), JOB_1_CHUNK_1), is(true));
        assertThat(DispatchOrder.outranks(Priority.NORMAL.getValue(), JOB_1_CHUNK_1,
                Priority.NORMAL.getValue(), JOB_1_CHUNK_0), is(false));
    }

    /**
     * A chunk does not outrank itself, so a caller that finds its own chunk at the head of the
     * parked order stands down and leaves it where it already is.
     */
    @Test
    void aChunkDoesNotOutrankItself() {
        assertThat(DispatchOrder.outranks(Priority.NORMAL.getValue(), JOB_1_CHUNK_0,
                Priority.NORMAL.getValue(), JOB_1_CHUNK_0), is(false));
    }
}
