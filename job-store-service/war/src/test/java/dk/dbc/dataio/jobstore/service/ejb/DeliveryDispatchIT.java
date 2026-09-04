package dk.dbc.dataio.jobstore.service.ejb;

import dk.dbc.dataio.commons.types.Priority;
import dk.dbc.dataio.commons.types.SinkContent;
import dk.dbc.dataio.commons.utils.test.model.SinkBuilder;
import dk.dbc.dataio.commons.utils.test.model.SinkContentBuilder;
import dk.dbc.dataio.jobstore.distributed.ChunkSchedulingStatus;
import dk.dbc.dataio.jobstore.distributed.DependencyTracking;
import dk.dbc.dataio.jobstore.distributed.TrackingKey;
import dk.dbc.dataio.jobstore.service.AbstractJobStoreIT;
import dk.dbc.dataio.jobstore.service.dependencytracking.DependencyTrackingService;
import dk.dbc.dataio.jobstore.service.entity.ChunkEntity;
import dk.dbc.dataio.jobstore.service.entity.ItemEntity;
import dk.dbc.dataio.jobstore.service.entity.JobEntity;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.Set;

import static dk.dbc.dataio.jobstore.distributed.ChunkSchedulingStatus.READY_FOR_DELIVERY;
import static dk.dbc.dataio.jobstore.distributed.ChunkSchedulingStatus.SCHEDULED_FOR_DELIVERY;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Ordered delivery dispatch and the gate filter, see docs/chunk-scheduling-redesign.md,
 * "Delivery Ordering".
 * <p>
 * The candidate query is exercised against the table directly. The dispatch tests go through the
 * bulk sweep and the direct path, and assert on what reaches the sink, since the gate decides
 * dispatch and nothing else about a chunk.
 */
public class DeliveryDispatchIT extends AbstractJobStoreIT {
    private static final int SINK_ID = 4712;
    private static final long SUBMITTER = 820020;

    // ---------------------------------------------------------------- the candidate query

    /**
     * The whole point of the story: highest priority first, then lowest job, then lowest chunk.
     * <p>
     * The rows are seeded so that insertion order, primary key order and the expected order are all
     * different, or the assertion would pass on an unordered query.
     */
    @org.junit.Test
    public void candidatesComeBackInDispatchOrder() throws Exception {
        JobEntity low = newPersistedJob();
        JobEntity high = newPersistedJob();

        seedCandidate(low, 1, Priority.NORMAL, SCHEDULED_FOR_DELIVERY, true);
        seedCandidate(high, 1, Priority.HIGH, SCHEDULED_FOR_DELIVERY, true);
        seedCandidate(low, 0, Priority.NORMAL, SCHEDULED_FOR_DELIVERY, true);
        seedCandidate(high, 0, Priority.HIGH, SCHEDULED_FOR_DELIVERY, true);

        assertThat(candidates(10), contains(
                new TrackingKey(high.getId(), 0),
                new TrackingKey(high.getId(), 1),
                new TrackingKey(low.getId(), 0),
                new TrackingKey(low.getId(), 1)));
    }

    /**
     * A closed gate is not a candidate, even sorting first on every other key.
     */
    @org.junit.Test
    public void closedGateIsNotACandidate() throws Exception {
        JobEntity job = newPersistedJob();
        seedCandidate(job, 0, Priority.HIGH, SCHEDULED_FOR_DELIVERY, false, true);
        seedCandidate(job, 1, Priority.NORMAL, SCHEDULED_FOR_DELIVERY, true);

        assertThat(candidates(10), contains(new TrackingKey(job.getId(), 1)));
    }

    /**
     * The limit takes the first rows of the order, not an arbitrary subset of the matching set.
     */
    @org.junit.Test
    public void limitTakesTheHeadOfTheOrder() throws Exception {
        JobEntity job = newPersistedJob();
        seedCandidate(job, 0, Priority.NORMAL, SCHEDULED_FOR_DELIVERY, true);
        seedCandidate(job, 1, Priority.NORMAL, SCHEDULED_FOR_DELIVERY, true);
        seedCandidate(job, 2, Priority.HIGH, SCHEDULED_FOR_DELIVERY, true);

        assertThat(candidates(2), contains(
                new TrackingKey(job.getId(), 2),
                new TrackingKey(job.getId(), 0)));
    }

