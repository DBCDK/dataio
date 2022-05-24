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

import dk.dbc.commons.jsonb.JSONBContext;
import dk.dbc.commons.jsonb.JSONBException;
import dk.dbc.dataio.commons.types.JobSpecification;
import dk.dbc.dataio.commons.utils.lang.StringUtil;
import dk.dbc.dataio.commons.utils.test.model.DiagnosticBuilder;
import dk.dbc.dataio.jobstore.service.entity.JobEntity;
import dk.dbc.dataio.jobstore.service.entity.NotificationEntity;
import dk.dbc.dataio.jobstore.types.InvalidTransfileNotificationContext;
import dk.dbc.dataio.jobstore.types.JobStoreException;
import dk.dbc.dataio.jobstore.types.Notification;
import dk.dbc.dataio.jobstore.types.State;
import dk.dbc.dataio.jobstore.types.StateChange;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.jvnet.mock_javamail.Mailbox;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Part;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Properties;

import static dk.dbc.dataio.jobstore.service.ejb.JobNotificationRepositoryTest.getNotificationEntity;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;

public class MailNotificationTest {
    private static final String JOB_CREATED_OK_BODY = "/notifications/job_created_ok.body";
    private static final String JOB_CREATED_FAIL_BODY = "/notifications/job_created_fail.body";
    private static final String JOB_COMPLETED_BODY = "/notifications/job_completed.body";
    private static final String JOB_COMPLETED_WITH_FAILURES_BODY = "/notifications/job_completed_with_failures.body";
    private static final String JOB_COMPLETED_WITH_FAILURES_APPENDED_BODY = "/notifications/job_completed_with_failures_appended.body";
    private static final String JOB_COMPLETED_WITH_FAILURES_APPENDED_AND_ID_OVERWRITE_BODY = "/notifications/job_completed_with_failures_appended_and_id_overwrite.body";
    private static final String INVALID_TRANSFILE_BODY = "/notifications/invalid_transfile.body";
    private static final String JOB_CREATED_SUBJECT = "DANBIB:postmester";
    private static final String JOB_COMPLETED_SUBJECT = "DANBIB:baseindlaeg";
    private final String destination = "mail@example.com";
    private final String mailFrom = "dataio@dbc.dk";
    private final String mailFromName = "DANBIB Fællesbruger";
    private final byte[] bytes = "Appended data".getBytes();

    @Before
    public void clearMailBoxes() {
        Mailbox.clearAll();
    }

    @Test
    public void send_setsNotificationDestination() throws JobStoreException {
        final NotificationEntity notification = getNotificationEntity(Notification.Type.JOB_CREATED, getJobEntity());
        notification.setDestination(null);

        final MailNotification mailNotification = getMailNotification(notification);
        mailNotification.send();

        assertThat(notification.getDestination(), is(destination));
    }

    @Ignore("Can't figure why this fails on is.dbc.dk")
    @Test
    public void send_setsFromAddress() throws JobStoreException, MessagingException, IOException {
        final NotificationEntity notification = getNotificationEntity(Notification.Type.JOB_CREATED, getJobEntity());

        final MailNotification mailNotification = getMailNotification(notification);
        mailNotification.send();

        final List<Message> inbox = Mailbox.get(destination);
        assertThat("Number of notifications for destination", inbox.size(), is(1));
        final Message message = inbox.get(0);
        assertThat("Notification from address", message.getFrom(), is(new InternetAddress[]{new InternetAddress(mailFrom, mailFromName)}));
    }

    @Test
    public void send_transportLayerFails_throws() throws JobStoreException {
        final JobSpecification jobSpecification = new JobSpecification()
                .withMailForNotificationAboutVerification("verification@company.com");
        final NotificationEntity notification = getNotificationEntity(Notification.Type.JOB_CREATED, jobSpecification);

        final MailDestination mailDestination = new MailDestination(null, notification, null);
        final MailNotification mailNotification = new MailNotification(mailDestination, notification);
        try {
            mailNotification.send();
            fail("No JobStoreException thrown");
        } catch (JobStoreException e) {
        }
    }

