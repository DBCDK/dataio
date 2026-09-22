package dk.dbc.dataio.jobstore.service.dependencytracking;

import dk.dbc.dataio.commons.types.Priority;
import dk.dbc.dataio.commons.types.SinkContent;
import dk.dbc.dataio.commons.utils.test.model.SinkBuilder;
import dk.dbc.dataio.commons.utils.test.model.SinkContentBuilder;
import dk.dbc.dataio.jobstore.distributed.ChunkSchedulingStatus;
import dk.dbc.dataio.jobstore.distributed.TrackingKey;
import dk.dbc.dataio.jobstore.service.AbstractJobStoreIT;
import dk.dbc.dataio.jobstore.service.entity.ChunkEntity;
import dk.dbc.dataio.jobstore.service.entity.JobEntity;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Status;
import jakarta.transaction.Synchronization;
import jakarta.transaction.TransactionSynchronizationRegistry;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static dk.dbc.dataio.jobstore.distributed.ChunkSchedulingStatus.READY_FOR_PROCESSING;
import static dk.dbc.dataio.jobstore.distributed.ChunkSchedulingStatus.SCHEDULED_FOR_PROCESSING;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;

/**
 * The sink chunk counts, and what the bulk submitters may conclude from them.
 * <p>
 * The counts are a Hazelcast map maintained from the write sites, so they sit outside the
 * transaction that causes each move and can end up saying something the table does not. Two
 * properties follow from that and are covered here: a move whose transaction does not commit is
 * taken back, and a count that is wrong all the same cannot hide a sink that has chunks waiting.
 * <p>
 * The interleaving that produces a wrong count is arranged by hand. A transaction is held open
 * across the recount rather than released and hoped for, since a recount that runs after the commit
 * reads the right number and every implementation looks correct against it.
 */
public class DependencyTrackingServiceIT extends AbstractJobStoreIT {
    private static final int SINK_ID = 4713;
    private static final long SUBMITTER = 820030;

    // ---------------------------------------------------------------- the counter and its transaction

    /**
     * A counter move is taken back when its transaction does not commit, which is what keeps the
     * counts and the table together without waiting for a recount.
     */
    @org.junit.Test
    public void counterMove_transactionRollsBack_isUndone() throws Exception {
        JobEntity job = newPersistedJob();
        TrackingKey key = seed(job, 0, READY_FOR_PROCESSING);
        CapturingRegistry registry = new CapturingRegistry();
        DependencyTrackingService service = newDependencyTrackingService()
                .withTransactionSynchronizationRegistry(registry);

        persistenceContext.run(() -> service.remove(key));
        assertThat("moved while the transaction is open",
                service.getCount(SINK_ID, READY_FOR_PROCESSING), is(0));

        registry.complete(Status.STATUS_ROLLEDBACK);

        assertThat("put back once the transaction rolled back",
                service.getCount(SINK_ID, READY_FOR_PROCESSING), is(1));
    }

    /**
     * A counter move stands once its transaction commits.
     */
    @org.junit.Test
    public void counterMove_transactionCommits_stands() throws Exception {
        JobEntity job = newPersistedJob();
        TrackingKey key = seed(job, 0, READY_FOR_PROCESSING);
        CapturingRegistry registry = new CapturingRegistry();
        DependencyTrackingService service = newDependencyTrackingService()
                .withTransactionSynchronizationRegistry(registry);

        persistenceContext.run(() -> service.remove(key));
        registry.complete(Status.STATUS_COMMITTED);

        assertThat(service.getCount(SINK_ID, READY_FOR_PROCESSING), is(0));
    }

    /**
     * An inserted chunk stays counted when the caller's transaction rolls back, because its row
     * does too.
     * <p>
     * The row is written by a {@code REQUIRES_NEW} insert that commits before the count is made, so
     * reversing the count here would take the chunk out of the counters while the table still holds
     * it. That is the direction that makes a sink look idle, which is the failure the reversal
     * exists to prevent rather than to cause.
     */
    @org.junit.Test
    public void insertedChunk_callersTransactionRollsBack_staysCounted() {
        CapturingRegistry registry = new CapturingRegistry();
        DependencyTrackingService service = newDependencyTrackingService()
                .withTransactionSynchronizationRegistry(registry);

        service.countInsertedChunk(SINK_ID, READY_FOR_PROCESSING);
        registry.complete(Status.STATUS_ROLLEDBACK);

        assertThat(service.getCount(SINK_ID, READY_FOR_PROCESSING), is(1));
    }

    /**
     * A reversal stands down once the counters have been recounted, since a census of the table
     * already accounts for whatever the transaction did, and subtracting from it would take a
     * correct number out of true.
     */
    @org.junit.Test
    public void counterMove_recountedBeforeTheRollback_isLeftAlone() throws Exception {
        JobEntity job = newPersistedJob();
        TrackingKey key = seed(job, 0, READY_FOR_PROCESSING);
        CapturingRegistry registry = new CapturingRegistry();
        DependencyTrackingService service = newDependencyTrackingService()
                .withTransactionSynchronizationRegistry(registry);
        assertThat("the seeded row is counted", service.getCount(SINK_ID, READY_FOR_PROCESSING), is(1));

        persistenceContext.run(() -> service.remove(key));
        assertThat("removing it moved the counter", service.getCount(SINK_ID, READY_FOR_PROCESSING), is(0));
        service.recountSinkStatus(Set.of());

        registry.complete(Status.STATUS_ROLLEDBACK);

        assertThat("the recount's number stands, the reversal did not add the chunk back",
                service.getCount(SINK_ID, READY_FOR_PROCESSING), is(0));
    }

