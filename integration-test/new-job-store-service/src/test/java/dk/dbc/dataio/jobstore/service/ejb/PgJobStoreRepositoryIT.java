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

import dk.dbc.dataio.commons.types.SupplementaryProcessData;
import dk.dbc.dataio.jobstore.service.entity.ChunkEntity;
import dk.dbc.dataio.jobstore.service.entity.ItemEntity;
import dk.dbc.dataio.jobstore.service.entity.JobEntity;
import dk.dbc.dataio.jobstore.service.partitioner.DanMarc2LineFormatDataPartitioner;
import dk.dbc.dataio.jobstore.service.partitioner.DataPartitioner;
import dk.dbc.dataio.jobstore.service.partitioner.RawRepoMarcXmlDataPartitioner;
import dk.dbc.dataio.jobstore.test.types.WorkflowNoteBuilder;
import dk.dbc.dataio.jobstore.types.JobStoreException;
import dk.dbc.dataio.jobstore.types.MarcRecordInfo;
import dk.dbc.dataio.jobstore.types.RecordInfo;
import dk.dbc.dataio.jobstore.types.ResourceBundle;
import dk.dbc.dataio.jobstore.types.WorkflowNote;
import org.junit.Test;

import javax.persistence.EntityTransaction;
import javax.persistence.Query;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

public class PgJobStoreRepositoryIT extends PgJobStoreRepositoryAbstractIT {


    public void createEmptyJobs(int... jobids) {
        final EntityTransaction transaction = entityManager.getTransaction();
        transaction.begin();
        for( int jobid: jobids ) {
            Query q=entityManager.createNativeQuery("INSERT INTO job (id,specification,state,flowstorereferences ) VALUES (?1, '{}'::JSONB, '{}'::JSON, '{}'::JSON );");
            q.setParameter(1, jobid);
            q.executeUpdate();
        }
        transaction.commit();
    }

    /**
     * Given: a job repository containing chunks from multiple jobs
     * When : the chunks are purged for a job
     * Then : all the chunks associated with the given job are deleted
     * And  : the remaining chunks do not belong to the purged job
     */

    @Test
    public void purgeChunks() {
        // Given...
        final int job1 = 1;
        final int job2 = 2;

        createEmptyJobs( 1, 2);
        newPersistedChunkEntity(new ChunkEntity.Key(0, job1));
        newPersistedChunkEntity(new ChunkEntity.Key(1, job1));
        newPersistedChunkEntity(new ChunkEntity.Key(0, job2));
        newPersistedChunkEntity(new ChunkEntity.Key(1, job2));
        newPersistedChunkEntity(new ChunkEntity.Key(2, job2));
        // When...
        final EntityTransaction transaction = entityManager.getTransaction();
        transaction.begin();
        assertThat("Number of chunks purged", pgJobStoreRepository.purgeChunks(job2), is(3));
        transaction.commit();

        // Then...
        final List<ChunkEntity> remainingChunks = findAllChunks();
        assertThat("Number of remaining chunks", remainingChunks.size(), is(2));

        // And...
        for (ChunkEntity entity : remainingChunks) {
            assertThat("Job ID of remaining chunk", entity.getKey().getJobId(), is(not(job2)));
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
        final int job1 = 1;
        final int job2 = 2;
        final int chunkId = 0;

        createEmptyJobs( 1, 2);
        createEmptyChunksForJob(1, 0);
        createEmptyChunksForJob(2, 0);

        newPersistedItemEntity(new ItemEntity.Key(job1, chunkId, (short)0));
        newPersistedItemEntity(new ItemEntity.Key(job1, chunkId, (short)1));
        newPersistedItemEntity(new ItemEntity.Key(job2, chunkId, (short)0));
        newPersistedItemEntity(new ItemEntity.Key(job2, chunkId, (short)1));
        newPersistedItemEntity(new ItemEntity.Key(job2, chunkId, (short)2));

        // When...
        final EntityTransaction transaction = entityManager.getTransaction();
        transaction.begin();
        assertThat("Number of items purged", pgJobStoreRepository.purgeItems(job2), is(3));
        transaction.commit();

        // Then...
        final List<ItemEntity> remainingItems = findAllItems();
        assertThat("Number of remaining items", remainingItems.size(), is(2));

        // And...
        for (ItemEntity entity : remainingItems) {
            assertThat("Job ID of remaining item", entity.getKey().getJobId(), is(not(job2)));
        }
    }

    private void createEmptyChunksForJob(int jobId, int chunkId) {
        entityManager.getTransaction().begin();
        Query q=entityManager.createNativeQuery("INSERT INTO chunk (jobid, id, datafileid, sequenceanalysisdata, state ) VALUES (?1,?2,'test','{}'::JSON, '{}'::JSON);");
        q.setParameter(1,jobId);
        q.setParameter(2,chunkId);
        q.executeUpdate();
        entityManager.getTransaction().commit();
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
        final JobEntity job = newPersistedJobEntity();
        newPersistedChunkEntity(new ChunkEntity.Key(0, job.getId()));
        newPersistedItemEntity(new ItemEntity.Key(job.getId(), chunkId, (short)0));
        newPersistedItemEntity(new ItemEntity.Key(job.getId(), chunkId, (short)1));

        // When...
        final EntityTransaction transaction = entityManager.getTransaction();
        transaction.begin();
        final JobEntity purgedJob = pgJobStoreRepository.resetJob(job.getId());
        transaction.commit();

        // Then...
        assertThat(purgedJob.getId(), is(job.getId()));
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
        final JobEntity jobEntity = newPersistedJobEntity();
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
        final JobEntity jobEntity = newPersistedJobEntity();
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
        final JobEntity jobEntity = newPersistedJobEntity();
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


    /*
     * private methods
     */


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
