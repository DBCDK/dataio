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
import dk.dbc.dataio.commons.types.Flow;
import dk.dbc.dataio.commons.types.FlowBinder;
import dk.dbc.dataio.commons.types.Sink;
import dk.dbc.dataio.commons.types.SinkContent;
import dk.dbc.dataio.commons.types.Submitter;
import dk.dbc.dataio.filestore.service.connector.FileStoreServiceConnectorException;
import dk.dbc.dataio.jobstore.service.entity.JobEntity;
import dk.dbc.dataio.jobstore.service.entity.JobQueueEntity;
import dk.dbc.dataio.jobstore.test.types.FlowStoreReferencesBuilder;
import dk.dbc.dataio.jobstore.types.FlowStoreReference;
import dk.dbc.dataio.jobstore.types.FlowStoreReferences;
import dk.dbc.dataio.jobstore.types.JobInfoSnapshot;
import dk.dbc.dataio.jobstore.types.JobStoreException;
import dk.dbc.dataio.jobstore.types.State;
import org.junit.Test;
import types.TestableJobEntity;
import types.TestableJobEntityBuilder;
import types.TestablePartitioningParam;
import types.TestablePartitioningParamBuilder;

import javax.persistence.LockModeType;
import javax.persistence.Query;
import java.util.Collections;

