package dk.dbc.dataio.jobstore.service.ejb;

import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.types.Diagnostic;
import dk.dbc.dataio.commons.utils.lang.StringUtil;
import dk.dbc.dataio.commons.utils.test.model.ChunkItemBuilder;
import dk.dbc.dataio.jobstore.service.AbstractJobStoreIT;
import dk.dbc.dataio.jobstore.service.entity.ChunkEntity;
import dk.dbc.dataio.jobstore.service.entity.ItemEntity;
import dk.dbc.dataio.jobstore.service.entity.JobEntity;
import dk.dbc.dataio.jobstore.service.entity.NotificationEntity;
import dk.dbc.dataio.jobstore.types.InvalidTransfileNotificationContext;
import dk.dbc.dataio.jobstore.types.Notification;
import dk.dbc.dataio.jobstore.types.State;
import dk.dbc.dataio.jobstore.types.StateChange;
import dk.dbc.vipcore.service.VipCoreServiceConnector;
import org.apache.commons.io.IOUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.EnvironmentVariables;
import org.jvnet.mock_javamail.Mailbox;

import javax.ejb.SessionContext;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Part;
import javax.mail.Session;
import javax.mail.internet.AddressException;
import javax.persistence.Query;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class JobNotificationRepositoryIT extends AbstractJobStoreIT {
    private final SessionContext sessionContext = mock(SessionContext.class);
    private final VipCoreServiceConnector vipCoreServiceConnector = mock(VipCoreServiceConnector.class);
    private final String mailToFallback = "mail-to-fallback@dbc.dk";

    @Rule
    public final EnvironmentVariables environmentVariables = new EnvironmentVariables();

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
                jobNotificationRepository.addNotification(Notification.Type.JOB_CREATED, jobEntity));

        // Then...
        final List<NotificationEntity> notifications = findAllNotifications();
        assertThat("Number of notifications", notifications.size(), is(1));
        assertThat("getId()", notifications.get(0).getId(), is(notification.getId()));
        assertThat("getJobId()", notifications.get(0).getJobId(), is(notification.getJobId()));

        // And...
        assertThat("getStatus()", notifications.get(0).getStatus(), is(Notification.Status.WAITING));
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
            jobNotificationRepository.addNotification(Notification.Type.JOB_CREATED, job1);
            jobNotificationRepository.addNotification(Notification.Type.JOB_CREATED, job2);
            jobNotificationRepository.addNotification(Notification.Type.JOB_COMPLETED, job1);
        });

        // When...
        final List<NotificationEntity> notifications = jobNotificationRepository.getNotificationsForJob(job1.getId());

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
        jobEntity.getSpecification().withMailForNotificationAboutVerification("verification@company.com");

        final JobNotificationRepository jobNotificationRepository = newJobNotificationRepository();

        final NotificationEntity notification = persistenceContext.run(() ->
                jobNotificationRepository.addNotification(Notification.Type.JOB_CREATED, jobEntity));

        // When...
        persistenceContext.run(() ->
                jobNotificationRepository.processNotification(notification));

        // Then...
        final List<NotificationEntity> notifications = findAllNotifications();
        assertThat("Number of notifications", notifications.size(), is(1));
        assertThat("getStatus()", notifications.get(0).getStatus(), is(Notification.Status.COMPLETED));
    }

    /**
     * Given: a repository containing a job with two chunks, where
     * the first chunk has a single item failed during processing,
     * the second chunk has a single item failed during delivering
     * with a fatal diagnostic
     * When : a notification for the job of type JOB_COMPLETED is processed
     * Then : notification is updated with completed status
     * And  : a mail notification is sent to the fallback mail containing
     * item exports in the order first chunk item before second chunk item
     */
    @Test
    public void processNotificationWithAppendedFailures() throws MessagingException, IOException {
        environmentVariables.set("MAIL_TO_FALLBACK", mailToFallback);

        // Given...
        final JobEntity jobEntity = newJobEntity();
        jobEntity.getSpecification()
                .withMailForNotificationAboutVerification("verification@company.com");

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
        itemFailedDuringProcessing.setPartitioningOutcome(new ChunkItemBuilder()
                .setData(asAddi(getMarcXchange("recordFromPartitioning")))
                .build());
        final ItemEntity itemFailedDuringDelivering = newItemEntity(new ItemEntity.Key(jobEntity.getId(), 1, (short) 0));
        itemFailedDuringDelivering.getState()
                .updateState(new StateChange()
                        .setPhase(State.Phase.DELIVERING)
                        .incFailed(1));
        itemFailedDuringDelivering.setProcessingOutcome(new ChunkItemBuilder()
                .setData(asAddi(getMarcXchange("recordFromProcessing")))
                .build());
        itemFailedDuringDelivering.setDeliveringOutcome(new ChunkItemBuilder()
                .setDiagnostics(Collections.singletonList(
                        new Diagnostic(Diagnostic.Level.FATAL, "died")))
                .setData("fatal error")
                .build());

        persist(itemFailedDuringProcessing);
        persist(itemFailedDuringDelivering);

        final JobNotificationRepository jobNotificationRepository = newJobNotificationRepository();

        // When...
        final NotificationEntity notification = persistenceContext.run(() ->
                jobNotificationRepository.addNotification(Notification.Type.JOB_COMPLETED, jobEntity));

        persistenceContext.run(() ->
                jobNotificationRepository.processNotification(notification));

        // Then...
        final List<NotificationEntity> notifications = findAllNotifications();
        assertThat("Number of notifications", notifications.size(), is(1));
        assertThat("getStatus()", notifications.get(0).getStatus(), is(Notification.Status.COMPLETED));

        // And...
        final List<Message> inbox = Mailbox.get(mailToFallback);
        assertThat("Number of notifications published", inbox.size(), is(1));

        final String mailContent = (String) inbox.get(0).getContent();
        final Pattern pattern = Pattern.compile("\\*arecordFromPartitioning.*\\*arecordFromProcessing", Pattern.DOTALL);
        final Matcher matcher = pattern.matcher(mailContent);
        assertThat(matcher.find(), is(true));
    }

    /**
     * Given: a repository containing a job with one chunk, where
     * the first item has failed in partitioning and the second item has failed in delivering,
     * When : a notification for the job of type JOB_COMPLETED is processed
     * Then : notification is updated with completed status
     * And  : a mail notification is sent containing item export of the item failed in delivering and with
     * the data from the item failed in partitioning as attachment
     */
    @Test
    public void processNotificationWithAppendedAndAttachedFailures() throws MessagingException, IOException {
        // Given...
        final JobEntity jobEntity = newJobEntity();
        jobEntity.getSpecification()
                .withMailForNotificationAboutVerification("verification@company.com")
                .withPackaging("lin")
                .withCharset("uft8");

        jobEntity.getState()
                .updateState(new StateChange()
                        .setPhase(State.Phase.PARTITIONING)
                        .incFailed(1));
        jobEntity.getState()
                .updateState(new StateChange()
                        .setPhase(State.Phase.DELIVERING)
                        .incFailed(1));

        persist(jobEntity);

        newPersistedChunkEntity(new ChunkEntity.Key(0, jobEntity.getId()));

        final ItemEntity itemFailedDuringPartitioning = newItemEntity(new ItemEntity.Key(jobEntity.getId(), 0, (short) 0));
        itemFailedDuringPartitioning.getState()
                .updateState(new StateChange()
                        .setPhase(State.Phase.PARTITIONING)
                        .incFailed(1));

        final ChunkItem partitioningOutcome = new ChunkItemBuilder()
                .setType(ChunkItem.Type.BYTES)
                .setDiagnostics(Collections.singletonList(
                        new Diagnostic(Diagnostic.Level.ERROR, "error")))
                .setData("** unreadable record failed in partitioning **")
                .build();

        itemFailedDuringPartitioning.setPartitioningOutcome(partitioningOutcome);

        final ItemEntity itemFailedDuringDelivering = newItemEntity(new ItemEntity.Key(jobEntity.getId(), 0, (short) 2));
        itemFailedDuringDelivering.getState()
                .updateState(new StateChange()
                        .setPhase(State.Phase.DELIVERING)
                        .incFailed(1));
        itemFailedDuringDelivering.setProcessingOutcome(new ChunkItemBuilder().setData(asAddi(getMarcXchange("recordFromProcessing"))).build());

        persist(itemFailedDuringPartitioning);
        persist(itemFailedDuringDelivering);

        final JobNotificationRepository jobNotificationRepository = newJobNotificationRepository();

        // When...
        final NotificationEntity notification = persistenceContext.run(() ->
                jobNotificationRepository.addNotification(Notification.Type.JOB_COMPLETED, jobEntity));

        persistenceContext.run(() ->
                jobNotificationRepository.processNotification(notification));

        // Then...
        final List<NotificationEntity> notifications = findAllNotifications();
        assertThat("Number of notifications", notifications.size(), is(1));
        assertThat("getStatus()", notifications.get(0).getStatus(), is(Notification.Status.COMPLETED));

        // And...
        final List<Message> inbox = Mailbox.get(jobEntity.getSpecification().getMailForNotificationAboutVerification());
        assertThat("Number of notifications published", inbox.size(), is(1));

        // Mail consists of 2 parts
        Multipart multipart = (Multipart) inbox.get(0).getContent();
        assertThat("Number of parts which the mail consist of", multipart.getCount(), is(2));

        // First part contains the expected resource content
        final Part mailContentBodyPart = multipart.getBodyPart(0);

        final Pattern pattern = Pattern.compile("\\*arecordFromProcessing", Pattern.DOTALL);
        final Matcher matcher = pattern.matcher(mailContentBodyPart.getContent().toString());
        assertThat(matcher.find(), is(true));

        // Second part contains the expected attachment
        Part mailAttachment = multipart.getBodyPart(1);
        assertThat("Notification attachment disposition", mailAttachment.getDisposition(), is(Part.ATTACHMENT));
        final Writer writer = new StringWriter();
        IOUtils.copy(mailAttachment.getInputStream(), writer);
        assertThat("Notification attachment content", writer.toString(), is(StringUtil.asString(partitioningOutcome.getData())));
    }

    /**
     * Given: an empty repository
     * When : a notification for a type without an associated job is processed
     * Then : notification is updated with completed status
     */
    @Test
    public void processNotification_withoutJob() {
        // Given...
        final InvalidTransfileNotificationContext notificationContext =
                new InvalidTransfileNotificationContext("transfile", "content", "status");

        final JobNotificationRepository jobNotificationRepository = newJobNotificationRepository();

        // When...
        final NotificationEntity notification = persistenceContext.run(() ->
                jobNotificationRepository.addNotification(Notification.Type.INVALID_TRANSFILE, "mail@company.com",
                        notificationContext));

        persistenceContext.run(() ->
                jobNotificationRepository.processNotification(notification));

        // Then...
        final List<NotificationEntity> notifications = findAllNotifications();
        assertThat("Number of notifications", notifications.size(), is(1));
        assertThat("getStatus()", notifications.get(0).getStatus(), is(Notification.Status.COMPLETED));
    }

    /**
     * Given: a repository with a number of notifications
     * When : notifications are flushed
     * Then : all waiting notifications are processed
     */
    @Test
    public void flushNotifications() throws AddressException {
        final JobEntity jobEntity = newPersistedJobEntity();
        jobEntity.getSpecification()
                .withMailForNotificationAboutVerification("verification@company.com");

        final JobNotificationRepository jobNotificationRepository = newJobNotificationRepository();

        persistenceContext.run(() -> {
            jobNotificationRepository.addNotification(Notification.Type.JOB_CREATED, jobEntity);
            jobNotificationRepository.addNotification(Notification.Type.JOB_CREATED, jobEntity);
            jobNotificationRepository.addNotification(Notification.Type.JOB_CREATED, jobEntity);
            jobNotificationRepository.addNotification(Notification.Type.JOB_COMPLETED, jobEntity)
                    .setStatus(Notification.Status.COMPLETED);
        });

        // When...
        persistenceContext.run(jobNotificationRepository::flushNotifications);

        // Then...
        final List<NotificationEntity> notifications = findAllNotifications();
        assertThat("Number of notifications", notifications.size(), is(4));
        for (NotificationEntity entity : notifications) {
            assertThat("Entity status", entity.getStatus(), is(Notification.Status.COMPLETED));
        }

        final List<Message> inbox = Mailbox.get(jobEntity.getSpecification().getMailForNotificationAboutVerification());
        assertThat("Number of notifications published", inbox.size(), is(3));
    }

    @Test
    public void getNotificationsByType() {
        final JobEntity jobEntity = newPersistedJobEntity();
        final JobNotificationRepository jobNotificationRepository = newJobNotificationRepository();

        persistenceContext.run(() -> {
            jobNotificationRepository.addNotification(Notification.Type.JOB_CREATED, jobEntity);
            jobNotificationRepository.addNotification(Notification.Type.JOB_COMPLETED, jobEntity);
            jobNotificationRepository.addNotification(Notification.Type.JOB_CREATED, jobEntity);
        });

        final List<NotificationEntity> notifications =
                jobNotificationRepository.getNotificationsByType(Notification.Type.JOB_CREATED);

        assertThat("Number of notifications found", notifications.size(), is(2));
    }

    private JobNotificationRepository newJobNotificationRepository() {
        final Properties mailSessionProperties = new Properties();
        mailSessionProperties.setProperty("mail.from", "dataio@dbc.dk");

        final JobNotificationRepository jobNotificationRepository = new JobNotificationRepository();
        jobNotificationRepository.entityManager = entityManager;
        jobNotificationRepository.mailSession = Session.getDefaultInstance(mailSessionProperties);
        jobNotificationRepository.sessionContext = sessionContext;
        jobNotificationRepository.vipCoreServiceConnector = vipCoreServiceConnector;

        when(sessionContext.getBusinessObject(JobNotificationRepository.class)).thenReturn(jobNotificationRepository);

        return jobNotificationRepository;
    }

    public List<NotificationEntity> findAllNotifications() {
        final Query query = entityManager.createQuery("SELECT e FROM NotificationEntity e");
        return (List<NotificationEntity>) query.getResultList();
    }

    private String getMarcXchange(String id) {
        return "<?xml version='1.0' encoding='UTF-8'?>\n" +
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
}