    @Test
    public void send_appliesIncompleteTransfileTemplate() throws JobStoreException, MessagingException, IOException, JSONBException {
        final JSONBContext jsonbContext = new JSONBContext();
        final InvalidTransfileNotificationContext context = new InvalidTransfileNotificationContext("file.trans", "content", "Trans fil mangler slut markering");
        final NotificationEntity notification = getNotificationEntity(Notification.Type.INVALID_TRANSFILE, getJobEntity());
        notification.setDestination(destination);
        notification.setContext(jsonbContext.marshall(context));

        final MailNotification mailNotification = getMailNotification(notification);
        mailNotification.send();

        final List<Message> inbox = Mailbox.get(destination);
        assertThat("Number of notifications for destination", inbox.size(), is(1));
        final String content = (String) inbox.get(0).getContent();
        assertThat("Notification content", content, is(getResourceContent(INVALID_TRANSFILE_BODY)));
    }

    @Test
    public void send_appliesJobCreatedOkTemplate() throws JobStoreException, MessagingException, IOException {
        final NotificationEntity notification = getNotificationEntity(Notification.Type.JOB_CREATED, getJobEntity());

        final MailNotification mailNotification = getMailNotification(notification);
        mailNotification.send();

        final List<Message> inbox = Mailbox.get(destination);
        assertThat("Number of notifications for destination", inbox.size(), is(1));
        final Message message = inbox.get(0);
        assertThat("Notification subject", message.getSubject(), is(JOB_CREATED_SUBJECT));
        assertThat("Notification content", message.getContent(), is(getResourceContent(JOB_CREATED_OK_BODY)));
    }

    @Test
    public void send_appliesJobCreatedFailTemplate() throws JobStoreException, MessagingException, IOException {
        final NotificationEntity notification = getNotificationEntity(Notification.Type.JOB_CREATED, getJobEntity());
        notification.getJob().getState().getDiagnostics().add(new DiagnosticBuilder().setMessage("Job dannelse fejlet").build());

        final MailNotification mailNotification = getMailNotification(notification);
        mailNotification.send();

        final List<Message> inbox = Mailbox.get(destination);
        assertThat("Number of notifications for destination", inbox.size(), is(1));
        final Message message = inbox.get(0);
        assertThat("Notification subject", message.getSubject(), is(JOB_CREATED_SUBJECT));
        assertThat("Notification content", message.getContent(), is(getResourceContent(JOB_CREATED_FAIL_BODY)));
    }

    @Test
    public void send_appliesJobCompletedTemplate() throws JobStoreException, MessagingException, IOException {
        final NotificationEntity notification = getNotificationEntity(Notification.Type.JOB_COMPLETED, getJobEntity());
        updateStateForJobCompletedBody(notification.getJob().getState());

        final MailNotification mailNotification = getMailNotification(notification);
        mailNotification.send();

        final List<Message> inbox = Mailbox.get(destination);
        assertThat("Number of notifications for destination", inbox.size(), is(1));
        final Message message = inbox.get(0);
        assertThat("Notification subject", message.getSubject(), is(JOB_COMPLETED_SUBJECT));
        assertThat("Notification content", message.getContent(), is(getResourceContent(JOB_COMPLETED_BODY)));
    }

    @Test
    public void send_appliesJobCompletedWithFailuresTemplate() throws JobStoreException, MessagingException, IOException {
        final NotificationEntity notification = getNotificationEntity(Notification.Type.JOB_COMPLETED, getJobEntity());
        updateStateForJobCompletedWithFailures(notification.getJob().getState());

        final MailNotification mailNotification = getMailNotification(notification);
        mailNotification.send();

        final List<Message> inbox = Mailbox.get(destination);
        assertThat("Number of notifications for destination", inbox.size(), is(1));
        final Message message = inbox.get(0);
        assertThat("Notification subject", message.getSubject(), is(JOB_COMPLETED_SUBJECT));
        assertThat("Notification content", message.getContent(), is(getResourceContent(JOB_COMPLETED_WITH_FAILURES_BODY)));
    }

