package dk.dbc.dataio.jobstore.service.ejb;


import dk.dbc.dataio.commons.types.Chunk;
import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.types.Flow;
import dk.dbc.dataio.commons.types.JobSpecification;
import dk.dbc.dataio.commons.types.Sink;
import dk.dbc.dataio.commons.utils.lang.StringUtil;
import dk.dbc.dataio.commons.utils.test.model.ChunkBuilder;
import dk.dbc.dataio.commons.utils.test.model.ChunkItemBuilder;
import dk.dbc.dataio.commons.utils.test.model.FlowBuilder;
import dk.dbc.dataio.commons.utils.test.model.SinkBuilder;
import dk.dbc.dataio.jobstore.service.dependencytracking.DefaultKeyGenerator;
import dk.dbc.dataio.jobstore.service.dependencytracking.KeyGenerator;
import dk.dbc.dataio.jobstore.service.entity.ChunkEntity;
import dk.dbc.dataio.jobstore.service.entity.ItemEntity;
import dk.dbc.dataio.jobstore.service.entity.JobEntity;
import dk.dbc.dataio.jobstore.service.partitioner.DanMarc2LineFormatDataPartitioner;
import dk.dbc.dataio.jobstore.service.partitioner.DataPartitioner;
import dk.dbc.dataio.jobstore.service.partitioner.DefaultXmlDataPartitioner;
import dk.dbc.dataio.jobstore.test.types.FlowStoreReferencesBuilder;
import dk.dbc.dataio.jobstore.types.DuplicateChunkException;
import dk.dbc.dataio.jobstore.types.FlowStoreReferences;
import dk.dbc.dataio.jobstore.types.InvalidInputException;
import dk.dbc.dataio.jobstore.types.JobError;
import dk.dbc.dataio.jobstore.types.JobInfoSnapshot;
import dk.dbc.dataio.jobstore.types.JobInputStream;
import dk.dbc.dataio.jobstore.types.JobStoreException;
import dk.dbc.dataio.jobstore.types.SequenceAnalysisData;
import dk.dbc.dataio.jobstore.types.State;
import dk.dbc.dataio.jobstore.types.StateChange;
import dk.dbc.dataio.jobstore.types.StateElement;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import types.TestableJobEntityBuilder;

import java.io.ByteArrayInputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import static dk.dbc.dataio.commons.types.Chunk.Type.DELIVERED;
import static dk.dbc.dataio.commons.types.Chunk.Type.PARTITIONED;
import static dk.dbc.dataio.commons.types.Chunk.Type.PROCESSED;
import static dk.dbc.dataio.commons.types.ChunkItem.Status.FAILURE;
import static dk.dbc.dataio.commons.types.ChunkItem.Status.IGNORE;
import static dk.dbc.dataio.commons.types.ChunkItem.Status.SUCCESS;
import static dk.dbc.dataio.jobstore.types.State.Phase.DELIVERING;
import static dk.dbc.dataio.jobstore.types.State.Phase.PARTITIONING;
import static dk.dbc.dataio.jobstore.types.State.Phase.PROCESSING;
import static jakarta.persistence.LockModeType.PESSIMISTIC_WRITE;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

public class PgJobStore_ChunksTest extends PgJobStoreBaseTest {

    final List<String> chunkData = Arrays.asList("itemData0", "itemData1", "itemData2");

    @Test
    public void createChunkItemEntities() {
        PgJobStore pgJobStore = newPgJobStore(newPgJobStoreReposity());
        Params params = new Params();
        JobEntity jobEntity = getJobEntity(DEFAULT_JOB_ID);
        Sink sink = new SinkBuilder().build();
        when(jobEntity.getCachedSink().getSink()).thenReturn(sink);

        PgJobStoreRepository.ChunkItemEntities chunkItemEntities =
                pgJobStore.jobStoreRepository.createChunkItemEntities(101010, 1, 0,
                        params.maxChunkSize, params.dataPartitioner);
        assertThat("First chunk: items", chunkItemEntities, is(notNullValue()));
        assertThat("First chunk: number of items", chunkItemEntities.size(), is((short) 10));
        assertThat("First chunk: number of failed items", chunkItemEntities.chunkStateChange.getFailed(), is(0));
        assertChunkItemEntities(chunkItemEntities, PARTITIONING, EXPECTED_DATA_ENTRIES.subList(0, 10), StandardCharsets.UTF_8);

        chunkItemEntities = pgJobStore.jobStoreRepository.createChunkItemEntities(
                101010, 1, 1, params.maxChunkSize, params.dataPartitioner);
        assertThat("Second chunk: items", chunkItemEntities, is(notNullValue()));
        assertThat("Second chunk: number of items", chunkItemEntities.size(), is((short) 1));
        assertThat("Second chunk: number of failed items", chunkItemEntities.chunkStateChange.getFailed(), is(0));
        assertChunkItemEntities(chunkItemEntities, PARTITIONING, EXPECTED_DATA_ENTRIES.subList(10, 11), StandardCharsets.UTF_8);

        chunkItemEntities = pgJobStore.jobStoreRepository.createChunkItemEntities(
                101010, 1, 2, params.maxChunkSize, params.dataPartitioner);
        assertThat("Third chunk: items", chunkItemEntities, is(notNullValue()));
        assertThat("Third chunk: number of items", chunkItemEntities.size(), is((short) 0));
        assertThat("Third chunk: number of failed items", chunkItemEntities.chunkStateChange.getFailed(), is(0));
    }

