package dk.dbc.dataio.jobstore.ejb;

import dk.dbc.dataio.commons.types.Flow;
import dk.dbc.dataio.commons.types.JobInfo;
import dk.dbc.dataio.commons.types.JobSpecification;
import dk.dbc.dataio.jobstore.JobStore;
import dk.dbc.dataio.jobstore.fsjobstore.FileSystemJobStore;
import dk.dbc.dataio.jobstore.types.Chunk;
import dk.dbc.dataio.jobstore.types.Job;
import dk.dbc.dataio.jobstore.types.JobStoreException;
import dk.dbc.dataio.commons.types.ChunkResult;
import dk.dbc.dataio.commons.types.FlowBinder;
import dk.dbc.dataio.commons.types.Sink;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.ejb.LocalBean;
import javax.ejb.Singleton;
import java.nio.file.FileSystems;
import java.nio.file.Path;

@LocalBean
@Singleton
public class JobStoreBean implements JobStore {
    private static final Logger LOGGER = LoggerFactory.getLogger(JobStoreBean.class);
    private static final String JOB_STORE_NAME = "dataio-job-store";

    // class scoped for easy test injection
    Path jobStorePath = FileSystems.getDefault().getPath(System.getProperty("java.io.tmpdir"), JOB_STORE_NAME);
    JobStore jobStore;

    @PostConstruct
    public void setupJobStore() {
        try {
            jobStore = FileSystemJobStore.newFileSystemJobStore(jobStorePath);
        } catch (JobStoreException ex) {
            final String errMsg = "An Error occured while setting up the job-store.";
            LOGGER.error(errMsg, ex);
            throw new RuntimeException(errMsg, ex);
        }
    }

    @Override
    public Job createJob(JobSpecification jobSpec, FlowBinder flowBinder, Flow flow, Sink sink) throws JobStoreException {
        return jobStore.createJob(jobSpec, flowBinder, flow, sink);
    }

    @Override
    public void updateJobInfo(Job job, JobInfo jobInfo) throws JobStoreException {
        jobStore.updateJobInfo(job, jobInfo);
    }

    @Override
    public long getNumberOfChunksInJob(Job job) throws JobStoreException {
        return jobStore.getNumberOfChunksInJob(job);
    }

    @Override
    public Chunk getChunk(Job job, long chunkId) throws JobStoreException {
        return jobStore.getChunk(job, chunkId);
    }

    @Override
    public void addChunkResult(Job job, ChunkResult processedChunk) throws JobStoreException {
        jobStore.addChunkResult(job, processedChunk);
    }

    @Override
    public ChunkResult getProcessChunkResult(Job job, long chunkId) throws JobStoreException {
        return jobStore.getProcessChunkResult(job, chunkId);
    }


}
