package dk.dbc.dataio.jobstore.test.types;

import dk.dbc.dataio.jobstore.types.JobNotification;

import java.util.Date;

public class JobNotificationBuilder {
    private int id = 1;
    private Date timeOfCreation = new Date();
    private Date timeOfLastModification = new Date();
    private JobNotification.Type type = JobNotification.Type.JOB_CREATED;
    private JobNotification.Status status = JobNotification.Status.WAITING;
    private String statusMessage = "status OK";
    private String destination = "mail@company.com";
    private String content = "mail body";
    private int jobId = 42;

    public JobNotificationBuilder setId(int id) {
        this.id = id;
        return this;
    }

    public JobNotificationBuilder setTimeOfCreation(Date timeOfCreation) {
        this.timeOfCreation = (timeOfCreation == null) ? null : new Date(timeOfCreation.getTime());
        return this;
    }

    public JobNotificationBuilder setTimeOfLastModification(Date timeOfLastModification) {
        this.timeOfLastModification = (timeOfLastModification == null) ? null : new Date(timeOfLastModification.getTime());
        return this;
    }

    public JobNotificationBuilder setType(JobNotification.Type type) {
        this.type = type;
        return this;
    }

    public JobNotificationBuilder setStatus(JobNotification.Status status) {
        this.status = status;
        return this;
    }

    public JobNotificationBuilder setStatusMessage(String statusMessage) {
        this.statusMessage = statusMessage;
        return this;
    }

    public JobNotificationBuilder setDestination(String destination) {
        this.destination = destination;
        return this;
    }

    public JobNotificationBuilder setContent(String content) {
        this.content = content;
        return this;
    }

    public JobNotificationBuilder setJobId(int jobId) {
        this.jobId = jobId;
        return this;
    }

    public JobNotification build() {
        return new JobNotification(id, timeOfCreation, timeOfLastModification, type, status, statusMessage, destination, content, jobId);
    }
}