    @Test
    public void createChunkItemEntities_dataPartitionerThrowsDataException_failedItemIsCreated() {
        PgJobStore pgJobStore = newPgJobStore(newPgJobStoreReposity());
        Params params = new Params();
        final String invalidXml =
                "<records>"
                        + "<record>first</record>"
                        + "<record>second"
                        + "</records>";

        params.dataPartitioner = DefaultXmlDataPartitioner.newInstance(new ByteArrayInputStream(invalidXml.getBytes(StandardCharsets.UTF_8)),
                StandardCharsets.UTF_8.name());

        Sink sink = new SinkBuilder().build();
        JobEntity jobEntity = getJobEntity(DEFAULT_JOB_ID);
        when(jobEntity.getCachedSink().getSink()).thenReturn(sink);

        PgJobStoreRepository.ChunkItemEntities chunkItemEntities =
                pgJobStore.jobStoreRepository.createChunkItemEntities(101010, 1, 0,
                        params.maxChunkSize, params.dataPartitioner);
        assertThat("Chunk: items", chunkItemEntities, is(notNullValue()));
        assertThat("Chunk: number of items", chunkItemEntities.size(), is((short) 2));
        assertThat("Chunk: number of failed items", chunkItemEntities.chunkStateChange.getFailed(), is(1));
        assertThat("First item: succeeded", chunkItemEntities.entities.get(0).getState().getPhase(PARTITIONING).getSucceeded(), is(1));
        assertThat("Second item: failed", chunkItemEntities.entities.get(1).getState().getPhase(PARTITIONING).getFailed(), is(1));
        assertThat("Second item: has fatal diagnostic", chunkItemEntities.entities.get(1).getState().fatalDiagnosticExists(), is(true));
    }

    @Test
    public void createChunkItemEntities_dataPartitionerSkipsRecord_failedItemIsCreated() throws JobStoreException {
        final String partiallyInvalidRecord = "245 00 *aA good beginning\n260 00 *atest*b@@dbc\ninvalid\ninvalid\n$\n";

        PgJobStoreRepository.ChunkItemEntities chunkItemEntities = createChunkItemEntitiesForDanMarc2Partitioning(partiallyInvalidRecord);

        assertThat("Chunk: items", chunkItemEntities, is(notNullValue()));
        assertThat("Chunk: number of items", chunkItemEntities.size(), is((short) 1));
        assertThat("Chunk: number of failed items", chunkItemEntities.chunkStateChange.getFailed(), is(1));
        assertThat("First item: failed", chunkItemEntities.entities.get(0).getState().getPhase(PARTITIONING).getFailed(), is(1));
        assertThat("First item: has fatal diagnostic", chunkItemEntities.entities.get(0).getState().fatalDiagnosticExists(), is(false));
    }

    @Test
    public void createChunkEntity() throws JobStoreException {
        Params params = new Params();
        PgJobStore pgJobStore = newPgJobStore(newPgJobStoreReposity());

        JobEntity jobEntity = getJobEntity(DEFAULT_JOB_ID);
        jobEntity.setState(new State());

        Sink sink = new SinkBuilder().build();
        when(jobEntity.getCachedSink().getSink()).thenReturn(sink);
        when(entityManager.find(eq(JobEntity.class), anyInt(), eq(PESSIMISTIC_WRITE))).thenReturn(jobEntity);

        ChunkEntity chunkEntity = pgJobStore.jobStoreRepository.createChunkEntity(
                101010, 1, 0, params.maxChunkSize, params.dataPartitioner,
                params.keyGenerator, params.dataFileId);
        assertThat("First chunk", chunkEntity, is(notNullValue()));
        assertThat("First chunk: number of items", chunkEntity.getNumberOfItems(), is(params.maxChunkSize));
        assertThat("First chunk: Partitioning phase endDate set", chunkEntity.getState().getPhase(PARTITIONING).getEndDate(), is(notNullValue()));
        assertThat("First chunk: number of seq keys", chunkEntity.getSequenceAnalysisData().getData().size(), is(0));
        assertThat("Job: number of chunks after first chunk", jobEntity.getNumberOfChunks(), is(1));
        assertThat("Job: number of items after first chunk", jobEntity.getNumberOfItems(), is((int) params.maxChunkSize));
        assertThat("Job: partitioning phase endDate not set after first chunk", jobEntity.getState().getPhase(PARTITIONING).getEndDate(), is(nullValue()));

        chunkEntity = pgJobStore.jobStoreRepository.createChunkEntity(101010,
                1, 1, params.maxChunkSize, params.dataPartitioner, params.keyGenerator,
                params.dataFileId);
        assertThat("Second chunk", chunkEntity, is(notNullValue()));
        assertThat("Second chunk: number of items", chunkEntity.getNumberOfItems(), is((short) (EXPECTED_NUMBER_OF_ITEMS - params.maxChunkSize)));
        assertThat("Second chunk: Partitioning phase endDate set", chunkEntity.getState().getPhase(PARTITIONING).getEndDate(), is(notNullValue()));
        assertThat("Second chunk: number of seq keys", chunkEntity.getSequenceAnalysisData().getData().size(), is(0));
        assertThat("Job: number of chunks after second chunk", jobEntity.getNumberOfChunks(), is(2));
        assertThat("Job: number of items after second chunk", jobEntity.getNumberOfItems(), is(EXPECTED_NUMBER_OF_ITEMS));
        assertThat("Job: partitioning phase endDate not set after second chunk", jobEntity.getState().getPhase(PARTITIONING).getEndDate(), is(nullValue()));

        chunkEntity = pgJobStore.jobStoreRepository.createChunkEntity(101010, 1,
                2, params.maxChunkSize, params.dataPartitioner, params.keyGenerator,
                params.dataFileId);
        assertThat("Third chunk", chunkEntity, is(nullValue()));
        assertThat("Job: number of chunks after third chunk", jobEntity.getNumberOfChunks(), is(2));
        assertThat("Job: number of items after third chunk", jobEntity.getNumberOfItems(), is(EXPECTED_NUMBER_OF_ITEMS));
        assertThat("Job: partitioning phase endDate not set after third chunk", jobEntity.getState().getPhase(PARTITIONING).getEndDate(), is(nullValue()));
    }

