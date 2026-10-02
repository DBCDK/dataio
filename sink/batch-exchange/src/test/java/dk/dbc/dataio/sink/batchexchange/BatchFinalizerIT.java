package dk.dbc.dataio.sink.batchexchange;

import dk.dbc.batchexchange.dto.Batch;
import dk.dbc.batchexchange.dto.BatchEntry;
import dk.dbc.commons.addi.AddiRecord;
import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.types.Diagnostic;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnector;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnectorException;
import dk.dbc.dataio.commons.utils.lang.StringUtil;
import dk.dbc.dataio.jobstore.types.ItemDeliveryResult;
import dk.dbc.dataio.jse.artemis.common.service.ServiceHub;
import dk.dbc.dataio.jse.artemis.common.service.ZombieWatch;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static dk.dbc.commons.testutil.Assert.assertThat;
import static dk.dbc.commons.testutil.Assert.isThrowing;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyShort;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;

class BatchFinalizerIT extends IntegrationTest {
    private final JobStoreServiceConnector jobStoreServiceConnector = mock(JobStoreServiceConnector.class);

    @Test
    void noCompletedBatch_nothingIsFinalized() {
        assertThat(finalizer().finalizeNextCompletedBatch(), is(false));
    }

    @Test
    void reportingThrows_theBatchIsLeftForTheNextPass() throws JobStoreServiceConnectorException {
        executeScriptResource("/completed_batches.sql");
        doThrow(new JobStoreServiceConnectorException("Died"))
                .when(jobStoreServiceConnector).addItemDelivered(any(), anyInt(), anyInt(), anyShort());

        assertThat(finalizer()::finalizeNextCompletedBatch, isThrowing(RuntimeException.class));
    }

    /* Leaving it would have the finalizer meet it again on every pass and reach nothing
       behind it, and its item cannot be addressed, so there is nothing to report it against. */
    @Test
    void batchWithNoBookkeepingRow_isDiscardedWithoutBeingReported()
            throws JobStoreServiceConnectorException {
        executeScriptResource("/batch_without_staged_item.sql");

        assertThat("batch was handled", finalizer().finalizeNextCompletedBatch(), is(true));

        verify(jobStoreServiceConnector, never()).addItemDelivered(any(), anyInt(), anyInt(), anyShort());
        assertThat("batch removed", finalizer().finalizeNextCompletedBatch(), is(false));
    }

    @Test
    void ignoredEntry_itemIsReportedIgnored() throws JobStoreServiceConnectorException {
        executeScriptResource("/completed_batches.sql");

        assertThat("batch was finalized", finalizer().finalizeNextCompletedBatch(), is(true));

        ItemDeliveryResult result = reported(JOB_ID, 0, (short) 1);
        assertThat("verdict", result.status(), is(ItemDeliveryResult.Status.IGNORED));
        assertThat("watermark row sink id", result.sinkId(), is(15L));
        assertThat("watermark row record key", result.recordKey(), is("870970:1"));

        ChunkItem outcome = result.chunkItem();
        assertThat("outcome item id", outcome.getId(), is(1L));
        assertThat("outcome status", outcome.getStatus(), is(ChunkItem.Status.IGNORE));
        assertThat("outcome tracking id", outcome.getTrackingId(), is("42-0-1"));
        assertThat("outcome diagnostics", outcome.getDiagnostics(), is(nullValue()));
        assertThat("outcome data", StringUtil.asString(outcome.getData()),
                is("Consumer system responded with OK: ok42-0-1\n"));
    }

