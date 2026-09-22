package dk.dbc.dataio.jobstore.service.ejb;

import dk.dbc.dataio.commons.types.Priority;
import dk.dbc.dataio.commons.types.SinkContent;
import dk.dbc.dataio.commons.utils.test.model.SinkBuilder;
import dk.dbc.dataio.commons.utils.test.model.SinkContentBuilder;
import dk.dbc.dataio.jobstore.distributed.ChunkSchedulingStatus;
import dk.dbc.dataio.jobstore.distributed.DependencyTracking;
import dk.dbc.dataio.jobstore.distributed.StatusChangeEvent;
import dk.dbc.dataio.jobstore.distributed.TrackingKey;
import dk.dbc.dataio.jobstore.service.AbstractJobStoreIT;
import dk.dbc.dataio.jobstore.service.entity.ChunkEntity;
import dk.dbc.dataio.jobstore.service.entity.JobEntity;
import jakarta.persistence.EntityManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static dk.dbc.dataio.jobstore.distributed.ChunkSchedulingStatus.QUEUED_FOR_DELIVERY;
import static dk.dbc.dataio.jobstore.distributed.ChunkSchedulingStatus.QUEUED_FOR_PROCESSING;
import static dk.dbc.dataio.jobstore.distributed.ChunkSchedulingStatus.READY_FOR_DELIVERY;
import static dk.dbc.dataio.jobstore.distributed.ChunkSchedulingStatus.READY_FOR_PROCESSING;
import static dk.dbc.dataio.jobstore.distributed.ChunkSchedulingStatus.SCHEDULED_FOR_DELIVERY;
import static dk.dbc.dataio.jobstore.distributed.ChunkSchedulingStatus.SCHEDULED_FOR_PROCESSING;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;

/**
 * The statements that make {@code dependencytracking} the sole store of chunk scheduling state.
 * <p>
 * Three of them carry a concurrency property, and each is exercised here with the interleaving
 * arranged by hand rather than hoped for: two threads released together often run one call to
 * completion before the other starts, and against a completed call every implementation looks
 * correct. The first call is driven directly and its transaction held open, and the second is
 * asserted to be queued behind it on a row lock before the first commits.
 */
public class DependencyTrackingRepositoryIT extends AbstractJobStoreIT {
    private static final int SINK_ID = 4713;
    private static final long SUBMITTER = 820030;

    // ---------------------------------------------------------------- the conditional status change

    /**
     * The predicate makes the status change a decision rather than an overwrite: only a chunk in a
     * status it may legally move from is moved.
     */
    @org.junit.Test
    public void validatedStatusChange_wrongPredecessor_changesNothing() throws Exception {
        JobEntity job = newPersistedJob();
        TrackingKey key = seed(job, 0, READY_FOR_PROCESSING, Priority.NORMAL);

        Optional<StatusChangeEvent> change = persistenceContext.run(() ->
                newDependencyTrackingRepository().updateStatusValidated(key, READY_FOR_DELIVERY));

        assertThat("declined", change.isPresent(), is(false));
        assertThat("status untouched", statusOf(key), is(READY_FOR_PROCESSING.value));
    }

    /**
     * The prior status comes back on the event, which is what the sink counters are corrected from.
     */
    @org.junit.Test
    public void validatedStatusChange_legalPredecessor_reportsBothStatuses() throws Exception {
        JobEntity job = newPersistedJob();
        TrackingKey key = seed(job, 0, QUEUED_FOR_PROCESSING, Priority.NORMAL);

        StatusChangeEvent change = persistenceContext.run(() ->
                newDependencyTrackingRepository().updateStatusValidated(key, READY_FOR_DELIVERY)).orElseThrow();

        assertThat("sink", change.getSinkId(), is(SINK_ID));
        assertThat("old status", change.getOldStatus(), is(QUEUED_FOR_PROCESSING));
        assertThat("new status", change.getNewStatus(), is(READY_FOR_DELIVERY));
        assertThat("row", statusOf(key), is(READY_FOR_DELIVERY.value));
    }

