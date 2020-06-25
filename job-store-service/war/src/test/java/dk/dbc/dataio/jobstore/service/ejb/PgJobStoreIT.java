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

import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnectorException;
import dk.dbc.dataio.commons.types.Chunk;
import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.types.Diagnostic;
import dk.dbc.dataio.commons.types.FileStoreUrn;
import dk.dbc.dataio.commons.types.JobSpecification;
import dk.dbc.dataio.commons.types.RecordSplitterConstants;
import dk.dbc.dataio.commons.utils.lang.StringUtil;
import dk.dbc.dataio.commons.utils.test.model.ChunkBuilder;
import dk.dbc.dataio.commons.utils.test.model.ChunkItemBuilder;
import dk.dbc.dataio.commons.utils.test.model.DiagnosticBuilder;
import dk.dbc.dataio.commons.utils.test.model.FlowBinderBuilder;
import dk.dbc.dataio.commons.utils.test.model.FlowBinderContentBuilder;
import dk.dbc.dataio.commons.utils.test.model.SubmitterBuilder;
import dk.dbc.dataio.commons.utils.test.model.SubmitterContentBuilder;
import dk.dbc.dataio.filestore.service.connector.FileStoreServiceConnectorException;
import dk.dbc.dataio.jobstore.service.AbstractJobStoreIT;
import dk.dbc.dataio.jobstore.service.entity.ChunkEntity;
import dk.dbc.dataio.jobstore.service.entity.ItemEntity;
import dk.dbc.dataio.jobstore.service.entity.JobEntity;
import dk.dbc.dataio.jobstore.types.DuplicateChunkException;
import dk.dbc.dataio.jobstore.types.JobError;
import dk.dbc.dataio.jobstore.types.JobInfoSnapshot;
import dk.dbc.dataio.jobstore.types.JobStoreException;
import dk.dbc.dataio.jobstore.types.State;
import dk.dbc.dataio.jobstore.types.criteria.JobListCriteria;
import dk.dbc.dataio.jobstore.types.criteria.ListFilter;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import types.TestableAddJobParam;
import types.TestableAddJobParamBuilder;

import javax.persistence.EntityTransaction;
import javax.ws.rs.ProcessingException;
import java.io.ByteArrayInputStream;
import java.io.IOError;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

public class PgJobStoreIT extends AbstractJobStoreIT {

    private static final long SLEEP_INTERVAL_IN_MS = 1000;
    private static final long MAX_WAIT_IN_MS = 50000;
    private static final Logger LOGGER = LoggerFactory.getLogger(PgJobStoreIT.class);
    private static final JobSchedulerBean JOB_SCHEDULER_BEAN = mock(JobSchedulerBean.class);
    private static final int MAX_CHUNK_SIZE = 10;

    private byte[] defaultXml = (
              "<records>"
            + "<record>first</record>"
            + "<record>second</record>"
            + "<record>third</record>"
            + "<record>fourth</record>"
            + "<record>fifth</record>"
            + "<record>sixth</record>"
            + "<record>seventh</record>"
            + "<record>eighth</record>"
            + "<record>ninth</record>"
            + "<record>tenth</record>"
            + "<record>eleventh</record>"
            + "</records>").getBytes();

    private final long defaultByteSize = defaultXml.length;

    /**
     * Given: an empty job store
     * When : adding a job
     * Then : a new job entity and the required number of chunk and item entities are created
     */
    @Test
    public void addAndScheduleJob() throws JobStoreException, SQLException, FileStoreServiceConnectorException, FlowStoreServiceConnectorException {
        // Given...
        final PgJobStore pgJobStore = newPgJobStore();
        final int expectedNumberOfJobs = 1;
        final int expectedNumberOfChunks = 2;
        final int expectedNumberOfItems = 11;

        final TestableAddJobParam testableAddJobParam = new TestableAddJobParamBuilder()
                .setSubmitter(new SubmitterBuilder().build())
                .setJobSpecification(createJobSpecification()).build();

        // Setup mocks
        setupSuccessfulMockedReturnsFromFlowStore(testableAddJobParam);

        // Set up mocked return for identical byte sizes
        when(mockedFileStoreServiceConnector.getByteSize(anyString())).thenReturn((long) testableAddJobParam.getRecords().length);
        when(mockedFlowStoreServiceConnector.getSubmitter(anyLong())).thenReturn(testableAddJobParam.getSubmitter());

        // When...
        final EntityTransaction jobTransaction = entityManager.getTransaction();
        jobTransaction.begin();
        final JobInfoSnapshot jobInfoSnapshot = pgJobStore.addAndScheduleJob(testableAddJobParam.getJobInputStream());
        jobTransaction.commit();

        // Then...
        final JobEntity jobEntity = entityManager.find(JobEntity.class, jobInfoSnapshot.getJobId());
        assertTableSizes(expectedNumberOfJobs, expectedNumberOfChunks, expectedNumberOfItems);
        assertEntities(jobEntity, expectedNumberOfChunks, expectedNumberOfItems, Collections.singletonList(State.Phase.PARTITIONING));

        assertThat("JobEntity.hasFatalError()", jobEntity.hasFatalError(), is(false));
        assertThat("JobEntity.getTimeOfCompletion()", jobEntity.getTimeOfCompletion(), is(nullValue()));
        assertThat("JobEntity.getCachedFlow()", jobEntity.getCachedFlow(), is(notNullValue()));
        assertThat("JobEntity.getCachedSink()", jobEntity.getCachedSink(), is(notNullValue()));
        assertThat("JobEntity.getWorkflowNote()", jobEntity.getWorkflowNote(), is(nullValue()));
        assertThat("JobEntity.getFlowStoreReferences()", jobEntity.getFlowStoreReferences(), is(notNullValue()));
    }

    /**
     * Given: an empty job store
     * When : adding an empty job
     * Then : a new job entity is created
     * And  : the job is scheduled
     */
    @Test
    public void addAndScheduleEmptyJob() throws FileStoreServiceConnectorException, FlowStoreServiceConnectorException,
                                                JobStoreException, SQLException {
        // Given...
        final PgJobStore pgJobStore = newPgJobStore();

        final TestableAddJobParam testableAddJobParam = new TestableAddJobParamBuilder()
                .setJobSpecification(createJobSpecification()
                        .withType(JobSpecification.Type.PERIODIC)
                        .withDataFile(FileStoreUrn.EMPTY_JOB_FILE.toString()))
                .build();

        // Setup mocks
        setupSuccessfulMockedReturnsFromFlowStore(testableAddJobParam);

        // When...
        final EntityTransaction jobTransaction = entityManager.getTransaction();
        jobTransaction.begin();
        final JobInfoSnapshot jobInfoSnapshot = pgJobStore.addAndScheduleEmptyJob(testableAddJobParam.getJobInputStream());
        jobTransaction.commit();

        // Then...
        assertTableSizes(1, 0, 0);
        final JobEntity jobEntity = entityManager.find(JobEntity.class, jobInfoSnapshot.getJobId());
        assertThat("JobEntity", jobEntity,
                is(notNullValue()));
        assertThat("JobEntity.getNumberOfChunks()", jobEntity.getNumberOfChunks(),
                is(0));
        assertThat("JobEntity.getNumberOfItems()", jobEntity.getNumberOfItems(),
                is(0));
        assertThat("JobEntity.getTimeOfCreation()", jobEntity.getTimeOfCreation(),
                is(notNullValue()));
        assertThat("JobEntity.getTimeOfLastModification()", jobEntity.getTimeOfLastModification(),
                is(notNullValue()));
        assertThat("JobEntity.getTimeOfCompletion()", jobEntity.getTimeOfCompletion(),
                is(nullValue()));
        assertThat("JobEntity.hasFatalError()", jobEntity.hasFatalError(),
                is(false));

        // And...
        verify(JOB_SCHEDULER_BEAN).markJobAsPartitioned(jobEntity);
    }

