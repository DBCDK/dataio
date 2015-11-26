/*
 * DataIO - Data IO
 * Copyright (C) 2015 Dansk Bibliotekscenter a/s, Tempovej 7-11, DK-2750 Ballerup,
 * Denmark. CVR: 15149043
 *
 * This file is part of DataIO.
 *
 * DataIO is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * DataIO is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with DataIO.  If not, see <http://www.gnu.org/licenses/>.
 */

package dk.dbc.dataio.gui.client.model;

import java.util.ArrayList;
import java.util.List;

/**
 * JobModel holds all GUI data related to showing the Job Model
 */
public class JobModel extends GenericBackendModel {


    private final static String     JOB_ID_EMPTY = "";
    private final static String     JOB_CREATION_TIME_EMPTY = "";
    private final static String     JOB_COMPLETION_TIME_EMPTY = "";
    private final static String     SUBMITTER_NUMBER_EMPTY = "";
    private final static String     SUBMITTER_NAME_EMPTY = "";
    private final static String     FLOW_BINDER_NAME_EMPTY = "";
    private final static long       SINK_ID_ZERO = 0;
    private final static String     SINK_NAME_EMPTY = "";
    private final static boolean    IS_JOB_DONE_FALSE = false;
    private final static long       ITEM_COUNTER_ZERO = 0;
    private final static long       FAILED_COUNTER_ZERO = 0;
    private final static long       IGNORED_COUNTER_ZERO = 0;
    private final static long       PROCESSING_IGNORED_COUNTER_ZERO = 0;
    private final static long       PARTITIONED_COUNTER_ZERO = 0;
    private final static long       PROCESSED_COUNTER_ZERO = 0;
    private final static long       DELIVERED_COUNTER_ZERO = 0;
    private final static ArrayList  LIST_OF_DIAGNOSTICS_EMPTY = new ArrayList<DiagnosticModel>();
    private final static boolean    HAS_FATAL_DIAGNOSTIC_FALSE = false;
    private final static String     PACKAGING_EMPTY = "";
    private final static String     FORMAT_EMPTY = "";
    private final static String     CHARSET_EMPTY = "";
    private final static String     DESTINATION_EMPTY = "";
    private final static String     MAIL_FOR_NOTIFICATION_ABOUT_VERIFICATION_EMPTY = "";
    private final static String     MAIL_FOR_NOTIFICATION_ABOUT_PROCESSING_EMPTY = "";
    private final static String     RESULT_MAIL_INITIALS_EMPTY = "";
    private final static String     DATAFILE_EMPTY = "";
    private final static int        PARTNUMBER_ZERO = 0;

    public enum Type { TRANSIENT, PERSISTENT, TEST, ACCTEST }

    private String jobCreationTime;
    private String jobCompletionTime;
    private String jobId;
    private String submitterNumber;
    private String submitterName;
    private String flowBinderName;
    private long sinkId;
    private String sinkName;
    private boolean jobDone;
    private long itemCounter;
    private long failedCounter;
    private long ignoredCounter;
    private long processingIgnoredCounter;
    private long partitionedCounter;
    private long processedCounter;
    private long deliveredCounter;
    private List<DiagnosticModel> diagnosticModels;
    private boolean diagnosticFatal;
    private String packaging;
    private String format;
    private String charset;
    private String destination;
    private String mailForNotificationAboutVerification;
    private String mailForNotificationAboutProcessing;
    private String resultmailInitials;
    private Type type;
    private String dataFile;
    private int partNumber;

