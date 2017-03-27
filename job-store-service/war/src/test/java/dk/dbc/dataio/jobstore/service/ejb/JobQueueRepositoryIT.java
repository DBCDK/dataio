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

import dk.dbc.dataio.commons.types.RecordSplitterConstants;
import dk.dbc.dataio.commons.types.Sink;
import dk.dbc.dataio.commons.utils.test.model.SinkBuilder;
import dk.dbc.dataio.jobstore.service.entity.JobEntity;
import dk.dbc.dataio.jobstore.service.entity.JobQueueEntity;
import dk.dbc.dataio.jobstore.service.entity.SinkCacheEntity;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
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

        persistenceContext.run(() -> {
            job1QueueEntry.withState(JobQueueEntity.State.IN_PROGRESS);
            job2QueueEntry.withState(JobQueueEntity.State.WAITING);
            job3QueueEntry.withState(JobQueueEntity.State.IN_PROGRESS);
        });

        // When...
        final JobQueueRepository jobQueueRepository = newJobQueueRepository();
        final List<JobQueueEntity> inProgress = jobQueueRepository.getInProgress();

        // Then...
        assertThat("Number of queue entries", inProgress.size(), is(2));
        assertThat("Queue entries", new HashSet<>(Arrays.asList(inProgress.get(0).getId(), inProgress.get(1).getId())),
                is(new HashSet<>(Arrays.asList(job1QueueEntry.getId(), job3QueueEntry.getId()))));
    }

    /**
     * Given: a job queue with multiple entries
     * When : waiting entries are requested
     * Then : only entries with state WAITING are returned
     */
    @Test
    public void getWaiting() {
        // Given...
        final JobEntity job1 = newPersistedJobEntity();
        final JobEntity job2 = newPersistedJobEntity();
        final JobEntity job3 = newPersistedJobEntity();
        final JobQueueEntity job1QueueEntry = newPersistedJobQueueEntity(job1);
        final JobQueueEntity job2QueueEntry = newPersistedJobQueueEntity(job2);
        final JobQueueEntity job3QueueEntry = newPersistedJobQueueEntity(job3);

        persistenceContext.run(() -> {
            job1QueueEntry.withState(JobQueueEntity.State.WAITING);
            job2QueueEntry.withState(JobQueueEntity.State.IN_PROGRESS);
            job3QueueEntry.withState(JobQueueEntity.State.WAITING);
        });

        // When...
        final JobQueueRepository jobQueueRepository = newJobQueueRepository();
        final List<JobQueueEntity> waiting = jobQueueRepository.getWaiting();

        // Then...
        assertThat("Number of queue entries", waiting.size(), is(2));
        assertThat("Queue entries", new HashSet<>(Arrays.asList(waiting.get(0).getId(), waiting.get(1).getId())),
                is(new HashSet<>(Arrays.asList(job1QueueEntry.getId(), job3QueueEntry.getId()))));
    }

    /**
     * Given: a empty job queue
     * When : a job queue entry is added via addWaiting
     * Then : the entry is added with state WAITING
     */
    @Test
    public void addWaiting() {
        // Given...
        final JobEntity job = newPersistedJobEntity();

        final JobQueueEntity jobQueueEntity = new JobQueueEntity()
                .withJob(job)
                .withSinkId(42)
                .withTypeOfDataPartitioner(RecordSplitterConstants.RecordSplitter.XML);

        // When...
        final JobQueueRepository jobQueueRepository = newJobQueueRepository();
        persistenceContext.run(() -> jobQueueRepository.addWaiting(jobQueueEntity));

        // Then...
        assertThat("Number of queue entries", jobQueueRepository.getWaiting().size(), is(1));
    }

    /**
     * Given: a non-empty job queue
     * When : remove is called
     * Then : the entry is deleted
     */
    @Test
    public void remove() {
        // Given...
        final JobEntity job = newPersistedJobEntity();
        final JobQueueEntity jobQueueEntity = newPersistedJobQueueEntity(job);

        // When...
        final JobQueueRepository jobQueueRepository = newJobQueueRepository();
        persistenceContext.run(() -> jobQueueRepository.remove(jobQueueEntity));

        // Then...
        assertThat("Number of waiting queue entries", jobQueueRepository.getWaiting().size(), is(0));
        assertThat("Number of in-progress queue entries", jobQueueRepository.getInProgress().size(), is(0));
    }

    /**
     * Given: a non-empty job queue
     * When : seizeHeadOfQueueIfWaiting is called for sink with no entries in the queue
     * Then : an empty response is returned
     */
    @Test
    public void seizeHeadOfQueueIfWaiting_noEntriesExistForSink() {
        // Given...
        final JobEntity job = newPersistedJobEntity();
        final JobQueueEntity jobQueueEntity = newPersistedJobQueueEntity(job);

        // When...
        final JobQueueRepository jobQueueRepository = newJobQueueRepository();
        final Sink sink = new SinkBuilder()
                .setId(jobQueueEntity.getSinkId() + 1)
                .build();

        final Optional<JobQueueEntity> head = persistenceContext.run(() ->
                jobQueueRepository.seizeHeadOfQueueIfWaiting(sink));

        // Then...
        assertThat("No waiting head of queue found for sink", head, is(Optional.empty()));
    }

    /**
     * Given: a non-empty job queue
     * When : seizeHeadOfQueueIfWaiting is called for sink where head of the queue is NOT waiting
     * Then : an empty response is returned
     */
    @Test
    public void seizeHeadOfQueueIfWaiting_headOfQueueForSinkIsNotWaiting() {
        // Given...
        final SinkCacheEntity sinkCacheEntity = newPersistedSinkCacheEntity();
        final JobEntity job1 = newPersistedJobEntity();
        final JobEntity job2 = newPersistedJobEntity();

        persistenceContext.run(() -> {
            job1.setCachedSink(sinkCacheEntity);
            job2.setCachedSink(sinkCacheEntity);
        });

        final JobQueueEntity jobQueueEntity1 = new JobQueueEntity()
                .withJob(job1)
                .withSinkId(sinkCacheEntity.getSink().getId())
                .withState(JobQueueEntity.State.IN_PROGRESS)
                .withTypeOfDataPartitioner(RecordSplitterConstants.RecordSplitter.XML);
        final JobQueueEntity jobQueueEntity2 = new JobQueueEntity()
                .withJob(job2)
                .withSinkId(sinkCacheEntity.getSink().getId())
                .withState(JobQueueEntity.State.WAITING)
                .withTypeOfDataPartitioner(RecordSplitterConstants.RecordSplitter.XML);

        persist(jobQueueEntity1);
        persist(jobQueueEntity2);

        // When...
        final JobQueueRepository jobQueueRepository = newJobQueueRepository();
        final Optional<JobQueueEntity> head = persistenceContext.run(() ->
                jobQueueRepository.seizeHeadOfQueueIfWaiting(sinkCacheEntity.getSink()));

        // Then...
        assertThat("No waiting head of queue found for sink", head, is(Optional.empty()));
    }

    /**
     * Given: a non-empty job queue
     * When : seizeHeadOfQueueIfWaiting is called for sink where head of the queue IS waiting
     * Then : the queue entry is returned with state IN_PROGRESS
     */
    @Test
    public void seizeHeadOfQueueIfWaiting_headOfQueueForSinkIsWaiting() {
        // Given...
        final SinkCacheEntity sinkCacheEntity = newPersistedSinkCacheEntity();
        final JobEntity job1 = newPersistedJobEntity();
        final JobEntity job2 = newPersistedJobEntity();

        persistenceContext.run(() -> {
            job1.setCachedSink(sinkCacheEntity);
            job2.setCachedSink(sinkCacheEntity);
        });

        final JobQueueEntity jobQueueEntity1 = new JobQueueEntity()
                .withJob(job1)
                .withSinkId(sinkCacheEntity.getSink().getId())
                .withState(JobQueueEntity.State.WAITING)
                .withTypeOfDataPartitioner(RecordSplitterConstants.RecordSplitter.XML);
        final JobQueueEntity jobQueueEntity2 = new JobQueueEntity()
                .withJob(job2)
                .withSinkId(sinkCacheEntity.getSink().getId())
                .withState(JobQueueEntity.State.WAITING)
                .withTypeOfDataPartitioner(RecordSplitterConstants.RecordSplitter.XML);

        persist(jobQueueEntity1);
        persist(jobQueueEntity2);

        // When...
        final JobQueueRepository jobQueueRepository = newJobQueueRepository();
        final Optional<JobQueueEntity> head = persistenceContext.run(() ->
                jobQueueRepository.seizeHeadOfQueueIfWaiting(sinkCacheEntity.getSink()));

        // Then...
        final JobQueueEntity seized = head.orElse(null);
        assertThat("head of queue for sink returned", seized, is(notNullValue()));
        assertThat("head of queue ID", seized.getId(), is(jobQueueEntity1.getId()));
        assertThat("head of queue for sink is now in-progress", seized.getState(), is(JobQueueEntity.State.IN_PROGRESS));
    }

    /**
     * Given: a non-empty job queue with a queue entry with state IN_PROGRESS
     * When : retry is called for the job queue entry
     * Then : the queue entry has its retries field incremented by one
     * And  : the queue entry has its state changed to WAITING
     */
    @Test
    public void retry() {
        // Given...
        final SinkCacheEntity sinkCacheEntity = newPersistedSinkCacheEntity();
        final JobEntity job = newPersistedJobEntity();

        persistenceContext.run(() -> job.setCachedSink(sinkCacheEntity));

        persist(new JobQueueEntity()
                .withJob(job)
                .withSinkId(sinkCacheEntity.getSink().getId())
                .withState(JobQueueEntity.State.WAITING)
                .withTypeOfDataPartitioner(RecordSplitterConstants.RecordSplitter.XML));

        final JobQueueRepository jobQueueRepository = newJobQueueRepository();
        final JobQueueEntity jobQueueEntity = persistenceContext.run(() ->
                jobQueueRepository.seizeHeadOfQueueIfWaiting(sinkCacheEntity.getSink())).orElse(null);

        assertThat("initial number of retries", jobQueueEntity.getRetries(), is(0));

        // When...
        persistenceContext.run(() -> jobQueueRepository.retry(jobQueueEntity));

        // Then...
        assertThat("number of retries", jobQueueEntity.getRetries(), is(1));
        // And...
        assertThat("state", jobQueueEntity.getState(), is(JobQueueEntity.State.WAITING));
    }
}
