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

import dk.dbc.dataio.jobstore.service.entity.ChunkEntity;
import dk.dbc.dataio.jobstore.service.entity.FlowCacheEntity;
import dk.dbc.dataio.jobstore.service.entity.ItemEntity;
import dk.dbc.dataio.jobstore.service.entity.JobEntity;
import dk.dbc.dataio.jobstore.service.entity.JobQueueEntity;
import dk.dbc.dataio.jobstore.service.entity.SinkCacheEntity;
import org.junit.Test;

import javax.persistence.EntityTransaction;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;

public class BootstrapBeanIT extends AbstractJobStoreIT {
    /**
     * Given: a job queue with multiple entries marked as in-progress
     * When : job-store bootstrap process is executed
     * Then : jobs linked to in-progress queue entries are reset
     * And  : in-progress queue entries are updated to waiting state
     */
    @Test
    public void initialize_resetsJobsInterruptedDuringPartitioning() {
        // Given...
        final JobEntity job1 = newPersistedJobEntity();
        final JobEntity job2 = newPersistedJobEntity();
        final JobEntity job3 = newPersistedJobEntity();
        final FlowCacheEntity cachedFlow = newPersistedFlowCacheEntity();
        final SinkCacheEntity cachedSink = newPersistedSinkCacheEntity();
        final JobQueueEntity job1QueueEntry = newPersistedJobQueueEntity(job1);
        final JobQueueEntity job2QueueEntry = newPersistedJobQueueEntity(job2);
        newPersistedChunkEntity(new ChunkEntity.Key(0, job1.getId()));
        newPersistedChunkEntity(new ChunkEntity.Key(0, job2.getId()));
        newPersistedChunkEntity(new ChunkEntity.Key(0, job3.getId()));
        newPersistedItemEntity(new ItemEntity.Key(job1.getId(), 0, (short)0));
        newPersistedItemEntity(new ItemEntity.Key(job2.getId(), 0, (short)0));
        newPersistedItemEntity(new ItemEntity.Key(job3.getId(), 0, (short)0));

        final EntityTransaction transaction = entityManager.getTransaction();
        transaction.begin();
        job1.setCachedFlow(cachedFlow);
        job2.setCachedFlow(cachedFlow);
        job3.setCachedFlow(cachedFlow);
        job1.setCachedSink(cachedSink);
        job2.setCachedSink(cachedSink);
        job3.setCachedSink(cachedSink);
        job1QueueEntry.setState(JobQueueEntity.State.IN_PROGRESS);
        job2QueueEntry.setState(JobQueueEntity.State.IN_PROGRESS);
        transaction.commit();

        // When...
        final BootstrapBean bootstrapBean = newBootstrapBean();
        transaction.begin();
        bootstrapBean.initialize();
        transaction.commit();

        // Then...
        final List<ChunkEntity> remainingChunks = findAllChunks();
        assertThat("Number of remaining chunks", remainingChunks.size(), is(1));
        assertThat("Job ID of remaining chunk", remainingChunks.get(0).getKey().getJobId(), is(job3.getId()));

        final List<ItemEntity> remainingItems = findAllItems();
        assertThat("Number of remaining items", remainingItems.size(), is(1));
        assertThat("Job ID of remaining item", remainingItems.get(0).getKey().getJobId(), is(job3.getId()));

        // And...
        assertThat("Number of in-progress entries on job queue",
                bootstrapBean.jobQueueRepository.getInProgress().size(), is(0));
    }

    private BootstrapBean newBootstrapBean() {
        final BootstrapBean bootstrapBean = new BootstrapBean();
        bootstrapBean.jobStoreRepository = newPgJobStoreRepository();
        bootstrapBean.jobQueueRepository = newJobQueueRepository();
        bootstrapBean.jobSchedulerBean = mock(JobSchedulerBean.class);
        return bootstrapBean;
    }
}
