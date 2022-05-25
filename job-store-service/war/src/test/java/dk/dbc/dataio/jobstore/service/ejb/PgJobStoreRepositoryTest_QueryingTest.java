package dk.dbc.dataio.jobstore.service.ejb;

import dk.dbc.dataio.commons.types.Chunk;
import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.utils.test.model.ChunkItemBuilder;
import dk.dbc.dataio.jobstore.service.entity.ItemEntity;
import dk.dbc.dataio.jobstore.service.entity.JobEntity;
import dk.dbc.dataio.jobstore.types.ItemInfoSnapshot;
import dk.dbc.dataio.jobstore.types.JobInfoSnapshot;
import dk.dbc.dataio.jobstore.types.JobStoreException;
import dk.dbc.dataio.jobstore.types.State;
import dk.dbc.dataio.jobstore.types.criteria.ItemListCriteria;
import dk.dbc.dataio.jobstore.types.criteria.JobListCriteria;
import org.junit.Test;

import javax.persistence.Query;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import static dk.dbc.dataio.jobstore.service.util.JobInfoSnapshotConverter.toJobInfoSnapshot;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

public class PgJobStoreRepositoryTest_QueryingTest extends PgJobStoreBaseTest {
    private static final int DEFAULT_CHUNK_ID = 1;
    private static final short DEFAULT_ITEM_ID = 1;

    @Test
    public void listJobs_criteriaArgIsNull_throws() {
        final PgJobStoreRepository pgJobStoreRepository = newPgJobStoreReposity();
        try {
            pgJobStoreRepository.listJobs((JobListCriteria) null);
            fail("No exception thrown");
        } catch (NullPointerException e) {
        }
    }

    @Test
    public void listJobs_queryReturnsEmptyList_returnsEmptySnapshotList() {
        final Query query = whenCreateNativeQueryThenReturn();
        when(query.getResultList()).thenReturn(Collections.emptyList());

        final PgJobStoreRepository pgJobStoreRepository = newPgJobStoreReposity();
        final List<JobInfoSnapshot> jobInfoSnapshots = pgJobStoreRepository.listJobs(new JobListCriteria());
        assertThat("List of JobInfoSnapshot", jobInfoSnapshots, is(notNullValue()));
        assertThat("List of JobInfoSnapshot is empty", jobInfoSnapshots.isEmpty(), is(true));
    }

    @Test
    public void listJobs_queryReturnsNonEmptyList_returnsSnapshotList() {
        final JobEntity jobEntity1 = new JobEntity();
        jobEntity1.setNumberOfItems(42);
        final JobEntity jobEntity2 = new JobEntity();
        jobEntity2.setNumberOfItems(4242);
        final Query query = whenCreateNativeQueryThenReturn();
        when(query.getResultList()).thenReturn(Arrays.asList(jobEntity1, jobEntity2));

        final PgJobStoreRepository pgJobStoreRepository = newPgJobStoreReposity();
        final List<JobInfoSnapshot> jobInfoSnapshots = pgJobStoreRepository.listJobs(new JobListCriteria());
        assertThat("List of JobInfoSnapshot", jobInfoSnapshots, is(notNullValue()));
        assertThat("List of JobInfoSnapshot size", jobInfoSnapshots.size(), is(2));
        assertThat("List of JobInfoSnapshot first element", jobInfoSnapshots.get(0), is(toJobInfoSnapshot(jobEntity1)));
        assertThat("List of JobInfoSnapshot second element", jobInfoSnapshots.get(1), is(toJobInfoSnapshot(jobEntity2)));
    }

    @Test
    public void listItems_queryReturnsEmptyList_returnsEmptySnapshotList() {
        final Query query = whenCreateNativeQueryThenReturn();
        when(query.getResultList()).thenReturn(Collections.emptyList());

        final PgJobStoreRepository pgJobStoreRepository = newPgJobStoreReposity();
        final List<ItemInfoSnapshot> itemInfoSnapshots = pgJobStoreRepository.listItems(new ItemListCriteria());
        assertThat("List of ItemInfoSnapshot", itemInfoSnapshots, is(notNullValue()));
        assertThat("List of ItemInfoSnapshot is empty", itemInfoSnapshots.isEmpty(), is(true));
    }

