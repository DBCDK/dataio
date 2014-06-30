package dk.dbc.dataio.jobstore.ejb;

import dk.dbc.dataio.commons.types.Chunk;
import dk.dbc.dataio.commons.types.ChunkResult;
import dk.dbc.dataio.commons.types.Flow;
import dk.dbc.dataio.commons.types.FlowBinder;
import dk.dbc.dataio.commons.types.JobInfo;
import dk.dbc.dataio.commons.types.JobSpecification;
import dk.dbc.dataio.commons.types.JobState;
import dk.dbc.dataio.commons.types.Sink;
import dk.dbc.dataio.commons.types.SinkChunkResult;
import dk.dbc.dataio.commons.utils.service.ServiceUtil;
import dk.dbc.dataio.jobstore.JobStore;
import dk.dbc.dataio.jobstore.fsjobstore.FileSystemJobStore;
import dk.dbc.dataio.jobstore.types.Job;
import dk.dbc.dataio.jobstore.types.JobStoreException;
import dk.dbc.dataio.sequenceanalyser.keygenerator.SequenceAnalyserKeyGenerator;
import dk.dbc.dataio.sequenceanalyser.keygenerator.SequenceAnalyserSinkKeyGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.ejb.LocalBean;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.naming.NamingException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@LocalBean
@Singleton
@Startup
public class JobStoreBean {

    private static final Logger LOGGER = LoggerFactory.getLogger(JobStoreBean.class);
    public static final String PATH_RESOURCE_JOB_STORE_HOME = "path/dataio/jobstore/home";

    // class scoped for easy test injection
    Path jobStorePath;
    JobStore jobStore;
    SequenceAnalyserKeyGenerator sequenceAnalyserKeyGenerator;

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
        sequenceAnalyserKeyGenerator = new SequenceAnalyserSinkKeyGenerator();
    }

    public Job createJob(JobSpecification jobSpec, FlowBinder flowBinder, Flow flow, Sink sink, InputStream jobInputStream) throws JobStoreException {
        return jobStore.createJob(jobSpec, flowBinder, flow, sink, jobInputStream,
                sequenceAnalyserKeyGenerator);
    }

    public void updateJobInfo(Job job, JobInfo jobInfo) throws JobStoreException {
        jobStore.updateJobInfo(job, jobInfo);
    }

    public List<JobInfo> getAllJobInfos() throws JobStoreException {
        return jobStore.getAllJobInfos();
    }

    public long getNumberOfChunksInJob(long jobId) throws JobStoreException {
        return jobStore.getNumberOfChunksInJob(jobId);
    }

    public Chunk getChunk(long jobId, long chunkId) throws JobStoreException {
        return jobStore.getChunk(jobId, chunkId);
    }

    public void addProcessorResult(ChunkResult processorResult) throws JobStoreException {
        jobStore.addProcessorResult(processorResult);
    }

    public ChunkResult getProcessorResult(long jobId, long chunkId) throws JobStoreException {
        return jobStore.getProcessorResult(jobId, chunkId);
    }

    public void addSinkResult(SinkChunkResult sinkResult) throws JobStoreException {
        jobStore.addSinkResult(sinkResult);
    }

    public SinkChunkResult getSinkResult(long jobId, long chunkId) throws JobStoreException {
        return jobStore.getSinkResult(jobId, chunkId);
    }

    public Sink getSink(long jobId) throws JobStoreException {
        return jobStore.getSink(jobId);
    }

    public JobState getJobState(long jobId) throws JobStoreException {
        return jobStore.getJobState(jobId);
    }

}
