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

import dk.dbc.dataio.commons.types.Constants;
import dk.dbc.dataio.commons.types.JobSpecification;
import dk.dbc.dataio.commons.utils.test.model.JobSpecificationBuilder;
import dk.dbc.dataio.jobstore.service.ejb.JobNotificationRepositoryTest;
import dk.dbc.dataio.jobstore.service.entity.JobEntity;
import dk.dbc.dataio.jobstore.service.entity.NotificationEntity;
import dk.dbc.dataio.jobstore.types.Diagnostic;
import dk.dbc.dataio.jobstore.types.IncompleteTransfileNotificationContext;
import dk.dbc.dataio.jobstore.types.JobNotification;
import dk.dbc.dataio.jobstore.types.JobStoreException;
import dk.dbc.dataio.jobstore.types.State;
import dk.dbc.dataio.jobstore.types.StateChange;
import dk.dbc.dataio.jsonb.JSONBContext;
import dk.dbc.dataio.jsonb.JSONBException;
import org.junit.Before;
import org.junit.Test;
import org.jvnet.mock_javamail.Mailbox;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.AddressException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Properties;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

public class MailNotificationTest {
    private static final String JOB_CREATED_OK_BODY = "/notifications/job_created_ok.body";
    private static final String JOB_CREATED_FAIL_BODY = "/notifications/job_created_fail.body";
    private static final String JOB_COMPLETED_BODY = "/notifications/job_completed.body";
    private static final String INCOMPLETE_TRANSFILE_BODY = "/notifications/incomplete_transfile.body";
    private final String destination = "mail@example.com";
    private final String mailToFallback = "default@dbc.dk";
    private final String mailFrom = "dataio@dbc.dk";

    @Before
    public void clearMailBoxes() {
        Mailbox.clearAll();
    }

    @Test
    public void send_notificationWithNullDestination_usesDestinationFallback() throws JobStoreException, AddressException {
        final NotificationEntity notification = JobNotificationRepositoryTest.getNotificationEntity(
                JobNotification.Type.INCOMPLETE_TRANSFILE);
        notification.setDestination(null);
        notification.setContext("{}");

        final MailNotification mailNotification = getMailNotification(notification);
        mailNotification.send();

        final List<Message> inbox = Mailbox.get(mailToFallback);
        assertThat("Number of notifications for destination", inbox.size(), is(1));
    }

    @Test
    public void send_notificationWithEmptyDestination_usesDestinationFallback() throws JobStoreException, AddressException {
        final NotificationEntity notification = JobNotificationRepositoryTest.getNotificationEntity(
                JobNotification.Type.INCOMPLETE_TRANSFILE);
        notification.setDestination(" ");
        notification.setContext("{}");

        final MailNotification mailNotification = getMailNotification(notification);
        mailNotification.send();

        final List<Message> inbox = Mailbox.get(mailToFallback);
        assertThat("Number of notifications for destination", inbox.size(), is(1));
    }

    @Test
    public void send_notificationWithMissingDestination_usesDestinationFallback() throws JobStoreException, AddressException {
        final NotificationEntity notification = JobNotificationRepositoryTest.getNotificationEntity(
                JobNotification.Type.INCOMPLETE_TRANSFILE);
        notification.setDestination(Constants.MISSING_FIELD_VALUE);
        notification.setContext("{}");

        final MailNotification mailNotification = getMailNotification(notification);
        mailNotification.send();

        final List<Message> inbox = Mailbox.get(mailToFallback);
        assertThat("Number of notifications for destination", inbox.size(), is(1));
    }

    @Test
    public void send_notificationForJobSpecificationWithEmptyDestination_usesDestinationFallback() throws JobStoreException, AddressException {
        final JobSpecification jobSpecification = new JobSpecificationBuilder()
                .setMailForNotificationAboutVerification(" ")
                .build();
        final NotificationEntity notification = JobNotificationRepositoryTest.getNotificationEntity(
                JobNotification.Type.JOB_CREATED, jobSpecification);

        final MailNotification mailNotification = getMailNotification(notification);
        mailNotification.send();

        final List<Message> inbox = Mailbox.get(mailToFallback);
        assertThat("Number of notifications for destination", inbox.size(), is(1));
    }

