package dk.dbc.dataio.jobstore.types;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Date;

public class JobNotification {
    public enum Type {
        JOB_CREATED,
        JOB_COMPLETED
    }

    public enum Status {
        WAITING,
        COMPLETED,
        FAILED
    }

    private final int id;
    private final Date timeOfCreation;
    private final Date timeOfLastModification;
    private final Type type;
    private final Status status;
    private final String statusMessage;
    private final String destination;
    private final String content;
    private int jobId;

    @JsonCreator
    public JobNotification(@JsonProperty("id") int id,
                           @JsonProperty("timeOfCreation") Date timeOfCreation,
                           @JsonProperty("timeOfLastModification") Date timeOfLastModification,
                           @JsonProperty("type") Type type,
                           @JsonProperty("status") Status status,
                           @JsonProperty("statusMessage") String statusMessage,
                           @JsonProperty("destination") String destination,
                           @JsonProperty("content") String content,
                           @JsonProperty("jobId") int jobId) {

        this.id = id;
        this.timeOfCreation = (timeOfCreation == null) ? null : new Date(timeOfCreation.getTime());
        this.timeOfLastModification = (timeOfLastModification ==  null) ? null : new Date(timeOfLastModification.getTime());
        this.type = type;
        this.status = status;
        this.statusMessage = statusMessage;
        this.destination = destination;
        this.content = content;
        this.jobId = jobId;
    }

    public int getId() {
        return id;
    }

    public Date getTimeOfCreation() {
        return this.timeOfCreation == null? null : new Date(this.timeOfCreation.getTime());
    }

    public Date getTimeOfLastModification() {
        return this.timeOfLastModification == null? null : new Date(this.timeOfLastModification.getTime());
    }

    public Type getType() {
        return type;
    }

    public Status getStatus() {
        return status;
    }

    public String getStatusMessage() {
        return statusMessage;
    }

    public String getDestination() {
        return destination;
    }

    public String getContent() {
        return content;
    }

    public int getJobId() {
        return jobId;
    }
}