    /**
     * Two callers racing to advance one chunk, which is what {@code chunkProcessingDone} and
     * {@code submitToDelivering} do from separate instances.
     * <p>
     * A read followed by a write would let both through: both read {@code QUEUED_FOR_PROCESSING},
     * both decide the change is legal, and both write. One statement instead re-evaluates its
     * qualification against the row the first caller committed, finds the status out of the
     * predecessor set, and affects nothing.
     */
    @org.junit.Test
    public void validatedStatusChange_concurrent_exactlyOneSucceeds() throws Exception {
        JobEntity job = newPersistedJob();
        TrackingKey key = seed(job, 0, QUEUED_FOR_PROCESSING, Priority.NORMAL);

        EntityManager firstEm = entityManager.getEntityManagerFactory().createEntityManager();
        EntityManager secondEm = entityManager.getEntityManagerFactory().createEntityManager();
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Optional<StatusChangeEvent> secondChange;
        try {
            firstEm.getTransaction().begin();
            Optional<StatusChangeEvent> firstChange = newDependencyTrackingRepository(firstEm)
                    .updateStatusValidated(key, READY_FOR_DELIVERY);
            assertThat("the first caller made the change", firstChange.isPresent(), is(true));

            Future<Optional<StatusChangeEvent>> second = executor.submit(() ->
                    runInTransaction(secondEm, () -> newDependencyTrackingRepository(secondEm)
                            .updateStatusValidated(key, READY_FOR_DELIVERY)));

            awaitRowLockWaiters(1);
            assertThat("the second caller is queued behind the first", second.isDone(), is(false));

            firstEm.getTransaction().commit();
            secondChange = second.get(30, TimeUnit.SECONDS);
        } finally {
            executor.shutdownNow();
            executor.awaitTermination(30, TimeUnit.SECONDS);
            rollbackAndClose(firstEm);
            rollbackAndClose(secondEm);
        }

        assertThat("the second caller was declined", secondChange.isPresent(), is(false));
        assertThat("one change took effect", statusOf(key), is(READY_FOR_DELIVERY.value));
    }

    // ---------------------------------------------------------------- the delivery acknowledgement

    /**
     * The delete is the once-only token: whatever number of callers acknowledge one chunk, the row
     * matches for exactly one of them.
     */
    @org.junit.Test
    public void delete_concurrent_exactlyOneCallerGetsTheRow() throws Exception {
        JobEntity job = newPersistedJob();
        TrackingKey key = seed(job, 0, QUEUED_FOR_DELIVERY, Priority.NORMAL);

        EntityManager firstEm = entityManager.getEntityManagerFactory().createEntityManager();
        EntityManager secondEm = entityManager.getEntityManagerFactory().createEntityManager();
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Optional<DependencyTracking> secondRemoval;
        try {
            firstEm.getTransaction().begin();
            Optional<DependencyTracking> firstRemoval = newDependencyTrackingRepository(firstEm)
                    .delete(key, QUEUED_FOR_DELIVERY);
            assertThat("the first caller removed the row", firstRemoval.isPresent(), is(true));

            Future<Optional<DependencyTracking>> second = executor.submit(() ->
                    runInTransaction(secondEm, () -> newDependencyTrackingRepository(secondEm)
                            .delete(key, QUEUED_FOR_DELIVERY)));

            awaitRowLockWaiters(1);
            assertThat("the second caller is queued behind the first", second.isDone(), is(false));

            firstEm.getTransaction().commit();
            secondRemoval = second.get(30, TimeUnit.SECONDS);
        } finally {
            executor.shutdownNow();
            executor.awaitTermination(30, TimeUnit.SECONDS);
            rollbackAndClose(firstEm);
            rollbackAndClose(secondEm);
        }

        assertThat("the second caller got nothing", secondRemoval.isPresent(), is(false));
    }

    /**
     * An acknowledgement of a chunk that is not out for delivery removes nothing, so nothing counts
     * it against its job's gate.
     */
    @org.junit.Test
    public void delete_wrongStatus_removesNothing() throws Exception {
        JobEntity job = newPersistedJob();
        TrackingKey key = seed(job, 0, SCHEDULED_FOR_DELIVERY, Priority.NORMAL);

        Optional<DependencyTracking> removed = persistenceContext.run(() ->
                newDependencyTrackingRepository().delete(key, QUEUED_FOR_DELIVERY));

        assertThat("nothing removed", removed.isPresent(), is(false));
        assertThat("row still there", statusOf(key), is(SCHEDULED_FOR_DELIVERY.value));
    }