    @Test
    public void send_notificationForJobSpecificationWithMissingDestination_usesDestinationFallback() throws JobStoreException, AddressException {
        final JobSpecification jobSpecification = new JobSpecificationBuilder()
                .setMailForNotificationAboutVerification(Constants.MISSING_FIELD_VALUE)
                .build();
        final NotificationEntity notification = JobNotificationRepositoryTest.getNotificationEntity(
                JobNotification.Type.JOB_CREATED, jobSpecification);

        final MailNotification mailNotification = getMailNotification(notification);
        mailNotification.send();

        final List<Message> inbox = Mailbox.get(mailToFallback);
        assertThat("Number of notifications for destination", inbox.size(), is(1));
    }

    @Test
    public void send_typeJobCreated_setDestinationToMailForNotificationAboutVerification() throws JobStoreException {
        final JobSpecification jobSpecification = new JobSpecificationBuilder()
                .setMailForNotificationAboutVerification("verification@company.com")
                .setMailForNotificationAboutProcessing(Constants.MISSING_FIELD_VALUE)
                .build();
        final NotificationEntity notification = JobNotificationRepositoryTest.getNotificationEntity(
                JobNotification.Type.JOB_CREATED, jobSpecification);

        final MailNotification mailNotification = getMailNotification(notification);
        mailNotification.send();
        assertThat("notification destination", notification.getDestination(), is(jobSpecification.getMailForNotificationAboutVerification()));
    }

    @Test
    public void send_typeJobCompleted_setDestinationToMailForNotificationAboutProcessing() throws JobStoreException {
        final JobSpecification jobSpecification = new JobSpecificationBuilder()
                .setMailForNotificationAboutProcessing("processing@company.com")
                .build();
        final NotificationEntity notification = JobNotificationRepositoryTest.getNotificationEntity(
                JobNotification.Type.JOB_COMPLETED, jobSpecification);

        final MailNotification mailNotification = getMailNotification(notification);
        mailNotification.send();
        assertThat("notification destination", notification.getDestination(), is(jobSpecification.getMailForNotificationAboutProcessing()));
    }

    @Test
    public void send_typeJobCompletedWithoutMailForNotificationAboutProcessing_setsDestinationToMailForNotificationAboutVerification() throws JobStoreException {
        final JobSpecification jobSpecification = new JobSpecificationBuilder()
                .setMailForNotificationAboutVerification("verification@company.com")
                .setMailForNotificationAboutProcessing(Constants.MISSING_FIELD_VALUE)
                .build();
        final NotificationEntity notification = JobNotificationRepositoryTest.getNotificationEntity(
                JobNotification.Type.JOB_COMPLETED, jobSpecification);

        final MailNotification mailNotification = getMailNotification(notification);
        mailNotification.send();
        assertThat("notification destination", notification.getDestination(), is(jobSpecification.getMailForNotificationAboutVerification()));
    }

    @Test
    public void send_transportLayerFails_throws() throws JobStoreException {
        final JobSpecification jobSpecification = new JobSpecificationBuilder().build();
        final NotificationEntity notification = JobNotificationRepositoryTest.getNotificationEntity(
                JobNotification.Type.JOB_CREATED, jobSpecification);

        final MailNotification mailNotification = new MailNotification(null, notification);
        try {
            mailNotification.send();
            fail("No JobStoreException thrown");
        } catch (JobStoreException e) {
        }
    }

