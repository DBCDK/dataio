package dk.dbc.dataio.jobstore.service.ejb;

import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.jobstore.service.AbstractJobStoreIT;
import dk.dbc.dataio.jobstore.service.entity.ChunkEntity;
import dk.dbc.dataio.jobstore.service.entity.ItemEntity;
import dk.dbc.dataio.jobstore.service.entity.JobEntity;
import dk.dbc.dataio.jobstore.service.entity.NotificationEntity;
import dk.dbc.dataio.jobstore.service.entity.WatermarkEntity;
import dk.dbc.dataio.jobstore.test.types.FlowStoreReferencesBuilder;
import dk.dbc.dataio.jobstore.types.ItemDeliveryResult;
import dk.dbc.dataio.jobstore.types.ItemDeliveryResult.Status;
import dk.dbc.dataio.jobstore.types.JobStoreException;
import dk.dbc.dataio.jobstore.types.Notification;
import dk.dbc.dataio.jobstore.types.State;
import dk.dbc.dataio.jobstore.types.StateChange;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;

import java.util.Date;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static dk.dbc.dataio.jobstore.types.State.Phase.DELIVERING;
import static dk.dbc.dataio.jobstore.types.State.Phase.PARTITIONING;
import static dk.dbc.dataio.jobstore.types.State.Phase.PROCESSING;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Runs against a real Testcontainers PostgreSQL (see AbstractJobStoreIT) - the native
 * upsert's "WHERE (...) > (...)" tuple comparison and pessimistic-lock concurrency
 * behaviour are Postgres-specific and cannot be verified with mocks.
 */
public class PgJobStore_AddItemDeliveredIT extends AbstractJobStoreIT {
    private static final int SINK_ID = 42;
    private static final String RECORD_KEY = "870970:12345678";

    // ******************** upsertWatermark: native SQL tuple-comparison matrix ********************

    @org.junit.Test
    public void upsertWatermark_noExistingRow_insertsRow() {
        PgJobStoreRepository repository = newPgJobStoreRepository();

        persistenceContext.run(() -> {
            repository.upsertWatermark(SINK_ID, RECORD_KEY, 100, 5, (short) 3);
            return null;
        });

        WatermarkEntity watermark = findWatermark();
        assertThat("watermark exists", watermark, is(notNullValue()));
        assertThat("watermark jobId", watermark.getJobId(), is(100));
        assertThat("watermark chunkId", watermark.getChunkId(), is(5));
        assertThat("watermark itemId", watermark.getItemId(), is((short) 3));
    }

    @org.junit.Test
    public void upsertWatermark_olderTuple_leavesWatermarkUnchanged() {
        PgJobStoreRepository repository = newPgJobStoreRepository();
        persistenceContext.run(() -> {
            repository.upsertWatermark(SINK_ID, RECORD_KEY, 100, 5, (short) 3);
            return null;
        });

        persistenceContext.run(() -> {
            repository.upsertWatermark(SINK_ID, RECORD_KEY, 100, 5, (short) 2);
            return null;
        });

        WatermarkEntity watermark = findWatermark();
        assertThat("watermark itemId unchanged", watermark.getItemId(), is((short) 3));
    }

    @org.junit.Test
    public void upsertWatermark_equalTuple_leavesWatermarkUnchanged_noError() {
        PgJobStoreRepository repository = newPgJobStoreRepository();
        persistenceContext.run(() -> {
            repository.upsertWatermark(SINK_ID, RECORD_KEY, 100, 5, (short) 3);
            return null;
        });

        persistenceContext.run(() -> {
            repository.upsertWatermark(SINK_ID, RECORD_KEY, 100, 5, (short) 3);
            return null;
        });

        WatermarkEntity watermark = findWatermark();
        assertThat("watermark itemId unchanged", watermark.getItemId(), is((short) 3));
    }