    // ---------------------------------------------------------------- the processing candidate query

    /**
     * The priority inversion the ordered query fixes. A HIGH chunk sitting behind more NORMAL chunks
     * than the queue cap comes back first, where the old paging predicate truncated to an arbitrary
     * page and left it waiting for a queue slot.
     */
    @org.junit.Test
    public void processingCandidates_highPriorityBehindABacklog_comesBackFirst() throws Exception {
        JobEntity job = newPersistedJob();
        for (int chunkId = 0; chunkId < 20; chunkId++) {
            seed(job, chunkId, SCHEDULED_FOR_PROCESSING, Priority.NORMAL);
        }
        TrackingKey urgent = seed(job, 20, SCHEDULED_FOR_PROCESSING, Priority.HIGH);

        List<TrackingKey> candidates = candidateKeys(5);

        assertThat("the HIGH chunk is dispatched first", candidates.getFirst(), is(urgent));
        assertThat("and the window is the requested size", candidates.size(), is(5));
    }

    /**
     * Within one priority the order is job then chunk, so the oldest work goes first.
     */
    @org.junit.Test
    public void processingCandidates_samePriority_orderedByJobThenChunk() throws Exception {
        JobEntity earlier = newPersistedJob();
        JobEntity later = newPersistedJob();
        seed(later, 1, SCHEDULED_FOR_PROCESSING, Priority.NORMAL);
        seed(earlier, 1, SCHEDULED_FOR_PROCESSING, Priority.NORMAL);
        seed(later, 0, SCHEDULED_FOR_PROCESSING, Priority.NORMAL);
        seed(earlier, 0, SCHEDULED_FOR_PROCESSING, Priority.NORMAL);

        assertThat(candidateKeys(10), contains(
                new TrackingKey(earlier.getId(), 0),
                new TrackingKey(earlier.getId(), 1),
                new TrackingKey(later.getId(), 0),
                new TrackingKey(later.getId(), 1)));
    }

    /**
     * <b>The gate takes no part in the processing query.</b> Under the full barrier width a queued
     * job's data chunks sit at {@code gate_open = FALSE} while still needing to be processed, so a
     * gate predicate here would stop the processing of exactly the chunks the barrier assumes get
     * processed.
     */
    @org.junit.Test
    public void processingCandidates_closedGate_isStillACandidate() throws Exception {
        JobEntity job = newPersistedJob();
        TrackingKey gated = seed(job, 0, SCHEDULED_FOR_PROCESSING, Priority.NORMAL, false);

        assertThat(candidateKeys(10), contains(gated));
    }

    /**
     * Only chunks awaiting processing are candidates.
     */
    @org.junit.Test
    public void processingCandidates_otherStatuses_areNotCandidates() throws Exception {
        JobEntity job = newPersistedJob();
        seed(job, 0, READY_FOR_PROCESSING, Priority.NORMAL);
        TrackingKey awaiting = seed(job, 1, SCHEDULED_FOR_PROCESSING, Priority.NORMAL);
        seed(job, 2, QUEUED_FOR_PROCESSING, Priority.NORMAL);

        assertThat(candidateKeys(10), contains(awaiting));
    }

    // ---------------------------------------------------------------- resend

    /**
     * The successor is per status rather than constant, and the retry increment is coupled to it.
     */
    @org.junit.Test
    public void resend_movesToTheStatusSuccessorAndCountsTheRetry() throws Exception {
        JobEntity job = newPersistedJob();
        TrackingKey processing = seed(job, 0, QUEUED_FOR_PROCESSING, Priority.NORMAL);
        TrackingKey delivering = seed(job, 1, QUEUED_FOR_DELIVERY, Priority.NORMAL);

        persistenceContext.run(() -> {
            newDependencyTrackingRepository().resend(processing, 1);
            newDependencyTrackingRepository().resend(delivering, 1);
        });

        assertThat("processing goes back to the processing queue",
                statusOf(processing), is(SCHEDULED_FOR_PROCESSING.value));
        assertThat("delivery goes back to the delivery queue",
                statusOf(delivering), is(SCHEDULED_FOR_DELIVERY.value));
        assertThat("retry counted", retriesOf(processing), is(1));
    }

