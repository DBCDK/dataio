package dk.dbc.dataio.jobstore.service.ejb;

import dk.dbc.dataio.commons.types.SinkContent;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * The two sink type sets that decide barrier width, see docs/chunk-scheduling-redesign.md,
 * "Barrier Width - Per-Sink-Type Job Isolation".
 */
class JobSchedulerBeanTest {

    /**
     * Holding a later job's data chunks behind a barrier is meaningless for a sink type that raises
     * no barrier, since there would be no termination chunk for them to wait on. A member of the
     * wider set that is not in the narrower one would close gates that nothing ever reopens.
     */
    @Test
    void fullWidthBarrierIsASubsetOfTerminationChunk() {
        List<SinkContent.SinkType> fullWidthWithoutABarrier = Arrays.stream(SinkContent.SinkType.values())
                .filter(JobSchedulerBean::requiresFullWidthBarrier)
                .filter(sinkType -> !JobSchedulerBean.requiresTerminationChunk(sinkType))
                .toList();

        assertThat("full width sink types that raise no barrier", fullWidthWithoutABarrier, is(List.of()));
    }

    /**
     * Tickle is the only sink type whose job-end work is dataset-wide rather than scoped to its own
     * job, so it is the only one that needs the width the waitingOn barrier has today. Adding a
     * member here holds back every data chunk of every queued job on that sink type, so it is a
     * deliberate decision rather than a default.
     */
    @Test
    void tickleIsTheOnlyFullWidthSinkType() {
        List<SinkContent.SinkType> fullWidth = Arrays.stream(SinkContent.SinkType.values())
                .filter(JobSchedulerBean::requiresFullWidthBarrier)
                .toList();

        assertThat(fullWidth, is(List.of(SinkContent.SinkType.TICKLE)));
    }

    @Test
    void terminationChunkSinkTypesAreUnchanged() {
        List<SinkContent.SinkType> withTermination = Arrays.stream(SinkContent.SinkType.values())
                .filter(JobSchedulerBean::requiresTerminationChunk)
                .toList();

        assertThat(withTermination, is(List.of(SinkContent.SinkType.MARCCONV,
                SinkContent.SinkType.PERIODIC_JOBS, SinkContent.SinkType.TICKLE)));
    }
}