    @org.junit.Test
    public void upsertWatermark_newerTuple_advancesWatermark() {
        PgJobStoreRepository repository = newPgJobStoreRepository();
        persistenceContext.run(() -> {
            repository.upsertWatermark(SINK_ID, RECORD_KEY, 100, 5, (short) 3);
            return null;
        });

        persistenceContext.run(() -> {
            repository.upsertWatermark(SINK_ID, RECORD_KEY, 100, 6, (short) 0);
            return null;
        });

        WatermarkEntity watermark = findWatermark();
        assertThat("watermark chunkId advanced", watermark.getChunkId(), is(6));
        assertThat("watermark itemId advanced", watermark.getItemId(), is((short) 0));
    }

    // ******************** addItemDelivered: end-to-end ********************

    @org.junit.Test
    public void addItemDelivered_lastItemOfSingleItemJob_completesChunkAndJob_upsertsWatermark() throws JobStoreException {
        JobEntity job = newJob(1);
        ChunkEntity chunk = newChunk(job.getId(), 1);
        ItemEntity item = newDeliverableItem(job.getId(), chunk.getKey().getId(), (short) 0);
        PgJobStore pgJobStore = newPgJobStore();

        boolean chunkDeliveringDone = persistenceContext.run(() ->
                pgJobStore.addItemDelivered(job.getId(), chunk.getKey().getId(), item.getKey().getId(),
                        new ItemDeliveryResult(SINK_ID, RECORD_KEY, Status.DELIVERED,
                                ChunkItem.successfulChunkItem().withId(item.getKey().getId()).withData("data"))));

        assertThat("chunk delivering done", chunkDeliveringDone, is(true));

        entityManager.clear();
        ItemEntity refreshedItem = entityManager.find(ItemEntity.class, item.getKey());
        assertThat("item delivering outcome", refreshedItem.getDeliveringOutcome(), is(notNullValue()));

        ChunkEntity refreshedChunk = entityManager.find(ChunkEntity.class, chunk.getKey());
        assertThat("chunk DELIVERING closed", refreshedChunk.getState().getPhase(DELIVERING).getEndDate(), is(notNullValue()));
        assertThat("chunk time of completion", refreshedChunk.getTimeOfCompletion(), is(notNullValue()));

        JobEntity refreshedJob = entityManager.find(JobEntity.class, job.getId());
        assertThat("job time of completion", refreshedJob.getTimeOfCompletion(), is(notNullValue()));

        WatermarkEntity watermark = findWatermark();
        assertThat("watermark exists", watermark, is(notNullValue()));
        assertThat("watermark jobId", watermark.getJobId(), is(job.getId()));
    }

    @org.junit.Test
    public void addItemDelivered_notLastItemOfChunk_doesNotCompleteChunk() throws JobStoreException {
        JobEntity job = newJob(2);
        ChunkEntity chunk = newChunk(job.getId(), 2);
        ItemEntity item = newDeliverableItem(job.getId(), chunk.getKey().getId(), (short) 0);
        newDeliverableItem(job.getId(), chunk.getKey().getId(), (short) 1);
        PgJobStore pgJobStore = newPgJobStore();

        boolean chunkDeliveringDone = persistenceContext.run(() ->
                pgJobStore.addItemDelivered(job.getId(), chunk.getKey().getId(), item.getKey().getId(),
                        new ItemDeliveryResult(SINK_ID, RECORD_KEY, Status.DELIVERED,
                                ChunkItem.successfulChunkItem().withId(item.getKey().getId()).withData("data"))));

        assertThat("chunk delivering done", chunkDeliveringDone, is(false));

        entityManager.clear();
        ChunkEntity refreshedChunk = entityManager.find(ChunkEntity.class, chunk.getKey());
        assertThat("chunk DELIVERING not closed", refreshedChunk.getState().getPhase(DELIVERING).getEndDate(), is(nullValue()));
        assertThat("chunk time of completion", refreshedChunk.getTimeOfCompletion(), is(nullValue()));
    }

    @org.junit.Test
    public void addItemDelivered_failedStatus_doesNotUpsertWatermark() throws JobStoreException {
        JobEntity job = newJob(1);
        ChunkEntity chunk = newChunk(job.getId(), 1);
        ItemEntity item = newDeliverableItem(job.getId(), chunk.getKey().getId(), (short) 0);
        PgJobStore pgJobStore = newPgJobStore();

        persistenceContext.run(() ->
                pgJobStore.addItemDelivered(job.getId(), chunk.getKey().getId(), item.getKey().getId(),
                        new ItemDeliveryResult(SINK_ID, RECORD_KEY, Status.FAILED,
                                ChunkItem.failedChunkItem().withId(item.getKey().getId()).withData("data"))));

        assertThat("no watermark row", findWatermark(), is(nullValue()));
    }

