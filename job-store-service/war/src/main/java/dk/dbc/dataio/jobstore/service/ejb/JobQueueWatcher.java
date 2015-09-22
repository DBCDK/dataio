package dk.dbc.dataio.jobstore.service.ejb;

import dk.dbc.dataio.commons.types.interceptor.Stopwatch;
import dk.dbc.dataio.filestore.service.connector.ejb.FileStoreServiceConnectorBean;
import dk.dbc.dataio.jobstore.service.entity.JobEntity;
import dk.dbc.dataio.jobstore.service.entity.JobQueueEntity;
import dk.dbc.dataio.jobstore.service.param.PartitioningParam;
import dk.dbc.dataio.jobstore.types.JobStoreException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.DependsOn;
import javax.ejb.EJB;
import javax.ejb.Schedule;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import static dk.dbc.dataio.jobstore.service.entity.JobQueueEntity.AVAILABLE;
import static dk.dbc.dataio.jobstore.service.entity.JobQueueEntity.OCCUPIED;

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

    @Schedule(second = "*/5", minute = "*", hour = "*")
    @Stopwatch
    public void doWatch() throws JobStoreException{

        LOGGER.info("Start watching the queue table...");

        final Hashtable<Long, List<JobQueueEntity>> jobsGroupedBySink = buildJobQueueEntitiesGroupedBySink();
        boolean didWatcherStartAtLeastOneJob = false;
        for (Map.Entry<Long, List<JobQueueEntity>> sinkEntry : jobsGroupedBySink.entrySet()) {

            final long uniqueSinkId = sinkEntry.getKey();
            final List<JobQueueEntity> jobsForUniqueSink = sinkEntry.getValue();
            if(!sinkOccupied(uniqueSinkId, jobsForUniqueSink) && jobsForUniqueSink != null && !jobsForUniqueSink.isEmpty()) {

                final JobEntity firstWaitingJob = sinkEntry.getValue().get(0).getJob();
                LOGGER.info("----- starting job: " + firstWaitingJob.getId());
                didWatcherStartAtLeastOneJob = true;
                this.startJob(firstWaitingJob);
            }
        }

        if(!didWatcherStartAtLeastOneJob) {
            LOGGER.info("----- Watcher did NOT start any jobs.");
        }

        LOGGER.info("Done watching the queue table.");
    }

    private boolean sinkOccupied(long uniqueSinkId, List<JobQueueEntity> jobsForUniqueSink) {

        for (JobQueueEntity jobQueueEntity : jobsForUniqueSink) {

            if(jobQueueEntity.getState() == JobQueueEntity.State.IN_PROGRESS) {
                return OCCUPIED;
            }
        }
        return AVAILABLE;
    }

    private void startJob(JobEntity jobToStart) throws JobStoreException{

        final JobQueueEntity jobQueueEntity = this.jobQueueRepository.getJobQueueEntityByJob(jobToStart);
        jobStore.handlePartitioningAsynchronously(
                new PartitioningParam(
                        jobToStart,
                        fileStoreServiceConnectorBean.getConnector(),
                        jobQueueEntity.isSequenceAnalysis(),
                        jobQueueEntity.getRecordSplitterType()));
    }

    private Hashtable<Long, List<JobQueueEntity>> buildJobQueueEntitiesGroupedBySink() {

        Hashtable<Long, List<JobQueueEntity>> mapOfJobQueueEntitiesGroupedBySink = new Hashtable();

        final List<Long> uniqueSinkIds = jobQueueRepository.getUniqueSinkIds();

        for (Long uniqueSinkId : uniqueSinkIds) {

            mapOfJobQueueEntitiesGroupedBySink.put(
                    uniqueSinkId,
                    jobQueueRepository.getJobQueueEntitiesBySink(uniqueSinkId));
        }

        return mapOfJobQueueEntitiesGroupedBySink;
    }
}