    @Test
    void acceptedEntryWithAWarning_itemIsReportedDelivered() throws JobStoreServiceConnectorException {
        executeScriptResource("/completed_batches.sql");
        finalizeAll();

        ItemDeliveryResult result = reported(JOB_ID, 0, (short) 2);
        assertThat("verdict", result.status(), is(ItemDeliveryResult.Status.DELIVERED));
        assertThat("watermark row record key", result.recordKey(), is("870970:2"));

        ChunkItem outcome = result.chunkItem();
        assertThat("outcome status", outcome.getStatus(), is(ChunkItem.Status.SUCCESS));
        assertThat("outcome diagnostics", outcome.getDiagnostics().size(), is(1));
        assertThat("diagnostic level", outcome.getDiagnostics().get(0).getLevel(), is(Diagnostic.Level.WARNING));
        assertThat("outcome data", StringUtil.asString(outcome.getData()),
                is("Consumer system responded with OK: ok42-0-2\n"
                        + "Consumer system responded with WARNING: warning42-0-2\n"));
    }

    /* The entry status says OK, the diagnostic says otherwise. Counting this as succeeded
       would also advance the record's watermark for a version the consumer system objected
       to, so the diagnostic decides. */
    @Test
    void acceptedEntryCarryingAnErrorDiagnostic_itemIsReportedFailed() throws JobStoreServiceConnectorException {
        executeScriptResource("/completed_batches.sql");
        finalizeAll();

        ItemDeliveryResult result = reported(JOB_ID, 0, (short) 3);
        assertThat("verdict", result.status(), is(ItemDeliveryResult.Status.FAILED));
        assertThat("outcome status", result.chunkItem().getStatus(), is(ChunkItem.Status.FAILURE));
        assertThat("diagnostic level", result.chunkItem().getDiagnostics().get(0).getLevel(),
                is(Diagnostic.Level.FATAL));
    }

    @Test
    void rejectedEntry_itemIsReportedFailed() throws JobStoreServiceConnectorException {
        executeScriptResource("/completed_batches.sql");
        finalizeAll();

        ItemDeliveryResult result = reported(JOB_ID, 0, (short) 4);
        assertThat("verdict", result.status(), is(ItemDeliveryResult.Status.FAILED));
        assertThat("outcome status", result.chunkItem().getStatus(), is(ChunkItem.Status.FAILURE));
        assertThat("outcome data", StringUtil.asString(result.chunkItem().getData()),
                is("Consumer system responded with ERROR: error42-0-4\n"));
    }

    /* A partial success is failed, so the watermark is left where it was and a later version
       of the record is free to reach the consumer system. */
    @Test
    void itemWhoseRecordsFaredDifferently_isReportedFailedAsOneItem() throws JobStoreServiceConnectorException {
        executeScriptResource("/completed_batches.sql");
        finalizeAll();

        ItemDeliveryResult result = reported(JOB_ID, 0, (short) 5);
        assertThat("verdict", result.status(), is(ItemDeliveryResult.Status.FAILED));

        ChunkItem outcome = result.chunkItem();
        assertThat("one outcome for the whole item", outcome.getId(), is(5L));
        assertThat("outcome status", outcome.getStatus(), is(ChunkItem.Status.FAILURE));
        assertThat("every record's diagnostics", outcome.getDiagnostics().size(), is(2));
        assertThat("1st diagnostic", outcome.getDiagnostics().get(0).getMessage(), is("error42-0-5a"));
        assertThat("2nd diagnostic", outcome.getDiagnostics().get(1).getMessage(), is("error42-0-5b"));
        assertThat("every record's answer", StringUtil.asString(outcome.getData()),
                is("Consumer system responded with ERROR: error42-0-5a\n"
                        + "Consumer system responded with ERROR: error42-0-5b\n"
                        + "Consumer system responded with OK: ok42-0-5c\n"));
    }

    @Test
    void itemWithoutARecordKey_isReportedWithoutOne() throws JobStoreServiceConnectorException {
        executeScriptResource("/batch_without_record_key.sql");

        assertThat("batch was finalized", finalizer().finalizeNextCompletedBatch(), is(true));

        ItemDeliveryResult result = reported(JOB_ID, 0, (short) 9);
        assertThat("verdict", result.status(), is(ItemDeliveryResult.Status.DELIVERED));
        assertThat("no watermark row to advance", result.recordKey(), is(nullValue()));
        assertThat("watermark row sink id", result.sinkId(), is(15L));
    }

