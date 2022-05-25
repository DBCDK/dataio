package dk.dbc.dataio.jobstore.types;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import dk.dbc.dataio.commons.types.JobSpecification;

import java.util.Date;

public class JobInfoSnapshot {
    private int jobId;
    private boolean eoj;
    @JsonProperty("hasFatalError")
    private boolean fatalError;
    private int partNumber;
    private int numberOfChunks;
    private int numberOfItems;
    private Date timeOfCreation;
    private Date timeOfLastModification;
    private Date timeOfCompletion;
    private JobSpecification specification;
    private State state;
    private FlowStoreReferences flowStoreReferences;
    private WorkflowNote workflowNote;

    @JsonCreator
    public JobInfoSnapshot() {
    }

    public int getJobId() {
        return jobId;
    }

    public JobInfoSnapshot withJobId(int jobId) {
        this.jobId = jobId;
        return this;
    }

    public boolean isEoj() {
        return eoj;
    }

    public JobInfoSnapshot withEoj(boolean eoj) {
        this.eoj = eoj;
        return this;
    }

    @JsonProperty
    public boolean hasFatalError() {
        return fatalError;
    }

    public JobInfoSnapshot withFatalError(boolean fatalError) {
        this.fatalError = fatalError;
        return this;
    }

    public int getPartNumber() {
        return partNumber;
    }

    public JobInfoSnapshot withPartNumber(int partNumber) {
        this.partNumber = partNumber;
        return this;
    }

    public int getNumberOfChunks() {
        return numberOfChunks;
    }

    public JobInfoSnapshot withNumberOfChunks(int numberOfChunks) {
        this.numberOfChunks = numberOfChunks;
        return this;
    }

    public int getNumberOfItems() {
        return numberOfItems;
    }

    public JobInfoSnapshot withNumberOfItems(int numberOfItems) {
        this.numberOfItems = numberOfItems;
        return this;
    }

    public Date getTimeOfCreation() {
        return this.timeOfCreation == null ? null : new Date(this.timeOfCreation.getTime());
    }

    public JobInfoSnapshot withTimeOfCreation(Date timeOfCreation) {
        this.timeOfCreation = timeOfCreation == null ? null : new Date(timeOfCreation.getTime());
        return this;
    }

    public Date getTimeOfLastModification() {
        return this.timeOfLastModification == null ? null : new Date(this.timeOfLastModification.getTime());
    }

    public JobInfoSnapshot withTimeOfLastModification(Date timeOfLastModification) {
        this.timeOfLastModification = timeOfLastModification == null ? null : new Date(timeOfLastModification.getTime());
        return this;
    }

    public Date getTimeOfCompletion() {
        return this.timeOfCompletion == null ? null : new Date(this.timeOfCompletion.getTime());
    }

    public JobInfoSnapshot withTimeOfCompletion(Date timeOfCompletion) {
        this.timeOfCompletion = timeOfCompletion == null ? null : new Date(timeOfCompletion.getTime());
        return this;
    }

    public JobSpecification getSpecification() {
        return specification;
    }

    public JobInfoSnapshot withSpecification(JobSpecification jobSpecification) {
        this.specification = jobSpecification;
        return this;
    }

    public State getState() {
        return state;
    }

    public JobInfoSnapshot withState(State state) {
        this.state = state;
        return this;
    }

    public FlowStoreReferences getFlowStoreReferences() {
        return flowStoreReferences;
    }

    public JobInfoSnapshot withFlowStoreReferences(FlowStoreReferences flowStoreReferences) {
        this.flowStoreReferences = flowStoreReferences;
        return this;
    }

    public WorkflowNote getWorkflowNote() {
        return workflowNote;
    }

    public JobInfoSnapshot withWorkflowNote(WorkflowNote workflowNote) {
        this.workflowNote = workflowNote;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof JobInfoSnapshot)) return false;

        JobInfoSnapshot that = (JobInfoSnapshot) o;

        if (jobId != that.jobId) return false;
        if (eoj != that.eoj) return false;
        if (fatalError != that.fatalError) return false;
        if (partNumber != that.partNumber) return false;
        if (numberOfChunks != that.numberOfChunks) return false;
        if (numberOfItems != that.numberOfItems) return false;
        if (timeOfCreation != null ? !timeOfCreation.equals(that.timeOfCreation) : that.timeOfCreation != null)
            return false;
        if (timeOfLastModification != null ? !timeOfLastModification.equals(that.timeOfLastModification) : that.timeOfLastModification != null)
            return false;
        if (timeOfCompletion != null ? !timeOfCompletion.equals(that.timeOfCompletion) : that.timeOfCompletion != null)
            return false;
        if (specification != null ? !specification.equals(that.specification) : that.specification != null)
            return false;
        if (state != null ? !state.equals(that.state) : that.state != null) return false;
        if (flowStoreReferences != null ? !flowStoreReferences.equals(that.flowStoreReferences) : that.flowStoreReferences != null)
            return false;
        return workflowNote != null ? workflowNote.equals(that.workflowNote) : that.workflowNote == null;
    }

    @Override
    public int hashCode() {
        int result = jobId;
        result = 31 * result + (eoj ? 1 : 0);
        result = 31 * result + (fatalError ? 1 : 0);
        result = 31 * result + partNumber;
        result = 31 * result + numberOfChunks;
        result = 31 * result + numberOfItems;
        result = 31 * result + (timeOfCreation != null ? timeOfCreation.hashCode() : 0);
        result = 31 * result + (timeOfLastModification != null ? timeOfLastModification.hashCode() : 0);
        result = 31 * result + (timeOfCompletion != null ? timeOfCompletion.hashCode() : 0);
        result = 31 * result + (specification != null ? specification.hashCode() : 0);
        result = 31 * result + (state != null ? state.hashCode() : 0);
        result = 31 * result + (flowStoreReferences != null ? flowStoreReferences.hashCode() : 0);
        result = 31 * result + (workflowNote != null ? workflowNote.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "JobInfoSnapshot{" +
                "jobId=" + jobId +
                ", eoj=" + eoj +
                ", fatalError=" + fatalError +
                ", partNumber=" + partNumber +
                ", numberOfChunks=" + numberOfChunks +
                ", numberOfItems=" + numberOfItems +
                ", timeOfCreation=" + timeOfCreation +
                ", timeOfLastModification=" + timeOfLastModification +
                ", timeOfCompletion=" + timeOfCompletion +
                ", specification=" + specification +
                ", state=" + state +
                ", flowStoreReferences=" + flowStoreReferences +
                ", workflowNote=" + workflowNote +
                '}';
    }
}
