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


import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.types.ExternalChunk;
import dk.dbc.dataio.commons.utils.lang.StringUtil;
import dk.dbc.dataio.jobstore.service.entity.ChunkEntity;
import dk.dbc.dataio.jobstore.service.entity.ItemEntity;
import dk.dbc.dataio.jobstore.service.entity.JobEntity;
import dk.dbc.dataio.jobstore.service.sequenceanalyser.ChunkIdentifier;
import dk.dbc.dataio.jobstore.test.types.FlowStoreReferencesBuilder;
import dk.dbc.dataio.jobstore.types.ItemData;
import dk.dbc.dataio.jobstore.types.ItemInfoSnapshot;
import dk.dbc.dataio.jobstore.types.JobInfoSnapshot;
import dk.dbc.dataio.jobstore.types.JobStoreException;
import dk.dbc.dataio.jobstore.types.SequenceAnalysisData;
import dk.dbc.dataio.jobstore.types.State;
import dk.dbc.dataio.jobstore.types.criteria.ChunkListCriteria;
import dk.dbc.dataio.jobstore.types.criteria.ItemListCriteria;
import dk.dbc.dataio.jobstore.types.criteria.JobListCriteria;
import dk.dbc.dataio.sequenceanalyser.CollisionDetectionElement;
import org.junit.Test;

