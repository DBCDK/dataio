package dk.dbc.dataio.gui.client.model;

import dk.dbc.dataio.commons.types.JobErrorCode;

import java.io.Serializable;

/**
 * Failed Items Model<br>
 * Holds data to to be used, when showing list of Jobs
 */
public class JobModelOld implements Serializable {
    private static final long serialVersionUID = 5222362969415514657L;
    private String jobCreationTime;
    private String jobId;
    private String fileName;
    private String submitterNumber;
    private boolean jobDone;
    private JobErrorCode jobErrorCode;
    private long chunkifyingTotalCounter;
    private long chunkifyingSuccessCounter;
    private long chunkifyingFailureCounter;
    private long chunkifyingIgnoredCounter;
    private long processingTotalCounter;
    private long processingSuccessCounter;
    private long processingFailureCounter;
    private long processingIgnoredCounter;
    private long deliveringTotalCounter;
    private long deliveringSuccessCounter;
    private long deliveringFailureCounter;
    private long deliveringIgnoredCounter;



    public JobModelOld(String jobCreationTime, String jobId, String fileName, String submitterNumber,
                       boolean jobDone, JobErrorCode jobErrorCode,
                       long chunkifyingTotalCounter,
                       long chunkifyingSuccessCounter,
                       long chunkifyingFailureCounter,
                       long chunkifyingIgnoredCounter,
                       long processingTotalCounter,
                       long processingSuccessCounter,
                       long processingFailureCounter,
                       long processingIgnoredCounter,
                       long deliveringTotalCounter,
                       long deliveringSuccessCounter,
                       long deliveringFailureCounter,
                       long deliveringIgnoredCounter) {
        this.jobCreationTime = jobCreationTime;
        this.jobId = jobId;
        this.fileName = fileName;
        this.submitterNumber = submitterNumber;
        this.jobDone = jobDone;
        this.jobErrorCode = jobErrorCode;
        this.chunkifyingTotalCounter = chunkifyingTotalCounter;
        this.chunkifyingSuccessCounter = chunkifyingSuccessCounter;
        this.chunkifyingFailureCounter = chunkifyingFailureCounter;
        this.chunkifyingIgnoredCounter = chunkifyingIgnoredCounter;
        this.processingTotalCounter = processingTotalCounter;
        this.processingSuccessCounter = processingSuccessCounter;
        this.processingFailureCounter = processingFailureCounter;
        this.processingIgnoredCounter = processingIgnoredCounter;
        this.deliveringTotalCounter = deliveringTotalCounter;
        this.deliveringSuccessCounter = deliveringSuccessCounter;
        this.deliveringFailureCounter = deliveringFailureCounter;
        this.deliveringIgnoredCounter = deliveringIgnoredCounter;
    }

    public JobModelOld() {
        this("", "", "", "",
                false, JobErrorCode.NO_ERROR,
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
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

    public long getChunkifyingTotalCounter() {
        return chunkifyingTotalCounter;
    }

    public void setChunkifyingTotalCounter(long chunkifyingTotalCounter) {
        this.chunkifyingTotalCounter = chunkifyingTotalCounter;
    }

    public long getChunkifyingSuccessCounter() {
        return chunkifyingSuccessCounter;
    }

    public void setChunkifyingSuccessCounter(long chunkifyingSuccessCounter) {
        this.chunkifyingSuccessCounter = chunkifyingSuccessCounter;
    }

    public long getChunkifyingFailureCounter() {
        return chunkifyingFailureCounter;
    }

    public void setChunkifyingFailureCounter(long chunkifyingFailureCounter) {
        this.chunkifyingFailureCounter = chunkifyingFailureCounter;
    }

    public long getChunkifyingIgnoredCounter() {
        return chunkifyingIgnoredCounter;
    }

    public void setChunkifyingIgnoredCounter(long chunkifyingIgnoredCounter) {
        this.chunkifyingIgnoredCounter = chunkifyingIgnoredCounter;
    }

    public long getProcessingTotalCounter() {
        return processingTotalCounter;
    }

    public void setProcessingTotalCounter(long processingTotalCounter) {
        this.processingTotalCounter = processingTotalCounter;
    }

    public long getProcessingSuccessCounter() {
        return processingSuccessCounter;
    }

    public void setProcessingSuccessCounter(long processingSuccessCounter) {
        this.processingSuccessCounter = processingSuccessCounter;
    }

    public long getProcessingFailureCounter() {
        return processingFailureCounter;
    }

    public void setProcessingFailureCounter(long processingFailureCounter) {
        this.processingFailureCounter = processingFailureCounter;
    }

    public long getProcessingIgnoredCounter() {
        return processingIgnoredCounter;
    }

    public void setProcessingIgnoredCounter(long processingIgnoredCounter) {
        this.processingIgnoredCounter = processingIgnoredCounter;
    }

    public long getDeliveringTotalCounter() {
        return deliveringTotalCounter;
    }

    public void setDeliveringTotalCounter(long deliveringTotalCounter) {
        this.deliveringTotalCounter = deliveringTotalCounter;
    }

    public long getDeliveringSuccessCounter() {
        return deliveringSuccessCounter;
    }

    public void setDeliveringSuccessCounter(long deliveringSuccessCounter) {
        this.deliveringSuccessCounter = deliveringSuccessCounter;
    }

    public long getDeliveringFailureCounter() {
        return deliveringFailureCounter;
    }

    public void setDeliveringFailureCounter(long deliveringFailureCounter) {
        this.deliveringFailureCounter = deliveringFailureCounter;
    }

    public long getDeliveringIgnoredCounter() {
        return deliveringIgnoredCounter;
    }

    public void setDeliveringIgnoredCounter(long deliveringIgnoredCounter) {
        this.deliveringIgnoredCounter = deliveringIgnoredCounter;
    }
}
