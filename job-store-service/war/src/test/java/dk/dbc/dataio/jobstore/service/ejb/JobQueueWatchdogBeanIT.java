package dk.dbc.dataio.jobstore.service.ejb;

import dk.dbc.dataio.jobstore.service.AbstractJobStoreIT;
import dk.dbc.dataio.jobstore.service.entity.JobEntity;
import dk.dbc.dataio.jobstore.service.entity.JobQueueEntity;

import java.time.Instant;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class JobQueueWatchdogBeanIT extends AbstractJobStoreIT {

    private JobQueueWatchdogBean newWatchdog() {
        final JobQueueWatchdogBean watchdog = new JobQueueWatchdogBean();
        watchdog.jobQueueRepository = newJobQueueRepository();
        return watchdog;
    }

    /**
     * Given: a job queue with only waiting entries
     * When : the watchdog runs
     * Then : nothing is reported
     */
    @org.junit.Test
    public void noInProgressEntries() {
        // Given...
        final JobEntity job = newPersistedJobEntity();
        final JobQueueEntity entry = newPersistedJobQueueEntity(job);
        persistenceContext.run(() -> entry.withState(JobQueueEntity.State.WAITING));

        // When...
        final JobQueueWatchdogBean watchdog = newWatchdog();
        final int stuck = watchdog.checkForStuckEntries(Instant.now());

        // Then...
        assertThat("stuck entries", stuck, is(0));
    }

    /**
     * Given: a job queue with an in-progress entry
     * When : the watchdog runs for the first time
     * Then : the entry is not reported, since it has only just been seen
     */
    @org.junit.Test
    public void freshInProgressEntryIsNotReported() {
        // Given...
        final JobEntity job = newPersistedJobEntity();
        final JobQueueEntity entry = newPersistedJobQueueEntity(job);
        persistenceContext.run(() -> entry.withState(JobQueueEntity.State.IN_PROGRESS));

        // When...
        final JobQueueWatchdogBean watchdog = newWatchdog();
        final int stuck = watchdog.checkForStuckEntries(Instant.now());

        // Then...
        assertThat("stuck entries", stuck, is(0));
    }

    /**
     * Given: an in-progress entry which the watchdog has already seen
     * When : the watchdog runs again once the threshold has elapsed
     * Then : the entry is reported as stuck
     */
    @org.junit.Test
    public void inProgressEntryPastThresholdIsReported() {
        // Given...
        final JobEntity job = newPersistedJobEntity();
        final JobQueueEntity entry = newPersistedJobQueueEntity(job);
        persistenceContext.run(() -> entry.withState(JobQueueEntity.State.IN_PROGRESS));

        final Instant firstTick = Instant.now();
        final JobQueueWatchdogBean watchdog = newWatchdog();
        assertThat("stuck entries on first tick", watchdog.checkForStuckEntries(firstTick), is(0));

        // When...
        final Instant laterTick = firstTick.plus(JobQueueWatchdogBean.STUCK_THRESHOLD).plusSeconds(1);
        final int stuck = watchdog.checkForStuckEntries(laterTick);

        // Then...
        assertThat("stuck entries", stuck, is(1));
        assertThat("stuck count gauge", watchdog.getStuckCount(), is(1));
    }

    /**
     * Given: an in-progress entry which the watchdog has already seen
     * When : the entry is removed and a new entry takes its place before the threshold elapses
     * Then : the new entry is not reported, so a recycled id cannot inherit the old sighting
     */
    @org.junit.Test
    public void sightingIsForgottenWhenEntryLeavesInProgress() {
        // Given...
        final JobEntity job1 = newPersistedJobEntity();
        final JobQueueEntity entry1 = newPersistedJobQueueEntity(job1);
        persistenceContext.run(() -> entry1.withState(JobQueueEntity.State.IN_PROGRESS));

        final Instant firstTick = Instant.now();
        final JobQueueWatchdogBean watchdog = newWatchdog();
        assertThat("stuck entries on first tick", watchdog.checkForStuckEntries(firstTick), is(0));

        // When...
        persistenceContext.run(() -> entry1.withState(JobQueueEntity.State.WAITING));
        final Instant laterTick = firstTick.plus(JobQueueWatchdogBean.STUCK_THRESHOLD).plusSeconds(1);

        // Then...
        assertThat("stuck entries once the entry is no longer in progress",
                watchdog.checkForStuckEntries(laterTick), is(0));

        // And when the same entry goes in progress again, its clock starts over
        persistenceContext.run(() -> entry1.withState(JobQueueEntity.State.IN_PROGRESS));
        assertThat("stuck entries after re-entering in progress",
                watchdog.checkForStuckEntries(laterTick), is(0));
    }
}
