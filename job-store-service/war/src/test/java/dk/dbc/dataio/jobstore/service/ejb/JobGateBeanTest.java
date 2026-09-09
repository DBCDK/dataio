package dk.dbc.dataio.jobstore.service.ejb;

import dk.dbc.dataio.jobstore.distributed.ChunkSchedulingStatus;
import dk.dbc.dataio.jobstore.distributed.DependencyTracking;
import dk.dbc.dataio.jobstore.distributed.TrackingKey;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import java.util.List;
import java.util.OptionalInt;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JobGateBeanTest {
    private static final int SINK_ID = 7;
    private static final int SUBMITTER = 424242;
    private static final int JOB_ID = 42;
    private static final int TERMINATION_CHUNK_ID = 3;
    private static final int PRIORITY = 4;

    private final JobGateRepository jobGateRepository = mock(JobGateRepository.class);
    private final DependencyTrackingRepository dependencyTrackingRepository = mock(DependencyTrackingRepository.class);
    private final JobGateBean jobGateBean = new JobGateBean(jobGateRepository, dependencyTrackingRepository);

    @Test
    void advanceGateState_dataChunk_isCounted() {
        TrackingKey dataChunk = new TrackingKey(JOB_ID, 0);
        when(jobGateRepository.dataChunksAccountedFor(JOB_ID)).thenReturn(false);

        jobGateBean.advanceGateState(dataChunkEntry(dataChunk));

        verify(jobGateRepository).incrementDataChunksDelivered(JOB_ID);
        verify(jobGateRepository, never()).markTerminationBarrierLifted(anyInt());
    }

    @Test
    void advanceGateState_terminationChunk_isNotCounted() {
        TrackingKey terminationChunk = new TrackingKey(JOB_ID, TERMINATION_CHUNK_ID);
        when(jobGateRepository.markTerminationBarrierLifted(JOB_ID)).thenReturn(1);
        when(jobGateRepository.laterClosedGates(SINK_ID, SUBMITTER, JOB_ID)).thenReturn(List.of());

        jobGateBean.advanceGateState(terminationChunkEntry(terminationChunk));

        verify(jobGateRepository, never()).incrementDataChunksDelivered(anyInt());
        verify(jobGateRepository).markTerminationBarrierLifted(JOB_ID);
    }

    @Test
    void advanceGateState_jobWithoutTerminationChunk_countsButDoesNotEvaluate() {
        TrackingKey dataChunk = new TrackingKey(JOB_ID, 0);
        when(jobGateRepository.dataChunksAccountedFor(JOB_ID)).thenReturn(true);
        when(jobGateRepository.closedTerminationChunkId(SINK_ID, SUBMITTER, JOB_ID)).thenReturn(OptionalInt.empty());

        jobGateBean.advanceGateState(dataChunkEntry(dataChunk));

        verify(jobGateRepository).incrementDataChunksDelivered(JOB_ID);
        verify(jobGateRepository, never()).advisoryLock(anyInt(), anyInt());
        verify(jobGateRepository, never()).openGate(dataChunk);
    }

    @Test
    void advanceGateState_lastDataChunk_opensGate() {
        TrackingKey dataChunk = new TrackingKey(JOB_ID, TERMINATION_CHUNK_ID - 1);
        when(jobGateRepository.dataChunksAccountedFor(JOB_ID)).thenReturn(true);
        when(jobGateRepository.closedTerminationChunkId(SINK_ID, SUBMITTER, JOB_ID))
                .thenReturn(OptionalInt.of(TERMINATION_CHUNK_ID));
        when(jobGateRepository.hasEarlierUndeliveredTermination(SINK_ID, SUBMITTER, JOB_ID)).thenReturn(false);

        jobGateBean.advanceGateState(dataChunkEntry(dataChunk));

        verify(jobGateRepository).advisoryLock(SINK_ID, SUBMITTER);
        verify(jobGateRepository).openGate(new TrackingKey(JOB_ID, TERMINATION_CHUNK_ID));
    }

    @Test
    void advanceGateState_lastDataChunkButEarlierBarrierHolds_gateStaysClosed() {
        TrackingKey dataChunk = new TrackingKey(JOB_ID, TERMINATION_CHUNK_ID - 1);
        when(jobGateRepository.dataChunksAccountedFor(JOB_ID)).thenReturn(true);
        when(jobGateRepository.closedTerminationChunkId(SINK_ID, SUBMITTER, JOB_ID))
                .thenReturn(OptionalInt.of(TERMINATION_CHUNK_ID));
        when(jobGateRepository.hasEarlierUndeliveredTermination(SINK_ID, SUBMITTER, JOB_ID)).thenReturn(true);

        jobGateBean.advanceGateState(dataChunkEntry(dataChunk));

        verify(jobGateRepository).advisoryLock(SINK_ID, SUBMITTER);
        verify(jobGateRepository, never()).openGate(new TrackingKey(JOB_ID, TERMINATION_CHUNK_ID));
    }

    @Test
    void advanceGateState_terminationChunk_reTriggersLaterJobs() {
        TrackingKey terminationChunk = new TrackingKey(JOB_ID, TERMINATION_CHUNK_ID);
        TrackingKey laterJobTermination = new TrackingKey(JOB_ID + 1, 5);
        when(jobGateRepository.markTerminationBarrierLifted(JOB_ID)).thenReturn(1);
        when(jobGateRepository.laterClosedGates(SINK_ID, SUBMITTER, JOB_ID))
                .thenReturn(List.of(laterJobTermination));
        when(jobGateRepository.dataChunksAccountedFor(JOB_ID + 1)).thenReturn(true);
        when(jobGateRepository.hasEarlierUndeliveredTermination(SINK_ID, SUBMITTER, JOB_ID + 1)).thenReturn(false);

        jobGateBean.advanceGateState(terminationChunkEntry(terminationChunk));

        verify(jobGateRepository).advisoryLock(SINK_ID, SUBMITTER);
        verify(jobGateRepository).markTerminationBarrierLifted(JOB_ID);
        verify(jobGateRepository).openGate(laterJobTermination);
    }

    @Test
    void advanceGateState_terminationChunk_laterJobWithIncompleteCounterStaysClosed() {
        TrackingKey terminationChunk = new TrackingKey(JOB_ID, TERMINATION_CHUNK_ID);
        TrackingKey laterJobTermination = new TrackingKey(JOB_ID + 1, 5);
        when(jobGateRepository.markTerminationBarrierLifted(JOB_ID)).thenReturn(1);
        when(jobGateRepository.laterClosedGates(SINK_ID, SUBMITTER, JOB_ID))
                .thenReturn(List.of(laterJobTermination));
        when(jobGateRepository.dataChunksAccountedFor(JOB_ID + 1)).thenReturn(false);

        jobGateBean.advanceGateState(terminationChunkEntry(terminationChunk));

        verify(jobGateRepository, never()).openGate(laterJobTermination);
    }

    @Test
    void liftBarrierAndRetrigger_opensDataChunksOfLaterJobs() {
        when(jobGateRepository.markTerminationBarrierLifted(JOB_ID)).thenReturn(1);
        when(jobGateRepository.laterClosedGates(SINK_ID, SUBMITTER, JOB_ID)).thenReturn(List.of());

        jobGateBean.liftBarrierAndRetrigger(JOB_ID, SINK_ID, SUBMITTER);

        verify(jobGateRepository).openLaterDataChunkGates(SINK_ID, SUBMITTER, JOB_ID);
    }

    /**
     * The guard is what lets the abort and recheck paths call this for any job at all. A job that
     * held no barrier must not have its nullable flag rewritten, and must not pay for a scan of a
     * scope it was never blocking.
     */
    @Test
    void liftBarrierAndRetrigger_jobHeldNoBarrier_doesNothingFurther() {
        when(jobGateRepository.markTerminationBarrierLifted(JOB_ID)).thenReturn(0);

        jobGateBean.liftBarrierAndRetrigger(JOB_ID, SINK_ID, SUBMITTER);

        verify(jobGateRepository, never()).advisoryLock(anyInt(), anyInt());
        verify(jobGateRepository, never()).laterClosedGates(anyInt(), anyInt(), anyInt());
        verify(jobGateRepository, never()).openLaterDataChunkGates(anyInt(), anyInt(), anyInt());
    }

    @Test
    void insertDataChunkRow_stillBlocked_insertsTheRowClosed() {
        TrackingKey dataChunk = new TrackingKey(JOB_ID, 0);
        when(jobGateRepository.hasEarlierUndeliveredTermination(SINK_ID, SUBMITTER, JOB_ID)).thenReturn(true);

        jobGateBean.insertDataChunkRow(dataChunk, SINK_ID, SUBMITTER,
                ChunkSchedulingStatus.READY_FOR_PROCESSING, PRIORITY, true);

        InOrder inOrder = inOrder(jobGateRepository, dependencyTrackingRepository);
        inOrder.verify(jobGateRepository).advisoryLock(SINK_ID, SUBMITTER);
        inOrder.verify(jobGateRepository).hasEarlierUndeliveredTermination(SINK_ID, SUBMITTER, JOB_ID);
        inOrder.verify(dependencyTrackingRepository).insert(dataChunk, SINK_ID, SUBMITTER,
                ChunkSchedulingStatus.READY_FOR_PROCESSING, PRIORITY, false);
    }

    /**
     * The re-read under the lock is the whole point of the second evaluation: the barrier the
     * unlocked pre-check saw may have been lifted since. The row is still inserted, now with its
     * gate open, because it is the chunk's only row and not a gate write of its own.
     */
    @Test
    void insertDataChunkRow_barrierLiftedSincePreCheck_insertsTheRowOpen() {
        TrackingKey dataChunk = new TrackingKey(JOB_ID, 0);
        when(jobGateRepository.hasEarlierUndeliveredTermination(SINK_ID, SUBMITTER, JOB_ID)).thenReturn(false);

        jobGateBean.insertDataChunkRow(dataChunk, SINK_ID, SUBMITTER,
                ChunkSchedulingStatus.READY_FOR_PROCESSING, PRIORITY, true);

        verify(jobGateRepository).advisoryLock(SINK_ID, SUBMITTER);
        verify(dependencyTrackingRepository).insert(dataChunk, SINK_ID, SUBMITTER,
                ChunkSchedulingStatus.READY_FOR_PROCESSING, PRIORITY, true);
    }

    /**
     * Nothing to wait for, so no lock is taken and the insert is one unconditional statement. This
     * is every chunk on a sink type outside the full barrier width, and most chunks on one inside it.
     */
    @Test
    void insertDataChunkRow_notBlocked_takesNoLock() {
        TrackingKey dataChunk = new TrackingKey(JOB_ID, 0);

        jobGateBean.insertDataChunkRow(dataChunk, SINK_ID, SUBMITTER,
                ChunkSchedulingStatus.READY_FOR_PROCESSING, PRIORITY, false);

        verify(jobGateRepository, never()).advisoryLock(anyInt(), anyInt());
        verify(jobGateRepository, never()).hasEarlierUndeliveredTermination(anyInt(), anyInt(), anyInt());
        verify(dependencyTrackingRepository).insert(dataChunk, SINK_ID, SUBMITTER,
                ChunkSchedulingStatus.READY_FOR_PROCESSING, PRIORITY, true);
    }

    @Test
    void sweepClosedGates_terminationChunkWithUndeliveredDataChunks_staysClosed() {
        TrackingKey terminationChunk = new TrackingKey(JOB_ID, TERMINATION_CHUNK_ID);
        when(jobGateRepository.closedGateScopes())
                .thenReturn(List.of(new JobGateRepository.BarrierScope(SINK_ID, SUBMITTER)));
        when(jobGateRepository.closedTerminationGates(SINK_ID, SUBMITTER)).thenReturn(List.of(terminationChunk));
        when(jobGateRepository.hasEarlierUndeliveredTermination(SINK_ID, SUBMITTER, JOB_ID)).thenReturn(false);
        when(jobGateRepository.hasUndeliveredDataChunks(JOB_ID)).thenReturn(true);

        jobGateBean.sweepClosedGates();

        verify(jobGateRepository).advisoryLock(SINK_ID, SUBMITTER);
        verify(jobGateRepository).openDataChunkGates(SINK_ID, SUBMITTER);
        verify(jobGateRepository, never()).openGate(terminationChunk);
    }

    /**
     * The failure the sweep exists for: the delivered count was rolled back and is permanently
     * short, so the gate has to open on the absence of data-chunk rows instead. A sweep that read
     * {@code data_chunks_delivered} could not repair the one case it is there for, so this test
     * fails on any implementation that does.
     */
    @Test
    void sweepClosedGates_countWasLost_opensOnRowAbsenceAnyway() {
        TrackingKey terminationChunk = new TrackingKey(JOB_ID, TERMINATION_CHUNK_ID);
        when(jobGateRepository.closedGateScopes())
                .thenReturn(List.of(new JobGateRepository.BarrierScope(SINK_ID, SUBMITTER)));
        when(jobGateRepository.closedTerminationGates(SINK_ID, SUBMITTER)).thenReturn(List.of(terminationChunk));
        when(jobGateRepository.hasEarlierUndeliveredTermination(SINK_ID, SUBMITTER, JOB_ID)).thenReturn(false);
        when(jobGateRepository.hasUndeliveredDataChunks(JOB_ID)).thenReturn(false);
        when(jobGateRepository.dataChunksAccountedFor(JOB_ID)).thenReturn(false);

        jobGateBean.sweepClosedGates();

        verify(jobGateRepository).openGate(terminationChunk);
        verify(jobGateRepository, never()).dataChunksAccountedFor(JOB_ID);
    }

    @Test
    void sweepClosedGates_earlierBarrierStillStands_staysClosed() {
        TrackingKey terminationChunk = new TrackingKey(JOB_ID, TERMINATION_CHUNK_ID);
        when(jobGateRepository.closedGateScopes())
                .thenReturn(List.of(new JobGateRepository.BarrierScope(SINK_ID, SUBMITTER)));
        when(jobGateRepository.closedTerminationGates(SINK_ID, SUBMITTER)).thenReturn(List.of(terminationChunk));
        when(jobGateRepository.hasEarlierUndeliveredTermination(SINK_ID, SUBMITTER, JOB_ID)).thenReturn(true);

        jobGateBean.sweepClosedGates();

        verify(jobGateRepository, never()).openGate(terminationChunk);
    }

    @Test
    void sweepUnliftedBarriers_liftsAndReTriggers() {
        when(jobGateRepository.jobsWithUnliftedBarrierAndNoTerminationRow())
                .thenReturn(List.of(new JobGateRepository.JobBarrier(JOB_ID, SINK_ID, SUBMITTER)));
        when(jobGateRepository.markTerminationBarrierLifted(JOB_ID)).thenReturn(1);
        when(jobGateRepository.laterClosedGates(SINK_ID, SUBMITTER, JOB_ID)).thenReturn(List.of());

        int lifted = jobGateBean.sweepUnliftedBarriers();

        assertThat(lifted, is(1));
        verify(jobGateRepository).markTerminationBarrierLifted(JOB_ID);
        verify(jobGateRepository).openLaterDataChunkGates(SINK_ID, SUBMITTER, JOB_ID);
    }

    /**
     * A termination chunk delivered again, after its barrier has already been lifted, still takes
     * the termination branch and is still not counted. What stops it doing the work twice is the
     * guarded update reporting no rows, not the branch declining to run.
     */
    @Test
    void advanceGateState_terminationChunkAgain_isStillNotCounted() {
        TrackingKey terminationChunk = new TrackingKey(JOB_ID, TERMINATION_CHUNK_ID);
        when(jobGateRepository.markTerminationBarrierLifted(JOB_ID)).thenReturn(0);

        jobGateBean.advanceGateState(terminationChunkEntry(terminationChunk));

        verify(jobGateRepository, never()).incrementDataChunksDelivered(anyInt());
        verify(jobGateRepository, never()).advisoryLock(anyInt(), anyInt());
        verify(jobGateRepository, never()).openLaterDataChunkGates(anyInt(), anyInt(), anyInt());
    }

    private DependencyTracking dataChunkEntry(TrackingKey key) {
        return new DependencyTracking(key, SINK_ID, SUBMITTER);
    }

    private DependencyTracking terminationChunkEntry(TrackingKey key) {
        return dataChunkEntry(key).setTermination(true);
    }
}