    @Test
    public void addChunk_chunkArgIsNull_throws() throws JobStoreException {
        PgJobStore pgJobStore = newPgJobStore();
        try {
            pgJobStore.addChunk(null);
            Assertions.fail("No exception thrown");
        } catch (NullPointerException e) {
        }
    }

    @Test
    public void addChunk_chunkEntityCanNotBeFound_throws() {

        Chunk chunk = getChunk(PROCESSED, chunkData, Arrays.asList(SUCCESS, FAILURE, IGNORE));

        setItemEntityExpectations(chunk, Collections.singletonList(PARTITIONING));

        when(entityManager.find(eq(ChunkEntity.class), any(ChunkEntity.Key.class))).thenReturn(null);

        PgJobStore pgJobStore = newPgJobStore();
        try {
            pgJobStore.addChunk(chunk);
            Assertions.fail("No exception thrown");
        } catch (JobStoreException e) {
        }
    }

    @Test
    public void addChunk_jobEntityCanNotBeFound_throws() {
        Chunk chunk = getChunk(PROCESSED, chunkData, Arrays.asList(SUCCESS, FAILURE, IGNORE));
        setItemEntityExpectations(chunk, Collections.singletonList(PARTITIONING));

        when(entityManager.find(eq(ChunkEntity.class), any(ChunkEntity.Key.class))).thenReturn(getChunkEntity(chunk.size(), Collections.singletonList(PARTITIONING)));
        when(entityManager.find(eq(JobEntity.class), anyInt())).thenReturn(null);

        PgJobStore pgJobStore = newPgJobStore();
        try {
            pgJobStore.addChunk(chunk);
            Assertions.fail("No exception thrown");
        } catch (JobStoreException e) {
        }
    }

    @Test
    public void addChunk_numberOfChunkItemsDiffersFromInternal_throws() {
        Chunk chunk = getChunk(PROCESSED, chunkData, Arrays.asList(SUCCESS, FAILURE, IGNORE));
        setItemEntityExpectations(chunk, Collections.singletonList(PARTITIONING));

        when(entityManager.find(eq(ChunkEntity.class), any(ChunkEntity.Key.class))).thenReturn(getChunkEntity(chunk.size() + 42, Collections.singletonList(PARTITIONING)));

        PgJobStore pgJobStore = newPgJobStore();
        try {
            pgJobStore.addChunk(chunk);
            Assertions.fail("No exception thrown");
        } catch (JobStoreException e) {
        }
    }

    @Test
    public void addChunk_attemptToOverwriteExistingChunk_throws() throws JobStoreException {
        PgJobStore pgJobStore = null;
        Chunk chunk = null;
        try {
            chunk = getChunk(PROCESSED, chunkData, Arrays.asList(SUCCESS, FAILURE, IGNORE));
            setItemEntityExpectations(chunk, Collections.singletonList(PARTITIONING));

            ChunkEntity chunkEntity = getChunkEntity(chunk.size(), Collections.singletonList(PARTITIONING));
            State state = getUpdatedState(chunk.size(), Collections.singletonList(PARTITIONING));
            JobEntity jobEntity = new TestableJobEntityBuilder().setNumberOfItems(chunk.size()).setState(state).build();

            when(entityManager.find(eq(ChunkEntity.class), any(ChunkEntity.Key.class), eq(PESSIMISTIC_WRITE))).thenReturn(chunkEntity);
            when(entityManager.find(eq(JobEntity.class), anyInt(), eq(PESSIMISTIC_WRITE))).thenReturn(jobEntity);

            pgJobStore = newPgJobStore(newPgJobStoreReposity());
            pgJobStore.addChunk(chunk);
        } catch (Exception e) {
            Assertions.fail(e.getMessage());
        }

        try {
            pgJobStore.addChunk(chunk);
            Assertions.fail("No exception thrown");
        } catch (DuplicateChunkException e) {
        }
    }

