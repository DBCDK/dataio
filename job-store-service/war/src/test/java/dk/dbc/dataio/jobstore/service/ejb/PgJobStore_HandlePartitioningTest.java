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

import dk.dbc.dataio.commons.types.Flow;
import dk.dbc.dataio.commons.types.FlowBinder;
import dk.dbc.dataio.commons.types.Sink;
import dk.dbc.dataio.commons.types.SinkContent;
import dk.dbc.dataio.commons.types.Submitter;
import dk.dbc.dataio.commons.utils.test.model.JobSpecificationBuilder;
import dk.dbc.dataio.filestore.service.connector.FileStoreServiceConnectorException;
import dk.dbc.dataio.jobstore.service.entity.JobEntity;
import dk.dbc.dataio.jobstore.service.entity.JobQueueEntity;
import dk.dbc.dataio.jobstore.service.entity.SinkCacheEntity;
import dk.dbc.dataio.jobstore.service.param.PartitioningParam;
import dk.dbc.dataio.jobstore.service.partitioner.DefaultXmlDataPartitionerFactory;
import dk.dbc.dataio.jobstore.test.types.FlowStoreReferencesBuilder;
import dk.dbc.dataio.jobstore.types.Diagnostic;
import dk.dbc.dataio.jobstore.types.FlowStoreReference;
import dk.dbc.dataio.jobstore.types.FlowStoreReferences;
import dk.dbc.dataio.jobstore.types.JobInfoSnapshot;
import dk.dbc.dataio.jobstore.types.JobStoreException;
import dk.dbc.dataio.jobstore.types.State;
import org.junit.Test;

