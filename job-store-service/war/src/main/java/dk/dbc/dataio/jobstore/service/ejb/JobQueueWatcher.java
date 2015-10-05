package dk.dbc.dataio.jobstore.service.ejb;

import dk.dbc.dataio.commons.types.interceptor.Stopwatch;
import dk.dbc.dataio.filestore.service.connector.ejb.FileStoreServiceConnectorBean;
import dk.dbc.dataio.jobstore.service.entity.JobEntity;
import dk.dbc.dataio.jobstore.service.entity.JobQueueEntity;
import dk.dbc.dataio.jobstore.service.param.PartitioningParam;
import dk.dbc.dataio.jobstore.types.JobStoreException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.ejb.DependsOn;
import javax.ejb.EJB;
import javax.ejb.Schedule;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import java.util.List;

@Singleton
@Startup
@DependsOn("BootstrapBean")
public class JobQueueWatcher {

    private static final Logger LOGGER = LoggerFactory.getLogger(JobQueueWatcher.class);

    @EJB
    FileStoreServiceConnectorBean fileStoreServiceConnectorBean;

    @EJB
    JobQueueRepository jobQueueRepository;

    @EJB
    PgJobStoreRepository jobStoreRepository;

    @EJB
    PgJobStore jobStore;

    @PostConstruct
    public void init() {
        LOGGER.info("Jeg er simpelthen i live!");
    }

    @Stopwatch
    @Schedule(second = "*/5", minute = "*", hour = "*", persistent = false)
    public void doWatch() {
        LOGGER.info("Watcher...");
        final List<Long> uniqueSinkIds = jobQueueRepository.getUniqueSinkIds();

        if(uniqueSinkIds != null && !uniqueSinkIds.isEmpty()) {
            for (Long uniqueSinkId : uniqueSinkIds) {

                final JobQueueEntity firstWaitingJobToStart = jobQueueRepository.getFirstWaitingJobQueueEntityBySink(uniqueSinkId);
                if (!jobQueueRepository.isSinkOccupied(uniqueSinkId) && firstWaitingJobToStart != null) {

                    this.startJob(firstWaitingJobToStart.getJob());
                }
            }
        }
    }


    private void startJob(JobEntity jobToStart) {
        LOGGER.info("Starter job: " + jobToStart.getId());
        final JobQueueEntity jobQueueEntity = this.jobQueueRepository.getJobQueueEntityByJob(jobToStart);
        try{
            jobStore.handlePartitioningAsynchronously(
                    new PartitioningParam(
                            jobToStart,
                            fileStoreServiceConnectorBean.getConnector(),
                            jobQueueEntity.isSequenceAnalysis(),
                            jobQueueEntity.getRecordSplitterType()));
        } catch (JobStoreException jse) {
            LOGGER.info(
                    "this job received an error and is rolled back hence stays in the queue!. Queue ID: {}, Job ID: {}, Sink ID: {}",
                    jobQueueEntity.getId(),
                    jobQueueEntity.getJob().getId(),
                    jobQueueEntity.getSinkId());
        }
    }
}