    @Test
    public void send_appliesJobCompletedWithFailuresAppendedTemplate() throws JobStoreException, MessagingException, IOException {
        final NotificationEntity notification = getNotificationEntity(Notification.Type.JOB_COMPLETED, getJobEntity());
        updateStateForJobCompletedWithFailures(notification.getJob().getState());

        final MailNotification mailNotification = getMailNotification(notification);
        mailNotification.append(bytes);
        mailNotification.send();

        final List<Message> inbox = Mailbox.get(destination);
        assertThat("Number of notifications for destination", inbox.size(), is(1));
        final Message message = inbox.get(0);
        assertThat("Notification subject", message.getSubject(), is(JOB_COMPLETED_SUBJECT));
        assertThat("Notification content", message.getContent(), is(getResourceContent(JOB_COMPLETED_WITH_FAILURES_APPENDED_BODY)));
    }

    @Test
    public void send_appliesJobCompletedWithFailuresAppendedTemplateAndJobIdOverwrite() throws JobStoreException, MessagingException, IOException {
        final NotificationEntity notification = getNotificationEntity(Notification.Type.JOB_COMPLETED, getJobEntity(123456));
        updateStateForJobCompletedWithFailures(notification.getJob().getState());

        final MailNotification mailNotification = getMailNotification(notification);
        mailNotification.append(bytes);
        mailNotification.send();

        final List<Message> inbox = Mailbox.get(destination);
        assertThat("Number of notifications for destination", inbox.size(), is(1));
        final Message message = inbox.get(0);
        assertThat("Notification subject", message.getSubject(), is(JOB_COMPLETED_SUBJECT));
        assertThat("Notification content", message.getContent(), is(getResourceContent(JOB_COMPLETED_WITH_FAILURES_APPENDED_AND_ID_OVERWRITE_BODY)));
    }

    @Test
    public void send_appliesJobCompletedWithFailuresAppendedTemplateAndUnreadableLineFormatAttachment() throws JobStoreException, IOException, MessagingException {
        final NotificationEntity notification = getNotificationEntity(Notification.Type.JOB_COMPLETED, getJobEntity());
        updateStateForJobCompletedWithFailures(notification.getJob().getState());

        final MailNotification mailNotification = getMailNotification(notification);
        final Attachment attachment = new Attachment("**This is an unreadable record".getBytes(), "test.lin", StandardCharsets.UTF_8);
        mailNotification.attach(attachment);
        mailNotification.append(bytes);
        mailNotification.send();

        // Assert that one mail has been received
        final List<Message> inbox = Mailbox.get(destination);
        assertThat("Number of notifications for destination", inbox.size(), is(1));

        // Assert that the mail consists of 2 parts: Content and attachment
        Multipart multipart = (Multipart)inbox.get(0).getContent();
        assertThat("Number of parts which the mail consist of", multipart.getCount(), is(2));

        // Assert that first part contains the expected resource content
        final Part mailContent = multipart.getBodyPart(0);
        assertThat("Notification content", mailContent.getContent(), is(getResourceContent(JOB_COMPLETED_WITH_FAILURES_APPENDED_BODY)));

        // Assert that second part contains the expected attachment
        assertNotificationAttachment(attachment, multipart.getBodyPart(1));
    }

    @Test
    public void send_appliesJobCompletedWithFailuresTemplateUnreadableIsoAttachment() throws JobStoreException, MessagingException, IOException {
        final NotificationEntity notification = getNotificationEntity(Notification.Type.JOB_COMPLETED, getJobEntity());
        updateStateForJobCompletedWithFailures(notification.getJob().getState());

        final MailNotification mailNotification = getMailNotification(notification);
        final Attachment attachment = new Attachment(readTestRecord("/broken-iso2709-2.iso"), "test.iso2709", StandardCharsets.ISO_8859_1);
        mailNotification.attach(attachment);
        mailNotification.send();

        // Assert that one mail has been received
        final List<Message> inbox = Mailbox.get(destination);
        assertThat("Number of notifications for destination", inbox.size(), is(1));

        // Assert that the mail consists of 2 parts: Content and attachment
        Multipart multipart = (Multipart)inbox.get(0).getContent();
        assertThat("Number of parts which the mail consist of", multipart.getCount(), is(2));

        // Assert that first part contains the expected resource content
        final Part mailContentBodyPart = multipart.getBodyPart(0);
        assertThat("Notification content", mailContentBodyPart.getContent(), is(getResourceContent(JOB_COMPLETED_WITH_FAILURES_BODY)));

        // Assert that second part contains the expected attachment
        assertNotificationAttachment(attachment, multipart.getBodyPart(1));
    }