import javax.persistence.LockModeType;
import javax.persistence.Query;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static dk.dbc.dataio.commons.types.RecordSplitterConstants.RecordSplitter;
import static dk.dbc.dataio.jobstore.types.Diagnostic.Level.FATAL;
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
        final MockedPartitioningParam mockedPartitioningParam = new MockedPartitioningParam();

        when(entityManager.find(eq(JobEntity.class), anyInt(), eq(LockModeType.PESSIMISTIC_WRITE))).thenReturn(mockedPartitioningParam.getJobEntity());
        when(mockedPartitioningParam.getJobEntity().getCachedSink().getSink()).thenReturn(mock(Sink.class));
        when(mockedFileStoreServiceConnector.getByteSize(anyString())).thenThrow(fileStoreUnexpectedException);

        // Subject Under Test
        final JobInfoSnapshot jobInfoSnapshot = pgJobStore.handlePartitioning(mockedPartitioningParam);

        // Verify
        assertThat("JobInfoSnapshot", jobInfoSnapshot, is(notNullValue()));
        assertThat("Fatal error occurred", jobInfoSnapshot.hasFatalError(), is(true));

        final Diagnostic diagnostic = mockedPartitioningParam.getJobEntity().getState().getDiagnostics().get(0);
        final String diagnosticsStacktrace = diagnostic.getStacktrace();
        assertTrue(!mockedPartitioningParam.getJobEntity().getState().getDiagnostics().isEmpty());
        assertThat("Diagnostics level", diagnostic.getLevel(), is(FATAL));
        assertThat("Diagnostics stacktrace", diagnosticsStacktrace, containsString("Caused by: dk.dbc.dataio.filestore.service.connector.FileStoreServiceConnectorUnexpectedStatusCodeException: unexpected status code"));
        assertThat("Diagnostics stacktrace", diagnosticsStacktrace, containsString("dk.dbc.dataio.jobstore.types.JobStoreException: Could not retrieve byte size"));
    }

    @Test
    public void handlePartitioning_differentByteSize_returnsSnapshotWithJobMarkedAsCompletedAndDiagnosticsAdded() throws JobStoreException, FileStoreServiceConnectorException {

        // Setup preconditions
        final PgJobStore pgJobStore = newPgJobStore(newPgJobStoreReposity());
        pgJobStore.jobQueueRepository = newJobQueueRepository();

        final MockedPartitioningParam mockedPartitioningParam = new MockedPartitioningParam();

        when(entityManager.find(eq(JobEntity.class), anyInt(), eq(LockModeType.PESSIMISTIC_WRITE))).thenReturn(mockedPartitioningParam.getJobEntity());
        when(mockedPartitioningParam.getJobEntity().getCachedSink().getSink()).thenReturn(mock(Sink.class));
        when(mockedFileStoreServiceConnector.getByteSize(anyString())).thenReturn(99999l);

        setupMockedSink();
        setupMockedJobQueueNamedQueryForFindByJob();

        // Subject Under Test
        final JobInfoSnapshot jobInfoSnapshot = pgJobStore.handlePartitioning(mockedPartitioningParam);

        // Verify
        assertThat("JobInfoSnapshot", jobInfoSnapshot, is(notNullValue()));
        assertThat("Fatal error occurred", jobInfoSnapshot.hasFatalError(), is(true));

        final Diagnostic diagnostic = mockedPartitioningParam.getJobEntity().getState().getDiagnostics().get(0);
        final String diagnosticsMessage = diagnostic.getMessage();
        assertTrue(!mockedPartitioningParam.getJobEntity().getState().getDiagnostics().isEmpty());
        assertThat("Diagnostics level", diagnostic.getLevel(), is(FATAL));
        assertThat("Diagnostics message", diagnosticsMessage, containsString("DataPartitioner.byteSize was: 269"));
        assertThat("Diagnostics message", diagnosticsMessage, containsString("FileStore.byteSize was: 99999"));
    }

    @Test
    public void handlePartitioning_diagnosticWithLevelFatalFound_returnsJobInformationSnapshotWithJobMarkedAsCompleted() throws JobStoreException {

        // Setup preconditions
        final PgJobStore pgJobStore = newPgJobStore(newPgJobStoreReposity());

        final MockedAddJobParam mockedAddJobParam = new MockedAddJobParam();
        mockedAddJobParam.setDiagnostics(Collections.singletonList(new Diagnostic(Diagnostic.Level.FATAL, ERROR_MESSAGE)));
        final JobEntity jobEntity = pgJobStore.jobStoreRepository.createJobEntity(mockedAddJobParam);

        final MockedPartitioningParam mockedPartitioningParam = new MockedPartitioningParam(jobEntity);

        when(entityManager.find(eq(JobEntity.class), anyInt(), eq(LockModeType.PESSIMISTIC_WRITE))).thenReturn(mockedPartitioningParam.getJobEntity());

        // Subject Under Test
        final JobInfoSnapshot jobInfoSnapshot = pgJobStore.handlePartitioning(mockedPartitioningParam);

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

        final MockedPartitioningParam mockedPartitioningParam = new MockedPartitioningParam();
        when(mockedPartitioningParam.getJobEntity().getCachedSink().getSink()).thenReturn(mock(Sink.class));

        final TestableJobEntity jobEntity = new TestableJobEntity();
        jobEntity.setTimeOfCreation(new Timestamp(new Date().getTime()));
        jobEntity.setState(new State());
        jobEntity.setSpecification(mockedPartitioningParam.getJobEntity().getSpecification());
        jobEntity.setCachedSink(EXPECTED_SINK_CACHE_ENTITY);

        when(entityManager.find(eq(JobEntity.class), anyInt(), eq(LockModeType.PESSIMISTIC_WRITE))).thenReturn(jobEntity);
        when(mockedFileStoreServiceConnector.getByteSize(anyString())).thenReturn(269l);

        setupMockedSink();
        setupMockedJobQueueNamedQueryForFindByJob();

        // Subject Under Test
        final JobInfoSnapshot jobInfoSnapshot = pgJobStore.handlePartitioning(mockedPartitioningParam);

        // Verify
        assertThat("Returned JobInfoSnapshot", jobInfoSnapshot, is(notNullValue()));
        assertThat("Fatal error did not occur", jobInfoSnapshot.hasFatalError(), is(false));
        assertThat("Number of chunks created", jobInfoSnapshot.getNumberOfChunks(), is(EXPECTED_NUMBER_OF_CHUNKS));
        assertThat("Number of items created", jobInfoSnapshot.getNumberOfItems(), is(EXPECTED_NUMBER_OF_ITEMS));
        assertThat("Partitioning phase endDate set", jobInfoSnapshot.getState().getPhase(State.Phase.PARTITIONING).getEndDate(), is(notNullValue()));
        assertThat("Time of completion not set", jobInfoSnapshot.getTimeOfCompletion(), is(nullValue()));

        assertThat("JobInfoSnapshot.State.Diagnostics", jobInfoSnapshot.getState().getDiagnostics(), is(mockedPartitioningParam.getDiagnostics()));
    }

    @Test
    public void handlePartitioning_diagnosticWithLevelWarningFound_returnsJobInformationSnapshot() throws JobStoreException, FileStoreServiceConnectorException {

        // Setup preconditions
        final PgJobStore pgJobStore = newPgJobStore(newPgJobStoreReposity());
        pgJobStore.jobQueueRepository = newJobQueueRepository();

        final MockedPartitioningParam mockedPartitioningParam = new MockedPartitioningParam();
        mockedPartitioningParam.setDiagnostics(Collections.singletonList(new Diagnostic(Diagnostic.Level.WARNING, ERROR_MESSAGE)));
        when(mockedPartitioningParam.getJobEntity().getCachedSink().getSink()).thenReturn(mock(Sink.class));

        final State state = new State();
        state.getDiagnostics().addAll(mockedPartitioningParam.getDiagnostics());

        final TestableJobEntity jobEntity = new TestableJobEntity();
        jobEntity.setTimeOfCreation(new Timestamp(new Date().getTime()));
        jobEntity.setState(state);
        jobEntity.setSpecification(new JobSpecificationBuilder().build());
        jobEntity.setCachedSink(EXPECTED_SINK_CACHE_ENTITY);

        when(entityManager.find(eq(JobEntity.class), anyInt(), eq(LockModeType.PESSIMISTIC_WRITE))).thenReturn(jobEntity);
        when(mockedFileStoreServiceConnector.getByteSize(anyString())).thenReturn(269l);

        setupMockedSink();
        setupMockedJobQueueNamedQueryForFindByJob();

        // Subject Under Test
        final JobInfoSnapshot jobInfoSnapshot = pgJobStore.handlePartitioning(mockedPartitioningParam);

        // Verify
        assertThat("Returned JobInfoSnapshot", jobInfoSnapshot, is(notNullValue()));
        assertThat("Fatal error did not occur", jobInfoSnapshot.hasFatalError(), is(false));
        assertThat("Number of chunks created", jobInfoSnapshot.getNumberOfChunks(), is(EXPECTED_NUMBER_OF_CHUNKS));
        assertThat("Number of items created", jobInfoSnapshot.getNumberOfItems(), is(EXPECTED_NUMBER_OF_ITEMS));
        assertThat("Partitioning phase endDate set", jobInfoSnapshot.getState().getPhase(State.Phase.PARTITIONING).getEndDate(), is(notNullValue()));
        assertThat("Time of completion not set", jobInfoSnapshot.getTimeOfCompletion(), is(nullValue()));

        assertThat("JobInfoSnapshot.State.Diagnostics", jobInfoSnapshot.getState().getDiagnostics(), is(mockedPartitioningParam.getDiagnostics()));
    }


    @Test
    public void handlePartitioning_sinkNotOccupied() throws FileStoreServiceConnectorException, JobStoreException {

        // Setup preconditions
        final PgJobStore pgJobStore = newPgJobStore();
        final MockedPartitioningParam mockedPartitioningParam = new MockedPartitioningParam();
        when(mockedPartitioningParam.getJobEntity().getCachedSink().getSink()).thenReturn(mock(Sink.class));

        final Sink EXPECTED_SINK = new Sink(5l, 1l, new SinkContent("TestSink", "TestResource", "TestDescription"));
        final SinkCacheEntity mockedSinkCache  = mock(SinkCacheEntity.class);
        final TestableJobEntity jobEntity = new TestableJobEntity();
        jobEntity.setTimeOfCreation(new Timestamp(new Date().getTime()));
        jobEntity.setState(new State());
        jobEntity.setSpecification(mockedPartitioningParam.getJobEntity().getSpecification());
        jobEntity.setCachedSink(mockedSinkCache);

        when(mockedSinkCache.getSink()).thenReturn(EXPECTED_SINK);
        when(pgJobStore.jobStoreRepository.getExclusiveAccessFor(eq(JobEntity.class), anyInt())).thenReturn(jobEntity);
        setupMockedSink();
        setupMockedJobQueueNamedQueryForFindByJob();

        // Subject Under Test
        final JobInfoSnapshot jobInfoSnapshot = pgJobStore.handlePartitioning(mockedPartitioningParam);

        // Verify
        assertThat("Sink not occupied!", jobInfoSnapshot, is(notNullValue()));
        verify(pgJobStore.jobQueueRepository).addJobToJobQueueInDatabase(anyLong(), any(JobEntity.class), anyBoolean(), any(RecordSplitter.class));
    }

    @Test
    public void handlePartitioning_sinkOccupied() throws FileStoreServiceConnectorException, JobStoreException {

        // Setup preconditions
        final PgJobStore pgJobStore = newPgJobStore();
        final MockedPartitioningParam mockedPartitioningParam = new MockedPartitioningParam();
        when(mockedPartitioningParam.getJobEntity().getCachedSink().getSink()).thenReturn(mock(Sink.class));

        final SinkCacheEntity mockedSinkCacheJob1  = mock(SinkCacheEntity.class);
        final TestableJobEntity jobEntity = new TestableJobEntity();
        jobEntity.setTimeOfCreation(new Timestamp(new Date().getTime()));
        jobEntity.setState(new State());
        jobEntity.setSpecification(mockedPartitioningParam.getJobEntity().getSpecification());
        jobEntity.setCachedSink(mockedSinkCacheJob1);

        when(pgJobStore.jobQueueRepository.addJobToJobQueueInDatabase(anyLong(), any(JobEntity.class), anyBoolean(), any(RecordSplitter.class))).thenReturn(OCCUPIED);

        // Subject Under Test
        final JobInfoSnapshot jobInfoSnapshot = pgJobStore.handlePartitioning(mockedPartitioningParam);

        // Verify
        assertThat("Sink for job occupied!", jobInfoSnapshot, is(notNullValue()));
        verify(pgJobStore.jobQueueRepository).addJobToJobQueueInDatabase(anyLong(), any(JobEntity.class), anyBoolean(), any(RecordSplitter.class));
    }

    @Test
    public void handlePartitioning_updateJobQueueInDatabase_sinkNotOccupied() throws FileStoreServiceConnectorException {

        // Setup preconditions
        final PgJobStore pgJobStore = newPgJobStore();
        final MockedPartitioningParam mockedPartitioningParam = new MockedPartitioningParam();
        when(mockedPartitioningParam.getJobEntity().getCachedSink().getSink()).thenReturn(mock(Sink.class));

        final SinkCacheEntity mockedSinkCacheJob  = mock(SinkCacheEntity.class);
        final TestableJobEntity jobEntity = new TestableJobEntity();
        jobEntity.setTimeOfCreation(new Timestamp(new Date().getTime()));
        jobEntity.setState(new State());
        jobEntity.setSpecification(mockedPartitioningParam.getJobEntity().getSpecification());
        jobEntity.setCachedSink(mockedSinkCacheJob);

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

        final SinkCacheEntity mockedSinkCacheJob1  = mock(SinkCacheEntity.class);
        final TestableJobEntity job1Entity = new TestableJobEntity();
        job1Entity.setTimeOfCreation(new Timestamp(new Date().getTime()));
        job1Entity.setState(new State());
        job1Entity.setFlowStoreReferences(mockedAddJobParam.getFlowStoreReferences());
        job1Entity.setSpecification(mockedAddJobParam.getJobInputStream().getJobSpecification());
        job1Entity.setCachedSink(mockedSinkCacheJob1);

        when(entityManager.find(eq(JobEntity.class), anyInt(), eq(LockModeType.PESSIMISTIC_WRITE))).thenReturn(job1Entity);
        when(mockedFileStoreServiceConnector.getByteSize(anyString())).thenReturn(269l);
        when(mockedSinkCacheJob1.getSink()).thenReturn(EXPECTED_SINK);
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
        Long expectedNumberOfJobsBySink = numberJobsBySink;
        Query mockedNamedQueryFindJobsBySink = mock(Query.class);
        when(entityManager.createNamedQuery(JobQueueEntity.NQ_FIND_NUMBER_OF_JOBS_BY_SINK)).thenReturn(mockedNamedQueryFindJobsBySink);
        when(mockedNamedQueryFindJobsBySink.setParameter(eq(JobQueueEntity.FIELD_SINK_ID), anyLong())).thenReturn(mockedNamedQueryFindJobsBySink);
        when(mockedNamedQueryFindJobsBySink.setParameter(eq(JobQueueEntity.FIELD_STATE), anyObject())).thenReturn(mockedNamedQueryFindJobsBySink);
        when(mockedNamedQueryFindJobsBySink.getSingleResult()).thenReturn(expectedNumberOfJobsBySink);
    }

    private void setupMockedJobQueueNamedQueryForFindByJob() {
        JobQueueEntity jobQueueEntity = new JobQueueEntity();
        Query mockedNamedQueryFindByJob = mock(Query.class);
        when(entityManager.createNamedQuery(JobQueueEntity.NQ_FIND_BY_JOB)).thenReturn(mockedNamedQueryFindByJob);
        when(mockedNamedQueryFindByJob.setParameter(eq(JobQueueEntity.FIELD_JOB_ID), anyInt())).thenReturn(mockedNamedQueryFindByJob);
        when(mockedNamedQueryFindByJob.getSingleResult()).thenReturn(jobQueueEntity);
    }
    class MockedPartitioningParam extends PartitioningParam {

        final String xml =
                "<records>"
                        + "<record>first</record>" + "<record>second</record>" + "<record>third</record>"
                        + "<record>fourth</record>" + "<record>fifth</record>" + "<record>sixth</record>"
                        + "<record>seventh</record>" + "<record>eighth</record>" + "<record>ninth</record>"
                        + "<record>tenth</record>" + "<record>eleventh</record>"
                        + "</records>";

        public MockedPartitioningParam() {
            this(newTestableJobEntity(new JobSpecificationBuilder().setDataFile(FILE_STORE_URN.toString()).build()));
        }
        public MockedPartitioningParam(JobEntity jobEntity) {
            super(jobEntity, mockedFileStoreServiceConnector, false, RecordSplitter.XML);

            diagnostics = new ArrayList<>();
            dataFileInputStream = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));
            dataPartitioner = new DefaultXmlDataPartitionerFactory().createDataPartitioner(dataFileInputStream, StandardCharsets.UTF_8.name());
        }

        public void setDiagnostics(List<Diagnostic> diagnostics) {
            this.diagnostics.clear();
            this.diagnostics.addAll(diagnostics);
        }
    }
}