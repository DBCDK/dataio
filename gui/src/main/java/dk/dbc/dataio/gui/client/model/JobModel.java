package dk.dbc.dataio.gui.client.model;

import dk.dbc.dataio.commons.types.JobErrorCode;

import java.io.Serializable;

/**
 * Failed Items Model<br>
 * Holds data to to be used, when showing list of Jobs
 */
public class JobModel implements Serializable {
    private static final long serialVersionUID = 5222362969415514657L;
    private String jobCreationTime;
    private String jobId;
    private String fileName;
    private String submitterNumber;
    private boolean jobDone;
    private JobErrorCode jobErrorCode;
    private long chunkifyingFailureCounter;
    private long processingFailureCounter;
    private long deliveringFailureCounter;



    public JobModel(String jobCreationTime, String jobId, String fileName, String submitterNumber,
                    boolean jobDone, JobErrorCode jobErrorCode,
                    long chunkifyingFailureCounter, long processingFailureCounter, long deliveringFailureCounter) {
        this.jobCreationTime = jobCreationTime;
        this.jobId = jobId;
        this.fileName = fileName;
        this.submitterNumber = submitterNumber;
        this.jobDone = jobDone;
        this.jobErrorCode = jobErrorCode;
        this.chunkifyingFailureCounter = chunkifyingFailureCounter;
        this.processingFailureCounter = processingFailureCounter;
        this.deliveringFailureCounter = deliveringFailureCounter;
    }

    public JobModel() {
        this("", "", "", "",
                false, JobErrorCode.NO_ERROR,
                0, 0, 0);
    }

    public String getJobCreationTime() {
        return jobCreationTime;
    }

    public void setJobCreationTime(String jobCreationTime) {
        this.jobCreationTime = jobCreationTime;
    }

    public String getJobId() {
        return jobId;
    }

    public void setJobId(String jobId) {
        this.jobId = jobId;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getSubmitterNumber() {
        return submitterNumber;
    }

    public void setSubmitterNumber(String submitterNumber) {
        this.submitterNumber = submitterNumber;
    }

    public boolean getJobDone() {
        return jobDone;
    }

    public void setJobDone(boolean jobDone) {
        this.jobDone = jobDone;
    }

    public JobErrorCode getJobErrorCode() {
        return jobErrorCode;
    }

    public void setJobErrorCode(JobErrorCode jobErrorCode) {
        this.jobErrorCode = jobErrorCode;
    }

    public long getChunkifyingFailureCounter() {
        return chunkifyingFailureCounter;
    }

    public void setChunkifyingFailureCounter(long chunkifyingFailureCounter) {
        this.chunkifyingFailureCounter = chunkifyingFailureCounter;
    }

    public long getProcessingFailureCounter() {
        return processingFailureCounter;
    }

    public void setProcessingFailureCounter(long processingFailureCounter) {
        this.processingFailureCounter = processingFailureCounter;
    }

    public long getDeliveringFailureCounter() {
        return deliveringFailureCounter;
    }

    public void setDeliveringFailureCounter(long deliveringFailureCounter) {
        this.deliveringFailureCounter = deliveringFailureCounter;
    }
}
