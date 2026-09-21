package dk.dbc.dataio.sink.batchexchange;

import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.types.Diagnostic;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnector;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnectorException;
import dk.dbc.dataio.commons.utils.lang.StringUtil;
import dk.dbc.dataio.jobstore.types.ItemDeliveryResult;
import dk.dbc.dataio.jse.artemis.common.service.ServiceHub;
import dk.dbc.dataio.jse.artemis.common.service.ZombieWatch;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

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
    void batchNameDoesNotNameAnItem_theBatchIsDiscardedWithoutBeingReported()
            throws JobStoreServiceConnectorException {
        executeScriptResource("/invalid_named_batch.sql");

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

    @Test
    void everyCompletedBatchIsFinalized() throws JobStoreServiceConnectorException {
        executeScriptResource("/completed_batches.sql");

        assertThat("number of batches finalized", finalizeAll(), is(5));

        verify(jobStoreServiceConnector, atLeastOnce())
                .addItemDelivered(any(), anyInt(), anyInt(), anyShort());
        assertThat("nothing left", finalizer().finalizeNextCompletedBatch(), is(false));
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