    @Test
    public void addChunk_chunkAdded_chunkEntityPhaseComplete() throws JobStoreException {
        Chunk chunk = getChunk(PROCESSED, chunkData, Arrays.asList(SUCCESS, FAILURE, IGNORE));
        setItemEntityExpectations(chunk, Collections.singletonList(PARTITIONING));

        ChunkEntity chunkEntity = getChunkEntity(chunk.size(), Collections.singletonList(PARTITIONING));
        State state = getUpdatedState(chunk.size(), Collections.singletonList(PARTITIONING));
        JobEntity jobEntity = new TestableJobEntityBuilder().setNumberOfItems(chunk.size()).setState(state).build();

        when(entityManager.find(eq(ChunkEntity.class), any(ChunkEntity.Key.class), eq(PESSIMISTIC_WRITE))).thenReturn(chunkEntity);
        when(entityManager.find(eq(JobEntity.class), anyInt(), eq(PESSIMISTIC_WRITE))).thenReturn(jobEntity);

        PgJobStore pgJobStore = newPgJobStore(newPgJobStoreReposity());
        JobInfoSnapshot jobInfoSnapshot = pgJobStore.addChunk(chunk);

        // assert ChunkEntity (counters + beginDate and endDate set)

        StateElement chunkStateElement = chunkEntity.getState().getPhase(PROCESSING);
        assertThat("ChunkEntity: processing phase beginDate set", chunkStateElement.getBeginDate(), is(notNullValue()));
        assertThat("ChunkEntity: processing phase endDate set", chunkStateElement.getEndDate(), is(notNullValue()));
        assertThat("ChunkEntity: number of failed items", chunkStateElement.getFailed(), is(1));
        assertThat("ChunkEntity: number of ignored items", chunkStateElement.getIgnored(), is(1));
        assertThat("ChunkEntity: number of succeeded items", chunkStateElement.getSucceeded(), is(1));
        assertThat("ChunkEntity: number of items", chunkEntity.getNumberOfItems(), is((short) chunk.size()));
        assertThat("ChunkEntity: time of completion not set", chunkEntity.getTimeOfCompletion(), is(nullValue()));
        assertThat("JobInfoSnapshot: time of completion not set", jobInfoSnapshot.getTimeOfCompletion(), is(nullValue()));
    }

    @Test
    public void addChunk_finalChunkAdded_jobEntityPhaseComplete() throws JobStoreException {
        Chunk chunk = getChunk(PROCESSED, chunkData, Arrays.asList(SUCCESS, FAILURE, IGNORE));
        setItemEntityExpectations(chunk, Collections.singletonList(PARTITIONING));

        ChunkEntity chunkEntity = getChunkEntity(chunk.size(), Collections.singletonList(PARTITIONING));
        State state = getUpdatedState(chunk.size(), Collections.singletonList(PARTITIONING));
        state.getPhase(PARTITIONING).withSucceeded(126);
        state.getPhase(PROCESSING).withFailed(41);
        state.getPhase(PROCESSING).withIgnored(41);
        state.getPhase(PROCESSING).withSucceeded(41);
        JobEntity jobEntity = new TestableJobEntityBuilder().setNumberOfItems(chunk.size()).setState(state).build();

        when(entityManager.find(eq(ChunkEntity.class), any(ChunkEntity.Key.class), eq(PESSIMISTIC_WRITE))).thenReturn(chunkEntity);
        when(entityManager.find(eq(JobEntity.class), anyInt(), eq(PESSIMISTIC_WRITE))).thenReturn(jobEntity);

        PgJobStore pgJobStore = newPgJobStore(newPgJobStoreReposity());
        JobInfoSnapshot jobInfoSnapshot = pgJobStore.addChunk(chunk);
        assertThat("JobInfoSnapshot:", jobInfoSnapshot, is(notNullValue()));
        assertThat("JobInfoSnapshot: time of completion not set", jobInfoSnapshot.getTimeOfCompletion(), is(nullValue()));

        // assert JobEntity (counters + beginDate and endDate set)

        StateElement jobStateElement = jobEntity.getState().getPhase(PROCESSING);
        assertThat("JobEntity: processing phase beginDate set", jobStateElement.getBeginDate(), is(notNullValue()));
        assertThat("JobEntity: processing phase endDate set", jobStateElement.getEndDate(), is(notNullValue()));
        assertThat("JobEntity: number of failed items", jobStateElement.getFailed(), is(42));
        assertThat("JobEntity: number of ignored items", jobStateElement.getIgnored(), is(42));
        assertThat("JobEntity: number of succeeded items", jobStateElement.getSucceeded(), is(42));
        assertThat("JobEntity: number of items", jobEntity.getNumberOfItems(), is(chunk.size()));
        assertThat("JobEntity: time of completion not set", jobEntity.getTimeOfCompletion(), is(nullValue()));
        assertThat("ChunkEntity: time of completion not set", chunkEntity.getTimeOfCompletion(), is(nullValue()));
    }