    private void assertNotificationAttachment(Attachment attachment, Part mailAttachment) throws IOException, MessagingException {
        assertThat("Notification attachment disposition", mailAttachment.getDisposition(), is(Part.ATTACHMENT));
        assertThat("Notification attachment contentType", mailAttachment.getContentType().contains(attachment.getContentType()), is(true));
        assertThat("Notification attachment filename", mailAttachment.getFileName(), is(attachment.getFileName()));

        final Writer writer = new StringWriter();
        IOUtils.copy(mailAttachment.getInputStream(), writer);
        assertThat("Notification attachment content", writer.toString(), is(StringUtil.asString(attachment.getContent())));
    }

    private void updateStateForJobCompletedBody(State state) {
        final StateChange stateChange = new StateChange();
        stateChange.setPhase(State.Phase.PARTITIONING);
        stateChange.setPhase(State.Phase.DELIVERING);
        stateChange.setSucceeded(96);
        state.updateState(stateChange);
    }

    private void updateStateForJobCompletedWithFailures(State state) {
        final StateChange stateChange = new StateChange();
        stateChange.setPhase(State.Phase.PARTITIONING);
        stateChange.setFailed(1);
        state.updateState(stateChange);
        stateChange.setPhase(State.Phase.PROCESSING);
        stateChange.setFailed(2);
        state.updateState(stateChange);
        stateChange.setPhase(State.Phase.DELIVERING);
        stateChange.setFailed(3);
        stateChange.setIgnored(3);
        stateChange.setSucceeded(94);
        state.updateState(stateChange);
    }

    private MailNotification getMailNotification(NotificationEntity notification) throws JobStoreException {
        final Properties mailSessionProperties = new Properties();
        mailSessionProperties.setProperty("mail.from", mailFrom);
        final MailDestination mailDestination = new MailDestination(Session.getDefaultInstance(mailSessionProperties), notification, null);
        return new MailNotification(mailDestination, notification);
    }

    private JobEntity getJobEntity() {
        return getJobEntity(0);
    }

    private JobEntity getJobEntity(int previousJobId) {
        final JobSpecification.Ancestry ancestry = new JobSpecification.Ancestry()
                .withTransfile("file.trans")
                .withDatafile("file.dat")
                .withBatchId("batch001")
                .withDetails("details".getBytes())
                .withPreviousJobId(previousJobId);
        final JobSpecification jobSpecification = new JobSpecification()
                .withSubmitterId(424242)
                .withAncestry(ancestry)
                .withMailForNotificationAboutVerification(destination)
                .withResultmailInitials("TEST");
        final JobEntity jobEntity = new JobEntity();
        jobEntity.setSpecification(jobSpecification);
        jobEntity.setState(new State());
        jobEntity.setNumberOfItems(100);
        return jobEntity;
    }

    private String getResourceContent(String resourceName) {
        try {
            return new String(Files.readAllBytes(Paths.get(getClass().getResource(resourceName).toURI())),
                    StandardCharsets.UTF_8);
        } catch (IOException | URISyntaxException e) {
            throw new IllegalStateException(e);
        }
    }

    protected byte[] readTestRecord(String resourceName) {
        try {
            final URL url = MailNotificationTest.class.getResource(resourceName);
            final Path resPath;
            resPath = Paths.get(url.toURI());
            return Files.readAllBytes(resPath);
        } catch (IOException | URISyntaxException e) {
            throw new IllegalStateException(e);
        }
    }
}
