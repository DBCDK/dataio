package dk.dbc.dataio.sink.batchexchange;

import dk.dbc.dataio.commons.types.Chunk;
import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.types.Diagnostic;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnector;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnectorException;
import dk.dbc.dataio.commons.utils.lang.StringUtil;
import dk.dbc.dataio.jse.artemis.common.service.ServiceHub;
import dk.dbc.dataio.jse.artemis.common.service.ZombieWatch;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.time.Duration;
import java.time.Instant;

import static dk.dbc.commons.testutil.Assert.assertThat;
import static dk.dbc.commons.testutil.Assert.isThrowing;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class BatchFinalizerIT extends IntegrationTest {
    final private JobStoreServiceConnector jobStoreServiceConnector = mock(JobStoreServiceConnector.class);

    /*  When: no completed batch exists in the batch-exchange
     *  Then: finalizer returns false
     */
    @Test
    public void finalizeNextCompletedBatch_noBatchFound() {
        BatchFinalizer batchFinalizer = createBatchFinalizerBean();
        assertThat(batchFinalizer.finalizeNextCompletedBatch(), is(false));
    }

    /*  Given: a completed batch
     *   When: job-store upload throws
     *   Then: finalizer throws SinkException
     */
    @Test
    public void finalizeNextCompletedBatch_jobStoreUploadThrows() throws JobStoreServiceConnectorException {
        executeScriptResource("/completed_batch.sql");

        when(jobStoreServiceConnector.addChunkIgnoreDuplicates(any(Chunk.class), anyInt(), anyLong()))
                .thenThrow(new JobStoreServiceConnectorException("Died"));
        BatchFinalizer batchFinalizer = createBatchFinalizerBean();
        assertThat(batchFinalizer::finalizeNextCompletedBatch, isThrowing(RuntimeException.class));
    }

    /*  Given: a completed batch
     *   When: batch name does not match [JOB_ID]-[CHUNK_ID] pattern
     *   Then: finalizer throws IllegalArgumentException
     */
    @Test
    public void finalizeNextCompletedBatch_batchIsWronglyNamed() {
        executeScriptResource("/invalid_named_batch.sql");
        BatchFinalizer batchFinalizer = createBatchFinalizerBean();
        assertThat(batchFinalizer::finalizeNextCompletedBatch, isThrowing(IllegalArgumentException.class));
    }

    /*  Given: a completed batch
     *   When: finalized
     *   Then: the corresponding chunk is uploaded to the job-store
     */
    @Test
    public void finalizeNextCompletedBatch() throws JobStoreServiceConnectorException {
        executeScriptResource("/completed_batch.sql");

        BatchFinalizer batchFinalizer = createBatchFinalizerBean();
        assertThat("batch was finalized", batchFinalizer.finalizeNextCompletedBatch(), is(true));

        ArgumentCaptor<Chunk> chunkArgumentCaptor = ArgumentCaptor.forClass(Chunk.class);
        verify(jobStoreServiceConnector).addChunkIgnoreDuplicates(chunkArgumentCaptor.capture(), anyInt(), anyLong());

        Chunk chunk = chunkArgumentCaptor.getValue();
        assertThat("chunk job ID", chunk.getJobId(), is(42));
        assertThat("chunk ID", chunk.getChunkId(), is(0L));
        assertThat("chunk size", chunk.size(), is(5));

        ChunkItem chunkItem = chunk.getItems().get(0);
        assertThat("1st chunkItem status", chunkItem.getStatus(), is(ChunkItem.Status.IGNORE));
        assertThat("1st chunkItem trackingId", chunkItem.getTrackingId(), is("42-0-1"));
        assertThat("1st chunkItem diagnostics", chunkItem.getDiagnostics(), is(nullValue()));
        assertThat("1st chunkItem data", StringUtil.asString(chunkItem.getData()),
                is("Consumer system responded with OK: ok42-0-1\n"));

        chunkItem = chunk.getItems().get(1);
        assertThat("2nd chunkItem status", chunkItem.getStatus(), is(ChunkItem.Status.SUCCESS));
        assertThat("2nd chunkItem trackingId", chunkItem.getTrackingId(), is("42-0-2"));
        assertThat("2nd chunkItem diagnostics", chunkItem.getDiagnostics().size(), is(1));
        assertThat("2nd chunkItem 1st diagnostic level", chunkItem.getDiagnostics().get(0).getLevel(), is(Diagnostic.Level.WARNING));
        assertThat("2nd chunkItem 1st diagnostic message", chunkItem.getDiagnostics().get(0).getMessage(), is("warning42-0-2"));
        assertThat("2nd chunkItem data", StringUtil.asString(chunkItem.getData()),
                is("Consumer system responded with OK: ok42-0-2\nConsumer system responded with WARNING: warning42-0-2\n"));

        chunkItem = chunk.getItems().get(2);
        assertThat("3rd chunkItem status", chunkItem.getStatus(), is(ChunkItem.Status.FAILURE));
        assertThat("3rd chunkItem trackingId", chunkItem.getTrackingId(), is("42-0-3"));
        assertThat("3rd chunkItem diagnostics", chunkItem.getDiagnostics().size(), is(1));
        assertThat("3rd chunkItem 1st diagnostic level", chunkItem.getDiagnostics().get(0).getLevel(), is(Diagnostic.Level.FATAL));
        assertThat("3rd chunkItem 1st diagnostic message", chunkItem.getDiagnostics().get(0).getMessage(), is("error42-0-3"));
        assertThat("3rd chunkItem data", StringUtil.asString(chunkItem.getData()),
                is("Consumer system responded with ERROR: error42-0-3\n"));

        chunkItem = chunk.getItems().get(3);
        assertThat("4th chunkItem status", chunkItem.getStatus(), is(ChunkItem.Status.FAILURE));
        assertThat("4th chunkItem trackingId", chunkItem.getTrackingId(), is("42-0-4"));
        assertThat("4th chunkItem diagnostics", chunkItem.getDiagnostics().size(), is(1));
        assertThat("4th chunkItem 1st diagnostic level", chunkItem.getDiagnostics().get(0).getLevel(), is(Diagnostic.Level.FATAL));
        assertThat("4th chunkItem 1st diagnostic message", chunkItem.getDiagnostics().get(0).getMessage(), is("error42-0-4"));
        assertThat("4th chunkItem data", StringUtil.asString(chunkItem.getData()),
                is("Consumer system responded with ERROR: error42-0-4\n"));

        chunkItem = chunk.getItems().get(4);
        assertThat("5th chunkItem status", chunkItem.getStatus(), is(ChunkItem.Status.FAILURE));
        assertThat("5th chunkItem trackingId", chunkItem.getTrackingId(), is("42-0-5"));
        assertThat("5th chunkItem diagnostics", chunkItem.getDiagnostics().size(), is(2));
        assertThat("5th chunkItem 1st diagnostic level", chunkItem.getDiagnostics().get(0).getLevel(), is(Diagnostic.Level.FATAL));
        assertThat("5th chunkItem 1st diagnostic message", chunkItem.getDiagnostics().get(0).getMessage(), is("error42-0-5a"));
        assertThat("5th chunkItem 2nd diagnostic level", chunkItem.getDiagnostics().get(1).getLevel(), is(Diagnostic.Level.FATAL));
        assertThat("5th chunkItem 2nd diagnostic message", chunkItem.getDiagnostics().get(1).getMessage(), is("error42-0-5b"));
        assertThat("5th chunkItem data", StringUtil.asString(chunkItem.getData()),
                is("Consumer system responded with ERROR: error42-0-5a\nConsumer system responded with ERROR: error42-0-5b\nConsumer system responded with OK: ok42-0-5c\n"));
    }

    @Test
    public void isUpTest() {
        ScheduledBatchFinalizer batchFinalizerBean = new MockScheduledBatchFinalizer(Instant.now().minus(SinkConfig.FINALIZER_LIVENESS_THRESHOLD.asDuration()).plus(Duration.ofSeconds(1)));
        assertThat("Bean should be up", !batchFinalizerBean.isDown());
    }

    @Test
    public void isDownTest() {
        ScheduledBatchFinalizer batchFinalizerBean = new MockScheduledBatchFinalizer(Instant.now().minus(SinkConfig.FINALIZER_LIVENESS_THRESHOLD.asDuration()).minus(Duration.ofSeconds(1)));
        assertThat("Bean should be down", batchFinalizerBean.isDown());
    }

    private BatchFinalizer createBatchFinalizerBean() {
        return new BatchFinalizer(entityManager, jobStoreServiceConnector);
    }

    public static class MockScheduledBatchFinalizer extends ScheduledBatchFinalizer {
        Instant lastRun;

        public MockScheduledBatchFinalizer(Instant lastRun) {
            super(new ServiceHub.Builder().withJobStoreServiceConnector(mock(JobStoreServiceConnector.class)).withZombieWatch(mock(ZombieWatch.class)).test(), null);
            this.lastRun = lastRun;
        }

        @Override
        protected Instant getLastRun() {
            return lastRun;
        }
    }
}