    @Test
    public void addChunk_nonFinalChunkAdded_jobEntityPhaseIncomplete() throws JobStoreException {
        Chunk chunk = getChunk(PROCESSED, chunkData, Arrays.asList(SUCCESS, FAILURE, IGNORE));
        setItemEntityExpectations(chunk, Collections.singletonList(PARTITIONING));

        ChunkEntity chunkEntity = getChunkEntity(chunk.size(), Collections.singletonList(PARTITIONING));
        State state = getUpdatedState(chunk.size(), Collections.emptyList());
        JobEntity jobEntity = new TestableJobEntityBuilder().setNumberOfItems(chunk.size()).setState(state).build();

        when(entityManager.find(eq(ChunkEntity.class), any(ChunkEntity.Key.class), eq(PESSIMISTIC_WRITE))).thenReturn(chunkEntity);
        when(entityManager.find(eq(JobEntity.class), anyInt(), eq(PESSIMISTIC_WRITE))).thenReturn(jobEntity);

        PgJobStore pgJobStore = newPgJobStore(newPgJobStoreReposity());
        JobInfoSnapshot jobInfoSnapshot = pgJobStore.addChunk(chunk);
        assertThat("JobInfoSnapshot:", jobInfoSnapshot, is(notNullValue()));
        assertThat("JobInfoSnapshot: time of completion not set", jobInfoSnapshot.getTimeOfCompletion(), is(nullValue()));

        // assert JobEntity (counters + beginDate set, endData == null)

        StateElement jobStateElement = jobEntity.getState().getPhase(PROCESSING);
        assertThat("JobEntity: processing phase beginDate set", jobStateElement.getBeginDate(), is(notNullValue()));
        assertThat("JobEntity: processing phase endDate not set", jobStateElement.getEndDate(), is(nullValue()));
        assertThat("JobEntity: number of failed items", jobStateElement.getFailed(), is(1));
        assertThat("JobEntity: number of ignored items", jobStateElement.getIgnored(), is(1));
        assertThat("JobEntity: number of succeeded items", jobStateElement.getSucceeded(), is(1));
        assertThat("JobEntity: number of items", jobEntity.getNumberOfItems(), is(chunk.size()));
        assertThat("JobEntity: time of completion not set", jobEntity.getTimeOfCompletion(), is(nullValue()));
        assertThat("ChunkEntity: time of completion not set", chunkEntity.getTimeOfCompletion(), is(nullValue()));
    }

    @Test
    public void addChunk_allPhasesComplete_timeOfCompletionIsSet() throws JobStoreException {
        Chunk chunk = getChunk(DELIVERED, chunkData, Arrays.asList(SUCCESS, SUCCESS, SUCCESS));
        setItemEntityExpectations(chunk, Arrays.asList(PARTITIONING, PROCESSING));

        ChunkEntity chunkEntity = getChunkEntity(chunk.size(), Arrays.asList(PARTITIONING, PROCESSING));
        State state = getUpdatedState(chunk.size(), Arrays.asList(PARTITIONING, PROCESSING));
        JobEntity jobEntity = new TestableJobEntityBuilder().setNumberOfItems(chunk.size()).setState(state).build();

        when(entityManager.find(eq(ChunkEntity.class), any(ChunkEntity.Key.class), eq(PESSIMISTIC_WRITE))).thenReturn(chunkEntity);
        when(entityManager.find(eq(JobEntity.class), anyInt(), eq(PESSIMISTIC_WRITE))).thenReturn(jobEntity);

        PgJobStore pgJobStore = newPgJobStore(newPgJobStoreReposity());
        JobInfoSnapshot jobInfoSnapshot = pgJobStore.addChunk(chunk);
        assertThat("JobInfoSnapshot:", jobInfoSnapshot, is(notNullValue()));
        assertThat("JobInfoSnapshot.timeOfCompletion", jobInfoSnapshot.getTimeOfCompletion(), is(notNullValue()));

        StateElement jobStateElement = jobEntity.getState().getPhase(PROCESSING);
        assertThat("JobEntity: processing phase beginDate set", jobStateElement.getBeginDate(), is(notNullValue()));
        assertThat("JobEntity: processing phase endDate set", jobStateElement.getEndDate(), is(notNullValue()));
        assertThat("JobEntity: number of failed items", jobStateElement.getFailed(), is(0));
        assertThat("JobEntity: number of ignored items", jobStateElement.getIgnored(), is(0));
        assertThat("JobEntity: number of succeeded items", jobStateElement.getSucceeded(), is(3));
        assertThat("JobEntity: number of items", jobEntity.getNumberOfItems(), is(chunk.size()));
        assertThat("JobEntity: time of completion", jobEntity.getTimeOfCompletion(), is(notNullValue()));
    }

