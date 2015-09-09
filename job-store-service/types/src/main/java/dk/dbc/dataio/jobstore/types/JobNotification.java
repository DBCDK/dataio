package dk.dbc.dataio.jobstore.types;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class JobNotification {
    public enum Type {
        // Do not change these enum short values
        // since they are used as database attribute values
        JOB_CREATED((short) 1),
        JOB_COMPLETED((short) 2);

        private final short value;
        Type(short value) {
            this.value = value;
        }
        public short getValue() {
            return value;
        }

        private static Map<Short, Type> typeMap = new HashMap<>(2);

        static {
            for (Type type : values()) {
                typeMap.put(type.getValue(), type);
            }
        }

        public static Type getTypeFromValue(Short value) {
            return typeMap.get(value);
        }
    }

    public enum Status {
        // Do not change these enum short values
        // since they are used as database attribute values
        WAITING((short) 1),
        COMPLETED((short) 2),
        FAILED((short) 3);

        private final short value;
        Status(short value) {
            this.value = value;
        }
        public short getValue() {
            return value;
        }

        private static Map<Short, Status> statusMap = new HashMap<>(3);

        static {
            for (Status status : values()) {
                statusMap.put(status.getValue(), status);
            }
        }

        public static Status getStatusFromValue(Short value) {
            return statusMap.get(value);
        }
    }

    private final int id;
    private final Date timeOfCreation;
    private final Date timeOfLastModification;
    private final Type type;
    private final Status status;
    private final String statusMessage;
    private final String destination;
    private final String content;
    private final int jobId;

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

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        JobNotification that = (JobNotification) o;

        if (id != that.id) {
            return false;
        }
        if (jobId != that.jobId) {
            return false;
        }
        if (timeOfCreation != null ? !timeOfCreation.equals(that.timeOfCreation) : that.timeOfCreation != null) {
            return false;
        }
        if (timeOfLastModification != null ? !timeOfLastModification.equals(that.timeOfLastModification) : that.timeOfLastModification != null) {
            return false;
        }
        if (type != that.type) {
            return false;
        }
        if (status != that.status) {
            return false;
        }
        if (statusMessage != null ? !statusMessage.equals(that.statusMessage) : that.statusMessage != null) {
            return false;
        }
        if (destination != null ? !destination.equals(that.destination) : that.destination != null) {
            return false;
        }
        return !(content != null ? !content.equals(that.content) : that.content != null);

    }

    @Override
    public int hashCode() {
        int result = id;
        result = 31 * result + (timeOfCreation != null ? timeOfCreation.hashCode() : 0);
        result = 31 * result + (timeOfLastModification != null ? timeOfLastModification.hashCode() : 0);
        result = 31 * result + (type != null ? type.hashCode() : 0);
        result = 31 * result + (status != null ? status.hashCode() : 0);
        result = 31 * result + (statusMessage != null ? statusMessage.hashCode() : 0);
        result = 31 * result + (destination != null ? destination.hashCode() : 0);
        result = 31 * result + (content != null ? content.hashCode() : 0);
        result = 31 * result + jobId;
        return result;
    }

    @Override
    public String toString() {
        return "JobNotification{" +
                "id=" + id +
                ", timeOfCreation=" + timeOfCreation +
                ", timeOfLastModification=" + timeOfLastModification +
                ", type=" + type +
                ", status=" + status +
                ", statusMessage='" + statusMessage + '\'' +
                ", destination='" + destination + '\'' +
                ", content='" + content + '\'' +
                ", jobId=" + jobId +
                '}';
    }
}
