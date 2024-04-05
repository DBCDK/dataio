package dk.dbc.dataio.jobstore.service.ejb;

import dk.dbc.dataio.commons.types.Sink;
import dk.dbc.dataio.jobstore.service.entity.JobQueueEntity;
import dk.dbc.dataio.jobstore.service.entity.RerunEntity;
import dk.dbc.dataio.jobstore.types.JobStoreException;
import jakarta.annotation.PostConstruct;
import jakarta.ejb.DependsOn;
import jakarta.ejb.EJB;
import jakarta.ejb.Schedule;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Singleton
@Startup
@DependsOn({"DatabaseMigrator", "DependencyTrackingService"})
public class BootstrapBean {
    private static final Logger LOGGER = LoggerFactory.getLogger(BootstrapBean.class);
    @EJB
    JobQueueRepository jobQueueRepository;
    @EJB
    JobSchedulerBean jobSchedulerBean;
    @EJB
    RerunsRepository rerunsRepository;
    @EJB
    PgJobStore jobStore;
    @EJB
    JobRerunnerBean jobRerunnerBean;
    private boolean partitioningInitialized = false;

    @PostConstruct
    public void initialize() {
        resetJobsInterruptedDuringPartitioning();
        resetInterruptedRerunTasks();
        jobSchedulerBean.registerMetrics();
        jobSchedulerBean.loadSinkStatusOnBootstrap(Set.of());
    }

    /**
     * Resumes partially partitioned jobs
     */
    @Schedule(minute = "*", hour = "*")
    public void resumePartitioning() {
        if(partitioningInitialized) return;
        HashSet<Integer> sinkIds = new HashSet<>();
        @SuppressWarnings("RedundantStreamOptionalCall") // sort used to filter out old sink versions
        Set<Sink> sinks = jobQueueRepository.getWaiting().stream()
                .map(jqe -> jqe.getJob().getCachedSink().getSink())
                .sorted(Comparator.comparing(Sink::getVersion).reversed())
                .filter(s -> sinkIds.add(s.getId()))
                .collect(Collectors.toSet());
        LOGGER.info("jumpStart(): found {} sinks to jump-start", sinks.size());
        sinks.forEach(sink -> {
            LOGGER.info("jumpStart(): jump-starting partitioning for sink {}({})", sink.getId(), sink.getContent().getName());
            jobStore.partitionNextJobForSinkIfAvailable(sink);
        });
        partitioningInitialized = true;
        try {
            jobRerunnerBean.rerunNextIfAvailable();
        } catch (JobStoreException e) {
            LOGGER.error("Error jump-starting rerun tasks handling", e);
        }
    }

    /*
     * Locates and resets any job interrupted in its partitioning phase during a previous shutdown,
     * restoring the corresponding job queue entry to its waiting state
     */
    private void resetJobsInterruptedDuringPartitioning() {
        for (JobQueueEntity inProgress : jobQueueRepository.getInProgress()) {
            jobSchedulerBean.ensureLastChunkIsScheduled(inProgress.getJob().getId());
            inProgress.withState(JobQueueEntity.State.WAITING);
        }
    }

    private void resetInterruptedRerunTasks() {
        for (RerunEntity interrupted : rerunsRepository.getInProgress()) {
            interrupted.withState(RerunEntity.State.WAITING);
        }
    }
}