    /**
     * An item a sink chose not to send counts as ignored and claims no watermark. Both
     * halves matter: the counter keeps the meaning the chunk-level path gave it, and the
     * absent row is what lets a genuinely older version of the record still be delivered
     * rather than judged stale against a delivery that never happened.
     */
    @org.junit.Test
    public void addItemDelivered_ignoredStatus_countsAsIgnoredAndDoesNotUpsertWatermark() throws JobStoreException {
        JobEntity job = newJob(1);
        ChunkEntity chunk = newChunk(job.getId(), 1);
        ItemEntity item = newDeliverableItem(job.getId(), chunk.getKey().getId(), (short) 0);
        PgJobStore pgJobStore = newPgJobStore();

        persistenceContext.run(() ->
                pgJobStore.addItemDelivered(job.getId(), chunk.getKey().getId(), item.getKey().getId(),
                        new ItemDeliveryResult(SINK_ID, RECORD_KEY, Status.IGNORED,
                                ChunkItem.ignoredChunkItem().withId(item.getKey().getId()).withData("data"))));

        ItemEntity refreshedItem = entityManager.find(ItemEntity.class, item.getKey());
        assertThat("item ignored in delivering",
                refreshedItem.getState().getPhase(DELIVERING).getIgnored(), is(1));
        assertThat("item succeeded in delivering",
                refreshedItem.getState().getPhase(DELIVERING).getSucceeded(), is(0));
        assertThat("no watermark row", findWatermark(), is(nullValue()));
    }

    // ******************** jobs with a termination chunk ********************
    //
    // A job with a termination chunk has numberOfItems == PARTITIONING count + 1,
    // because createJobTerminationChunkEntity bumps numberOfItems without contributing
    // to the job's PARTITIONING counters. The job's DELIVERING phase therefore closes
    // on the last DATA item, before the termination item has reported, and only the
    // termination item may complete the job.

    @org.junit.Test
    public void addItemDelivered_lastDataItemOfJobWithTerminationChunk_doesNotCompleteJob() throws JobStoreException {
        JobEntity job = newJobWithTerminationChunk(1);
        ItemEntity dataItem = newDeliverableItem(job.getId(), 0, (short) 0);
        PgJobStore pgJobStore = newPgJobStore();

        persistenceContext.run(() -> pgJobStore.addItemDelivered(job.getId(), 0, dataItem.getKey().getId(),
                new ItemDeliveryResult(SINK_ID, RECORD_KEY, Status.DELIVERED,
                        ChunkItem.successfulChunkItem().withId(dataItem.getKey().getId()).withData("data"))));

        entityManager.clear();
        JobEntity refreshedJob = entityManager.find(JobEntity.class, job.getId());
        assertThat("job DELIVERING closed by the last data item",
                refreshedJob.getState().getPhase(DELIVERING).getEndDate(), is(notNullValue()));
        assertThat("job not completed, termination item still outstanding",
                refreshedJob.getTimeOfCompletion(), is(nullValue()));
        assertThat("no JOB_COMPLETED notification yet", findAllNotifications(), is(empty()));
    }

