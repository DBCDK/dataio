package dk.dbc.dataio.gui.client.model;

/**
 * JobModel holds all GUI data related to showing the Job Model
 */
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
    private String packaging;
    private String format;
    private String charset;
    private String destination;
    private String mailForNotificationAboutVerification;
    private String mailForNotificationAboutProcessing;
    private String resultmailInitials;


    /**
     * Constructor with full parameter list
     *
     * @param jobCreationTime                      The Job Creation Time
     * @param jobId                                The Job Id
     * @param fileName                             The File Name
     * @param submitterNumber                      The Submitter Number
     * @param submitterName                        The Submitter Name
     * @param flowBinderName                       The Flow Binder Name
     * @param sinkName                             The Sink Name
     * @param jobDone                              The Job Done
     * @param itemCounter                          The Item Counter
     * @param succeededCounter                     The Succeeded Counter
     * @param failedCounter                        The Failed Counter
     * @param ignoredCounter                       The Ignored Counter
     * @param packaging                            The Packaging
     * @param format                               The Format
     * @param charset                              The Charset
     * @param destination                          The Destination
     * @param mailForNotificationAboutVerification The Mail For Notification About Verification
     * @param mailForNotificationAboutProcessing   The Mail For Notification About Processing
     * @param resultmailInitials                   The Resultmail Initials
     */
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
                    long ignoredCounter,
                    String packaging,
                    String format,
                    String charset,
                    String destination,
                    String mailForNotificationAboutVerification,
                    String mailForNotificationAboutProcessing,
                    String resultmailInitials) {
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
        this.packaging = packaging;
        this.format = format;
        this.charset = charset;
        this.destination = destination;
        this.mailForNotificationAboutVerification = mailForNotificationAboutVerification;
        this.mailForNotificationAboutProcessing = mailForNotificationAboutProcessing;
        this.resultmailInitials = resultmailInitials;
    }

    /**
     * Default empty constructor
     */
    public JobModel() {
        this("", "", "", "", "", "", "", false, 0, 0, 0, 0, "", "", "", "", "", "", "");
    }


    /**
     * Gets the Job Creation Time
     *
     * @return Job Creation Time
     */
    public String getJobCreationTime() {
        return jobCreationTime;
    }

    /**
     * Sets the Job Creation Time
     *
     * @param jobCreationTime Job Creation Time
     */
    public void setJobCreationTime(String jobCreationTime) {
        this.jobCreationTime = jobCreationTime;
    }

    /**
     * Gets the Job Id
     *
     * @return Job Id
     */
    public String getJobId() {
        return jobId;
    }

    /**
     * Sets the Job Id
     *
     * @param jobId The Job Id
     */
    public void setJobId(String jobId) {
        this.jobId = jobId;
    }

    /**
     * Gets the File Name
     *
     * @return The File Name
     */
    public String getFileName() {
        return fileName;
    }

    /**
     * Sets the File Name
     *
     * @param fileName The File Name
     */
    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    /**
     * Gets the Submitter Number
     *
     * @return The Submitter Number
     */
    public String getSubmitterNumber() {
        return submitterNumber;
    }

    /**
     * Sets the Submitter Number
     *
     * @param submitterNumber The Submitter Number
     */
    public void setSubmitterNumber(String submitterNumber) {
        this.submitterNumber = submitterNumber;
    }

    /**
     * Gets the Submitter Name
     *
     * @return The Submitter Name
     */
    public String getSubmitterName() {
        return submitterName;
    }

    /**
     * Sets the Submitter Name
     *
     * @param submitterName The Submitter Name
     */
    public void setSubmitterName(String submitterName) {
        this.submitterName = submitterName;
    }

    /**
     * Gets the Flow Binder Name
     *
     * @return The Flow Binder Name
     */
    public String getFlowBinderName() {
        return flowBinderName;
    }

    /**
     * Sets the Flow Binder Name
     *
     * @param flowBinderName The Flow Binder Name
     */
    public void setFlowBinderName(String flowBinderName) {
        this.flowBinderName = flowBinderName;
    }

    /**
     * Gets the Sink Name
     *
     * @return The Sink Name
     */
    public String getSinkName() {
        return sinkName;
    }

    /**
     * Sets the Sink Name
     *
     * @param sinkName The Sink Name
     */
    public void setSinkName(String sinkName) {
        this.sinkName = sinkName;
    }

    /**
     * Gets the Item Counter
     *
     * @return The Item Counter
     */
    public long getItemCounter() {
        return itemCounter;
    }

    /**
     * Sets the Item Counter
     *
     * @param itemCounter The Item Counter
     */
    public void setItemCounter(long itemCounter) {
        this.itemCounter = itemCounter;
    }

    /**
     * Returns the Job Done boolean
     *
     * @return True if job is done, False if job is not done
     */
    public boolean isJobDone() {
        return jobDone;
    }

    /**
     * Sets the Job Done boolean
     *
     * @param jobDone The Job Done boolearn
     */
    public void setJobDone(boolean jobDone) {
        this.jobDone = jobDone;
    }

    /**
     * Gets the Succeeded Counter
     *
     * @return The Succeeded Counter
     */
    public long getSucceededCounter() {
        return succeededCounter;
    }

    /**
     * Sets the Succeeded Counter
     *
     * @param succeededCounter The Succeeded Counter
     */
    public void setSucceededCounter(long succeededCounter) {
        this.succeededCounter = succeededCounter;
    }

    /**
     * Gets the Failed Counter
     *
     * @return The Failed Counter
     */
    public long getFailedCounter() {
        return failedCounter;
    }

    /**
     * Sets the Failed Counter
     *
     * @param failedCounter The Failed Counter
     */
    public void setFailedCounter(long failedCounter) {
        this.failedCounter = failedCounter;
    }

    /**
     * Gets the Ignored Counter
     *
     * @return The Ignored Counter
     */
    public long getIgnoredCounter() {
        return ignoredCounter;
    }

    /**
     * Sets the Ignored Counter
     *
     * @param ignoredCounter The Ignored Counter
     */
    public void setIgnoredCounter(long ignoredCounter) {
        this.ignoredCounter = ignoredCounter;
    }

    /**
     * Gets the Packaging
     *
     * @return The Packaging
     */
    public String getPackaging() {
        return packaging;
    }

    /**
     * Sets the Packagin
     *
     * @param packaging The Packaging
     */
    public void setPackaging(String packaging) {
        this.packaging = packaging;
    }

    /**
     * Gets the Format
     *
     * @return The Format
     */
    public String getFormat() {
        return format;
    }

    /**
     * Sets the Format
     *
     * @param format The Format
     */
    public void setFormat(String format) {
        this.format = format;
    }

    /**
     * Gets the Charset
     *
     * @return The Charset
     */
    public String getCharset() {
        return charset;
    }

    /**
     * Sets the Charset
     *
     * @param charset The Charset
     */
    public void setCharset(String charset) {
        this.charset = charset;
    }

    /**
     * Gets the Destination
     *
     * @return The Destination
     */
    public String getDestination() {
        return destination;
    }

    /**
     * Sets the Destination
     *
     * @param destination The Destination
     */
    public void setDestination(String destination) {
        this.destination = destination;
    }

    /**
     * Gets the Mail For Notification About Verification
     *
     * @return The Mail For Notification About Verification
     */
    public String getMailForNotificationAboutVerification() {
        return mailForNotificationAboutVerification;
    }

    /**
     * Sets the Mail For Notification About Verification
     *
     * @param mailForNotificationAboutVerification The Mail For Notification About Verification
     */
    public void setMailForNotificationvoidAboutVerification(String mailForNotificationAboutVerification) {
        this.mailForNotificationAboutVerification = mailForNotificationAboutVerification;
    }

    /**
     * Gets the Mail For Notification About Processing
     *
     * @return The Mail For Notification About Processing
     */
    public String getMailForNotificationAboutProcessing() {
        return mailForNotificationAboutProcessing;
    }

    /**
     * Set the Mail For Notification About Processing
     *
     * @param mailForNotificationAboutProcessing The Mail For Notification About Processing
     */
    public void setMailForNotificationvoidAboutProcessing(String mailForNotificationAboutProcessing) {
        this.mailForNotificationAboutProcessing = mailForNotificationAboutProcessing;
    }

    /**
     * Gets the Resultmail Initials
     *
     * @return The Resultmail Initials
     */
    public String getResultmailInitials() {
        return resultmailInitials;
    }

    /**
     * Sets the Resultmail Initials
     *
     * @param resultmailInitials The Resultmail Initials
     */
    public void setResultmailInitials(String resultmailInitials) {
        this.resultmailInitials = resultmailInitials;
    }

}
