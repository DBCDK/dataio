package dk.dbc.dataio.commons.types;

import dk.dbc.dataio.commons.utils.invariant.InvariantUtil;

import java.io.Serializable;

/**
 * JobInfo DTO class.
 *
 * In all essence objects of this class are immutable, but due to GWT serialization
 * issues we cannot have final fields and need a default no-arg constructor.
 */
public class JobInfo implements Serializable {
    private static final long serialVersionUID = -1574107691937748872L;

    private /* final */ long jobId;
    private /* final */ JobSpecification jobSpecification;
    private /* final */ long jobCreationTime;

    private JobErrorCode jobErrorCode;
    private long jobRecordCount;

    private ChunkCounter chunkifyingChunkCounter;
    private ChunkCounter processingChunkCounter;
    private ChunkCounter deliveringChunkCounter;

    private JobInfo() { }

    /**
     * Class constructor
     *
     * Attention: when changing the signature of this constructor
     * remember to also change the signature in the corresponding *JsonMixIn class.
     *
     * @param jobId job id (>= {@value dk.dbc.dataio.commons.types.Constants#JOB_ID_LOWER_BOUND})
     * @param jobSpecification job specification
     * @param jobCreationTime job creation time
     *
     * @throws NullPointerException if given null-valued argument
     * @throws IllegalArgumentException if value of jobId argument is < {@value dk.dbc.dataio.commons.types.Constants#JOB_ID_LOWER_BOUND}
     */
    public JobInfo(long jobId, JobSpecification jobSpecification, long jobCreationTime) {
        this.jobId = InvariantUtil.checkLowerBoundOrThrow(jobId, "jobId", Constants.JOB_ID_LOWER_BOUND);
        this.jobSpecification = InvariantUtil.checkNotNullOrThrow(jobSpecification, "jobSpecification");
        this.jobCreationTime = InvariantUtil.checkLowerBoundOrThrow(jobCreationTime, "jobCreationTime", 0);
        this.jobErrorCode = JobErrorCode.NO_ERROR;  // Default value
        this.jobRecordCount = 0;  // Default value
    }

    /**
     * Gets the Job Creation Time
     * @return Job Creation Time
     */
    public long getJobCreationTime() {
        return jobCreationTime;
    }

    /**
     * Gets the Job ID
     * @return Job ID
     */
    public long getJobId() {
        return jobId;
    }

    /**
     * Gets the Job Specification
     * @return Job Specification
     */
    public JobSpecification getJobSpecification() {
        return jobSpecification;
    }

    /**
     * Gets the Job Error Code
     * @return Job Error Code
     */
    public JobErrorCode getJobErrorCode() {
        return jobErrorCode;
    }

    /**
     * Gets the Job Record Count
     * @return Job Record Count
     */
    public long getJobRecordCount() {
        return jobRecordCount;
    }

    /**
     * Sets the Job Error Code
     * @param jobErrorCode
     */
    public void setJobErrorCode(JobErrorCode jobErrorCode) {
        this.jobErrorCode = InvariantUtil.checkNotNullOrThrow(jobErrorCode, "jobErrorCode");
    }

    /**
     * Sets the Job Record Count
     * @param jobRecordCount
     */
    public void setJobRecordCount(long jobRecordCount) {
        this.jobRecordCount = InvariantUtil.checkLowerBoundOrThrow(jobRecordCount, "jobRecordCount", 0);
    }

    /**
     * Gets the ChunkCounter for Chunkifying
     * @return chunkCounter
     */
    public ChunkCounter getChunkifyingChunkCounter() {
        return chunkifyingChunkCounter;
    }

    /**
     * Gets the ChunkCounter for Processing
     * @return chunkCounter
     */
    public ChunkCounter getProcessingChunkCounter() {
        return processingChunkCounter;
    }

    /**
     * Gets the ChunkCounter for Delivering
     * @return chunkCounter
     */
    public ChunkCounter getDeliveringChunkCounter() {
        return deliveringChunkCounter;
    }

    /**
     * Sets the ChunkCounter for Chunkifying
     * @param chunkifyingChunkCounter
     */
    public void setChunkifyingChunkCounter(ChunkCounter chunkifyingChunkCounter) {
        this.chunkifyingChunkCounter = chunkifyingChunkCounter;
    }

    /**
     * Sets the ChunkCounter for Processing
     * @param processingChunkCounter
     */
    public void setProcessingChunkCounter(ChunkCounter processingChunkCounter) {
        this.processingChunkCounter = processingChunkCounter;
    }

    /**
     * Sets the ChunkCounter for Delivering
     * @param deliveringChunkCounter
     */
    public void setDeliveringChunkCounter(ChunkCounter deliveringChunkCounter) {
        this.deliveringChunkCounter = deliveringChunkCounter;
    }
}
