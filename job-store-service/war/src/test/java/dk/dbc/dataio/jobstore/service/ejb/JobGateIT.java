package dk.dbc.dataio.jobstore.service.ejb;

import dk.dbc.dataio.commons.types.Chunk;
import dk.dbc.dataio.commons.types.Priority;
import dk.dbc.dataio.commons.types.SinkContent;
import dk.dbc.dataio.commons.utils.test.model.SinkBuilder;
import dk.dbc.dataio.commons.utils.test.model.SinkContentBuilder;
import dk.dbc.dataio.jobstore.distributed.ChunkSchedulingStatus;
import dk.dbc.dataio.jobstore.distributed.DependencyTracking;
import dk.dbc.dataio.jobstore.distributed.TrackingKey;
import dk.dbc.dataio.jobstore.distributed.hz.store.DependencyTrackingStore;
import dk.dbc.dataio.jobstore.service.AbstractJobStoreIT;
import dk.dbc.dataio.jobstore.service.dependencytracking.DependencyTrackingService;
import dk.dbc.dataio.jobstore.service.entity.ChunkEntity;
import dk.dbc.dataio.jobstore.service.entity.JobEntity;
import dk.dbc.dataio.jobstore.types.JobStoreException;

import jakarta.persistence.EntityManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;

/**
 * The per-job gate on termination chunks, see docs/chunk-scheduling-redesign.md,
 * "Barrier Chunks - Per-Job Gate".
 * <p>
 * The assertions read the gate columns straight from PostgreSQL on their own connections, so they
 * see committed state and nothing else. What a closed gate withholds from dispatch is the dispatch
 * filter's contract and is asserted with it.
 */
public class JobGateIT extends AbstractJobStoreIT {
    private static final int SINK_ID = 4711;
    private static final long SUBMITTER = 820010;
    private static final long OTHER_SUBMITTER = 820011;

    /**
     * The MapStore's {@code do update set} clause names five columns and must never name the two
     * gate columns. This test is what fails if anyone extends it.
     */
    @org.junit.Test
    public void mapStoreDoesNotClobberGateColumns() throws Exception {
        JobEntity job = newPersistedTerminationJob(SUBMITTER, 2);
        TrackingKey key = new TrackingKey(job.getId(), 2);
        // dependencytracking (jobid, chunkid) is a foreign key into chunk, so the row the gate
        // writes needs its chunk. createJobTerminationChunkEntity persists that chunk first.
        newPersistedChunkEntity(new ChunkEntity.Key(key.getChunkId(), key.getJobId()));
        DependencyTracking tracker = new DependencyTracking(key, SINK_ID, (int) SUBMITTER, "" + SUBMITTER, Set.of());

        persistenceContext.run(() -> newJobGateRepository().upsertTerminationRow(
                key, SINK_ID, (int) SUBMITTER, tracker.getStatus(), tracker.getMatchKeys(), false));

        new DependencyTrackingStore(datasource).store(key, tracker.setStatus(ChunkSchedulingStatus.QUEUED_FOR_DELIVERY));

        assertThat("is_termination survived the MapStore", isTermination(key), is(true));
        assertThat("gate_open survived the MapStore", gateOpen(key), is(false));
    }

    /**
     * data_chunks_expected is the termination chunk's own chunk id, and the gate opens on the
     * delivery of the last data chunk.
     */
    @org.junit.Test
    public void jobWithTerminationChunk() throws Exception {
        JobEntity job = newPersistedTerminationJob(SUBMITTER, 3);
        markJobAsPartitioned(job);

        TrackingKey terminationChunk = new TrackingKey(job.getId(), 3);
        assertThat("data_chunks_expected", dataChunksExpected(job.getId()), is(3));
        assertThat("data_chunks_expected equals the termination chunk id",
                dataChunksExpected(job.getId()), is(terminationChunk.getChunkId()));
        assertThat("is_termination", isTermination(terminationChunk), is(true));
        assertThat("gate closed while data chunks are outstanding", gateOpen(terminationChunk), is(false));
        assertThat("termination_barrier_lifted", terminationBarrierLifted(job.getId()), is(false));

        deliverDataChunk(job.getId(), 0);
        deliverDataChunk(job.getId(), 1);
        assertThat("gate still closed", gateOpen(terminationChunk), is(false));

        deliverDataChunk(job.getId(), 2);
        assertThat("gate open after the last data chunk", gateOpen(terminationChunk), is(true));
        assertThat("data_chunks_delivered counts data chunks only",
                dataChunksDelivered(job.getId()), is(3));

        // The termination chunk does not count itself.
        deliverChunk(job.getId(), 3);
        assertThat("termination chunk was not counted", dataChunksDelivered(job.getId()), is(3));
        assertThat("barrier lifted on delivery", terminationBarrierLifted(job.getId()), is(true));
    }

