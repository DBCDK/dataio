package dk.dbc.dataio.commons.utils.test.model;

import dk.dbc.dataio.commons.types.ChunkCounter;
import dk.dbc.dataio.commons.types.JobErrorCode;
import dk.dbc.dataio.commons.types.JobInfo;
import dk.dbc.dataio.commons.types.JobSpecification;


public class JobInfoBuilder {
    private long jobId = 1L;
    private JobSpecification jobSpecification = new JobSpecificationBuilder().build();
    private long jobCreationTime;
    private JobErrorCode jobErrorCode = JobErrorCode.NO_ERROR;
    private long jobRecordCount = 1L;
    private ChunkCounter chunkifyingChunkCounter = new ChunkCounterBuilder().build();
    private ChunkCounter processingChunkCounter = new ChunkCounterBuilder().build();
    private ChunkCounter deliveringChunkCounter = new ChunkCounterBuilder().build();

    public JobInfoBuilder() {
        this.jobCreationTime = System.currentTimeMillis();
    }

    public JobInfoBuilder setJobId(long jobId) {
        this.jobId = jobId;
        return this;
    }

    public JobInfoBuilder setJobSpecification(JobSpecification jobSpecification) {
        this.jobSpecification = jobSpecification;
        return this;
    }

    public JobInfoBuilder setJobCreationTime(long jobCreationTime) {
        this.jobCreationTime = jobCreationTime;
        return this;
    }

    public JobInfoBuilder setJobErrorCode(JobErrorCode jobErrorCode) {
        this.jobErrorCode = jobErrorCode;
        return this;
    }

    public JobInfoBuilder setJobRecordCount(long jobRecordCount) {
        this.jobRecordCount = jobRecordCount;
        return this;
    }

    public JobInfoBuilder setChunkifyingChunkCounter(ChunkCounter chunkifyingChunkCounter) {
        this.chunkifyingChunkCounter = chunkifyingChunkCounter;
        return this;
    }

    public JobInfoBuilder setProcessingChunkCounter(ChunkCounter processingChunkCounter) {
        this.processingChunkCounter = processingChunkCounter;
        return this;
    }

    public JobInfoBuilder setDeliveringChunkCounter(ChunkCounter deliveringChunkCounter) {
        this.deliveringChunkCounter = deliveringChunkCounter;
        return this;
    }

    public JobInfo build() {
        JobInfo jobInfo = new JobInfo(jobId, jobSpecification, jobCreationTime);
        jobInfo.setJobErrorCode(this.jobErrorCode);
        jobInfo.setJobRecordCount(this.jobRecordCount);
        jobInfo.setChunkifyingChunkCounter(chunkifyingChunkCounter);
        jobInfo.setProcessingChunkCounter(processingChunkCounter);
        jobInfo.setDeliveringChunkCounter(deliveringChunkCounter);
        return jobInfo;
    }
}