    /* Finishing with a batch releases its record, and the version waiting on that record has
       to be staged in the same transaction, or nothing else ever releases it. */
    @Test
    void completedBatchWithAHeldVersion_theHeldVersionIsStagedInItsPlace() {
        executeScriptResource("/completed_batch_with_held_version.sql");
        holdVersion(43, new AddiRecord(
                "<referenceData><info submitter=\"424242\"/></referenceData>".getBytes(), "held".getBytes()));

        assertThat("batch was finalized", finalizer().finalizeNextCompletedBatch(), is(true));

        assertThat("nothing left held", heldItemCount(), is(0L));
        Batch staged = onlyBatch();
        assertThat("the held version is staged", staged.getName(), is("15-870970:1-43-0-1"));
        List<BatchEntry> entries = entriesOf(staged);
        assertThat("its records are staged with it", entries.size(), is(1));
        assertThat("its content", StringUtil.asString(entries.get(0).getContent()), is("held"));
        assertThat("its tracking id", entries.get(0).getTrackingId(), is("43-0-1"));
    }

    /* A failed item whose record collapsed earlier versions is stuck at whatever was last
       delivered, and the outcome is where that is visible without piecing it together from
       other jobs' items. */
    @Test
    void itemThatCollapsedEarlierVersions_theOutcomeNamesWhatWasSkipped()
            throws JobStoreServiceConnectorException {
        executeScriptResource("/completed_batch_with_collapsed_versions.sql");

        assertThat("batch was finalized", finalizer().finalizeNextCompletedBatch(), is(true));

        ItemDeliveryResult result = reported(44, 0, (short) 1);
        assertThat("verdict", result.status(), is(ItemDeliveryResult.Status.FAILED));
        assertThat("outcome names what was skipped", StringUtil.asString(result.chunkItem().getData()),
                is("Consumer system responded with ERROR: error44-0-1\n"
                        + "2 version(s) of this record were skipped to arrive at this one, from version 42/0/1\n"));
    }

    @Test
    void everyCompletedBatchIsFinalized() throws JobStoreServiceConnectorException {
        executeScriptResource("/completed_batches.sql");

        assertThat("number of batches finalized", finalizeAll(), is(5));

        verify(jobStoreServiceConnector, atLeastOnce())
                .addItemDelivered(any(), anyInt(), anyInt(), anyShort());
        assertThat("nothing left", finalizer().finalizeNextCompletedBatch(), is(false));
    }

