package dk.dbc.dataio.jobstore.ejb;

import dk.dbc.dataio.commons.types.Flow;
import dk.dbc.dataio.commons.types.FlowBinder;
import dk.dbc.dataio.commons.types.JobSpecification;
import dk.dbc.dataio.commons.types.Sink;
import dk.dbc.dataio.commons.utils.newjobstore.JobStoreServiceConnectorException;
import dk.dbc.dataio.commons.utils.newjobstore.JobStoreServiceConnectorUnexpectedStatusCodeException;
import dk.dbc.dataio.commons.utils.newjobstore.ejb.JobStoreServiceConnectorBean;
import dk.dbc.dataio.commons.utils.service.ServiceUtil;
import dk.dbc.dataio.jobstore.JobStore;
import dk.dbc.dataio.jobstore.fsjobstore.FileSystemJobStore;
import dk.dbc.dataio.jobstore.types.Job;
import dk.dbc.dataio.jobstore.types.JobError;
import dk.dbc.dataio.jobstore.types.JobInfoSnapshot;
import dk.dbc.dataio.jobstore.types.JobInputStream;
import dk.dbc.dataio.jobstore.types.JobStoreException;
import dk.dbc.dataio.sequenceanalyser.keygenerator.SequenceAnalyserKeyGenerator;
import dk.dbc.dataio.sequenceanalyser.keygenerator.SequenceAnalyserNoOrderKeyGenerator;
import dk.dbc.dataio.sequenceanalyser.keygenerator.SequenceAnalyserSinkKeyGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.ejb.LocalBean;
import javax.ejb.Lock;
import javax.ejb.LockType;
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

    @EJB
    JobStoreServiceConnectorBean newJobStoreServiceConnectorBean;

    // class scoped for easy test injection
    Path jobStorePath;
    FileSystemJobStore jobStore;

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

    @Lock(LockType.READ)
    public JobStore getJobStore() {
        return jobStore;
    }

    @Lock(LockType.READ)
    public Job createAndScheduleJob(long jobId, JobSpecification jobSpec, FlowBinder flowBinder, Flow flow, Sink sink, InputStream jobInputStream) throws JobStoreException {
        //scheduleJob(job, sink);
        return jobStore.createJob(jobId, jobSpec, flowBinder, flow, sink, jobInputStream,
                getSequenceAnalyserKeyGenerator(flowBinder));
    }

    private SequenceAnalyserKeyGenerator getSequenceAnalyserKeyGenerator(FlowBinder flowBinder) {
        if(flowBinder.getContent().getSequenceAnalysis()) {
            return new SequenceAnalyserSinkKeyGenerator();
        } else {
            return new SequenceAnalyserNoOrderKeyGenerator();
        }
    }

    @Lock(LockType.READ)
    public int addJobToNewJobStore(JobSpecification jobSpecification) throws JobStoreException {
        final JobInputStream jobInputStream = new JobInputStream(jobSpecification, true, 0);
        try {
            final JobInfoSnapshot jobInfoSnapshot = newJobStoreServiceConnectorBean.getConnector().addJob(jobInputStream);
            return jobInfoSnapshot.getJobId();
        } catch (JobStoreServiceConnectorException e) {
            if (e instanceof JobStoreServiceConnectorUnexpectedStatusCodeException) {
                final JobError jobError = ((JobStoreServiceConnectorUnexpectedStatusCodeException) e).getJobError();
                if (jobError != null) {
                    LOGGER.error("New job-store returned error: {}", jobError.getDescription());
                }
            }
            throw new JobStoreException("Error in communication with new job-store", e);
        }
    }

}