    /**
     * A non-barrier sink type gets no termination chunk. Deliveries are still counted, but the job
     * never appears as a blocker to anything.
     */
    @org.junit.Test
    public void jobWithoutTerminationChunk() throws Exception {
        JobEntity job = newPersistedJob(SUBMITTER, 2, SinkContent.SinkType.ES);
        markJobAsPartitioned(job);

        assertThat("no termination row", isTermination(new TrackingKey(job.getId(), 2)), is(nullValue()));
        assertThat("data_chunks_expected untouched", dataChunksExpected(job.getId()), is(0));
        assertThat("termination_barrier_lifted stays null so the job never blocks",
                terminationBarrierLifted(job.getId()), is(nullValue()));

        deliverDataChunk(job.getId(), 0);
        deliverDataChunk(job.getId(), 1);
        assertThat("deliveries are still counted", dataChunksDelivered(job.getId()), is(2));
    }

    /**
     * A job with zero data chunks, the shape addAndScheduleEmptyJob produces. No data chunk will
     * ever be delivered to trigger the count, so the gate has to be open from the insert.
     */
    @org.junit.Test
    public void emptyJobGateIsOpenAtInsert() throws Exception {
        JobEntity job = newPersistedTerminationJob(SUBMITTER, 0);
        markJobAsPartitioned(job);

        TrackingKey terminationChunk = new TrackingKey(job.getId(), 0);
        assertThat("termination chunk id", isTermination(terminationChunk), is(true));
        assertThat("data_chunks_expected", dataChunksExpected(job.getId()), is(0));
        assertThat("gate open at insert", gateOpen(terminationChunk), is(true));
    }

    /**
     * All data chunks delivered before partitioning ended. There is no further delivery to
     * evaluate on, so the insert has to open the gate. Driven at bean level rather than by racing
     * the partitioning loop.
     */
    @org.junit.Test
    public void allDataChunksDeliveredBeforePartitioningEnds() throws Exception {
        JobEntity job = newPersistedTerminationJob(SUBMITTER, 2);

        deliverDataChunk(job.getId(), 0);
        deliverDataChunk(job.getId(), 1);
        assertThat("counted before the termination chunk existed", dataChunksDelivered(job.getId()), is(2));

        markJobAsPartitioned(job);

        assertThat("gate open at insert", gateOpen(new TrackingKey(job.getId(), 2)), is(true));
    }

    /**
     * An earlier job on the same submitter and sink holds the later job's gate closed, whatever
     * the earlier job's own gate state.
     */
    @org.junit.Test
    public void crossJobBarrierHoldsLaterJobClosed() throws Exception {
        JobEntity jobA = newPersistedTerminationJob(SUBMITTER, 1);
        JobEntity jobB = newPersistedTerminationJob(SUBMITTER, 1);
        markJobAsPartitioned(jobA);
        markJobAsPartitioned(jobB);

        // A's own gate opens, but its barrier is not lifted until its termination chunk is
        // delivered, so B stays closed even with its own counter complete.
        deliverDataChunk(jobA.getId(), 0);
        deliverDataChunk(jobB.getId(), 0);

        assertThat("A's own gate", gateOpen(new TrackingKey(jobA.getId(), 1)), is(true));
        assertThat("B held by A's barrier", gateOpen(new TrackingKey(jobB.getId(), 1)), is(false));

        // A different submitter on the same sink is not part of the barrier scope.
        JobEntity jobC = newPersistedTerminationJob(OTHER_SUBMITTER, 1);
        markJobAsPartitioned(jobC);
        deliverDataChunk(jobC.getId(), 0);
        assertThat("other submitter is unaffected", gateOpen(new TrackingKey(jobC.getId(), 1)), is(true));
    }

