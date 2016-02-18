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
import dk.dbc.dataio.jobstore.types.JobStoreException;
import dk.dbc.dataio.jsonb.JSONBContext;
import dk.dbc.dataio.jsonb.JSONBException;

import javax.activation.DataHandler;
import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Part;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.util.ByteArrayDataSource;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Optional;

import static dk.dbc.dataio.commons.types.Constants.MISSING_FIELD_VALUE;
import static dk.dbc.dataio.jobstore.service.util.JobInfoSnapshotConverter.toJobInfoSnapshot;

/**
 * Class wrapping a NotificationEntity instance as an email notification
 */
public class MailNotification {
    private static final String JOB_CREATED_OK_TEMPLATE = "/notifications/job_created_ok.template";
    private static final String JOB_CREATED_FAIL_TEMPLATE = "/notifications/job_created_fail.template";
    private static final String JOB_COMPLETED_TEMPLATE = "/notifications/job_completed.template";
    private static final String JOB_COMPLETED_WITH_FAILURES_TEMPLATE = "/notifications/job_completed_with_failures.template";
    private static final String INCOMPLETE_TRANSFILE_TEMPLATE = "/notifications/incomplete_transfile.template";
    private static final String SUBJECT_FOR_JOB_CREATED = "DANBIB:postmester";
    private static final String SUBJECT_FOR_JOB_COMPLETED = "DANBIB:baseindlaeg";
    private static final String FROM_ADDRESS_PERSONAL_NAME = "DANBIB Fællesbruger";

    private final Session mailSession;
    private final NotificationEntity notification;
    private final StringBuilder builder;
    private Attachment attachment;

    public MailNotification(Session mailSession, NotificationEntity notification) throws JobStoreException {
        this.mailSession = mailSession;
        this.notification = notification;
        if (isUndefined(notification.getContent())) {
            format();
        }
        builder = new StringBuilder(notification.getContent());
    }

    /**
     * Formats and sends this email notification
     *
     * @throws JobStoreException in case of error during formatting or sending
     */
    public void send() throws JobStoreException {
        try {
            final String destination = notification.getDestination();
            if (isUndefined(destination)) {
                setDestination();
            }
            final InternetAddress fromAddress = new InternetAddress(mailSession.getProperty("mail.from"), FROM_ADDRESS_PERSONAL_NAME);
            final InternetAddress[] toAddresses = {new InternetAddress(notification.getDestination())};

            // sends the e-mail
            Transport.send(buildMimeMessage(fromAddress, toAddresses));
        } catch (Exception e) {
            throw new JobStoreException("Unable to send notification", e);
        }
    }

    public void append(byte[] addenda) {
        builder.append(new String(addenda, StandardCharsets.UTF_8));
    }

    public void attach(Attachment attachment) {
        this.attachment = attachment;
    }

    private void setDestination() {
        final String destination = inferDestinationFromType().orElse(MISSING_FIELD_VALUE);
        if (destination.equals(MISSING_FIELD_VALUE)) {
            notification.setDestination(mailSession.getProperty("mail.to.fallback"));
            notification.setStatusMessage("Destination fallback used");
        } else {
            notification.setDestination(destination);
        }
    }

    private Optional<String> inferDestinationFromType() {
        switch (notification.getType()) {
            case JOB_CREATED:   return getDestinationForJobCreatedNotification(notification.getJob().getSpecification());
            case JOB_COMPLETED: return getDestinationForJobCompletedNotification(notification.getJob().getSpecification());
            default: return Optional.empty();
        }
    }

    private Optional<String> getDestinationForJobCreatedNotification(JobSpecification jobSpecification) {
        final String destination = jobSpecification.getMailForNotificationAboutVerification();
        if (destination.trim().isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(destination);
    }

    private Optional<String> getDestinationForJobCompletedNotification(JobSpecification jobSpecification) {
        final String destination = jobSpecification.getMailForNotificationAboutProcessing();
        if (destination.trim().isEmpty() || destination.equals(MISSING_FIELD_VALUE)) {
            // fall back to primary destination
            return getDestinationForJobCreatedNotification(jobSpecification);
        }
        return Optional.of(destination);
    }

    private boolean isUndefined(String value) {
        return value == null || value.trim().isEmpty() || value.equals(MISSING_FIELD_VALUE);
    }

    private void format() throws JobStoreException {
        final JSONBContext jsonbContext = new JSONBContext();
        final JsonValueTemplateEngine templateEngine = new JsonValueTemplateEngine(jsonbContext);
        try {
            switch (notification.getType()) {
                case JOB_CREATED:
                case JOB_COMPLETED:
                    notification.setContent(templateEngine.apply(getNotificationTemplate(),
                            jsonbContext.marshall(toJobInfoSnapshot(notification.getJob()))));
                    break;
                default:
                    notification.setContent(templateEngine.apply(getNotificationTemplate(),
                            notification.getContext()));
            }
        } catch (JSONBException e) {
            throw new JobStoreException("Unable to marshall linked job", e);
        }
    }

    private String getNotificationTemplate() {
        final String resource;
        switch (notification.getType()) {
            case INCOMPLETE_TRANSFILE:
                resource = INCOMPLETE_TRANSFILE_TEMPLATE;
                break;
            case JOB_COMPLETED:
                if (notification.getJob().hasFailedItems()) {
                    resource = JOB_COMPLETED_WITH_FAILURES_TEMPLATE;
                } else {
                    resource = JOB_COMPLETED_TEMPLATE;
                }
                break;
            default:
                if (notification.getJob().hasFatalDiagnostics()) {
                    resource = JOB_CREATED_FAIL_TEMPLATE;
                } else {
                    resource = JOB_CREATED_OK_TEMPLATE;
                }
                break;
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

    public MimeMessage buildMimeMessage(InternetAddress fromAddress, InternetAddress[] toAddresses) throws MessagingException, IOException {
        final MimeMessage message = new MimeMessage(mailSession);
        message.setFrom(fromAddress);
        message.setRecipients(Message.RecipientType.TO, toAddresses);
        message.setSentDate(new Date());

        switch (notification.getType()) {
            case JOB_COMPLETED:
                message.setSubject(SUBJECT_FOR_JOB_COMPLETED);
                break;
            default:
                message.setSubject(SUBJECT_FOR_JOB_CREATED);
                break;
        }

        final DataHandler messageDataHandler = new DataHandler(builder.toString(), "text/plain; charset=UTF-8");
        if (attachment != null) {
            message.setContent(buildMimeMultipart(messageDataHandler));
        } else {
            message.setDataHandler(messageDataHandler);
        }
        message.saveChanges();
        return message;
    }

    private Multipart buildMimeMultipart(DataHandler messageDataHandler) throws MessagingException {

        // Creating body part for mail content
        final BodyPart mailContent = new MimeBodyPart();
        mailContent.setDataHandler(messageDataHandler);

        // Creating body part for mail attachment
        final MimeBodyPart mailAttachment = new MimeBodyPart();
        final ByteArrayDataSource attachmentDataSource = new ByteArrayDataSource(attachment.getContent(), attachment.getContentType());
        mailAttachment.setDataHandler(new DataHandler(attachmentDataSource));
        mailAttachment.setDisposition(Part.ATTACHMENT);
        mailAttachment.setFileName(attachment.getFileName());

        // Creating MimeMultipart
        final MimeMultipart multipart = new MimeMultipart();
        multipart.addBodyPart(mailContent);
        multipart.addBodyPart(mailAttachment);
        return multipart;
    }
}