    @org.junit.Test
    public void addItemDelivered_terminationItemAfterLastDataItem_completesJobAndNotifies() throws JobStoreException {
        JobEntity job = newJobWithTerminationChunk(1);
        ItemEntity dataItem = newDeliverableItem(job.getId(), 0, (short) 0);
        ItemEntity terminationItem = newTerminationItem(job.getId(), 1, ChunkItem.Status.SUCCESS);
        PgJobStore pgJobStore = newPgJobStore();

        persistenceContext.run(() -> pgJobStore.addItemDelivered(job.getId(), 0, dataItem.getKey().getId(),
                new ItemDeliveryResult(SINK_ID, RECORD_KEY, Status.DELIVERED,
                        ChunkItem.successfulChunkItem().withId(dataItem.getKey().getId()).withData("data"))));

        persistenceContext.run(() -> pgJobStore.addItemDelivered(job.getId(), 1, terminationItem.getKey().getId(),
                new ItemDeliveryResult(SINK_ID, null, Status.DELIVERED,
                        ChunkItem.successfulChunkItem().withId(terminationItem.getKey().getId()).withData("done"))));

        entityManager.clear();
        JobEntity refreshedJob = entityManager.find(JobEntity.class, job.getId());
        assertThat("job completed by the termination item", refreshedJob.getTimeOfCompletion(), is(notNullValue()));
        assertThat("no fatal error for a successful termination item", refreshedJob.hasFatalError(), is(false));

        List<NotificationEntity> notifications = findAllNotifications();
        assertThat("exactly one JOB_COMPLETED notification", notifications.size(), is(1));
        assertThat("notification type", notifications.get(0).getType(), is(Notification.Type.JOB_COMPLETED));
    }

    @org.junit.Test
    public void addItemDelivered_failedTerminationItem_completesJobWithFatalError() throws JobStoreException {
        JobEntity job = newJobWithTerminationChunk(1);
        ItemEntity dataItem = newDeliverableItem(job.getId(), 0, (short) 0);
        ItemEntity terminationItem = newTerminationItem(job.getId(), 1, ChunkItem.Status.FAILURE);
        PgJobStore pgJobStore = newPgJobStore();

        persistenceContext.run(() -> pgJobStore.addItemDelivered(job.getId(), 0, dataItem.getKey().getId(),
                new ItemDeliveryResult(SINK_ID, RECORD_KEY, Status.DELIVERED,
                        ChunkItem.successfulChunkItem().withId(dataItem.getKey().getId()).withData("data"))));

        persistenceContext.run(() -> pgJobStore.addItemDelivered(job.getId(), 1, terminationItem.getKey().getId(),
                new ItemDeliveryResult(SINK_ID, null, Status.FAILED,
                        ChunkItem.failedChunkItem().withId(terminationItem.getKey().getId()).withData("boom"))));

        entityManager.clear();
        JobEntity refreshedJob = entityManager.find(JobEntity.class, job.getId());
        assertThat("job completed", refreshedJob.getTimeOfCompletion(), is(notNullValue()));
        assertThat("fatal error set by the failed termination item", refreshedJob.hasFatalError(), is(true));
    }

    @org.junit.Test
    public void addItemDelivered_redeliveredTerminationItem_doesNotCompleteJobTwice() throws JobStoreException {
        JobEntity job = newJobWithTerminationChunk(1);
        ItemEntity dataItem = newDeliverableItem(job.getId(), 0, (short) 0);
        ItemEntity terminationItem = newTerminationItem(job.getId(), 1, ChunkItem.Status.SUCCESS);
        PgJobStore pgJobStore = newPgJobStore();

        persistenceContext.run(() -> pgJobStore.addItemDelivered(job.getId(), 0, dataItem.getKey().getId(),
                new ItemDeliveryResult(SINK_ID, RECORD_KEY, Status.DELIVERED,
                        ChunkItem.successfulChunkItem().withId(dataItem.getKey().getId()).withData("data"))));

        ItemDeliveryResult terminationResult = new ItemDeliveryResult(SINK_ID, null, Status.DELIVERED,
                ChunkItem.successfulChunkItem().withId(terminationItem.getKey().getId()).withData("done"));
        persistenceContext.run(() -> pgJobStore.addItemDelivered(job.getId(), 1, terminationItem.getKey().getId(), terminationResult));
        persistenceContext.run(() -> pgJobStore.addItemDelivered(job.getId(), 1, terminationItem.getKey().getId(), terminationResult));

        assertThat("still exactly one JOB_COMPLETED notification", findAllNotifications().size(), is(1));
    }

    // ******************** concurrency: two items of one chunk, real pessimistic locking ********************

