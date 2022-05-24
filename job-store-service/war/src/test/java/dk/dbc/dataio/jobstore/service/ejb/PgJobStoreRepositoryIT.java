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
import dk.dbc.dataio.commons.types.Flow;
import dk.dbc.dataio.commons.utils.lang.StringUtil;
import dk.dbc.dataio.commons.utils.test.model.ChunkItemBuilder;
import dk.dbc.dataio.jobstore.service.dependencytracking.DefaultKeyGenerator;
import dk.dbc.dataio.jobstore.service.dependencytracking.KeyGenerator;
import dk.dbc.dataio.jobstore.service.entity.ChunkEntity;
import dk.dbc.dataio.jobstore.service.entity.ItemEntity;
import dk.dbc.dataio.jobstore.service.entity.JobEntity;
import dk.dbc.dataio.jobstore.service.partitioner.AddiDataPartitioner;
import dk.dbc.dataio.jobstore.service.partitioner.DanMarc2LineFormatDataPartitioner;
import dk.dbc.dataio.jobstore.service.partitioner.DataPartitioner;
import dk.dbc.dataio.jobstore.service.partitioner.DefaultXmlDataPartitioner;
import dk.dbc.dataio.jobstore.service.partitioner.IncludeFilterDataPartitioner;
import dk.dbc.dataio.jobstore.test.types.WorkflowNoteBuilder;
import dk.dbc.dataio.jobstore.types.InvalidInputException;
import dk.dbc.dataio.jobstore.types.JobStoreException;
import dk.dbc.dataio.jobstore.types.MarcRecordInfo;
import dk.dbc.dataio.jobstore.types.RecordInfo;
import dk.dbc.dataio.jobstore.types.State;
import dk.dbc.dataio.jobstore.types.WorkflowNote;
import org.junit.Test;

import javax.persistence.Query;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.BitSet;
import java.util.List;

import static dk.dbc.dataio.commons.types.ChunkItem.Status.SUCCESS;
import static dk.dbc.dataio.commons.types.ChunkItem.Type.JOB_END;
import static java.lang.String.format;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertNotNull;
import static org.hamcrest.MatcherAssert.assertThat;

