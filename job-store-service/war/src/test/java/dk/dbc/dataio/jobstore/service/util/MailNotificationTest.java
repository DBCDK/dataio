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
import dk.dbc.dataio.jobstore.service.entity.NotificationEntity;
import dk.dbc.dataio.jobstore.types.Diagnostic;
import dk.dbc.dataio.jobstore.types.JobNotification;
import dk.dbc.dataio.jobstore.types.JobStoreException;
import org.junit.Before;
import org.junit.Test;
import org.jvnet.mock_javamail.Mailbox;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.AddressException;
import java.io.IOException;
import java.util.List;
import java.util.Properties;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

public class MailNotificationTest {
    private final String destination = "mail@example.com";
    private final String mailToFallback = "default@dbc.dk";
    private final String mailFrom = "dataio@dbc.dk";

    @Before
    public void clearMailBoxes() {
        Mailbox.clearAll();
    }

    @Test
    public void send_notificationWithEmptyDestination_usesDestinationFallback() throws JobStoreException, AddressException {
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
    public void send_notificationWithMissingDestination_usesDestinationFallback() throws JobStoreException, AddressException {
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
    public void send_appliesJobCreatedOkTemplate() throws JobStoreException, MessagingException, IOException {
        final JobSpecification jobSpecification = new JobSpecificationBuilder()
                .setSubmitterId(42)
                .setMailForNotificationAboutVerification(destination)
                .build();
        final NotificationEntity notification = JobNotificationRepositoryTest.getNotificationEntity(
                JobNotification.Type.JOB_CREATED, jobSpecification);

        final MailNotification mailNotification = getMailNotification(notification);
        mailNotification.send();

        final List<Message> inbox = Mailbox.get(destination);
        assertThat("Number of notifications for destination", inbox.size(), is(1));
        final String content = (String) inbox.get(0).getContent();
        assertThat("Message contains 'Besked fra DanBibs Posthus'", content.contains("Besked fra DanBibs Posthus"), is(true));
        assertThat("Message contains 'Bibliotek: 42'", content.contains("Bibliotek: 42"), is(true));
    }

    @Test
    public void send_appliesJobCreatedFailTemplate() throws JobStoreException, MessagingException, IOException {
        final JobSpecification jobSpecification = new JobSpecificationBuilder()
                .setSubmitterId(42)
                .setMailForNotificationAboutVerification(destination)
                .build();
        final NotificationEntity notification = JobNotificationRepositoryTest.getNotificationEntity(
                JobNotification.Type.JOB_CREATED, jobSpecification);
        notification.getJob().getState().getDiagnostics().add(new Diagnostic(Diagnostic.Level.FATAL, "DIED"));

        final MailNotification mailNotification = getMailNotification(notification);
        mailNotification.send();

        final List<Message> inbox = Mailbox.get(destination);
        assertThat("Number of notifications for destination", inbox.size(), is(1));
        final String content = (String) inbox.get(0).getContent();
        assertThat("Message contains 'Fejlmeddelelse fra DanBibs Posthus'", content.contains("Fejlmeddelelse fra DanBibs Posthus"), is(true));
        assertThat("Message contains 'Bibliotek: 42'", content.contains("Bibliotek: 42"), is(true));
    }

    @Test
    public void send_appliesJobCompletedTemplate() throws JobStoreException, MessagingException, IOException {
        final JobSpecification jobSpecification = new JobSpecificationBuilder()
                .setSubmitterId(42)
                .setMailForNotificationAboutProcessing(destination)
                .build();
        final NotificationEntity notification = JobNotificationRepositoryTest.getNotificationEntity(
                JobNotification.Type.JOB_COMPLETED, jobSpecification);

        final MailNotification mailNotification = getMailNotification(notification);
        mailNotification.send();

        final List<Message> inbox = Mailbox.get(destination);
        assertThat("Number of notifications for destination", inbox.size(), is(1));
        final String content = (String) inbox.get(0).getContent();
        assertThat("Message contains 'Besked fra postmesteren'", content.contains("Besked fra postmesteren"), is(true));
        assertThat("Message contains 'Bibliotek: 42'", content.contains("Bibliotek: 42"), is(true));
    }

    private MailNotification getMailNotification(NotificationEntity notification) {
        final Properties mailSessionProperties = new Properties();
        mailSessionProperties.setProperty("mail.from", mailFrom);
        mailSessionProperties.setProperty("mail.to.fallback", mailToFallback);
        return new MailNotification(Session.getDefaultInstance(mailSessionProperties), notification);
    }
}