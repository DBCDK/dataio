/*
 * DataIO - Data IO
 * Copyright (C) 2015 Dansk Bibliotekscenter a/s, Tempovej 7-11, DK-2750 Ballerup,
 * Denmark. CVR: 15149043
 *
 * This file is part of DataIO.
 *
 * DataIO is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * DataIO is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with DataIO.  If not, see <http://www.gnu.org/licenses/>.
 */

package dk.dbc.dataio.sink.batchexchange;

import dk.dbc.dataio.commons.types.Chunk;
import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.types.Diagnostic;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnector;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnectorException;
import dk.dbc.dataio.commons.utils.jobstore.ejb.JobStoreServiceConnectorBean;
import dk.dbc.dataio.commons.utils.lang.StringUtil;
import dk.dbc.dataio.sink.types.SinkException;
import org.eclipse.microprofile.metrics.Counter;
import org.eclipse.microprofile.metrics.Metadata;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.SimpleTimer;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.time.Duration;

import static dk.dbc.commons.testutil.Assert.assertThat;
import static dk.dbc.commons.testutil.Assert.isThrowing;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class BatchFinalizerBeanIT extends IntegrationTest {
    final private JobStoreServiceConnectorBean jobStoreServiceConnectorBean = mock(JobStoreServiceConnectorBean.class);
    final private JobStoreServiceConnector jobStoreServiceConnector = mock(JobStoreServiceConnector.class);
    final private MetricRegistry metricRegistry = mock(MetricRegistry.class);
    final private SimpleTimer batchFinalizerTimer = mock(SimpleTimer.class);
    final private Counter batchFinalizerCounter = mock(Counter.class);

    @Before
    public void setupMocks() {
        when(jobStoreServiceConnectorBean.getConnector()).thenReturn(jobStoreServiceConnector);
        when(metricRegistry.simpleTimer(any(Metadata.class))).thenReturn(batchFinalizerTimer);
        doNothing().when(batchFinalizerTimer).update(any(Duration.class));
        when(metricRegistry.counter(any(Metadata.class))).thenReturn(batchFinalizerCounter);
        doNothing().when(batchFinalizerCounter).inc();
    }

    /*  When: no completed batch exists in the batch-exchange
     *  Then: finalizer returns false
     */
    @Test
    public void finalizeNextCompletedBatch_noBatchFound() throws SinkException {
        final BatchFinalizerBean batchFinalizerBean = createBatchFinalizerBean();
        assertThat(batchFinalizerBean.finalizeNextCompletedBatch(), is(false));
    }

    /*  Given: a completed batch
     *   When: job-store upload throws
     *   Then: finalizer throws SinkException
     */
    @Test
    public void finalizeNextCompletedBatch_jobStoreUploadThrows() throws JobStoreServiceConnectorException {
        executeScriptResource("/completed_batch.sql");

        when(jobStoreServiceConnector.addChunkIgnoreDuplicates(any(Chunk.class), anyLong(), anyLong()))
                .thenThrow(new JobStoreServiceConnectorException("Died"));
        final BatchFinalizerBean batchFinalizerBean = createBatchFinalizerBean();
        assertThat(batchFinalizerBean::finalizeNextCompletedBatch, isThrowing(SinkException.class));
    }

    /*  Given: a completed batch
     *   When: batch name does not match [JOB_ID]-[CHUNK_ID] pattern
     *   Then: finalizer throws IllegalArgumentException
     */
    @Test
    public void finalizeNextCompletedBatch_batchIsWronglyNamed() {
        executeScriptResource("/invalid_named_batch.sql");
        final BatchFinalizerBean batchFinalizerBean = createBatchFinalizerBean();
        assertThat(batchFinalizerBean::finalizeNextCompletedBatch, isThrowing(IllegalArgumentException.class));
    }

    /*  Given: a completed batch
     *   When: finalized
     *   Then: the corresponding chunk is uploaded to the job-store
     */
    @Test
    public void finalizeNextCompletedBatch() throws JobStoreServiceConnectorException, SinkException {
        executeScriptResource("/completed_batch.sql");

        final BatchFinalizerBean batchFinalizerBean = createBatchFinalizerBean();
        assertThat("batch was finalized", batchFinalizerBean.finalizeNextCompletedBatch(), is(true));

        final ArgumentCaptor<Chunk> chunkArgumentCaptor = ArgumentCaptor.forClass(Chunk.class);
        verify(jobStoreServiceConnector).addChunkIgnoreDuplicates(chunkArgumentCaptor.capture(), anyLong(), anyLong());

        final Chunk chunk = chunkArgumentCaptor.getValue();
        assertThat("chunk job ID", chunk.getJobId(), is(42L));
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

    private BatchFinalizerBean createBatchFinalizerBean() {
        final BatchFinalizerBean bean = new BatchFinalizerBean();
        bean.entityManager = entityManager;
        bean.jobStoreServiceConnectorBean = jobStoreServiceConnectorBean;
        bean.metricRegistry = metricRegistry;
        return bean;
    }
}