    /**
     * Constructor with full parameter list
     *
     * @param jobCreationTime                      The Job Creation Time
     * @param jobCompletionTime                    The Job Completion Time
     * @param jobId                                The Job Id
     * @param submitterNumber                      The Submitter Number
     * @param submitterName                        The Submitter Name
     * @param flowBinderName                       The Flow Binder Name
     * @param sinkId                               The Sink Id
     * @param sinkName                             The Sink Name
     * @param jobDone                              The Job Done
     * @param itemCounter                          The Item Counter
     * @param failedCounter                        The Failed Counter
     * @param ignoredCounter                       The Ignored Counter
     * @param processingIgnoredCounter             The Failed Counter for processing phase
     * @param partitionedCounter                   The number of Partitioned Items
     * @param processedCounter                     The number of Processed Items
     * @param deliveredCounter                     The number of Delivered Items
     * @param diagnosticModels                     The list of Diagnostics
     * @param diagnosticFatal                      The boolean telling if the list of Diagnostics contains FATAL
     * @param packaging                            The Packaging
     * @param format                               The Format
     * @param charset                              The Charset
     * @param destination                          The Destination
     * @param mailForNotificationAboutVerification The Mail For Notification About Verification
     * @param mailForNotificationAboutProcessing   The Mail For Notification About Processing
     * @param resultmailInitials                   The Resultmail Initials
     * @param type                                 The type of job (TRANSIENT, PERSISTENT, TEST, ACCTEST)
     */
    public JobModel(String jobCreationTime,
                    String jobCompletionTime,
                    String jobId,
                    String submitterNumber,
                    String submitterName,
                    String flowBinderName,
                    long sinkId,
                    String sinkName,
                    boolean jobDone,
                    long itemCounter,
                    long failedCounter,
                    long ignoredCounter,
                    long processingIgnoredCounter,
                    long partitionedCounter,
                    long processedCounter,
                    long deliveredCounter,
                    List<DiagnosticModel> diagnosticModels,
                    boolean diagnosticFatal,
                    String packaging,
                    String format,
                    String charset,
                    String destination,
                    String mailForNotificationAboutVerification,
                    String mailForNotificationAboutProcessing,
                    String resultmailInitials,
                    Type type,
                    String dataFile,
                    int partNumber) {
        this.jobCreationTime = jobCreationTime;
        this.jobCompletionTime = jobCompletionTime;
        this.jobId = jobId;
        this.submitterNumber = submitterNumber;
        this.submitterName = submitterName;
        this.flowBinderName = flowBinderName;
        this.sinkId = sinkId;
        this.sinkName = sinkName;
        this.jobDone = jobDone;
        this.itemCounter = itemCounter;
        this.failedCounter = failedCounter;
        this.ignoredCounter = ignoredCounter;
        this.processingIgnoredCounter = processingIgnoredCounter;
        this.partitionedCounter = partitionedCounter;
        this.processedCounter = processedCounter;
        this.deliveredCounter = deliveredCounter;
        this.diagnosticModels = diagnosticModels;
        this.diagnosticFatal = diagnosticFatal;
        this.packaging = packaging;
        this.format = format;
        this.charset = charset;
        this.destination = destination;
        this.mailForNotificationAboutVerification = mailForNotificationAboutVerification;
        this.mailForNotificationAboutProcessing = mailForNotificationAboutProcessing;
        this.resultmailInitials = resultmailInitials;
        this.type = type;
        this.dataFile = dataFile;
        this.partNumber = partNumber;
    }

