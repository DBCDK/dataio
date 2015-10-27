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

package dk.dbc.dataio.jobstore.service.util;

import dk.dbc.dataio.commons.types.JobSpecification;
import dk.dbc.dataio.jobstore.service.entity.NotificationEntity;
import dk.dbc.dataio.jobstore.types.JobNotification;
import dk.dbc.dataio.jobstore.types.JobStoreException;
import dk.dbc.dataio.jsonb.JSONBContext;
import dk.dbc.dataio.jsonb.JSONBException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Date;

import static dk.dbc.dataio.commons.types.Constants.MISSING_FIELD_VALUE;
import static dk.dbc.dataio.jobstore.service.util.JobInfoSnapshotConverter.toJobInfoSnapshot;

/**
 * Class wrapping a NotificationEntity instance as an email notification
 */
public class MailNotification {
    private static final Logger LOGGER = LoggerFactory.getLogger(MailNotification.class);
    private static final String JOB_CREATED_OK_TEMPLATE = "/notifications/job_created_ok.template";
    private static final String JOB_CREATED_FAIL_TEMPLATE = "/notifications/job_created_fail.template";
    private static final String JOB_COMPLETED_TEMPLATE = "/notifications/job_completed.template";

    private final Session mailSession;
    private final NotificationEntity notification;

    public MailNotification(Session mailSession, NotificationEntity notification) {
        this.mailSession = mailSession;
        this.notification = notification;
    }

    /**
     * Formats and sends this email notification
     * @throws JobStoreException in case of error during formatting or sending
     */
    public void send() throws JobStoreException {
        try {
            final String destination = notification.getDestination();
            if (destination == null || destination.isEmpty() || destination.equals(MISSING_FIELD_VALUE)) {
                setDestination();
            }
            if (notification.getContent() == null || notification.getContent().isEmpty()) {
                format();
            }
            final InternetAddress fromAddress = new InternetAddress(mailSession.getProperty("mail.from"));
            final InternetAddress[] toAddresses = {new InternetAddress(notification.getDestination())};
            Transport.send(buildMimeMessage(fromAddress, toAddresses));
        } catch (Exception e) {
            throw new JobStoreException("Unable to send notification", e);
        }
    }

    private void setDestination() {
        final String destination = inferDestinationFromType();
        if (destination.equals(MISSING_FIELD_VALUE)) {
            notification.setDestination(mailSession.getProperty("mail.to.fallback"));
            notification.setStatusMessage("Destination fallback used");
        } else {
            notification.setDestination(destination);
        }
    }

    private String inferDestinationFromType() {
        final JobNotification.Type type = notification.getType();
        switch (notification.getType()) {
            case JOB_CREATED:   return getDestinationForJobCreatedNotification(notification.getJob().getSpecification());
            case JOB_COMPLETED: return getDestinationForJobCompletedNotification(notification.getJob().getSpecification());
            default:
                LOGGER.error("Unhandled notification type {}", type);
                return MISSING_FIELD_VALUE;
        }
    }

    private String getDestinationForJobCreatedNotification(JobSpecification jobSpecification) {
        final String destination = jobSpecification.getMailForNotificationAboutVerification();
        if (destination.trim().isEmpty()) {
            return MISSING_FIELD_VALUE;
        }
        return destination;
    }

     private String getDestinationForJobCompletedNotification(JobSpecification jobSpecification) {
        final String destination = jobSpecification.getMailForNotificationAboutProcessing();
        if (destination.trim().isEmpty() || destination.equals(MISSING_FIELD_VALUE)) {
            // fall back to primary destination
            return getDestinationForJobCreatedNotification(jobSpecification);
        }
        return destination;
    }

    private void format() throws JobStoreException {
        final JSONBContext jsonbContext = new JSONBContext();
        final JsonValueTemplateEngine templateEngine = new JsonValueTemplateEngine(jsonbContext);
        try {
            notification.setContent(templateEngine.apply(getNotificationTemplate(),
                    jsonbContext.marshall(toJobInfoSnapshot(notification.getJob()))));
        } catch (JSONBException e) {
            throw new JobStoreException("Unable to marshall linked job", e);
        }
    }

    private String getNotificationTemplate() {
        String resource;
        if (notification.getType() == JobNotification.Type.JOB_COMPLETED) {
            resource = JOB_COMPLETED_TEMPLATE;
        } else {
            if (notification.getJob().getState().fatalDiagnosticExists()) {
                resource = JOB_CREATED_FAIL_TEMPLATE;
            } else {
                resource = JOB_CREATED_OK_TEMPLATE;
            }
        }
        return getNotificationTemplateResource(resource);
    }

    private String getNotificationTemplateResource(String resource) {
        final StringBuilder buffer = new StringBuilder();
        try (
                final InputStreamReader isr = new InputStreamReader(getClass().getResourceAsStream(resource), StandardCharsets.UTF_8);
                final BufferedReader br = new BufferedReader(isr)) {
            for (int c = br.read(); c != -1; c = br.read())
                buffer.append((char) c);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        return buffer.toString();
    }

    private MimeMessage buildMimeMessage(InternetAddress fromAddress, InternetAddress[] toAddresses) throws MessagingException {
        final MimeMessage message = new MimeMessage(mailSession);
        message.setFrom(fromAddress);
        message.setRecipients(Message.RecipientType.TO, toAddresses);
        message.setSubject("DBC dataIO notification");
        message.setSentDate(new Date());
        message.setText(notification.getContent(), StandardCharsets.UTF_8.name());
        return message;
    }
}