    @org.junit.Test
    public void addItemDelivered_concurrentItemsOfSameChunk_bothCounted_chunkDeliveringDoneFiresExactlyOnce() throws Exception {
        JobEntity job = newJob(2);
        ChunkEntity chunk = newChunk(job.getId(), 2);
        ItemEntity item0 = newDeliverableItem(job.getId(), chunk.getKey().getId(), (short) 0);
        ItemEntity item1 = newDeliverableItem(job.getId(), chunk.getKey().getId(), (short) 1);

        EntityManager em0 = entityManager.getEntityManagerFactory().createEntityManager();
        EntityManager em1 = entityManager.getEntityManagerFactory().createEntityManager();
        try {
            PgJobStore pgJobStore0 = newPgJobStore(em0);
            PgJobStore pgJobStore1 = newPgJobStore(em1);

            Callable<Boolean> deliverItem0 = () -> runInTransaction(em0, () -> pgJobStore0.addItemDelivered(
                    job.getId(), chunk.getKey().getId(), item0.getKey().getId(),
                    new ItemDeliveryResult(SINK_ID, null, Status.DELIVERED,
                            ChunkItem.successfulChunkItem().withId(item0.getKey().getId()).withData("data"))));
            Callable<Boolean> deliverItem1 = () -> runInTransaction(em1, () -> pgJobStore1.addItemDelivered(
                    job.getId(), chunk.getKey().getId(), item1.getKey().getId(),
                    new ItemDeliveryResult(SINK_ID, null, Status.DELIVERED,
                            ChunkItem.successfulChunkItem().withId(item1.getKey().getId()).withData("data"))));

            ExecutorService executor = Executors.newFixedThreadPool(2);
            List<Future<Boolean>> futures = executor.invokeAll(List.of(deliverItem0, deliverItem1));
            executor.shutdown();

            long chunkDeliveringDoneCount = 0;
            for (Future<Boolean> future : futures) {
                if (future.get()) {
                    chunkDeliveringDoneCount++;
                }
            }
            assertThat("chunkDeliveringDone fires exactly once", chunkDeliveringDoneCount, is(1L));
        } finally {
            em0.close();
            em1.close();
        }

        entityManager.clear();
        ChunkEntity refreshedChunk = entityManager.find(ChunkEntity.class, chunk.getKey());
        assertThat("chunk DELIVERING succeeded count", refreshedChunk.getState().getPhase(DELIVERING).getSucceeded(), is(2));
        assertThat("chunk DELIVERING closed", refreshedChunk.getState().getPhase(DELIVERING).getEndDate(), is(notNullValue()));
    }