    @Test
    public void updateChunkItemEntities_itemsCanNotBeFound_throws() throws JobStoreException {
        Chunk chunk = new ChunkBuilder(PROCESSED).build();

        when(entityManager.find(eq(ItemEntity.class), any(ItemEntity.Key.class))).thenReturn(null);

        PgJobStore pgJobStore = newPgJobStore(newPgJobStoreReposity());
        try {
            pgJobStore.jobStoreRepository.updateChunkItemEntities(chunk);
            Assertions.fail("No exception thrown");
        } catch (InvalidInputException e) {
            assertThat("JobError:", e.getJobError(), is(notNullValue()));
            assertThat("JobError: code", e.getJobError().getCode(), is(JobError.Code.INVALID_ITEM_IDENTIFIER));
        }
    }

    @Test
    public void updateChunkItemEntities_itemsForPartitioningPhase_throws() throws JobStoreException {
        Chunk chunk = new ChunkBuilder(PARTITIONED).build();
        ItemEntity itemEntity = new ItemEntity();
        itemEntity.setState(new State());

        when(entityManager.find(eq(ItemEntity.class), any(ItemEntity.Key.class))).thenReturn(itemEntity);

        PgJobStore pgJobStore = newPgJobStore(newPgJobStoreReposity());
        try {
            pgJobStore.jobStoreRepository.updateChunkItemEntities(chunk);
            Assertions.fail("No exception thrown");
        } catch (InvalidInputException e) {
            assertThat("JobError:", e.getJobError(), is(notNullValue()));
            assertThat("JobError: code", e.getJobError().getCode(), is(JobError.Code.ILLEGAL_CHUNK));
        }
    }

    @Test
    public void updateChunkItemEntities_itemsForProcessingPhase() throws JobStoreException {
        List<String> expectedItemData = Arrays.asList(
                (chunkData.get(0)),
                (chunkData.get(1)),
                (chunkData.get(2)));
        Chunk chunk = getChunk(PROCESSED, chunkData, Arrays.asList(SUCCESS, FAILURE, IGNORE));
        List<ItemEntity> entities = setItemEntityExpectations(chunk, Collections.singletonList(PARTITIONING));

        PgJobStore pgJobStore = newPgJobStore(newPgJobStoreReposity());
        PgJobStoreRepository.ChunkItemEntities chunkItemEntities = pgJobStore.jobStoreRepository.updateChunkItemEntities(chunk);
        assertThat("Chunk: items", chunkItemEntities, is(notNullValue()));
        assertThat("Chunk: number of items", chunkItemEntities.size(), is((short) chunk.size()));
        assertThat("Chunk: number of failed items", chunkItemEntities.chunkStateChange.getFailed(), is(1));
        assertThat("Chunk: number of ignored items", chunkItemEntities.chunkStateChange.getIgnored(), is(1));
        assertThat("Chunk: number of succeeded items", chunkItemEntities.chunkStateChange.getSucceeded(), is(1));

        assertChunkItemEntities(chunkItemEntities, PROCESSING, expectedItemData, StandardCharsets.UTF_8);

        StateElement entityStateElement = entities.get(0).getState().getPhase(PROCESSING);
        assertThat(String.format("%s failed counter", entities.get(0).getKey()), entityStateElement.getFailed(), is(0));
        assertThat(String.format("%s ignored counter", entities.get(0).getKey()), entityStateElement.getIgnored(), is(0));
        assertThat(String.format("%s succeeded counter", entities.get(0).getKey()), entityStateElement.getSucceeded(), is(1));

        entityStateElement = entities.get(1).getState().getPhase(State.Phase.PROCESSING);
        assertThat(String.format("%s failed counter", entities.get(1).getKey()), entityStateElement.getFailed(), is(1));
        assertThat(String.format("%s ignored counter", entities.get(1).getKey()), entityStateElement.getIgnored(), is(0));
        assertThat(String.format("%s succeeded counter", entities.get(1).getKey()), entityStateElement.getSucceeded(), is(0));

        entityStateElement = entities.get(2).getState().getPhase(State.Phase.PROCESSING);
        assertThat(String.format("%s failed counter", entities.get(2).getKey()), entityStateElement.getFailed(), is(0));
        assertThat(String.format("%s ignored counter", entities.get(2).getKey()), entityStateElement.getIgnored(), is(1));
        assertThat(String.format("%s succeeded counter", entities.get(2).getKey()), entityStateElement.getSucceeded(), is(0));
    }