    @Test
    public void send_appliesIncompleteTransfileTemplate() throws JobStoreException, MessagingException, IOException, JSONBException {
        final JSONBContext jsonbContext = new JSONBContext();
        final IncompleteTransfileNotificationContext context = new IncompleteTransfileNotificationContext("file.trans", "content");
        final NotificationEntity notification = JobNotificationRepositoryTest.getNotificationEntity(
                JobNotification.Type.INCOMPLETE_TRANSFILE);
        notification.setDestination(destination);
        notification.setContext(jsonbContext.marshall(context));

        final MailNotification mailNotification = getMailNotification(notification);
        mailNotification.send();

        final List<Message> inbox = Mailbox.get(destination);
        assertThat("Number of notifications for destination", inbox.size(), is(1));
        final String content = (String) inbox.get(0).getContent();
        assertThat("Notification content", content, is(getResourceContent(INCOMPLETE_TRANSFILE_BODY)));
    }

    @Test
    public void send_appliesJobCreatedOkTemplate() throws JobStoreException, MessagingException, IOException {
        final NotificationEntity notification = JobNotificationRepositoryTest.getNotificationEntity(
                JobNotification.Type.JOB_CREATED, getJobEntity());

        final MailNotification mailNotification = getMailNotification(notification);
        mailNotification.send();

        final List<Message> inbox = Mailbox.get(destination);
        assertThat("Number of notifications for destination", inbox.size(), is(1));
        final String content = (String) inbox.get(0).getContent();
        assertThat("Notification content", content, is(getResourceContent(JOB_CREATED_OK_BODY)));
    }

    @Test
    public void send_appliesJobCreatedFailTemplate() throws JobStoreException, MessagingException, IOException {
        final NotificationEntity notification = JobNotificationRepositoryTest.getNotificationEntity(
                JobNotification.Type.JOB_CREATED, getJobEntity());
        notification.getJob().getState().getDiagnostics().add(new Diagnostic(Diagnostic.Level.FATAL, "Job dannelse fejlet"));

        final MailNotification mailNotification = getMailNotification(notification);
        mailNotification.send();

        final List<Message> inbox = Mailbox.get(destination);
        assertThat("Number of notifications for destination", inbox.size(), is(1));
        final String content = (String) inbox.get(0).getContent();
        assertThat("Notification content", content, is(getResourceContent(JOB_CREATED_FAIL_BODY)));
    }

    @Test
    public void send_appliesJobCompletedTemplate() throws JobStoreException, MessagingException, IOException {
        final NotificationEntity notification = JobNotificationRepositoryTest.getNotificationEntity(
                JobNotification.Type.JOB_COMPLETED, getJobEntity());
        final State state = notification.getJob().getState();
        final StateChange stateChange = new StateChange();
        stateChange.setPhase(State.Phase.PARTITIONING);
        stateChange.setFailed(1);
        state.updateState(stateChange);
        stateChange.setPhase(State.Phase.PROCESSING);
        stateChange.setFailed(2);
        state.updateState(stateChange);
        stateChange.setPhase(State.Phase.DELIVERING);
        stateChange.setFailed(3);
        stateChange.setIgnored(1);
        stateChange.setSucceeded(96);
        state.updateState(stateChange);

        final MailNotification mailNotification = getMailNotification(notification);
        mailNotification.send();

        final List<Message> inbox = Mailbox.get(destination);
        assertThat("Number of notifications for destination", inbox.size(), is(1));
        final String content = (String) inbox.get(0).getContent();
        assertThat("Notification content", content, is(getResourceContent(JOB_COMPLETED_BODY)));
    }

    private MailNotification getMailNotification(NotificationEntity notification) {
        final Properties mailSessionProperties = new Properties();
        mailSessionProperties.setProperty("mail.from", mailFrom);
        mailSessionProperties.setProperty("mail.to.fallback", mailToFallback);
        return new MailNotification(Session.getDefaultInstance(mailSessionProperties), notification);
    }

    private JobEntity getJobEntity() {
        final JobSpecification.Ancestry ancestry = new JobSpecification.Ancestry("file.trans", "file.dat", "batch001");
        final JobSpecification jobSpecification = new JobSpecificationBuilder()
                .setSubmitterId(424242)
                .setAncestry(ancestry)
                .setMailForNotificationAboutVerification(destination)
                .setResultmailInitials("TEST")
                .build();
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
}