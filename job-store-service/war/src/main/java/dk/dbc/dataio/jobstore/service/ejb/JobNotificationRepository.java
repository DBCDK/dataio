package dk.dbc.dataio.jobstore.service.ejb;

import dk.dbc.commons.jsonb.JSONBContext;
import dk.dbc.commons.jsonb.JSONBException;
import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.types.interceptor.Stopwatch;
import dk.dbc.dataio.commons.utils.lang.StringUtil;
import dk.dbc.dataio.jobstore.service.entity.JobEntity;
import dk.dbc.dataio.jobstore.service.entity.NotificationEntity;
import dk.dbc.dataio.jobstore.service.util.Attachment;
import dk.dbc.dataio.jobstore.service.util.JobExporter;
import dk.dbc.dataio.jobstore.service.util.MailDestination;
import dk.dbc.dataio.jobstore.service.util.MailNotification;
import dk.dbc.dataio.jobstore.types.JobStoreException;
import dk.dbc.dataio.jobstore.types.Notification;
import dk.dbc.dataio.jobstore.types.NotificationContext;
import dk.dbc.dataio.jobstore.types.State;
import dk.dbc.vipcore.service.VipCoreServiceConnector;
import jakarta.annotation.Resource;
import jakarta.ejb.Asynchronous;
import jakarta.ejb.SessionContext;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.inject.Inject;
import jakarta.mail.Session;
import jakarta.persistence.LockModeType;
import jakarta.persistence.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

@Stateless
public class JobNotificationRepository extends RepositoryBase {
    private static final Logger LOGGER = LoggerFactory.getLogger(JobNotificationRepository.class);

    private static final int MAX_NUMBER_OF_NOTIFICATIONS_PER_RESULT = 100;

    // TODO: 13-03-18 Move these to NotificationEntity class as named queries
    private static final String SELECT_NOTIFICATIONS_BY_STATUS_STATEMENT =
            "SELECT n from NotificationEntity n WHERE n.status=:status ORDER BY n.id ASC";
    private static final String SELECT_NOTIFICATIONS_BY_JOB_STATEMENT =
            "SELECT n from NotificationEntity n WHERE n.jobId=:jobId ORDER BY n.id ASC";

    private JSONBContext jsonbContext = new JSONBContext();

    @Resource
    SessionContext sessionContext;

    @Resource(lookup = "mail/dataio/jobstore/notifications")
    Session mailSession;

    @Inject
    VipCoreServiceConnector vipCoreServiceConnector;

    /**
     * Gets all notifications associated with job
     *
     * @param jobId id of job for which to get notifications
     * @return list of notifications
     */
    @Stopwatch
    public List<NotificationEntity> getNotificationsForJob(long jobId) {
        return entityManager.createQuery(SELECT_NOTIFICATIONS_BY_JOB_STATEMENT, NotificationEntity.class)
                .setParameter("jobId", jobId)
                .getResultList();
    }

    /**
     * Flushes all waiting notifications in a separate transactional
     * scope to avoid tearing down any controlling timers in case of an exception
     */
    @Asynchronous
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public void flushNotifications() {
        final Query waitingNotificationsQuery = getWaitingNotificationsQuery();
        int numberOfNotificationsFound;
        do {
            @SuppressWarnings("unchecked") final List<NotificationEntity> waitingNotifications = (List<NotificationEntity>) waitingNotificationsQuery.getResultList();

            numberOfNotificationsFound = waitingNotifications.size();
            if (numberOfNotificationsFound > 0) {
                final JobNotificationRepository jobNotifyProxy = getProxyToSelf();
                for (NotificationEntity notification : waitingNotifications) {
                    try {
                        jobNotifyProxy.processNotification(notification);
                    } catch (RuntimeException e) {
                        LOGGER.error("Unhandled exception caught during processing of notification " + notification.getId(), e);
                    }
                }
            }
        } while (numberOfNotificationsFound == MAX_NUMBER_OF_NOTIFICATIONS_PER_RESULT);
    }

    /**
     * Processes given notification in a separate transactional scope
     *
     * @param notification notification to be processed
     * @return true if notification was processed without failure, otherwise false
     */
    @Stopwatch
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public boolean processNotification(NotificationEntity notification) {
        notification = entityManager.merge(notification);

        // lock notification to avoid dual processing
        entityManager.lock(notification, LockModeType.PESSIMISTIC_WRITE);
        entityManager.refresh(notification);

        if (notification.getStatus() != Notification.Status.WAITING) {
            LOGGER.warn("Processing of notification {} aborted since it has status {}",
                    notification.getId(), notification.getStatus());
            return false;
        }

        try {
            final MailNotification mailNotification = newMailNotification(notification);
            // flush to catch database errors early
            // and therefore avoid mail bombings
            entityManager.flush();
            send(mailNotification);
        } catch (JobStoreException e) {
            LOGGER.error("Notification processing failed", e);
            notification.setStatus(Notification.Status.FAILED);
            notification.setStatusMessage(StringUtil.getStackTraceString(e, ""));
            return false;
        }

        notification.setStatus(Notification.Status.COMPLETED);
        return true;
    }

