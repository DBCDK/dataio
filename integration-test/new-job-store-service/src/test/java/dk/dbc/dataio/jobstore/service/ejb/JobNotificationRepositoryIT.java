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

import dk.dbc.dataio.commons.utils.test.model.JobSpecificationBuilder;
import dk.dbc.dataio.jobstore.service.entity.JobEntity;
import dk.dbc.dataio.jobstore.service.entity.NotificationEntity;
import dk.dbc.dataio.jobstore.types.JobNotification;
import org.junit.Test;
import org.jvnet.mock_javamail.Mailbox;

import javax.ejb.SessionContext;
import javax.mail.Message;
import javax.mail.Session;
import javax.mail.internet.AddressException;
import javax.persistence.EntityTransaction;
import javax.persistence.Query;
import java.util.List;
import java.util.Properties;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class JobNotificationRepositoryIT extends AbstractJobStoreIT {
    private final SessionContext sessionContext = mock(SessionContext.class);

    /**
     * Given: an empty notification repository
     * When : a notification is added
     * Then : the notification is linked to a job
     * And  : the notification has status WAITING
     */
    @Test
    public void addNotification() {
        final JobEntity jobEntity = newPersistedJobEntity();
        final JobNotificationRepository jobNotificationRepository = newJobNotificationRepository();

        // When...
        final EntityTransaction transaction = entityManager.getTransaction();
        transaction.begin();
        final NotificationEntity notification = jobNotificationRepository.addNotification(
                JobNotification.Type.JOB_CREATED, jobEntity);
        transaction.commit();

        // Then...
        final List<NotificationEntity> notifications = findAllNotifications();
        assertThat("Number of notifications", notifications.size(), is(1));
        assertThat("getId()", notifications.get(0).getId(), is(notification.getId()));
        assertThat("getJobId()", notifications.get(0).getJobId(), is(notification.getJobId()));

        // And...
        assertThat("getStatus()", notifications.get(0).getStatus(), is(JobNotification.Status.WAITING));
    }

    /**
     * Given: a non-empty notification repository
     * When : notifications linked to a specific job are requested
     * Then : only relevant notifications are returned
     */
    @Test
    public void getNotificationsForJob() {
        // Given...
        final JobEntity job1 = newPersistedJobEntity();
        final JobEntity job2 = newPersistedJobEntity();
        final JobNotificationRepository jobNotificationRepository = newJobNotificationRepository();

        final EntityTransaction transaction = entityManager.getTransaction();
        transaction.begin();
        jobNotificationRepository.addNotification(JobNotification.Type.JOB_CREATED, job1);
        jobNotificationRepository.addNotification(JobNotification.Type.JOB_CREATED, job2);
        jobNotificationRepository.addNotification(JobNotification.Type.JOB_COMPLETED, job1);
        transaction.commit();

        // When...
        final List<JobNotification> notifications = jobNotificationRepository.getNotificationsForJob(job1.getId());

        // Then...
        assertThat("Number of notifications", notifications.size(), is(2));
        assertThat("getJobId() of first notification", notifications.get(0).getJobId(), is(job1.getId()));
        assertThat("getJobId() of second notification", notifications.get(1).getJobId(), is(job1.getId()));
    }

    /**
     * Given: a notification
     * When : notifications is processed
     * Then : notification is updated with new status
     */
    @Test
    public void processNotification() {
        final JobEntity jobEntity = newPersistedJobEntity();
        jobEntity.setSpecification(
                new JobSpecificationBuilder()
                        .setMailForNotificationAboutVerification("verification@company.com")
                        .build()
        );
        final JobNotificationRepository jobNotificationRepository = newJobNotificationRepository();

        final EntityTransaction transaction = entityManager.getTransaction();
        transaction.begin();
        NotificationEntity notification = jobNotificationRepository.addNotification(
                JobNotification.Type.JOB_CREATED, jobEntity);
        transaction.commit();

        // When...
        transaction.begin();
        jobNotificationRepository.processNotification(notification);
        transaction.commit();

        // Then...
        final List<NotificationEntity> notifications = findAllNotifications();
        assertThat("Number of notifications", notifications.size(), is(1));
        assertThat("getStatus()", notifications.get(0).getStatus(), is(JobNotification.Status.COMPLETED));
    }

    /**
     * Given: a repository with a number of notifications
     * When : notifications are flushed
     * Then : all waiting notifications are processed
     */
    @Test
    public void flushNotifications() throws AddressException {
        final JobEntity jobEntity = newPersistedJobEntity();
        jobEntity.setSpecification(
                new JobSpecificationBuilder()
                        .setMailForNotificationAboutVerification("verification@company.com")
                        .build()
        );
        final JobNotificationRepository jobNotificationRepository = newJobNotificationRepository();

        final EntityTransaction transaction = entityManager.getTransaction();
        transaction.begin();
        jobNotificationRepository.addNotification(JobNotification.Type.JOB_CREATED, jobEntity);
        jobNotificationRepository.addNotification(JobNotification.Type.JOB_CREATED, jobEntity);
        jobNotificationRepository.addNotification(JobNotification.Type.JOB_CREATED, jobEntity);
        final NotificationEntity notification =
                jobNotificationRepository.addNotification(JobNotification.Type.JOB_COMPLETED, jobEntity);
        notification.setStatus(JobNotification.Status.COMPLETED);
        transaction.commit();

        // When...
        transaction.begin();
        jobNotificationRepository.flushNotifications();
        transaction.commit();

        // Then...
        final List<NotificationEntity> notifications = findAllNotifications();
        assertThat("Number of notifications", notifications.size(), is(4));
        for (NotificationEntity entity : notifications) {
            assertThat("Entity status", entity.getStatus(), is(JobNotification.Status.COMPLETED));
        }

        final List<Message> inbox = Mailbox.get(jobEntity.getSpecification().getMailForNotificationAboutVerification());
        assertThat("Number of notifications published", inbox.size(), is(3));
    }

    private JobNotificationRepository newJobNotificationRepository() {
        final Properties mailSessionProperties = new Properties();
        mailSessionProperties.setProperty("mail.from", "dataio@dbc.dk");

        final JobNotificationRepository jobNotificationRepository = new JobNotificationRepository();
        jobNotificationRepository.entityManager = entityManager;
        jobNotificationRepository.mailSession = Session.getDefaultInstance(mailSessionProperties);
        jobNotificationRepository.sessionContext = sessionContext;

        when(sessionContext.getBusinessObject(JobNotificationRepository.class)).thenReturn(jobNotificationRepository);

        return jobNotificationRepository;
    }

    public List<NotificationEntity> findAllNotifications() {
        final Query query = entityManager.createQuery("SELECT e FROM NotificationEntity e");
        return (List<NotificationEntity>) query.getResultList();
    }
}
