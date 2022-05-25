package dk.dbc.dataio.gui.client.model;

import dk.dbc.dataio.commons.types.JobSpecification;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * JobModel holds all GUI data related to showing the Job Model
 */
public class JobModel extends GenericBackendModel {

    private String jobCreationTime;
    private String jobCompletionTime;
    private String jobId;
    private String submitterNumber;
    private String submitterName;
    private String flowBinderName;
    private long sinkId;
    private String sinkName;
    private List<DiagnosticModel> diagnosticModels = new ArrayList<>();
    private boolean diagnosticFatal;
    private String packaging;
    private String format;
    private String charset;
    private String destination;
    private String mailForNotificationAboutVerification;
    private String mailForNotificationAboutProcessing;
    private String resultMailInitials;
    private JobSpecification.Type type;
    private String dataFile;
    private int partNumber;
    private WorkflowNoteModel workflowNoteModel;
    private JobSpecification.Ancestry ancestry;
    private int numberOfItems;
    private int numberOfChunks;
    private StateModel stateModel = new StateModel();

    /**
     * Gets the Job Creation Time
     *
     * @return Job Creation Time
     */
    public String getJobCreationTime() {
        return jobCreationTime;
    }

    /**
     * Sets the jobCreationTime
     *
     * @param jobCreationTime The jobCreationTime to set
     * @return the updated jobModel
     */
    public JobModel withJobCreationTime(String jobCreationTime) {
        this.jobCreationTime = jobCreationTime;
        return this;
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
     * Sets the jobCompletionTime
     *
     * @param jobCompletionTime The jobCompletionTime to set
     * @return the updated jobModel
     */
    public JobModel withJobCompletionTime(String jobCompletionTime) {
        this.jobCompletionTime = jobCompletionTime;
        return this;
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
     * Sets the jobId
     *
     * @param jobId The jobId to set
     * @return the updated jobModel
     */
    public JobModel withJobId(String jobId) {
        this.jobId = jobId;
        return this;
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
     * Sets the submitterNumber
     *
     * @param submitterNumber The submitterNumber to set
     * @return the updated jobModel
     */
    public JobModel withSubmitterNumber(String submitterNumber) {
        this.submitterNumber = submitterNumber;
        return this;
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
     * Sets the submitterName
     *
     * @param submitterName The submitterName to set
     * @return the updated jobModel
     */
    public JobModel withSubmitterName(String submitterName) {
        this.submitterName = submitterName;
        return this;
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
     * Sets the flowBinderName
     *
     * @param flowBinderName The flowBinderName to set
     * @return the updated jobModel
     */
    public JobModel withFlowBinderName(String flowBinderName) {
        this.flowBinderName = flowBinderName;
        return this;
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
     * Sets the sinkId
     *
     * @param sinkId The sinkId to set
     * @return the updated jobModel
     */
    public JobModel withSinkId(long sinkId) {
        this.sinkId = sinkId;
        return this;
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
     * Sets the sinkName
     *
     * @param sinkName The sinkName to set
     * @return the updated jobModel
     */
    public JobModel withSinkName(String sinkName) {
        this.sinkName = sinkName;
        return this;
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
     * Sets the The list of Diagnostics
     *
     * @param diagnosticModels The list of Diagnostics to set
     * @return the updated jobModel
     */
    public JobModel withDiagnosticModels(List<DiagnosticModel> diagnosticModels) {
        this.diagnosticModels = diagnosticModels;
        return this;
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
     *
     * @param diagnosticFatal The boolean to set
     * @return the updated jobModel
     */
    public JobModel withDiagnosticFatal(boolean diagnosticFatal) {
        this.diagnosticFatal = diagnosticFatal;
        return this;
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
     * Sets the packaging
     *
     * @param packaging The packaging to set
     * @return the updated jobModel
     */
    public JobModel withPackaging(String packaging) {
        this.packaging = packaging;
        return this;
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
     * Sets the format
     *
     * @param format The format to set
     * @return the updated jobModel
     */
    public JobModel withFormat(String format) {
        this.format = format;
        return this;
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
     * Sets the charset
     *
     * @param charset The charset to set
     * @return the updated jobModel
     */
    public JobModel withCharset(String charset) {
        this.charset = charset;
        return this;
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
     * Sets the destination
     *
     * @param destination The destination to set
     * @return the updated jobModel
     */
    public JobModel withDestination(String destination) {
        this.destination = destination;
        return this;
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
     * Sets the mailForNotificationAboutVerification
     *
     * @param mailForNotificationAboutVerification The mailForNotificationAboutVerification to set
     * @return the updated jobModel
     */
    public JobModel withMailForNotificationAboutVerification(String mailForNotificationAboutVerification) {
        this.mailForNotificationAboutVerification = mailForNotificationAboutVerification;
        return this;
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
     * Sets the mailForNotificationAboutProcessing
     *
     * @param mailForNotificationAboutProcessing The mailForNotificationAboutProcessing to set
     * @return the updated jobModel
     */
    public JobModel withMailForNotificationAboutProcessing(String mailForNotificationAboutProcessing) {
        this.mailForNotificationAboutProcessing = mailForNotificationAboutProcessing;
        return this;
    }

    /**
     * Gets the Resultmail Initials
     *
     * @return The Resultmail Initials
     */
    public String getResultMailInitials() {
        return resultMailInitials;
    }

    /**
     * Sets the resultMailInitials
     *
     * @param resultMailInitials The resultMailInitials to set
     * @return the updated jobModel
     */
    public JobModel withResultMailInitials(String resultMailInitials) {
        this.resultMailInitials = resultMailInitials;
        return this;
    }

    /**
     * Gets the Type
     *
     * @return The type of job
     */
    public JobSpecification.Type getType() {
        return type;
    }


    /**
     * Sets the Type
     *
     * @param type The Type to set
     * @return the updated jobModel
     */
    public JobModel withType(JobSpecification.Type type) {
        this.type = type;
        return this;
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
     * Sets the dataFile
     *
     * @param dataFile the dataFile to set
     * @return the updated jobModel
     */
    public JobModel withDataFile(String dataFile) {
        this.dataFile = dataFile;
        return this;
    }

    /**
     * Gets the Part Number
     *
     * @return The Part Number
     */
    public int getPartNumber() {
        return partNumber;
    }

    /**
     * Sets the part number
     *
     * @param partNumber the part number to set
     * @return the updated jobModel
     */
    public JobModel withPartNumber(int partNumber) {
        this.partNumber = partNumber;
        return this;
    }

    /**
     * Gets the Workflow Note Model
     *
     * @return The Workflow Note Model
     */
    public WorkflowNoteModel getWorkflowNoteModel() {
        return workflowNoteModel;
    }

    /**
     * Sets the workflowNoteModel
     *
     * @param workflowNoteModel the workflowNoteModel to set
     * @return the updated jobModel
     */
    public JobModel withWorkflowNoteModel(WorkflowNoteModel workflowNoteModel) {
        this.workflowNoteModel = workflowNoteModel;
        return this;
    }

    /**
     * Sets the ancestry
     *
     * @param ancestry The Ancestry to set
     * @return the updated jobModel
     */
    public JobModel withAncestry(JobSpecification.Ancestry ancestry) {
        this.ancestry = ancestry;
        return this;
    }

    /**
     * Gets the name of the Trans File from the Ancestry
     *
     * @return The name of the Trans File from the Ancestry
     */
    public String getTransFileAncestry() {
        return ancestry != null ? ancestry.getTransfile() : null;
    }

    /**
     * Gets the name of the Data File from the Ancestry
     *
     * @return The name of the Data File from the Ancestry
     */
    public String getDataFileAncestry() {
        return ancestry != null ? ancestry.getDatafile() : null;
    }

    /**
     * Gets the Batch Id from the Ancestry
     *
     * @return The Batch Id from the Ancestry
     */
    public String getBatchIdAncestry() {
        return ancestry != null ? ancestry.getBatchId() : null;
    }

    /**
     * Gets the Details (The content of the Trans File) from the Ancestry
     *
     * @return The Details from the Ancestry
     */
    public String getDetailsAncestry() {
        try {
            if (ancestry != null && ancestry.getDetails() != null) {
                return new String(ancestry.getDetails(), StandardCharsets.UTF_8);
            }
            return "";
        } catch (Exception e) {
            return "Details not available: " + e.getMessage();
        }
    }

    /**
     * Sets the details on the Ancestry
     *
     * @param details The details to set
     * @return the updated jobModel
     */
    public JobModel withDetailsAncestry(String details) {
        if (ancestry == null) {
            ancestry = new JobSpecification.Ancestry();
        }
        ancestry.withDetails(details.getBytes());
        return this;
    }

    /**
     * Gets the Previous Job Id from the Ancestry, if this job is a re-run of a job
     *
     * @return The Previous Job Id from the Ancestry
     */
    public int getPreviousJobIdAncestry() {
        return ancestry != null ? ancestry.getPreviousJobId() : 0;
    }

    /**
     * Sets the Previous Job Id from the Ancestry, if this job is a re-run of a job
     *
     * @param previousJobIdAncestry The Previous Job Id in the Ancestry to set
     * @return the updated jobModel
     */
    public JobModel withPreviousJobIdAncestry(int previousJobIdAncestry) {
        if (ancestry == null) {
            ancestry = new JobSpecification.Ancestry();
        }
        ancestry.withPreviousJobId(previousJobIdAncestry);
        return this;
    }

    /**
     * Gets the Harvester Token from the Ancestry
     *
     * @return The Harvester Token from the Ancestry
     */
    public String getHarvesterTokenAncestry() {
        return ancestry != null ? ancestry.getHarvesterToken() : null;
    }

    /**
     * Gets the number of items created during partitioning
     *
     * @return The number of items
     */
    public int getNumberOfItems() {
        return numberOfItems;
    }

    /**
     * Sets the number of items created during partitioning
     *
     * @param numberOfItems the number of items to set
     * @return the updated jobModel
     */
    public JobModel withNumberOfItems(int numberOfItems) {
        this.numberOfItems = numberOfItems;
        return this;
    }

    /**
     * Gets the number of chunks created during partitioning
     *
     * @return The number of chunks
     */
    public int getNumberOfChunks() {
        return numberOfChunks;
    }

    /**
     * Sets the number of chunks created during partitioning
     *
     * @param numberOfChunks the number of chunks to set
     * @return the updated jobModel
     */
    public JobModel withNumberOfChunks(int numberOfChunks) {
        this.numberOfChunks = numberOfChunks;
        return this;
    }

    /**
     * Gets the state model containing information regarding the state of a job
     *
     * @return The state model
     */
    public StateModel getStateModel() {
        return stateModel;
    }

    /**
     * Sets the state model containing information regarding the state of a job
     *
     * @param stateModel the state model
     * @return the updated jobModel
     */
    public JobModel withStateModel(StateModel stateModel) {
        this.stateModel = stateModel;
        return this;
    }

    /**
     * Checks for empty String values
     *
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
