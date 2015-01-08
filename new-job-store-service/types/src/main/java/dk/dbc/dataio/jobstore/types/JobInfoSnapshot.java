package dk.dbc.dataio.jobstore.types;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import dk.dbc.dataio.commons.types.JobSpecification;

import java.util.Date;

public class JobInfoSnapshot {

    private int jobId;
    private boolean eoj;
    private int partNumber;
    private int numberOfChunks;
    private int numberOfItems;
    private Date timeOfCreation;
    private Date timeOfLastModification ;
    private Date timeOfCompletion;
    private JobSpecification specification;
    private State state;
    private String flowName;
    private String sinkName;

    @JsonCreator
    public JobInfoSnapshot(@JsonProperty ("jobId")int jobId,
                           @JsonProperty ("eoj")boolean eoj,
                           @JsonProperty ("partNumber")int partNumber,
                           @JsonProperty ("numberOfChunks")int numberOfChunks,
                           @JsonProperty ("numberOfItems")int numberOfItems,
                           @JsonProperty ("timeOfCreation")Date timeOfCreation,
                           @JsonProperty ("timeOfLastModification")Date timeOfLastModification,
                           @JsonProperty ("timeOfCompletion")Date timeOfCompletion,
                           @JsonProperty ("specification") JobSpecification specification,
                           @JsonProperty ("state")State state,
                           @JsonProperty ("flowName")String flowName,
                           @JsonProperty ("sinkName")String sinkName) {

        this.jobId = jobId;
        this.eoj = eoj;
        this.partNumber = partNumber;
        this.numberOfChunks = numberOfChunks;
        this.numberOfItems = numberOfItems;
        this.timeOfCreation = (timeOfCreation == null) ? null : new Date(timeOfCreation.getTime());
        this.timeOfLastModification = (timeOfLastModification ==  null) ? null : new Date(timeOfLastModification.getTime());
        this.timeOfCompletion = (timeOfCompletion == null) ? null : new Date(timeOfCompletion.getTime());
        this.specification = specification;
        this.state = state;
        this.flowName = flowName;
        this.sinkName = sinkName;
    }

    public int getJobId() {
        return jobId;
    }

    public boolean isEoj() {
        return eoj;
    }

    public int getPartNumber() {
        return partNumber;
    }

    public int getNumberOfChunks() {
        return numberOfChunks;
    }

    public int getNumberOfItems() {
        return numberOfItems;
    }

    public Date getTimeOfCreation() {
        return this.timeOfCreation == null? null : new Date(this.timeOfCreation.getTime());
    }

    public Date getTimeOfLastModification() {
        return this.timeOfLastModification == null? null : new Date(this.timeOfLastModification.getTime());
    }

    public Date getTimeOfCompletion() {
        return this.timeOfCompletion == null? null : new Date(this.timeOfCompletion.getTime());
    }

    public JobSpecification getSpecification() {
        return specification;
    }

    public State getState() {
        return state;
    }

    public String getFlowName() {
        return flowName;
    }

    public String getSinkName() {
        return sinkName;
    }
}