    protected void send(MailNotification mailNotification) throws JobStoreException {
        mailNotification.send();
    }

    /**
     * Adds waiting notification entity of given type linked to given job
     *
     * @param type type of notification
     * @param job  associated job
     * @return managed instance of notification entity
     */
    public NotificationEntity addNotification(Notification.Type type, JobEntity job) {
        final NotificationEntity notificationEntity = new NotificationEntity();
        notificationEntity.setStatus(Notification.Status.WAITING);
        notificationEntity.setType(type);
        notificationEntity.setJob(job);
        syncedPersist(notificationEntity);
        return notificationEntity;
    }

    /**
     * Adds waiting notification entity of given type with given context
     *
     * @param notificationType type of notification
     * @param mailDestination  destination of mail
     * @param context          notification Context
     * @return created notification entity
     * @throws JobStoreException on internal failure to marshall notification context
     */
    public NotificationEntity addNotification(Notification.Type notificationType,
                                              String mailDestination, NotificationContext context) throws JobStoreException {
        final NotificationEntity notificationEntity = new NotificationEntity();
        notificationEntity.setStatus(Notification.Status.WAITING);
        notificationEntity.setType(notificationType);
        notificationEntity.setDestination(mailDestination);
        try {
            notificationEntity.setContext(jsonbContext.marshall(context));
        } catch (JSONBException e) {
            throw new JobStoreException("Unable to marshall notification context", e);
        }
        syncedPersist(notificationEntity);
        return notificationEntity;
    }

    /**
     * Lists notifications of given type ordered by descending timeOfCreation
     *
     * @param type {@link Notification.Type}
     * @return list of {@link NotificationEntity}
     */
    public List<NotificationEntity> getNotificationsByType(Notification.Type type) {
        return entityManager.createNamedQuery(NotificationEntity.SELECT_BY_TYPE, NotificationEntity.class)
                .setParameter("type", type)
                .getResultList();
    }

    protected MailNotification newMailNotification(MailDestination destination, NotificationEntity notification) throws JobStoreException {
        return new MailNotification(destination, notification);
    }

    private Query getWaitingNotificationsQuery() {
        return entityManager.createQuery(SELECT_NOTIFICATIONS_BY_STATUS_STATEMENT)
                .setParameter("status", Notification.Status.WAITING)
                .setMaxResults(MAX_NUMBER_OF_NOTIFICATIONS_PER_RESULT);
    }

    private JobNotificationRepository getProxyToSelf() {
        return sessionContext.getBusinessObject(JobNotificationRepository.class);
    }

    private MailNotification newMailNotification(NotificationEntity notification) throws JobStoreException {
        final MailDestination mailDestination = new MailDestination(mailSession, notification, vipCoreServiceConnector);
        final MailNotification mailNotification = newMailNotification(mailDestination, notification);
        final JobEntity job = notification.getJob();
        if (notification.getType() == Notification.Type.JOB_COMPLETED && job.hasFailedItems() && !job.hasFatalDiagnostics()) {
            final JobExporter jobExporter = new JobExporter(entityManager);
            if (job.getState().getPhase(State.Phase.PARTITIONING).getFailed() > 0) {
                final JobExporter.FailedItemsContent failedItemsContent =
                        jobExporter.exportFailedItemsContent(job.getId(),
                                Collections.singletonList(State.Phase.PARTITIONING),
                                ChunkItem.Type.BYTES, StandardCharsets.UTF_8);
                if (failedItemsContent.hasFatalItems()) {
                    mailDestination.useFallbackDestination();
                }
                mailNotification.attach(createAttachment(job, failedItemsContent));
            }

            mailNotification.append(jobExporter.exportFailedItemsContent(
                    job.getId(), Collections.singletonList(State.Phase.PROCESSING),
                    ChunkItem.Type.DANMARC2_LINEFORMAT, StandardCharsets.UTF_8));
            mailNotification.append(jobExporter.exportFailedItemsContent(
                    job.getId(), Collections.singletonList(State.Phase.DELIVERING),
                    ChunkItem.Type.DANMARC2_LINEFORMAT, StandardCharsets.UTF_8));
        }
        return mailNotification;
    }

    private Attachment createAttachment(JobEntity job, JobExporter.FailedItemsContent failedItemsContent) {
        Charset charset;
        try {
            charset = Attachment.decipherCharset(job.getSpecification().getCharset());
        } catch (IllegalArgumentException e) {
            charset = StandardCharsets.UTF_8;
        }
        final String filename = String.format("fejl_i_poststruktur.%s",
                Attachment.decipherFileNameExtensionFromPackaging(job.getSpecification().getPackaging()));
        return new Attachment(failedItemsContent.getContent().toByteArray(), filename, charset);
    }
}