    /**
     * Only chunks in the requested status are candidates.
     */
    @org.junit.Test
    public void otherStatusesAreNotCandidates() throws Exception {
        JobEntity job = newPersistedJob();
        seedCandidate(job, 0, Priority.NORMAL, READY_FOR_DELIVERY, true);
        seedCandidate(job, 1, Priority.NORMAL, SCHEDULED_FOR_DELIVERY, true);

        assertThat(candidates(10), contains(new TrackingKey(job.getId(), 1)));
    }

    // ---------------------------------------------------------------- the bulk sweep

    /**
     * End to end for the gate: closed, the bulk sweep leaves the chunk alone; opened, the next sweep
     * dispatches it.
     */
    @org.junit.Test
    public void openingAGateReleasesTheChunkToTheNextSweep() throws Exception {
        JobEntity job = newPersistedJob();
        DependencyTrackingService trackingService = new DependencyTrackingService().init();

        TrackingKey gated = new TrackingKey(job.getId(), 0);
        trackingService.add(tracker(gated, SCHEDULED_FOR_DELIVERY));
        seedCandidate(job, 0, Priority.NORMAL, SCHEDULED_FOR_DELIVERY, false, true);

        JobSchedulerTransactionsBean transactions = dispatchingTransactionsBean();
        bulkSchedule(trackingService, transactions);
        verify(transactions, never()).submitToDeliveringNewTransaction(any());

        persistenceContext.run(() -> newJobGateRepository().openGate(gated));

        bulkSchedule(trackingService, transactions);
        verify(transactions, times(1)).submitToDeliveringNewTransaction(gated);
    }

    // ---------------------------------------------------------------- the direct path

    /**
     * A closed gate parks the chunk in SCHEDULED_FOR_DELIVERY rather than sending it, and rather
     * than leaving it in READY_FOR_DELIVERY where only the five minute stale sweep would find it.
     */
    @org.junit.Test
    public void closedGateParksTheChunkOnTheDirectPath() throws Exception {
        JobEntity job = newPersistedJob();
        DependencyTrackingService trackingService = new DependencyTrackingService().init();

        TrackingKey gated = new TrackingKey(job.getId(), 0);
        trackingService.add(tracker(gated, READY_FOR_DELIVERY));
        seedCandidate(job, 0, Priority.NORMAL, READY_FOR_DELIVERY, false, true);

        SinkMessageProducerBean producer = mock(SinkMessageProducerBean.class);
        JobSchedulerTransactionsBean bean = directPathBean(trackingService, producer, job);

        JobsBeanTest.notAborted(job.getId(), jb -> bean.submitToDeliveringIfPossible(gated));

        verify(producer, never()).send(any(), any(), anyInt());
        assertThat("chunk parked for the bulk sweep",
                trackingService.get(gated).getStatus(), is(SCHEDULED_FOR_DELIVERY));
    }

    /**
     * No {@code dependencytracking} row at all means an open gate, and the chunk is dispatched.
     * <p>
     * A gate is only ever closed by a writer that inserts the row itself, so nothing having written
     * a row means nothing has closed the gate. Reading it the other way would withhold a chunk on
     * the strength of a write that never happened.
     */
    @org.junit.Test
    public void missingRowIsAnOpenGateOnTheDirectPath() throws Exception {
        JobEntity job = newPersistedJob();
        DependencyTrackingService trackingService = new DependencyTrackingService().init();

        TrackingKey ungated = new TrackingKey(job.getId(), 0);
        trackingService.add(tracker(ungated, READY_FOR_DELIVERY));
        newPersistedChunkEntity(new ChunkEntity.Key(0, job.getId()));

        SinkMessageProducerBean producer = mock(SinkMessageProducerBean.class);
        JobSchedulerTransactionsBean bean = directPathBean(trackingService, producer, job);

        JobsBeanTest.notAborted(job.getId(), jb -> bean.submitToDeliveringIfPossible(ungated));

        verify(producer, times(1)).send(any(), any(), anyInt());
    }

    // ---------------------------------------------------------------- fixtures

    private List<TrackingKey> candidates(int limit) {
        return newDeliveryDispatchRepository().findDeliveryCandidates(SINK_ID, limit);
    }

