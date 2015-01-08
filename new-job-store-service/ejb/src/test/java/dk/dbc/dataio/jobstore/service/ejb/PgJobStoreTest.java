package dk.dbc.dataio.jobstore.service.ejb;

import dk.dbc.dataio.commons.types.Flow;
import dk.dbc.dataio.commons.types.Sink;
import dk.dbc.dataio.commons.utils.service.Base64Util;
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
import dk.dbc.dataio.jobstore.types.ItemData;
import dk.dbc.dataio.jobstore.types.JobError;
import dk.dbc.dataio.jobstore.types.JobInfoSnapshot;
import dk.dbc.dataio.jobstore.types.JobInputStream;
import dk.dbc.dataio.jobstore.types.JobStoreException;
import dk.dbc.dataio.jobstore.types.State;
import dk.dbc.dataio.jsonb.JSONBContext;
import dk.dbc.dataio.jsonb.JSONBException;
import dk.dbc.dataio.jsonb.ejb.JSONBBean;
import dk.dbc.dataio.sequenceanalyser.keygenerator.SequenceAnalyserKeyGenerator;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import javax.ejb.SessionContext;
import javax.persistence.EntityManager;
import javax.persistence.LockModeType;
import javax.persistence.Query;
import java.io.ByteArrayInputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
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

    @Ignore("until sequence analysis functionality is integrated")
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

        PgJobStore.ChunkItemEntities chunkItemEntities = pgJobStore.createChunkItemEntities(
                1, 0, params.maxChunkSize, params.dataPartitioner, params.sequenceAnalyserKeyGenerator);
        assertThat("First chunk: items", chunkItemEntities, is(notNullValue()));
        assertThat("First chunk: number of items", chunkItemEntities.getEntities().size(), is(10));
        assertThat("First chunk: failed flag", chunkItemEntities.isFailed(), is(false));
        assertChunkItemEntities(chunkItemEntities, State.Phase.PARTITIONING, EXPECTED_DATA_ENTRIES.subList(0,10), StandardCharsets.UTF_8);

        chunkItemEntities = pgJobStore.createChunkItemEntities(
                1, 1, params.maxChunkSize, params.dataPartitioner, params.sequenceAnalyserKeyGenerator);
        assertThat("Second chunk: items", chunkItemEntities, is(notNullValue()));
        assertThat("Second chunk: number of items", chunkItemEntities.getEntities().size(), is(1));
        assertThat("Second chunk: failed flag", chunkItemEntities.isFailed(), is(false));
        assertChunkItemEntities(chunkItemEntities, State.Phase.PARTITIONING, EXPECTED_DATA_ENTRIES.subList(10, 11), StandardCharsets.UTF_8);

        chunkItemEntities = pgJobStore.createChunkItemEntities(
                1, 2, params.maxChunkSize, params.dataPartitioner, params.sequenceAnalyserKeyGenerator);
        assertThat("Third chunk: items", chunkItemEntities, is(notNullValue()));
        assertThat("Third chunk: number of items", chunkItemEntities.getEntities().size(), is(0));
        assertThat("Third chunk: failed flag", chunkItemEntities.isFailed(), is(false));
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

        final PgJobStore.ChunkItemEntities chunkItemEntities = pgJobStore.createChunkItemEntities(
                1, 0, params.maxChunkSize, params.dataPartitioner, params.sequenceAnalyserKeyGenerator);
        assertThat("Chunk: items", chunkItemEntities, is(notNullValue()));
        assertThat("Chunk: number of items", chunkItemEntities.getEntities().size(), is(2));
        assertThat("Chunk: failed flag", chunkItemEntities.isFailed(), is(true));
        assertThat("First item: succeeded", chunkItemEntities.getEntities().get(0).getState().getPhase(State.Phase.PARTITIONING).getSucceeded(), is(1));
        assertThat("Second item: failed", chunkItemEntities.getEntities().get(1).getState().getPhase(State.Phase.PARTITIONING).getFailed(), is(1));

        final JobError jobError = new JSONBContext().unmarshall(Base64Util.base64decode(
                chunkItemEntities.getEntities().get(1).getPartitioningOutcome().getData()), JobError.class);
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
        assertThat("First chunk: number of items", (short) chunkEntity.getNumberOfItems(), is(params.maxChunkSize));
        assertThat("First chunk: Partitioning phase endDate set", chunkEntity.getState().getPhase(State.Phase.PARTITIONING).getEndDate(), is(notNullValue()));
        assertThat("Job: number of chunks after first chunk", jobEntity.getNumberOfChunks(), is(1));
        assertThat("Job: number of items after first chunk", jobEntity.getNumberOfItems(), is((int) params.maxChunkSize));
        assertThat("Job: partitioning phase endDate not set after first chunk", jobEntity.getState().getPhase(State.Phase.PARTITIONING).getEndDate(), is(nullValue()));

        chunkEntity = pgJobStore.createChunkEntity(
                1, 1, params.maxChunkSize, params.dataPartitioner, params.sequenceAnalyserKeyGenerator, params.dataFileId);
        assertThat("Second chunk", chunkEntity, is(notNullValue()));
        assertThat("Second chunk: number of items", chunkEntity.getNumberOfItems(), is(EXPECTED_NUMBER_OF_ITEMS - params.maxChunkSize));
        assertThat("Second chunk: Partitioning phase endDate set", chunkEntity.getState().getPhase(State.Phase.PARTITIONING).getEndDate(), is(notNullValue()));
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
    public void persistJob_jobArgIsNull_throws() throws JobStoreException {
        final PgJobStore pgJobStore = newPgJobStore();
        try {
            pgJobStore.persistJob(null);
            fail("No exception thrown");
        } catch (NullPointerException e) {
        }
    }

    @Test
    public void persistJob_jobArgIsValid_jobIsPersistedAndManagedEntityIsRefreshed() {
        final JobEntity job = new JobEntity();
        final PgJobStore pgJobStore = newPgJobStore();
        final JobEntity jobEntity = pgJobStore.persistJob(job);

        assertThat(jobEntity, is(job));
        verify(entityManager).persist(job);
        verify(entityManager).refresh(job);
    }

    @Test
    public void persistChunk_chunkArgIsNull_throws() {
        final PgJobStore pgJobStore = newPgJobStore();
        try {
            pgJobStore.persistChunk(null);
            fail("No exception thrown");
        } catch (NullPointerException e) {
        }
    }

    @Test
    public void persistChunk_chunkArgIsValid_chunkIsPersistedAndManagedEntityIsRefreshed() {
        final ChunkEntity chunk = new ChunkEntity();
        final PgJobStore pgJobStore = newPgJobStore();
        final ChunkEntity chunkEntity = pgJobStore.persistChunk(chunk);

        assertThat(chunkEntity, is(chunk));
        verify(entityManager).persist(chunk);
        verify(entityManager).refresh(chunk);
    }

    @Test
    public void persistItem_itemArgIsNull_throws() {
        final PgJobStore pgJobStore = newPgJobStore();
        try {
            pgJobStore.persistItem(null);
            fail("No exception thrown");
        } catch (NullPointerException e) {
        }
    }

    @Test
    public void persistItem_itemArgIsValid_itemIsPersistedAndManagedEntityIsRefreshed() {
        final ItemEntity item = new ItemEntity();
        final PgJobStore pgJobStore = newPgJobStore();
        final ItemEntity itemEntity = pgJobStore.persistItem(item);

        assertThat(itemEntity, is(item));
        verify(entityManager).persist(item);
        //verify(entityManager).refresh(item);
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

    private void assertChunkItemEntities(PgJobStore.ChunkItemEntities entities, State.Phase phase, List<String> dataEntries, Charset dataEncoding) {
        final LinkedList<String> expectedData = new LinkedList<>(dataEntries);

        for (ItemEntity itemEntity : entities.getEntities()) {
            final String itemEntityKey = String.format("Item{jobId,chunkId,itemId}[%d,%d,%d]",
                    itemEntity.getKey().getJobId(), itemEntity.getKey().getChunkId(), itemEntity.getKey().getId());

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
            maxChunkSize = 10;
            dataFileId = "datafile";
        }
    }
}