import javax.persistence.Query;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import static dk.dbc.dataio.commons.utils.lang.StringUtil.base64encode;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PgJobStore_QueriesTest extends PgJobStoreBaseTest {

    private static final int DEFAULT_CHUNK_ID = 1;
    private static final short DEFAULT_ITEM_ID = 1;

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
        jobEntity1.setFlowStoreReferences(new FlowStoreReferencesBuilder().build());
        final JobEntity jobEntity2 = new JobEntity();
        jobEntity2.setNumberOfItems(4242);
        jobEntity2.setFlowStoreReferences(new FlowStoreReferencesBuilder().build());
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

    @Test
    public void listItems_queryReturnsEmptyList_returnsEmptySnapshotList() {
        final Query query = mock(Query.class);
        when(entityManager.createNativeQuery(anyString(), eq(ItemEntity.class))).thenReturn(query);
        when(query.getResultList()).thenReturn(Collections.emptyList());

        final PgJobStore pgJobStore = newPgJobStore();
        final List<ItemInfoSnapshot> itemInfoSnapshots = pgJobStore.listItems(new ItemListCriteria());
        assertThat("List of ItemInfoSnapshot", itemInfoSnapshots, is(notNullValue()));
        assertThat("List of ItemInfoSnapshot is empty", itemInfoSnapshots.isEmpty(), is(true));
    }

    @Test
    public void listItems_queryReturnsNonEmptyList_returnsSnapshotList() {
        final Query query = mock(Query.class);
        final ItemEntity itemEntity1 = new ItemEntity();
        itemEntity1.setKey(new ItemEntity.Key(1, 0, (short) 0));
        final ItemEntity itemEntity2 = new ItemEntity();
        itemEntity2.setKey(new ItemEntity.Key(1, 0,(short) 1));
        when(entityManager.createNativeQuery(anyString(), eq(ItemEntity.class))).thenReturn(query);
        when(query.getResultList()).thenReturn(Arrays.asList(itemEntity1, itemEntity2));

        final PgJobStore pgJobStore = newPgJobStore();
        final List<ItemInfoSnapshot> itemInfoSnapshots = pgJobStore.listItems(new ItemListCriteria());
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
        final PgJobStore pgJobStore = newPgJobStore();
        try {
            pgJobStore.countItems(null);
            fail("No exception thrown");
        } catch (NullPointerException e) {
        }
    }

    @Test
    public void countItems_queryReturnsItemCount() {
        final Query query = mock(Query.class);
        when(entityManager.createNativeQuery(anyString())).thenReturn(query);

        when(query.getSingleResult()).thenReturn(2L);

        final PgJobStore pgJobStore = newPgJobStore();
        final Long count = pgJobStore.countItems(new ItemListCriteria());
        assertThat(count, is(2L));
    }

    @Test
    public void listChunksCollisionDetectionElements_criteriaArgIsNull_throws() {
        final PgJobStore pgJobStore = newPgJobStore();
        try {
            pgJobStore.listChunksCollisionDetectionElements(null);
            fail("No exception thrown");
        } catch (NullPointerException e) {
        }
    }

    @Test
    public void listChunksCollisionDetectionElements_queryReturnsEmptyList_returnsEmptyCollisionDetectionElementList() {
        final Query query = mock(Query.class);
        when(entityManager.createNativeQuery(anyString(), eq(ChunkEntity.class))).thenReturn(query);
        when(query.getResultList()).thenReturn(Collections.emptyList());

        final PgJobStore pgJobStore = newPgJobStore();
        final List<CollisionDetectionElement> collisionDetectionElements = pgJobStore.listChunksCollisionDetectionElements(new ChunkListCriteria());
        assertThat("List of CollisionDetectionElement", collisionDetectionElements, is(notNullValue()));
        assertThat("List of CollisionDetectionElement is empty", collisionDetectionElements.isEmpty(), is(true));
    }

    @Test
    public void listChunksCollisionDetectionElements_queryReturnsNonEmptyList_returnsCollisionDetectionElementList() {
        final Query query = mock(Query.class);
        final SequenceAnalysisData mockedSequenceAnalysisData = mock(SequenceAnalysisData.class);
        final ChunkEntity chunkEntity1 = new ChunkEntity();
        chunkEntity1.setKey(new ChunkEntity.Key(1, 1));
        chunkEntity1.setSequenceAnalysisData(mockedSequenceAnalysisData);

        final ChunkEntity chunkEntity2 = new ChunkEntity();
        chunkEntity2.setKey(new ChunkEntity.Key(0, 1));
        chunkEntity2.setSequenceAnalysisData(mockedSequenceAnalysisData);

        when(entityManager.createNativeQuery(anyString(), eq(ChunkEntity.class))).thenReturn(query);
        when(query.getResultList()).thenReturn(Arrays.asList(chunkEntity1, chunkEntity2));


        final PgJobStore pgJobStore = newPgJobStore();
        final List<CollisionDetectionElement> collisionDetectionElements = pgJobStore.listChunksCollisionDetectionElements(new ChunkListCriteria());
        assertThat("List of CollisionDetectionElement", collisionDetectionElements, is(notNullValue()));
        assertThat("List of CollisionDetectionElement size", collisionDetectionElements.size(), is(2));
        assertThat("List of CollisionDetectionElement first element numberOfItems",
                ((ChunkIdentifier) collisionDetectionElements.get(0).getIdentifier()).getChunkId(), is((long) chunkEntity1.getKey().getId()));
        assertThat("List of CollisionDetectionElement second element numberOfItems",
                ((ChunkIdentifier) collisionDetectionElements.get(1).getIdentifier()).getChunkId(), is((long) chunkEntity2.getKey().getId()));
    }

    @Test
    public void getChunk_typeArgIsNull_throws() {
        final PgJobStore pgJobStore = newPgJobStore();
        try {
            pgJobStore.getChunk(null, 2, 1);
            fail("No exception thrown");
        } catch (NullPointerException e) {
        }
    }

    @Test
    public void getChunk_queryReturnsEmptyList_returnsNull() {
        final Query query = mock(Query.class);
        when(entityManager.createNativeQuery(anyString(), eq(ItemEntity.class))).thenReturn(query);
        when(query.getResultList()).thenReturn(Collections.emptyList());

        final PgJobStore pgJobStore = newPgJobStore();
        assertThat(pgJobStore.getChunk(ExternalChunk.Type.PARTITIONED, 2, 1), is(nullValue()));
    }

    @Test
    public void getChunk_queryReturnsItemEntityWithoutData_throws() {
        final Query query = mock(Query.class);
        when(entityManager.createNativeQuery(anyString(), eq(ItemEntity.class))).thenReturn(query);
        when(query.getResultList()).thenReturn(Collections.singletonList(new ItemEntity()));

        final PgJobStore pgJobStore = newPgJobStore();
        try {
            pgJobStore.getChunk(ExternalChunk.Type.PARTITIONED, 2, 1);
            fail("No exception thrown");
        } catch (NullPointerException e) {
        }
    }

    @Test
    public void getChunk_queryReturnsItemEntityWithData_returnsChunk() {
        final ItemData data1 = new ItemData(base64encode("data1"), StandardCharsets.UTF_8);
        final State state1 = new State();
        state1.getPhase(State.Phase.PARTITIONING).setSucceeded(1);
        final ItemEntity entity1 = new ItemEntity();
        entity1.setKey(new ItemEntity.Key(2, 1, (short) 0));
        entity1.setPartitioningOutcome(data1);
        entity1.setState(state1);

        final ItemData data2 = new ItemData(base64encode("data2"), StandardCharsets.ISO_8859_1);
        final State state2 = new State();
        state2.getPhase(State.Phase.PARTITIONING).setFailed(1);
        final ItemEntity entity2 = new ItemEntity();
        entity2.setKey(new ItemEntity.Key(2, 1, (short) 1));
        entity2.setPartitioningOutcome(data2);
        entity2.setState(state2);

        final Query query = mock(Query.class);
        when(entityManager.createNativeQuery(anyString(), eq(ItemEntity.class))).thenReturn(query);
        when(query.getResultList()).thenReturn(Arrays.asList(entity1, entity2));

        final PgJobStore pgJobStore = newPgJobStore();
        final ExternalChunk chunk = pgJobStore.getChunk(ExternalChunk.Type.PARTITIONED, 2, 1);
        assertThat("chunk", chunk, is(notNullValue()));
        assertThat("chunk.size()", chunk.size(), is(2));
        assertThat("chunk.getEncoding()", chunk.getEncoding(), is(data1.getEncoding()));
        final Iterator<ChunkItem> iterator = chunk.iterator();
        final ChunkItem firstChunkItem = iterator.next();
        assertThat("chunk[0].getId()", firstChunkItem.getId(), is((long) entity1.getKey().getId()));
        assertThat("chunk[0].getData()", StringUtil.asString(firstChunkItem.getData()), is(StringUtil.base64decode(data1.getData())));
        final ChunkItem secondChunkItem = iterator.next();
        assertThat("chunk[1].getId()", secondChunkItem.getId(), is((long) entity2.getKey().getId()));
        assertThat("chunk[1].getData()", StringUtil.asString(secondChunkItem.getData()), is(StringUtil.base64decode(data2.getData())));
    }

    @Test
    public void getItemData_itemEntityNotFound_throws() throws JobStoreException {
        final PgJobStore pgJobStore = newPgJobStore();
        when(entityManager.find(eq(ItemEntity.class), any(ItemEntity.Key.class))).thenReturn(null);
        try {
            pgJobStore.getItemData(DEFAULT_JOB_ID, DEFAULT_CHUNK_ID, DEFAULT_ITEM_ID, State.Phase.PARTITIONING);
            fail("No exception thrown");
        } catch (JobStoreException e) {}
    }

    @Test
    public void getItemData_phasePartitioning_returnsItemData() throws JobStoreException{
        final PgJobStore pgJobStore = newPgJobStore();

        ItemEntity itemEntity = getItemEntity(DEFAULT_JOB_ID, DEFAULT_CHUNK_ID, DEFAULT_ITEM_ID);
        when(entityManager.find(eq(ItemEntity.class), any(ItemEntity.Key.class))).thenReturn(itemEntity);

        final ItemData itemData = pgJobStore.getItemData(DEFAULT_JOB_ID, DEFAULT_CHUNK_ID, DEFAULT_ITEM_ID, State.Phase.PARTITIONING);
        assertThat("itemData not null", itemData, not(nullValue()));
        assertThat(String.format("itemData.data: {%s} expected to match: {%s}", itemData.getData(), itemEntity.getPartitioningOutcome().getData()),
                itemData.getData(), is(itemEntity.getPartitioningOutcome().getData()));
    }


    @Test
    public void getItemData_phaseProcessing_returnsItemData() throws JobStoreException{
        final PgJobStore pgJobStore = newPgJobStore();

        ItemEntity itemEntity = getItemEntity(DEFAULT_JOB_ID, DEFAULT_CHUNK_ID, DEFAULT_ITEM_ID);
        when(entityManager.find(eq(ItemEntity.class), any(ItemEntity.Key.class))).thenReturn(itemEntity);

        final ItemData itemData = pgJobStore.getItemData(DEFAULT_JOB_ID, DEFAULT_CHUNK_ID, DEFAULT_ITEM_ID, State.Phase.PROCESSING);
        assertThat("itemData not null", itemData, not(nullValue()));
        assertThat(String.format("itemData.data: {%s} expected to match: {%s}", itemData.getData(), itemEntity.getProcessingOutcome().getData()),
                itemData.getData(), is(itemEntity.getProcessingOutcome().getData()));
    }

    @Test
    public void getItemData_phaseDelivering_returnsItemData() throws JobStoreException{
        final PgJobStore pgJobStore = newPgJobStore();

        ItemEntity itemEntity = getItemEntity(DEFAULT_JOB_ID, DEFAULT_CHUNK_ID, DEFAULT_ITEM_ID);
        when(entityManager.find(eq(ItemEntity.class), any(ItemEntity.Key.class))).thenReturn(itemEntity);

        final ItemData itemData = pgJobStore.getItemData(DEFAULT_JOB_ID, DEFAULT_CHUNK_ID, DEFAULT_ITEM_ID, State.Phase.DELIVERING);
        assertThat("itemData not null", itemData, not(nullValue()));
        assertThat(String.format("itemData.data: {%s} expected to match: {%s}", itemData.getData(), itemEntity.getDeliveringOutcome().getData()),
                itemData.getData(), is(itemEntity.getDeliveringOutcome().getData()));
    }

    // Private methods
    private ItemEntity getItemEntity(int jobId, int chunkId, short itemId) {
        final ItemEntity.Key itemKey = new ItemEntity.Key(jobId, chunkId, itemId);
        final ItemEntity itemEntity = new ItemEntity();
        itemEntity.setKey(itemKey);
        itemEntity.setState(new State());
        itemEntity.setPartitioningOutcome(new ItemData("Partitioning data", Charset.defaultCharset()));
        itemEntity.setProcessingOutcome(new ItemData("processing data", Charset.defaultCharset()));
        itemEntity.setDeliveringOutcome(new ItemData("delivering data", Charset.defaultCharset()));
        return itemEntity;
    }

}