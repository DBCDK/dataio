package dk.dbc.dataio.jobstore.service.ejb;

import dk.dbc.dataio.commons.types.Chunk;
import dk.dbc.dataio.commons.types.SinkContent;
import dk.dbc.dataio.jobstore.distributed.ChunkSchedulingStatus;
import dk.dbc.dataio.jobstore.distributed.DependencyTracking;
import dk.dbc.dataio.jobstore.distributed.TrackingKey;
import dk.dbc.dataio.jobstore.service.dependencytracking.DependencyTrackingService;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * The two sink type sets that decide barrier width, see docs/chunk-scheduling-redesign.md,
 * "Barrier Width - Per-Sink-Type Job Isolation", and the once-only marker on a delivered chunk.
 */
class JobSchedulerBeanTest {
    private static final int JOB_ID = 42;
    private static final int CHUNK_ID = 0;
    private static final int SINK_ID = 7;
    private static final int SUBMITTER = 424242;

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

    /**
     * A delivery that did not remove the chunk's tracking entry does not reach the gate at all.
     * <p>
     * Every caller of chunkDeliveringDone sees the entry before removing it, so a concurrent pair
     * can both find it and both ask to remove it. Only one removal takes effect, and the count of
     * the job's delivered data chunks belongs to that one: counted twice, it reaches
     * data_chunks_expected while a data chunk is still in flight, and the gate opens early.
     */
    @Test
    void chunkDeliveringDone_removalLost_doesNotAdvanceTheGate() {
        TrackingKey key = new TrackingKey(JOB_ID, CHUNK_ID);
        DependencyTrackingService dependencyTrackingService = mock(DependencyTrackingService.class);
        JobGateBean jobGateBean = mock(JobGateBean.class);
        when(dependencyTrackingService.get(key)).thenReturn(new DependencyTracking(key, SINK_ID, SUBMITTER)
                .setStatus(ChunkSchedulingStatus.QUEUED_FOR_DELIVERY));
        when(dependencyTrackingService.remove(key)).thenReturn(null);
        when(dependencyTrackingService.removeFromWaitingOn(key)).thenReturn(Set.of());
        JobSchedulerBean jobSchedulerBean = new JobSchedulerBean(null, null, null, null,
                dependencyTrackingService, jobGateBean, null);

        jobSchedulerBean.chunkDeliveringDone(new Chunk(JOB_ID, CHUNK_ID, Chunk.Type.DELIVERED));

        verifyNoInteractions(jobGateBean);
    }
}