    @org.junit.Test
    public void addItemDelivered_concurrentRedeliveryOfSameItem_doesNotDoubleCount() throws Exception {
        // 3 items, not 2: with only 2 items, a double-counted item0 plus a correctly
        // counted item1 lands on the same total (2) as the correct outcome (item0 once
        // + item1 once), so the two cases would be indistinguishable. With 3 items,
        // "double-counted" (3, item2 never delivered) and "correct" (2, item2 still
        // pending) diverge, so the assertions below actually prove which one happened.
        JobEntity job = newJob(3);
        ChunkEntity chunk = newChunk(job.getId(), 3);
        ItemEntity item0 = newDeliverableItem(job.getId(), chunk.getKey().getId(), (short) 0);
        ItemEntity item1 = newDeliverableItem(job.getId(), chunk.getKey().getId(), (short) 1);
        newDeliverableItem(job.getId(), chunk.getKey().getId(), (short) 2);

        EntityManager emController = entityManager.getEntityManagerFactory().createEntityManager();
        EntityManager em0 = entityManager.getEntityManagerFactory().createEntityManager();
        EntityManager em1 = entityManager.getEntityManagerFactory().createEntityManager();
        try {
            // Hold the chunk's exclusive lock externally so both racing calls below are
            // guaranteed to complete their own unlocked idempotency pre-check (both
            // seeing deliveringOutcome == null) before either can proceed past the
            // chunk lock - this reproduces the same-item concurrent-redelivery race
            // deterministically, rather than hoping thread scheduling happens to
            // interleave that way.
            emController.getTransaction().begin();
            emController.find(ChunkEntity.class, chunk.getKey(), LockModeType.PESSIMISTIC_WRITE);

            PgJobStore pgJobStore0 = newPgJobStore(em0);
            PgJobStore pgJobStore1 = newPgJobStore(em1);
            Callable<Boolean> firstDelivery = () -> runInTransaction(em0, () -> pgJobStore0.addItemDelivered(
                    job.getId(), chunk.getKey().getId(), item0.getKey().getId(),
                    new ItemDeliveryResult(SINK_ID, null, Status.DELIVERED,
                            ChunkItem.successfulChunkItem().withId(item0.getKey().getId()).withData("data"))));
            Callable<Boolean> duplicateRedelivery = () -> runInTransaction(em1, () -> pgJobStore1.addItemDelivered(
                    job.getId(), chunk.getKey().getId(), item0.getKey().getId(),
                    new ItemDeliveryResult(SINK_ID, null, Status.DELIVERED,
                            ChunkItem.successfulChunkItem().withId(item0.getKey().getId()).withData("data"))));

            ExecutorService executor = Executors.newFixedThreadPool(2);
            List<Future<Boolean>> futures = List.of(executor.submit(firstDelivery), executor.submit(duplicateRedelivery));

            // Give both threads time to reach the (blocked) chunk-lock acquisition -
            // both must have already performed their own unlocked pre-check by now.
            Thread.sleep(500);
            emController.getTransaction().commit();

            for (Future<Boolean> future : futures) {
                future.get();
            }
            executor.shutdown();
        } finally {
            emController.close();
            em0.close();
            em1.close();
        }

        // Deliver one more (genuinely distinct) item normally. item2 is deliberately
        // left undelivered: if the duplicate above had been double-counted, the total
        // would wrongly reach 3 (this chunk's full count) and the phase would wrongly
        // close despite item2 never having been delivered.
        persistenceContext.run(() -> newPgJobStore().addItemDelivered(
                job.getId(), chunk.getKey().getId(), item1.getKey().getId(),
                new ItemDeliveryResult(SINK_ID, null, Status.DELIVERED,
                        ChunkItem.successfulChunkItem().withId(item1.getKey().getId()).withData("data"))));

        entityManager.clear();
        ChunkEntity refreshedChunk = entityManager.find(ChunkEntity.class, chunk.getKey());
        assertThat("chunk DELIVERING succeeded count reflects 2 genuine deliveries, not 3",
                refreshedChunk.getState().getPhase(DELIVERING).getSucceeded(), is(2));
        assertThat("chunk DELIVERING correctly still open pending item2",
                refreshedChunk.getState().getPhase(DELIVERING).getEndDate(), is(nullValue()));

        ItemEntity refreshedItem0 = entityManager.find(ItemEntity.class, item0.getKey());
        assertThat("item0 delivering outcome", refreshedItem0.getDeliveringOutcome(), is(notNullValue()));
    }

    private <T> T runInTransaction(EntityManager em, Callable<T> callable) throws Exception {
        em.getTransaction().begin();
        try {
            T result = callable.call();
            em.getTransaction().commit();
            return result;
        } catch (Exception e) {
            // Without this, a failing callable leaves the transaction active; the
            // caller's em.close() then returns the underlying connection to the pool
            // still "idle in transaction" at the Postgres level, holding whatever
            // row locks it acquired forever (no lock/statement timeout is configured
            // anywhere in this test setup) - which then hangs the NEXT test's @After
            // DELETE FROM cleanup instead of failing this one.
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw e;
        }
    }

    /*
     * Fixture helpers
     */

    private WatermarkEntity findWatermark() {
        entityManager.clear();
        return entityManager.find(WatermarkEntity.class, new WatermarkEntity.Key(SINK_ID, RECORD_KEY));
    }

    private PgJobStore newPgJobStore() {
        return newPgJobStore(entityManager);
    }

    private PgJobStore newPgJobStore(EntityManager em) {
        PgJobStore pgJobStore = new PgJobStore();
        pgJobStore.entityManager = em;
        PgJobStoreRepository repository = new PgJobStoreRepository();
        repository.entityManager = em;
        pgJobStore.jobStoreRepository = repository;
        pgJobStore.jobNotificationRepository = new JobNotificationRepository();
        pgJobStore.jobNotificationRepository.entityManager = em;
        return pgJobStore;
    }

