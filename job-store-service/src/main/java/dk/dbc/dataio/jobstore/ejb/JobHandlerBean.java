package dk.dbc.dataio.jobstore.ejb;

import dk.dbc.dataio.commons.types.Flow;
import dk.dbc.dataio.commons.types.JobInfo;
import dk.dbc.dataio.commons.types.JobSpecification;
import dk.dbc.dataio.commons.types.JobState;
import dk.dbc.dataio.jobstore.processor.ChunkProcessor;
import dk.dbc.dataio.jobstore.types.Chunk;
import dk.dbc.dataio.jobstore.types.Job;
import dk.dbc.dataio.jobstore.types.JobStoreException;
import dk.dbc.dataio.jobstore.types.ProcessChunkResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.ConcurrencyManagement;
import javax.ejb.ConcurrencyManagementType;
import javax.ejb.EJB;
import javax.ejb.Singleton;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

import static dk.dbc.dataio.jobstore.util.Base64Util.base64decode;

@ConcurrencyManagement(ConcurrencyManagementType.BEAN)
@Singleton
public class JobHandlerBean {
    private static final Logger LOGGER = LoggerFactory.getLogger(JobHandlerBean.class);

    @EJB
    JobStoreBean jobStore;

    public Job createJob(JobSpecification jobSpec, Flow flow) throws JobStoreException {
        Job job = jobStore.createJob(jobSpec, flow);
        JobInfo jobInfo = job.getJobInfo();
        try {
            if (jobInfo.getJobState() == JobState.CREATED) {
                processJob(job);
                jobInfo = new JobInfo(jobInfo.getJobId(), jobInfo.getJobSpecification(), jobInfo.getJobCreationTime(),
                        JobState.COMPLETED, "Job processing completed", null);
                job = new Job(jobInfo, flow);
            }
            return job;

        } catch (JobStoreException e) {
            jobInfo = new JobInfo(jobInfo.getJobId(), jobInfo.getJobSpecification(), jobInfo.getJobCreationTime(),
                    JobState.FAILED_DURING_PROCESSING, e.getMessage(), null);
            throw e;
        } finally {
            jobStore.updateJobInfo(job, jobInfo);
        }
    }

    private void processJob(Job job) throws JobStoreException {
        final long numberOfChunks = jobStore.getNumberOfChunksInJob(job);
        LOGGER.info("Processing {} chunks for job({})", numberOfChunks, job.getId());
        for (int chunkId = 1; chunkId <= numberOfChunks; chunkId++) {
            final Chunk chunk = jobStore.getChunk(job, chunkId);
            final ProcessChunkResult processedChunk = ChunkProcessor.processChunk(chunk);
            jobStore.addChunkResult(job, processedChunk);
        }
    }

    public String sendToSink(Job job) throws JobStoreException {
        final long numberOfChunks = jobStore.getNumberOfChunksInJob(job);
        final String sinkFileName = String.format("%s.sink.txt", job.getId());
        final Path sinkFilePath = Paths.get(jobStore.jobStorePath.toString(), sinkFileName);
        LOGGER.info("Combining decoded chunk results from {} chunks for jobId {} in {}", numberOfChunks, job.getId(), sinkFilePath);
        try (BufferedWriter writer = Files.newBufferedWriter(sinkFilePath,
                StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {

            for (int chunkId = 1; chunkId <= numberOfChunks; chunkId++) {
                final ProcessChunkResult chunkResult = jobStore.getProcessChunkResult(job, chunkId);
                for (String encodedRecord : chunkResult.getResults()) {
                    writer.write(base64decode(encodedRecord));
                    writer.newLine();
                }
            }
        } catch (IOException e) {
            throw new JobStoreException(String.format("Exception caught when trying to write to sink file %s", sinkFilePath), e);
        }
        return sinkFilePath.toString();
    }
}