    /**
     * Delivering a termination chunk re-evaluates the jobs queued behind it, and opens only the
     * next eligible one because that job's own barrier is now in the way of the one after it.
     */
    @org.junit.Test
    public void reTriggerOpensTheNextJobOnly() throws Exception {
        JobEntity jobA = newPersistedTerminationJob(SUBMITTER, 1);
        JobEntity jobB = newPersistedTerminationJob(SUBMITTER, 1);
        JobEntity jobC = newPersistedTerminationJob(SUBMITTER, 1);
        markJobAsPartitioned(jobA);
        markJobAsPartitioned(jobB);
        markJobAsPartitioned(jobC);
        deliverDataChunk(jobA.getId(), 0);
        deliverDataChunk(jobB.getId(), 0);
        deliverDataChunk(jobC.getId(), 0);

        assertThat("B closed", gateOpen(new TrackingKey(jobB.getId(), 1)), is(false));
        assertThat("C closed", gateOpen(new TrackingKey(jobC.getId(), 1)), is(false));

        deliverChunk(jobA.getId(), 1);
        assertThat("B opened by the re-trigger", gateOpen(new TrackingKey(jobB.getId(), 1)), is(true));
        assertThat("C still behind B", gateOpen(new TrackingKey(jobC.getId(), 1)), is(false));

        deliverChunk(jobB.getId(), 1);
        assertThat("C opened in turn", gateOpen(new TrackingKey(jobC.getId(), 1)), is(true));
    }

    /**
     * The gate is reached through chunkDeliveringDone, not only by calling JobGateBean directly.
     */
    @org.junit.Test
    public void chunkDeliveringDoneAdvancesTheGate() throws Exception {
        JobEntity job = newPersistedTerminationJob(SUBMITTER, 1);
        markJobAsPartitioned(job);

        DependencyTrackingService trackingService = new DependencyTrackingService().init();
        TrackingKey dataChunk = new TrackingKey(job.getId(), 0);
        trackingService.add(new DependencyTracking(dataChunk, SINK_ID, (int) SUBMITTER)
                .setStatus(ChunkSchedulingStatus.QUEUED_FOR_DELIVERY));

        JobSchedulerBean jobSchedulerBean = new JobSchedulerBean(entityManager,
                mock(JobSchedulerTransactionsBean.class), null, null, trackingService, newJobGateBean(), newDeliveryDispatchRepository());

        persistenceContext.run(() -> JobsBeanTest.notAborted(job.getId(), jb ->
                jobSchedulerBean.chunkDeliveringDone(new Chunk(job.getId(), 0, Chunk.Type.DELIVERED))));

        assertThat("data chunk counted", dataChunksDelivered(job.getId()), is(1));
        assertThat("gate opened", gateOpen(new TrackingKey(job.getId(), 1)), is(true));
    }

