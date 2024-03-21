package dk.dbc.dataio.jobstore.service.ejb;

import dk.dbc.dataio.jobstore.service.entity.JobQueueEntity;
import dk.dbc.dataio.jobstore.service.entity.RerunEntity;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import jakarta.ejb.DependsOn;
import jakarta.ejb.EJB;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import jakarta.ejb.TimerService;

import java.util.Set;

@Singleton
@Startup
@DependsOn({"DatabaseMigrator", "DependencyTrackingService"})
public class BootstrapBean {
    @EJB
    JobQueueRepository jobQueueRepository;
    @EJB
    JobSchedulerBean jobSchedulerBean;
    @EJB
    RerunsRepository rerunsRepository;
    @Resource
    TimerService timerService;

    @PostConstruct
    public void initialize() {
        resetJobsInterruptedDuringPartitioning();
        resetInterruptedRerunTasks();
        jobSchedulerBean.registerMetrics();
        jobSchedulerBean.loadSinkStatusOnBootstrap(Set.of());
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
