package dk.dbc.dataio.gui.client.model;

public class JobModel extends GenericBackendModel {

    private String jobCreationTime;
    private String jobId;
    private String fileName;
    private String submitterNumber;
    private String submitterName;
    private String flowBinderName;
    private String sinkName;
    private boolean jobDone;
    private long itemCounter;
    private long succeededCounter;
    private long failedCounter;
    private long ignoredCounter;

    public JobModel(String jobCreationTime,
                    String jobId,
                    String fileName,
                    String submitterNumber,
                    String submitterName,
                    String flowBinderName,
                    String sinkName,
                    boolean jobDone,
                    long itemCounter,
                    long succeededCounter,
                    long failedCounter,
                    long ignoredCounter) {
        this.jobCreationTime = jobCreationTime;
        this.jobId = jobId;
        this.fileName = fileName;
        this.submitterNumber = submitterNumber;
        this.submitterName = submitterName;
        this.flowBinderName = flowBinderName;
        this.sinkName = sinkName;
        this.itemCounter = itemCounter;
        this.jobDone = jobDone;
        this.succeededCounter = succeededCounter;
        this.failedCounter = failedCounter;
        this.ignoredCounter = ignoredCounter;

    }

    public JobModel() {
        this("", "", "", "", "", "", "", false, 0, 0, 0, 0);
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

    public String getSubmitterName() {
        return submitterName;
    }

    public void setSubmitterName(String submitterName) {
        this.submitterName = submitterName;
    }

    public String getFlowBinderName() {
        return flowBinderName;
    }

    public void setFlowBinderName(String flowBinderName) {
        this.flowBinderName = flowBinderName;
    }

    public String getSinkName() {
        return sinkName;
    }

    public void setSinkName(String sinkName) {
        this.sinkName = sinkName;
    }

    public long getItemCounter() {
        return itemCounter;
    }

    public void setItemCounter(long itemCounter) {
        this.itemCounter = itemCounter;
    }

    public boolean isJobDone() {
        return jobDone;
    }

    public void setJobDone(boolean jobDone) {
        this.jobDone = jobDone;
    }

    public long getSucceededCounter() {
        return succeededCounter;
    }

    public void setSucceededCounter(long succeededCounter) {
        this.succeededCounter = succeededCounter;
    }

    public long getFailedCounter() {
        return failedCounter;
    }

    public void setFailedCounter(long failedCounter) {
        this.failedCounter = failedCounter;
    }

    public long getIgnoredCounter() {
        return ignoredCounter;
    }

    public void setIgnoredCounter(long ignoredCounter) {
        this.ignoredCounter = ignoredCounter;
    }

}
