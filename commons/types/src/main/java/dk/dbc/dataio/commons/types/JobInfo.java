package dk.dbc.dataio.commons.types;

import dk.dbc.dataio.commons.utils.invariant.InvariantUtil;

import java.io.Serializable;
import java.util.Date;

/**
 * JobInfo DTO class.
 *
 * In all essence objects of this class are immutable, but due to GWT serialization
 * issues we cannot have final fields and need a default no-arg constructor.
 */
public class JobInfo implements Serializable {
    static /* final */ long JOB_ID_LOWER_THRESHOLD = 0;

    private static final long serialVersionUID = -1574107691937748872L;

    private /* final */ long jobId;
    private /* final */ JobSpecification jobSpecification;
    private /* final */ Date jobCreationTime;
    private /* final */ JobState jobState;

    private /* final */ JobErrorCode jobErrorCode;

    // Perhaps we should consider some sort of audit trail instead?
    private /* final */ String jobStatusMessage;

    private /* final */ long jobRecordCount;

    // Temporary placeholder for URL to job result
    private /* final */ String jobResultDataFile;

    private JobInfo() { }

    /**
     * Class constructor
     *
     * Attention: when changing the signature of this constructor
     * remember to also change the signature in the corresponding *JsonMixIn class.
     *
     * @param jobId job id (> {@value #JOB_ID_LOWER_THRESHOLD})
     * @param jobSpecification job specification
     * @param jobCreationTime job creation time
     * @param jobState job state
     * @param jobErrorCode job error code
     * @param jobStatusMessage job status message (can be null or empty)
     * @param jobResultDataFile uri of job result data file (can be null or empty)
     *
     * @throws NullPointerException if given null-valued argument
     * @throws IllegalArgumentException if value of jobId argument is <= {@value #JOB_ID_LOWER_THRESHOLD}
     */
    public JobInfo(long jobId, JobSpecification jobSpecification, Date jobCreationTime, JobState jobState, JobErrorCode jobErrorCode, String jobStatusMessage, long jobRecordCount, String jobResultDataFile) {
        this.jobId = InvariantUtil.checkAboveThresholdOrThrow(jobId, "jobId", JOB_ID_LOWER_THRESHOLD);
        this.jobSpecification = InvariantUtil.checkNotNullOrThrow(jobSpecification, "jobSpecification");
        this.jobCreationTime = new Date(InvariantUtil.checkNotNullOrThrow(jobCreationTime, "jobCreationTime").getTime());
        this.jobState = InvariantUtil.checkNotNullOrThrow(jobState, "jobState");
        this.jobErrorCode = InvariantUtil.checkNotNullOrThrow(jobErrorCode, "jobErrorCode");
        this.jobStatusMessage = jobStatusMessage;
        this.jobRecordCount = InvariantUtil.checkAboveThresholdOrThrow(jobRecordCount, "jobRecordCount", -1);
        this.jobResultDataFile = jobResultDataFile;
    }

    public Date getJobCreationTime() {
        return new Date(jobCreationTime.getTime());
    }

    public long getJobId() {
        return jobId;
    }

    public String getJobResultDataFile() {
        return jobResultDataFile;
    }

    public JobSpecification getJobSpecification() {
        return jobSpecification;
    }

    public JobState getJobState() {
        return jobState;
    }

    public String getJobStatusMessage() {
        return jobStatusMessage;
    }

    public JobErrorCode getJobErrorCode() {
        return jobErrorCode;
    }

    public long getJobRecordCount() {
        return jobRecordCount;
    }
}
