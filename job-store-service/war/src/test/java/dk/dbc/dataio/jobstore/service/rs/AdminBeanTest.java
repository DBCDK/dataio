package dk.dbc.dataio.jobstore.service.rs;

import dk.dbc.dataio.commons.types.Chunk;
import dk.dbc.dataio.commons.types.Sink;
import dk.dbc.dataio.commons.types.SinkContent;
import dk.dbc.dataio.jobstore.distributed.ChunkSchedulingStatus;
import dk.dbc.dataio.jobstore.distributed.DependencyTracking;
import dk.dbc.dataio.jobstore.distributed.DependencyTrackingRO;
import dk.dbc.dataio.jobstore.distributed.StatusChangeEvent;
import dk.dbc.dataio.jobstore.distributed.TrackingKey;
import dk.dbc.dataio.jobstore.service.dependencytracking.DependencyTrackingService;
import dk.dbc.dataio.jobstore.service.ejb.JobGateBean;
import dk.dbc.dataio.jobstore.service.ejb.JobSchedulerBean;
import dk.dbc.dataio.jobstore.service.ejb.PgJobStoreRepository;
import dk.dbc.dataio.jobstore.service.ejb.SweepMetrics;
import dk.dbc.dataio.jobstore.service.ejb.SweepMetrics.Repair;
import dk.dbc.dataio.jobstore.service.entity.ChunkEntity;
import dk.dbc.dataio.jobstore.service.entity.JobEntity;
import dk.dbc.dataio.jobstore.types.State;
import dk.dbc.dataio.jobstore.types.StateChange;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static dk.dbc.dataio.jobstore.distributed.ChunkSchedulingStatus.QUEUED_FOR_DELIVERY;
import static dk.dbc.dataio.jobstore.distributed.ChunkSchedulingStatus.QUEUED_FOR_PROCESSING;
import static dk.dbc.dataio.jobstore.distributed.ChunkSchedulingStatus.READY_FOR_DELIVERY;
import static dk.dbc.dataio.jobstore.distributed.ChunkSchedulingStatus.READY_FOR_PROCESSING;
import static dk.dbc.dataio.jobstore.distributed.ChunkSchedulingStatus.SCHEDULED_FOR_DELIVERY;
import static dk.dbc.dataio.jobstore.distributed.ChunkSchedulingStatus.SCHEDULED_FOR_PROCESSING;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class AdminBeanTest {
    private final static Map<Integer, Sink> SINKS = Map.of(
            1, new Sink(1, 1, newSinkContent("sink1", 2)),
            2, new Sink(2, 1, newSinkContent("sink2", 1)),
            3, new Sink(3, 1, newSinkContent("sink3", 4)),
            4, new Sink(4, 1, newSinkContent("sink4", 3))
    );

    @Test
    public void getSinkName() {
        DependencyTracking dte = new DependencyTracking(new TrackingKey(0, 0), 2, 0);
        String sinkName = new TestAdminBean().getSink(dte.getSinkId()).getContent().getName();
        Assertions.assertEquals("sink2", sinkName);
    }

    @Test
    public void isTimeout() {
        Assertions.assertFalse(new TestAdminBean().isTimeout(new TestDependencyTracking(Instant.now(), 2)));
        Assertions.assertTrue(new TestAdminBean().isTimeout(new TestDependencyTracking(Instant.now().minus(Duration.ofHours(2)), 2)));
        Assertions.assertFalse(new TestAdminBean().isTimeout(new TestDependencyTracking(Instant.now().minus(Duration.ofHours(2)), 3)));
    }

    @Test
    void advanceChunksWhosePhaseFinished_processingAlreadyFinished_completionIsRunAgain() {
        TestAdminBean adminBean = newAdminBeanSeeing(chunkWithPhaseDone(State.Phase.PROCESSING));
        DependencyTrackingRO stale = stale(20, 0, QUEUED_FOR_PROCESSING, 0);

        List<DependencyTrackingRO> outstanding = adminBean.advanceChunksWhosePhaseFinished(List.of(stale));

        Assertions.assertTrue(outstanding.isEmpty(), "the chunk is dealt with, not left to be resent");
        verify(adminBean.jobSchedulerBean).advanceCompletedChunk(any(Chunk.class), eq(State.Phase.PROCESSING));
    }

    @Test
    void advanceChunksWhosePhaseFinished_deliveryAlreadyFinished_completionIsRunAgain() {
        TestAdminBean adminBean = newAdminBeanSeeing(chunkWithPhaseDone(State.Phase.DELIVERING));
        DependencyTrackingRO stale = stale(21, 0, QUEUED_FOR_DELIVERY, 0);

        List<DependencyTrackingRO> outstanding = adminBean.advanceChunksWhosePhaseFinished(List.of(stale));

        Assertions.assertTrue(outstanding.isEmpty());
        verify(adminBean.jobSchedulerBean).advanceCompletedChunk(any(Chunk.class), eq(State.Phase.DELIVERING));
    }

    /**
     * The case the bounded resend exists for. Nothing has run, so there is nothing to advance.
     */
    @Test
    void advanceChunksWhosePhaseFinished_phaseStillOutstanding_isLeftToTheResend() {
        TestAdminBean adminBean = newAdminBeanSeeing(chunkWithPhaseDone(State.Phase.PARTITIONING));
        DependencyTrackingRO stale = stale(22, 0, QUEUED_FOR_PROCESSING, 0);

        List<DependencyTrackingRO> outstanding = adminBean.advanceChunksWhosePhaseFinished(List.of(stale));

        Assertions.assertEquals(List.of(stale), outstanding);
        verify(adminBean.jobSchedulerBean, never()).advanceCompletedChunk(any(Chunk.class), any());
    }

    @Test
    void advanceChunksWhosePhaseFinished_chunkRowIsGone_isLeftToTheResend() {
        TestAdminBean adminBean = newAdminBeanSeeing(null);
        DependencyTrackingRO stale = stale(23, 0, QUEUED_FOR_PROCESSING, 0);

        List<DependencyTrackingRO> outstanding = adminBean.advanceChunksWhosePhaseFinished(List.of(stale));

        Assertions.assertEquals(List.of(stale), outstanding);
        verify(adminBean.jobSchedulerBean, never()).advanceCompletedChunk(any(Chunk.class), any());
    }

    /**
     * One chunk that cannot be advanced does not take the sweep down with it.
     * <p>
     * The advance writes, and the sweep runs in one transaction, so without isolation a single
     * throw would roll back the rescue that ran before it and stop the resend that runs after. The
     * same chunk is stale again a minute later, so the sweep would never complete again.
     */
    @Test
    void advanceChunksWhosePhaseFinished_oneChunkThrows_theRestAreStillAdvanced() {
        TestAdminBean adminBean = newAdminBeanSeeing(chunkWithPhaseDone(State.Phase.PROCESSING));
        DependencyTrackingRO failing = stale(30, 0, QUEUED_FOR_PROCESSING, 0);
        DependencyTrackingRO following = stale(31, 0, QUEUED_FOR_PROCESSING, 0);
        doThrow(new IllegalStateException("gate work failed"))
                .when(adminBean.jobSchedulerBean)
                .advanceCompletedChunk(argThat(chunk -> chunk != null && chunk.getJobId() == 30), any());

        List<DependencyTrackingRO> outstanding =
                adminBean.advanceChunksWhosePhaseFinished(List.of(failing, following));

        verify(adminBean.jobSchedulerBean).advanceCompletedChunk(
                argThat(chunk -> chunk != null && chunk.getJobId() == 31), eq(State.Phase.PROCESSING));
        Assertions.assertTrue(outstanding.isEmpty(),
                "a chunk whose phase finished is not resent, whether or not the advance worked");
    }

    @Test
    void reportExhaustedRetries_chunkBelowTheLimit_isNotReported() {
        TestAdminBean adminBean = new TestAdminBean();

        int reported = adminBean.reportExhaustedRetries(List.of(stale(10, 0, 2)));

        Assertions.assertEquals(0, reported);
    }

    @Test
    void reportExhaustedRetries_chunkAtTheLimit_isReportedOnce() {
        TestAdminBean adminBean = new TestAdminBean();
        List<DependencyTrackingRO> stale = List.of(stale(11, 0, 3), stale(11, 1, 3));

        int first = adminBean.reportExhaustedRetries(stale);
        int second = adminBean.reportExhaustedRetries(stale);

        Assertions.assertEquals(2, first);
        Assertions.assertEquals(0, second, "a sweep a minute later says nothing further");
    }

    @Test
    void reportExhaustedRetries_chunkRecoversAndStrandsAgain_isReportedAgain() {
        TestAdminBean adminBean = new TestAdminBean();
        List<DependencyTrackingRO> stale = List.of(stale(12, 0, 3));

        adminBean.reportExhaustedRetries(stale);
        adminBean.reportExhaustedRetries(List.of());
        int afterRecovery = adminBean.reportExhaustedRetries(stale);

        Assertions.assertEquals(1, afterRecovery);
    }

    private static DependencyTrackingRO stale(int jobId, int chunkId, int retries) {
        return new DependencyTracking(new TrackingKey(jobId, chunkId), 2, 0).withRetries(retries);
    }

    private static DependencyTrackingRO stale(int jobId, int chunkId, ChunkSchedulingStatus status, int retries) {
        return new DependencyTracking(new TrackingKey(jobId, chunkId), 2, 0)
                .setStatus(status)
                .withRetries(retries);
    }

    /**
     * Closes every phase up to and including the one named, since {@code State} refuses a phase
     * completed out of order and a real chunk reaches one only through the ones before it.
     */
    private static ChunkEntity chunkWithPhaseDone(State.Phase phase) {
        State state = new State();
        for (State.Phase closed : State.Phase.values()) {
            state.updateState(new StateChange()
                    .setPhase(closed)
                    .setBeginDate(new Date())
                    .setEndDate(new Date())
                    .setSucceeded(1));
            if (closed == phase) {
                break;
            }
        }
        ChunkEntity chunk = new ChunkEntity();
        chunk.setState(state);
        return chunk;
    }

    private static TestAdminBean newAdminBeanSeeing(ChunkEntity chunk) {
        TestAdminBean adminBean = new TestAdminBean();
        adminBean.jobSchedulerBean = mock(JobSchedulerBean.class);
        adminBean.entityManager = mock(EntityManager.class);
        when(adminBean.entityManager.find(eq(ChunkEntity.class), any())).thenReturn(chunk);
        return adminBean;
    }

    public static SinkContent newSinkContent(String name, int timeout) {
        return new SinkContent(name, "queue", "description", SinkContent.SinkType.DUMMY, null, timeout);
    }

    private static class TestDependencyTracking extends DependencyTracking {
        private final Instant lm;

        private TestDependencyTracking(Instant lm, int sinkId) {
            super(new TrackingKey(0, 0), sinkId, 0);
            this.lm = lm;
        }

        @Override
        public Instant getLastModified() {
            return lm;
        }
    }

    /**
     * A lost delta is silent once the recount has replaced the number, so what the recount had to
     * correct is the only thing left to report it by.
     */
    @Test
    void recountAndReportDrift_countersWereWrong_theCorrectionIsCounted() {
        TestAdminBean adminBean = new TestAdminBean();
        adminBean.dependencyTrackingService = mock(DependencyTrackingService.class);
        when(adminBean.dependencyTrackingService.recountSinkStatus(any())).thenReturn(4);

        adminBean.recountAndReportDrift();

        Assertions.assertEquals(4, adminBean.correctedCounted);
    }

    /**
     * Counters that agree with the table add nothing, so the series stays at 0 between
     * corrections.
     */
    @Test
    void recountAndReportDrift_countersAgreed_nothingIsCounted() {
        TestAdminBean adminBean = new TestAdminBean();
        adminBean.dependencyTrackingService = mock(DependencyTrackingService.class);
        when(adminBean.dependencyTrackingService.recountSinkStatus(any())).thenReturn(0);

        adminBean.recountAndReportDrift();

        Assertions.assertEquals(-1, adminBean.correctedCounted, "nothing was counted");
    }

    @Test
    void advanceChunksWhosePhaseFinished_chunkAdvanced_isCountedAsARepair() {
        TestAdminBean adminBean = newAdminBeanSeeing(chunkWithPhaseDone(State.Phase.PROCESSING));

        adminBean.advanceChunksWhosePhaseFinished(List.of(stale(40, 0, QUEUED_FOR_PROCESSING, 0)));

        verify(adminBean.sweepMetrics).countRepairs(Repair.PHASE_ADVANCED, 1);
    }

    /**
     * A chunk that moved on between the query and the write was not rescued by the sweep, so only
     * the moves the validated status change made are counted.
     */
    @Test
    void rescueChunksLeftReady_countsOnlyTheChunksActuallyMoved() {
        TestAdminBean adminBean = new TestAdminBean();
        adminBean.dependencyTrackingService = mock(DependencyTrackingService.class);
        DependencyTrackingRO moved = stale(50, 0, READY_FOR_DELIVERY, 0);
        DependencyTrackingRO movedOn = stale(50, 1, READY_FOR_DELIVERY, 0);
        DependencyTrackingRO processing = stale(51, 0, READY_FOR_PROCESSING, 0);
        when(adminBean.dependencyTrackingService.getStaleDependencies(eq(READY_FOR_DELIVERY), any()))
                .thenReturn(List.of(moved, movedOn));
        when(adminBean.dependencyTrackingService.getStaleDependencies(eq(READY_FOR_PROCESSING), any()))
                .thenReturn(List.of(processing));
        when(adminBean.dependencyTrackingService.setValidatedStatus(moved.getKey(), SCHEDULED_FOR_DELIVERY))
                .thenReturn(Optional.of(mock(StatusChangeEvent.class)));
        when(adminBean.dependencyTrackingService.setValidatedStatus(movedOn.getKey(), SCHEDULED_FOR_DELIVERY))
                .thenReturn(Optional.empty());
        when(adminBean.dependencyTrackingService.setValidatedStatus(processing.getKey(), SCHEDULED_FOR_PROCESSING))
                .thenReturn(Optional.of(mock(StatusChangeEvent.class)));

        adminBean.rescueChunksLeftReady();

        verify(adminBean.sweepMetrics).countRepairs(Repair.READY_RESCUED_DELIVERY, 1);
        verify(adminBean.sweepMetrics).countRepairs(Repair.READY_RESCUED_PROCESSING, 1);
    }

    @Test
    void resendIfNeeded_countsOnlyTheChunksActuallySent() {
        TestAdminBean adminBean = new TestAdminBean();
        adminBean.dependencyTrackingService = mock(DependencyTrackingService.class);
        adminBean.jobSchedulerBean = mock(JobSchedulerBean.class);
        DependencyTrackingRO sent = stale(60, 0, QUEUED_FOR_DELIVERY, 0);
        DependencyTrackingRO refused = stale(60, 1, QUEUED_FOR_DELIVERY, 1);
        DependencyTrackingRO exhausted = stale(60, 2, QUEUED_FOR_DELIVERY, 3);
        when(adminBean.dependencyTrackingService.resend(sent.getKey(), 3))
                .thenReturn(Optional.of(mock(StatusChangeEvent.class)));
        when(adminBean.dependencyTrackingService.resend(refused.getKey(), 3))
                .thenReturn(Optional.empty());

        adminBean.resendIfNeeded(List.of(sent, refused, exhausted));

        verify(adminBean.sweepMetrics).countRepairs(Repair.RESENT, 1);
        verify(adminBean.dependencyTrackingService, never()).resend(exhausted.getKey(), 3);
    }

    @Test
    void recheck_countsDroppedRowsLiftedBarriersAndOpenedGates() {
        TestAdminBean adminBean = new TestAdminBean();
        adminBean.dependencyTrackingService = mock(DependencyTrackingService.class);
        adminBean.jobStoreRepository = mock(PgJobStoreRepository.class);
        adminBean.jobGateBean = mock(JobGateBean.class);
        when(adminBean.dependencyTrackingService.getAllJobIs()).thenReturn(Set.of(70, 71));
        when(adminBean.jobStoreRepository.getJobEntityById(70)).thenReturn(null);
        when(adminBean.jobStoreRepository.getJobEntityById(71)).thenReturn(mock(JobEntity.class));
        when(adminBean.jobGateBean.sweepUnliftedBarriers()).thenReturn(2);
        when(adminBean.jobGateBean.sweepClosedGates()).thenReturn(3);

        adminBean.recheck();

        verify(adminBean.dependencyTrackingService).removeJobId(70);
        verify(adminBean.sweepMetrics).countRepairs(Repair.ROWS_DROPPED, 1);
        verify(adminBean.sweepMetrics).countRepairs(Repair.BARRIER_LIFTED, 2);
        verify(adminBean.sweepMetrics).countRepairs(Repair.GATE_OPENED, 3);
    }

    /**
     * The on-demand sweep makes the same repairs as the hourly one, so they count the same.
     */
    @Test
    void gateSweep_countsLiftedBarriersAndOpenedGates() throws Exception {
        TestAdminBean adminBean = new TestAdminBean();
        adminBean.jobGateBean = mock(JobGateBean.class);
        when(adminBean.jobGateBean.sweepUnliftedBarriers()).thenReturn(1);
        when(adminBean.jobGateBean.sweepClosedGates()).thenReturn(0);

        adminBean.gateSweep();

        verify(adminBean.sweepMetrics).countRepairs(Repair.BARRIER_LIFTED, 1);
        verify(adminBean.sweepMetrics).countRepairs(Repair.GATE_OPENED, 0);
    }

    private static class TestAdminBean extends AdminBean {
        private int correctedCounted = -1;

        private TestAdminBean() {
            chunkResendLimit = 3;
            sweepMetrics = mock(SweepMetrics.class);
        }

        /**
         * Records the count, so the test can assert it without a metric registry.
         */
        @Override
        void countCorrectedCounters(int corrected) {
            correctedCounted = corrected;
        }

        @Override
        Sink getSink(int id) {
            return SINKS.get(id);
        }

        /**
         * Skips the registration, which needs a metric registry the container injects.
         */
        @Override
        void registerExhaustedRetriesMetric(String sinkName) {
        }
    }
}
