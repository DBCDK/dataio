package dk.dbc.dataio.jobstore.service.ejb;

import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.types.ExternalChunk;
import dk.dbc.dataio.commons.types.Flow;
import dk.dbc.dataio.commons.types.Sink;
import dk.dbc.dataio.commons.utils.service.Base64Util;
import dk.dbc.dataio.commons.utils.test.model.ExternalChunkBuilder;
import dk.dbc.dataio.commons.utils.test.model.FlowBuilder;
import dk.dbc.dataio.commons.utils.test.model.JobSpecificationBuilder;
import dk.dbc.dataio.commons.utils.test.model.SinkBuilder;
import dk.dbc.dataio.jobstore.service.entity.ChunkEntity;
import dk.dbc.dataio.jobstore.service.entity.FlowCacheEntity;
import dk.dbc.dataio.jobstore.service.entity.ItemEntity;
import dk.dbc.dataio.jobstore.service.entity.JobEntity;
import dk.dbc.dataio.jobstore.service.entity.SinkCacheEntity;
import dk.dbc.dataio.jobstore.service.partitioner.DataPartitionerFactory;
import dk.dbc.dataio.jobstore.service.partitioner.DefaultXmlDataPartitionerFactory;
import dk.dbc.dataio.jobstore.types.InvalidInputException;
import dk.dbc.dataio.jobstore.types.ItemData;
import dk.dbc.dataio.jobstore.types.JobError;
import dk.dbc.dataio.jobstore.types.JobInfoSnapshot;
import dk.dbc.dataio.jobstore.types.JobInputStream;
import dk.dbc.dataio.jobstore.types.JobStoreException;
import dk.dbc.dataio.jobstore.types.State;
import dk.dbc.dataio.jobstore.types.StateChange;
import dk.dbc.dataio.jobstore.types.StateElement;
import dk.dbc.dataio.jobstore.types.criteria.JobListCriteria;
import dk.dbc.dataio.jsonb.JSONBContext;
import dk.dbc.dataio.jsonb.JSONBException;
import dk.dbc.dataio.jsonb.ejb.JSONBBean;
import dk.dbc.dataio.sequenceanalyser.keygenerator.SequenceAnalyserKeyGenerator;
import dk.dbc.dataio.sequenceanalyser.keygenerator.SequenceAnalyserSinkKeyGenerator;
import org.junit.Before;
import org.junit.Test;

