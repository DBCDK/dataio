package dk.dbc.dataio.jobstore.service.ejb;

import dk.dbc.dataio.jobstore.service.AbstractJobStoreIT;
import dk.dbc.dataio.jobstore.service.entity.JobEntity;
import dk.dbc.dataio.jobstore.service.entity.RerunEntity;
import org.junit.Test;

import java.util.List;
import java.util.Optional;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

public class RerunsRepositoryIT extends AbstractJobStoreIT {
    /**
     * Given: a rerun queue with multiple entries
     * When : in-progress entries are requested
     * Then : only entries with state IN_PROGRESS are returned
     */
    @Test
    public void getInProgress() {
        // Given...
        final JobEntity job1 = newPersistedJobEntity();
        final JobEntity job2 = newPersistedJobEntity();
        final JobEntity job3 = newPersistedJobEntity();
        final RerunEntity rerun1 = newPersistedRerunEntity(job1);
        final RerunEntity rerun2 = newPersistedRerunEntity(job2);
        final RerunEntity rerun3 = newPersistedRerunEntity(job3);

        persistenceContext.run(() -> {
            rerun1.withState(RerunEntity.State.IN_PROGRESS);
            rerun2.withState(RerunEntity.State.WAITING);
            rerun3.withState(RerunEntity.State.WAITING);
        });

        // When...
        final RerunsRepository rerunsRepository = newRerunsRepository();
        final List<RerunEntity> inProgress = rerunsRepository.getInProgress();

        // Then...
        assertThat("Number of queue entries", inProgress.size(), is(1));
        assertThat("queue entry state", inProgress.get(0).getState(), is(RerunEntity.State.IN_PROGRESS));
    }

    /**
     * Given: a empty rerun queue
     * When : a rerun entry is added via addWaiting
     * Then : the entry is added with state WAITING
     */
    @Test
    public void addWaiting() {
        // Given...
        final JobEntity job = newPersistedJobEntity();

        final RerunEntity rerunEntity = new RerunEntity()
                .withJob(job);

        // When...
        final RerunsRepository rerunsRepository = newRerunsRepository();
        persistenceContext.run(() -> rerunsRepository.addWaiting(rerunEntity));

        // Then...
        assertThat("Number of queue entries", rerunsRepository.getWaiting().size(), is(1));
    }


    /**
     * Given: a non-empty rerun queue
     * When : remove is called
     * Then : the entry is deleted
     */
    @Test
    public void remove() {
        // Given...
        final JobEntity job = newPersistedJobEntity();
        final RerunEntity rerunEntity = newPersistedRerunEntity(job);

        // When...
        final RerunsRepository rerunsRepository = newRerunsRepository();
        persistenceContext.run(() -> rerunsRepository.remove(rerunEntity));

        // Then...
        assertThat(entityManager.find(RerunEntity.class, rerunEntity.getId()), is(nullValue()));
    }

    /**
     * Given: an empty rerun queue
     * When : seizeHeadOfQueueIfWaiting is called
     * Then : an empty response is returned
     */
    @Test
    public void seizeHeadOfQueueIfWaiting_noEntriesExist() {
        // When...
        final RerunsRepository rerunsRepository = newRerunsRepository();
        final Optional<RerunEntity> head = persistenceContext.run(rerunsRepository::seizeHeadOfQueueIfWaiting);

        // Then...
        assertThat("No waiting head of queue found for sink", head, is(Optional.empty()));
    }

    /**
     * Given: a non-empty rerun queue
     * When : seizeHeadOfQueueIfWaiting is called when head of queue is NOT waiting
     * Then : an empty response is returned
     */
    @Test
    public void seizeHeadOfQueueIfWaiting_headOfQueueIsNotWaiting() {
        // Given...
        final JobEntity job = newPersistedJobEntity();

        final RerunEntity rerun1 = new RerunEntity()
                .withJob(job)
                .withState(RerunEntity.State.IN_PROGRESS);
        final RerunEntity rerun2 = new RerunEntity()
                .withJob(job)
                .withState(RerunEntity.State.WAITING);

        persist(rerun1);
        persist(rerun2);

        // When...
        final RerunsRepository rerunsRepository = newRerunsRepository();
        final Optional<RerunEntity> head = persistenceContext.run(rerunsRepository::seizeHeadOfQueueIfWaiting);

        // Then...
        assertThat("No waiting head of queue found", head, is(Optional.empty()));
    }

    /**
     * Given: a non-empty rerun queue
     * When : seizeHeadOfQueueIfWaiting is called when head of queue IS waiting
     * Then : the queue entry is returned with state IN_PROGRESS
     */
    @Test
    public void seizeHeadOfQueueIfWaiting_headOfQueueIsWaiting() {
        // Given...
        final JobEntity job = newPersistedJobEntity();

        final RerunEntity rerun1 = new RerunEntity()
                .withJob(job)
                .withState(RerunEntity.State.WAITING);
        final RerunEntity rerun2 = new RerunEntity()
                .withJob(job)
                .withState(RerunEntity.State.WAITING);

        persist(rerun1);
        persist(rerun2);

        // When...
        final RerunsRepository rerunsRepository = newRerunsRepository();
        final Optional<RerunEntity> head = persistenceContext.run(rerunsRepository::seizeHeadOfQueueIfWaiting);

        // Then...
        final RerunEntity seized = head.orElse(null);
        assertThat("head of queue returned", seized, is(notNullValue()));
        assertThat("head of queue ID", seized.getId(), is(rerun1.getId()));
        assertThat("head of queue for is now in-progress", seized.getState(), is(RerunEntity.State.IN_PROGRESS));
    }

    @Test
    public void reset() {
        // Given...
        final JobEntity job = newPersistedJobEntity();

        final RerunEntity rerun = new RerunEntity()
                .withJob(job)
                .withState(RerunEntity.State.IN_PROGRESS);
        persist(rerun);

        // When...
        final RerunsRepository rerunsRepository = newRerunsRepository();
        persistenceContext.run(() -> rerunsRepository.reset(rerun));

        // Then...
        assertThat(entityManager.find(RerunEntity.class, rerun.getId()).getState(), is(RerunEntity.State.WAITING));
    }
}