    /**
     * Given: an empty job store
     * When : numbers of bytes reported by the file store service differs from the number of bytes read
     * Then : a new job entity is created without chunk and item entities
     * And  : time of completion is set on the job entity.
     * And  : a diagnostic with level FATAL is set on the state of the job entity
     */
    @Test
    public void addJob_fileSizeInFileStoreDiffersFromActualNumberOfBytesRead_jobWithFatalErrorIsCreated()
            throws SQLException, FileStoreServiceConnectorException, FlowStoreServiceConnectorException, JobStoreException {
        // Given...
        final PgJobStore pgJobStore = newPgJobStore();
        final int expectedNumberOfJobs = 1;
        final int expectedNumberOfChunks = 2;
        final int expectedNumberOfItems = 11;
        final long fileStoreByteSize = 42;

        final TestableAddJobParam testableAddJobParam = new TestableAddJobParamBuilder()
                .setSubmitter(new SubmitterBuilder().build())
                .setJobSpecification(createJobSpecification())
                .build();

        final long dataPartitionerByteSize = testableAddJobParam.getRecords().length;

        // Setup mocks
        setupSuccessfulMockedReturnsFromFlowStore(testableAddJobParam);

        // Set up mocked return for different byte sizes
        when(mockedFileStoreServiceConnector.getByteSize(anyString())).thenReturn(fileStoreByteSize);
        when(mockedFlowStoreServiceConnector.getSubmitter(anyLong())).thenReturn(testableAddJobParam.getSubmitter());

        // When...
        final EntityTransaction jobTransaction = entityManager.getTransaction();
        jobTransaction.begin();
        final JobInfoSnapshot snapshot = pgJobStore.addJob(testableAddJobParam);
        jobTransaction.commit();

        waitForJobCompletion(snapshot.getJobId(), pgJobStore);

        // Then...
        final JobEntity jobEntity = entityManager.find(JobEntity.class, snapshot.getJobId());

        // And...
        assertThat("JobEntity.getTimeOfCompletion()", jobEntity.getTimeOfCompletion(), is(notNullValue()));

        // And...
        assertThat("JobEntity.hasFatalError()", jobEntity.hasFatalError(), is(true));
        assertThat("JobEntity.hasFatalDiagnostics()", jobEntity.hasFatalDiagnostics(), is(true));
        assertThat("JobEntity.getState().getDiagnostics().size()", jobEntity.getState().getDiagnostics().size(), is(1));
        assertThat("JobEntity.getState().getDiagnostics().get(0).getLevel()",
                jobEntity.getState().getDiagnostics().get(0).getLevel(), is(Diagnostic.Level.FATAL));

        // And...
        final String expectedStacktrace = String.format("DataPartitioner.byteSize was: %s expected %s", dataPartitionerByteSize, fileStoreByteSize);
        assertThat(expectedStacktrace, jobEntity.getState().getDiagnostics().get(0).getStacktrace().contains("Error reading data file"), is(true));

        // And...
        assertTableSizes(expectedNumberOfJobs, expectedNumberOfChunks, expectedNumberOfItems);
    }

    /**
     * Given: an empty job store
     * When : adding a job with addJobParam.level.FATAL as input
     * Then : a new job entity is created but chunks and item entities are not created
     * And  : a flow cache entity is not created
     * And  : a sink cache entity is not created
     * And  : time of completion is set on the job entity.
     * And  : a diagnostic with level FATAL is set on the state of the job entity
     */
    @Test
    public void addJob_failsFastDueToAddJobParamFailures_jobWithFatalErrorIsCreated() throws SQLException, FileStoreServiceConnectorException {
        // Given...
        final PgJobStore pgJobStore = newPgJobStore();
        final int expectedNumberOfJobs = 1;
        final int expectedNumberOfChunks = 0;
        final int expectedNumberOfItems = 0;

        // When...
        final JobSpecification jobSpecification = createJobSpecification();
        final TestableAddJobParam testableAddJobParam = new TestableAddJobParamBuilder()
                .setSubmitter(new SubmitterBuilder().build())
                .setJobSpecification(jobSpecification.withMailForNotificationAboutVerification("mail"))
                .setDiagnostics(Collections.singletonList(new DiagnosticBuilder().build()))
                .build();

        final JobInfoSnapshot jobInfoSnapshot = commitJob(pgJobStore, testableAddJobParam);

        // Then...
        final JobEntity jobEntity = entityManager.find(JobEntity.class, jobInfoSnapshot.getJobId());
        assertTableSizes(expectedNumberOfJobs, expectedNumberOfChunks, expectedNumberOfItems);
        assertEntities(jobEntity, expectedNumberOfChunks, expectedNumberOfItems, new ArrayList<>());

        // And...
        assertThat("JobEntity.getCachedFlow()", jobEntity.getCachedFlow(), is(nullValue()));

        // And...
        assertThat("JobEntity.getCachedSink()", jobEntity.getCachedSink(), is(nullValue()));

        // And...
        assertThat("JobEntity.getTimeOfCompletion()", jobEntity.getTimeOfCompletion(), is(notNullValue()));

        // And...
        assertThat("JobEntity.hasFatalError()", jobEntity.hasFatalError(), is(true));
        assertThat("JobEntity.hasFatalDiagnostics()", jobEntity.hasFatalDiagnostics(), is(true));
        assertThat("JobEntity.getState().getDiagnostics().size()", jobEntity.getState().getDiagnostics().size(), is(1));
        assertThat("JobEntity.getState().getDiagnostics().get(0).getLevel()",
                jobEntity.getState().getDiagnostics().get(0).getLevel(), is(Diagnostic.Level.FATAL));
    }

    /**
     * Given: an empty job store
     * When : adding a (preview) job that has AddJobParam failures
     * Then : no jobNotification is send
     */
    @Test
    public void addJob_previewFails_noNotification() throws FileStoreServiceConnectorException {
        // Given...
        final PgJobStore pgJobStore = newPgJobStore();
        pgJobStore.jobNotificationRepository = mock(JobNotificationRepository.class);

        // When...
        final TestableAddJobParam testableAddJobParam = new TestableAddJobParamBuilder()
                .setSubmitter(new SubmitterBuilder().setContent(new SubmitterContentBuilder().setEnabled(false).build()).build())
                .setJobSpecification(createJobSpecification())
                .setDiagnostics(Collections.singletonList(new DiagnosticBuilder().build()))
                .build();

        commitJob(pgJobStore, testableAddJobParam);

        // Then...
        verifyNoInteractions(pgJobStore.jobNotificationRepository);
    }

    /**
     * Given: an empty job store
     * When : adding a (preview) job
     * Then  : no jobNotification is send
     */
    @Test
    public void addJob_previewOk_noNotification() throws FileStoreServiceConnectorException {
        // Given...
        final PgJobStore pgJobStore = newPgJobStore();
        pgJobStore.jobNotificationRepository = mock(JobNotificationRepository.class);

        // When...
        final TestableAddJobParam testableAddJobParam = new TestableAddJobParamBuilder()
                .setJobSpecification(createJobSpecification())
                .setSubmitter(new SubmitterBuilder().setContent(new SubmitterContentBuilder().setEnabled(false).build()).build())
                .build();

        commitJob(pgJobStore, testableAddJobParam);

        // Then...
        verifyNoInteractions(pgJobStore.jobNotificationRepository);
    }

    /**
     * Given: an empty job store
     * When : adding a job which fails immediately during partitioning
     * Then : a new job entity with a fatal diagnostic is created
     */
    @Test
    public void addJob_failsFastDuringCreation_jobWithFatalErrorIsAdded() throws JobStoreException, FileStoreServiceConnectorException, FlowStoreServiceConnectorException {
        // Given...
        final PgJobStore pgJobStore = newPgJobStore();
        final TestableAddJobParam testableAddJobParam = new TestableAddJobParamBuilder()
                .setSubmitter(new SubmitterBuilder().build())
                .setJobSpecification(createJobSpecification())
                .build();

        when(mockedFlowStoreServiceConnector.getSubmitter(anyLong())).thenReturn(testableAddJobParam.getSubmitter());
        when(mockedFileStoreServiceConnector.getFile(anyString())).thenThrow(new ProcessingException("Died"));

        // When...
        final EntityTransaction transaction = entityManager.getTransaction();
        transaction.begin();
        final JobInfoSnapshot jobInfoSnapshot = pgJobStore.addJob(testableAddJobParam);
        transaction.commit();

        // Then...
        final JobEntity jobEntity = entityManager.find(JobEntity.class, jobInfoSnapshot.getJobId());
        assertThat("JobEntity.hasFatalError()", jobEntity.hasFatalError(), is(true));
        assertThat("JobEntity.getTimeOfCompletion()", jobEntity.getTimeOfCompletion(), is(notNullValue()));
        assertThat("JobEntity.hasFatalDiagnostics()", jobEntity.hasFatalDiagnostics(), is(true));
        assertThat("JobEntity.getState().getDiagnostics().size()", jobEntity.getState().getDiagnostics().size(), is(1));
        assertThat("JobEntity.getState().getDiagnostics().get(0).getLevel()",
                jobEntity.getState().getDiagnostics().get(0).getLevel(), is(Diagnostic.Level.FATAL));
    }

