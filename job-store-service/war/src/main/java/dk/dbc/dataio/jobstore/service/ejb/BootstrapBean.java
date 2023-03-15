package dk.dbc.dataio.jobstore.service.ejb;

import dk.dbc.dataio.jobstore.service.entity.JobQueueEntity;
import dk.dbc.dataio.jobstore.service.entity.RerunEntity;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.ejb.DependsOn;
import javax.ejb.EJB;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.ejb.TimerService;

@Singleton
@Startup
@DependsOn("DatabaseMigrator")
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
        jobSchedulerBean.loadSinkStatusOnBootstrap();
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