public class PgJobStoreRepositoryIT extends PgJobStoreRepositoryAbstractIT {
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
        persistenceContext.run(() -> pgJobStoreRepository.createChunkItemEntities(101010,
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
    public void createChunkItemEntities_setsDataioTrackingId() {
        // Given...
        final JobEntity jobEntity = newPersistedJobEntityWithSinkAndFlowCache();
        final ChunkEntity chunkEntity = newPersistedChunkEntity(new ChunkEntity.Key(0, jobEntity.getId()));
        final DataPartitioner dataPartitioner = getDanMarc2LineFormatDataPartitioner("/test-record-danmarc2.lin");
        final String expectedTrackingId = "{112613:101010}-" +
                jobEntity.getId() + "-" + chunkEntity.getKey().getId() + "-" + 0;

        // When...
        persistenceContext.run(() -> pgJobStoreRepository.createChunkItemEntities(101010,
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
    public void createChunkItemEntities_setsHarvestedTrackingId()  {
        // Given...
        final JobEntity jobEntity = newPersistedJobEntityWithSinkAndFlowCache();
        final ChunkEntity chunkEntity = newPersistedChunkEntity(new ChunkEntity.Key(0, jobEntity.getId()));
        final InputStream addiStream = StringUtil.asInputStream("27\n{\"trackingId\": \"trackedAs\"}\n7\ncontent\n");
        final DataPartitioner dataPartitioner = getAddiDataPartitioner(addiStream);

        // When...
        persistenceContext.run(() -> pgJobStoreRepository.createChunkItemEntities(101010,
                jobEntity.getId(), chunkEntity.getKey().getId(), (short) 10, dataPartitioner)
        );

        // Then...
        final List<ItemEntity> itemEntities = findAllItems();
        assertThat("itemEntities.size", itemEntities.size(), is(1));
        assertThat("itemEntity.trackingId", itemEntities.get(0).getPartitioningOutcome().getTrackingId(), is("trackedAs"));
    }

    @Test
    public void createChunkItemEntities_nonMarcRecordTrackingId() throws UnknownHostException {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                "<toplevel>" +
                "<child>\"I feel the color in my cheeks rising again. I must " +
                "be the color of The Communist Manifesto.\"</child>" +
                "<child>\"Inside, I'm doing graceful cartwheels in my head, " +
                "knowing full well that's the only place I can do graceful cartwheels.\"</child>" +
                "</toplevel>";
        ByteArrayInputStream inputStream = new ByteArrayInputStream(
                xml.getBytes(StandardCharsets.UTF_8));
        final JobEntity jobEntity = newPersistedJobEntityWithSinkAndFlowCache();
        final ChunkEntity chunkEntity = newPersistedChunkEntity(
                new ChunkEntity.Key(0, jobEntity.getId()));
        final DefaultXmlDataPartitioner partitioner = DefaultXmlDataPartitioner.newInstance(
                inputStream, StandardCharsets.UTF_8.name());
        final String expectedTrackingId = InetAddress.getLocalHost().getHostAddress()
                + "-" + jobEntity.getId() + "-" + chunkEntity.getKey().getId() + "-" + 0;

        persistenceContext.run(() -> pgJobStoreRepository.createChunkItemEntities(
                101010, jobEntity.getId(), chunkEntity.getKey().getId(), (short) 10, partitioner));

        final List<ItemEntity> itemEntities = findAllItems();
        assertThat("number of items", itemEntities.size(), is(2));
        final Query query = entityManager.createQuery(
                "SELECT e FROM ItemEntity e WHERE e.key.id = 0");
        ItemEntity item = (ItemEntity) query.getSingleResult();
        assertNotNull("has partitioningOutcome", item.getPartitioningOutcome());
        assertThat("trackingId", item.getPartitioningOutcome()
                .getTrackingId(), is(expectedTrackingId));
    }

    @Test
    public void createChunkItemEntities_setsPositionInDatafile() {
        final String xml =
                "<toplevel>" +
                        "<child>one</child>" +
                        "<child>two</child>" +
                        "<child>three</child>" +
                        "<child>four</child>" +
                        "<child>five</child>" +
                        "<child>six</child>" +
                        "<child>seven</child>" +
                        "<child>eight</child>" +
                        "<child>nine</child>" +
                        "<child>ten</child>" +
                        "</toplevel>";

        final JobEntity job = newPersistedJobEntityWithSinkAndFlowCache();
        final ChunkEntity chunk = newPersistedChunkEntity(new ChunkEntity.Key(0, job.getId()));

        final BitSet includeFilter = new BitSet();
        includeFilter.set(0);
        includeFilter.set(2);
        includeFilter.set(4);
        includeFilter.set(6);
        includeFilter.set(8);

        final IncludeFilterDataPartitioner partitioner = IncludeFilterDataPartitioner.newInstance(
                DefaultXmlDataPartitioner.newInstance(new ByteArrayInputStream(
                        xml.getBytes()), StandardCharsets.UTF_8.name()), includeFilter);

        final PgJobStoreRepository.ChunkItemEntities chunkItemEntities =
                persistenceContext.run(() -> pgJobStoreRepository.createChunkItemEntities(
                        101010, job.getId(), chunk.getKey().getId(), (short) 10, partitioner));
        assertThat("number of items", chunkItemEntities.size(), is((short) 5));
        assertThat("1st item position in datafile", chunkItemEntities.entities.get(0).getPositionInDatafile(), is(0));
        assertThat("2nd item position in datafile", chunkItemEntities.entities.get(1).getPositionInDatafile(), is(2));
        assertThat("3rd item position in datafile", chunkItemEntities.entities.get(2).getPositionInDatafile(), is(4));
        assertThat("4th item position in datafile", chunkItemEntities.entities.get(3).getPositionInDatafile(), is(6));
        assertThat("5th item position in datafile", chunkItemEntities.entities.get(4).getPositionInDatafile(), is(8));
    }

    @Test
    public void createChunkEntity_maintainsSkippedCount() {
        final JobEntity jobEntity = newPersistedJobEntityWithSinkAndFlowCache();
        final long submitter = jobEntity.getSpecification().getSubmitterId();
        final String dataFileId = jobEntity.getSpecification().getDataFile();
        final KeyGenerator keyGenerator = new DefaultKeyGenerator();

        final BitSet includeFilter = new BitSet();
        includeFilter.set(2);

        final DataPartitioner wrappedDataPartitioner =
                getDanMarc2LineFormatDataPartitioner("/test-records-74-danmarc2.lin");
        final DataPartitioner dataPartitioner =
                IncludeFilterDataPartitioner.newInstance(wrappedDataPartitioner, includeFilter);

        persistenceContext.run(() -> pgJobStoreRepository.createChunkEntity(
                submitter, jobEntity.getId(), 0, (short) 10, dataPartitioner,
                keyGenerator, dataFileId)
        );

        assertThat("skipped", jobEntity.getSkipped(), is(73));
    }

    /**
     * Given: a job store where a job exists
     * When : requesting a flow bundle for the existing job
     * Then : the cashed flow is returned
     */
    @Test
    public void getCachedFlow() throws JobStoreException {
        // Given...
        final JobEntity jobEntity = newPersistedJobEntityWithSinkAndFlowCache();

        // When...
        Flow cachedFlow = pgJobStoreRepository.getCachedFlow(jobEntity.getId());

        // Then...
        assertThat("flow", cachedFlow, is(jobEntity.getCachedFlow().getFlow()));
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


        assertThat("Item record Id", itemEntity.getRecordInfo().getId(), is("EndItem"));
        assertThat("Item Diagnostics", itemEntity.getState().getDiagnostics(), empty());
        assertThat("Item", itemEntity.getProcessingOutcome().getTrackingId(), is(format("%d.JOB_END", jobId)));

        final ChunkItem partitioningOutcome = itemEntity.getPartitioningOutcome();
        List<ChunkItem.Type> itemTypeList= partitioningOutcome.getType();
        assertThat("Item Type list size ",itemTypeList.size(), is(1));
        assertThat("Item Type list type",itemTypeList.get(0), is(JOB_END));

        assertThat("Item", new String(partitioningOutcome.getData()), is("Job termination item"));
        assertThat("Item", partitioningOutcome.getStatus(), is(SUCCESS));


        final ChunkItem processingOutcome = itemEntity.getPartitioningOutcome();
        itemTypeList= processingOutcome.getType();
        assertThat("Item Type list size ",itemTypeList.size(), is(1));
        assertThat("Item Type list type",itemTypeList.get(0), is(JOB_END));

        assertThat("Item", new String(processingOutcome.getData()), is("Job termination item"));
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

    private AddiDataPartitioner getAddiDataPartitioner(InputStream inputStream) {
        return AddiDataPartitioner.newInstance(inputStream, StandardCharsets.UTF_8.name());
    }

}