    /**
     * Given: an empty job store
     * When : adding a job which fails eventually during partitioning
     * Then : a new job entity with a fatal diagnostic is created
     */
    @Test
    public void addJob_failsEventuallyDuringPartitioning_jobWithFatalErrorIsAdded() throws JobStoreException, FileStoreServiceConnectorException, FlowStoreServiceConnectorException {
        // Given...
        final PgJobStore pgJobStore = newPgJobStore();

        final TestableAddJobParam testableAddJobParam = new TestableAddJobParamBuilder()
                .setSubmitter(new SubmitterBuilder().build())
                .setJobSpecification(createJobSpecification())
                .setRecords(Arrays.copyOfRange(defaultXml, 0, 25))
                .build();
        when(mockedFlowStoreServiceConnector.getSubmitter(anyLong())).thenReturn(testableAddJobParam.getSubmitter());
        // When...
        final EntityTransaction transaction = entityManager.getTransaction();
        transaction.begin();
        final JobInfoSnapshot jobInfoSnapshot = pgJobStore.addJob(testableAddJobParam);
        transaction.commit();

        // Then...
        final JobEntity jobEntity = entityManager.find(JobEntity.class, jobInfoSnapshot.getJobId());
        assertThat("JobEntity.hasFatalError()", jobEntity.hasFatalError(), is(true));
        assertThat("JobEntity.getTimeOfCompletion()", jobEntity.getTimeOfCompletion(), is(notNullValue()));
        assertThat("JobEntity.hasFatalDiagnostics()", jobEntity.hasFatalDiagnostics(), is(true));
        assertThat("JobEntity.getState().getDiagnostics().size()", jobEntity.getState().getDiagnostics().size(), is(1));
        assertThat("JobEntity.getState().getDiagnostics().get(0).getLevel()",
                jobEntity.getState().getDiagnostics().get(0).getLevel(), is(Diagnostic.Level.FATAL));
    }

    /**
     * Given: a datafile
     * When : adding a job pointing to this datafile
     * Then : an unexpected exception is thrown during partitioning resulting in a new job entity with a fatal diagnostic
     */
    @Test
    public void addJob_partitioningThrowsUnexpectedException_jobWithFatalErrorIsAdded() throws FileStoreServiceConnectorException {
        // Given...
        final JobSpecification jobSpecification = createJobSpecification().withCharset("latin1").withDataFile("urn:dataio-fs:42");
        final TestableAddJobParam addJobParam = new TestableAddJobParamBuilder()
                .setJobSpecification(jobSpecification)
                .setFlowBinder(new FlowBinderBuilder()
                        .setContent(new FlowBinderContentBuilder()
                                .setRecordSplitter(RecordSplitterConstants.RecordSplitter.DANMARC2_LINE_FORMAT)
                                .build())
                        .build())
                .setSubmitter(new SubmitterBuilder().build())
                .build();

        final PgJobStore pgJobStore = newPgJobStore();

        when(mockedFileStoreServiceConnector.getFile(anyString())).thenThrow(new IOError(new Exception("Forced Test exception" ) ));

        // When...
        final JobInfoSnapshot jobInfoSnapshot = persistenceContext.run(() -> pgJobStore.addJob(addJobParam));

        // Then...
        final JobEntity jobEntity = entityManager.find(JobEntity.class, jobInfoSnapshot.getJobId());
        assertThat("JobEntity.hasFatalError()", jobEntity.hasFatalError(), is(true));
        assertThat("JobEntity.getTimeOfCompletion()", jobEntity.getTimeOfCompletion(), is(notNullValue()));
        assertThat("JobEntity.hasFatalDiagnostics()", jobEntity.hasFatalDiagnostics(), is(true));
        assertThat("diagnostics message", jobEntity.getState().getDiagnostics().get(0).getMessage(),
                is("unexpected exception caught while partitioning job"));
    }

    /**
     * Given: an empty job store
     * When : adding a job with addJobParam.level.WARNING as input
     * Then : a new job entity and the required number of chunk and item entities are created
     * And  : a diagnostic with level WARNING is set on the state of the job entity
     */
    @Test
    public void addJob_withWarningDiagnostic_jobIsAdded() throws FileStoreServiceConnectorException, SQLException, FlowStoreServiceConnectorException {
        // Given...
        final PgJobStore pgJobStore = newPgJobStore();
        final int expectedNumberOfJobs = 1;
        final int expectedNumberOfChunks = 2;
        final int expectedNumberOfItems = 11;

        // When...
        setupExpectationOnGetByteSize(defaultByteSize);

        final TestableAddJobParam addJobParam = new TestableAddJobParamBuilder()
                .setJobSpecification(createJobSpecification())
                .setDiagnostics(Collections.singletonList(new DiagnosticBuilder().setLevel(Diagnostic.Level.WARNING).build()))
                .setSubmitter(new SubmitterBuilder().build())
                .build();

        when(mockedFlowStoreServiceConnector.getSubmitter(anyLong())).thenReturn(addJobParam.getSubmitter());

        final JobInfoSnapshot jobInfoSnapshot = commitJob(pgJobStore, addJobParam);

        // Then...
        final JobEntity jobEntity = entityManager.find(JobEntity.class, jobInfoSnapshot.getJobId());
        assertTableSizes(expectedNumberOfJobs, expectedNumberOfChunks, expectedNumberOfItems);
        assertEntities(jobEntity, expectedNumberOfChunks, expectedNumberOfItems, Collections.singletonList(State.Phase.PARTITIONING));

        // And ...
        assertThat("JobEntity.hasFatalError()", jobEntity.hasFatalError(), is(false));
        assertThat("JobEntity.getTimeOfCompletion()", jobEntity.getTimeOfCompletion(), is(nullValue()));
        assertThat("JobEntity.hasFatalDiagnostics()", jobEntity.hasFatalDiagnostics(), is(false));
        assertThat("JobEntity.getState().getDiagnostics().size()", jobEntity.getState().getDiagnostics().size(), is(1));
        assertThat("JobEntity.getState().getDiagnostics().get(0).getLevel()",
                jobEntity.getState().getDiagnostics().get(0).getLevel(), is(Diagnostic.Level.WARNING));
    }

    /**
     * Given: an empty job store
     * When : adding a job
     * Then : a new job entity and the required number of chunk and item entities are created
     * And  : a flow cache entity is created
     * And  : a sink cache entity is created
     * And  : no diagnostics were created while adding job
     */
    @Test
    public void addJob() throws JobStoreException, SQLException, FileStoreServiceConnectorException, FlowStoreServiceConnectorException {
        // Given...
        final PgJobStore pgJobStore = newPgJobStore();
        final int expectedNumberOfJobs = 1;
        final int expectedNumberOfChunks = 2;
        final int expectedNumberOfItems = 11;

        // When...
        setupExpectationOnGetByteSize(defaultByteSize);
        JobInfoSnapshot jobInfoSnapshot = addJobs(expectedNumberOfJobs, pgJobStore).get(0);

        // Then...
        final JobEntity jobEntity = entityManager.find(JobEntity.class, jobInfoSnapshot.getJobId());
        assertTableSizes(expectedNumberOfJobs, expectedNumberOfChunks, expectedNumberOfItems);
        assertEntities(jobEntity, expectedNumberOfChunks, expectedNumberOfItems, Collections.singletonList(State.Phase.PARTITIONING));

        // And...
        assertThat("JobEntity.getCachedFlow()", jobEntity.getCachedFlow(), is(notNullValue()));

        // And...
        assertThat("JobEntity.getCachedSink()", jobEntity.getCachedSink(), is(notNullValue()));

        // And ...
        assertThat("jobEntity.getState().getDiagnostics().size()", jobEntity.getState().getDiagnostics().size(), is(0));
    }