    private JobEntity newJob(int numberOfItems) {
        JobEntity jobEntity = new JobEntity();
        jobEntity.setSpecification(createJobSpecification());
        jobEntity.setFlowStoreReferences(new FlowStoreReferencesBuilder().build());
        jobEntity.setNumberOfItems(numberOfItems);
        jobEntity.setState(closedPhases(numberOfItems, PARTITIONING, PROCESSING));
        persist(jobEntity);
        return jobEntity;
    }

    /**
     * Mirrors the accounting createJobTerminationChunkEntity leaves behind: numberOfItems
     * counts the termination item, the job's PARTITIONING/PROCESSING counters do not.
     * A notification destination is set so the JOB_COMPLETED notification is actually
     * written, which the default spec from createJobSpecification() leaves off.
     */
    private JobEntity newJobWithTerminationChunk(int numberOfDataItems) {
        JobEntity jobEntity = new JobEntity();
        jobEntity.setSpecification(createJobSpecification()
                .withMailForNotificationAboutProcessing("test@dbc.dk"));
        jobEntity.setFlowStoreReferences(new FlowStoreReferencesBuilder().build());
        jobEntity.setNumberOfItems(numberOfDataItems + 1);
        jobEntity.setState(closedPhases(numberOfDataItems, PARTITIONING, PROCESSING));
        persist(jobEntity);

        newChunkWithId(jobEntity.getId(), 0, numberOfDataItems);
        newChunkWithId(jobEntity.getId(), 1, 1);
        return jobEntity;
    }

    private ChunkEntity newChunkWithId(int jobId, int chunkId, int numberOfItems) {
        ChunkEntity chunkEntity = newChunkEntity(new ChunkEntity.Key(chunkId, jobId));
        chunkEntity.setNumberOfItems((short) numberOfItems);
        chunkEntity.setState(closedPhases(numberOfItems, PARTITIONING, PROCESSING));
        persist(chunkEntity);
        return chunkEntity;
    }

    /**
     * isTerminationItem() identifies a termination item by its PROCESSING outcome being
     * typed JOB_END, exactly as createJobTerminationChunkEntity persists it.
     */
    private ItemEntity newTerminationItem(int jobId, int chunkId, ChunkItem.Status status) {
        ItemEntity itemEntity = newItemEntity(new ItemEntity.Key(jobId, chunkId, (short) 0));
        itemEntity.setState(closedPhases(1, PARTITIONING, PROCESSING));
        ChunkItem terminationItem = new ChunkItem()
                .withId(0)
                .withStatus(status)
                .withType(ChunkItem.Type.JOB_END)
                .withData("Job termination item");
        itemEntity.setPartitioningOutcome(terminationItem);
        itemEntity.setProcessingOutcome(terminationItem);
        persist(itemEntity);
        return itemEntity;
    }

    private List<NotificationEntity> findAllNotifications() {
        entityManager.clear();
        return entityManager.createQuery("SELECT e FROM NotificationEntity e", NotificationEntity.class)
                .getResultList();
    }

    private ChunkEntity newChunk(int jobId, int numberOfItems) {
        ChunkEntity chunkEntity = newChunkEntity(new ChunkEntity.Key(0, jobId));
        chunkEntity.setNumberOfItems((short) numberOfItems);
        chunkEntity.setState(closedPhases(numberOfItems, PARTITIONING, PROCESSING));
        persist(chunkEntity);
        return chunkEntity;
    }

    private ItemEntity newDeliverableItem(int jobId, int chunkId, short itemId) {
        ItemEntity itemEntity = newItemEntity(new ItemEntity.Key(jobId, chunkId, itemId));
        itemEntity.setState(closedPhases(1, PARTITIONING, PROCESSING));
        persist(itemEntity);
        return itemEntity;
    }

    private State closedPhases(int succeeded, State.Phase... phases) {
        State state = new State();
        StateChange stateChange = new StateChange();
        for (State.Phase phase : phases) {
            stateChange.setPhase(phase)
                    .setSucceeded(succeeded)
                    .setBeginDate(new Date())
                    .setEndDate(new Date());
            state.updateState(stateChange);
        }
        return state;
    }
}
