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
     * @param jobErrorCode job error code
     *
     * @throws NullPointerException if given null-valued argument
     * @throws IllegalArgumentException if value of jobId argument is < {@value dk.dbc.dataio.commons.types.Constants#JOB_ID_LOWER_BOUND}
     */
    public JobInfo(long jobId, JobSpecification jobSpecification, long jobCreationTime, JobErrorCode jobErrorCode, long jobRecordCount) {
        this.jobId = InvariantUtil.checkLowerBoundOrThrow(jobId, "jobId", Constants.JOB_ID_LOWER_BOUND);
        this.jobSpecification = InvariantUtil.checkNotNullOrThrow(jobSpecification, "jobSpecification");
        this.jobCreationTime = InvariantUtil.checkLowerBoundOrThrow(jobCreationTime, "jobCreationTime", 0);
        this.jobErrorCode = InvariantUtil.checkNotNullOrThrow(jobErrorCode, "jobErrorCode");
        this.jobRecordCount = InvariantUtil.checkLowerBoundOrThrow(jobRecordCount, "jobRecordCount", 0);
    }

    public long getJobCreationTime() {
        return jobCreationTime;
    }

    public long getJobId() {
        return jobId;
    }

    public JobSpecification getJobSpecification() {
        return jobSpecification;
    }

    public JobErrorCode getJobErrorCode() {
        return jobErrorCode;
    }

    public long getJobRecordCount() {
        return jobRecordCount;
    }

    public void setJobErrorCode(JobErrorCode jobErrorCode) {
        this.jobErrorCode = InvariantUtil.checkNotNullOrThrow(jobErrorCode, "jobErrorCode");
    }

    public void setJobRecordCount(long jobRecordCount) {
        this.jobRecordCount = InvariantUtil.checkLowerBoundOrThrow(jobRecordCount, "jobRecordCount", 0);
    }

}