    /**
     * The statement carries the limit, so two callers reaching one chunk produce one retry between
     * them rather than relying on a filter outside it.
     */
    @org.junit.Test
    public void resend_limitOfOne_secondCallIsANoOp() throws Exception {
        JobEntity job = newPersistedJob();
        TrackingKey key = seed(job, 0, QUEUED_FOR_PROCESSING, Priority.NORMAL);

        Optional<StatusChangeEvent> first = persistenceContext.run(() ->
                newDependencyTrackingRepository().resend(key, 1));
        Optional<StatusChangeEvent> second = persistenceContext.run(() ->
                newDependencyTrackingRepository().resend(key, 1));

        assertThat("the first call retried", first.isPresent(), is(true));
        assertThat("the second did not", second.isPresent(), is(false));
        assertThat("one retry", retriesOf(key), is(1));
    }

    /**
     * A chunk stranded again after a retry is sent again, up to the limit, so recovery is not spent
     * on the first attempt. Each retry parks the chunk, and the sweep that re-dispatches it is what
     * puts it back in a status this statement will act on, which is why the test does the same.
     */
    @org.junit.Test
    public void resend_belowTheLimit_sendsAgain() throws Exception {
        JobEntity job = newPersistedJob();
        TrackingKey key = seed(job, 0, QUEUED_FOR_PROCESSING, Priority.NORMAL);

        for (int attempt = 1; attempt <= 3; attempt++) {
            Optional<StatusChangeEvent> change = persistenceContext.run(() ->
                    newDependencyTrackingRepository().resend(key, 3));
            assertThat("retry " + attempt + " was sent", change.isPresent(), is(true));
            persistenceContext.run(() ->
                    newDependencyTrackingRepository().updateStatus(key, QUEUED_FOR_PROCESSING));
        }

        Optional<StatusChangeEvent> beyondLimit = persistenceContext.run(() ->
                newDependencyTrackingRepository().resend(key, 3));

        assertThat("the fourth attempt was declined", beyondLimit.isPresent(), is(false));
        assertThat("three retries", retriesOf(key), is(3));
        assertThat("left where the sweep can reach it", statusOf(key), is(QUEUED_FOR_PROCESSING.value));
    }

    /**
     * A status with no successor is a no-op the affected-row count reports as such, rather than a
     * chunk moved to null.
     */
    @org.junit.Test
    public void resend_statusWithNoSuccessor_isANoOp() throws Exception {
        JobEntity job = newPersistedJob();
        TrackingKey key = seed(job, 0, READY_FOR_PROCESSING, Priority.NORMAL);

        Optional<StatusChangeEvent> change = persistenceContext.run(() ->
                newDependencyTrackingRepository().resend(key, 1));

        assertThat("declined", change.isPresent(), is(false));
        assertThat("status untouched", statusOf(key), is(READY_FOR_PROCESSING.value));
        assertThat("no retry counted", retriesOf(key), is(0));
    }

    // ---------------------------------------------------------------- row lifecycle

    /**
     * The insert is idempotent, which is what makes the existence check in front of re-entry into
     * scheduling advisory rather than necessary.
     */
    @org.junit.Test
    public void insert_secondTime_leavesTheRowAlone() throws Exception {
        JobEntity job = newPersistedJob();
        TrackingKey key = seed(job, 0, QUEUED_FOR_PROCESSING, Priority.NORMAL);

        int inserted = persistenceContext.run(() -> newDependencyTrackingRepository()
                .insert(key, SINK_ID, (int) SUBMITTER, READY_FOR_PROCESSING, Priority.LOW.getValue(), true));

        assertThat("nothing inserted", inserted, is(0));
        assertThat("status untouched", statusOf(key), is(QUEUED_FOR_PROCESSING.value));
    }