import static dk.dbc.dataio.commons.types.Diagnostic.Level.FATAL;
import static dk.dbc.dataio.commons.types.RecordSplitterConstants.RecordSplitter;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class PgJobStore_HandlePartitioningTest extends PgJobStoreBaseTest {

    private static final String ERROR_MESSAGE = "Error Message";
    private static final int EXPECTED_NUMBER_OF_CHUNKS = 2;

    @Test
    public void handlePartitioning_byteSizeNotFound_returnsSnapshotWithJobMarkedAsCompletedAndDiagnosticsAdded() throws JobStoreException, FileStoreServiceConnectorException {

        // Setup preconditions
        final PgJobStore pgJobStore = newPgJobStore(newPgJobStoreReposity());
        final TestablePartitioningParam testablePartitioningParam = new TestablePartitioningParamBuilder().build();

        when(entityManager.find(eq(JobEntity.class), anyInt(), eq(LockModeType.PESSIMISTIC_WRITE))).thenReturn(testablePartitioningParam.getJobEntity());
        when(testablePartitioningParam.getJobEntity().getCachedSink().getSink()).thenReturn(mock(Sink.class));
        when(mockedFileStoreServiceConnector.getByteSize(anyString())).thenThrow(fileStoreUnexpectedException);

        // Subject Under Test
        final JobInfoSnapshot jobInfoSnapshot = pgJobStore.handlePartitioning(testablePartitioningParam);

        // Verify
        assertThat("JobInfoSnapshot", jobInfoSnapshot, is(notNullValue()));
        assertThat("Fatal error occurred", jobInfoSnapshot.hasFatalError(), is(true));

        final Diagnostic diagnostic = testablePartitioningParam.getJobEntity().getState().getDiagnostics().get(0);
        final String diagnosticsStacktrace = diagnostic.getStacktrace();
        assertTrue(!testablePartitioningParam.getJobEntity().getState().getDiagnostics().isEmpty());
        assertThat("Diagnostics level", diagnostic.getLevel(), is(FATAL));
        assertThat("Diagnostics stacktrace", diagnosticsStacktrace, containsString("Caused by: dk.dbc.dataio.filestore.service.connector.FileStoreServiceConnectorUnexpectedStatusCodeException: unexpected status code"));
        assertThat("Diagnostics stacktrace", diagnosticsStacktrace, containsString("dk.dbc.dataio.jobstore.types.JobStoreException: Could not retrieve byte size"));
    }

    @Test
    public void handlePartitioning_differentByteSize_returnsSnapshotWithJobMarkedAsCompletedAndDiagnosticsAdded() throws JobStoreException, FileStoreServiceConnectorException {

        // Setup preconditions
        final PgJobStore pgJobStore = newPgJobStore(newPgJobStoreReposity());
        pgJobStore.jobQueueRepository = newJobQueueRepository();

        final TestablePartitioningParam testablePartitioningParam = new TestablePartitioningParamBuilder().build();

        when(entityManager.find(eq(JobEntity.class), anyInt(), eq(LockModeType.PESSIMISTIC_WRITE))).thenReturn(testablePartitioningParam.getJobEntity());
        when(testablePartitioningParam.getJobEntity().getCachedSink().getSink()).thenReturn(mock(Sink.class));
        when(mockedFileStoreServiceConnector.getByteSize(anyString())).thenReturn(99999l);

        setupMockedSink();
        setupMockedJobQueueNamedQueryForFindByJob();

        // Subject Under Test
        final JobInfoSnapshot jobInfoSnapshot = pgJobStore.handlePartitioning(testablePartitioningParam);

        // Verify
        assertThat("JobInfoSnapshot", jobInfoSnapshot, is(notNullValue()));
        assertThat("Fatal error occurred", jobInfoSnapshot.hasFatalError(), is(true));

        final Diagnostic diagnostic = testablePartitioningParam.getJobEntity().getState().getDiagnostics().get(0);
        final String diagnosticsMessage = diagnostic.getMessage();
        assertTrue(!testablePartitioningParam.getJobEntity().getState().getDiagnostics().isEmpty());
        assertThat("Diagnostics level", diagnostic.getLevel(), is(FATAL));
        assertThat("Diagnostics message", diagnosticsMessage, containsString("DataPartitioner.byteSize was: 307"));
        assertThat("Diagnostics message", diagnosticsMessage, containsString("FileStore.byteSize was: 99999"));
    }

    @Test
    public void handlePartitioning_diagnosticWithLevelFatalFound_returnsJobInformationSnapshotWithJobMarkedAsCompleted() throws JobStoreException {

        // Setup preconditions
        final PgJobStore pgJobStore = newPgJobStore(newPgJobStoreReposity());

        final MockedAddJobParam mockedAddJobParam = new MockedAddJobParam();
        mockedAddJobParam.setDiagnostics(Collections.singletonList(new Diagnostic(Diagnostic.Level.FATAL, ERROR_MESSAGE)));
        final JobEntity jobEntity = pgJobStore.jobStoreRepository.createJobEntity(mockedAddJobParam);

        final TestablePartitioningParam testablePartitioningParam = new TestablePartitioningParamBuilder().setJobEntity(jobEntity).build();

        when(entityManager.find(eq(JobEntity.class), anyInt(), eq(LockModeType.PESSIMISTIC_WRITE))).thenReturn(testablePartitioningParam.getJobEntity());

        // Subject Under Test
        final JobInfoSnapshot jobInfoSnapshot = pgJobStore.handlePartitioning(testablePartitioningParam);

        // Verify
        assertThat("JobInfoSnapshot", jobInfoSnapshot, is(notNullValue()));
        assertThat("Fatal error occurred", jobInfoSnapshot.hasFatalError(), is(true));
        assertThat("JobInfoSnapshot.timeOfCompletion", jobInfoSnapshot.getTimeOfCompletion(), is(notNullValue()));
        assertThat("State.Phase.PARTITIONING.beginDate", jobInfoSnapshot.getState().getPhase(State.Phase.PARTITIONING).getBeginDate(), is(nullValue()));
        assertThat("JobInfoSnapshot.State.Diagnostics", jobInfoSnapshot.getState().getDiagnostics(), is(mockedAddJobParam.getDiagnostics()));
        assertThat("JobInfoSnapshot.FlowStoreReferences", jobInfoSnapshot.getFlowStoreReferences(), is(mockedAddJobParam.getFlowStoreReferences()));
    }

    @Test
    public void handlePartitioning_allArgsAreValid_returnsJobInformationSnapshot() throws JobStoreException, FileStoreServiceConnectorException {

        // Setup preconditions
        final PgJobStore pgJobStore = newPgJobStore(newPgJobStoreReposity());
        pgJobStore.jobQueueRepository = newJobQueueRepository();

        final TestablePartitioningParam testablePartitioningParam = new TestablePartitioningParamBuilder().build();
        when(testablePartitioningParam.getJobEntity().getCachedSink().getSink()).thenReturn(mock(Sink.class));

        final TestableJobEntity jobEntity = new TestableJobEntityBuilder().setJobSpecification(testablePartitioningParam.getJobEntity().getSpecification()).build();
        when(entityManager.find(eq(JobEntity.class), anyInt(), eq(LockModeType.PESSIMISTIC_WRITE))).thenReturn(jobEntity);

        when(mockedFileStoreServiceConnector.getByteSize(anyString())).thenReturn(307L);

        setupMockedSink();
        setupMockedJobQueueNamedQueryForFindByJob();

        // Subject Under Test
        final JobInfoSnapshot jobInfoSnapshot = pgJobStore.handlePartitioning(testablePartitioningParam);

        // Verify
        assertThat("Returned JobInfoSnapshot", jobInfoSnapshot, is(notNullValue()));
        assertThat("Fatal error did not occur", jobInfoSnapshot.hasFatalError(), is(false));
        assertThat("Number of chunks created", jobInfoSnapshot.getNumberOfChunks(), is(EXPECTED_NUMBER_OF_CHUNKS));
        assertThat("Number of items created", jobInfoSnapshot.getNumberOfItems(), is(EXPECTED_NUMBER_OF_ITEMS));
        assertThat("Partitioning phase endDate set", jobInfoSnapshot.getState().getPhase(State.Phase.PARTITIONING).getEndDate(), is(notNullValue()));
        assertThat("Time of completion not set", jobInfoSnapshot.getTimeOfCompletion(), is(nullValue()));

        assertThat("JobInfoSnapshot.State.Diagnostics", jobInfoSnapshot.getState().getDiagnostics(), is(testablePartitioningParam.getDiagnostics()));
    }

    @Test
    public void handlePartitioning_diagnosticWithLevelWarningFound_returnsJobInformationSnapshot() throws JobStoreException, FileStoreServiceConnectorException {

        // Setup preconditions
        final PgJobStore pgJobStore = newPgJobStore(newPgJobStoreReposity());
        pgJobStore.jobQueueRepository = newJobQueueRepository();

        final TestablePartitioningParam testablePartitioningParam = new TestablePartitioningParamBuilder()
                .setDiagnostics(Collections.singletonList(new Diagnostic(Diagnostic.Level.WARNING, ERROR_MESSAGE)))
                .build();

        when(testablePartitioningParam.getJobEntity().getCachedSink().getSink()).thenReturn(mock(Sink.class));

        final State state = new State();
        state.getDiagnostics().addAll(testablePartitioningParam.getDiagnostics());

        final TestableJobEntity jobEntity = new TestableJobEntityBuilder().setState(state).build();

        when(entityManager.find(eq(JobEntity.class), anyInt(), eq(LockModeType.PESSIMISTIC_WRITE))).thenReturn(jobEntity);
        when(mockedFileStoreServiceConnector.getByteSize(anyString())).thenReturn(307l);

        setupMockedSink();
        setupMockedJobQueueNamedQueryForFindByJob();

        // Subject Under Test
        final JobInfoSnapshot jobInfoSnapshot = pgJobStore.handlePartitioning(testablePartitioningParam);

        // Verify
        assertThat("Returned JobInfoSnapshot", jobInfoSnapshot, is(notNullValue()));
        assertThat("Fatal error did not occur", jobInfoSnapshot.hasFatalError(), is(false));
        assertThat("Number of chunks created", jobInfoSnapshot.getNumberOfChunks(), is(EXPECTED_NUMBER_OF_CHUNKS));
        assertThat("Number of items created", jobInfoSnapshot.getNumberOfItems(), is(EXPECTED_NUMBER_OF_ITEMS));
        assertThat("Partitioning phase endDate set", jobInfoSnapshot.getState().getPhase(State.Phase.PARTITIONING).getEndDate(), is(notNullValue()));
        assertThat("Time of completion not set", jobInfoSnapshot.getTimeOfCompletion(), is(nullValue()));

        assertThat("JobInfoSnapshot.State.Diagnostics", jobInfoSnapshot.getState().getDiagnostics(), is(testablePartitioningParam.getDiagnostics()));
    }


    @Test
    public void handlePartitioning_sinkNotOccupied() throws FileStoreServiceConnectorException, JobStoreException {

        // Setup preconditions
        final PgJobStore pgJobStore = newPgJobStore();
        final TestablePartitioningParam testablePartitioningParam = new TestablePartitioningParamBuilder().build();
        when(testablePartitioningParam.getJobEntity().getCachedSink().getSink()).thenReturn(mock(Sink.class));

        final Sink EXPECTED_SINK = new Sink(5l, 1l, new SinkContent("TestSink", "TestResource", "TestDescription"));
        final TestableJobEntity jobEntity = new TestableJobEntityBuilder().setJobSpecification(testablePartitioningParam.getJobEntity().getSpecification()).build();

        when(jobEntity.getCachedSink().getSink()).thenReturn(EXPECTED_SINK);
        when(pgJobStore.jobStoreRepository.getExclusiveAccessFor(eq(JobEntity.class), anyInt())).thenReturn(jobEntity);
        setupMockedSink();
        setupMockedJobQueueNamedQueryForFindByJob();

        // Subject Under Test
        final JobInfoSnapshot jobInfoSnapshot = pgJobStore.handlePartitioning(testablePartitioningParam);

        // Verify
        assertThat("Sink not occupied!", jobInfoSnapshot, is(notNullValue()));
        verify(pgJobStore.jobQueueRepository).addJobToJobQueueInDatabase(anyLong(), any(JobEntity.class), anyBoolean(), any(RecordSplitter.class));
    }

    @Test
    public void handlePartitioning_sinkOccupied() throws FileStoreServiceConnectorException, JobStoreException {

        // Setup preconditions
        final PgJobStore pgJobStore = newPgJobStore();
        final TestablePartitioningParam testablePartitioningParam = new TestablePartitioningParamBuilder().build();
        when(testablePartitioningParam.getJobEntity().getCachedSink().getSink()).thenReturn(mock(Sink.class));
        when(pgJobStore.jobQueueRepository.addJobToJobQueueInDatabase(anyLong(), any(JobEntity.class), anyBoolean(), any(RecordSplitter.class))).thenReturn(OCCUPIED);

        // Subject Under Test
        final JobInfoSnapshot jobInfoSnapshot = pgJobStore.handlePartitioning(testablePartitioningParam);

        // Verify
        assertThat("Sink for job occupied!", jobInfoSnapshot, is(notNullValue()));
        verify(pgJobStore.jobQueueRepository).addJobToJobQueueInDatabase(anyLong(), any(JobEntity.class), anyBoolean(), any(RecordSplitter.class));
    }

    @Test
    public void handlePartitioning_updateJobQueueInDatabase_sinkNotOccupied() throws FileStoreServiceConnectorException {

        // Setup preconditions
        final PgJobStore pgJobStore = newPgJobStore();
        final TestablePartitioningParam testablePartitioningParam = new TestablePartitioningParamBuilder().build();
        when(testablePartitioningParam.getJobEntity().getCachedSink().getSink()).thenReturn(mock(Sink.class));
        setupMockedSink(0l);
        setupMockedJobQueueNamedQueryForFindByJob();

        // Subject Under Test
        final boolean sinkOccupied = pgJobStore.jobQueueRepository.addJobToJobQueueInDatabase(
                2l,
                new JobEntity(),
                Boolean.FALSE,
                RecordSplitter.XML);

        // Verify
        assertThat("Sink for job NOT occupied!", sinkOccupied, is(false));
    }

    @Test
    public void handlePartitioning_updateJobQueueInDatabase_sinkOccupied() throws FileStoreServiceConnectorException {

        // Setup preconditions
        final PgJobStore pgJobStore = newPgJobStore(newPgJobStoreReposity());
        pgJobStore.jobQueueRepository = newJobQueueRepository();
        final MockedAddJobParam mockedAddJobParam = new MockedAddJobParam();
        final FlowStoreReferences flowStoreReferences = buildFlowStoreReferences(
                mockedAddJobParam.getSubmitter(),
                mockedAddJobParam.getFlowBinder(),
                mockedAddJobParam.getFlow(),
                mockedAddJobParam.getSink());

        mockedAddJobParam.setFlowStoreReferences(flowStoreReferences);

        final TestableJobEntity jobEntity = new TestableJobEntityBuilder().setFlowStoreReferences(mockedAddJobParam.getFlowStoreReferences()).build();

        when(entityManager.find(eq(JobEntity.class), anyInt(), eq(LockModeType.PESSIMISTIC_WRITE))).thenReturn(jobEntity);
        when(mockedFileStoreServiceConnector.getByteSize(anyString())).thenReturn(269l);
        Long expectedNumberOfJobsBySink = 1l;

        Query mockedNamedQueryFindJobsBySink = mock(Query.class);
        when(entityManager.createNamedQuery(JobQueueEntity.NQ_FIND_NUMBER_OF_JOBS_BY_SINK)).thenReturn(mockedNamedQueryFindJobsBySink);
        when(mockedNamedQueryFindJobsBySink.setParameter(eq(JobQueueEntity.FIELD_SINK_ID), anyLong())).thenReturn(mockedNamedQueryFindJobsBySink);
        when(mockedNamedQueryFindJobsBySink.setParameter(eq(JobQueueEntity.FIELD_STATE), anyObject())).thenReturn(mockedNamedQueryFindJobsBySink);
        when(mockedNamedQueryFindJobsBySink.getSingleResult()).thenReturn(expectedNumberOfJobsBySink);

        // Subject Under Test
        final boolean sinkOccupied = pgJobStore.jobQueueRepository.addJobToJobQueueInDatabase(2l, new JobEntity(), Boolean.FALSE, RecordSplitter.XML);

        // Verify
        assertThat("Sink for job IS occupied!", sinkOccupied, is(true));
    }

    // private methods
    private FlowStoreReferences buildFlowStoreReferences(Submitter submitter, FlowBinder flowBinder, Flow flow, Sink sink) {
        return new FlowStoreReferencesBuilder()
                .setFlowStoreReference(FlowStoreReferences.Elements.SUBMITTER,
                        new FlowStoreReference(submitter.getId(), submitter.getVersion(), submitter.getContent().getName()))
                .setFlowStoreReference(FlowStoreReferences.Elements.FLOW_BINDER,
                        new FlowStoreReference(flowBinder.getId(), flowBinder.getVersion(), flowBinder.getContent().getName()))
                .setFlowStoreReference(FlowStoreReferences.Elements.FLOW,
                        new FlowStoreReference(flow.getId(), flow.getVersion(), flow.getContent().getName()))
                .setFlowStoreReference(FlowStoreReferences.Elements.SINK,
                        new FlowStoreReference(sink.getId(), sink.getVersion(), sink.getContent().getName()))
                .build();
    }

    private void setupMockedSink() {
        setupMockedSink(0l);
    }
    private void setupMockedSink(Long numberJobsBySink) {
        Query mockedNamedQueryFindJobsBySink = mock(Query.class);
        when(entityManager.createNamedQuery(JobQueueEntity.NQ_FIND_NUMBER_OF_JOBS_BY_SINK)).thenReturn(mockedNamedQueryFindJobsBySink);
        when(mockedNamedQueryFindJobsBySink.setParameter(eq(JobQueueEntity.FIELD_SINK_ID), anyLong())).thenReturn(mockedNamedQueryFindJobsBySink);
        when(mockedNamedQueryFindJobsBySink.setParameter(eq(JobQueueEntity.FIELD_STATE), anyObject())).thenReturn(mockedNamedQueryFindJobsBySink);
        when(mockedNamedQueryFindJobsBySink.getSingleResult()).thenReturn(numberJobsBySink);
    }

    private void setupMockedJobQueueNamedQueryForFindByJob() {
        JobQueueEntity jobQueueEntity = new JobQueueEntity();
        Query mockedNamedQueryFindByJob = mock(Query.class);
        when(entityManager.createNamedQuery(JobQueueEntity.NQ_FIND_BY_JOB)).thenReturn(mockedNamedQueryFindByJob);
        when(mockedNamedQueryFindByJob.setParameter(eq(JobQueueEntity.FIELD_JOB_ID), anyInt())).thenReturn(mockedNamedQueryFindByJob);
        when(mockedNamedQueryFindByJob.getSingleResult()).thenReturn(jobQueueEntity);
    }
}