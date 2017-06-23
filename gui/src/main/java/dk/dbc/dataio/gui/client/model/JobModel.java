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

import dk.dbc.dataio.commons.types.JobSpecification;

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
    private final static int       FAILED_COUNTER_ZERO = 0;
    private final static int       IGNORED_COUNTER_ZERO = 0;
    private final static int       PROCESSING_IGNORED_COUNTER_ZERO = 0;
    private final static int       PARTITIONED_COUNTER_ZERO = 0;
    private final static int       PROCESSED_COUNTER_ZERO = 0;
    private final static int       DELIVERED_COUNTER_ZERO = 0;
    private final static int       PARTITIONING_FAILED_COUNTER_ZERO = 0;
    private final static int       PROCESSING_FAILED_COUNTER_ZERO = 0;
    private final static int       DELIVERING_FAILED_COUNTER_ZERO = 0;
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
    private final static WorkflowNoteModel WORKFLOW_NOTE_MODEL_NULL = null;
    private final static int       NUMBER_OF_ITEMS_ZERO = 0;
    private final static int       NUMBER_OF_CHUNKS_ZERO = 0;


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
    private int failedCounter;
    private int ignoredCounter;
    private int processingIgnoredCounter;
    private int partitionedCounter;
    private int processedCounter;
    private int deliveredCounter;
    private int partitioningFailedCounter;
    private int processingFailedCounter;
    private int deliveringFailedCounter;
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
    private WorkflowNoteModel workflowNoteModel;
    private JobSpecification.Ancestry ancestry;
    private int numberOfItems;
    private int numberOfChunks;


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
     * @param failedCounter                        The Failed Counter
     * @param ignoredCounter                       The Ignored Counter
     * @param processingIgnoredCounter             The Failed Counter for processing phase
     * @param partitionedCounter                   The number of Partitioned Items
     * @param processedCounter                     The number of Processed Items
     * @param deliveredCounter                     The number of Delivered Items
     * @param partitioningFailedCounter            The number of items failed in partitioning
     * @param processingFailedCounter              The number of items failed in processing
     * @param deliveringFailedCounter              The number of items failed in delivering
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
     * @param dataFile                             The data file of the job
     * @param partNumber                           The part number
     * @param workflowNoteModel                    The workflow note model
     * @param ancestry                             The ancestry
     * @param numberOfItems                        The number of items created during partitioning
     * @param numberOfChunks                       The number of chunks created during partitioning
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
                    int failedCounter,
                    int ignoredCounter,
                    int processingIgnoredCounter,
                    int partitionedCounter,
                    int processedCounter,
                    int deliveredCounter,
                    int partitioningFailedCounter,
                    int processingFailedCounter,
                    int deliveringFailedCounter,
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
                    int partNumber,
                    WorkflowNoteModel workflowNoteModel,
                    JobSpecification.Ancestry ancestry,
                    int numberOfItems,
                    int numberOfChunks) {
        this.jobCreationTime = jobCreationTime;
        this.jobCompletionTime = jobCompletionTime;
        this.jobId = jobId;
        this.submitterNumber = submitterNumber;
        this.submitterName = submitterName;
        this.flowBinderName = flowBinderName;
        this.sinkId = sinkId;
        this.sinkName = sinkName;
        this.jobDone = jobDone;
        this.failedCounter = failedCounter;
        this.ignoredCounter = ignoredCounter;
        this.processingIgnoredCounter = processingIgnoredCounter;
        this.partitionedCounter = partitionedCounter;
        this.processedCounter = processedCounter;
        this.deliveredCounter = deliveredCounter;
        this.partitioningFailedCounter = partitioningFailedCounter;
        this.processingFailedCounter = processingFailedCounter;
        this.deliveringFailedCounter = deliveringFailedCounter;
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
        this.workflowNoteModel = workflowNoteModel;
        this.ancestry = ancestry;
        this.numberOfItems = numberOfItems;
        this.numberOfChunks = numberOfChunks;
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
                FAILED_COUNTER_ZERO,
                IGNORED_COUNTER_ZERO,
                PROCESSING_IGNORED_COUNTER_ZERO,
                PARTITIONED_COUNTER_ZERO,
                PROCESSED_COUNTER_ZERO,
                DELIVERED_COUNTER_ZERO,
                PARTITIONING_FAILED_COUNTER_ZERO,
                PROCESSING_FAILED_COUNTER_ZERO,
                DELIVERING_FAILED_COUNTER_ZERO,
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
                PARTNUMBER_ZERO,
                WORKFLOW_NOTE_MODEL_NULL,
                null,
                NUMBER_OF_ITEMS_ZERO,
                NUMBER_OF_CHUNKS_ZERO);
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
     * Gets the Submitter Number
     *
     * @return The Submitter Number
     */
    public String getSubmitterNumber() {
        return submitterNumber;
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
     * Gets the Flow Binder Name
     *
     * @return The Flow Binder Name
     */
    public String getFlowBinderName() {
        return flowBinderName;
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
     * Returns the Job Done boolean
     *
     * @return True if job is done, False if job is not done
     */
    public boolean isJobDone() {
        return jobDone;
    }

    /**
     * Gets the Failed Counter
     *
     * @return The Failed Counter
     */
    public int getFailedCounter() {
        return failedCounter;
    }

    /**
     * Gets the Ignored Counter
     *
     * @return The Ignored Counter
     */
    public int getIgnoredCounter() {
        return ignoredCounter;
    }

    /**
     * Gets the Ignored Counter for processing phase
     *
     * @return The Processing Ignored Counter
     */
    public int getProcessingIgnoredCounter() {
        return processingIgnoredCounter;
    }

    /**
     * Gets the Partitioned Counter
     *
     * @return The Partitioned Counter
     */
    public int getPartitionedCounter() {
        return partitionedCounter;
    }

    /**
     * Gets the Processed Counter
     *
     * @return The Processed Counter
     */
    public int getProcessedCounter() {
        return processedCounter;
    }

    /**
     * Gets the Delivered Counter
     *
     * @return The Delivered Counter
     */
    public int getDeliveredCounter() {
        return deliveredCounter;
    }

    /**
     * Gets the Partitioning Failed Counter
     *
     * @return The Partitioning Failed Counter
     */
    public int getPartitioningFailedCounter() {
        return partitioningFailedCounter;
    }

    /**
     * Gets the Processing Failed Counter
     *
     * @return The Processing Failed Counter
     */
    public int getProcessingFailedCounter() {
        return processingFailedCounter;
    }

    /**
     * Gets the Delivering Failed Counter
     *
     * @return The Delivering Failed Counter
     */
    public int getDeliveringFailedCounter() {
        return deliveringFailedCounter;
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
     * Sets the boolean telling if the list of Diagnostics contains FATAL
     * @param diagnosticFatal boolean telling if a job has a FATAL error
     */
    public void setDiagnosticFatal(boolean diagnosticFatal) {
        this.diagnosticFatal = diagnosticFatal;
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
     * Sets the Packaging
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

    /**
     * Sets the Type
     * @param type The Type to set
     */
    public void setType(JobModel.Type type) {
        this.type = type;
    }

    /**
     * Gets the data file
     *
     * @return The data file of the job
     */
    public String getDataFile() {
        return dataFile;
    }

    /**
     * Sets the data file
     * @param dataFile The data file to set
     */
    public void setDataFile(String dataFile) {
        this.dataFile = dataFile;
    }

    /**
     * Gets the Part Number
     * @return The Part Number
     */
    public int getPartNumber() {
        return partNumber;
    }

    /**
     * Gets the Workflow Note Model
     * @return The Workflow Note Model
     */
    public WorkflowNoteModel getWorkflowNoteModel() {
        return workflowNoteModel;
    }

    /**
     * Sets the Workflow Note Model
     * @param workflowNoteModel The Workflow Note Model
     */
    public void setWorkflowNoteModel(WorkflowNoteModel workflowNoteModel) {
        this.workflowNoteModel = workflowNoteModel;
    }

    public void setAncestry(JobSpecification.Ancestry ancestry) {
        this.ancestry = ancestry;
    }

    /**
     * Gets the name of the Trans File from the Ancestry
     * @return The name of the Trans File from the Ancestry
     */
    public String getTransFileAncestry() {
        return ancestry != null ? ancestry.getTransfile() : null;
    }

    /**
     * Gets the name of the Data File from the Ancestry
     * @return The name of the Data File from the Ancestry
     */
    public String getDataFileAncestry() {
        return ancestry != null ? ancestry.getDatafile() : null;
    }

    /**
     * Gets the Batch Id from the Ancestry
     * @return The Batch Id from the Ancestry
     */
    public String getBatchIdAncestry() {
        return ancestry != null ? ancestry.getBatchId() : null;
    }

    /**
     * Gets the Details (The content of the Trans File) from the Ancestry
     * @return The Details from the Ancestry
     */
    public String getDetailsAncestry() {
        return ancestry != null && ancestry.getDetails() != null ? new String(ancestry.getDetails()) : "";
    }

    /**
     * Gets the Previous Job Id from the Ancestry, if this job is a re-run of a job
     * @return The Previous Job Id from the Ancestry
     */
    public int getPreviousJobIdAncestry() {
        return ancestry != null ? ancestry.getPreviousJobId() : 0;
    }

    /**
     * Sets the Previous Job Id from the Ancestry, if this job is a re-run of a job
     * @param previousJobIdAncestry The Previous Job Id in the Ancestry
     */
    public void setPreviousJobIdAncestry(int previousJobIdAncestry) {
        if(ancestry == null) {
            this.ancestry = new JobSpecification.Ancestry();
        }
        this.ancestry.withPreviousJobId(previousJobIdAncestry);
        System.out.println(ancestry.getPreviousJobId());
    }

    /**
     * Gets the Harvester Token from the Ancestry
     * @return The Harvester Token from the Ancestry
     */
    public String getHarvesterToken() {
        return ancestry != null ? ancestry.getHarvesterToken() : null;
    }


    /**
     * Gets the number of items created during partitioning
     * @return The number of items
     */
    public int getNumberOfItems() {
        return numberOfItems;
    }


    /**
     * Sets the number of items created during partitioning
     * @param numberOfItems created during partitioning
     */
    public void setNumberOfItems(int numberOfItems) {
        this.numberOfItems = numberOfItems;
    }

    /**
     * Gets the number of chunks created during partitioning
     * @return The number of chunks
     */
    public int getNumberOfChunks() {
        return numberOfChunks;
    }

    /**
     * Sets the number of chunks created during partitioning
     * @param numberOfChunks created during partitioning
     */
    public void setNumberOfChunks(int numberOfChunks) {
        this.numberOfChunks = numberOfChunks;
    }

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

    /*
     *  The job is to be recreated from file store if:
     *      The job has encountered a fatal error
     *      The job is of type preview (items present without chunk)
     */
    public boolean isResubmitJob() {
        if(numberOfChunks == 0 && numberOfItems > 0 || diagnosticFatal) {
            return true;
        }
        return false;
    }

    /*
     * The job can be re run with just the failed items if:
     *      The job has failed items within it
     *      The job has not failed with a fatal error
     *      The job is not of type preview (it contain chunks)
     */
    public boolean hasFailedOnlyOption() {
        return failedCounter > 0 && !diagnosticFatal && numberOfChunks > 0;
    }
}
