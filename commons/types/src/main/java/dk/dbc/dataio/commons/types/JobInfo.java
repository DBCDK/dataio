package dk.dbc.dataio.commons.types;

import dk.dbc.dataio.commons.utils.invariant.InvariantUtil;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

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

    private Map<JobState.OperationalState, ChunkCounter> chunkCounters = new HashMap<JobState.OperationalState, ChunkCounter>();

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
     * Get map containing chunk counters
     * @return chunkCounters
     */
    public Map<JobState.OperationalState, ChunkCounter> getChunkCounters (){
        return chunkCounters;

    }

    /**
     * Sets a ChunkCounter in the JobInfo object.
     *
     * Containing the possible errors for chunks.    -> enum OperationalState: CHUNKIFYING
     * Containing the possible errors for processor. -> enum OperationalState: PROCESSING
     * Containing the possible errors for sinks.     -> enum OperationalState: DELIVERING
     *
     * @param operationalState
     * @param chunkCounter
     */
    public void setChunkCounter(JobState.OperationalState operationalState, ChunkCounter chunkCounter) {
        chunkCounters.put(operationalState, chunkCounter);
    }
}