    @Test
    public void listItems_queryReturnsNonEmptyList_returnsSnapshotList() {
        final ItemEntity itemEntity1 = new ItemEntity();
        itemEntity1.setKey(new ItemEntity.Key(1, 0, (short) 0));
        final ItemEntity itemEntity2 = new ItemEntity();
        itemEntity2.setKey(new ItemEntity.Key(1, 0, (short) 1));
        final Query query = whenCreateNativeQueryThenReturn();
        when(query.getResultList()).thenReturn(Arrays.asList(itemEntity1, itemEntity2));

        final PgJobStoreRepository pgJobStoreRepository = newPgJobStoreReposity();
        final List<ItemInfoSnapshot> itemInfoSnapshots = pgJobStoreRepository.listItems(new ItemListCriteria());
        assertThat("List of ItemInfoSnapshot", itemInfoSnapshots, is(notNullValue()));
        assertThat("List of ItemInfoSnapshot size", itemInfoSnapshots.size(), is(2));
        assertThat("List of ItemInfoSnapshot first element itemId",
                itemInfoSnapshots.get(0).getItemId(), is(itemEntity1.getKey().getId()));
        assertThat("List of ItemInfoSnapshot first element chunkId",
                itemInfoSnapshots.get(0).getChunkId(), is(itemEntity1.getKey().getChunkId()));
        assertThat("List of ItemInfoSnapshot first element jobId",
                itemInfoSnapshots.get(0).getJobId(), is(itemEntity1.getKey().getJobId()));
        assertThat("List of ItemInfoSnapshot first element itemNumber",
                itemInfoSnapshots.get(0).getItemNumber(), is(1));

        assertThat("List of JobInfoSnapshot second element itemId",
                itemInfoSnapshots.get(1).getItemId(), is(itemEntity2.getKey().getId()));
        assertThat("List of JobInfoSnapshot second element chunkId",
                itemInfoSnapshots.get(1).getChunkId(), is(itemEntity2.getKey().getChunkId()));
        assertThat("List of JobInfoSnapshot second element jobId",
                itemInfoSnapshots.get(1).getJobId(), is(itemEntity2.getKey().getJobId()));
        assertThat("List of ItemInfoSnapshot second element itemNumber",
                itemInfoSnapshots.get(1).getItemNumber(), is(2));
    }

    @Test
    public void countItems_criteriaArgIsNull_throws() {
        final PgJobStoreRepository pgJobStoreRepository = newPgJobStoreReposity();
        try {
            pgJobStoreRepository.countItems((ItemListCriteria) null);
            fail("No exception thrown");
        } catch (NullPointerException e) {
        }
    }

    @Test
    public void countItems_queryReturnsItemCount() {
        final Query query = whenCreateNativeQueryThenReturn();
        when(query.getSingleResult()).thenReturn(2L);

        final PgJobStoreRepository pgJobStoreRepository = newPgJobStoreReposity();
        assertThat(pgJobStoreRepository.countItems(new ItemListCriteria()), is(2L));
    }

    @Test
    public void getChunk_typeArgIsNull_throws() {
        final PgJobStoreRepository pgJobStoreRepository = newPgJobStoreReposity();
        try {
            pgJobStoreRepository.getChunk(null, 2, 1);
            fail("No exception thrown");
        } catch (NullPointerException e) {
        }
    }

    @Test
    public void getChunk_queryReturnsEmptyList_returnsNull() {
        final Query query = whenCreateNativeQueryThenReturn();
        when(query.getResultList()).thenReturn(Collections.emptyList());

        final PgJobStoreRepository pgJobStoreRepository = newPgJobStoreReposity();
        assertThat(pgJobStoreRepository.getChunk(Chunk.Type.PARTITIONED, 2, 1), is(nullValue()));
    }

    @Test
    public void getChunk_queryReturnsItemEntityWithoutChunkItem_throws() {
        final Query query = whenCreateNativeQueryThenReturn();
        when(query.getResultList()).thenReturn(Collections.singletonList(new ItemEntity()));

        final PgJobStoreRepository pgJobStoreRepository = newPgJobStoreReposity();
        try {
            pgJobStoreRepository.getChunk(Chunk.Type.PARTITIONED, 2, 1);
            fail("No exception thrown");
        } catch (IllegalArgumentException e) {
        }
    }

    @Test
    public void getChunk_queryReturnsItemEntityWithChunkItems_returnsChunk() {
        final ChunkItem chunkItem1 = new ChunkItemBuilder().setId(0).setData("data1").build();
        final State state1 = new State();
        state1.getPhase(State.Phase.PARTITIONING).withSucceeded(1);
        final ItemEntity entity1 = new ItemEntity();
        entity1.setKey(new ItemEntity.Key(2, 1, (short) chunkItem1.getId()));
        entity1.setPartitioningOutcome(chunkItem1);
        entity1.setState(state1);

        final ChunkItem chunkItem2 = new ChunkItemBuilder().setId(1).setData("data2").setEncoding(StandardCharsets.ISO_8859_1).build();
        final State state2 = new State();
        state2.getPhase(State.Phase.PARTITIONING).withFailed(1);
        final ItemEntity entity2 = new ItemEntity();
        entity2.setKey(new ItemEntity.Key(2, 1, (short) chunkItem2.getId()));
        entity2.setPartitioningOutcome(chunkItem2);
        entity2.setState(state2);

        final Query query = whenCreateNativeQueryThenReturn();
        when(query.getResultList()).thenReturn(Arrays.asList(entity1, entity2));

        final PgJobStoreRepository pgJobStoreRepository = newPgJobStoreReposity();
        final Chunk chunk = pgJobStoreRepository.getChunk(Chunk.Type.PARTITIONED, 2, 1);

        assertThat("chunk", chunk, is(notNullValue()));
        assertThat("chunk.size()", chunk.size(), is(2));
        assertThat("chunk.getEncoding()", chunk.getEncoding(), is(chunkItem1.getEncoding()));
        final Iterator<ChunkItem> iterator = chunk.iterator();
        final ChunkItem firstChunkItem = iterator.next();
        assertThat("chunk[0]", firstChunkItem, is(chunkItem1));
        assertThat("chunk[0].getId()", firstChunkItem.getId(), is((long) entity1.getKey().getId()));

        final ChunkItem secondChunkItem = iterator.next();
        assertThat("chunk[1]", secondChunkItem, is(chunkItem2));
        assertThat("chunk[1].getId()", secondChunkItem.getId(), is((long) entity2.getKey().getId()));
    }