    /**
     * A job's last data chunk and its own termination chunk, acknowledged concurrently.
     * <p>
     * This is the interleaving that deadlocks if any gate site takes the barrier scope's advisory
     * lock before the job row: the data chunk's transaction holds the job row from its increment
     * and then wants the advisory lock, so a termination chunk's transaction holding the advisory
     * lock and waiting for the same job row closes the cycle. PostgreSQL breaks it by aborting one
     * of the two, which surfaces here as an exception from either side.
     * <p>
     * It is reachable in production because deliveries within a job are not ordered:
     * optimizeDependencies prunes the termination chunk's waitingOn down to the chunks that
     * transitively cover the rest, so it can be dispatched while an earlier data chunk of its own
     * job is still in flight.
     */
    @org.junit.Test
    public void concurrentTerminationAndLastDataChunk_doesNotDeadlock() throws Exception {
        JobEntity job = newPersistedTerminationJob(SUBMITTER, 2);
        markJobAsPartitioned(job);
        deliverDataChunk(job.getId(), 0);

        EntityManager dataChunkEm = entityManager.getEntityManagerFactory().createEntityManager();
        EntityManager terminationEm = entityManager.getEntityManagerFactory().createEntityManager();
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            // Step one of the data-chunk branch, run by hand so the job row lock it takes is held
            // across the start of the termination delivery below. The rest of the branch follows
            // after that delivery is provably blocked.
            JobGateRepository dataChunkGate = new JobGateRepository().withEntityManager(dataChunkEm);
            dataChunkEm.getTransaction().begin();
            dataChunkGate.incrementDataChunksDelivered(job.getId());

            JobGateBean terminationGate = new JobGateBean(new JobGateRepository().withEntityManager(terminationEm));
            Future<?> terminationDelivery = executor.submit(() -> runInTransaction(terminationEm, () -> {
                terminationGate.advanceGateState(new TrackingKey(job.getId(), 2), SINK_ID, (int) SUBMITTER);
                return null;
            }));

            // It has to block on the job row held above, and must not have taken the advisory lock
            // on its way there.
            awaitRowLockWaiters(1);
            assertThat("termination delivery still blocked", terminationDelivery.isDone(), is(false));

            // Step two, the advisory lock that countDataChunk takes after its increment. It is
            // granted at once, because the termination delivery is blocked on the job row above
            // and has therefore not reached the lock. That is the whole claim of this test: had
            // either site taken the barrier scope before the job row, the two would hold what the
            // other wants, PostgreSQL's detector would abort one of them inside deadlock_timeout,
            // and the abort would surface as a failure here or from the future below.
            dataChunkGate.advisoryLock(SINK_ID, (int) SUBMITTER);
            dataChunkEm.getTransaction().commit();

            terminationDelivery.get(30, TimeUnit.SECONDS);
        } finally {
            // Roll back first, so a task still blocked on the job row can run to completion, and
            // only then wait for it. Closing an entity manager under a running task raises a
            // secondary failure that buries the one the test was reporting.
            if (dataChunkEm.getTransaction().isActive()) {
                dataChunkEm.getTransaction().rollback();
            }
            executor.shutdownNow();
            executor.awaitTermination(30, TimeUnit.SECONDS);
            dataChunkEm.close();
            terminationEm.close();
        }