    /**
     * Default empty constructor
     */
    public JobModel() {
        this(
                JOB_CREATION_TIME_EMPTY,
                JOB_COMPLETION_TIME_EMPTY,
                JOB_ID_EMPTY,
                SUBMITTER_NUMBER_EMPTY,
                SUBMITTER_NAME_EMPTY,
                FLOW_BINDER_NAME_EMPTY,
                SINK_ID_ZERO,
                SINK_NAME_EMPTY,
                IS_JOB_DONE_FALSE,
                ITEM_COUNTER_ZERO,
                FAILED_COUNTER_ZERO,
                IGNORED_COUNTER_ZERO,
                PROCESSING_IGNORED_COUNTER_ZERO,
                PARTITIONED_COUNTER_ZERO,
                PROCESSED_COUNTER_ZERO,
                DELIVERED_COUNTER_ZERO,
                LIST_OF_DIAGNOSTICS_EMPTY,
                HAS_FATAL_DIAGNOSTIC_FALSE,
                PACKAGING_EMPTY,
                FORMAT_EMPTY,
                CHARSET_EMPTY,
                DESTINATION_EMPTY,
                MAIL_FOR_NOTIFICATION_ABOUT_VERIFICATION_EMPTY,
                MAIL_FOR_NOTIFICATION_ABOUT_PROCESSING_EMPTY,
                RESULT_MAIL_INITIALS_EMPTY,
                Type.TRANSIENT,
                DATAFILE_EMPTY,
                PARTNUMBER_ZERO);
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
     * Sets the Job Completion Time
     *
     * @param jobCompletionTime Job Completion Time
     */
    public void setJobCompletionTime(String jobCompletionTime) {
        this.jobCompletionTime = jobCompletionTime;
    }

    /**
     * Gets the Job Completion Time
     *
     * @return Job Completion Time
     */
    public String getJobCompletionTime() {
        return jobCompletionTime;
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
     * Gets the Sink Id
     *
     * @return The Sink Id
     */
    public long getSinkId() {
        return sinkId;
    }

    /**
     * Sets the Sink Id
     *
     * @param sinkId The Sink Id
     */
    public void setSinkId(long sinkId) {
        this.sinkId = sinkId;
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
     * Gets the Ignored Counter for processing phase
     *
     * @return The Processing Ignored Counter
     */
    public long getProcessingIgnoredCounter() {
        return processingIgnoredCounter;
    }

    /**
     * Sets the Processing Ignored Counter
     *
     * @param processingIgnoredCounter The Ignored Counter for processing phase
     */
    public void setProcessingIgnoredCounter(long processingIgnoredCounter) {
        this.processingIgnoredCounter = processingIgnoredCounter;
    }

    /**
     * Gets the Partitioned Counter
     *
     * @return The Partitioned Counter
     */
    public long getPartitionedCounter() {
        return partitionedCounter;
    }

    /**
     * Sets the Partitioned Counter
     *
     * @param partitionedCounter The Partitioned Counter
     */
    public void setPartitionedCounter(long partitionedCounter) {
        this.partitionedCounter = partitionedCounter;
    }

    /**
     * Gets the Processed Counter
     *
     * @return The Processed Counter
     */
    public long getProcessedCounter() {
        return processedCounter;
    }

    /**
     * Sets the Processed Counter
     *
     * @param processedCounter The Processed Counter
     */
    public void setProcessedCounter(long processedCounter) {
        this.processedCounter = processedCounter;
    }

    /**
     * Gets the Delivered Counter
     *
     * @return The Delivered Counter
     */
    public long getDeliveredCounter() {
        return deliveredCounter;
    }

    /**
     * Sets the Delivered Counter
     *
     * @param deliveredCounter The Delivered Counter
     */
    public void setDeliveredCounter(long deliveredCounter) {
        this.deliveredCounter = deliveredCounter;
    }


    /**
     * Gets the list of Diagnostics
     *
     * @return The list of Diagnostics
     */
    public List<DiagnosticModel> getDiagnosticModels() {
        return diagnosticModels;
    }

    /**
     * Gets the boolean telling if the list of Diagnostics contains FATAL
     *
     * @return true if FATAL is in list, otherwise false
     */
    public boolean isDiagnosticFatal() {
        return diagnosticFatal;
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

    /**
     * Gets the Type
     *
     * @return The type of job
     */
    public Type getType() {
        return type;
    }

    public void setType(JobModel.Type type) {
        this.type = type;
    }


    public int getPartNumber() {
        return partNumber;
    }

    /**
     * Gets the data file
     *
     * @return The data file of the job
     */
    public String getDataFile() {
        return dataFile;
    };
    /**
     * Checks for empty String values
     * @return true if no empty String values were found, otherwise false
     */
    public boolean isInputFieldsEmpty() {
        return
                jobId.isEmpty()
                || packaging.isEmpty()
                || format.isEmpty()
                || charset.isEmpty()
                || destination.isEmpty()
                || type == null;
    }
}
