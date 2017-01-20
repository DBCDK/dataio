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

import dk.dbc.dataio.commons.types.Chunk;
import dk.dbc.dataio.commons.types.ChunkItem;
import static dk.dbc.dataio.commons.types.ChunkItem.Status.SUCCESS;
import static dk.dbc.dataio.commons.types.ChunkItem.Type.TICKLE_JOB_END;
import dk.dbc.dataio.commons.types.SupplementaryProcessData;
import dk.dbc.dataio.commons.utils.lang.StringUtil;
import dk.dbc.dataio.commons.utils.test.model.ChunkItemBuilder;
import dk.dbc.dataio.jobstore.service.entity.ChunkEntity;
import dk.dbc.dataio.jobstore.service.entity.ItemEntity;
import dk.dbc.dataio.jobstore.service.entity.JobEntity;
import dk.dbc.dataio.jobstore.service.partitioner.DanMarc2LineFormatDataPartitioner;
import dk.dbc.dataio.jobstore.service.partitioner.DataPartitioner;
import dk.dbc.dataio.jobstore.service.partitioner.RawRepoMarcXmlDataPartitioner;
import dk.dbc.dataio.jobstore.test.types.WorkflowNoteBuilder;
import dk.dbc.dataio.jobstore.types.InvalidInputException;
import dk.dbc.dataio.jobstore.types.JobStoreException;
import dk.dbc.dataio.jobstore.types.MarcRecordInfo;
import dk.dbc.dataio.jobstore.types.RecordInfo;
import dk.dbc.dataio.jobstore.types.ResourceBundle;
import dk.dbc.dataio.jobstore.types.State;
import dk.dbc.dataio.jobstore.types.WorkflowNote;
import static java.lang.String.format;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import org.junit.Test;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class PgJobStoreRepositoryIT extends PgJobStoreRepositoryAbstractIT {

    /**
     * Given: a job repository containing chunks from multiple jobs
     * When : the chunks are purged for a job
     * Then : all the chunks associated with the given job are deleted
     * And  : the remaining chunks do not belong to the purged job
     */
    @Test
    public void purgeChunks() {
        // Given...
        final int jobId1 = newPersistedJobEntity().getId();
        final int jobId2 = newPersistedJobEntity().getId();

        newPersistedChunkEntity(new ChunkEntity.Key(0, jobId1));
        newPersistedChunkEntity(new ChunkEntity.Key(1, jobId1));
        newPersistedChunkEntity(new ChunkEntity.Key(0, jobId2));
        newPersistedChunkEntity(new ChunkEntity.Key(1, jobId2));
        newPersistedChunkEntity(new ChunkEntity.Key(2, jobId2));

        // When...
        final int numberOfPurgedChunks = persistenceContext.run(() ->
                pgJobStoreRepository.purgeChunks(jobId2)
        );

        // Then...
        assertThat("Number of chunks purged", numberOfPurgedChunks, is(3));
        final List<ChunkEntity> remainingChunks = findAllChunks();
        assertThat("Number of remaining chunks", remainingChunks.size(), is(2));

        // And...
        for (ChunkEntity entity : remainingChunks) {
            assertThat("Job ID of remaining chunk", entity.getKey().getJobId(), is(not(jobId2)));
        }
    }

    /**
     * Given: a job repository containing items from multiple jobs
     * When : the items are purged for a job
     * Then : all the items associated with the given job are deleted
     * And  : the remaining items do not belong to the purged job
     */
    @Test
    public void purgeItems() {
        // Given...
        final int jobId1 = newPersistedJobEntity().getId();
        final int jobId2 = newPersistedJobEntity().getId();
        final int chunkId = 0;
        newPersistedChunkEntity(new ChunkEntity.Key(0, jobId1));
        newPersistedChunkEntity(new ChunkEntity.Key(0, jobId2));

        newPersistedItemEntity(new ItemEntity.Key(jobId1, chunkId, (short)0));
        newPersistedItemEntity(new ItemEntity.Key(jobId1, chunkId, (short)1));
        newPersistedItemEntity(new ItemEntity.Key(jobId2, chunkId, (short)0));
        newPersistedItemEntity(new ItemEntity.Key(jobId2, chunkId, (short)1));
        newPersistedItemEntity(new ItemEntity.Key(jobId2, chunkId, (short)2));

        // When...
        final int numberOfPurgedItems = persistenceContext.run(() ->
                pgJobStoreRepository.purgeItems(jobId2)
        );

        // Then...
        assertThat("Number of items purged", numberOfPurgedItems, is(3));
        final List<ItemEntity> remainingItems = findAllItems();
        assertThat("Number of remaining items", remainingItems.size(), is(2));

        // And...
        for (ItemEntity entity : remainingItems) {
            assertThat("Job ID of remaining item", entity.getKey().getJobId(), is(not(jobId2)));
        }
    }

    /**
     * Given: a job repository containing job with chunks and items
     * When : the job is reset
     * Then : the purged job is returned
     * And  : all chunks associated with the job are deleted
     * And  : all items associated with the job are deleted
     */
    @Test
    public void resetJob() {
        // Given...
        final int chunkId = 0;
        final int jobId = newPersistedJobEntity().getId();
        newPersistedChunkEntity(new ChunkEntity.Key(0, jobId));
        newPersistedItemEntity(new ItemEntity.Key(jobId, chunkId, (short)0));
        newPersistedItemEntity(new ItemEntity.Key(jobId, chunkId, (short)1));

        // When...
        final JobEntity purgedJob = persistenceContext.run(() ->
                pgJobStoreRepository.resetJob(jobId)
        );

        // Then...
        assertThat(purgedJob.getId(), is(jobId));
        // And...
        assertThat(findAllChunks().size(), is(0));
        // And..
        assertThat(findAllItems().size(), is(0));
    }

    /**
     * Given: a job repository containing one job and one chunk
     * When : the item entity is created
     * Then : record info of type MarcRecordInfo is set on the item entity containing the expected record id
     */
    @Test
    public void createChunkItemEntities_setsRecordInfo() {
        // Given...
        final JobEntity jobEntity = newPersistedJobEntityWithSinkAndFlowCache();
        final ChunkEntity chunkEntity = newPersistedChunkEntity(new ChunkEntity.Key(0, jobEntity.getId()));
        final DataPartitioner dataPartitioner = getDanMarc2LineFormatDataPartitioner("/test-record-danmarc2.lin");

        // When...
        persistenceContext.run(() -> pgJobStoreRepository.createChunkItemEntities(
                jobEntity.getId(), chunkEntity.getKey().getId(), (short) 10, dataPartitioner)
        );

        // Then...
        final List<ItemEntity> itemEntities = findAllItems();
        assertThat("itemEntities.size", itemEntities.size(), is(1));

        final RecordInfo recordInfo = itemEntities.get(0).getRecordInfo();
        assertThat("recordInfo is instanceof MarcRecordInfo", recordInfo instanceof MarcRecordInfo, is(true));
        assertThat("recordInfo.id", recordInfo.getId(), is("112613"));
    }

    /**
     * Given: a job repository containing one job and one chunk
     * When : the item entity is created
     * Then : the dataio specific tracking id is generated and set on the item entity
     */
    @Test
    public void createChunkItemEntities_setsDataioTrackingId() throws UnknownHostException {
        // Given...
        final JobEntity jobEntity = newPersistedJobEntityWithSinkAndFlowCache();
        final ChunkEntity chunkEntity = newPersistedChunkEntity(new ChunkEntity.Key(0, jobEntity.getId()));
        final DataPartitioner dataPartitioner = getDanMarc2LineFormatDataPartitioner("/test-record-danmarc2.lin");
        final String expectedTrackingId = InetAddress.getLocalHost().getHostAddress()
                + "-" + jobEntity.getId() + "-" + chunkEntity.getKey().getId() + "-" + 0;

        // When...
        persistenceContext.run(() -> pgJobStoreRepository.createChunkItemEntities(
                jobEntity.getId(), chunkEntity.getKey().getId(), (short) 10, dataPartitioner)
        );

        // Then...
        final List<ItemEntity> itemEntities = findAllItems();
        assertThat("itemEntities.size", itemEntities.size(), is(1));
        assertThat("itemEntity.trackingId", itemEntities.get(0).getPartitioningOutcome().getTrackingId(), is(expectedTrackingId));
    }

    /**
     * Given: a job repository containing one job and one chunk
     * When : the item entity is created
     * Then : the tracking id harvested is set on the item entity
     */
    @Test
    public void createChunkItemEntities_setsHarvestedTrackingId() throws UnknownHostException {
        // Given...
        final JobEntity jobEntity = newPersistedJobEntityWithSinkAndFlowCache();
        final ChunkEntity chunkEntity = newPersistedChunkEntity(new ChunkEntity.Key(0, jobEntity.getId()));
        final DataPartitioner dataPartitioner = getRawRepoMarcXmlDataPartitioner("/datacontainer-with-tracking-id.xml");

        // When...
        persistenceContext.run(() -> pgJobStoreRepository.createChunkItemEntities(
                jobEntity.getId(), chunkEntity.getKey().getId(), (short) 10, dataPartitioner)
        );

        // Then...
        final List<ItemEntity> itemEntities = findAllItems();
        assertThat("itemEntities.size", itemEntities.size(), is(1));
        assertThat("itemEntity.trackingId", itemEntities.get(0).getPartitioningOutcome().getTrackingId(), is("123456789"));
    }

    /**
     * Given: a job store where a job exists
     * When : requesting a resource bundle for the existing job
     * Then : the resource bundle contains the correct flow, sink and supplementary process data
     */
    @Test
    public void getResourceBundle() throws JobStoreException {
        // Given...
        final JobEntity jobEntity = newPersistedJobEntityWithSinkAndFlowCache();

        // When...
        ResourceBundle resourceBundle = pgJobStoreRepository.getResourceBundle(jobEntity.getId());

        // Then...
        assertThat("ResourceBundle", resourceBundle, not(nullValue()));
        assertThat("ResourceBundle.flow", resourceBundle.getFlow(), is(jobEntity.getCachedFlow().getFlow()));
        assertThat("ResourceBundle.sink", resourceBundle.getSink(), is(jobEntity.getCachedSink().getSink()));

        SupplementaryProcessData supplementaryProcessData = resourceBundle.getSupplementaryProcessData();
        assertThat("ResourceBundle.supplementaryProcessData.submitter", supplementaryProcessData.getSubmitter(), is(jobEntity.getSpecification().getSubmitterId()));
        assertThat("ResourceBundle.supplementaryProcessData.format", supplementaryProcessData.getFormat(), is(jobEntity.getSpecification().getFormat()));
    }

    /**
     * Given: an a job store containing a job entity
     * When : calling setWorkflowNote()
     * Then : the job entity is updated
     */
    @Test
    public void setJobEntityWorkFlowNote_jobEntityUpdated() {
        // Given...
        final JobEntity jobEntity = newPersistedJobEntity();
        final WorkflowNote workflowNote = new WorkflowNoteBuilder().build();

        // When...
        persistenceContext.run(() ->
                pgJobStoreRepository.setJobEntityWorkFlowNote(workflowNote, jobEntity.getId())
        );

        // Then...
        assertThat("JobEntity.workflowNote", jobEntity.getWorkflowNote(), is(workflowNote));
    }

    /**
     * Given: an a job store containing a persisted item entity
     * When : calling setWorkflowNote() on a the item
     * Then : the item entity is updated
     */
    @Test
    public void setItemEntityWorkFlowNote_itemEntityUpdated() {
        // Given...
        final JobEntity jobEntity = newPersistedJobEntity();

        final int jobId = jobEntity.getId();
        final int chunkId = 0;
        final short itemId = 0;

        newPersistedChunkEntity(new ChunkEntity.Key(chunkId, jobId));
        final ItemEntity itemEntity = newPersistedItemEntity(new ItemEntity.Key(jobId, chunkId, itemId));
        final WorkflowNote workflowNote = new WorkflowNoteBuilder().build();

        // When...
        persistenceContext.run(() ->
                pgJobStoreRepository.setItemEntityWorkFlowNote(workflowNote, jobId, chunkId, itemId)
        );

        // Then...
        assertThat("ItemEntity.workflowNote", itemEntity.getWorkflowNote(), is(workflowNote));
    }

    /**
     * Given: a job store containing one job with one chunk and one item that has completed partitioning
     * When : attempting to retrieve chunks for existing job and chunk id with type PARTITIONING
     * Then : one chunk containing the expected chunk item is returned
     */
    @Test
    public void getChunk() {
        // Given...
        final JobEntity jobEntity = newPersistedJobEntity();
        final int jobId = jobEntity.getId();
        final int chunkId = 0;
        final short itemId = 0;
        newPersistedChunkEntity(new ChunkEntity.Key(chunkId, jobId));
        final ItemEntity itemEntity = newPersistedItemEntityWithChunkItemsSet(new ItemEntity.Key(jobId, chunkId, itemId));

        // When...
        final Chunk chunk = pgJobStoreRepository.getChunk(Chunk.Type.PARTITIONED, jobId, chunkId);

        // Then...
        assertThat("chunk", chunk, is(notNullValue()));
        assertThat("chunk.size()", chunk.size(), is(1));
        assertThat("chunk.chunkItem.partitioningOutcome", chunk.getItems().get(0), is(itemEntity.getPartitioningOutcome()));
    }

    /**
     * Given    : a job store containing one job with one chunk and one item that has successfully completed all phases.
     * When     : requesting chunk item for phase: PARTITIONING
     * Then     : the expected chunk item is returned
     * And when : requesting chunk item for phase: PROCESSING
     * Then     : the expected chunk item is returned
     * And when : requesting chunk item for phase: DELIVERING
     * Then     : the expected chunk item is returned
     */
    @Test
    public void getChunkItemForPhase() throws InvalidInputException {
        final int jobId = newPersistedJobEntity().getId();
        final int chunkId = 0;
        final short itemId = 0;
        newPersistedChunkEntity(new ChunkEntity.Key(chunkId, jobId));
        final ItemEntity itemEntity = newPersistedItemEntityWithChunkItemsSet(new ItemEntity.Key(jobId, chunkId, itemId));

        // When...
        final ChunkItem partitionedChunkItem = pgJobStoreRepository.getChunkItemForPhase(jobId, chunkId, itemId, State.Phase.PARTITIONING);

        // Then...
        assertThat("partitionedChunkItem", partitionedChunkItem, is(itemEntity.getPartitioningOutcome()));

        // And When...
        final ChunkItem processedChunkItem = pgJobStoreRepository.getChunkItemForPhase(jobId, chunkId, itemId, State.Phase.PROCESSING);

        // Then...
        assertThat("processedChunkItem", processedChunkItem, is(itemEntity.getProcessingOutcome()));

        // And When...
        final ChunkItem deliveredChunkItem = pgJobStoreRepository.getChunkItemForPhase(jobId, chunkId, itemId, State.Phase.DELIVERING);

        // Then...
        assertThat("deliveredChunkItem", deliveredChunkItem, is(itemEntity.getDeliveringOutcome()));
    }

    @Test
    public void createJobTerminationChunkEntity() throws Exception {

        final String TEST_FILE_NAME = "TestFileName";
        short chunkId=0;
        short itemId=0;
        // Given...
        final int jobId = newPersistedJobEntity().getId();

        // When...
        final ChunkEntity chunkEntity = persistenceContext.run(() -> pgJobStoreRepository.createJobTerminationChunkEntity(jobId, chunkId, TEST_FILE_NAME, SUCCESS) );


        // Then ChunkEntity is
        assertThat("Chunk id", chunkEntity.getKey(), is( new ChunkEntity.Key(chunkId, jobId)));
        assertThat("Chunk Items ", chunkEntity.getNumberOfItems(), is( (short)1));
        assertThat("Chunk DatafileId", chunkEntity.getDataFileId(), is(TEST_FILE_NAME));

        final State chunkState = chunkEntity.getState();
        assertThat(" Item State Diagnostics",          chunkState.getDiagnostics().size(), is(0));
        assertThat(" Item State PARTITIONING endData", chunkState.getPhase(State.Phase.PARTITIONING ).getEndDate(), is(notNullValue()));
        assertThat(" Item State Processing endDate",   chunkState.getPhase(State.Phase.PROCESSING ).getEndDate(), is(notNullValue()));
        assertThat(" Item State Delivering end date",  chunkState.getPhase(State.Phase.DELIVERING ).getEndDate(), is(nullValue()));

        // And Item is

        ItemEntity.Key key = new ItemEntity.Key(jobId, chunkId, itemId);
        final ItemEntity itemEntity = persistenceContext.run(()->entityManager.find(ItemEntity.class, key) );


        assertThat("Item record Id", itemEntity.getRecordInfo().getId(), is("End Item"));
        assertThat("Item Diagnostics", itemEntity.getState().getDiagnostics(), empty());
        assertThat("Item", itemEntity.getProcessingOutcome().getTrackingId(), is(format("TickleEndItem for Job %d", jobId)));

        final ChunkItem partitioningOutcome = itemEntity.getPartitioningOutcome();
        List<ChunkItem.Type> itemTypeList= partitioningOutcome.getType();
        assertThat("Item Type list size ",itemTypeList.size(), is(1));
        assertThat("Item Type list type",itemTypeList.get(0), is(TICKLE_JOB_END));

        assertThat("Item", new String(partitioningOutcome.getData()), is("Tickle Job Termination Item"));
        assertThat("Item", partitioningOutcome.getStatus(), is(SUCCESS));


        final ChunkItem processingOutcome = itemEntity.getPartitioningOutcome();
        itemTypeList= processingOutcome.getType();
        assertThat("Item Type list size ",itemTypeList.size(), is(1));
        assertThat("Item Type list type",itemTypeList.get(0), is(TICKLE_JOB_END));

        assertThat("Item", new String(processingOutcome.getData()), is("Tickle Job Termination Item"));
        assertThat("Item", processingOutcome.getStatus(), is(SUCCESS));

        final State itemState = itemEntity.getState();
        assertThat(" Item State Diagnostics", itemState.getDiagnostics().size(), is(0));
        assertThat(" Item State PARTITIONING endData", itemState.getPhase(State.Phase.PARTITIONING ).getEndDate(), is(notNullValue()));
        assertThat(" Item State Processing endDate", itemState.getPhase(State.Phase.PROCESSING ).getEndDate(), is(notNullValue()));
        assertThat(" Item State Delivering end date", itemState.getPhase(State.Phase.DELIVERING ).getEndDate(), is(nullValue()));

        

        // And job1 is
        final JobEntity job1 = persistenceContext.run( () -> pgJobStoreRepository.getJobEntityById(jobId));

        assertThat("Number of chunks", job1.getNumberOfChunks(), is(1));
        assertThat("Number of Items ", job1.getNumberOfItems(), is(1));

    }


/*
     * private methods
     */

    private ItemEntity newPersistedItemEntityWithChunkItemsSet(ItemEntity.Key key) {
        final ItemEntity itemEntity = newItemEntityWithChunkItemsSet(key);
        persist(itemEntity);
        return itemEntity;
    }

    private ItemEntity newItemEntityWithChunkItemsSet(ItemEntity.Key key) {
        final ItemEntity itemEntity = newItemEntity(key);
        itemEntity.setPartitioningOutcome(new ChunkItemBuilder().setData(StringUtil.asBytes("Partitioning outcome data")).build());
        itemEntity.setProcessingOutcome(new ChunkItemBuilder().setData(StringUtil.asBytes("Processing outcome data")).build());
        itemEntity.setDeliveringOutcome(new ChunkItemBuilder().setData(StringUtil.asBytes("Delivering outcome data")).build());
        return itemEntity;
    }

    private JobEntity newPersistedJobEntityWithSinkAndFlowCache() {
        final JobEntity jobEntity = newJobEntityWithSinkAndFlowCache();
        persist(jobEntity);
        return jobEntity;
    }

    private JobEntity newJobEntityWithSinkAndFlowCache() {
        final JobEntity jobEntity = newJobEntity();
        jobEntity.setCachedSink(newPersistedSinkCacheEntity());
        jobEntity.setCachedFlow(newPersistedFlowCacheEntity());
        return jobEntity;
    }

    private DanMarc2LineFormatDataPartitioner getDanMarc2LineFormatDataPartitioner(String resourceName) {
        return DanMarc2LineFormatDataPartitioner.newInstance(getClass().getResourceAsStream(resourceName), "latin1");
    }

    private RawRepoMarcXmlDataPartitioner getRawRepoMarcXmlDataPartitioner(String resourceName) {
        return RawRepoMarcXmlDataPartitioner.newInstance(getClass().getResourceAsStream(resourceName), StandardCharsets.UTF_8.name());
    }

}
