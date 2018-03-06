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

package dk.dbc.dataio.jobstore.types;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * This class is deprecated use {@link Notification} instead
 */
public class JobNotification implements Serializable {
    public JobNotification() {
        this(1, null, null, Type.JOB_CREATED, Status.WAITING, "", "", "", 1);
    }

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
        public Notification.Type toNotificationType() {
            switch (this) {
                case JOB_CREATED: return Notification.Type.JOB_CREATED;
                case JOB_COMPLETED: return Notification.Type.JOB_COMPLETED;
                case INVALID_TRANSFILE: return Notification.Type.INVALID_TRANSFILE;
                default: throw new IllegalStateException("Unknown type " + this);
            }
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
        public Notification.Status toNotificationStatus() {
            switch (this) {
                case WAITING: return Notification.Status.WAITING;
                case COMPLETED: return Notification.Status.COMPLETED;
                case FAILED: return Notification.Status.FAILED;
                default: throw new IllegalStateException("Unknown status " + this);
            }
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

    private final Integer id;
    private final Date timeOfCreation;
    private final Date timeOfLastModification;
    private final Type type;
    private final Status status;
    private final String statusMessage;
    private final String destination;
    private final String content;
    private final Integer jobId;

    @JsonCreator
    public JobNotification(@JsonProperty("id") Integer id,
                           @JsonProperty("timeOfCreation") Date timeOfCreation,
                           @JsonProperty("timeOfLastModification") Date timeOfLastModification,
                           @JsonProperty("type") Type type,
                           @JsonProperty("status") Status status,
                           @JsonProperty("statusMessage") String statusMessage,
                           @JsonProperty("destination") String destination,
                           @JsonProperty("content") String content,
                           @JsonProperty("jobId") Integer jobId) {

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

    public Integer getId() {
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

    public Integer getJobId() {
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
