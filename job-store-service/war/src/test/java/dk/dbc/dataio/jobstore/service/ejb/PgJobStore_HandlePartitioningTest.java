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

package dk.dbc.dataio.jobstore.service.ejb;

import dk.dbc.dataio.commons.types.Diagnostic;
import dk.dbc.dataio.commons.types.Sink;
import dk.dbc.dataio.commons.utils.test.model.SinkBuilder;
import dk.dbc.dataio.filestore.service.connector.FileStoreServiceConnectorException;
import dk.dbc.dataio.jobstore.service.entity.JobEntity;
import dk.dbc.dataio.jobstore.service.entity.SinkCacheEntity;
import dk.dbc.dataio.jobstore.service.param.PartitioningParam;
import dk.dbc.dataio.jobstore.service.partitioner.DefaultXmlDataPartitioner;
import dk.dbc.dataio.jobstore.types.JobInfoSnapshot;
import dk.dbc.dataio.jobstore.types.JobStoreException;
import dk.dbc.dataio.jobstore.types.State;
import org.junit.Before;
import org.junit.Test;
import types.TestablePartitioningParamBuilder;

import javax.persistence.LockModeType;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static dk.dbc.dataio.commons.types.Diagnostic.Level.FATAL;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

public class PgJobStore_HandlePartitioningTest extends PgJobStoreBaseTest {
    private PgJobStore pgJobStore;
    private JobEntity jobEntity;
    private TestablePartitioningParamBuilder partitioningParamBuilder;

    @Before
    public void createPgJobStore() {
        pgJobStore = newPgJobStore(newPgJobStoreReposity());
        pgJobStore.jobQueueRepository = newJobQueueRepository();
    }

    @Before
    public void createPartitioningParamBuilder() {
        final Sink sink = new SinkBuilder().build();
        final SinkCacheEntity sinkCacheEntity = SinkCacheEntity.create(sink);
        jobEntity = getJobEntity(0);
        jobEntity.setCachedSink(sinkCacheEntity);

        partitioningParamBuilder = new TestablePartitioningParamBuilder()
                .setJobEntity(jobEntity);

        setupMocks();
    }

    private void setupMocks() {
        when(entityManager.find(eq(JobEntity.class), anyInt(), eq(LockModeType.PESSIMISTIC_WRITE)))
                .thenReturn(jobEntity);
    }

    @Test
    public void partition_byteSizeNotFound_returnsSnapshotWithJobMarkedAsCompletedAndDiagnosticsAdded() throws JobStoreException, FileStoreServiceConnectorException {
        // Setup preconditions
        when(mockedFileStoreServiceConnector.getByteSize(anyString())).thenThrow(fileStoreUnexpectedException);
        final PartitioningParam param = partitioningParamBuilder.build();

        // Subject Under Test
        final JobInfoSnapshot jobInfoSnapshot = pgJobStore.partition(param);

        // Verify
        assertThat("JobInfoSnapshot", jobInfoSnapshot, is(notNullValue()));
        assertThat("Fatal error occurred", jobInfoSnapshot.hasFatalError(), is(true));

        final Diagnostic diagnostic = param.getJobEntity().getState().getDiagnostics().get(0);
        final String diagnosticsStacktrace = diagnostic.getStacktrace();
        assertTrue(!param.getJobEntity().getState().getDiagnostics().isEmpty());
        assertThat("Diagnostics level", diagnostic.getLevel(), is(FATAL));
        assertThat("Diagnostics stacktrace", diagnosticsStacktrace, containsString("Caused by: dk.dbc.dataio.filestore.service.connector.FileStoreServiceConnectorUnexpectedStatusCodeException: unexpected status code"));
        assertThat("Diagnostics stacktrace", diagnosticsStacktrace, containsString("dk.dbc.dataio.jobstore.types.JobStoreException: Could not retrieve byte size"));
    }