    /**
     * Given: a job store where a job is added
     * When : an chunk with Next processing Data is added
     * Then : the job info snapshot is updated
     * And  : the referenced entities are updated
     */
    @Test
    public void addChunk_whenChunkHasNextEntry_chunkIsAdded() throws JobStoreException, FileStoreServiceConnectorException, FlowStoreServiceConnectorException {
        // Given...
        final PgJobStore pgJobStore = newPgJobStore();
        final int chunkId = 1;                   // second chunk is used, hence the chunk id is 1.
        final short itemId = 0;                  // The second chunk contains only one item, hence the item id is 0.

        setupExpectationOnGetByteSize(defaultByteSize);
        final JobInfoSnapshot jobInfoSnapshotNewJob = addJobs(1, pgJobStore).get(0);

        assertThat(jobInfoSnapshotNewJob, not(nullValue()));

        // Validate that nothing has been processed on job level
        assertThat(jobInfoSnapshotNewJob.getState().getPhase(State.Phase.PROCESSING).getSucceeded(), is(0));

        // Validate that nothing has been processed on chunk level
        assertAndReturnChunkState(jobInfoSnapshotNewJob.getJobId(), chunkId, 0, State.Phase.PROCESSING, false);

        // Validate that nothing has been processed on item level
        assertAndReturnItemState(jobInfoSnapshotNewJob.getJobId(), chunkId, itemId, 0, State.Phase.PROCESSING, false);

        Chunk chunk = buildChunkWithNextItems(
                jobInfoSnapshotNewJob.getJobId(),
                chunkId, 1,
                Chunk.Type.PROCESSED,
                ChunkItem.Status.SUCCESS);

        // When...
        final EntityTransaction chunkTransaction = entityManager.getTransaction();
        chunkTransaction.begin();
        final JobInfoSnapshot jobInfoSnapShotUpdatedJob = pgJobStore.addChunk(chunk);
        chunkTransaction.commit();

        // Then...
        assertThat(jobInfoSnapShotUpdatedJob, not(nullValue()));

        // Validate that one chunk has been processed on job level
        assertThat(jobInfoSnapShotUpdatedJob.getState().getPhase(State.Phase.PROCESSING).getSucceeded(), is(1));
        LOGGER.info("new-job: {} updated-job: {}", jobInfoSnapshotNewJob.getTimeOfLastModification().getTime(), jobInfoSnapShotUpdatedJob.getTimeOfLastModification().getTime());
        assertThat(jobInfoSnapShotUpdatedJob.getTimeOfLastModification().after(jobInfoSnapshotNewJob.getTimeOfLastModification()), is(true));

        // And...

        // Validate that one chunk has been processed on chunk level
        assertAndReturnChunkState(jobInfoSnapShotUpdatedJob.getJobId(), chunkId, 1, State.Phase.PROCESSING, true);

        // Validate that one chunk has been processed on item level
        assertAndReturnItemState(jobInfoSnapShotUpdatedJob.getJobId(), chunkId, itemId, 1, State.Phase.PROCESSING, true);

        clearEntityManagerCache();

        Chunk fromDB = pgJobStore.jobStoreRepository.getChunk(Chunk.Type.PROCESSED, jobInfoSnapshotNewJob.getJobId(), chunkId);
        assertThat(fromDB.hasNextItems(), is(true));

        // extra checks for next items.
        assertThat(fromDB.getNext().size(), is(1));
        assertThat(fromDB.getItems().size(), is(1));

        ChunkItem chunkItem = fromDB.getItems().get(0);
        ChunkItem nextChunkItem = fromDB.getNext().get(0);

        assertThat("nextChunkItem.getData() NOT chunkItem.getData()", nextChunkItem.getData(), not(chunkItem.getData()));
        assertThat("nextChunkItem.getStatus() IS nextChunkItem.getStatus()", nextChunkItem.getStatus(), is(chunkItem.getStatus()));
    }


    /**
     * Given: a job store where a job is added
     * When : the same chunk is added twice
     * Then : a DuplicateChunkException is thrown
     * And  : the DuplicateChunkException contains a JobError with Code.ILLEGAL_CHUNK
     * And  : job, chunk and item entities have not been updated after the second add.
     */
    @Test
    public void addChunk_duplicateChunksAdded_throws() throws JobStoreException, FileStoreServiceConnectorException, FlowStoreServiceConnectorException {
        final PgJobStore pgJobStore = newPgJobStore();
        final int chunkId = 0;                   // first chunk is used, hence the chunk id is 0.
        final int numberOfItems = 10;

        setupExpectationOnGetByteSize(defaultByteSize);
        JobInfoSnapshot jobInfoSnapshot = addJobs(1, pgJobStore).get(0);

        Chunk chunk = buildChunk(
                jobInfoSnapshot.getJobId(),
                chunkId,
                numberOfItems,
                Chunk.Type.PROCESSED,
                ChunkItem.Status.SUCCESS);

        // When...
        final EntityTransaction chunkTransaction = entityManager.getTransaction();

        chunkTransaction.begin();
        pgJobStore.addChunk(chunk);
        chunkTransaction.commit();

        // Retrieve the entities for comparison
        final JobEntity jobEntityFirstAddChunk = entityManager.find(JobEntity.class, jobInfoSnapshot.getJobId());

        final ChunkEntity.Key chunkKey = new ChunkEntity.Key(chunkId, jobEntityFirstAddChunk.getId());
        final ChunkEntity chunkEntityFirstAddChunk = entityManager.find(ChunkEntity.class, chunkKey);

        final List<ItemEntity> itemEntities = new ArrayList<>(numberOfItems);
        for (int i = 0; i < numberOfItems; i++) {
            itemEntities.add(entityManager.find(ItemEntity.class, new ItemEntity.Key(jobEntityFirstAddChunk.getId(), chunkId, (short) i)));
        }

        try {
            chunkTransaction.begin();
            pgJobStore.addChunk(chunk);
            chunkTransaction.commit();

        // Then...
        } catch (DuplicateChunkException e) {

            // And...
            assertThat(e.getJobError().getCode(), is(JobError.Code.ILLEGAL_CHUNK));

            // And...
            final JobEntity jobEntitySecondAddChunk = entityManager.find(JobEntity.class, jobInfoSnapshot.getJobId());
            assertThat("JobEntity not updated", jobEntitySecondAddChunk.getState(), is(jobEntityFirstAddChunk.getState()));

            final ChunkEntity chunkEntitySecondAddChunk = entityManager.find(ChunkEntity.class, chunkKey);
            assertThat("ChunkEntity not updated", chunkEntitySecondAddChunk, is(chunkEntityFirstAddChunk));

            for(int i = 0; i < numberOfItems; i++) {
                    final ItemEntity itemEntitySecondAddChunk = entityManager.find(ItemEntity.class, new ItemEntity.Key(jobEntityFirstAddChunk.getId(), chunkId, (short) i));
                    assertThat("ItemEntity not updated", itemEntitySecondAddChunk, is(itemEntities.get(i)));
            }
        }
    }