import javax.ejb.SessionContext;
import javax.persistence.EntityManager;
import javax.persistence.LockModeType;
import javax.persistence.Query;
import java.io.ByteArrayInputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PgJobStoreTest {
    static {
        System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "trace");
    }

    private static final FlowCacheEntity EXPECTED_FLOW_CACHE_ENTITY = new FlowCacheEntity();
    private static final SinkCacheEntity EXPECTED_SINK_CACHE_ENTITY = new SinkCacheEntity();
    private static final int EXPECTED_NUMBER_OF_CHUNKS = 2;
    private static final List<String> EXPECTED_DATA_ENTRIES = Arrays.asList(
            Base64Util.base64encode("<?xml version=\"1.0\" encoding=\"UTF-8\"?><records><record>first</record></records>"),
            Base64Util.base64encode("<?xml version=\"1.0\" encoding=\"UTF-8\"?><records><record>second</record></records>"),
            Base64Util.base64encode("<?xml version=\"1.0\" encoding=\"UTF-8\"?><records><record>third</record></records>"),
            Base64Util.base64encode("<?xml version=\"1.0\" encoding=\"UTF-8\"?><records><record>fourth</record></records>"),
            Base64Util.base64encode("<?xml version=\"1.0\" encoding=\"UTF-8\"?><records><record>fifth</record></records>"),
            Base64Util.base64encode("<?xml version=\"1.0\" encoding=\"UTF-8\"?><records><record>sixth</record></records>"),
            Base64Util.base64encode("<?xml version=\"1.0\" encoding=\"UTF-8\"?><records><record>seventh</record></records>"),
            Base64Util.base64encode("<?xml version=\"1.0\" encoding=\"UTF-8\"?><records><record>eighth</record></records>"),
            Base64Util.base64encode("<?xml version=\"1.0\" encoding=\"UTF-8\"?><records><record>ninth</record></records>"),
            Base64Util.base64encode("<?xml version=\"1.0\" encoding=\"UTF-8\"?><records><record>tenth</record></records>"),
            Base64Util.base64encode("<?xml version=\"1.0\" encoding=\"UTF-8\"?><records><record>eleventh</record></records>"));
    private static final int EXPECTED_NUMBER_OF_ITEMS = EXPECTED_DATA_ENTRIES.size();

    private final EntityManager entityManager = mock(EntityManager.class);
    private final SessionContext sessionContext = mock(SessionContext.class);

    @Before
    public void setupExpectations() {
        final Query cacheFlowQuery = mock(Query.class);
        when(entityManager.createNamedQuery(FlowCacheEntity.NAMED_QUERY_SET_CACHE)).thenReturn(cacheFlowQuery);
        when(cacheFlowQuery.getSingleResult()).thenReturn(EXPECTED_FLOW_CACHE_ENTITY);
        final Query cacheSinkQuery = mock(Query.class);
        when(entityManager.createNamedQuery(SinkCacheEntity.NAMED_QUERY_SET_CACHE)).thenReturn(cacheSinkQuery);
        when(cacheSinkQuery.getSingleResult()).thenReturn(EXPECTED_SINK_CACHE_ENTITY);
    }

    @Test
    public void addJob_jobInputStreamArgIsNull_throws() throws JobStoreException {
        final PgJobStore pgJobStore = newPgJobStore();
        try {
            final Params params = new Params();
            pgJobStore.addJob(null, params.dataPartitioner, params.sequenceAnalyserKeyGenerator, params.flow, params.sink);
            fail("No exception thrown");
        } catch (NullPointerException e) {
        }
    }

    @Test
    public void addJob_dataPartitionerArgIsNull_throws() throws JobStoreException {
        final PgJobStore pgJobStore = newPgJobStore();
        try {
            final Params params = new Params();
            pgJobStore.addJob(params.jobInputStream, null, params.sequenceAnalyserKeyGenerator, params.flow, params.sink);
            fail("No exception thrown");
        } catch (NullPointerException e) {
        }
    }

    @Test
    public void addJob_sequenceAnalyserKeyGeneratorArgIsNull_throws() throws JobStoreException {
        final PgJobStore pgJobStore = newPgJobStore();
        try {
            final Params params = new Params();
            pgJobStore.addJob(params.jobInputStream, params.dataPartitioner, null, params.flow, params.sink);
            fail("No exception thrown");
        } catch (NullPointerException e) {
        }
    }

    @Test
    public void addJob_flowArgIsNull_throws() throws JobStoreException {
        final PgJobStore pgJobStore = newPgJobStore();
        try {
            final Params params = new Params();
            pgJobStore.addJob(params.jobInputStream, params.dataPartitioner, params.sequenceAnalyserKeyGenerator, null, params.sink);
            fail("No exception thrown");
        } catch (NullPointerException e) {
        }
    }

    @Test
    public void addJob_sinkArgIsNull_throws() throws JobStoreException {
        final PgJobStore pgJobStore = newPgJobStore();
        try {
            final Params params = new Params();
            pgJobStore.addJob(params.jobInputStream, params.dataPartitioner, params.sequenceAnalyserKeyGenerator, params.flow, null);
            fail("No exception thrown");
        } catch (NullPointerException e) {
        }
    }

    @Test
    public void addJob_allArgsAreValid_returnsJobInformationSnapshot() throws JobStoreException {
        final PgJobStore pgJobStore = newPgJobStore();
        final JobEntity jobEntity = new JobEntity();
        jobEntity.setState(new State());
        when(sessionContext.getBusinessObject(PgJobStore.class)).thenReturn(pgJobStore);
        when(entityManager.find(eq(JobEntity.class), anyInt(), eq(LockModeType.PESSIMISTIC_WRITE))).thenReturn(jobEntity);

        final Params params = new Params();
        final JobInfoSnapshot jobInfoSnapshot = pgJobStore.addJob(params.jobInputStream, params.dataPartitioner,
                params.sequenceAnalyserKeyGenerator, params.flow, params.sink);

        assertThat("Returned JobInfoSnapshot", jobInfoSnapshot, is(notNullValue()));
        assertThat("Number of chunks created", jobInfoSnapshot.getNumberOfChunks(), is(EXPECTED_NUMBER_OF_CHUNKS));
        assertThat("Number of items created", jobInfoSnapshot.getNumberOfItems(), is(EXPECTED_NUMBER_OF_ITEMS));
        assertThat("Partitioning phase endDate set", jobInfoSnapshot.getState().getPhase(State.Phase.PARTITIONING).getEndDate(), is(notNullValue()));
    }

    @Test
    public void createChunkItemEntities() throws JobStoreException {
        final PgJobStore pgJobStore = newPgJobStore();
        final Params params = new Params();

        PgJobStore.ChunkItemEntities chunkItemEntities =
                pgJobStore.createChunkItemEntities(1, 0, params.maxChunkSize, params.dataPartitioner);
        assertThat("First chunk: items", chunkItemEntities, is(notNullValue()));
        assertThat("First chunk: number of items", chunkItemEntities.size(), is((short) 10));
        assertThat("First chunk: number of failed items", chunkItemEntities.chunkStateChange.getFailed(), is(0));
        assertChunkItemEntities(chunkItemEntities, State.Phase.PARTITIONING, EXPECTED_DATA_ENTRIES.subList(0,10), StandardCharsets.UTF_8);

        chunkItemEntities = pgJobStore.createChunkItemEntities(1, 1, params.maxChunkSize, params.dataPartitioner);
        assertThat("Second chunk: items", chunkItemEntities, is(notNullValue()));
        assertThat("Second chunk: number of items", chunkItemEntities.size(), is((short) 1));
        assertThat("Second chunk: number of failed items", chunkItemEntities.chunkStateChange.getFailed(), is(0));
        assertChunkItemEntities(chunkItemEntities, State.Phase.PARTITIONING, EXPECTED_DATA_ENTRIES.subList(10, 11), StandardCharsets.UTF_8);

        chunkItemEntities = pgJobStore.createChunkItemEntities(1, 2, params.maxChunkSize, params.dataPartitioner);
        assertThat("Third chunk: items", chunkItemEntities, is(notNullValue()));
        assertThat("Third chunk: number of items", chunkItemEntities.size(), is((short) 0));
        assertThat("Third chunk: number of failed items", chunkItemEntities.chunkStateChange.getFailed(), is(0));
    }

    @Test
    public void createChunkItemEntities_dataPartitionerThrowsDataException_failedItemIsCreated() throws JobStoreException, JSONBException {
        final PgJobStore pgJobStore = newPgJobStore();
        final Params params = new Params();
        final String invalidXml =
                  "<records>"
                + "<record>first</record>"
                + "<record>second"
                + "</records>";

        params.dataPartitioner =  new DefaultXmlDataPartitionerFactory().createDataPartitioner(
                    new ByteArrayInputStream(invalidXml.getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8.name());

        final PgJobStore.ChunkItemEntities chunkItemEntities =
                pgJobStore.createChunkItemEntities(1, 0, params.maxChunkSize, params.dataPartitioner);
        assertThat("Chunk: items", chunkItemEntities, is(notNullValue()));
        assertThat("Chunk: number of items", chunkItemEntities.size(), is((short) 2));
        assertThat("Chunk: number of failed items", chunkItemEntities.chunkStateChange.getFailed(), is(1));
        assertThat("First item: succeeded", chunkItemEntities.entities.get(0).getState().getPhase(State.Phase.PARTITIONING).getSucceeded(), is(1));
        assertThat("Second item: failed", chunkItemEntities.entities.get(1).getState().getPhase(State.Phase.PARTITIONING).getFailed(), is(1));

        final JobError jobError = new JSONBContext().unmarshall(Base64Util.base64decode(
                chunkItemEntities.entities.get(1).getPartitioningOutcome().getData()), JobError.class);
        assertThat(jobError.getCode(), is(JobError.Code.INVALID_DATA));
        assertThat(jobError.getDescription().isEmpty(), is(false));
        assertThat(jobError.getStacktrace().isEmpty(), is(false));
    }

    @Test
    public void createChunkEntity() throws JobStoreException {
        final Params params = new Params();
        final PgJobStore pgJobStore = newPgJobStore();
        final JobEntity jobEntity = new JobEntity();
        jobEntity.setState(new State());

        when(entityManager.find(eq(JobEntity.class), anyInt(), eq(LockModeType.PESSIMISTIC_WRITE))).thenReturn(jobEntity);

        ChunkEntity chunkEntity = pgJobStore.createChunkEntity(
                1, 0, params.maxChunkSize, params.dataPartitioner, params.sequenceAnalyserKeyGenerator, params.dataFileId);
        assertThat("First chunk", chunkEntity, is(notNullValue()));
        assertThat("First chunk: number of items", chunkEntity.getNumberOfItems(), is(params.maxChunkSize));
        assertThat("First chunk: Partitioning phase endDate set", chunkEntity.getState().getPhase(State.Phase.PARTITIONING).getEndDate(), is(notNullValue()));
        assertThat("First chunk: number of seq keys", chunkEntity.getSequenceAnalysisData().getData().size(), is(1));
        assertThat("First chunk: seq keys", chunkEntity.getSequenceAnalysisData().getData().contains(params.sink.getContent().getName()), is(true));
        assertThat("Job: number of chunks after first chunk", jobEntity.getNumberOfChunks(), is(1));
        assertThat("Job: number of items after first chunk", jobEntity.getNumberOfItems(), is((int) params.maxChunkSize));
        assertThat("Job: partitioning phase endDate not set after first chunk", jobEntity.getState().getPhase(State.Phase.PARTITIONING).getEndDate(), is(nullValue()));

        chunkEntity = pgJobStore.createChunkEntity(
                1, 1, params.maxChunkSize, params.dataPartitioner, params.sequenceAnalyserKeyGenerator, params.dataFileId);
        assertThat("Second chunk", chunkEntity, is(notNullValue()));
        assertThat("Second chunk: number of items", chunkEntity.getNumberOfItems(), is((short) (EXPECTED_NUMBER_OF_ITEMS - params.maxChunkSize)));
        assertThat("Second chunk: Partitioning phase endDate set", chunkEntity.getState().getPhase(State.Phase.PARTITIONING).getEndDate(), is(notNullValue()));
        assertThat("Second chunk: number of seq keys", chunkEntity.getSequenceAnalysisData().getData().size(), is(1));
        assertThat("Second chunk: seq keys", chunkEntity.getSequenceAnalysisData().getData().contains(params.sink.getContent().getName()), is(true));
        assertThat("Job: number of chunks after second chunk", jobEntity.getNumberOfChunks(), is(2));
        assertThat("Job: number of items after second chunk", jobEntity.getNumberOfItems(), is(EXPECTED_NUMBER_OF_ITEMS));
        assertThat("Job: partitioning phase endDate not set after second chunk", jobEntity.getState().getPhase(State.Phase.PARTITIONING).getEndDate(), is(nullValue()));

        chunkEntity = pgJobStore.createChunkEntity(
                1, 2, params.maxChunkSize, params.dataPartitioner, params.sequenceAnalyserKeyGenerator, params.dataFileId);
        assertThat("Third chunk", chunkEntity, is(nullValue()));
        assertThat("Job: number of chunks after third chunk", jobEntity.getNumberOfChunks(), is(2));
        assertThat("Job: number of items after third chunk", jobEntity.getNumberOfItems(), is(EXPECTED_NUMBER_OF_ITEMS));
        assertThat("Job: partitioning phase endDate not set after third chunk", jobEntity.getState().getPhase(State.Phase.PARTITIONING).getEndDate(), is(nullValue()));
    }

    @Test
    public void addChunk_chunkArgIsNull_throws() throws JobStoreException {
        final PgJobStore pgJobStore = newPgJobStore();
        try {
            pgJobStore.addChunk(null);
            fail("No exception thrown");
        } catch (NullPointerException e) {
        }
    }

    @Test
    public void addChunk_chunkEntityCanNotBeFound_throws() throws JobStoreException {
        final List<String> chunkData = Arrays.asList("itemData0", "itemData1", "itemData2");
        final ExternalChunk chunk = getExternalChunk(1, 0, ExternalChunk.Type.PROCESSED, chunkData,
                Arrays.asList(ChunkItem.Status.SUCCESS, ChunkItem.Status.FAILURE, ChunkItem.Status.IGNORE));
        setItemEntityExpectations(chunk, Arrays.asList(State.Phase.PARTITIONING));

        when(entityManager.find(eq(ChunkEntity.class), any(ChunkEntity.Key.class))).thenReturn(null);

        final PgJobStore pgJobStore = newPgJobStore();
        try {
            pgJobStore.addChunk(chunk);
            fail("No exception thrown");
        } catch (JobStoreException e) {
        }
    }

    @Test
    public void addChunk_jobEntityCanNotBeFound_throws() throws JobStoreException {
        final List<String> chunkData = Arrays.asList("itemData0", "itemData1", "itemData2");
        final ExternalChunk chunk = getExternalChunk(1, 0, ExternalChunk.Type.PROCESSED, chunkData,
                Arrays.asList(ChunkItem.Status.SUCCESS, ChunkItem.Status.FAILURE, ChunkItem.Status.IGNORE));
        setItemEntityExpectations(chunk, Arrays.asList(State.Phase.PARTITIONING));

        when(entityManager.find(eq(ChunkEntity.class), any(ChunkEntity.Key.class)))
                .thenReturn(getChunkEntity(1, 0, chunk.size(), Arrays.asList(State.Phase.PARTITIONING)));
        when(entityManager.find(eq(JobEntity.class), anyInt())).thenReturn(null);

        final PgJobStore pgJobStore = newPgJobStore();
        try {
            pgJobStore.addChunk(chunk);
            fail("No exception thrown");
        } catch (JobStoreException e) {
        }
    }

    @Test
    public void addChunk_numberOfExternalChunkItemsDiffersFromInternal_throws() throws JobStoreException {
        final List<String> chunkData = Arrays.asList("itemData0", "itemData1", "itemData2");
        final ExternalChunk chunk = getExternalChunk(1, 0, ExternalChunk.Type.PROCESSED, chunkData,
                Arrays.asList(ChunkItem.Status.SUCCESS, ChunkItem.Status.FAILURE, ChunkItem.Status.IGNORE));
        setItemEntityExpectations(chunk, Arrays.asList(State.Phase.PARTITIONING));

        when(entityManager.find(eq(ChunkEntity.class), any(ChunkEntity.Key.class)))
                .thenReturn(getChunkEntity(1, 0, chunk.size() + 42, Arrays.asList(State.Phase.PARTITIONING)));

        final PgJobStore pgJobStore = newPgJobStore();
        try {
            pgJobStore.addChunk(chunk);
            fail("No exception thrown");
        } catch (JobStoreException e) {
        }
    }

    @Test
    public void addChunk_chunkAdded_chunkEntityPhaseComplete() throws JobStoreException {
        final List<String> chunkData = Arrays.asList("itemData0", "itemData1", "itemData2");
        final ExternalChunk chunk = getExternalChunk(1, 0, ExternalChunk.Type.PROCESSED, chunkData,
                Arrays.asList(ChunkItem.Status.SUCCESS, ChunkItem.Status.FAILURE, ChunkItem.Status.IGNORE));
        setItemEntityExpectations(chunk, Arrays.asList(State.Phase.PARTITIONING));

        final ChunkEntity chunkEntity = getChunkEntity(1, 0, chunk.size(), Arrays.asList(State.Phase.PARTITIONING));
        final JobEntity jobEntity = getJobEntity(chunk.size(), Arrays.asList(State.Phase.PARTITIONING));

        when(entityManager.find(eq(ChunkEntity.class), any(ChunkEntity.Key.class))).thenReturn(chunkEntity);
        when(entityManager.find(eq(JobEntity.class), anyInt(), eq(LockModeType.PESSIMISTIC_WRITE))).thenReturn(jobEntity);

        final PgJobStore pgJobStore = newPgJobStore();
        pgJobStore.addChunk(chunk);

        // assert ChunkEntity (counters + beginDate and endDate set)

        final StateElement chunkStateElement = chunkEntity.getState().getPhase(State.Phase.PROCESSING);
        assertThat("ChunkEntity: processing phase beginDate set", chunkStateElement.getBeginDate(), is(notNullValue()));
        assertThat("ChunkEntity: processing phase endDate set", chunkStateElement.getEndDate(), is(notNullValue()));
        assertThat("ChunkEntity: number of failed items", chunkStateElement.getFailed(), is(1));
        assertThat("ChunkEntity: number of ignored items", chunkStateElement.getIgnored(), is(1));
        assertThat("ChunkEntity: number of succeeded items", chunkStateElement.getSucceeded(), is(1));
        assertThat("ChunkEntity: number of items", chunkEntity.getNumberOfItems(), is((short) chunk.size()));
    }

    @Test
    public void addChunk_finalChunkAdded_jobEntityPhaseComplete() throws JobStoreException {
        final List<String> chunkData = Arrays.asList("itemData0", "itemData1", "itemData2");
        final ExternalChunk chunk = getExternalChunk(1, 0, ExternalChunk.Type.PROCESSED, chunkData,
                Arrays.asList(ChunkItem.Status.SUCCESS, ChunkItem.Status.FAILURE, ChunkItem.Status.IGNORE));
        setItemEntityExpectations(chunk, Arrays.asList(State.Phase.PARTITIONING));

        final ChunkEntity chunkEntity = getChunkEntity(1, 0, chunk.size(), Arrays.asList(State.Phase.PARTITIONING));
        final JobEntity jobEntity = getJobEntity(chunk.size(), Arrays.asList(State.Phase.PARTITIONING));
        jobEntity.getState().getPhase(State.Phase.PARTITIONING).setSucceeded(126);
        jobEntity.getState().getPhase(State.Phase.PROCESSING).setFailed(41);
        jobEntity.getState().getPhase(State.Phase.PROCESSING).setIgnored(41);
        jobEntity.getState().getPhase(State.Phase.PROCESSING).setSucceeded(41);

        when(entityManager.find(eq(ChunkEntity.class), any(ChunkEntity.Key.class))).thenReturn(chunkEntity);
        when(entityManager.find(eq(JobEntity.class), anyInt(), eq(LockModeType.PESSIMISTIC_WRITE))).thenReturn(jobEntity);

        final PgJobStore pgJobStore = newPgJobStore();
        final JobInfoSnapshot jobInfoSnapshot = pgJobStore.addChunk(chunk);
        assertThat("JobInfoSnapshot:", jobInfoSnapshot, is(notNullValue()));

        // assert JobEntity (counters + beginDate and endDate set)

        final StateElement jobStateElement = jobEntity.getState().getPhase(State.Phase.PROCESSING);
        assertThat("JobEntity: processing phase beginDate set", jobStateElement.getBeginDate(), is(notNullValue()));
        assertThat("JobEntity: processing phase endDate set", jobStateElement.getEndDate(), is(notNullValue()));
        assertThat("JobEntity: number of failed items", jobStateElement.getFailed(), is(42));
        assertThat("JobEntity: number of ignored items", jobStateElement.getIgnored(), is(42));
        assertThat("JobEntity: number of succeeded items", jobStateElement.getSucceeded(), is(42));
        assertThat("JobEntity: number of items", jobEntity.getNumberOfItems(), is(chunk.size()));
    }

    @Test
    public void addChunk_nonFinalChunkAdded_jobEntityPhaseIncomplete() throws JobStoreException {
        final List<String> chunkData = Arrays.asList("itemData0", "itemData1", "itemData2");
        final ExternalChunk chunk = getExternalChunk(1, 0, ExternalChunk.Type.PROCESSED, chunkData,
                Arrays.asList(ChunkItem.Status.SUCCESS, ChunkItem.Status.FAILURE, ChunkItem.Status.IGNORE));
        setItemEntityExpectations(chunk, Arrays.asList(State.Phase.PARTITIONING));

        final ChunkEntity chunkEntity = getChunkEntity(1, 0, chunk.size(), Arrays.asList(State.Phase.PARTITIONING));
        final JobEntity jobEntity = getJobEntity(chunk.size(), Collections.<State.Phase>emptyList());

        when(entityManager.find(eq(ChunkEntity.class), any(ChunkEntity.Key.class))).thenReturn(chunkEntity);
        when(entityManager.find(eq(JobEntity.class), anyInt(), eq(LockModeType.PESSIMISTIC_WRITE))).thenReturn(jobEntity);

        final PgJobStore pgJobStore = newPgJobStore();
        final JobInfoSnapshot jobInfoSnapshot = pgJobStore.addChunk(chunk);
        assertThat("JobInfoSnapshot:", jobInfoSnapshot, is(notNullValue()));

        // assert JobEntity (counters + beginDate set, endData == null)

        final StateElement jobStateElement = jobEntity.getState().getPhase(State.Phase.PROCESSING);
        assertThat("JobEntity: processing phase beginDate set", jobStateElement.getBeginDate(), is(notNullValue()));
        assertThat("JobEntity: processing phase endDate not set", jobStateElement.getEndDate(), is(nullValue()));
        assertThat("JobEntity: number of failed items", jobStateElement.getFailed(), is(1));
        assertThat("JobEntity: number of ignored items", jobStateElement.getIgnored(), is(1));
        assertThat("JobEntity: number of succeeded items", jobStateElement.getSucceeded(), is(1));
        assertThat("JobEntity: number of items", jobEntity.getNumberOfItems(), is(chunk.size()));
    }

    @Test
    public void updateChunkItemEntities_itemsCanNotBeFound_throws() throws JobStoreException {
        final ExternalChunk chunk = new ExternalChunkBuilder(ExternalChunk.Type.PROCESSED).build();

        when(entityManager.find(eq(ItemEntity.class), any(ItemEntity.Key.class))).thenReturn(null);

        final PgJobStore pgJobStore = newPgJobStore();
        try {
            pgJobStore.updateChunkItemEntities(chunk);
            fail("No exception thrown");
        } catch (InvalidInputException e) {
            assertThat("JobError:", e.getJobError(), is(notNullValue()));
            assertThat("JobError: code", e.getJobError().getCode(), is(JobError.Code.INVALID_ITEM_IDENTIFIER));
        }
    }

    @Test
    public void updateChunkItemEntities_itemsForPartitioningPhase_throws() throws JobStoreException {
        final ExternalChunk chunk = new ExternalChunkBuilder(ExternalChunk.Type.PARTITIONED).build();
        final ItemEntity itemEntity = new ItemEntity();
        itemEntity.setState(new State());

        when(entityManager.find(eq(ItemEntity.class), any(ItemEntity.Key.class))).thenReturn(itemEntity);

        final PgJobStore pgJobStore = newPgJobStore();
        try {
            pgJobStore.updateChunkItemEntities(chunk);
            fail("No exception thrown");
        } catch (InvalidInputException e) {
            assertThat("JobError:", e.getJobError(), is(notNullValue()));
            assertThat("JobError: code", e.getJobError().getCode(), is(JobError.Code.ILLEGAL_CHUNK));
        }
    }

    @Test
    public void updateChunkItemEntities_itemsForProcessingPhase() throws JobStoreException {
        final List<String> chunkData = Arrays.asList("itemData0", "itemData1", "itemData2");
        final ExternalChunk chunk = getExternalChunk(1, 0, ExternalChunk.Type.PROCESSED, chunkData,
                Arrays.asList(ChunkItem.Status.SUCCESS, ChunkItem.Status.FAILURE, ChunkItem.Status.IGNORE));
        final List<ItemEntity> entities = setItemEntityExpectations(chunk, Arrays.asList(State.Phase.PARTITIONING));

        final PgJobStore pgJobStore = newPgJobStore();
        final PgJobStore.ChunkItemEntities chunkItemEntities = pgJobStore.updateChunkItemEntities(chunk);
        assertThat("Chunk: items", chunkItemEntities, is(notNullValue()));
        assertThat("Chunk: number of items", chunkItemEntities.size(), is((short) chunk.size()));
        assertThat("Chunk: number of failed items", chunkItemEntities.chunkStateChange.getFailed(), is(1));
        assertThat("Chunk: number of ignored items", chunkItemEntities.chunkStateChange.getIgnored(), is(1));
        assertThat("Chunk: number of succeeded items", chunkItemEntities.chunkStateChange.getSucceeded(), is(1));

        assertChunkItemEntities(chunkItemEntities, State.Phase.PROCESSING, chunkData, StandardCharsets.UTF_8);
        StateElement entityStateElement = entities.get(0).getState().getPhase(State.Phase.PROCESSING);
        assertThat(String.format("%s failed counter", entities.get(0).getKey()),
                entityStateElement.getFailed(), is(0));
        assertThat(String.format("%s ignored counter", entities.get(0).getKey()),
                entityStateElement.getIgnored(), is(0));
        assertThat(String.format("%s succeeded counter", entities.get(0).getKey()),
                entityStateElement.getSucceeded(), is(1));
        entityStateElement = entities.get(1).getState().getPhase(State.Phase.PROCESSING);
        assertThat(String.format("%s failed counter", entities.get(1).getKey()),
                entityStateElement.getFailed(), is(1));
        assertThat(String.format("%s ignored counter", entities.get(1).getKey()),
                entityStateElement.getIgnored(), is(0));
        assertThat(String.format("%s succeeded counter", entities.get(1).getKey()),
                entityStateElement.getSucceeded(), is(0));
        entityStateElement = entities.get(2).getState().getPhase(State.Phase.PROCESSING);
        assertThat(String.format("%s failed counter", entities.get(2).getKey()),
                entityStateElement.getFailed(), is(0));
        assertThat(String.format("%s ignored counter", entities.get(2).getKey()),
                entityStateElement.getIgnored(), is(1));
        assertThat(String.format("%s succeeded counter", entities.get(2).getKey()),
                entityStateElement.getSucceeded(), is(0));
    }

    @Test
    public void updateChunkItemEntities_itemsForDeliveringPhase() throws JobStoreException {
        final List<String> chunkData = Arrays.asList("itemData0", "itemData1", "itemData2");
        final ExternalChunk chunk = getExternalChunk(1, 0, ExternalChunk.Type.DELIVERED, chunkData,
                Arrays.asList(ChunkItem.Status.SUCCESS, ChunkItem.Status.FAILURE, ChunkItem.Status.IGNORE));
        final List<ItemEntity> entities = setItemEntityExpectations(chunk, Arrays.asList(State.Phase.PARTITIONING));

        final PgJobStore pgJobStore = newPgJobStore();
        final PgJobStore.ChunkItemEntities chunkItemEntities = pgJobStore.updateChunkItemEntities(chunk);
        assertThat("Chunk: items", chunkItemEntities, is(notNullValue()));
        assertThat("Chunk: number of items", chunkItemEntities.size(), is((short) chunk.size()));
        assertThat("Chunk: number of failed items", chunkItemEntities.chunkStateChange.getFailed(), is(1));
        assertThat("Chunk: number of ignored items", chunkItemEntities.chunkStateChange.getIgnored(), is(1));
        assertThat("Chunk: number of succeeded items", chunkItemEntities.chunkStateChange.getSucceeded(), is(1));

        assertChunkItemEntities(chunkItemEntities, State.Phase.DELIVERING, chunkData, StandardCharsets.UTF_8);
        StateElement entityStateElement = entities.get(0).getState().getPhase(State.Phase.DELIVERING);
        assertThat(String.format("%s failed counter", entities.get(0).getKey()),
                entityStateElement.getFailed(), is(0));
        assertThat(String.format("%s ignored counter", entities.get(0).getKey()),
                entityStateElement.getIgnored(), is(0));
        assertThat(String.format("%s succeeded counter", entities.get(0).getKey()),
                entityStateElement.getSucceeded(), is(1));
        entityStateElement = entities.get(1).getState().getPhase(State.Phase.DELIVERING);
        assertThat(String.format("%s failed counter", entities.get(1).getKey()),
                entityStateElement.getFailed(), is(1));
        assertThat(String.format("%s ignored counter", entities.get(1).getKey()),
                entityStateElement.getIgnored(), is(0));
        assertThat(String.format("%s succeeded counter", entities.get(1).getKey()),
                entityStateElement.getSucceeded(), is(0));
        entityStateElement = entities.get(2).getState().getPhase(State.Phase.DELIVERING);
        assertThat(String.format("%s failed counter", entities.get(2).getKey()),
                entityStateElement.getFailed(), is(0));
        assertThat(String.format("%s ignored counter", entities.get(2).getKey()),
                entityStateElement.getIgnored(), is(1));
        assertThat(String.format("%s succeeded counter", entities.get(2).getKey()),
                entityStateElement.getSucceeded(), is(0));
    }

    @Test
    public void updateChunkItemEntities_attemptToOverwriteAlreadyAddedChunk() throws JobStoreException {
        final List<String> chunkData = Arrays.asList("itemData0");
        final ExternalChunk chunk = getExternalChunk(1, 0, ExternalChunk.Type.PROCESSED, chunkData,
                Arrays.asList(ChunkItem.Status.SUCCESS));
        final List<String> chunkDataOverwrite = Arrays.asList("itemData0-overwrite");
        final ExternalChunk chunkOverwrite = getExternalChunk(1, 0, ExternalChunk.Type.PROCESSED, chunkDataOverwrite,
                Arrays.asList(ChunkItem.Status.FAILURE));

        final List<ItemEntity> entities = setItemEntityExpectations(chunk, Arrays.asList(State.Phase.PARTITIONING));

        final PgJobStore pgJobStore = newPgJobStore();
        // add original chunk
        pgJobStore.updateChunkItemEntities(chunk);
        // try to overwrite already added chunk - assert that no state change is returned
        final PgJobStore.ChunkItemEntities chunkItemEntities = pgJobStore.updateChunkItemEntities(chunkOverwrite);
        assertThat("Chunk: items", chunkItemEntities, is(notNullValue()));
        assertThat("Chunk: number of items", chunkItemEntities.size(), is((short) chunk.size()));
        assertThat("Chunk: number of failed items", chunkItemEntities.chunkStateChange.getFailed(), is(0));
        assertThat("Chunk: number of ignored items", chunkItemEntities.chunkStateChange.getIgnored(), is(0));
        assertThat("Chunk: number of succeeded items", chunkItemEntities.chunkStateChange.getSucceeded(), is(0));

        // assert that item entity retains original state
        assertChunkItemEntities(chunkItemEntities, State.Phase.PROCESSING, chunkData, StandardCharsets.UTF_8);
        final StateElement entityStateElement = entities.get(0).getState().getPhase(State.Phase.PROCESSING);
        assertThat(String.format("%s failed counter", entities.get(0).getKey()),
                entityStateElement.getFailed(), is(0));
        assertThat(String.format("%s ignored counter", entities.get(0).getKey()),
                entityStateElement.getIgnored(), is(0));
        assertThat(String.format("%s succeeded counter", entities.get(0).getKey()),
                entityStateElement.getSucceeded(), is(1));
    }

    private ItemEntity getItemEntity(int jobId, int chunkId, short itemId, List<State.Phase> phasesDone) {
        final ItemEntity.Key itemKey = new ItemEntity.Key(jobId, chunkId, itemId);
        final ItemEntity itemEntity = new ItemEntity();
        itemEntity.setKey(itemKey);
        final StateChange itemStateChange = new StateChange();
        for (State.Phase phase : phasesDone) {
            itemStateChange.setPhase(phase)
                    .setSucceeded(1)
                    .setBeginDate(new Date())
                    .setEndDate(new Date());
        }
        final State itemState = new State();
        itemEntity.setState(itemState);
        if (!phasesDone.isEmpty()) {
            itemEntity.getState().updateState(itemStateChange);
        }
        return itemEntity;
    }

    private JobEntity getJobEntity(int numberOfItems, List<State.Phase> phasesDone) {
        final JobEntity jobEntity = new JobEntity();
        jobEntity.setNumberOfItems(numberOfItems);
        final StateChange jobStateChange = new StateChange();
        for (State.Phase phase : phasesDone) {
            jobStateChange.setPhase(phase)
                    .setSucceeded(numberOfItems)
                    .setBeginDate(new Date())
                    .setEndDate(new Date());
        }
        final State jobState = new State();
        jobEntity.setState(jobState);
        if (!phasesDone.isEmpty()) {
            jobEntity.getState().updateState(jobStateChange);
        }
        return jobEntity;
    }

    private ChunkEntity getChunkEntity(int jobId, int chunkId, int numberOfItems, List<State.Phase> phasesDone) {
        final ChunkEntity.Key chunkKey = new ChunkEntity.Key(chunkId, jobId);
        final ChunkEntity chunkEntity = new ChunkEntity();
        chunkEntity.setKey(chunkKey);
        chunkEntity.setNumberOfItems((short) numberOfItems);
        final StateChange chunkStateChange = new StateChange();
        for (State.Phase phase : phasesDone) {
            chunkStateChange.setPhase(phase)
                    .setBeginDate(new Date())
                    .setEndDate(new Date());
        }
        final State chunkState = new State();
        chunkEntity.setState(chunkState);
        if (!phasesDone.isEmpty()) {
            chunkEntity.getState().updateState(chunkStateChange);
        }
        return chunkEntity;
    }

    private ExternalChunk getExternalChunk(int jobId, int chunkId, ExternalChunk.Type type, List<String> data, List<ChunkItem.Status> statuses) {
        final List<ChunkItem> chunkItems = new ArrayList<>(data.size());
        final LinkedList<ChunkItem.Status> statusStack = new LinkedList<>(statuses);
        short itemId = 0;
        for (String itemData : data) {
            chunkItems.add(new ChunkItem(itemId++, itemData, statusStack.pop()));
        }
        return new ExternalChunkBuilder(type)
                .setJobId(jobId)
                .setChunkId(chunkId)
                .setItems(chunkItems)
                .build();
    }

    private List<ItemEntity> setItemEntityExpectations(ExternalChunk chunk, List<State.Phase> phasesDone) {
        final List<ItemEntity> entities = new ArrayList<>(chunk.size());
        for (ChunkItem chunkItem : chunk) {
            final ItemEntity itemEntity = getItemEntity((int) chunk.getJobId(), (int) chunk.getChunkId(), (short) chunkItem.getId(), phasesDone);
            entities.add(itemEntity);
        }
        final ItemEntity[] expectations = entities.toArray(new ItemEntity[entities.size()]);
        if (expectations.length > 1) {
            when(entityManager.find(eq(ItemEntity.class), any(ItemEntity.Key.class)))
                    .thenReturn(expectations[0], Arrays.copyOfRange(expectations, 1, expectations.length));
        } else {
            when(entityManager.find(eq(ItemEntity.class), any(ItemEntity.Key.class)))
                    .thenReturn(expectations[0]);
        }
        return entities;
    }

    @Test
    public void cacheFlow_flowArgIsNull_throws() throws JobStoreException {
        final PgJobStore pgJobStore = newPgJobStore();
        try {
            pgJobStore.cacheFlow(null);
            fail("No exception thrown");
        } catch (NullPointerException e) {
        }
    }

    @Test
    public void cacheFlow_flowArgIsCached_returnsFlowCacheEntityInstance() throws JobStoreException {
        final PgJobStore pgJobStore = newPgJobStore();
        final FlowCacheEntity flowCacheEntity = pgJobStore.cacheFlow(new FlowBuilder().build());
        assertThat(flowCacheEntity, is(EXPECTED_FLOW_CACHE_ENTITY));
    }

    @Test
    public void cacheSink_sinkArgIsNull_throws() throws JobStoreException {
        final PgJobStore pgJobStore = newPgJobStore();
        try {
            pgJobStore.cacheSink(null);
            fail("No exception thrown");
        } catch (NullPointerException e) {
        }
    }

    @Test
    public void cacheSink_sinkArgIsCached_returnsSinkCacheEntityInstance() throws JobStoreException {
        final PgJobStore pgJobStore = newPgJobStore();
        final SinkCacheEntity sinkCacheEntity = pgJobStore.cacheSink(new SinkBuilder().build());
        assertThat(sinkCacheEntity, is(EXPECTED_SINK_CACHE_ENTITY));
    }

    @Test
    public void listJobs_criteriaArgIsNull_throws() {
       final PgJobStore pgJobStore = newPgJobStore();
        try {
            pgJobStore.listJobs(null);
            fail("No exception thrown");
        } catch (NullPointerException e) {
        }
    }

    @Test
    public void listJobs_queryReturnsEmptyList_returnsEmptySnapshotList() {
        final Query query = mock(Query.class);
        when(entityManager.createNativeQuery(anyString(), eq(JobEntity.class))).thenReturn(query);
        when(query.getResultList()).thenReturn(Collections.emptyList());

        final PgJobStore pgJobStore = newPgJobStore();
        final List<JobInfoSnapshot> jobInfoSnapshots = pgJobStore.listJobs(new JobListCriteria());
        assertThat("List of JobInfoSnapshot", jobInfoSnapshots, is(notNullValue()));
        assertThat("List of JobInfoSnapshot is empty", jobInfoSnapshots.isEmpty(), is(true));
    }

    @Test
    public void listJobs_queryReturnsNonEmptyList_returnsSnapshotList() {
        final Query query = mock(Query.class);
        final JobEntity jobEntity1 = new JobEntity();
        jobEntity1.setNumberOfItems(42);
        final JobEntity jobEntity2 = new JobEntity();
        jobEntity2.setNumberOfItems(4242);
        when(entityManager.createNativeQuery(anyString(), eq(JobEntity.class))).thenReturn(query);
        when(query.getResultList()).thenReturn(Arrays.asList(jobEntity1, jobEntity2));

        final PgJobStore pgJobStore = newPgJobStore();
        final List<JobInfoSnapshot> jobInfoSnapshots = pgJobStore.listJobs(new JobListCriteria());
        assertThat("List of JobInfoSnapshot", jobInfoSnapshots, is(notNullValue()));
        assertThat("List of JobInfoSnapshot size", jobInfoSnapshots.size(), is(2));
        assertThat("List of JobInfoSnapshot first element numberOfItems",
                jobInfoSnapshots.get(0).getNumberOfItems(), is(jobEntity1.getNumberOfItems()));
        assertThat("List of JobInfoSnapshot second element numberOfItems",
                jobInfoSnapshots.get(1).getNumberOfItems(), is(jobEntity2.getNumberOfItems()));
    }

    /*
     * Private methods
     */

    private PgJobStore newPgJobStore() {
        final JSONBBean jsonbBean = new JSONBBean();
        jsonbBean.initialiseContext();

        final PgJobStore pgJobStore = new PgJobStore();
        pgJobStore.jsonbBean = jsonbBean;
        pgJobStore.entityManager = entityManager;
        pgJobStore.sessionContext = sessionContext;
        return pgJobStore;
    }

    private void assertChunkItemEntities(PgJobStore.ChunkItemEntities chunkItemEntities, State.Phase phase, List<String> dataEntries, Charset dataEncoding) {
        final LinkedList<String> expectedData = new LinkedList<>(dataEntries);
        assertThat("Chunk item entities: phase", chunkItemEntities.chunkStateChange.getPhase(), is(phase));

        for (ItemEntity itemEntity : chunkItemEntities.entities) {
            final String itemEntityKey = String.format("ItemEntity.Key{jobId=%d, chunkId=%d, itemId=%d}",
                    itemEntity.getKey().getJobId(), itemEntity.getKey().getChunkId(), itemEntity.getKey().getId());

            assertThat(String.format("%s %s phase beginDate set", itemEntityKey, phase),
                    itemEntity.getState().getPhase(phase).getEndDate(), is(notNullValue()));
            assertThat(String.format("%s %s phase endDate set", itemEntityKey, phase),
                    itemEntity.getState().getPhase(phase).getEndDate(), is(notNullValue()));

            ItemData itemData = null;
            switch (phase) {
                case PARTITIONING: itemData = itemEntity.getPartitioningOutcome();
                    break;
                case PROCESSING: itemData = itemEntity.getProcessingOutcome();
                    break;
                case DELIVERING: itemData = itemEntity.getDeliveringOutcome();
                    break;
            }
            assertThat(String.format("%s %s phase data", itemEntityKey, phase),
                    itemData.getData(), is(expectedData.pop()));
            assertThat(String.format("%s %s phase data encoding", itemEntityKey, phase),
                    itemData.getEncoding(), is(dataEncoding));
        }
    }

    /* Helper class for parameter values (with defaults)
     */
    private static class Params {
        final String xml =
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
                + "</records>";

        public JobInputStream jobInputStream;
        public DataPartitionerFactory.DataPartitioner dataPartitioner;
        public SequenceAnalyserKeyGenerator sequenceAnalyserKeyGenerator;
        public Flow flow;
        public Sink sink;
        public String dataFileId;
        public short maxChunkSize;

        public Params() {
            jobInputStream = new JobInputStream(new JobSpecificationBuilder().build(), true, 0);
            dataPartitioner = new DefaultXmlDataPartitionerFactory().createDataPartitioner(
                    new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8.name());
            flow = new FlowBuilder().build();
            sink = new SinkBuilder().build();
            sequenceAnalyserKeyGenerator = new SequenceAnalyserSinkKeyGenerator(sink);
            maxChunkSize = 10;
            dataFileId = "datafile";
        }
    }
}