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