    /**
     * Given: a job store containing one job with one chunk and three items that each has completed partitioning
     *
     * When : adding chunk where:
     *          1 item has failed in processing.
     *          1 item has been ignored in processing.
     *          1 item has been successfully processed.
     *
     * then : the item entities are updated correctly: Each having processing outcome set with the expected data
     * And  : The chunk and job entity are updated correctly
     *
     * And When : adding chunk where:
     *          1 item has failed in delivering.
     *          1 item has been ignored in delivering.
     *          1 item has been successfully delivered.
     *
     * then : the item entities are updated correctly: Each having delivering outcome set with the expected data
     * And  : The chunk and job entity are updated correctly
     */
    @Test
    public void addChunk_entitiesAreUpdated() throws FileStoreServiceConnectorException {
        // Given...
        final PgJobStore pgJobStore = newPgJobStore();
        final JobEntity jobEntity = newPersistedJobEntity();
        final int jobId = jobEntity.getId();
        final int chunkId = 0;
        final short numberOfItems = 3;
        final short failedItemId = 0;
        final short ignoredItemId = 1;
        final short succeededItemId = 2;
        Timestamp currentTime = new Timestamp(System.currentTimeMillis());

        // Mimic add job
        final ChunkEntity chunkEntity = newPersistedChunkEntityWithPartitioningCompleted(new ChunkEntity.Key(chunkId, jobId), numberOfItems);
        final ItemEntity failedItemEntity = newPersistedItemEntityWithPartitioningCompleted(new ItemEntity.Key(jobId, chunkId, failedItemId));
        final ItemEntity ignoredItemEntity = newPersistedItemEntityWithPartitioningCompleted(new ItemEntity.Key(jobId, chunkId, ignoredItemId));
        final ItemEntity succeededItemEntity = newPersistedItemEntityWithPartitioningCompleted(new ItemEntity.Key(jobId, chunkId, succeededItemId));

        // Create item entities for processed chunk
        final List<ChunkItem> processedChunkItems = new ArrayList<>();
        processedChunkItems.add(new ChunkItemBuilder().setId(failedItemId).setData(getData(Chunk.Type.PROCESSED)).setStatus(ChunkItem.Status.FAILURE).build());
        processedChunkItems.add(new ChunkItemBuilder().setId(ignoredItemId).setData(getData(Chunk.Type.PROCESSED)).setStatus(ChunkItem.Status.IGNORE).build());
        processedChunkItems.add(new ChunkItemBuilder().setId(succeededItemId).setData(getData(Chunk.Type.PROCESSED)).setStatus(ChunkItem.Status.SUCCESS).build());

        // Create processed chunk
        final Chunk processedChunk = new ChunkBuilder(Chunk.Type.PROCESSED).setJobId(jobId).setChunkId(chunkId).setItems(processedChunkItems).build();

        // Create item entities for delivered chunk
        final List<ChunkItem> deliveredChunkItems = new ArrayList<>();
        deliveredChunkItems.add(new ChunkItemBuilder().setId(failedItemId).setData(getData(Chunk.Type.DELIVERED)).setStatus(ChunkItem.Status.FAILURE).build());
        deliveredChunkItems.add(new ChunkItemBuilder().setId(ignoredItemId).setData(getData(Chunk.Type.DELIVERED)).setStatus(ChunkItem.Status.IGNORE).build());
        deliveredChunkItems.add(new ChunkItemBuilder().setId(succeededItemId).setData(getData(Chunk.Type.DELIVERED)).setStatus(ChunkItem.Status.SUCCESS).build());

        // Create delivered chunk
        final Chunk deliveredChunk = new ChunkBuilder(Chunk.Type.DELIVERED).setJobId(jobId).setChunkId(chunkId).setItems(deliveredChunkItems).build();

        // When...
        persistenceContext.run(() ->
                pgJobStore.addChunk(processedChunk)
        );

        // Then...
        assertFailedItemEntity(failedItemEntity, State.Phase.PROCESSING, Chunk.Type.PROCESSED);
        assertIgnoredItemEntity(ignoredItemEntity, State.Phase.PROCESSING, Chunk.Type.PROCESSED);
        assertSucceededItemEntity(succeededItemEntity, State.Phase.PROCESSING, Chunk.Type.PROCESSED);

        // And...
        assertThat("chunkEntity -> number of items failed in processing", chunkEntity.getState().getPhase(State.Phase.PROCESSING).getFailed(), is(1));
        assertThat("chunkEntity -> number of items ignored in processing", chunkEntity.getState().getPhase(State.Phase.PROCESSING).getIgnored(), is(1));
        assertThat("chunkEntity -> number of items succeeded in processing", chunkEntity.getState().getPhase(State.Phase.PROCESSING).getSucceeded(), is(1));
        assertThat("chunkEntity processing completed", chunkEntity.getState().phaseIsDone(State.Phase.PROCESSING), is(true));
        assertThat("chunkEntity.timeOfCompletion", chunkEntity.getTimeOfCompletion(), is(nullValue()));
        assertThat("chunkEntity.allPhasesAreDone", chunkEntity.getState().allPhasesAreDone(), is(false));

        assertThat("jobEntity -> number of items failed in processing", jobEntity.getState().getPhase(State.Phase.PROCESSING).getFailed(), is(1));
        assertThat("jobEntity -> number of items ignored in processing", jobEntity.getState().getPhase(State.Phase.PROCESSING).getIgnored(), is(1));
        assertThat("jobEntity -> number of items succeeded in processing", jobEntity.getState().getPhase(State.Phase.PROCESSING).getSucceeded(), is(1));
        assertThat("jobEntity.timeOfLastModification", jobEntity.getTimeOfLastModification().after(currentTime), is(true));
        currentTime = jobEntity.getTimeOfLastModification();

        // And When...
        persistenceContext.run(() ->
                pgJobStore.addChunk(deliveredChunk)
        );

        // Then...
        assertFailedItemEntity(failedItemEntity, State.Phase.DELIVERING, Chunk.Type.DELIVERED);
        assertIgnoredItemEntity(ignoredItemEntity, State.Phase.DELIVERING, Chunk.Type.DELIVERED);
        assertSucceededItemEntity(succeededItemEntity, State.Phase.DELIVERING, Chunk.Type.DELIVERED);

        // And...
        assertThat("chunkEntity -> number of items failed in delivering", chunkEntity.getState().getPhase(State.Phase.DELIVERING).getFailed(), is(1));
        assertThat("chunkEntity -> number of items ignored in delivering", chunkEntity.getState().getPhase(State.Phase.DELIVERING).getIgnored(), is(1));
        assertThat("chunkEntity -> number of items succeeded in delivering", chunkEntity.getState().getPhase(State.Phase.DELIVERING).getSucceeded(), is(1));
        assertThat("chunkEntity delivering completed", chunkEntity.getState().phaseIsDone(State.Phase.DELIVERING), is(true));
        assertThat("chunkEntity.timeOfCompletion", chunkEntity.getTimeOfCompletion(), is(notNullValue()));
        assertThat("chunkEntity.allPhasesAreDone", chunkEntity.getState().allPhasesAreDone(), is(true));

        assertThat("jobEntity -> number of items failed in delivering", jobEntity.getState().getPhase(State.Phase.DELIVERING).getFailed(), is(1));
        assertThat("jobEntity -> number of items ignored in delivering", jobEntity.getState().getPhase(State.Phase.DELIVERING).getIgnored(), is(1));
        assertThat("jobEntity -> number of items succeeded in delivering", jobEntity.getState().getPhase(State.Phase.DELIVERING).getSucceeded(), is(1));
        assertThat("jobEntity.timeOfLastModification", jobEntity.getTimeOfLastModification().after(currentTime), is(true));
    }

    /**
     * Given: a job store containing one job with one chunk with one item
     *        which has completed partitioning and processing phases
     *  When: adding result chunk for delivery phase
     *  Then: the job is completed
     */
    @Test
    public void addChunk_completesJob() throws FileStoreServiceConnectorException {
        // Given...

        final JobEntity jobEntity = newJobEntity();
        jobEntity.setNumberOfChunks(1);
        jobEntity.setNumberOfItems(1);
        jobEntity.getState().getPhase(State.Phase.PARTITIONING)
                .withSucceeded(1)
                .withEndDate(new Date());
        jobEntity.getState().getPhase(State.Phase.PROCESSING)
                .withSucceeded(1)
                .withEndDate(new Date());

        persist(jobEntity);

        final ChunkEntity chunkEntity = newChunkEntity(new ChunkEntity.Key(0, jobEntity.getId()));
        chunkEntity.getState().getPhase(State.Phase.PARTITIONING)
                .withSucceeded(1)
                .withEndDate(new Date());
        chunkEntity.getState().getPhase(State.Phase.PROCESSING)
                .withSucceeded(1)
                .withEndDate(new Date());
        chunkEntity.setNumberOfItems((short) 1);

        final ItemEntity itemEntity = newItemEntity(new ItemEntity.Key(jobEntity.getId(), 0, (short) 0));
        itemEntity.getState().getPhase(State.Phase.PARTITIONING)
                .withSucceeded(1)
                .withEndDate(new Date());
        itemEntity.getState().getPhase(State.Phase.PROCESSING)
                .withSucceeded(1)
                .withEndDate(new Date());

        persist(chunkEntity);
        persist(itemEntity);

        // When ...

        final PgJobStore pgJobStore = newPgJobStore();

        final Chunk deliveryResultChunk0 = new ChunkBuilder(Chunk.Type.DELIVERED)
                .setJobId(jobEntity.getId())
                .setChunkId(0)
                .setItems(Collections.singletonList(
                        new ChunkItemBuilder()
                                .setId(0)
                                .setData("OK")
                                .setStatus(ChunkItem.Status.SUCCESS)
                                .build()))
                .build();

        persistenceContext.run(() ->
                pgJobStore.addChunk(deliveryResultChunk0)
        );

        // Then...

        final JobEntity jobEntityAfterDeliveryOfChunk = entityManager.find(JobEntity.class, jobEntity.getId());
        assertThat("job timeOfCompletion after chunk delivered",
                jobEntityAfterDeliveryOfChunk.getTimeOfCompletion(), is(notNullValue()));
    }