    @Test
    public void partition_differentByteSize_returnsSnapshotWithJobMarkedAsCompletedAndDiagnosticsAdded() throws JobStoreException, FileStoreServiceConnectorException {
        // Setup preconditions
        when(mockedFileStoreServiceConnector.getByteSize(anyString())).thenReturn(99999L);
        final PartitioningParam param = partitioningParamBuilder.build();

        // Subject Under Test
        final JobInfoSnapshot jobInfoSnapshot = pgJobStore.partition(param);

        // Verify
        assertThat("JobInfoSnapshot", jobInfoSnapshot, is(notNullValue()));
        assertThat("Fatal error occurred", jobInfoSnapshot.hasFatalError(), is(true));

        final Diagnostic diagnostic = param.getJobEntity().getState().getDiagnostics().get(0);
        final String diagnosticsMessage = diagnostic.getMessage();
        assertTrue(!param.getJobEntity().getState().getDiagnostics().isEmpty());
        assertThat("Diagnostics level", diagnostic.getLevel(), is(FATAL));
        assertThat("Diagnostics message", diagnosticsMessage, containsString("DataPartitioner.byteSize was: 307"));
        assertThat("Diagnostics message", diagnosticsMessage, containsString("FileStore.byteSize was: 99999"));
    }

    @Test
    public void partition_allArgsAreValid_returnsJobInfoSnapshot() throws JobStoreException, FileStoreServiceConnectorException {
        // Setup preconditions
        when(mockedFileStoreServiceConnector.getByteSize(anyString())).thenReturn(307L);
        final PartitioningParam param = partitioningParamBuilder.build();

        // Subject Under Test
        final JobInfoSnapshot jobInfoSnapshot = pgJobStore.partition(param);

        // Verify
        assertThat("Returned JobInfoSnapshot", jobInfoSnapshot, is(notNullValue()));
        assertThat("Fatal error did not occur", jobInfoSnapshot.hasFatalError(), is(false));
        assertThat("Number of chunks created", jobInfoSnapshot.getNumberOfChunks(), is(EXPECTED_NUMBER_OF_CHUNKS));
        assertThat("Number of items created", jobInfoSnapshot.getNumberOfItems(), is(EXPECTED_NUMBER_OF_ITEMS));
        assertThat("Partitioning phase endDate set", jobInfoSnapshot.getState().getPhase(State.Phase.PARTITIONING).getEndDate(), is(notNullValue()));
        assertThat("Time of completion not set", jobInfoSnapshot.getTimeOfCompletion(), is(nullValue()));
        assertThat("JobInfoSnapshot.State.Diagnostics", jobInfoSnapshot.getState().getDiagnostics(), is(param.getDiagnostics()));
    }

    @Test
    public void partition_noRecords_returnsJobInfoSnapshot() throws JobStoreException, FileStoreServiceConnectorException {
        // Setup preconditions
        final byte[] records = "<records></records>".getBytes(StandardCharsets.UTF_8);
        final InputStream dataFileInputStream = new ByteArrayInputStream(records);
        final PartitioningParam param = partitioningParamBuilder.setDataPartitioner(DefaultXmlDataPartitioner.newInstance(dataFileInputStream, StandardCharsets.UTF_8.name())).build();

        when(mockedFileStoreServiceConnector.getByteSize(anyString())).thenReturn(Long.valueOf(records.length).longValue());

        // Subject Under Test
        final JobInfoSnapshot jobInfoSnapshot = pgJobStore.partition(param);

        // Verify
        assertThat("Returned JobInfoSnapshot", jobInfoSnapshot, is(notNullValue()));
        assertThat("Fatal error did not occur", jobInfoSnapshot.hasFatalError(), is(false));

        assertThat("Number of chunks created", jobInfoSnapshot.getNumberOfChunks(), is(0));
        assertThat("Number of items created", jobInfoSnapshot.getNumberOfItems(), is(0));
        assertThat("Partitioning phase endDate set", jobInfoSnapshot.getState().getPhase(State.Phase.PARTITIONING).getEndDate(), is(notNullValue()));
        assertThat("Processing phase endDate set", jobInfoSnapshot.getState().getPhase(State.Phase.PROCESSING).getEndDate(), is(notNullValue()));
        assertThat("Delivering phase endDate set", jobInfoSnapshot.getState().getPhase(State.Phase.DELIVERING).getEndDate(), is(notNullValue()));
        assertThat("Time of completion set", jobInfoSnapshot.getTimeOfCompletion(), is(notNullValue()));
    }
}