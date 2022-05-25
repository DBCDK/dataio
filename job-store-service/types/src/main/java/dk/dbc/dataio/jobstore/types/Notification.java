package dk.dbc.dataio.jobstore.types;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class Notification implements Serializable {
    public enum Type {
        // Do not change these enum short values
        // since they are used as database attribute values
        JOB_CREATED((short) 1),
        JOB_COMPLETED((short) 2),
        INVALID_TRANSFILE((short) 4);

        private final short value;

        Type(short value) {
            this.value = value;
        }

        public short getValue() {
            return value;
        }

        private static Map<Short, Type> typeMap = new HashMap<>(3);

        static {
            for (Type type : values()) {
                typeMap.put(type.getValue(), type);
            }
        }

        public static Type of(Short value) {
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

        public static Status of(Short value) {
            return statusMap.get(value);
        }
    }

    private Integer id;
    private Date timeOfCreation;
    private Date timeOfLastModification;
    private Type type;
    private Status status;
    private String statusMessage;
    private String destination;
    private String content;
    private Integer jobId;
    private NotificationContext context;

    public Notification() {
    }  // GWT demands this empty constructor - therefore: Do not delete it, though nobody uses it :)

    public Integer getId() {
        return id;
    }

    public Notification withId(Integer id) {
        this.id = id;
        return this;
    }

    public Date getTimeOfCreation() {
        return this.timeOfCreation == null ? null
                : new Date(this.timeOfCreation.getTime());
    }

    public Notification withTimeOfCreation(Date timeOfCreation) {
        if (timeOfCreation != null) {
            this.timeOfCreation = new Date(timeOfCreation.getTime());
        } else {
            this.timeOfCreation = null;
        }
        return this;
    }

    public Date getTimeOfLastModification() {
        return this.timeOfLastModification == null ? null
                : new Date(this.timeOfLastModification.getTime());
    }

    public Notification withTimeOfLastModification(Date timeOfLastModification) {
        if (timeOfLastModification != null) {
            this.timeOfLastModification = new Date(timeOfLastModification.getTime());
        } else {
            this.timeOfLastModification = null;
        }
        return this;
    }

    public Type getType() {
        return type;
    }

    public Notification withType(Type type) {
        this.type = type;
        return this;
    }

    public Status getStatus() {
        return status;
    }

    public Notification withStatus(Status status) {
        this.status = status;
        return this;
    }

    public String getStatusMessage() {
        return statusMessage;
    }

    public Notification withStatusMessage(String statusMessage) {
        this.statusMessage = statusMessage;
        return this;
    }

    public String getDestination() {
        return destination;
    }

    public Notification withDestination(String destination) {
        this.destination = destination;
        return this;
    }

    public String getContent() {
        return content;
    }

    public Notification withContent(String content) {
        this.content = content;
        return this;
    }

    public Integer getJobId() {
        return jobId;
    }

    public Notification withJobId(Integer jobId) {
        this.jobId = jobId;
        return this;
    }

    public NotificationContext getContext() {
        return context;
    }

    public Notification withContext(NotificationContext context) {
        this.context = context;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Notification that = (Notification) o;
        return Objects.equals(id, that.id) &&
                Objects.equals(timeOfCreation, that.timeOfCreation) &&
                Objects.equals(timeOfLastModification, that.timeOfLastModification) &&
                type == that.type &&
                status == that.status &&
                Objects.equals(statusMessage, that.statusMessage) &&
                Objects.equals(destination, that.destination) &&
                Objects.equals(content, that.content) &&
                Objects.equals(jobId, that.jobId) &&
                Objects.equals(context, that.context);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, timeOfCreation, timeOfLastModification, type,
                status, statusMessage, destination, content, jobId, context);
    }

    @Override
    public String toString() {
        return "Notification{" +
                "id=" + id +
                ", timeOfCreation=" + timeOfCreation +
                ", timeOfLastModification=" + timeOfLastModification +
                ", type=" + type +
                ", status=" + status +
                ", statusMessage='" + statusMessage + '\'' +
                ", destination='" + destination + '\'' +
                ", content='" + content + '\'' +
                ", jobId=" + jobId +
                ", context=" + context +
                '}';
    }
}
