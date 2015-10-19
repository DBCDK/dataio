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

import dk.dbc.dataio.jobstore.service.entity.JobEntity;
import dk.dbc.dataio.jobstore.service.entity.JobQueueEntity;
import org.junit.Test;

import javax.persistence.EntityTransaction;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class JobQueueRepositoryIT extends AbstractJobStoreIT {

    /**
     * Given: a job queue with multiple entries
     * When : in-progress entries are requested
     * Then : only entries with state IN_PROGRESS are returned
     */
    @Test
    public void getInProgress() {
        // Given...
        final JobEntity job1 = newPersistedJobEntity();
        final JobEntity job2 = newPersistedJobEntity();
        final JobEntity job3 = newPersistedJobEntity();
        final JobQueueEntity job1QueueEntry = newPersistedJobQueueEntity(job1);
        final JobQueueEntity job2QueueEntry = newPersistedJobQueueEntity(job2);
        final JobQueueEntity job3QueueEntry = newPersistedJobQueueEntity(job3);

        final EntityTransaction transaction = entityManager.getTransaction();
        transaction.begin();
        job1QueueEntry.setState(JobQueueEntity.State.IN_PROGRESS);
        job2QueueEntry.setState(JobQueueEntity.State.WAITING);
        job3QueueEntry.setState(JobQueueEntity.State.IN_PROGRESS);
        transaction.commit();

        // When...
        final JobQueueRepository jobQueueRepository = newJobQueueRepository();
        final List<JobQueueEntity> inProgress = jobQueueRepository.getInProgress();

        // Then...
        assertThat("Number of queue entries", inProgress.size(), is(2));
        assertThat("Queue entries", new HashSet<>(Arrays.asList(inProgress.get(0).getId(), inProgress.get(1).getId())),
                is(new HashSet<>(Arrays.asList(job1QueueEntry.getId(), job3QueueEntry.getId()))));
    }

    private JobQueueRepository newJobQueueRepository() {
        final JobQueueRepository jobQueueRepository = new JobQueueRepository();
        jobQueueRepository.entityManager = entityManager;
        return jobQueueRepository;
    }
}