    /**
     * Given: a job store containing one job with two chunks each with one item
     *        which has completed partitioning and processing phases, and the last
     *        chunk is a termination chunk
     *  When: adding result chunk for delivery phase for the first chunk
     *  Then: the job is not completed
     *  When: adding result chunk for delivery phase for the termination chunk
     *  Then: the job is completed
     */
    @Test
    public void addChunk_completesJobOnTerminationChunkDelivery() throws FileStoreServiceConnectorException {
        // Given...

        final JobEntity jobEntity = newJobEntity();
        jobEntity.setNumberOfChunks(2);     // last chunk is termination chunk
        jobEntity.setNumberOfItems(2);      // last item is termination chunk item
        jobEntity.getState().getPhase(State.Phase.PARTITIONING)
                .withSucceeded(1)
                .withEndDate(new Date());
        jobEntity.getState().getPhase(State.Phase.PROCESSING)
                .withSucceeded(1)
                .withEndDate(new Date());

        persist(jobEntity);

        final ChunkEntity chunkEntity0 = newChunkEntity(new ChunkEntity.Key(0, jobEntity.getId()));
        chunkEntity0.getState().getPhase(State.Phase.PARTITIONING)
                .withSucceeded(1)
                .withEndDate(new Date());
        chunkEntity0.getState().getPhase(State.Phase.PROCESSING)
                .withSucceeded(1)
                .withEndDate(new Date());
        chunkEntity0.setNumberOfItems((short) 1);

        final ItemEntity itemEntity0_0 = newItemEntity(new ItemEntity.Key(jobEntity.getId(), 0, (short) 0));
        itemEntity0_0.getState().getPhase(State.Phase.PARTITIONING)
                .withSucceeded(1)
                .withEndDate(new Date());
        itemEntity0_0.getState().getPhase(State.Phase.PROCESSING)
                .withSucceeded(1)
                .withEndDate(new Date());

        // Termination chunk
        final ChunkEntity chunkEntity1 = newChunkEntity(new ChunkEntity.Key(1, jobEntity.getId()));
        chunkEntity1.getState().getPhase(State.Phase.PARTITIONING)
                .withSucceeded(1)
                .withEndDate(new Date());
        chunkEntity1.getState().getPhase(State.Phase.PROCESSING)
                .withSucceeded(1)
                .withEndDate(new Date());
        chunkEntity1.setNumberOfItems((short) 1);

        final ItemEntity itemEntity1_0 = newItemEntity(new ItemEntity.Key(jobEntity.getId(), 1, (short) 0));
        itemEntity1_0.getState().getPhase(State.Phase.PARTITIONING)
                .withSucceeded(1)
                .withEndDate(new Date());
        itemEntity1_0.getState().getPhase(State.Phase.PROCESSING)
                .withSucceeded(1)
                .withEndDate(new Date());

        persist(chunkEntity0);
        persist(chunkEntity1);
        persist(itemEntity0_0);
        persist(itemEntity1_0);

        // When...

        final PgJobStore pgJobStore = newPgJobStore();

        final Chunk deliveryResultChunk0 = new ChunkBuilder(Chunk.Type.DELIVERED)
                .setJobId(jobEntity.getId())
                .setChunkId(0)
                .setItems(Collections.singletonList(
                        new ChunkItemBuilder()
                                .setId(0)
                                .setData("OK")
                                .setStatus(ChunkItem.Status.SUCCESS)
                                .build()))
                .build();

        persistenceContext.run(() ->
                pgJobStore.addChunk(deliveryResultChunk0)
        );

        // Then...

        final JobEntity jobEntityAfterDeliveryOfChunk0 = entityManager.find(JobEntity.class, jobEntity.getId());
        assertThat("job timeOfCompletion after first chunk delivered",
                jobEntityAfterDeliveryOfChunk0.getTimeOfCompletion(), is(nullValue()));

        // When...

        final Chunk deliveryResultChunk1 = new ChunkBuilder(Chunk.Type.DELIVERED)
                .setJobId(jobEntity.getId())
                .setChunkId(1)
                .setItems(Collections.singletonList(
                        new ChunkItemBuilder()
                                .setId(0)
                                .setData("OK")
                                .setStatus(ChunkItem.Status.SUCCESS)
                                .setType(ChunkItem.Type.JOB_END)
                                .build()))
                .build();

        persistenceContext.run(() ->
                pgJobStore.addChunk(deliveryResultChunk1)
        );

        // Then...

        final JobEntity jobEntityAfterDeliveryOfChunk1 = entityManager.find(JobEntity.class, jobEntity.getId());
        assertThat("job timeOfCompletion after termination chunk delivered",
                jobEntityAfterDeliveryOfChunk1.getTimeOfCompletion(), is(notNullValue()));
    }

    /**
     * Given: a job store containing one job with two chunks each with one item
     *        and the last chunk is a termination chunk which has only completed its
     *        partitioning and processing phases
     *  When: adding failed result chunk for delivery phase for the termination chunk
     *  Then: the job is completed
     *   And: the job is in error
     */
    @Test
    public void addChunk_failsJobOnFailedTerminationChunk() throws FileStoreServiceConnectorException {
        final JobEntity jobEntity = newJobEntity();
        jobEntity.setNumberOfChunks(2);     // last chunk is termination chunk
        jobEntity.setNumberOfItems(2);      // last item is termination chunk item
        jobEntity.getState().getPhase(State.Phase.PARTITIONING)
                .withSucceeded(1)
                .withEndDate(new Date());
        jobEntity.getState().getPhase(State.Phase.PROCESSING)
                .withSucceeded(1)
                .withEndDate(new Date());

        persist(jobEntity);

        final ChunkEntity chunkEntity0 = newChunkEntity(new ChunkEntity.Key(0, jobEntity.getId()));
        chunkEntity0.getState().getPhase(State.Phase.PARTITIONING)
                .withSucceeded(1)
                .withEndDate(new Date());
        chunkEntity0.getState().getPhase(State.Phase.PROCESSING)
                .withSucceeded(1)
                .withEndDate(new Date());
        chunkEntity0.getState().getPhase(State.Phase.DELIVERING)
                .withSucceeded(1)
                .withEndDate(new Date());
        chunkEntity0.setNumberOfItems((short) 1);

        final ItemEntity itemEntity0_0 = newItemEntity(new ItemEntity.Key(jobEntity.getId(), 0, (short) 0));
        itemEntity0_0.getState().getPhase(State.Phase.PARTITIONING)
                .withSucceeded(1)
                .withEndDate(new Date());
        itemEntity0_0.getState().getPhase(State.Phase.PROCESSING)
                .withSucceeded(1)
                .withEndDate(new Date());
        itemEntity0_0.getState().getPhase(State.Phase.DELIVERING)
                .withSucceeded(1)
                .withEndDate(new Date());

        // Termination chunk
        final ChunkEntity chunkEntity1 = newChunkEntity(new ChunkEntity.Key(1, jobEntity.getId()));
        chunkEntity1.getState().getPhase(State.Phase.PARTITIONING)
                .withSucceeded(1)
                .withEndDate(new Date());
        chunkEntity1.getState().getPhase(State.Phase.PROCESSING)
                .withSucceeded(1)
                .withEndDate(new Date());
        chunkEntity1.setNumberOfItems((short) 1);

        final ItemEntity itemEntity1_0 = newItemEntity(new ItemEntity.Key(jobEntity.getId(), 1, (short) 0));
        itemEntity1_0.getState().getPhase(State.Phase.PARTITIONING)
                .withSucceeded(1)
                .withEndDate(new Date());
        itemEntity1_0.getState().getPhase(State.Phase.PROCESSING)
                .withSucceeded(1)
                .withEndDate(new Date());

        persist(chunkEntity0);
        persist(chunkEntity1);
        persist(itemEntity0_0);
        persist(itemEntity1_0);

        final PgJobStore pgJobStore = newPgJobStore();

        final Chunk deliveryResultChunk1 = new ChunkBuilder(Chunk.Type.DELIVERED)
                .setJobId(jobEntity.getId())
                .setChunkId(1)
                .setItems(Collections.singletonList(
                        new ChunkItemBuilder()
                                .setId(0)
                                .setData("ERROR")
                                .setStatus(ChunkItem.Status.FAILURE)
                                .setType(ChunkItem.Type.JOB_END)
                                .build()))
                .build();

        persistenceContext.run(() ->
                pgJobStore.addChunk(deliveryResultChunk1)
        );

        final JobEntity jobEntityAfterDeliveryOfChunk1 = entityManager.find(JobEntity.class, jobEntity.getId());
        assertThat("job timeOfCompletion after termination chunk delivered",
                jobEntityAfterDeliveryOfChunk1.getTimeOfCompletion(), is(notNullValue()));

        assertThat("job has fatal error", jobEntityAfterDeliveryOfChunk1.hasFatalError(), is(true));
    }

    /**
     * Given: a job store containing a job
     *
     * When : requesting next processing outcome
     * Then : the next processing outcome returned contains the the correct data.
     */
    @Test
    public void getNextProcessingOutcome() throws JobStoreException, FileStoreServiceConnectorException, FlowStoreServiceConnectorException {
        // Given...
        final int chunkId = 1;                  // second chunk is used, hence the chunk id is 1.
        final PgJobStore pgJobStore = newPgJobStore();

        setupExpectationOnGetByteSize(defaultByteSize);

        final JobInfoSnapshot jobInfoSnapshot = addJobs(1, pgJobStore).get(0);

        Chunk chunk = buildChunkWithNextItems(jobInfoSnapshot.getJobId(), chunkId, 1, Chunk.Type.PROCESSED, ChunkItem.Status.SUCCESS);

        final EntityTransaction chunkTransaction = entityManager.getTransaction();
        chunkTransaction.begin();
        pgJobStore.addChunk(chunk);
        chunkTransaction.commit();

        // When...
        final ItemEntity.Key successfulItemKey = new ItemEntity.Key(jobInfoSnapshot.getJobId(), chunkId, (short)0);
        final ItemEntity successfulItemEntity = entityManager.find(ItemEntity.class, successfulItemKey);
        ChunkItem chunkItem = pgJobStore.jobStoreRepository.getNextProcessingOutcome(successfulItemKey.getJobId(), successfulItemKey.getChunkId(), successfulItemKey.getId());

        // Then...
        assertThat("chunkItem", chunkItem, not(nullValue()));
        assertThat("chunkItem.data", chunkItem.getData(), is(successfulItemEntity.getNextProcessingOutcome().getData()));
    }