    /* Staging the held version while a consumer is midway through replacing it would have the
       consumer report as superseded a version already on its way to the target. */
    @Test
    void finalizingWaitsForWhoeverElseHoldsTheRecord() throws Exception {
        executeScriptResource("/completed_batch_with_held_version.sql");
        holdVersion(43, new AddiRecord(
                "<referenceData><info submitter=\"424242\"/></referenceData>".getBytes(), "held".getBytes()));
        EntityManager holder = entityManagerFactory.createEntityManager();
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            holder.getTransaction().begin();
            RecordLock.acquire(holder, 15L, "870970:1");

            Future<Boolean> finalizing = executor.submit(() -> finalizer().finalizeNextCompletedBatch());

            assertThrows(TimeoutException.class, () -> finalizing.get(2, TimeUnit.SECONDS));
            assertThat("nothing reported while the record is held", heldItemCount(), is(1L));

            holder.getTransaction().commit();

            assertThat("finalized once the record is released", finalizing.get(20, TimeUnit.SECONDS), is(true));
            assertThat("and the held version was staged", heldItemCount(), is(0L));
        } finally {
            executor.shutdownNow();
            if (holder.getTransaction().isActive()) {
                holder.getTransaction().rollback();
            }
            holder.close();
        }
    }

    @Test
    void isUpTest() {
        ScheduledBatchFinalizer batchFinalizerBean = new MockScheduledBatchFinalizer(Instant.now()
                .minus(SinkConfig.FINALIZER_LIVENESS_THRESHOLD.asDuration()).plus(Duration.ofSeconds(1)));
        assertThat("Bean should be up", !batchFinalizerBean.isDown());
    }

    @Test
    void isDownTest() {
        ScheduledBatchFinalizer batchFinalizerBean = new MockScheduledBatchFinalizer(Instant.now()
                .minus(SinkConfig.FINALIZER_LIVENESS_THRESHOLD.asDuration()).minus(Duration.ofSeconds(1)));
        assertThat("Bean should be down", batchFinalizerBean.isDown());
    }

    private static final int JOB_ID = 42;

    private int finalizeAll() {
        BatchFinalizer batchFinalizer = finalizer();
        int finalized = 0;
        while (batchFinalizer.finalizeNextCompletedBatch()) {
            finalized++;
        }
        return finalized;
    }

    private ItemDeliveryResult reported(int jobId, int chunkId, short itemId)
            throws JobStoreServiceConnectorException {
        ArgumentCaptor<ItemDeliveryResult> captor = ArgumentCaptor.forClass(ItemDeliveryResult.class);
        verify(jobStoreServiceConnector, atLeastOnce())
                .addItemDelivered(captor.capture(), anyInt(), anyInt(), anyShort());
        List<ItemDeliveryResult> results = captor.getAllValues();
        return results.stream()
                .filter(result -> result.chunkItem().getId() == itemId)
                .findFirst()
                .orElseThrow(() -> new AssertionError("no result reported for item " + jobId + "/" + chunkId + "/" + itemId));
    }

    private void holdVersion(int jobId, AddiRecord records) {
        EntityManager entityManager = entityManagerFactory.createEntityManager();
        try {
            entityManager.getTransaction().begin();
            entityManager.createNativeQuery("INSERT INTO held_item"
                            + " (sink_id, record_key, job_id, chunk_id, item_id, tracking_id, priority, payload)"
                            + " VALUES (15, '870970:1', ?1, 0, 1, ?2, 4, ?3)")
                    .setParameter(1, jobId)
                    .setParameter(2, jobId + "-0-1")
                    .setParameter(3, records.getBytes())
                    .executeUpdate();
            entityManager.getTransaction().commit();
        } finally {
            entityManager.close();
        }
    }

    private long heldItemCount() {
        EntityManager entityManager = entityManagerFactory.createEntityManager();
        try {
            return ((Number) entityManager
                    .createNativeQuery("SELECT count(*) FROM held_item")
                    .getSingleResult()).longValue();
        } finally {
            entityManager.close();
        }
    }

    private Batch onlyBatch() {
        EntityManager entityManager = entityManagerFactory.createEntityManager();
        try {
            return (Batch) entityManager
                    .createNativeQuery("SELECT * FROM batch ORDER BY id", Batch.class)
                    .getResultList()
                    .get(0);
        } finally {
            entityManager.close();
        }
    }

    @SuppressWarnings("unchecked")
    private List<BatchEntry> entriesOf(Batch batch) {
        EntityManager entityManager = entityManagerFactory.createEntityManager();
        try {
            return (List<BatchEntry>) entityManager
                    .createNamedQuery(BatchEntry.GET_BATCH_ENTRIES_QUERY_NAME)
                    .setParameter(1, batch.getId())
                    .setHint("eclipselink.refresh", true)
                    .getResultList();
        } finally {
            entityManager.close();
        }
    }

    private BatchFinalizer finalizer() {
        return new BatchFinalizer(entityManagerFactory, jobStoreServiceConnector, BatchExchangeMessageConsumer.fqn());
    }

    public static class MockScheduledBatchFinalizer extends ScheduledBatchFinalizer {
        Instant lastRun;

        public MockScheduledBatchFinalizer(Instant lastRun) {
            super(new ServiceHub.Builder()
                    .withJobStoreServiceConnector(mock(JobStoreServiceConnector.class))
                    .withZombieWatch(mock(ZombieWatch.class)).test(), null);
            this.lastRun = lastRun;
        }

        @Override
        protected Instant getLastRun() {
            return lastRun;
        }
    }
}