    private void bulkSchedule(DependencyTrackingService trackingService, JobSchedulerTransactionsBean transactions) {
        JobSchedulerBean bean = new JobSchedulerBean(entityManager, transactions, null, null,
                trackingService, newJobGateBean(), newDeliveryDispatchRepository());
        JobsBeanTest.notAborted(0, jb -> bean.bulkScheduleToDeliveringForSink(SINK_ID));
    }

    /**
     * Reports every dispatch as successful, so that a candidate reaching it is not then dropped from
     * dependency tracking by the caller's no-items branch.
     */
    private JobSchedulerTransactionsBean dispatchingTransactionsBean() {
        JobSchedulerTransactionsBean transactions = mock(JobSchedulerTransactionsBean.class);
        when(transactions.submitToDeliveringNewTransaction(any())).thenReturn(true);
        return transactions;
    }

    private JobSchedulerTransactionsBean directPathBean(DependencyTrackingService trackingService,
                                                        SinkMessageProducerBean producer, JobEntity job) {
        PgJobStoreRepository jobStoreRepository = mock(PgJobStoreRepository.class);
        when(jobStoreRepository.getChunkItemEntities(anyInt(), anyInt())).thenReturn(List.of(new ItemEntity()));
        when(jobStoreRepository.getJobEntityById(anyInt())).thenReturn(job);
        return new JobSchedulerTransactionsBean(entityManager, jobStoreRepository, producer,
                mock(JobProcessorMessageProducerBean.class), trackingService, newDeliveryDispatchRepository());
    }

    private DependencyTracking tracker(TrackingKey key, ChunkSchedulingStatus status) {
        return new DependencyTracking(key, SINK_ID, (int) SUBMITTER, null, Set.of())
                .setPriority(Priority.NORMAL.getValue())
                .setStatus(status);
    }

    private JobEntity newPersistedJob() {
        JobEntity jobEntity = newJobEntity(SUBMITTER);
        jobEntity.setPriority(Priority.NORMAL);
        jobEntity.setCachedSink(newPersistedSinkCacheEntity(new SinkBuilder()
                .setId(SINK_ID)
                .setContent(new SinkContentBuilder().setSinkType(SinkContent.SinkType.DUMMY).build())
                .build()));
        persist(jobEntity);
        return jobEntity;
    }

    /**
     * Writes the chunk's {@code dependencytracking} row straight to PostgreSQL, gate column included.
     * <p>
     * The chunk row has to exist first: {@code dependencytracking_jobid_fkey} is a foreign key on
     * {@code (jobid, chunkid)}.
     */
    private void seedCandidate(JobEntity job, int chunkId, Priority priority,
                               ChunkSchedulingStatus status, boolean gateOpen) throws SQLException {
        seedCandidate(job, chunkId, priority, status, gateOpen, false);
    }

    /**
     * @param isTermination whether the row is the job's termination chunk. Under the barrier width
     *                      in force today only a termination chunk is ever inserted with a closed
     *                      gate, and {@code JobGateRepository.openGate} only matches such a row, so a
     *                      fixture with {@code gateOpen} false and this false would be a state the
     *                      scheduler cannot produce and cannot recover from.
     */
    private void seedCandidate(JobEntity job, int chunkId, Priority priority, ChunkSchedulingStatus status,
                               boolean gateOpen, boolean isTermination) throws SQLException {
        newPersistedChunkEntity(new ChunkEntity.Key(chunkId, job.getId()));
        try (Connection connection = newConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "insert into dependencytracking (jobid, chunkid, sinkid, submitter, status, priority, gate_open, is_termination) " +
                             "values (?, ?, ?, ?, ?, ?, ?, ?) " +
                             "on conflict on constraint dependencytracking_pkey do update " +
                             "  set status = excluded.status, priority = excluded.priority, " +
                             "      gate_open = excluded.gate_open, is_termination = excluded.is_termination")) {
            statement.setInt(1, job.getId());
            statement.setInt(2, chunkId);
            statement.setInt(3, SINK_ID);
            statement.setInt(4, (int) SUBMITTER);
            statement.setInt(5, status.value);
            statement.setInt(6, priority.getValue());
            statement.setBoolean(7, gateOpen);
            statement.setBoolean(8, isTermination);
            statement.executeUpdate();
            // newConnection() hands out a connection with auto-commit off.
            connection.commit();
        }
    }
}