    /*
     * Private methods
     */

    private ChunkEntity newPersistedChunkEntityWithPartitioningCompleted(ChunkEntity.Key key, short numberOfItems) {
        final ChunkEntity chunkEntity = newChunkEntityWithPartitioningCompleted(key, numberOfItems);
        persist(chunkEntity);
        return chunkEntity;
    }

    private ChunkEntity newChunkEntityWithPartitioningCompleted(ChunkEntity.Key key, short numberOfItems) {
        final ChunkEntity chunkEntity = newChunkEntity(key);
        chunkEntity.getState().getPhase(State.Phase.PARTITIONING).withEndDate(new Date());
        chunkEntity.setNumberOfItems(numberOfItems);
        return chunkEntity;
    }

    private ItemEntity newPersistedItemEntityWithPartitioningCompleted(ItemEntity.Key key) {
        final ItemEntity itemEntity = newItemEntityWithPartitioningCompleted(key);
        persist(itemEntity);
        return itemEntity;
    }

    private ItemEntity newItemEntityWithPartitioningCompleted(ItemEntity.Key key) {
        final ItemEntity itemEntity = newItemEntity(key);
        itemEntity.getState().getPhase(State.Phase.PARTITIONING).withEndDate(new Date());
        return itemEntity;
    }

    private JobInfoSnapshot waitForJobCompletion(long jobId, PgJobStore pgJobStore) {
        final JobListCriteria criteria = new JobListCriteria().where(new ListFilter<>(JobListCriteria.Field.JOB_ID, ListFilter.Op.EQUAL, jobId));
        JobInfoSnapshot jobInfoSnapshot = null;
        // Wait for Job-completion
        long remainingWaitInMs = MAX_WAIT_IN_MS;

        LOGGER.info("AddJobIT.createJob_jobSpecificationReferencesHarvesterMarcxchangeDataFile_newJobIsCreated - waiting in MAX milliseconds: " + MAX_WAIT_IN_MS);
        LOGGER.info("AddJobIT.createJob_jobSpecificationReferencesHarvesterMarcxchangeDataFile_newJobIsCreated - sleeping in milliseconds: " + SLEEP_INTERVAL_IN_MS);


        while ( remainingWaitInMs > 0 ) {
            LOGGER.info("AddJobIT.createJob_jobSpecificationReferencesHarvesterMarcxchangeDataFile_newJobIsCreated - remaining wait in milliseconds: " + remainingWaitInMs);

            jobInfoSnapshot = pgJobStore.jobStoreRepository.listJobs(criteria).get(0);
            if (phasePartitioningDoneSuccessfully(jobInfoSnapshot)) {
                break;
            } else {
                try {
                    Thread.sleep(SLEEP_INTERVAL_IN_MS);
                    remainingWaitInMs -= SLEEP_INTERVAL_IN_MS;
                } catch (InterruptedException ex) {
                    break;
                }
            }
        }
        if (!phasePartitioningDoneSuccessfully(jobInfoSnapshot)) {
            throw new IllegalStateException(String.format("Job %d did not complete successfully in time",
                    jobInfoSnapshot.getJobId()));
        }

        return jobInfoSnapshot;
    }

    private boolean phasePartitioningDoneSuccessfully(JobInfoSnapshot jobInfoSnapshot) {
        final State state = jobInfoSnapshot.getState();
        return state.phaseIsDone(State.Phase.PARTITIONING);
    }

    private void setupExpectationOnGetByteSize(long expectedByteSize) throws FileStoreServiceConnectorException {
        when(mockedFileStoreServiceConnector.getByteSize(anyString())).thenReturn(expectedByteSize);
    }

    private PgJobStore newPgJobStore() throws FileStoreServiceConnectorException {

        // Subjects Under Test -> hence no mocks!
        final PgJobStore pgJobStore = new PgJobStore();
        pgJobStore.entityManager = entityManager;

        pgJobStore.jobStoreRepository = new PgJobStoreRepository();
        pgJobStore.jobStoreRepository.entityManager = entityManager;

        pgJobStore.jobQueueRepository = new JobQueueRepository();
        pgJobStore.jobQueueRepository.entityManager = entityManager;

        pgJobStore.jobNotificationRepository = new JobNotificationRepository();
        pgJobStore.jobNotificationRepository.entityManager = entityManager;

        // Mocks
        pgJobStore.jobSchedulerBean = JOB_SCHEDULER_BEAN;
        pgJobStore.flowStoreServiceConnectorBean = mockedFlowStoreServiceConnectorBean;
        pgJobStore.fileStoreServiceConnectorBean = mockedFileStoreServiceConnectorBean;
        pgJobStore.sessionContext = mockedSessionContext;

        when(mockedFileStoreServiceConnectorBean.getConnector()).thenReturn(mockedFileStoreServiceConnector);
        when(mockedFlowStoreServiceConnectorBean.getConnector()).thenReturn(mockedFlowStoreServiceConnector);
        when(mockedFileStoreServiceConnector.getFile(anyString())).thenReturn(new ByteArrayInputStream(defaultXml));
        when(mockedSessionContext.getBusinessObject(PgJobStore.class)).thenReturn(pgJobStore);

        return pgJobStore;
    }

    private List<JobInfoSnapshot> addJobs(int numberOfJobs, PgJobStore pgJobStore) throws FileStoreServiceConnectorException, FlowStoreServiceConnectorException {
        List<JobInfoSnapshot> snapshots = new ArrayList<>(numberOfJobs);
        for (int i = 0; i < numberOfJobs; i++) {
            final TestableAddJobParam addJobParam = new TestableAddJobParamBuilder()
                    .setJobSpecification(createJobSpecification())
                    .setSubmitter(new SubmitterBuilder().build())
                    .build();
            when(mockedFileStoreServiceConnector.getFile(anyString())).thenReturn(new ByteArrayInputStream(defaultXml));
            when(mockedFlowStoreServiceConnector.getSubmitter(anyLong())).thenReturn(addJobParam.getSubmitter());
            JobInfoSnapshot jobInfoSnapshot = commitJob(pgJobStore, addJobParam);
            snapshots.add(jobInfoSnapshot);
        }
        return snapshots;
    }

    private JobInfoSnapshot commitJob(PgJobStore pgJobStore, TestableAddJobParam testableAddJobParam) {
        return persistenceContext.run(() ->
                pgJobStore.addJob(testableAddJobParam)
        );
    }

    private void setupSuccessfulMockedReturnsFromFlowStore(TestableAddJobParam testableAddJobParam) throws FlowStoreServiceConnectorException {
        when(mockedFlowStoreServiceConnector.getFlow(anyLong())).thenReturn(testableAddJobParam.getFlow());
        when(mockedFlowStoreServiceConnector.getSink(anyLong())).thenReturn(testableAddJobParam.getSink());
        when(mockedFlowStoreServiceConnector.getSubmitterBySubmitterNumber(anyLong())).thenReturn(testableAddJobParam.getSubmitter());
        when(mockedFlowStoreServiceConnector.getFlowBinder(
                anyString(),
                anyString(),
                anyString(),
                anyLong(),
                anyString())).
                thenReturn(testableAddJobParam.getFlowBinder());
    }

    private void assertTableSizes(int expectedJobTableSize, int expectedChunkTableSize, int expectedItemTableSize) throws SQLException {
        assertThat("Job table size", getSizeOfTable(JOB_TABLE_NAME), is((long) expectedJobTableSize));
        assertThat("Chunk table size", getSizeOfTable(CHUNK_TABLE_NAME), is((long) expectedChunkTableSize));
        assertThat("Item table size", getSizeOfTable(ITEM_TABLE_NAME), is((long) expectedItemTableSize));
    }

    private void assertEntities(JobEntity jobEntity, int expectedNumberOfChunks, int expectedNumberOfItems, List<State.Phase> phases) {
        assertJobEntity(jobEntity, expectedNumberOfChunks, expectedNumberOfItems, phases);
        for (int chunkId = 0; chunkId < expectedNumberOfChunks; chunkId++) {
            final short expectedNumberOfChunkItems = expectedNumberOfItems / ((chunkId + 1) * MAX_CHUNK_SIZE) > 0 ? MAX_CHUNK_SIZE
                    : (short) (expectedNumberOfItems - (chunkId * MAX_CHUNK_SIZE));
            final ChunkEntity.Key chunkKey = new ChunkEntity.Key(chunkId, jobEntity.getId());
            final ChunkEntity chunkEntity = entityManager.find(ChunkEntity.class, chunkKey);
            assertChunkEntity(chunkEntity, chunkKey, expectedNumberOfChunkItems, phases);

            for (short itemId = 0; itemId < expectedNumberOfChunkItems; itemId++) {
                final ItemEntity.Key itemKey = new ItemEntity.Key(jobEntity.getId(), chunkId, itemId);
                final ItemEntity itemEntity = entityManager.find(ItemEntity.class, new ItemEntity.Key(jobEntity.getId(), chunkId, itemId));
                entityManager.refresh(itemEntity);
                assertItemEntity(itemEntity, itemKey, phases);
            }
        }
    }

