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
import dk.dbc.dataio.commons.types.interceptor.Stopwatch;
import dk.dbc.dataio.commons.types.jndi.JndiConstants;
import dk.dbc.dataio.commons.utils.lang.StringUtil;
import dk.dbc.dataio.jobstore.service.entity.NotificationEntity;
import dk.dbc.dataio.jobstore.types.JobNotification;
import dk.dbc.dataio.jobstore.types.JobStoreException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Resource;
import javax.ejb.Asynchronous;
import javax.ejb.SessionContext;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.mail.Message;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.persistence.EntityManager;
import javax.persistence.LockModeType;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import java.util.Date;
import java.util.List;

import static dk.dbc.dataio.commons.types.Constants.MISSING_FIELD_VALUE;

@Stateless
public class PgJobNotify {
    private static final Logger LOGGER = LoggerFactory.getLogger(PgJobNotify.class);

    private static final int MAX_NUMBER_OF_NOTIFICATIONS_PER_RESULT = 100;
    private static final String SELECT_NOTIFICATIONS_STATEMENT =
            "SELECT n from NotificationEntity n WHERE n.status=:status ORDER BY n.id ASC";

    @Resource
    SessionContext sessionContext;

    @Resource(lookup = JndiConstants.MAIL_RESOURCE_JOBSTORE_NOTIFICATIONS)
    Session mailSession;

    @PersistenceContext(unitName = "jobstorePU")
    EntityManager entityManager;

    /**
     * Flushes all waiting notifications in a separate transactional
     * scope to avoid tearing down any controlling timers in case of an exception
     */
    @Stopwatch
    @Asynchronous
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public void flushNotifications() {
        final Query getWaitingNotificationsQuery = entityManager.createQuery(SELECT_NOTIFICATIONS_STATEMENT)
                .setParameter("status", JobNotification.Status.WAITING)
                .setMaxResults(MAX_NUMBER_OF_NOTIFICATIONS_PER_RESULT);

        int numberOfNotificationsFound;
        do {
            @SuppressWarnings("unchecked")
            final List<NotificationEntity> notifications =
                    (List<NotificationEntity>) getWaitingNotificationsQuery.getResultList();

            numberOfNotificationsFound = notifications.size();
            if (numberOfNotificationsFound > 0) {
                final PgJobNotify jobNotifyProxy = getProxyToSelf();
                for (NotificationEntity notification : notifications) {
                    jobNotifyProxy.processNotification(notification);
                }
            }
        } while (numberOfNotificationsFound == MAX_NUMBER_OF_NOTIFICATIONS_PER_RESULT);
    }

    /**
     * Processes given notification in a separate transactional scope
     * @param notification notification to be processed
     * @return true if notification was processed without failure, otherwise false
     */
    @Stopwatch
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public boolean processNotification(NotificationEntity notification) {
        // lock notification to avoid dual processing
        entityManager.lock(notification, LockModeType.PESSIMISTIC_WRITE);
        entityManager.refresh(notification);

        if (notification.getStatus() != JobNotification.Status.WAITING) {
            LOGGER.warn("Processing of notification {} aborted since it has status {}",
                    notification.getId(), notification.getStatus());
            return false;
        }

        final String destination = getDestination(notification.getType(), notification.getJob().getSpecification());
        if (destination.equals(MISSING_FIELD_VALUE)) {
            notification.setStatus(JobNotification.Status.FAILED);
            notification.setStatusMessage("Notification has no destination");
            return false;
        }

        notification.setDestination(destination);
        notification.setContent("notification body");

        try {
            sendNotification(notification);
        } catch (JobStoreException e) {
            LOGGER.error("Notification processing failed", e);
            notification.setStatus(JobNotification.Status.FAILED);
            notification.setStatusMessage(StringUtil.getStackTraceString(e, ""));
            return false;
        }

        notification.setStatus(JobNotification.Status.COMPLETED);
        return true;
    }

    private PgJobNotify getProxyToSelf() {
        return sessionContext.getBusinessObject(PgJobNotify.class);
    }

    private String getDestination(JobNotification.Type type, JobSpecification jobSpecification) {
        switch (type) {
            case JOB_CREATED:
                return getDestinationForJobCreatedNotification(jobSpecification);
            case JOB_COMPLETED:
                return getDestinationForJobCompletedNotification(jobSpecification);
            default:
                LOGGER.error("Unhandled notification type {}", type);
                return MISSING_FIELD_VALUE;
        }
    }

    private String getDestinationForJobCreatedNotification(JobSpecification jobSpecification) {
        final String destination = jobSpecification.getMailForNotificationAboutVerification();
        if (destination.trim().isEmpty()) {
            return MISSING_FIELD_VALUE;
        }
        return destination;
    }

     private String getDestinationForJobCompletedNotification(JobSpecification jobSpecification) {
        final String destination = jobSpecification.getMailForNotificationAboutProcessing();
        if (destination.trim().isEmpty() || destination.equals(MISSING_FIELD_VALUE)) {
            // fall back to primary destination
            return getDestinationForJobCreatedNotification(jobSpecification);
        }
        return destination;
    }

    private void sendNotification(NotificationEntity notification) throws JobStoreException {
        try {
            final MimeMessage message = new MimeMessage(mailSession);
            final InternetAddress fromAddress = new InternetAddress(mailSession.getProperty("mail.from"));
            final InternetAddress[] toAddresses = {new InternetAddress(notification.getDestination())};
            message.setFrom(fromAddress);
            message.setRecipients(Message.RecipientType.TO, toAddresses);
            message.setSubject("DBC dataIO notification");
            message.setSentDate(new Date());
            message.setText(notification.getContent());
            Transport.send(message);
        } catch (Exception e) {
            throw new JobStoreException("Unable to send notification", e);
        }
    }
}
