package dk.dbc.dataio.jobstore.service.ejb;

import dk.dbc.dataio.jobstore.distributed.TrackingKey;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.OptionalInt;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JobGateBeanTest {
    private static final int SINK_ID = 7;
    private static final int SUBMITTER = 424242;
    private static final int JOB_ID = 42;
    private static final int TERMINATION_CHUNK_ID = 3;

    private final JobGateRepository jobGateRepository = mock(JobGateRepository.class);
    private final JobGateBean jobGateBean = new JobGateBean(jobGateRepository);

    @Test
    void advanceGateState_dataChunk_isCounted() {
        TrackingKey dataChunk = new TrackingKey(JOB_ID, 0);
        when(jobGateRepository.isTerminationChunk(dataChunk)).thenReturn(false);
        when(jobGateRepository.dataChunksAccountedFor(JOB_ID)).thenReturn(false);

        jobGateBean.advanceGateState(dataChunk, SINK_ID, SUBMITTER);

        verify(jobGateRepository).incrementDataChunksDelivered(JOB_ID);
        verify(jobGateRepository, never()).markTerminationBarrierLifted(anyInt());
    }

    @Test
    void advanceGateState_terminationChunk_isNotCounted() {
        TrackingKey terminationChunk = new TrackingKey(JOB_ID, TERMINATION_CHUNK_ID);
        when(jobGateRepository.isTerminationChunk(terminationChunk)).thenReturn(true);
        when(jobGateRepository.laterClosedGates(SINK_ID, SUBMITTER, JOB_ID)).thenReturn(List.of());

        jobGateBean.advanceGateState(terminationChunk, SINK_ID, SUBMITTER);

        verify(jobGateRepository, never()).incrementDataChunksDelivered(anyInt());
        verify(jobGateRepository).markTerminationBarrierLifted(JOB_ID);
    }

    @Test
    void advanceGateState_jobWithoutTerminationChunk_countsButDoesNotEvaluate() {
        TrackingKey dataChunk = new TrackingKey(JOB_ID, 0);
        when(jobGateRepository.isTerminationChunk(dataChunk)).thenReturn(false);
        when(jobGateRepository.dataChunksAccountedFor(JOB_ID)).thenReturn(true);
        when(jobGateRepository.closedTerminationChunkId(SINK_ID, SUBMITTER, JOB_ID)).thenReturn(OptionalInt.empty());

        jobGateBean.advanceGateState(dataChunk, SINK_ID, SUBMITTER);

        verify(jobGateRepository).incrementDataChunksDelivered(JOB_ID);
        verify(jobGateRepository, never()).advisoryLock(anyInt(), anyInt());
        verify(jobGateRepository, never()).openGate(dataChunk);
    }

    @Test
    void advanceGateState_lastDataChunk_opensGate() {
        TrackingKey dataChunk = new TrackingKey(JOB_ID, TERMINATION_CHUNK_ID - 1);
        when(jobGateRepository.isTerminationChunk(dataChunk)).thenReturn(false);
        when(jobGateRepository.dataChunksAccountedFor(JOB_ID)).thenReturn(true);
        when(jobGateRepository.closedTerminationChunkId(SINK_ID, SUBMITTER, JOB_ID))
                .thenReturn(OptionalInt.of(TERMINATION_CHUNK_ID));
        when(jobGateRepository.hasEarlierUndeliveredTermination(SINK_ID, SUBMITTER, JOB_ID)).thenReturn(false);

        jobGateBean.advanceGateState(dataChunk, SINK_ID, SUBMITTER);

        verify(jobGateRepository).advisoryLock(SINK_ID, SUBMITTER);
        verify(jobGateRepository).openGate(new TrackingKey(JOB_ID, TERMINATION_CHUNK_ID));
    }

    @Test
    void advanceGateState_lastDataChunkButEarlierBarrierHolds_gateStaysClosed() {
        TrackingKey dataChunk = new TrackingKey(JOB_ID, TERMINATION_CHUNK_ID - 1);
        when(jobGateRepository.isTerminationChunk(dataChunk)).thenReturn(false);
        when(jobGateRepository.dataChunksAccountedFor(JOB_ID)).thenReturn(true);
        when(jobGateRepository.closedTerminationChunkId(SINK_ID, SUBMITTER, JOB_ID))
                .thenReturn(OptionalInt.of(TERMINATION_CHUNK_ID));
        when(jobGateRepository.hasEarlierUndeliveredTermination(SINK_ID, SUBMITTER, JOB_ID)).thenReturn(true);

        jobGateBean.advanceGateState(dataChunk, SINK_ID, SUBMITTER);

        verify(jobGateRepository).advisoryLock(SINK_ID, SUBMITTER);
        verify(jobGateRepository, never()).openGate(new TrackingKey(JOB_ID, TERMINATION_CHUNK_ID));
    }

    @Test
    void advanceGateState_terminationChunk_reTriggersLaterJobs() {
        TrackingKey terminationChunk = new TrackingKey(JOB_ID, TERMINATION_CHUNK_ID);
        TrackingKey laterJobTermination = new TrackingKey(JOB_ID + 1, 5);
        when(jobGateRepository.isTerminationChunk(terminationChunk)).thenReturn(true);
        when(jobGateRepository.laterClosedGates(SINK_ID, SUBMITTER, JOB_ID))
                .thenReturn(List.of(laterJobTermination));
        when(jobGateRepository.dataChunksAccountedFor(JOB_ID + 1)).thenReturn(true);
        when(jobGateRepository.hasEarlierUndeliveredTermination(SINK_ID, SUBMITTER, JOB_ID + 1)).thenReturn(false);

        jobGateBean.advanceGateState(terminationChunk, SINK_ID, SUBMITTER);

        verify(jobGateRepository).advisoryLock(SINK_ID, SUBMITTER);
        verify(jobGateRepository).markTerminationBarrierLifted(JOB_ID);
        verify(jobGateRepository).openGate(laterJobTermination);
    }

    @Test
    void advanceGateState_terminationChunk_laterJobWithIncompleteCounterStaysClosed() {
        TrackingKey terminationChunk = new TrackingKey(JOB_ID, TERMINATION_CHUNK_ID);
        TrackingKey laterJobTermination = new TrackingKey(JOB_ID + 1, 5);
        when(jobGateRepository.isTerminationChunk(terminationChunk)).thenReturn(true);
        when(jobGateRepository.laterClosedGates(SINK_ID, SUBMITTER, JOB_ID))
                .thenReturn(List.of(laterJobTermination));
        when(jobGateRepository.dataChunksAccountedFor(JOB_ID + 1)).thenReturn(false);

        jobGateBean.advanceGateState(terminationChunk, SINK_ID, SUBMITTER);

        verify(jobGateRepository, never()).openGate(laterJobTermination);
    }
}
