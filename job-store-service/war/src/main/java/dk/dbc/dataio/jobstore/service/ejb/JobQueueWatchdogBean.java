package dk.dbc.dataio.jobstore.service.ejb;

import dk.dbc.dataio.jobstore.service.dependencytracking.Hazelcast;
import dk.dbc.dataio.jobstore.service.entity.JobQueueEntity;
import jakarta.annotation.PostConstruct;
import jakarta.ejb.DependsOn;
import jakarta.ejb.EJB;
import jakarta.ejb.Schedule;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Reports job queue entries that have been stuck in
 * {@link JobQueueEntity.State#IN_PROGRESS} for longer than {@code stuckThreshold}.
 * <p>
 * A partitioning that never completes leaves its queue entry IN_PROGRESS, and the seize query's
 * prior-entry guard then blocks every later entry for the same sink and submitter. Other
 * submitters keep flowing, so the stall is invisible from the outside. Before this bean the only
 * code reading {@link JobQueueRepository#getInProgress()} was {@link BootstrapBean} at startup,
 * so such a stall was found by inspection rather than by alerting.
 * <p>
 * Detection only. Nothing here resets or removes an entry.
 */
@Singleton
@Startup
@DependsOn("DatabaseMigrator")
public class JobQueueWatchdogBean {
    private static final Logger LOGGER = LoggerFactory.getLogger(JobQueueWatchdogBean.class);

    @EJB
    JobQueueRepository jobQueueRepository;

    @Inject
    MetricRegistry metricRegistry;

    /* How long an entry may stay IN_PROGRESS before it is reported.  An entry is IN_PROGRESS for
       the whole of its partitioning, so any value has to clear the slowest legitimate one.
       Deliberately has no defaultValue: the default lives in the Dockerfile next to the other
       operational settings, and a missing value should fail deployment rather than silently
       pick a number. */
    @Inject
    @ConfigProperty(name = "JOBQUEUE_STUCK_THRESHOLD")
    Duration stuckThreshold;

    /* Queue entry id -> first tick at which this bean saw it IN_PROGRESS.
       JobQueueEntity.timeOfEntry is the queue insertion time, not the time the entry was seized,
       so it cannot be used to age an IN_PROGRESS entry: an entry may legitimately have waited for
       days before being picked up. Tracking first sighting here avoids a schema change, and is
       correct on the single Hazelcast master which is the only node that partitions. */
    private final Map<Integer, Instant> firstSeenInProgress = new HashMap<>();

    private volatile int stuckCount = 0;

    @PostConstruct
    public void registerMetrics() {
        if (metricRegistry != null) {
            metricRegistry.gauge("dataio_jobqueue_stuck_entries", () -> stuckCount);
        }
    }

    @Schedule(minute = "*", hour = "*", persistent = false)
    public void run() {
        try {
            if (Hazelcast.isSlave()) return;
            checkForStuckEntries(Instant.now());
        } catch (Exception e) {
            LOGGER.error("Exception caught while checking for stuck job queue entries", e);
        }
    }

    /**
     * @param now current time, a parameter so the ageing can be exercised without waiting
     * @return number of entries found to be stuck
     */
    int checkForStuckEntries(Instant now) {
        final List<JobQueueEntity> inProgress = jobQueueRepository.getInProgress();

        final Set<Integer> stillInProgress = inProgress.stream()
                .map(JobQueueEntity::getId)
                .collect(Collectors.toSet());
        firstSeenInProgress.keySet().retainAll(stillInProgress);

        int stuck = 0;
        for (JobQueueEntity entry : inProgress) {
            final Instant firstSeen = firstSeenInProgress.computeIfAbsent(entry.getId(), id -> now);
            final Duration age = Duration.between(firstSeen, now);
            if (age.compareTo(stuckThreshold) >= 0) {
                stuck++;
                LOGGER.error("Job queue entry {} for job {} on sink {} has been IN_PROGRESS for {} minutes." +
                                " Partitioning for this sink and submitter is stalled until it is resolved.",
                        entry.getId(), entry.getJob().getId(), entry.getSinkId(), age.toMinutes());
            }
        }
        stuckCount = stuck;
        return stuck;
    }

    int getStuckCount() {
        return stuckCount;
    }
}
