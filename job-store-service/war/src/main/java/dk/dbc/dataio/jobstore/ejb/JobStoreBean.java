package dk.dbc.dataio.jobstore.ejb;

import dk.dbc.dataio.commons.types.Flow;
import dk.dbc.dataio.commons.types.FlowBinder;
import dk.dbc.dataio.commons.types.JobSpecification;
import dk.dbc.dataio.commons.types.Sink;
import dk.dbc.dataio.commons.utils.service.ServiceUtil;
import dk.dbc.dataio.jobstore.JobStore;
import dk.dbc.dataio.jobstore.fsjobstore.FileSystemJobStore;
import dk.dbc.dataio.jobstore.types.Job;
import dk.dbc.dataio.jobstore.types.JobStoreException;
import dk.dbc.dataio.sequenceanalyser.keygenerator.SequenceAnalyserKeyGenerator;
import dk.dbc.dataio.sequenceanalyser.keygenerator.SequenceAnalyserNoOrderKeyGenerator;
import dk.dbc.dataio.sequenceanalyser.keygenerator.SequenceAnalyserSinkKeyGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.ejb.LocalBean;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.naming.NamingException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;

@LocalBean
@Singleton
@Startup
public class JobStoreBean {
    private static final Logger LOGGER = LoggerFactory.getLogger(JobStoreBean.class);

    public static final String PATH_RESOURCE_JOB_STORE_HOME = "path/dataio/jobstore/home";

    @EJB
    JobSchedulerBean jobScheduler;

    // class scoped for easy test injection
    Path jobStorePath;
    JobStore jobStore;

    @PostConstruct
    public void setupJobStore() {
        try {
            jobStorePath = Paths.get(ServiceUtil.getStringValueFromResource(PATH_RESOURCE_JOB_STORE_HOME));
        } catch (NamingException e) {
            final String errMsg = "An Error occurred while retrieving JNDI path: " + PATH_RESOURCE_JOB_STORE_HOME;
            LOGGER.error(errMsg, e);
            throw new RuntimeException(errMsg, e);
        }
        try {
            jobStore = FileSystemJobStore.newFileSystemJobStore(jobStorePath);
        } catch (JobStoreException ex) {
            final String errMsg = "An Error occurred while setting up the job-store.";
            LOGGER.error(errMsg, ex);
            throw new RuntimeException(errMsg, ex);
        }
    }

    public JobStore getJobStore() {
        return jobStore;
    }

    public Job createAndScheduleJob(JobSpecification jobSpec, FlowBinder flowBinder, Flow flow, Sink sink, InputStream jobInputStream) throws JobStoreException {
        final Job job = jobStore.createJob(jobSpec, flowBinder, flow, sink, jobInputStream,
                getSequenceAnalyserKeyGenerator(flowBinder));
        scheduleJob(job, sink);
        return job;
    }

    private SequenceAnalyserKeyGenerator getSequenceAnalyserKeyGenerator(FlowBinder flowBinder) {
        if(flowBinder.getContent().getSequenceAnalysis()) {
            return new SequenceAnalyserSinkKeyGenerator();
        } else {
            return new SequenceAnalyserNoOrderKeyGenerator();
        }
    }

    private void scheduleJob(Job job, Sink sink) throws JobStoreException {
       final long numberOfChunks = jobStore.getNumberOfChunksInJob(job.getId());
        for (long i = 1; i <= numberOfChunks; i++) {
            jobScheduler.scheduleChunk(jobStore.getChunk(job.getId(), i), sink);
        }
    }
}