    @Test
    public void updateChunkItemEntities_itemsForDeliveringPhase() throws JobStoreException {
        List<String> expectedItemData = Arrays.asList(
                (chunkData.get(0)),
                (chunkData.get(1)),
                (chunkData.get(2)));
        Chunk chunk = getChunk(DELIVERED, chunkData, Arrays.asList(SUCCESS, FAILURE, IGNORE));
        List<ItemEntity> entities = setItemEntityExpectations(chunk, Collections.singletonList(State.Phase.PARTITIONING));

        PgJobStore pgJobStore = newPgJobStore(newPgJobStoreReposity());
        PgJobStoreRepository.ChunkItemEntities chunkItemEntities = pgJobStore.jobStoreRepository.updateChunkItemEntities(chunk);
        assertThat("Chunk: items", chunkItemEntities, is(notNullValue()));
        assertThat("Chunk: number of items", chunkItemEntities.size(), is((short) chunk.size()));
        assertThat("Chunk: number of failed items", chunkItemEntities.chunkStateChange.getFailed(), is(1));
        assertThat("Chunk: number of ignored items", chunkItemEntities.chunkStateChange.getIgnored(), is(1));
        assertThat("Chunk: number of succeeded items", chunkItemEntities.chunkStateChange.getSucceeded(), is(1));

        assertChunkItemEntities(chunkItemEntities, DELIVERING, expectedItemData, StandardCharsets.UTF_8);
        StateElement entityStateElement = entities.get(0).getState().getPhase(DELIVERING);
        assertThat(String.format("%s failed counter", entities.get(0).getKey()), entityStateElement.getFailed(), is(0));
        assertThat(String.format("%s ignored counter", entities.get(0).getKey()), entityStateElement.getIgnored(), is(0));
        assertThat(String.format("%s succeeded counter", entities.get(0).getKey()), entityStateElement.getSucceeded(), is(1));

        entityStateElement = entities.get(1).getState().getPhase(DELIVERING);
        assertThat(String.format("%s failed counter", entities.get(1).getKey()), entityStateElement.getFailed(), is(1));
        assertThat(String.format("%s ignored counter", entities.get(1).getKey()), entityStateElement.getIgnored(), is(0));
        assertThat(String.format("%s succeeded counter", entities.get(1).getKey()), entityStateElement.getSucceeded(), is(0));

        entityStateElement = entities.get(2).getState().getPhase(DELIVERING);
        assertThat(String.format("%s failed counter", entities.get(2).getKey()), entityStateElement.getFailed(), is(0));
        assertThat(String.format("%s ignored counter", entities.get(2).getKey()), entityStateElement.getIgnored(), is(1));
        assertThat(String.format("%s succeeded counter", entities.get(2).getKey()), entityStateElement.getSucceeded(), is(0));
    }

    @Test
    public void updateChunkItemEntities_attemptToOverwriteAlreadyAddedChunk_throws() throws JobStoreException {
        List<String> chunkData = Collections.singletonList("itemData0");
        Chunk chunk = getChunk(PROCESSED, chunkData, Collections.singletonList(SUCCESS));
        List<String> chunkDataOverwrite = Collections.singletonList("itemData0-overwrite");
        Chunk chunkOverwrite = getChunk(PROCESSED, chunkDataOverwrite, Collections.singletonList(FAILURE));

        setItemEntityExpectations(chunk, Collections.singletonList(PARTITIONING));

        PgJobStore pgJobStore = newPgJobStore(newPgJobStoreReposity());
        // add original chunk
        pgJobStore.jobStoreRepository.updateChunkItemEntities(chunk);

        try {
            pgJobStore.jobStoreRepository.updateChunkItemEntities(chunkOverwrite);
            Assertions.fail("No exception thrown");
        } catch (DuplicateChunkException e) {
        }
    }

    /**
     * Private methods
     */

    // Helper class for parameter values (with defaults)
    private class Params {
        final String xml = getXml();
        public JobInputStream jobInputStream;
        public DataPartitioner dataPartitioner;
        public KeyGenerator keyGenerator;
        public Flow flow;
        public Sink sink;
        public FlowStoreReferences flowStoreReferences;
        public String dataFileId;
        public short maxChunkSize;

        public Params() {
            JobSpecification jobSpecification = new JobSpecification().withResultmailInitials("placeholder").withMailForNotificationAboutVerification("placeholder").withMailForNotificationAboutProcessing("placeholder");
            jobInputStream = new JobInputStream(jobSpecification, true, 0);
            dataPartitioner = DefaultXmlDataPartitioner.newInstance(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8.name());
            flow = new FlowBuilder().build();
            sink = new SinkBuilder().build();
            flowStoreReferences = new FlowStoreReferencesBuilder().build();
            keyGenerator = new DefaultKeyGenerator();
            maxChunkSize = 10;
            dataFileId = "datafile";
        }
    }