    /**
     * A job's rows go whatever status or gate state they are in, and the counts come back so the
     * caller can correct its counters without a full recount.
     */
    @org.junit.Test
    public void deleteByJob_reportsWhatItRemovedPerSinkAndStatus() throws Exception {
        JobEntity job = newPersistedJob();
        seed(job, 0, QUEUED_FOR_DELIVERY, Priority.NORMAL);
        seed(job, 1, QUEUED_FOR_DELIVERY, Priority.NORMAL, false);
        seed(job, 2, SCHEDULED_FOR_PROCESSING, Priority.NORMAL);
        JobEntity untouched = newPersistedJob();
        seed(untouched, 0, QUEUED_FOR_DELIVERY, Priority.NORMAL);

        var removed = persistenceContext.run(() -> newDependencyTrackingRepository().deleteByJob(job.getId()));

        assertThat("one sink", removed.keySet(), is(java.util.Set.of(SINK_ID)));
        assertThat("delivery rows, closed gate included",
                removed.get(SINK_ID).get(QUEUED_FOR_DELIVERY), is(2));
        assertThat("processing rows", removed.get(SINK_ID).get(SCHEDULED_FOR_PROCESSING), is(1));
        assertThat("the other job is untouched",
                statusOf(new TrackingKey(untouched.getId(), 0)), is(QUEUED_FOR_DELIVERY.value));
    }

    // ---------------------------------------------------------------- the row read

    /**
     * The gate comes back on the row, which is what lets the direct dispatch path decide from the
     * row it already holds instead of asking a second statement.
     */
    @org.junit.Test
    public void get_carriesTheGate() throws Exception {
        JobEntity job = newPersistedJob();
        TrackingKey open = seed(job, 0, SCHEDULED_FOR_DELIVERY, Priority.NORMAL);
        TrackingKey closed = seed(job, 1, SCHEDULED_FOR_DELIVERY, Priority.NORMAL, false);

        DependencyTrackingRepository repository = newDependencyTrackingRepository();

        assertThat("open gate", repository.get(open).orElseThrow().isGateOpen(), is(true));
        assertThat("closed gate", repository.get(closed).orElseThrow().isGateOpen(), is(false));
    }

    /**
     * {@link DependencyTrackingRepository#delete(TrackingKey)} builds its row from its own
     * {@code RETURNING} list rather than through the shared row mapper, so the gate has to be named
     * there too.
     * <p>
     * The unconditional overload can remove a row whose gate is shut, and a snapshot reporting the
     * field's default instead of the column would be a quiet lie to whatever consults it next.
     */
    @org.junit.Test
    public void delete_carriesTheGateOfTheRowItRemoved() throws Exception {
        JobEntity job = newPersistedJob();
        TrackingKey closed = seed(job, 0, SCHEDULED_FOR_DELIVERY, Priority.NORMAL, false);

        DependencyTracking removed = persistenceContext.run(() ->
                newDependencyTrackingRepository().delete(closed)).orElseThrow();

        assertThat(removed.isGateOpen(), is(false));
    }

    /**
     * The retry budget is per phase, so entering the delivery half clears what processing spent.
     * <p>
     * Without this a chunk that needed every retry to get through processing arrives in delivery
     * with the count already at the limit, and its first delivery stall is reported as beyond
     * repair without one delivery attempt having been retried.
     */
    @org.junit.Test
    public void validatedStatusChange_intoTheDeliveryHalf_clearsTheRetryCount() throws Exception {
        JobEntity job = newPersistedJob();
        TrackingKey key = seed(job, 0, QUEUED_FOR_PROCESSING, Priority.NORMAL);
        persistenceContext.run(() -> newDependencyTrackingRepository().resend(key, 3));
        assertThat("the chunk spent a retry in processing", retriesOf(key), is(1));
        persistenceContext.run(() -> newDependencyTrackingRepository()
                .updateStatusValidated(key, QUEUED_FOR_PROCESSING));

        persistenceContext.run(() -> newDependencyTrackingRepository()
                .updateStatusValidated(key, READY_FOR_DELIVERY));

        assertThat("status", statusOf(key), is(READY_FOR_DELIVERY.value));
        assertThat("the delivery half starts with a full budget", retriesOf(key), is(0));
    }

