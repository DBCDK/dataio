package dk.dbc.dataio.jobstore.service.ejb;

import dk.dbc.dataio.commons.types.Flow;
import dk.dbc.dataio.commons.utils.test.model.FlowBuilder;
import dk.dbc.dataio.jobstore.service.entity.ItemEntity;
import dk.dbc.dataio.jobstore.service.entity.JobEntity;
import dk.dbc.dataio.jobstore.test.types.WorkflowNoteBuilder;
import dk.dbc.dataio.jobstore.types.JobStoreException;
import dk.dbc.dataio.jobstore.types.WorkflowNote;
import org.junit.Test;

import javax.persistence.LockModeType;

import static dk.dbc.commons.testutil.Assert.assertThat;
import static dk.dbc.commons.testutil.Assert.isThrowing;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

public class PgJobStoreRepositoryTest extends PgJobStoreBaseTest {

    @Test
    public void setWorkflowNote_jobEntityNotFound_throws() {
        when(entityManager.find(JobEntity.class, DEFAULT_JOB_ID, LockModeType.PESSIMISTIC_WRITE)).thenReturn(null);

        final PgJobStoreRepository pgJobStoreRepository = newPgJobStoreReposity();
        final WorkflowNote workflowNote = new WorkflowNoteBuilder().build();

        try {
            pgJobStoreRepository.setJobEntityWorkFlowNote(workflowNote, DEFAULT_JOB_ID);
            fail("No exception thrown");
        } catch (JobStoreException e) {}
    }

    @Test
    public void setWorkflowNote_jobEntityFound_returnsUpdatedJobEntityWithGivenWorkflowNote() throws JobStoreException {
        when(entityManager.find(JobEntity.class, DEFAULT_JOB_ID, LockModeType.PESSIMISTIC_WRITE)).thenReturn(new JobEntity());

        final PgJobStoreRepository pgJobStoreRepository = newPgJobStoreReposity();
        final WorkflowNote workflowNote = new WorkflowNoteBuilder().build();

        JobEntity updatedJobEntity = pgJobStoreRepository.setJobEntityWorkFlowNote(workflowNote, DEFAULT_JOB_ID);
        assertThat(updatedJobEntity, is(notNullValue()));
        assertThat(updatedJobEntity.getWorkflowNote(), is(workflowNote));
    }

    @Test
    public void setWorkflowNote_jobEntityFound_returnsUpdatedJobEntityWithNullAsWorkflowNote() throws JobStoreException {
         final JobEntity jobEntity = new JobEntity();

         jobEntity.setWorkflowNote(new WorkflowNoteBuilder().build());
         when(entityManager.find(JobEntity.class, DEFAULT_JOB_ID, LockModeType.PESSIMISTIC_WRITE)).thenReturn(jobEntity);


         final PgJobStoreRepository pgJobStoreRepository = newPgJobStoreReposity();
         JobEntity updatedJobEntity = pgJobStoreRepository.setJobEntityWorkFlowNote(null, DEFAULT_JOB_ID);
         assertThat(updatedJobEntity, is(notNullValue()));
         assertThat(updatedJobEntity.getWorkflowNote(), is(nullValue()));
     }

    @Test
    public void setWorkflowNote_itemEntityNotFound_throws() {
        final ItemEntity.Key key = new ItemEntity.Key(DEFAULT_JOB_ID, DEFAULT_CHUNK_ID, DEFAULT_ITEM_ID);
        when(entityManager.find(ItemEntity.class, key, LockModeType.PESSIMISTIC_WRITE)).thenReturn(null);

        final PgJobStoreRepository pgJobStoreRepository = newPgJobStoreReposity();
        final WorkflowNote workflowNote = new WorkflowNoteBuilder().build();

        try {
            pgJobStoreRepository.setItemEntityWorkFlowNote(workflowNote, key.getJobId(), key.getChunkId(), key.getId());
            fail("No exception thrown");
        } catch (JobStoreException e) {}
    }

    @Test
    public void setWorkflowNote_itemEntityFound_returnsUpdatedItemEntityWithGivenWorkflowNote() throws JobStoreException {
        final ItemEntity.Key key = new ItemEntity.Key(DEFAULT_JOB_ID, DEFAULT_CHUNK_ID, DEFAULT_ITEM_ID);
        when(entityManager.find(ItemEntity.class, key, LockModeType.PESSIMISTIC_WRITE)).thenReturn(new ItemEntity());

        final PgJobStoreRepository pgJobStoreRepository = newPgJobStoreReposity();
        final WorkflowNote workflowNote = new WorkflowNoteBuilder().build();

        ItemEntity updatedItemEntity = pgJobStoreRepository.setItemEntityWorkFlowNote(workflowNote, key.getJobId(), key.getChunkId(), key.getId());
        assertThat(updatedItemEntity, is(notNullValue()));
        assertThat(updatedItemEntity.getWorkflowNote(), is(workflowNote));
    }

    @Test
    public void setWorkflowNote_itemEntityFound_returnsUpdatedItemEntityWithNullAsWorkflowNote() throws JobStoreException {
        final ItemEntity itemEntity = new ItemEntity();
        final ItemEntity.Key key = new ItemEntity.Key(DEFAULT_JOB_ID, DEFAULT_CHUNK_ID, DEFAULT_ITEM_ID);
        itemEntity.setWorkflowNote(new WorkflowNoteBuilder().build());
        when(entityManager.find(ItemEntity.class, key, LockModeType.PESSIMISTIC_WRITE)).thenReturn(itemEntity);


        final PgJobStoreRepository pgJobStoreRepository = newPgJobStoreReposity();
        ItemEntity updatedItemEntity = pgJobStoreRepository.setItemEntityWorkFlowNote(null, key.getJobId(), key.getChunkId(), key.getId());
        assertThat(updatedItemEntity, is(notNullValue()));
        assertThat(updatedItemEntity.getWorkflowNote(), is(nullValue()));
    }

    @Test
    public void getCachedFlow_jobEntityNotFound_throws() {
        final PgJobStoreRepository pgJobStoreRepository = newPgJobStoreReposity();
        when(entityManager.find(eq(JobEntity.class), anyInt())).thenReturn(null);
        assertThat(() -> pgJobStoreRepository.getCachedFlow(DEFAULT_JOB_ID), isThrowing(JobStoreException.class));
    }

    @Test
    public void getCachedFlow_jobEntityFound_returns() throws JobStoreException {
        Flow expectedFlow = new FlowBuilder().build();
        final JobEntity jobEntity = getJobEntity(DEFAULT_JOB_ID);
        when(jobEntity.getCachedFlow().getFlow()).thenReturn(expectedFlow);

        final PgJobStoreRepository pgJobStoreRepository = newPgJobStoreReposity();
        Flow cachedFlow = pgJobStoreRepository.getCachedFlow(DEFAULT_JOB_ID);
        assertThat("flow", cachedFlow, is(expectedFlow));
    }
}