    private void assertChunkItemEntities(PgJobStoreRepository.ChunkItemEntities chunkItemEntities, State.Phase phase, List<String> dataEntries, Charset dataEncoding) {
        LinkedList<String> expectedData = new LinkedList<>(dataEntries);
        assertThat("Chunk item entities: phase", chunkItemEntities.chunkStateChange.getPhase(), is(phase));

        for (ItemEntity itemEntity : chunkItemEntities.entities) {

            String itemEntityKey = String.format(
                    "ItemEntity.Key{jobId=%d, chunkId=%d, itemId=%d}",
                    itemEntity.getKey().getJobId(),
                    itemEntity.getKey().getChunkId(),
                    itemEntity.getKey().getId());

            assertThat(String.format("%s %s phase beginDate set", itemEntityKey, phase), itemEntity.getState().getPhase(phase).getEndDate(), is(notNullValue()));
            assertThat(String.format("%s %s phase endDate set", itemEntityKey, phase), itemEntity.getState().getPhase(phase).getEndDate(), is(notNullValue()));

            ChunkItem chunkItem = null;
            switch (phase) {
                case PARTITIONING:
                    chunkItem = itemEntity.getPartitioningOutcome();
                    break;
                case PROCESSING:
                    chunkItem = itemEntity.getProcessingOutcome();
                    break;
                case DELIVERING:
                    chunkItem = itemEntity.getDeliveringOutcome();
                    break;
            }
            assertThat(String.format("%s %s phase data encoding", itemEntityKey, phase), chunkItem.getEncoding(), is(dataEncoding));
            assertThat(String.format("%s %s phase data", itemEntityKey, phase), StringUtil.asString(chunkItem.getData(), dataEncoding), is(expectedData.pop()));
        }
    }

    private Chunk getChunk(Chunk.Type type, List<String> data, List<ChunkItem.Status> statuses) {
        List<ChunkItem> chunkItems = new ArrayList<>(data.size());
        LinkedList<ChunkItem.Status> statusStack = new LinkedList<>(statuses);
        short itemId = 0;
        for (String itemData : data) {
            chunkItems.add(new ChunkItemBuilder().setId(itemId++).setData(itemData).setStatus(statusStack.pop()).build());
        }
        return new ChunkBuilder(type)
                .setJobId(1)
                .setChunkId(0)
                .setItems(chunkItems)
                .build();
    }

    private List<ItemEntity> setItemEntityExpectations(Chunk chunk, List<State.Phase> phasesDone) {
        List<ItemEntity> entities = new ArrayList<>(chunk.size());
        for (ChunkItem chunkItem : chunk) {
            ItemEntity itemEntity = getItemEntity(chunk.getJobId(), (int) chunk.getChunkId(), (short) chunkItem.getId(), phasesDone);
            entities.add(itemEntity);
        }
        ItemEntity[] expectations = entities.toArray(new ItemEntity[entities.size()]);
        if (expectations.length > 1) {
            when(entityManager.find(eq(ItemEntity.class), any(ItemEntity.Key.class)))
                    .thenReturn(expectations[0], Arrays.copyOfRange(expectations, 1, expectations.length));
        } else {
            when(entityManager.find(eq(ItemEntity.class), any(ItemEntity.Key.class)))
                    .thenReturn(expectations[0]);
        }
        return entities;
    }

    private ItemEntity getItemEntity(int jobId, int chunkId, short itemId, List<State.Phase> phasesDone) {
        ItemEntity.Key itemKey = new ItemEntity.Key(jobId, chunkId, itemId);
        ItemEntity itemEntity = new ItemEntity();
        itemEntity.setKey(itemKey);
        StateChange itemStateChange = new StateChange();
        State itemState = new State();
        for (State.Phase phase : phasesDone) {
            itemStateChange.setPhase(phase)
                    .setSucceeded(1)
                    .setBeginDate(new Date())
                    .setEndDate(new Date());

            itemState.updateState(itemStateChange);
        }

        itemEntity.setState(itemState);
        return itemEntity;
    }

    private ChunkEntity getChunkEntity(int numberOfItems, List<State.Phase> phasesDone) {
        final int jobId = 1;
        final int chunkId = 0;
        ChunkEntity.Key chunkKey = new ChunkEntity.Key(chunkId, jobId);
        ChunkEntity chunkEntity = new ChunkEntity();
        chunkEntity.setKey(chunkKey);
        chunkEntity.setNumberOfItems((short) numberOfItems);
        StateChange chunkStateChange = new StateChange();
        State chunkState = new State();
        for (State.Phase phase : phasesDone) {
            chunkStateChange.setPhase(phase)
                    .setBeginDate(new Date())
                    .setEndDate(new Date());
            chunkState.updateState(chunkStateChange);
        }
        chunkEntity.setSequenceAnalysisData(new SequenceAnalysisData(Collections.emptySet()));
        chunkEntity.setState(chunkState);
        return chunkEntity;
    }

    private PgJobStoreRepository.ChunkItemEntities createChunkItemEntitiesForDanMarc2Partitioning(String data) throws JobStoreException {
        PgJobStore pgJobStore = newPgJobStore(newPgJobStoreReposity());
        Params params = new Params();

        params.dataPartitioner = DanMarc2LineFormatDataPartitioner.newInstance(
                new ByteArrayInputStream(data.getBytes(StandardCharsets.UTF_8)), "latin1");

        JobEntity jobEntity = getJobEntity(DEFAULT_JOB_ID);
        Sink sink = new SinkBuilder().build();
        when(jobEntity.getCachedSink().getSink()).thenReturn(sink);
        return pgJobStore.jobStoreRepository.createChunkItemEntities(101010, 1,
                0, params.maxChunkSize, params.dataPartitioner);
    }
}
