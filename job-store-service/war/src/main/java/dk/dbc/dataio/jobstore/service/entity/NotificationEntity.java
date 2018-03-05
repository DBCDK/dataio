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

package dk.dbc.dataio.jobstore.service.entity;

import dk.dbc.dataio.jobstore.types.JobNotification;
import dk.dbc.dataio.jobstore.types.Notification;
import dk.dbc.dataio.jobstore.types.NotificationContext;
import dk.dbc.dataio.jsonb.JSONBException;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.OneToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import java.sql.Timestamp;
import java.util.Date;

@Entity
@Table(name = "notification")
public class NotificationEntity {
    @Id
    @SequenceGenerator(
            name = "notification_id_seq",
            sequenceName = "notification_id_seq",
            allocationSize = 1)
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "notification_id_seq")
    @Column(updatable = false)
    private Integer id;

    @Column(insertable = false, updatable = false)
    private Timestamp timeOfCreation;

    @Column(insertable = false, updatable = false)
    private Timestamp timeOfLastModification;

    @Convert(converter = NotificationTypeConverter.class)
    private JobNotification.Type type;

    @Convert(converter = NotificationStatusConverter.class)
    private JobNotification.Status status;

    @Lob
    private String statusMessage;

    @Lob
    private String destination;

    @Lob
    private String content;

    @Lob
    private String context;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job")
    private JobEntity job;

    private Integer jobId;

    public NotificationEntity() {}

    public Integer getId() {
        return id;
    }

    public Timestamp getTimeOfCreation() {
        return timeOfCreation;
    }

    public Timestamp getTimeOfLastModification() {
        return timeOfLastModification;
    }

    public JobNotification.Type getType() {
        return type;
    }

    public void setType(JobNotification.Type type) {
        this.type = type;
    }

    public JobNotification.Status getStatus() {
        return status;
    }

    public void setStatus(JobNotification.Status status) {
        this.status = status;
    }

    public String getStatusMessage() {
        return statusMessage;
    }

    public void setStatusMessage(String statusMessage) {
        this.statusMessage = statusMessage;
    }

    public String getDestination() {
        return destination;
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getContext() {
        return context;
    }

    public void setContext(String context) {
        this.context = context;
    }

    public JobEntity getJob() {
        return job;
    }

    public void setJob(JobEntity job) {
        this.job = job;
        this.jobId = job.getId();
    }

    public Integer getJobId() {
        return jobId;
    }

    public JobNotification toJobNotification() {
        return new JobNotification(
                id,
                toDate(timeOfCreation),
                toDate(timeOfLastModification),
                type,
                status,
                statusMessage,
                destination,
                content,
                jobId
        );
    }

    public Notification toNotification() throws JSONBException {
        return new Notification()
                .withId(id)
                .withTimeOfCreation(toDate(timeOfCreation))
                .withTimeOfLastModification(toDate(timeOfLastModification))
                .withType(type.toNotificationType())
                .withStatus(status.toNotificationStatus())
                .withStatusMessage(statusMessage)
                .withDestination(destination)
                .withContent(content)
                .withJobId(getJobId())
                .withContext(toNotificationContext(context));
    }

    /* Package scoped constructor used for unit testing */
    NotificationEntity(Integer id, Date timeOfCreation, Date timeOfLastModification) {
        this.id = id;
        this.timeOfCreation = toTimestamp(timeOfCreation);
        this.timeOfLastModification = toTimestamp(timeOfLastModification);
    }

    private Date toDate(Timestamp timestamp) {
        if (timestamp != null) {
            return new Date(timestamp.getTime());
        }
        return null;
    }

    private Timestamp toTimestamp(Date date) {
        if (date != null) {
            return new Timestamp(date.getTime());
        }
        return null;
    }

    private NotificationContext toNotificationContext(String json) throws JSONBException {
        if (json != null) {
            return ConverterJSONBContext.getInstance().unmarshall(json, NotificationContext.class);
        }
        return null;
    }
}