    @Test
    public void getChunkItemForPhase_itemEntityNotFound_throws() {
        final PgJobStoreRepository pgJobStoreRepository = newPgJobStoreReposity();
        when(entityManager.find(eq(ItemEntity.class), any(ItemEntity.Key.class))).thenReturn(null);
        try {
            pgJobStoreRepository.getChunkItemForPhase(DEFAULT_JOB_ID, DEFAULT_CHUNK_ID, DEFAULT_ITEM_ID, State.Phase.PARTITIONING);
            fail("No exception thrown");
        } catch (JobStoreException e) {
        }
    }

    @Test
    public void getChunkItemForPhase_phasePartitioning_returnsChunkItem() throws JobStoreException {
        final ItemEntity itemEntity = getItemEntity(DEFAULT_JOB_ID, DEFAULT_CHUNK_ID, DEFAULT_ITEM_ID);
        when(entityManager.find(eq(ItemEntity.class), any(ItemEntity.Key.class))).thenReturn(itemEntity);

        final PgJobStoreRepository pgJobStoreRepository = newPgJobStoreReposity();
        final ChunkItem chunkItem = pgJobStoreRepository.getChunkItemForPhase(DEFAULT_JOB_ID, DEFAULT_CHUNK_ID, DEFAULT_ITEM_ID, State.Phase.PARTITIONING);
        assertThat("chunkItem not null", chunkItem, not(nullValue()));
        assertThat(String.format("chunkItem.data: {%s} expected to match: {%s}",
                        Arrays.toString(chunkItem.getData()), Arrays.toString(itemEntity.getPartitioningOutcome().getData())),
                chunkItem.getData(), is(itemEntity.getPartitioningOutcome().getData()));
    }

    @Test
    public void getChunkItemForPhase_phaseProcessing_returnsChunkItem() throws JobStoreException {
        final ItemEntity itemEntity = getItemEntity(DEFAULT_JOB_ID, DEFAULT_CHUNK_ID, DEFAULT_ITEM_ID);
        when(entityManager.find(eq(ItemEntity.class), any(ItemEntity.Key.class))).thenReturn(itemEntity);

        final PgJobStoreRepository pgJobStoreRepository = newPgJobStoreReposity();
        final ChunkItem chunkItem = pgJobStoreRepository.getChunkItemForPhase(DEFAULT_JOB_ID, DEFAULT_CHUNK_ID, DEFAULT_ITEM_ID, State.Phase.PROCESSING);
        assertThat("chunkItem not null", chunkItem, not(nullValue()));
        assertThat(String.format("chunkItem.data: {%s} expected to match: {%s}",
                        Arrays.toString(chunkItem.getData()), Arrays.toString(itemEntity.getProcessingOutcome().getData())),
                chunkItem.getData(), is(itemEntity.getProcessingOutcome().getData()));
    }

    @Test
    public void getChunkItemForPhase_phaseDelivering_returnsChunkItem() throws JobStoreException {
        final ItemEntity itemEntity = getItemEntity(DEFAULT_JOB_ID, DEFAULT_CHUNK_ID, DEFAULT_ITEM_ID);
        when(entityManager.find(eq(ItemEntity.class), any(ItemEntity.Key.class))).thenReturn(itemEntity);

        final PgJobStoreRepository pgJobStoreRepository = newPgJobStoreReposity();
        final ChunkItem chunkItem = pgJobStoreRepository.getChunkItemForPhase(DEFAULT_JOB_ID, DEFAULT_CHUNK_ID, DEFAULT_ITEM_ID, State.Phase.DELIVERING);
        assertThat("chunkItem not null", chunkItem, not(nullValue()));
        assertThat(String.format("chunkItem.data: {%s} expected to match: {%s}",
                        Arrays.toString(chunkItem.getData()), Arrays.toString(itemEntity.getDeliveringOutcome().getData())),
                chunkItem.getData(), is(itemEntity.getDeliveringOutcome().getData()));
    }

    private ItemEntity getItemEntity(int jobId, int chunkId, short itemId) {
        final ItemEntity.Key itemKey = new ItemEntity.Key(jobId, chunkId, itemId);
        final ItemEntity itemEntity = new ItemEntity();
        itemEntity.setKey(itemKey);
        itemEntity.setState(new State());
        itemEntity.setPartitioningOutcome(new ChunkItemBuilder().setData("Partitioning data").build());
        itemEntity.setProcessingOutcome(new ChunkItemBuilder().setData("processing data").build());
        itemEntity.setDeliveringOutcome(new ChunkItemBuilder().setData("delivering data").build());
        return itemEntity;
    }

}