    /**
     * A move within one phase carries the count, or the budget the resend spends would reset
     * itself every time it was spent.
     */
    @org.junit.Test
    public void validatedStatusChange_withinThePhase_keepsTheRetryCount() throws Exception {
        JobEntity job = newPersistedJob();
        TrackingKey key = seed(job, 0, QUEUED_FOR_PROCESSING, Priority.NORMAL);
        persistenceContext.run(() -> newDependencyTrackingRepository().resend(key, 3));

        persistenceContext.run(() -> newDependencyTrackingRepository()
                .updateStatusValidated(key, QUEUED_FOR_PROCESSING));

        assertThat(retriesOf(key), is(1));
    }

    // ---------------------------------------------------------------- discovery from the table

    /**
     * Which sinks hold a status is answered from the rows, which is what lets the bulk submitter
     * find work the sink chunk counts have lost.
     */
    @org.junit.Test
    public void distinctSinkIdsWithStatus_namesTheSinksHoldingThatStatus() throws Exception {
        JobEntity job = newPersistedJob();
        seed(job, 0, SCHEDULED_FOR_PROCESSING, Priority.NORMAL);
        seed(job, 1, QUEUED_FOR_PROCESSING, Priority.NORMAL);

        DependencyTrackingRepository repository = newDependencyTrackingRepository();

        assertThat("the sink holds a chunk parked for processing",
                repository.distinctSinkIdsWithStatus(SCHEDULED_FOR_PROCESSING), is(Set.of(SINK_ID)));
        assertThat("and none parked for delivery",
                repository.distinctSinkIdsWithStatus(SCHEDULED_FOR_DELIVERY), is(Set.of()));
    }

    /**
     * One sink is named once however many chunks it holds, since the caller dispatches per sink.
     */
    @org.junit.Test
    public void distinctSinkIdsWithStatus_namesASinkOnce() throws Exception {
        JobEntity job = newPersistedJob();
        seed(job, 0, SCHEDULED_FOR_PROCESSING, Priority.NORMAL);
        seed(job, 1, SCHEDULED_FOR_PROCESSING, Priority.NORMAL);

        assertThat(newDependencyTrackingRepository().distinctSinkIdsWithStatus(SCHEDULED_FOR_PROCESSING),
                is(Set.of(SINK_ID)));
    }

    // ---------------------------------------------------------------- fixtures

    private List<TrackingKey> candidateKeys(int limit) {
        return newDependencyTrackingRepository().findProcessingCandidates(SINK_ID, limit).stream()
                .map(DependencyTrackingRepository.ProcessingCandidate::key)
                .toList();
    }

    private TrackingKey seed(JobEntity job, int chunkId, ChunkSchedulingStatus status, Priority priority)
            throws SQLException {
        return seed(job, chunkId, status, priority, true);
    }

    /**
     * Writes the chunk's row straight to PostgreSQL, so a test sets up the state it means to and not
     * whatever the scheduler would have produced on the way there. The chunk row has to exist first:
     * {@code dependencytracking_jobid_fkey} is a foreign key on {@code (jobid, chunkid)}.
     */
    private TrackingKey seed(JobEntity job, int chunkId, ChunkSchedulingStatus status, Priority priority,
                             boolean gateOpen) throws SQLException {
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
            statement.setInt(6, priority.getValue());
            statement.setBoolean(7, gateOpen);
            statement.executeUpdate();
            // newConnection() hands out a connection with auto-commit off.
            connection.commit();
        }
        return new TrackingKey(job.getId(), chunkId);
    }

    private int statusOf(TrackingKey key) throws SQLException {
        return trackingColumn(key, "status");
    }

    private int retriesOf(TrackingKey key) throws SQLException {
        return trackingColumn(key, "retries");
    }

    private int trackingColumn(TrackingKey key, String column) throws SQLException {
        try (Connection connection = newConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "select " + column + " from dependencytracking where jobid = ? and chunkid = ?")) {
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
