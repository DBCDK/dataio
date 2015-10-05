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

package dk.dbc.dataio.jobstore.service.ejb;

import dk.dbc.dataio.commons.types.Constants;
import dk.dbc.dataio.commons.types.JobSpecification;
import dk.dbc.dataio.commons.utils.test.model.JobSpecificationBuilder;
import dk.dbc.dataio.jobstore.service.entity.JobEntity;
import dk.dbc.dataio.jobstore.service.entity.NotificationEntity;
import dk.dbc.dataio.jobstore.types.Diagnostic;
import dk.dbc.dataio.jobstore.types.JobNotification;
import dk.dbc.dataio.jobstore.types.State;
import org.junit.Before;
import org.junit.Test;
import org.jvnet.mock_javamail.Mailbox;

import javax.ejb.SessionContext;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.persistence.EntityManager;
import javax.persistence.Query;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class JobNotificationRepositoryTest {
    private final SessionContext sessionContext = mock(SessionContext.class);
    private final EntityManager entityManager = mock(EntityManager.class);
    private final String destination = "mail@example.com";

    @Before
    public void clearMailBoxes() {
        Mailbox.clearAll();
    }

    @Test
    public void processNotification_notificationHasNonWaitingStatus_returnsFalse() {
        final NotificationEntity notification = new NotificationEntity();
        notification.setStatus(JobNotification.Status.COMPLETED);

        final JobNotificationRepository jobNotificationRepository = getPgJobNotificationRepository();
        assertThat("processNotification() return value", jobNotificationRepository.processNotification(notification), is(false));
        assertThat("notification status", notification.getStatus(), is(JobNotification.Status.COMPLETED));
        assertThat("notification destination", notification.getDestination(), is(nullValue()));
    }

    @Test
    public void processNotification_typeJobCreatedWithEmptyDestination_failsNotificationAndReturnsFalse() {
        final JobSpecification jobSpecification = new JobSpecificationBuilder()
                .setMailForNotificationAboutVerification(" ")
                .build();
        final NotificationEntity notification = getNotificationEntity(JobNotification.Type.JOB_CREATED, jobSpecification);

        final JobNotificationRepository jobNotificationRepository = getPgJobNotificationRepository();
        assertThat("processNotification() return value", jobNotificationRepository.processNotification(notification), is(false));
        assertThat("notification status", notification.getStatus(), is(JobNotification.Status.FAILED));
        assertThat("notification statusMessage", notification.getStatusMessage(), is(notNullValue()));
        assertThat("notification destination", notification.getDestination(), is(nullValue()));
    }

    @Test
    public void processNotification_typeJobCreatedWithMissingDestination_failsNotificationAndReturnsFalse() {
        final JobSpecification jobSpecification = new JobSpecificationBuilder()
                .setMailForNotificationAboutVerification(Constants.MISSING_FIELD_VALUE)
                .build();
        final NotificationEntity notification = getNotificationEntity(JobNotification.Type.JOB_CREATED, jobSpecification);

        final JobNotificationRepository jobNotificationRepository = getPgJobNotificationRepository();
        assertThat("processNotification() return value", jobNotificationRepository.processNotification(notification), is(false));
        assertThat("notification status", notification.getStatus(), is(JobNotification.Status.FAILED));
        assertThat("notification statusMessage", notification.getStatusMessage(), is(notNullValue()));
        assertThat("notification destination", notification.getDestination(), is(nullValue()));
    }

    @Test
    public void processNotification_typeJobCompletedWithEmptyDestination_failsNotificationAndReturnsFalse() {
        final JobSpecification jobSpecification = new JobSpecificationBuilder()
                .setMailForNotificationAboutVerification(" ")
                .setMailForNotificationAboutProcessing(" ")
                .build();
        final NotificationEntity notification = getNotificationEntity(JobNotification.Type.JOB_CREATED, jobSpecification);

        final JobNotificationRepository jobNotificationRepository = getPgJobNotificationRepository();
        assertThat("processNotification() return value", jobNotificationRepository.processNotification(notification), is(false));
        assertThat("notification status", notification.getStatus(), is(JobNotification.Status.FAILED));
        assertThat("notification statusMessage", notification.getStatusMessage(), is(notNullValue()));
        assertThat("notification destination", notification.getDestination(), is(nullValue()));
    }

    @Test
    public void processNotification_typeJobCompletedWithMissingDestination_failsNotificationAndReturnsFalse() {
        final JobSpecification jobSpecification = new JobSpecificationBuilder()
                .setMailForNotificationAboutVerification(Constants.MISSING_FIELD_VALUE)
                .setMailForNotificationAboutProcessing(Constants.MISSING_FIELD_VALUE)
                .build();
        final NotificationEntity notification = getNotificationEntity(JobNotification.Type.JOB_CREATED, jobSpecification);

        final JobNotificationRepository jobNotificationRepository = getPgJobNotificationRepository();
        assertThat("processNotification() return value", jobNotificationRepository.processNotification(notification), is(false));
        assertThat("notification status", notification.getStatus(), is(JobNotification.Status.FAILED));
        assertThat("notification statusMessage", notification.getStatusMessage(), is(notNullValue()));
        assertThat("notification destination", notification.getDestination(), is(nullValue()));
    }

    @Test
    public void processNotification_sendingOfNotificationFails_failsNotificationAndReturnsFalse() {
        final JobSpecification jobSpecification = new JobSpecificationBuilder().build();
        final NotificationEntity notification = getNotificationEntity(JobNotification.Type.JOB_CREATED, jobSpecification);

        final JobNotificationRepository jobNotificationRepository = getPgJobNotificationRepository();
        jobNotificationRepository.mailSession = null; // force transport layer to fail
        assertThat("processNotification() return value", jobNotificationRepository.processNotification(notification), is(false));
        assertThat("notification status", notification.getStatus(), is(JobNotification.Status.FAILED));
        assertThat("notification statusMessage", notification.getStatusMessage(), is(notNullValue()));
    }

    @Test
    public void processNotification_sendingOfNotificationSucceeds_completesNotificationAndReturnsTrue() {
        final JobSpecification jobSpecification = new JobSpecificationBuilder().build();
        final NotificationEntity notification = getNotificationEntity(JobNotification.Type.JOB_CREATED, jobSpecification);

        final JobNotificationRepository jobNotificationRepository = getPgJobNotificationRepository();
        assertThat("processNotification() return value", jobNotificationRepository.processNotification(notification), is(true));
        assertThat("notification status", notification.getStatus(), is(JobNotification.Status.COMPLETED));
    }

    @Test
    public void processNotification_typeJobCreated_setDestinationToMailForNotificationAboutVerification() {
        final JobSpecification jobSpecification = new JobSpecificationBuilder()
                .setMailForNotificationAboutProcessing(Constants.MISSING_FIELD_VALUE)
                .build();
        final NotificationEntity notification = getNotificationEntity(JobNotification.Type.JOB_CREATED, jobSpecification);

        final JobNotificationRepository jobNotificationRepository = getPgJobNotificationRepository();
        assertThat("processNotification() return value", jobNotificationRepository.processNotification(notification), is(true));
        assertThat("notification status", notification.getStatus(), is(JobNotification.Status.COMPLETED));
        assertThat("notification destination", notification.getDestination(), is(jobSpecification.getMailForNotificationAboutVerification()));
    }

    @Test
    public void processNotification_typeJobCompleted_setDestinationToMailForNotificationAboutProcessing() {
        final JobSpecification jobSpecification = new JobSpecificationBuilder().build();
        final NotificationEntity notification = getNotificationEntity(JobNotification.Type.JOB_COMPLETED, jobSpecification);

        final JobNotificationRepository jobNotificationRepository = getPgJobNotificationRepository();
        assertThat("processNotification() return value", jobNotificationRepository.processNotification(notification), is(true));
        assertThat("notification status", notification.getStatus(), is(JobNotification.Status.COMPLETED));
        assertThat("notification destination", notification.getDestination(), is(jobSpecification.getMailForNotificationAboutProcessing()));
    }

    @Test
    public void processNotification_typeJobCompletedWithoutMailForNotificationAboutProcessing_setsDestinationToMailForNotificationAboutVerification() {
        final JobSpecification jobSpecification = new JobSpecificationBuilder()
                .setMailForNotificationAboutProcessing(Constants.MISSING_FIELD_VALUE)
                .build();
        final NotificationEntity notification = getNotificationEntity(JobNotification.Type.JOB_COMPLETED, jobSpecification);

        final JobNotificationRepository jobNotificationRepository = getPgJobNotificationRepository();
        assertThat("processNotification() return value", jobNotificationRepository.processNotification(notification), is(true));
        assertThat("notification status", notification.getStatus(), is(JobNotification.Status.COMPLETED));
        assertThat("notification destination", notification.getDestination(), is(jobSpecification.getMailForNotificationAboutVerification()));
    }

    @Test
    public void flushNotifications_transportLayerFails_allNotificationsAreProcessed() {
        final JobSpecification jobSpecification = new JobSpecificationBuilder().build();
        final List<NotificationEntity> notifications = Arrays.asList(
                getNotificationEntity(JobNotification.Type.JOB_CREATED, jobSpecification),
                getNotificationEntity(JobNotification.Type.JOB_CREATED, jobSpecification),
                getNotificationEntity(JobNotification.Type.JOB_CREATED, jobSpecification)
        );

        final Query query = mock(Query.class);
        when(entityManager.createQuery(anyString())).thenReturn(query);
        when(query.setParameter(anyString(), any())).thenReturn(query);
        when(query.setMaxResults(anyInt())).thenReturn(query);
        when(query.getResultList()).thenReturn(notifications);

        final JobNotificationRepository jobNotificationRepository = getPgJobNotificationRepository();
        jobNotificationRepository.mailSession = null; // force transport layer to fail
        jobNotificationRepository.flushNotifications();

        for (NotificationEntity notification : notifications) {
            assertThat("notification status", notification.getStatus(), is(JobNotification.Status.FAILED));
        }
    }

    @Test
    public void processNotification_appliesJobCreatedOkTemplate() throws MessagingException, IOException {
        final JobSpecification jobSpecification = new JobSpecificationBuilder()
                .setSubmitterId(42)
                .setMailForNotificationAboutVerification(destination)
                .build();
        final NotificationEntity notification = getNotificationEntity(JobNotification.Type.JOB_CREATED, jobSpecification);

        final JobNotificationRepository jobNotificationRepository = getPgJobNotificationRepository();
        assertThat("processNotification() return value", jobNotificationRepository.processNotification(notification), is(true));
        assertThat("notification status", notification.getStatus(), is(JobNotification.Status.COMPLETED));

        final List<Message> inbox = Mailbox.get(destination);
        assertThat("Number of notifications for destination", inbox.size(), is(1));
        final String content = (String) inbox.get(0).getContent();
        assertThat("Message contains 'Besked fra postmesteren'", content.contains("Besked fra postmesteren"), is(true));
        assertThat("Message contains 'Bibliotek: 42'", content.contains("Bibliotek: 42"), is(true));
    }

    @Test
    public void processNotification_appliesJobCreatedFailTemplate() throws MessagingException, IOException {
        final JobSpecification jobSpecification = new JobSpecificationBuilder()
                .setSubmitterId(42)
                .setMailForNotificationAboutVerification(destination)
                .build();
        final NotificationEntity notification = getNotificationEntity(JobNotification.Type.JOB_CREATED, jobSpecification);
        notification.getJob().getState().getDiagnostics().add(new Diagnostic(Diagnostic.Level.FATAL, "DIED"));

        final JobNotificationRepository jobNotificationRepository = getPgJobNotificationRepository();
        assertThat("processNotification() return value", jobNotificationRepository.processNotification(notification), is(true));
        assertThat("notification status", notification.getStatus(), is(JobNotification.Status.COMPLETED));

        final List<Message> inbox = Mailbox.get(destination);
        assertThat("Number of notifications for destination", inbox.size(), is(1));
        final String content = (String) inbox.get(0).getContent();
        assertThat("Message contains 'Fejlmeddelelse fra postmesteren'", content.contains("Fejlmeddelelse fra postmesteren"), is(true));
        assertThat("Message contains 'Bibliotek: 42'", content.contains("Bibliotek: 42"), is(true));
    }

    @Test
    public void processNotification_appliesJobCompletedTemplate() throws MessagingException, IOException {
        final JobSpecification jobSpecification = new JobSpecificationBuilder()
                .setSubmitterId(42)
                .setMailForNotificationAboutProcessing(destination)
                .build();
        final NotificationEntity notification = getNotificationEntity(JobNotification.Type.JOB_COMPLETED, jobSpecification);

        final JobNotificationRepository jobNotificationRepository = getPgJobNotificationRepository();
        assertThat("processNotification() return value", jobNotificationRepository.processNotification(notification), is(true));
        assertThat("notification status", notification.getStatus(), is(JobNotification.Status.COMPLETED));

        final List<Message> inbox = Mailbox.get(destination);
        assertThat("Number of notifications for destination", inbox.size(), is(1));
        final String content = (String) inbox.get(0).getContent();
        assertThat("Message contains 'Besked fra DanBibs Posthus'", content.contains("Besked fra DanBibs Posthus"), is(true));
        assertThat("Message contains 'Bibliotek: 42'", content.contains("Bibliotek: 42"), is(true));
    }

    private JobNotificationRepository getPgJobNotificationRepository() {
        final Properties mailSessionProperties = new Properties();
        mailSessionProperties.setProperty("mail.from", "dataio@dbc.dk");

        final JobNotificationRepository jobNotificationRepository = new JobNotificationRepository();
        jobNotificationRepository.entityManager = entityManager;
        jobNotificationRepository.sessionContext = sessionContext;
        jobNotificationRepository.mailSession = Session.getDefaultInstance(mailSessionProperties);
        when(sessionContext.getBusinessObject(JobNotificationRepository.class)).thenReturn(jobNotificationRepository);

        return jobNotificationRepository;
    }

    private NotificationEntity getNotificationEntity(JobNotification.Type type, JobSpecification jobSpecification) {
        final JobEntity jobEntity = new JobEntity();
        jobEntity.setSpecification(jobSpecification);
        jobEntity.setState(new State());

        final NotificationEntity notification = new NotificationEntity();
        notification.setStatus(JobNotification.Status.WAITING);
        notification.setType(type);
        notification.setJob(jobEntity);

        return notification;
    }
}