        assertThat("both data chunks counted", dataChunksDelivered(job.getId()), is(2));
        assertThat("barrier lifted", terminationBarrierLifted(job.getId()), is(true));
    }

    /**
     * The own-job race: the job's last data chunk is delivered while partitioning is still ending,
     * so its increment is in flight when the termination chunk is inserted. The insert decides
     * under the job row lock, so it cannot read a short count and leave the gate closed with no
     * further delivery to open it.
     */
    @org.junit.Test
    public void terminationChunkInsertAgainstAnInFlightDelivery_gateEndsUpOpen() throws Exception {
        JobEntity job = newPersistedTerminationJob(SUBMITTER, 1);

        EntityManager deliveryEm = entityManager.getEntityManagerFactory().createEntityManager();
        EntityManager partitioningEm = entityManager.getEntityManagerFactory().createEntityManager();
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            JobGateBean deliveryGate = new JobGateBean(new JobGateRepository().withEntityManager(deliveryEm));
            deliveryEm.getTransaction().begin();
            deliveryGate.advanceGateState(new TrackingKey(job.getId(), 0), SINK_ID, (int) SUBMITTER);

            Future<?> partitioningDone = executor.submit(() -> {
                markJobAsPartitioned(job, partitioningEm);
                return null;
            });

            awaitRowLockWaiters(1);
            assertThat("termination chunk insert still blocked", partitioningDone.isDone(), is(false));

            deliveryEm.getTransaction().commit();
            partitioningDone.get(30, TimeUnit.SECONDS);
        } finally {
            if (deliveryEm.getTransaction().isActive()) {
                deliveryEm.getTransaction().rollback();
            }
            executor.shutdownNow();
            executor.awaitTermination(30, TimeUnit.SECONDS);
            deliveryEm.close();
            partitioningEm.close();
        }

        assertThat("the delivery was counted", dataChunksDelivered(job.getId()), is(1));
        assertThat("gate open at insert despite the concurrent delivery",
                gateOpen(new TrackingKey(job.getId(), 1)), is(true));
    }

    /**
     * The cross-job race: an earlier job's termination delivery lifts its barrier and re-evaluates
     * the jobs behind it, while a later job's termination chunk is being inserted and reads whether
     * that barrier still holds.
     * <p>
     * Left unserialised the two can both decline. The insert reads the earlier barrier as unlifted
     * because the delivery has not committed, and the delivery's scan misses the later job because
     * its row has not committed either, so the gate is left closed with nothing to open it. Both
     * sites therefore take the barrier scope's advisory lock, which is what this test holds by hand
     * to prove they wait for it. Whichever of the two then goes first, the other sees its committed
     * work, so the later job's gate ends open either way.
     */
    @org.junit.Test
    public void terminationInsertAgainstTheReTriggerOfAnEarlierJob_gateEndsUpOpen() throws Exception {
        JobEntity earlier = newPersistedTerminationJob(SUBMITTER, 1);
        markJobAsPartitioned(earlier);
        deliverDataChunk(earlier.getId(), 0);

        // The later job's own counter is complete before partitioning ends, so its gate is decided
        // by the insert and turns entirely on the cross-job barrier.
        JobEntity later = newPersistedTerminationJob(SUBMITTER, 1);
        deliverDataChunk(later.getId(), 0);
        assertThat("earlier job still blocks", terminationBarrierLifted(earlier.getId()), is(false));

        EntityManager lockEm = entityManager.getEntityManagerFactory().createEntityManager();
        EntityManager reTriggerEm = entityManager.getEntityManagerFactory().createEntityManager();
        EntityManager partitioningEm = entityManager.getEntityManagerFactory().createEntityManager();
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            // Held for the whole race, so neither site can decide until it is released.
            lockEm.getTransaction().begin();
            new JobGateRepository().withEntityManager(lockEm).advisoryLock(SINK_ID, (int) SUBMITTER);

            Future<?> terminationInsert = executor.submit(() -> {
                markJobAsPartitioned(later, partitioningEm);
                return null;
            });
            JobGateBean reTriggerGate = new JobGateBean(new JobGateRepository().withEntityManager(reTriggerEm));
            Future<?> reTrigger = executor.submit(() -> runInTransaction(reTriggerEm, () -> {
                reTriggerGate.advanceGateState(new TrackingKey(earlier.getId(), 1), SINK_ID, (int) SUBMITTER);
                return null;
            }));

            awaitAdvisoryLockWaiters(2);
            assertThat("the insert is still waiting", terminationInsert.isDone(), is(false));
            assertThat("the re-trigger is still waiting", reTrigger.isDone(), is(false));

            lockEm.getTransaction().commit();
            terminationInsert.get(30, TimeUnit.SECONDS);
            reTrigger.get(30, TimeUnit.SECONDS);
        } finally {
            if (lockEm.getTransaction().isActive()) {
                lockEm.getTransaction().rollback();
            }
            executor.shutdownNow();
            executor.awaitTermination(30, TimeUnit.SECONDS);
            lockEm.close();
            reTriggerEm.close();
            partitioningEm.close();
        }

        assertThat("earlier barrier lifted", terminationBarrierLifted(earlier.getId()), is(true));
        assertThat("later job's gate ends open", gateOpen(new TrackingKey(later.getId(), 1)), is(true));
    }

    /**
     * The job's last two data chunks, in the interleaving that decides whether an increment can be
     * lost: the second is issued while the first is still uncommitted.
     * <p>
     * Read-then-write would lose one here. The second delivery would read the counter before the
     * first commits, block on the write, and then store that stale value plus one, leaving the
     * counter one short of data_chunks_expected with no chunk left to deliver and the gate closed
     * for good. One atomic statement instead blocks and re-evaluates against the committed row when
     * it unblocks, so the counter lands on exactly the total.
     * <p>
     * The first delivery is driven by hand and its transaction held open, rather than starting two
     * threads and hoping they overlap. Releasing two threads together makes the interleaving likely,
     * not certain: the first can increment, return and commit before the second is scheduled at
     * all, and against a committed increment nothing can be lost however it is implemented. Holding
     * the first open and asserting the second is queued behind it is what makes every run test the
     * property.
     */
    @org.junit.Test
    public void concurrentLastTwoDataChunks_counterLandsExactlyOnExpected() throws Exception {
        JobEntity job = newPersistedTerminationJob(SUBMITTER, 2);
        markJobAsPartitioned(job);

        EntityManager firstEm = entityManager.getEntityManagerFactory().createEntityManager();
        EntityManager secondEm = entityManager.getEntityManagerFactory().createEntityManager();
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            // Straight to the repository rather than through JobGateBean, because the bean would go
            // on to evaluate the gate from inside this transaction, which is a different test.
            JobGateRepository firstDelivery = new JobGateRepository().withEntityManager(firstEm);
            firstEm.getTransaction().begin();
            firstDelivery.incrementDataChunksDelivered(job.getId());

            JobGateBean secondDelivery = new JobGateBean(new JobGateRepository().withEntityManager(secondEm));
            Future<?> second = executor.submit(() -> runInTransaction(secondEm, () -> {
                secondDelivery.advanceGateState(new TrackingKey(job.getId(), 1), SINK_ID, (int) SUBMITTER);
                return null;
            }));

            awaitRowLockWaiters(1);
            assertThat("second delivery queued behind the first", second.isDone(), is(false));

            firstEm.getTransaction().commit();
            second.get(30, TimeUnit.SECONDS);
        } finally {
            if (firstEm.getTransaction().isActive()) {
                firstEm.getTransaction().rollback();
            }
            executor.shutdownNow();
            executor.awaitTermination(30, TimeUnit.SECONDS);
            firstEm.close();
            secondEm.close();
        }

        assertThat("no lost update", dataChunksDelivered(job.getId()), is(2));
        assertThat("gate opened by the delivery that unblocked", gateOpen(new TrackingKey(job.getId(), 2)), is(true));
    }

    private void awaitAdvisoryLockWaiters(int expected) throws Exception {
        awaitLockWaiters("backends waiting on the barrier scope", expected, "advisory");
    }

    /**
     * A waiter for a row held by an uncommitted transaction queues on that transaction and reports
     * {@code transactionid}. A second waiter for the same row first takes a {@code tuple} lock to
     * establish its place in the queue and reports that instead, so matching {@code transactionid}
     * alone would undercount a test with two contenders for one row.
     */
    private void awaitRowLockWaiters(int expected) throws Exception {
        awaitLockWaiters("backends waiting on a row lock", expected, "transactionid", "tuple");
    }

    /**
     * Blocks until the expected number of backends are waiting on one of the given lock wait
     * events, so a test asserts on the interleaving it set up rather than on a sleep having been
     * long enough. A backend that never blocks fails the assertion instead of passing unnoticed,
     * which a sleep followed by {@code isDone()} cannot tell apart from a task the pool has yet to
     * start.
     */
    private void awaitLockWaiters(String description, int expected, String... waitEvents) throws Exception {
        for (int attempt = 0; attempt < 100 && lockWaiters(waitEvents) < expected; attempt++) {
            Thread.sleep(100);
        }
        assertThat(description, lockWaiters(waitEvents), is(expected));
    }

    private int lockWaiters(String... waitEvents) throws SQLException {
        try (Connection connection = newConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "select count(*) from pg_stat_activity " +
                             " where datname = current_database() and wait_event = any (?)")) {
            statement.setArray(1, connection.createArrayOf("text", waitEvents));
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getInt(1);
            }
        }
    }

    private <T> T runInTransaction(EntityManager em, Callable<T> callable) throws Exception {
        em.getTransaction().begin();
        try {
            T result = callable.call();
            em.getTransaction().commit();
            return result;
        } catch (Exception e) {
            // Without this a failing callable leaves the transaction active, and the caller's
            // close() then returns the connection to the pool still holding its row locks, which
            // hangs the next test's cleanup instead of failing this one.
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw e;
        }
    }

    private void markJobAsPartitioned(JobEntity job) throws JobStoreException {
        markJobAsPartitioned(job, entityManager);
    }

    /**
     * Partitioning on a dedicated entity manager, for the tests that run it on another thread.
     * Sharing the one the base class creates would have two threads inside a non-thread-safe entity
     * manager, safe only while the other thread happens to be doing nothing.
     */
    private void markJobAsPartitioned(JobEntity job, EntityManager em) throws JobStoreException {
        JobSchedulerBean jobSchedulerBean = new JobSchedulerBean(em,
                mock(JobSchedulerTransactionsBean.class), newPgJobStoreRepository(em), null,
                new DependencyTrackingService().init(), newJobGateBean(em), newDeliveryDispatchRepository(em));
        jobSchedulerBean.markJobAsPartitioned(job);
    }

    private void deliverDataChunk(int jobId, int chunkId) {
        deliverChunk(jobId, chunkId);
    }

    private void deliverChunk(int jobId, int chunkId) {
        persistenceContext.run(() -> newJobGateBean()
                .advanceGateState(new TrackingKey(jobId, chunkId), SINK_ID, (int) submitterOf(jobId)));
    }

    private long submitterOf(int jobId) {
        entityManager.clear();
        return entityManager.find(JobEntity.class, jobId).getSpecification().getSubmitterId();
    }

    private JobEntity newPersistedTerminationJob(long submitterId, int numberOfChunks) {
        return newPersistedJob(submitterId, numberOfChunks, SinkContent.SinkType.TICKLE);
    }

    private JobEntity newPersistedJob(long submitterId, int numberOfChunks, SinkContent.SinkType sinkType) {
        JobEntity jobEntity = newJobEntity(submitterId);
        jobEntity.setNumberOfChunks(numberOfChunks);
        jobEntity.setPriority(Priority.NORMAL);
        jobEntity.setCachedSink(newPersistedSinkCacheEntity(new SinkBuilder()
                .setId(SINK_ID)
                .setContent(new SinkContentBuilder().setSinkType(sinkType).build())
                .build()));
        persist(jobEntity);
        return jobEntity;
    }

    private Boolean isTermination(TrackingKey key) throws SQLException {
        return trackingFlag(key, "is_termination");
    }

    private Boolean gateOpen(TrackingKey key) throws SQLException {
        return trackingFlag(key, "gate_open");
    }

    /**
     * @return the column value, or null if the chunk has no dependencytracking row at all
     */
    private Boolean trackingFlag(TrackingKey key, String column) throws SQLException {
        try (Connection connection = newConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "select " + column + " from dependencytracking where jobid = ? and chunkid = ?")) {
            statement.setInt(1, key.getJobId());
            statement.setInt(2, key.getChunkId());
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? resultSet.getBoolean(1) : null;
            }
        }
    }

    private int dataChunksExpected(int jobId) throws SQLException {
        return (Integer) jobColumn(jobId, "data_chunks_expected");
    }

    private int dataChunksDelivered(int jobId) throws SQLException {
        return (Integer) jobColumn(jobId, "data_chunks_delivered");
    }

    private Boolean terminationBarrierLifted(int jobId) throws SQLException {
        return (Boolean) jobColumn(jobId, "termination_barrier_lifted");
    }

    private Object jobColumn(int jobId, String column) throws SQLException {
        try (Connection connection = newConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "select " + column + " from job where id = ?")) {
            statement.setInt(1, jobId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return null;
                }
                return resultSet.getObject(1);
            }
        }
    }
}
