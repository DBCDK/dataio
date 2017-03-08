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
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.eq;
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
    public void getCachedFlow_jobEntityNotFound_throws() throws JobStoreException {
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