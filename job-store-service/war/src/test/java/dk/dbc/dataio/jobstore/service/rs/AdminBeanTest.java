package dk.dbc.dataio.jobstore.service.rs;

import dk.dbc.dataio.commons.types.Chunk;
import dk.dbc.dataio.commons.types.Sink;
import dk.dbc.dataio.commons.types.SinkContent;
import dk.dbc.dataio.jobstore.distributed.ChunkSchedulingStatus;
import dk.dbc.dataio.jobstore.distributed.DependencyTracking;
import dk.dbc.dataio.jobstore.distributed.DependencyTrackingRO;
import dk.dbc.dataio.jobstore.distributed.TrackingKey;
import dk.dbc.dataio.jobstore.service.ejb.JobSchedulerBean;
import dk.dbc.dataio.jobstore.service.entity.ChunkEntity;
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

import static dk.dbc.dataio.jobstore.distributed.ChunkSchedulingStatus.QUEUED_FOR_DELIVERY;
import static dk.dbc.dataio.jobstore.distributed.ChunkSchedulingStatus.QUEUED_FOR_PROCESSING;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
        verify(adminBean.jobSchedulerBean).chunkProcessingDone(any(Chunk.class));
        verify(adminBean.jobSchedulerBean, never()).chunkDeliveringDone(any(Chunk.class));
    }

    @Test
    void advanceChunksWhosePhaseFinished_deliveryAlreadyFinished_completionIsRunAgain() {
        TestAdminBean adminBean = newAdminBeanSeeing(chunkWithPhaseDone(State.Phase.DELIVERING));
        DependencyTrackingRO stale = stale(21, 0, QUEUED_FOR_DELIVERY, 0);

        List<DependencyTrackingRO> outstanding = adminBean.advanceChunksWhosePhaseFinished(List.of(stale));

        Assertions.assertTrue(outstanding.isEmpty());
        verify(adminBean.jobSchedulerBean).chunkDeliveringDone(any(Chunk.class));
        verify(adminBean.jobSchedulerBean, never()).chunkProcessingDone(any(Chunk.class));
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
        verify(adminBean.jobSchedulerBean, never()).chunkProcessingDone(any(Chunk.class));
    }

    @Test
    void advanceChunksWhosePhaseFinished_chunkRowIsGone_isLeftToTheResend() {
        TestAdminBean adminBean = newAdminBeanSeeing(null);
        DependencyTrackingRO stale = stale(23, 0, QUEUED_FOR_PROCESSING, 0);

        List<DependencyTrackingRO> outstanding = adminBean.advanceChunksWhosePhaseFinished(List.of(stale));

        Assertions.assertEquals(List.of(stale), outstanding);
        verify(adminBean.jobSchedulerBean, never()).chunkProcessingDone(any(Chunk.class));
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

    private static class TestAdminBean extends AdminBean {
        private TestAdminBean() {
            chunkResendLimit = 3;
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
