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

import dk.dbc.dataio.commons.types.JobSpecification;
import dk.dbc.dataio.commons.utils.test.model.JobSpecificationBuilder;
import dk.dbc.dataio.jobstore.service.entity.JobEntity;
import dk.dbc.dataio.jobstore.service.entity.NotificationEntity;
import dk.dbc.dataio.jobstore.types.JobNotification;
import dk.dbc.dataio.jobstore.types.State;
import org.junit.Before;
import org.junit.Test;
import org.jvnet.mock_javamail.Mailbox;

import javax.ejb.SessionContext;
import javax.mail.Session;
import javax.persistence.EntityManager;
import javax.persistence.Query;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class JobNotificationRepositoryTest {
    private final SessionContext sessionContext = mock(SessionContext.class);
    private final EntityManager entityManager = mock(EntityManager.class);
    private final String destination = "mail@example.com";
    private final String mailToFallback = "default@dbc.dk";
    private final String mailFrom = "dataio@dbc.dk";

    @Before
    public void clearMailBoxes() {
        Mailbox.clearAll();
    }

    @Before
    public void setupExpectations() {
        when(entityManager.merge(any(NotificationEntity.class))).then(returnsFirstArg());
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
        final JobSpecification jobSpecification = new JobSpecificationBuilder()
                .setMailForNotificationAboutVerification("verification@company.com")
                .build();
        final NotificationEntity notification = getNotificationEntity(JobNotification.Type.JOB_CREATED, jobSpecification);

        final JobNotificationRepository jobNotificationRepository = getPgJobNotificationRepository();
        assertThat("processNotification() return value", jobNotificationRepository.processNotification(notification), is(true));
        assertThat("notification status", notification.getStatus(), is(JobNotification.Status.COMPLETED));
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
    public void getNotificationsForJob_repositoryQueryReturnsEmptyResultList_returnsEmptyList() {
        final Query query = mock(Query.class);
        when(entityManager.createQuery(anyString())).thenReturn(query);
        when(query.setParameter(anyString(), any())).thenReturn(query);
        when(query.getResultList()).thenReturn(Collections.emptyList());

        final JobNotificationRepository jobNotificationRepository = getPgJobNotificationRepository();
        final List<JobNotification> notifications = jobNotificationRepository.getNotificationsForJob(42);
        assertThat("Return value", notifications, is(notNullValue()));
        assertThat("Number of notifications", notifications.size(), is(0));
    }

    @Test
    public void getNotificationsForJob_repositoryQueryReturnsNonEmptyResultList_returns() {
        final JobSpecification jobSpecification = new JobSpecificationBuilder().build();
        final List<NotificationEntity> entities = Arrays.asList(
                getNotificationEntity(JobNotification.Type.JOB_CREATED, jobSpecification),
                getNotificationEntity(JobNotification.Type.JOB_COMPLETED, jobSpecification)
        );

        final Query query = mock(Query.class);
        when(entityManager.createQuery(anyString())).thenReturn(query);
        when(query.setParameter(anyString(), any())).thenReturn(query);
        when(query.getResultList()).thenReturn(entities);

        final JobNotificationRepository jobNotificationRepository = getPgJobNotificationRepository();
        final List<JobNotification> notifications = jobNotificationRepository.getNotificationsForJob(42);
        assertThat("Return value", notifications, is(notNullValue()));
        assertThat("Number of notifications", notifications.size(), is(2));

        int i = 0;
        for (JobNotification notification : notifications) {
            assertThat("Notification " + i, notification, is(entities.get(i).toJobNotification()));
            i++;
        }
    }

    @Test
    public void addNotification_persistsAndReturnsEntityInWaitingState() {
        final JobEntity jobEntity = new JobEntity();
        final JobNotificationRepository jobNotificationRepository = getPgJobNotificationRepository();
        final NotificationEntity notificationEntity = jobNotificationRepository.addNotification(JobNotification.Type.JOB_COMPLETED, jobEntity);
        assertThat("getStatus()", notificationEntity.getStatus(), is(JobNotification.Status.WAITING));
        assertThat("getType()", notificationEntity.getType(), is(JobNotification.Type.JOB_COMPLETED));
        assertThat("getJob()", notificationEntity.getJob(), is(jobEntity));

        verify(entityManager).persist(notificationEntity);
    }

    private JobNotificationRepository getPgJobNotificationRepository() {
        final Properties mailSessionProperties = new Properties();
        mailSessionProperties.setProperty("mail.from", mailFrom);
        mailSessionProperties.setProperty("mail.to.fallback", mailToFallback);

        final JobNotificationRepository jobNotificationRepository = new JobNotificationRepository();
        jobNotificationRepository.entityManager = entityManager;
        jobNotificationRepository.sessionContext = sessionContext;
        jobNotificationRepository.mailSession = Session.getDefaultInstance(mailSessionProperties);
        when(sessionContext.getBusinessObject(JobNotificationRepository.class)).thenReturn(jobNotificationRepository);

        return jobNotificationRepository;
    }

    public static NotificationEntity getNotificationEntity(JobNotification.Type type) {
       final NotificationEntity notification = new NotificationEntity();
        notification.setStatus(JobNotification.Status.WAITING);
        notification.setType(type);
        return notification;
    }

    public static NotificationEntity getNotificationEntity(JobNotification.Type type, JobSpecification jobSpecification) {
        final JobEntity jobEntity = new JobEntity();
        jobEntity.setSpecification(jobSpecification);
        jobEntity.setState(new State());

        final NotificationEntity notification = getNotificationEntity(type);
        notification.setJob(jobEntity);
        return notification;
    }
}