    /**
     * A sink absent from the counts is counted from zero rather than skipped.
     * <p>
     * A sink is absent whenever it holds no rows, which is every sink immediately after a full
     * recount, and the entry processor behind a delta leaves a key it does not find alone. A delta
     * dropped there is a count that stays at zero while the table fills up.
     */
    @org.junit.Test
    public void counterMove_sinkAbsentFromTheCounts_isStillCounted() {
        DependencyTrackingService service = newDependencyTrackingService();
        service.recountSinkStatus(Set.of());

        service.countInsertedChunk(SINK_ID, READY_FOR_PROCESSING);

        assertThat(service.getCount(SINK_ID, READY_FOR_PROCESSING), is(1));
    }

    // ---------------------------------------------------------------- discovery against a wrong count

    /**
     * A recount that runs while another transaction has moved a chunk and not yet committed leaves
     * that sink's count saying it holds nothing parked.
     * <p>
     * The recount reads committed state, so it cannot see the move, while the move has already been
     * applied to the counts and is discarded when the recount replaces them. Nothing repairs the
     * number until the next recount, and {@code getActiveSinks} is what the once-a-second dispatch
     * sweeps ask, so the chunk waits with nothing looking at it.
     * <p>
     * Asking the table instead answers correctly, which is what bounds the wait to one sweep of
     * {@code JobSchedulerBulkSubmitterBean.sweepSinksWithParkedChunks}.
     */
    @org.junit.Test
    public void recount_duringAnUncommittedMove_losesTheCountButNotTheChunk() throws Exception {
        JobEntity job = newPersistedJob();
        TrackingKey key = seed(job, 0, READY_FOR_PROCESSING);

        DependencyTrackingService service = newDependencyTrackingService();
        service.recountSinkStatus(Set.of());
        assertThat("the chunk starts counted where it sits",
                service.getCount(SINK_ID, READY_FOR_PROCESSING), is(1));

        EntityManager movingEm = entityManager.getEntityManagerFactory().createEntityManager();
        try {
            movingEm.getTransaction().begin();
            assertThat("the chunk was moved", newDependencyTrackingService(movingEm)
                    .setValidatedStatus(key, SCHEDULED_FOR_PROCESSING).isPresent(), is(true));

            service.recountSinkStatus(Set.of());

            movingEm.getTransaction().commit();
        } finally {
            rollbackAndClose(movingEm);
        }

        assertThat("the row is parked", statusOf(key), is(SCHEDULED_FOR_PROCESSING.value));
        assertThat("the counts have lost it",
                service.getActiveSinks(SCHEDULED_FOR_PROCESSING), not(hasItem(SINK_ID)));
        assertThat("the table has not",
                service.findSinksWithChunksIn(SCHEDULED_FOR_PROCESSING), hasItem(SINK_ID));
    }

    /**
     * The recount says how many counts it had to correct, which is the only report a lost delta
     * produces.
     */
    @org.junit.Test
    public void recount_reportsWhatItCorrected() throws Exception {
        JobEntity job = newPersistedJob();
        // The service recounts as it is built, so it has to exist before the row does for the
        // counts to be behind at all.
        DependencyTrackingService service = newDependencyTrackingService();
        seed(job, 0, READY_FOR_PROCESSING);

        assertThat("the seeded row was never counted", service.recountSinkStatus(Set.of()), is(1));
        assertThat("and the counts agree the second time", service.recountSinkStatus(Set.of()), is(0));
    }

    // ---------------------------------------------------------------- fixtures

    /**
     * Stands in for the container's registry, holding what was registered so a test can complete
     * the transaction the way it means to.
     */
    private static class CapturingRegistry implements TransactionSynchronizationRegistry {
        private final List<Synchronization> synchronizations = new ArrayList<>();

        void complete(int status) {
            synchronizations.forEach(synchronization -> synchronization.afterCompletion(status));
            synchronizations.clear();
        }

        @Override
        public Object getTransactionKey() {
            return "transaction";
        }

        @Override
        public void registerInterposedSynchronization(Synchronization synchronization) {
            synchronizations.add(synchronization);
        }

        @Override
        public void putResource(Object key, Object value) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Object getResource(Object key) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setRollbackOnly() {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean getRollbackOnly() {
            throw new UnsupportedOperationException();
        }

        @Override
        public int getTransactionStatus() {
            return Status.STATUS_ACTIVE;
        }
    }

    /**
     * Writes the chunk's row straight to PostgreSQL, so the counts are left untouched and the test
     * decides what they hold. The chunk row has to exist first, since
     * {@code dependencytracking_jobid_fkey} is a foreign key on {@code (jobid, chunkid)}.
     */
    private TrackingKey seed(JobEntity job, int chunkId, ChunkSchedulingStatus status) throws SQLException {
        newPersistedChunkEntity(new ChunkEntity.Key(chunkId, job.getId()));
        try (Connection connection = newConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "insert into dependencytracking (jobid, chunkid, sinkid, submitter, status, priority, gate_open) " +
                             "values (?, ?, ?, ?, ?, ?, ?)")) {
            statement.setInt(1, job.getId());
            statement.setInt(2, chunkId);
            statement.setInt(3, SINK_ID);
            statement.setInt(4, (int) SUBMITTER);
            statement.setInt(5, status.value);
            statement.setInt(6, Priority.NORMAL.getValue());
            statement.setBoolean(7, true);
            statement.executeUpdate();
            // newConnection() hands out a connection with auto-commit off.
            connection.commit();
        }
        return new TrackingKey(job.getId(), chunkId);
    }

    private int statusOf(TrackingKey key) throws SQLException {
        try (Connection connection = newConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "select status from dependencytracking where jobid = ? and chunkid = ?")) {
            statement.setInt(1, key.getJobId());
            statement.setInt(2, key.getChunkId());
            try (var resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getInt(1);
            }
        }
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
}
