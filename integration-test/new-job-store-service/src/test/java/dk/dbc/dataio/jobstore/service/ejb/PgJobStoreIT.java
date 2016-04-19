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
import dk.dbc.dataio.commons.utils.lang.StringUtil;
import dk.dbc.dataio.commons.utils.test.model.ChunkBuilder;
import dk.dbc.dataio.commons.utils.test.model.ChunkItemBuilder;
import dk.dbc.dataio.commons.utils.test.model.DiagnosticBuilder;
import dk.dbc.dataio.filestore.service.connector.FileStoreServiceConnectorException;
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
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PgJobStoreIT extends AbstractJobStoreIT {

    private static final long SLEEP_INTERVAL_IN_MS = 1000;
    private static final long MAX_WAIT_IN_MS = 10000;
    private static final Logger LOGGER = LoggerFactory.getLogger(PgJobStoreIT.class);
    private static final JobSchedulerBean JOB_SCHEDULER_BEAN = mock(JobSchedulerBean.class);

    private static final State.Phase PROCESSING = State.Phase.PROCESSING;
    private static final int MAX_CHUNK_SIZE = 10;

    private final String defaultXml = "<records>"
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
            + "</records>";

    private final long defaultByteSize = defaultXml.getBytes().length;

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

        final TestableAddJobParam testableAddJobParam = new TestableAddJobParamBuilder().build();

        // Setup mocks
        setupSuccessfulMockedReturnsFromFlowStore(testableAddJobParam);

        // Set up mocked return for identical byte sizes
        when(mockedFileStoreServiceConnector.getByteSize(anyString())).thenReturn((long) testableAddJobParam.getRecords().getBytes(StandardCharsets.UTF_8).length);

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

        final TestableAddJobParam testableAddJobParam = new TestableAddJobParamBuilder().build();
        final long dataPartitionerByteSize = testableAddJobParam.getRecords().getBytes(StandardCharsets.UTF_8).length;

        // Setup mocks
        setupSuccessfulMockedReturnsFromFlowStore(testableAddJobParam);

        // Set up mocked return for different byte sizes
        when(mockedFileStoreServiceConnector.getByteSize(anyString())).thenReturn(fileStoreByteSize);

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
    public void addJob_failsFastDueToAddJobParamFailures_jobWithFatalErrorIsCreated() throws JobStoreException, SQLException, FileStoreServiceConnectorException {
        // Given...
        final PgJobStore pgJobStore = newPgJobStore();
        final int expectedNumberOfJobs = 1;
        final int expectedNumberOfChunks = 0;
        final int expectedNumberOfItems = 0;

        // When...
        final TestableAddJobParam testableAddJobParam = new TestableAddJobParamBuilder()
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
     * When : adding a job which fails immediately during partitioning
     * Then : a new job entity with a fatal diagnostic is created
     */
    @Test
    public void addJob_failsFastDuringCreation_jobWithFatalErrorIsAdded() throws JobStoreException, FileStoreServiceConnectorException {
        // Given...
        final PgJobStore pgJobStore = newPgJobStore();
        final TestableAddJobParam testableAddJobParam = new TestableAddJobParamBuilder().build();

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
    public void addJob_failsEventuallyDuringPartitioning_jobWithFatalErrorIsAdded() throws JobStoreException, FileStoreServiceConnectorException {
        // Given...
        final PgJobStore pgJobStore = newPgJobStore();

        final TestableAddJobParam testableAddJobParam = new TestableAddJobParamBuilder().setRecords(getInvalidXml()).build();

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
     * When : adding a job with addJobParam.level.WARNING as input
     * Then : a new job entity and the required number of chunk and item entities are created
     * And  : a diagnostic with level WARNING is set on the state of the job entity
     */
    @Test
    public void addJob_withWarningDiagnostic_jobIsAdded() throws FileStoreServiceConnectorException, JobStoreException, SQLException {
        // Given...
        final PgJobStore pgJobStore = newPgJobStore();
        final int expectedNumberOfJobs = 1;
        final int expectedNumberOfChunks = 2;
        final int expectedNumberOfItems = 11;

        // When...
        setupExpectationOnGetByteSize(defaultByteSize);

        final TestableAddJobParam testableAddJobParam = new TestableAddJobParamBuilder()
                .setDiagnostics(Collections.singletonList(new DiagnosticBuilder().setLevel(Diagnostic.Level.WARNING).build()))
                .build();

        final JobInfoSnapshot jobInfoSnapshot = commitJob(pgJobStore, testableAddJobParam);

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
    public void addJob() throws JobStoreException, SQLException, FileStoreServiceConnectorException {
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
     * When : a chunk is added
     * Then : the job info snapshot is updated
     * And  : the referenced entities are updated
     */
    @Test
    public void addChunk() throws JobStoreException, SQLException, FileStoreServiceConnectorException {
        // Given...
        final PgJobStore pgJobStore = newPgJobStore();
        final int chunkId = 1;                   // second chunk is used, hence the chunk id is 1.
        final short itemId = 0;                  // The second chunk contains only one item, hence the item id is 0.

        setupExpectationOnGetByteSize(defaultByteSize);
        final JobInfoSnapshot jobInfoSnapshotNewJob = addJobs(1, pgJobStore).get(0);

        assertThat(jobInfoSnapshotNewJob, not(nullValue()));

        // Validate that nothing has been processed on job level
        assertThat(jobInfoSnapshotNewJob.getState().getPhase(PROCESSING).getSucceeded(), is(0));

        // Validate that nothing has been processed on chunk level
        assertAndReturnChunkState(jobInfoSnapshotNewJob.getJobId(), chunkId, 0, PROCESSING, false);

        // Validate that nothing has been processed on item level
        assertAndReturnItemState(jobInfoSnapshotNewJob.getJobId(), chunkId, itemId, 0, PROCESSING, false);

        Chunk chunk = buildChunk(
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
        assertThat(jobInfoSnapShotUpdatedJob.getState().getPhase(PROCESSING).getSucceeded(), is(1));
        LOGGER.info("new-job: {} updated-job: {}", jobInfoSnapshotNewJob.getTimeOfLastModification().getTime(), jobInfoSnapShotUpdatedJob.getTimeOfLastModification().getTime());
        assertThat(jobInfoSnapShotUpdatedJob.getTimeOfLastModification().after(jobInfoSnapshotNewJob.getTimeOfLastModification()), is(true));

        // And...

        // Validate that one chunk has been processed on chunk level
        assertAndReturnChunkState(jobInfoSnapShotUpdatedJob.getJobId(), chunkId, 1, PROCESSING, true);

        // Validate that one chunk has been processed on item level
        assertAndReturnItemState(jobInfoSnapShotUpdatedJob.getJobId(), chunkId, itemId, 1, PROCESSING, true);
    }


    /**
     * Given: a job store where a job is added
     * When : an chunk with Next processing Data is added
     * Then : the job info snapshot is updated
     * And  : the referenced entities are updated
     */
    @Test
    public void addChunk_whenChunkHasNextEntry_chunkIsAdded() throws JobStoreException, SQLException, FileStoreServiceConnectorException {
        // Given...
        final PgJobStore pgJobStore = newPgJobStore();
        final int chunkId = 1;                   // second chunk is used, hence the chunk id is 1.
        final short itemId = 0;                  // The second chunk contains only one item, hence the item id is 0.

        setupExpectationOnGetByteSize(defaultByteSize);
        final JobInfoSnapshot jobInfoSnapshotNewJob = addJobs(1, pgJobStore).get(0);

        assertThat(jobInfoSnapshotNewJob, not(nullValue()));

        // Validate that nothing has been processed on job level
        assertThat(jobInfoSnapshotNewJob.getState().getPhase(PROCESSING).getSucceeded(), is(0));

        // Validate that nothing has been processed on chunk level
        assertAndReturnChunkState(jobInfoSnapshotNewJob.getJobId(), chunkId, 0, PROCESSING, false);

        // Validate that nothing has been processed on item level
        assertAndReturnItemState(jobInfoSnapshotNewJob.getJobId(), chunkId, itemId, 0, PROCESSING, false);

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
        assertThat(jobInfoSnapShotUpdatedJob.getState().getPhase(PROCESSING).getSucceeded(), is(1));
        LOGGER.info("new-job: {} updated-job: {}", jobInfoSnapshotNewJob.getTimeOfLastModification().getTime(), jobInfoSnapShotUpdatedJob.getTimeOfLastModification().getTime());
        assertThat(jobInfoSnapShotUpdatedJob.getTimeOfLastModification().after(jobInfoSnapshotNewJob.getTimeOfLastModification()), is(true));

        // And...

        // Validate that one chunk has been processed on chunk level
        assertAndReturnChunkState(jobInfoSnapShotUpdatedJob.getJobId(), chunkId, 1, PROCESSING, true);

        // Validate that one chunk has been processed on item level
        assertAndReturnItemState(jobInfoSnapShotUpdatedJob.getJobId(), chunkId, itemId, 1, PROCESSING, true);

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
    public void addChunk_duplicateChunksAdded_throws() throws JobStoreException, FileStoreServiceConnectorException {
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
                    assertThat("ItemEmtity not updated", itemEntitySecondAddChunk, is(itemEntities.get(i)));
            }
        }
    }

    /**
     * Given: a non-empty jobstore
     * Then : a chunks can be retrieved
     */
    @Test
    public void getChunk() throws JobStoreException, FileStoreServiceConnectorException {
        // Given...
        final PgJobStore pgJobStore = newPgJobStore();
        final EntityTransaction transaction = entityManager.getTransaction();

        final TestableAddJobParam testableAddJobParam = new TestableAddJobParamBuilder().build();
        setupExpectationOnGetByteSize(testableAddJobParam.getRecords().getBytes().length);
        transaction.begin();
        final JobInfoSnapshot jobInfoSnapshot = pgJobStore.addJob(testableAddJobParam);
        transaction.commit();

        // Then...
        final Chunk chunk0 = pgJobStore.jobStoreRepository.getChunk(Chunk.Type.PARTITIONED, jobInfoSnapshot.getJobId(), 0);
        assertThat("chunk0", chunk0, is(notNullValue()));
        assertThat("chunk0.size()", chunk0.size(), is(10));
        final Chunk chunk1 = pgJobStore.jobStoreRepository.getChunk(Chunk.Type.PARTITIONED, jobInfoSnapshot.getJobId(), 1);
        assertThat("chunk1", chunk1, is(notNullValue()));
        assertThat("chunk1.size()", chunk1.size(), is(1));
    }

    /**
     * Given: a job store where a job exists and where:
     *          10 items have been successfully partitioned.
     * When : requesting item data for the existing job for phase: PARTITIONING
     * Then : the item data is returned and contains the the correct data.
     */
    @Test
    public void getItemDataPartitioned() throws JobStoreException, FileStoreServiceConnectorException {
        final int chunkId = 0;                  // first chunk is used, hence the chunk id is 0.
        final short itemId = 3;
        // Given...
        final PgJobStore pgJobStore = newPgJobStore();

        setupExpectationOnGetByteSize(defaultByteSize);
        final JobInfoSnapshot jobInfoSnapshot = addJobs(1, pgJobStore).get(0);

        assertThat(jobInfoSnapshot, not(nullValue()));

        final ItemEntity.Key itemKey = new ItemEntity.Key(jobInfoSnapshot.getJobId(), chunkId, itemId);
        final ItemEntity itemEntity = entityManager.find(ItemEntity.class, itemKey);

        // When...
        ChunkItem chunkItem = pgJobStore.jobStoreRepository.getChunkItemForPhase(itemKey.getJobId(), itemKey.getChunkId(), itemKey.getId(), State.Phase.PARTITIONING);

        // Then...
        assertThat("chunkItem", chunkItem, not(nullValue()));
        assertThat("chunkItem.data", chunkItem.getData(), is(itemEntity.getPartitioningOutcome().getData()));
    }

    /**
     * Given: a job store where a job exists and where:
     *          10 items have been successfully partitioned.
     *          8 items have been successfully processed.
     *          1 item has failed in processing.
     *          1 item has been ignored in processing.
     *
     * When : requesting the data for the item failed in processing.
     * Then : the item data returned contains the the correct data.
     *
     * And when : requesting the data for one of the successful items.
     * Then : the item data returned contains the the correct data.
     *
     * And when : requesting the data for the item ignored in processing.
     * Then : the item data returned contains the the correct data.
     */
    @Test
    public void getItemDataProcessed() throws JobStoreException, FileStoreServiceConnectorException {
        // Given...
        final int chunkId = 0;                  // first chunk is used, hence the chunk id is 0.
        final short failedItemId = 3;          // The failed item will be the 4th out of 10
        final short ignoredItemId = 4;         // The ignored item is the 5th out of 10
        final short successfulItemId = 0;
        final PgJobStore pgJobStore = newPgJobStore();

        setupExpectationOnGetByteSize(defaultByteSize);
        final JobInfoSnapshot jobInfoSnapshot = addJobs(1, pgJobStore).get(0);

        Chunk chunk = buildChunkContainingFailedAndIgnoredItem(
                10, jobInfoSnapshot.getJobId(), chunkId, failedItemId, ignoredItemId, Chunk.Type.PROCESSED);

        final EntityTransaction chunkTransaction = entityManager.getTransaction();
        chunkTransaction.begin();
        pgJobStore.addChunk(chunk);
        chunkTransaction.commit();

        // When...
        final ItemEntity.Key failedItemKey = new ItemEntity.Key(jobInfoSnapshot.getJobId(), chunkId, failedItemId);
        final ItemEntity failedItemEntity = entityManager.find(ItemEntity.class, failedItemKey);
        ChunkItem failedChunkItem = pgJobStore.jobStoreRepository.getChunkItemForPhase(failedItemKey.getJobId(), failedItemKey.getChunkId(), failedItemKey.getId(), State.Phase.PROCESSING);

        // Then...
        assertThat("chunkItem", failedChunkItem, not(nullValue()));
        assertThat("chunkItem.data", failedChunkItem.getData(), is(failedItemEntity.getProcessingOutcome().getData()));

        // And when...
        final ItemEntity.Key successfulItemKey = new ItemEntity.Key(jobInfoSnapshot.getJobId(), chunkId, successfulItemId);
        final ItemEntity successfulItemEntity = entityManager.find(ItemEntity.class, successfulItemKey);
        ChunkItem successfulChunkItem = pgJobStore.jobStoreRepository.getChunkItemForPhase(successfulItemKey.getJobId(), successfulItemKey.getChunkId(), successfulItemKey.getId(), State.Phase.PROCESSING);

        // Then...
        assertThat("chunkItem", successfulItemEntity, not(nullValue()));
        assertThat("chunkItem.data", successfulChunkItem.getData(), is(successfulItemEntity.getProcessingOutcome().getData()));

        // And when...
        final ItemEntity.Key ignoredItemKey = new ItemEntity.Key(jobInfoSnapshot.getJobId(), chunkId, ignoredItemId);
        final ItemEntity ignoredItemEntity = entityManager.find(ItemEntity.class, ignoredItemKey);
        ChunkItem ignoredChunkItem = pgJobStore.jobStoreRepository.getChunkItemForPhase(ignoredItemKey.getJobId(), ignoredItemKey.getChunkId(), ignoredItemKey.getId(), State.Phase.PROCESSING);

        // Then...
        assertThat("chunkItem", ignoredItemEntity, not(nullValue()));
        assertThat("chunkItem.data", ignoredChunkItem.getData(), is(ignoredItemEntity.getProcessingOutcome().getData()));
    }

    /**
     * Given: a job store where a job exists and:
     *          10 items have been successfully partitioned.
     *          10 items have been successfully processed.
     *          8 items have been successfully delivered.
     *          1 item has failed in delivering.
     *          1 item has been ignored in delivering.
     *
     * When : requesting the data for the item failed in delivering.
     * Then : the item data returned contains the the correct data.
     *
     * And when : requesting the data for one of the successful items.
     * Then : the item data returned contains the the correct, data.
     *
     * And when : requesting the data for the item ignored in delivering.
     * Then : the item data returned contains the the correct data.
     */
    @Test
    public void getItemDataDelivered() throws JobStoreException, FileStoreServiceConnectorException {
        // Given...
        final int chunkId = 0;                  // first chunk is used, hence the chunk id is 0.
        final short failedItemId = 3;          // The failed item will be the 4th out of 10
        final short ignoredItemId = 4;         // The ignored item is the 5th out of 10
        final short successfulItemId = 0;
        final PgJobStore pgJobStore = newPgJobStore();

        setupExpectationOnGetByteSize(defaultByteSize);
        final JobInfoSnapshot jobInfoSnapshot = addJobs(1, pgJobStore).get(0);

        Chunk processedChunk = buildChunk(jobInfoSnapshot.getJobId(), chunkId, 10, Chunk.Type.PROCESSED, ChunkItem.Status.SUCCESS);

        final EntityTransaction chunkTransaction = entityManager.getTransaction();
        chunkTransaction.begin();
        pgJobStore.addChunk(processedChunk);
        chunkTransaction.commit();

        Chunk deliveredChunk = buildChunkContainingFailedAndIgnoredItem(
                10, jobInfoSnapshot.getJobId(), chunkId, failedItemId, ignoredItemId, Chunk.Type.DELIVERED);

        chunkTransaction.begin();
        pgJobStore.addChunk(deliveredChunk);
        chunkTransaction.commit();

        // When...
        final ItemEntity.Key failedItemKey = new ItemEntity.Key(jobInfoSnapshot.getJobId(), chunkId, failedItemId);
        final ItemEntity failedItemEntity = entityManager.find(ItemEntity.class, failedItemKey);
        ChunkItem failedChunkItem = pgJobStore.jobStoreRepository.getChunkItemForPhase(failedItemKey.getJobId(), failedItemKey.getChunkId(), failedItemKey.getId(), State.Phase.DELIVERING);

        // Then...
        assertThat("chunkItem", failedChunkItem, not(nullValue()));
        assertThat("chunkItem.data", failedChunkItem.getData(), is(failedItemEntity.getDeliveringOutcome().getData()));

        // When...
        final ItemEntity.Key successfulItemKey = new ItemEntity.Key(jobInfoSnapshot.getJobId(), chunkId, successfulItemId);
        final ItemEntity successfulItemEntity = entityManager.find(ItemEntity.class, successfulItemKey);
        ChunkItem successfulChunkItem = pgJobStore.jobStoreRepository.getChunkItemForPhase(successfulItemKey.getJobId(), successfulItemKey.getChunkId(), successfulItemKey.getId(), State.Phase.DELIVERING);

        // Then...
        assertThat("chunkItem", successfulChunkItem, not(nullValue()));
        assertThat("chunkItem.data", successfulChunkItem.getData(), is(successfulItemEntity.getDeliveringOutcome().getData()));

        // When...
        final ItemEntity.Key ignoredItemKey = new ItemEntity.Key(jobInfoSnapshot.getJobId(), chunkId, ignoredItemId);
        final ItemEntity ignoredItemEntity = entityManager.find(ItemEntity.class, ignoredItemKey);
        ChunkItem ignoredChunkItem = pgJobStore.jobStoreRepository.getChunkItemForPhase(ignoredItemKey.getJobId(), ignoredItemKey.getChunkId(), ignoredItemKey.getId(), State.Phase.DELIVERING);

        // Then...
        assertThat("chunkItem", ignoredChunkItem, not(nullValue()));
        assertThat("chunkItem.data", ignoredChunkItem.getData(), is(ignoredItemEntity.getDeliveringOutcome().getData()));
    }

    /**
     * Given: a job store containing a job
     *
     * When : requesting next processing outcome
     * Then : the next processing outcome returned contains the the correct data.
     */
    @Test
    public void getNextProcessingOutcome() throws JobStoreException, FileStoreServiceConnectorException {
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
        when(mockedFileStoreServiceConnector.getFile(anyString())).thenReturn(new ByteArrayInputStream(defaultXml.getBytes(StandardCharsets.UTF_8)));
        when(mockedSessionContext.getBusinessObject(PgJobStore.class)).thenReturn(pgJobStore);

        return pgJobStore;
    }

    private List<JobInfoSnapshot> addJobs(int numberOfJobs, PgJobStore pgJobStore) throws JobStoreException, FileStoreServiceConnectorException {
        List<JobInfoSnapshot> snapshots = new ArrayList<>(numberOfJobs);
        for (int i = 0; i < numberOfJobs; i++) {
            when(mockedFileStoreServiceConnector.getFile(anyString())).thenReturn(new ByteArrayInputStream(defaultXml.getBytes(StandardCharsets.UTF_8)));
            JobInfoSnapshot jobInfoSnapshot = commitJob(pgJobStore, new TestableAddJobParamBuilder().build());
            snapshots.add(jobInfoSnapshot);
        }
        return snapshots;
    }

    private JobInfoSnapshot commitJob(PgJobStore pgJobStore, TestableAddJobParam testableAddJobParam) throws JobStoreException {
        final EntityTransaction jobTransaction = entityManager.getTransaction();
        jobTransaction.begin();
        final JobInfoSnapshot jobInfoSnapshot = pgJobStore.addJob(testableAddJobParam);
        jobTransaction.commit();
        return jobInfoSnapshot;
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

    private Chunk buildChunkContainingFailedAndIgnoredItem(int numberOfItems, long jobId, long chunkId, long failedItemId, long ignoredItemId, Chunk.Type type) {
        List<ChunkItem> items = new ArrayList<>(numberOfItems);
        for(int i = 0; i < numberOfItems; i++) {
            if(i == failedItemId) {
                items.add(new ChunkItemBuilder().setId(i).setData(getData(type)).setStatus(ChunkItem.Status.FAILURE).build());
            } else if( i == ignoredItemId) {
                items.add(new ChunkItemBuilder().setId(i).setData(getData(type)).setStatus(ChunkItem.Status.IGNORE).build());
            } else {
                items.add(new ChunkItemBuilder().setId(i).setData(getData(type)).setStatus(ChunkItem.Status.SUCCESS).build());
            }
        }
        return new ChunkBuilder(type).setJobId(jobId).setChunkId(chunkId).setItems(items).build();
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

    private String getInvalidXml() {
        return "<records>"
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
                + "<record>eleventh"
                + "</records>";
    }
}