    private void assertJobEntity(JobEntity jobEntity, int numberOfChunks, int numberOfItems, List<State.Phase> phasesDone) {
        final String jobLabel = String.format("JobEntity[%d]:", jobEntity.getId());
        assertThat(String.format("%s", jobLabel), jobEntity, is(notNullValue()));
        assertThat(String.format("%s number of chunks created", jobLabel), jobEntity.getNumberOfChunks(), is(numberOfChunks));
        assertThat(String.format("%s number of items created", jobLabel), jobEntity.getNumberOfItems(), is(numberOfItems));
        assertThat(String.format("%s time of creation", jobLabel), jobEntity.getTimeOfCreation(), is(notNullValue()));
        assertThat(String.format("%s time of last modification", jobLabel), jobEntity.getTimeOfLastModification(), is(notNullValue()));
        if(phasesDone.isEmpty()) {
            assertThat(String.format("%s time of completion", jobLabel), jobEntity.getTimeOfCompletion(), is(notNullValue()));
        }
        for (State.Phase phase : phasesDone) {
            assertThat(String.format("%s %s phase done", jobLabel, phase), jobEntity.getState().phaseIsDone(phase), is(true));
        }
    }

    private void assertChunkEntity(ChunkEntity chunkEntity, ChunkEntity.Key key, short numberOfItems, List<State.Phase> phasesDone) {
        final String chunkLabel = String.format("ChunkEntity[%d,%d]:", key.getJobId(), key.getId());
        assertThat(String.format("%s", chunkLabel), chunkEntity, is(notNullValue()));
        assertThat(String.format("%s number of items", chunkLabel), chunkEntity.getNumberOfItems(), is(numberOfItems));
        assertThat(String.format("%s time of creation", chunkLabel), chunkEntity.getTimeOfCreation(), is(notNullValue()));
        assertThat(String.format("%s time of last modification", chunkLabel), chunkEntity.getTimeOfLastModification(), is(notNullValue()));
        assertThat(String.format("%s sequence analysis data", chunkLabel), chunkEntity.getSequenceAnalysisData().getData().isEmpty(), is(true));
        for (State.Phase phase : phasesDone) {
            assertThat(String.format("%s %s phase done", chunkLabel, phase), chunkEntity.getState().phaseIsDone(phase), is(true));
        }
    }

    private void assertItemEntity(ItemEntity itemEntity, ItemEntity.Key key, List<State.Phase> phasesDone) {
        final String itemLabel = String.format("ItemEntity[%d,%d,%d]:", key.getJobId(), key.getChunkId(), key.getId());
        assertThat(String.format("%s", itemLabel), itemEntity, is(notNullValue()));
        assertThat(String.format("%s time of creation", itemLabel), itemEntity.getTimeOfCreation(), is(notNullValue()));
        assertThat(String.format("%s time of last modification", itemLabel), itemEntity.getTimeOfLastModification(), is(notNullValue()));
        for (State.Phase phase : phasesDone) {
            assertThat(String.format("%s %s phase done", itemLabel, phase), itemEntity.getState().phaseIsDone(phase), is(true));
            switch (phase) {
                case PARTITIONING:
                    assertThat(String.format("%s %s data", itemLabel, phase), itemEntity.getPartitioningOutcome(), is(notNullValue()));
                    break;
                case PROCESSING:
                    assertThat(String.format("%s %s data", itemLabel, phase), itemEntity.getProcessingOutcome(), is(notNullValue()));
                    break;
                case DELIVERING:
                    assertThat(String.format("%s %S data", itemLabel, phase), itemEntity.getDeliveringOutcome(), is(notNullValue()));
                    break;
            }
        }
    }

    private Chunk buildChunk(long jobId, long chunkId, int numberOfItems, Chunk.Type type, ChunkItem.Status status) {
        List<ChunkItem> items = new ArrayList<>();
        for(long i = 0; i < numberOfItems; i++) {
            items.add(new ChunkItemBuilder().setId(i).setData(getData(type)).setStatus(status).build());
        }
        return new ChunkBuilder(type).setJobId(jobId).setChunkId(chunkId).setItems(items).build();
    }


    private Chunk buildChunkWithNextItems(long jobId, long chunkId, int numberOfItems, Chunk.Type type, ChunkItem.Status status) {
        List<ChunkItem> items = new ArrayList<>();
        List<ChunkItem> nextItems = new ArrayList<>();

        for(long i = 0; i < numberOfItems; i++) {
            items.add(new ChunkItemBuilder().setId(i).setData(getData(type)).setStatus(status).build());
            nextItems.add(new ChunkItemBuilder().setId(i).setData("next:" + getData(type)).setStatus(status).build());
        }
        return new ChunkBuilder(type).setJobId(jobId).setChunkId(chunkId).setItems(items).setNextItems(nextItems).build();
    }

    private void assertFailedItemEntity(ItemEntity itemEntity, State.Phase phase, Chunk.Type type) {
        assertThat("itemEntity -> number of failed items", itemEntity.getState().getPhase(phase).getFailed(), is(1));
        assertThat("itemEntity phase completed", itemEntity.getState().phaseIsDone(phase), is(true));
        assertThat("itemEntity.data", StringUtil.asString(getChunkItemOutcome(itemEntity, type).getData()), is(getData(type)));
    }

    private void assertIgnoredItemEntity(ItemEntity itemEntity, State.Phase phase, Chunk.Type type) {
        assertThat("itemEntity -> number of ignored items", itemEntity.getState().getPhase(phase).getIgnored(), is(1));
        assertThat("itemEntity phase completed", itemEntity.getState().phaseIsDone(phase), is(true));
        assertThat("itemEntity.data", StringUtil.asString(getChunkItemOutcome(itemEntity, type).getData()), is(getData(type)));
    }

    private void assertSucceededItemEntity(ItemEntity itemEntity, State.Phase phase, Chunk.Type type) {
        assertThat("itemEntity -> number of succeeded items", itemEntity.getState().getPhase(phase).getSucceeded(), is(1));
        assertThat("itemEntity phase completed", itemEntity.getState().phaseIsDone(phase), is(true));
        assertThat("itemEntity.data", StringUtil.asString(getChunkItemOutcome(itemEntity, type).getData()), is(getData(type)));
    }

    ChunkItem getChunkItemOutcome(ItemEntity itemEntity, Chunk.Type type) {
        switch (type) {
            case PARTITIONED: return itemEntity.getPartitioningOutcome();
            case PROCESSED: return itemEntity.getProcessingOutcome();
            case DELIVERED: return itemEntity.getDeliveringOutcome();
            default: throw new IllegalStateException("uknown chunk type");
        }
    }

    private String getData(Chunk.Type type) {
        switch (type) {
            case PARTITIONED:
                return "partitioned test data";
            case PROCESSED:
                return "processed test data";
            default:
                return "delivered test data";
        }
    }

    private State assertAndReturnChunkState(int jobId, int chunkId, int succeeded, State.Phase phase, boolean isPhaseDone) {
        final ChunkEntity.Key chunkKey = new ChunkEntity.Key(chunkId, jobId);
        final ChunkEntity chunkEntity = entityManager.find(ChunkEntity.class, chunkKey);
        State chunkState = chunkEntity.getState();
        assertThat(chunkState.getPhase(phase).getSucceeded(), is(succeeded));
        assertThat(chunkState.phaseIsDone(phase), is(isPhaseDone));
        return chunkState;
    }

    private State assertAndReturnItemState(int jobId, int chunkId, short itemId, int succeeded, State.Phase phase, boolean isPhaseDone) {
        final ItemEntity.Key itemKey = new ItemEntity.Key(jobId, chunkId, itemId);
        final ItemEntity itemEntity = entityManager.find(ItemEntity.class, itemKey);
        State itemState = itemEntity.getState();
        assertThat(itemState.getPhase(phase).getSucceeded(), is(succeeded));
        assertThat(itemState.phaseIsDone(phase), is(isPhaseDone));
        if(isPhaseDone) {
            assertThat(StringUtil.asString(itemEntity.getProcessingOutcome().getData()), is (getData(Chunk.Type.PROCESSED)));
        }
        return itemState;
    }
}