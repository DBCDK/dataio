package dk.dbc.dataio.jobstore.types;

import dk.dbc.dataio.commons.types.JobSpecification;

import java.sql.Timestamp;

public class JobInfoSnapshot {

    private int jobIid;

    private boolean eoj;
    private int partNumber;
    private int numberOfChunks;
    private int numberOfItems;

    private Timestamp timeOfCreation;
    private Timestamp timeOfLastModification;
    private Timestamp timeOfCompletion;
    private JobSpecification specification;
    private State state;
    private String flowName;
    private String sinkName;

    public JobInfoSnapshot() {}

    public int getJobId() {
        return jobIid;
    }

    public void setJobId(int jobIid) {
        this.jobIid = jobIid;
    }

    public boolean isEoj() {
        return eoj;
    }

    public void setEoj(boolean eoj) {
        this.eoj = eoj;
    }

    public int getPartNumber() {
        return partNumber;
    }

    public void setPartNumber(int partNumber) {
        this.partNumber = partNumber;
    }

    public int getNumberOfChunks() {
        return numberOfChunks;
    }

    public void setNumberOfChunks(int numberOfChunks) {
        this.numberOfChunks = numberOfChunks;
    }

    public int getNumberOfItems() {
        return numberOfItems;
    }

    public void setNumberOfItems(int numberOfItems) {
        this.numberOfItems = numberOfItems;
    }

    public Timestamp getTimeOfCreation() {
        return timeOfCreation;
    }

    public void setTimeOfCreation(Timestamp timeOfCreation) {
        this.timeOfCreation = timeOfCreation;
    }

    public Timestamp getTimeOfLastModification() {
        return timeOfLastModification;
    }

    public void setTimeOfLastModification(Timestamp timeOfLastModification) {
        this.timeOfLastModification = timeOfLastModification;
    }

    public Timestamp getTimeOfCompletion() {
        return timeOfCompletion;
    }

    public void setTimeOfCompletion(Timestamp timeOfCompletion) {
        this.timeOfCompletion = timeOfCompletion;
    }

    public JobSpecification getSpecification() {
        return specification;
    }

    public void setSpecification(JobSpecification specification) {
        this.specification = specification;
    }

    public State getState() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
    }

    public String getFlowName() {
        return flowName;
    }

    public void setFlowName(String flowName) {
        this.flowName = flowName;
    }

    public String getSinkName() {
        return sinkName;
    }

    public void setSinkName(String sinkName) {
        this.sinkName = sinkName;
    }
}
