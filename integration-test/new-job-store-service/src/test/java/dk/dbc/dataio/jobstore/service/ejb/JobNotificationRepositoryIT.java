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

import dk.dbc.dataio.commons.utils.lang.StringUtil;
import dk.dbc.dataio.commons.utils.test.model.JobSpecificationBuilder;
import dk.dbc.dataio.jobstore.service.entity.ChunkEntity;
import dk.dbc.dataio.jobstore.service.entity.ItemEntity;
import dk.dbc.dataio.jobstore.service.entity.JobEntity;
import dk.dbc.dataio.jobstore.service.entity.NotificationEntity;
import dk.dbc.dataio.jobstore.types.ItemData;
import dk.dbc.dataio.jobstore.types.JobNotification;
import dk.dbc.dataio.jobstore.types.State;
import dk.dbc.dataio.jobstore.types.StateChange;
import org.junit.Test;
import org.jvnet.mock_javamail.Mailbox;

import javax.ejb.SessionContext;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.AddressException;
import javax.persistence.Query;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
        final NotificationEntity notification = persistenceContext.run(() ->
                jobNotificationRepository.addNotification(JobNotification.Type.JOB_CREATED, jobEntity));

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

        persistenceContext.run(() -> {
            jobNotificationRepository.addNotification(JobNotification.Type.JOB_CREATED, job1);
            jobNotificationRepository.addNotification(JobNotification.Type.JOB_CREATED, job2);
            jobNotificationRepository.addNotification(JobNotification.Type.JOB_COMPLETED, job1);
        });

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

        final NotificationEntity notification = persistenceContext.run(() ->
            jobNotificationRepository.addNotification(JobNotification.Type.JOB_CREATED, jobEntity));

        // When...
        persistenceContext.run(() ->
            jobNotificationRepository.processNotification(notification));

        // Then...
        final List<NotificationEntity> notifications = findAllNotifications();
        assertThat("Number of notifications", notifications.size(), is(1));
        assertThat("getStatus()", notifications.get(0).getStatus(), is(JobNotification.Status.COMPLETED));
    }

    /**
     * Given: a repository containing a job with two chunks, where
     *        the first chunk has a single item failed during processing,
     *        the second chunk has a single item failed during delivering,
     * When : a notification for the job of type JOB_COMPLETED is processed
     * Then : notification is updated with completed status
     * And  : a mail notification is sent containing item exports in the order
     *        first chunk item before second chunk item
     */
    @Test
    public void processNotificationWithAppendedFailures() throws MessagingException, IOException {
        // Given...
        final JobEntity jobEntity = newJobEntity();
        jobEntity.setSpecification(
                new JobSpecificationBuilder()
                        .setMailForNotificationAboutVerification("verification@company.com")
                        .build()
        );
        jobEntity.getState()
                .updateState(new StateChange()
                    .setPhase(State.Phase.PROCESSING)
                    .incFailed(1));
        jobEntity.getState()
                .updateState(new StateChange()
                    .setPhase(State.Phase.DELIVERING)
                    .incFailed(1));
        persist(jobEntity);

        newPersistedChunkEntity(new ChunkEntity.Key(0, jobEntity.getId()));
        newPersistedChunkEntity(new ChunkEntity.Key(1, jobEntity.getId()));

        final ItemEntity itemFailedDuringProcessing = newItemEntity(new ItemEntity.Key(jobEntity.getId(), 0, (short) 1));
        itemFailedDuringProcessing.getState()
                .updateState(new StateChange()
                    .setPhase(State.Phase.PROCESSING)
                    .incFailed(1));
        itemFailedDuringProcessing.setPartitioningOutcome(getItemDataFor(asAddi(getMarcXchange("recordFromPartitioning"))));
        final ItemEntity itemFailedDuringDelivering = newItemEntity(new ItemEntity.Key(jobEntity.getId(), 1, (short) 0));
        itemFailedDuringDelivering.getState()
                .updateState(new StateChange()
                    .setPhase(State.Phase.DELIVERING)
                    .incFailed(1));
        itemFailedDuringDelivering.setProcessingOutcome(getItemDataFor(asAddi(getMarcXchange("recordFromProcessing"))));

        persist(itemFailedDuringProcessing);
        persist(itemFailedDuringDelivering);

        final JobNotificationRepository jobNotificationRepository = newJobNotificationRepository();

        // When...
        final NotificationEntity notification = persistenceContext.run(() ->
            jobNotificationRepository.addNotification(JobNotification.Type.JOB_COMPLETED, jobEntity));

        persistenceContext.run(() ->
            jobNotificationRepository.processNotification(notification));

        // Then...
        final List<NotificationEntity> notifications = findAllNotifications();
        assertThat("Number of notifications", notifications.size(), is(1));
        assertThat("getStatus()", notifications.get(0).getStatus(), is(JobNotification.Status.COMPLETED));

        // And...
        final List<Message> inbox = Mailbox.get(jobEntity.getSpecification().getMailForNotificationAboutVerification());
        assertThat("Number of notifications published", inbox.size(), is(1));

        final String mailContent = (String) inbox.get(0).getContent();
        final Pattern pattern = Pattern.compile("\\*arecordFromPartitioning.*\\*arecordFromProcessing", Pattern.DOTALL);
        final Matcher matcher = pattern.matcher(mailContent);
        assertThat(matcher.find(), is(true));
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

        persistenceContext.run(() -> {
            jobNotificationRepository.addNotification(JobNotification.Type.JOB_CREATED, jobEntity);
            jobNotificationRepository.addNotification(JobNotification.Type.JOB_CREATED, jobEntity);
            jobNotificationRepository.addNotification(JobNotification.Type.JOB_CREATED, jobEntity);
            jobNotificationRepository.addNotification(JobNotification.Type.JOB_COMPLETED, jobEntity)
                    .setStatus(JobNotification.Status.COMPLETED);
        });

        // When...
        persistenceContext.run(jobNotificationRepository::flushNotifications);

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

    private String getMarcXchange(String id) {
        return  "<?xml version='1.0' encoding='UTF-8'?>\n" +
                "<record xmlns='info:lc/xmlns/marcxchange-v1' xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance' xsi:schemaLocation='info:lc/xmlns/marcxchange-v1 http://www.loc.gov/standards/iso25577/marcxchange-1-1.xsd'>" +
                "<datafield ind1='0' ind2='0' tag='001'>" +
                "<subfield code='a'>" + id + "</subfield>" +
                "</datafield>" +
                "</record>";
    }

    private String asAddi(String content) {
        return String.format("19\n<es:referencedata/>\n%d\n%s\n",
                    content.getBytes(StandardCharsets.UTF_8).length, content);
    }

    private ItemData getItemDataFor(String data) {
        return new ItemData(StringUtil.base64encode(data), StandardCharsets.UTF_8);
    }
}
