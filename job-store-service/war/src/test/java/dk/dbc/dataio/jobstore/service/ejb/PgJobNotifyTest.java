package dk.dbc.dataio.jobstore.service.ejb;

import dk.dbc.dataio.commons.types.Constants;
import dk.dbc.dataio.commons.types.JobSpecification;
import dk.dbc.dataio.commons.utils.test.model.JobSpecificationBuilder;
import dk.dbc.dataio.jobstore.service.entity.JobEntity;
import dk.dbc.dataio.jobstore.service.entity.NotificationEntity;
import dk.dbc.dataio.jobstore.types.JobNotification;
import org.junit.Test;

import javax.ejb.SessionContext;
import javax.mail.Session;
import javax.persistence.EntityManager;
import javax.persistence.Query;
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

public class PgJobNotifyTest {
    private final SessionContext sessionContext = mock(SessionContext.class);
    private final EntityManager entityManager = mock(EntityManager.class);

    @Test
    public void processNotification_notificationHasNonWaitingStatus_returnsFalse() {
        final NotificationEntity notification = new NotificationEntity();
        notification.setStatus(JobNotification.Status.COMPLETED);

        final PgJobNotify pgJobNotify = getPgJobNotify();
        assertThat("processNotification() return value", pgJobNotify.processNotification(notification), is(false));
        assertThat("notification status", notification.getStatus(), is(JobNotification.Status.COMPLETED));
        assertThat("notification destination", notification.getDestination(), is(nullValue()));
    }

    @Test
    public void processNotification_typeJobCreatedWithEmptyDestination_failsNotificationAndReturnsFalse() {
        final JobSpecification jobSpecification = new JobSpecificationBuilder()
                .setMailForNotificationAboutVerification(" ")
                .build();
        final NotificationEntity notification = getNotificationEntity(JobNotification.Type.JOB_CREATED, jobSpecification);

        final PgJobNotify pgJobNotify = getPgJobNotify();
        assertThat("processNotification() return value", pgJobNotify.processNotification(notification), is(false));
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

        final PgJobNotify pgJobNotify = getPgJobNotify();
        assertThat("processNotification() return value", pgJobNotify.processNotification(notification), is(false));
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

        final PgJobNotify pgJobNotify = getPgJobNotify();
        assertThat("processNotification() return value", pgJobNotify.processNotification(notification), is(false));
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

        final PgJobNotify pgJobNotify = getPgJobNotify();
        assertThat("processNotification() return value", pgJobNotify.processNotification(notification), is(false));
        assertThat("notification status", notification.getStatus(), is(JobNotification.Status.FAILED));
        assertThat("notification statusMessage", notification.getStatusMessage(), is(notNullValue()));
        assertThat("notification destination", notification.getDestination(), is(nullValue()));
    }

    @Test
    public void processNotification_sendingOfNotificationFails_failsNotificationAndReturnsFalse() {
        final JobSpecification jobSpecification = new JobSpecificationBuilder().build();
        final NotificationEntity notification = getNotificationEntity(JobNotification.Type.JOB_CREATED, jobSpecification);

        final PgJobNotify pgJobNotify = getPgJobNotify();
        pgJobNotify.mailSession = null; // force transport layer to fail
        assertThat("processNotification() return value", pgJobNotify.processNotification(notification), is(false));
        assertThat("notification status", notification.getStatus(), is(JobNotification.Status.FAILED));
        assertThat("notification statusMessage", notification.getStatusMessage(), is(notNullValue()));
    }

    @Test
    public void processNotification_sendingOfNotificationSucceeds_completesNotificationAndReturnsTrue() {
        final JobSpecification jobSpecification = new JobSpecificationBuilder().build();
        final NotificationEntity notification = getNotificationEntity(JobNotification.Type.JOB_CREATED, jobSpecification);

        final PgJobNotify pgJobNotify = getPgJobNotify();
        assertThat("processNotification() return value", pgJobNotify.processNotification(notification), is(true));
        assertThat("notification status", notification.getStatus(), is(JobNotification.Status.COMPLETED));
    }

    @Test
    public void processNotification_typeJobCreated_setDestinationToMailForNotificationAboutVerification() {
        final JobSpecification jobSpecification = new JobSpecificationBuilder()
                .setMailForNotificationAboutProcessing(Constants.MISSING_FIELD_VALUE)
                .build();
        final NotificationEntity notification = getNotificationEntity(JobNotification.Type.JOB_CREATED, jobSpecification);

        final PgJobNotify pgJobNotify = getPgJobNotify();
        assertThat("processNotification() return value", pgJobNotify.processNotification(notification), is(true));
        assertThat("notification status", notification.getStatus(), is(JobNotification.Status.COMPLETED));
        assertThat("notification destination", notification.getDestination(), is(jobSpecification.getMailForNotificationAboutVerification()));
    }

    @Test
    public void processNotification_typeJobCompleted_setDestinationToMailForNotificationAboutProcessing() {
        final JobSpecification jobSpecification = new JobSpecificationBuilder().build();
        final NotificationEntity notification = getNotificationEntity(JobNotification.Type.JOB_COMPLETED, jobSpecification);

        final PgJobNotify pgJobNotify = getPgJobNotify();
        assertThat("processNotification() return value", pgJobNotify.processNotification(notification), is(true));
        assertThat("notification status", notification.getStatus(), is(JobNotification.Status.COMPLETED));
        assertThat("notification destination", notification.getDestination(), is(jobSpecification.getMailForNotificationAboutProcessing()));
    }

    @Test
    public void processNotification_typeJobCompletedWithoutMailForNotificationAboutProcessing_setsDestinationToMailForNotificationAboutVerification() {
        final JobSpecification jobSpecification = new JobSpecificationBuilder()
                .setMailForNotificationAboutProcessing(Constants.MISSING_FIELD_VALUE)
                .build();
        final NotificationEntity notification = getNotificationEntity(JobNotification.Type.JOB_COMPLETED, jobSpecification);

        final PgJobNotify pgJobNotify = getPgJobNotify();
        assertThat("processNotification() return value", pgJobNotify.processNotification(notification), is(true));
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

        final PgJobNotify pgJobNotify = getPgJobNotify();
        pgJobNotify.mailSession = null; // force transport layer to fail
        pgJobNotify.flushNotifications();

        for (NotificationEntity notification : notifications) {
            assertThat("notification status", notification.getStatus(), is(JobNotification.Status.FAILED));
        }
    }

    private PgJobNotify getPgJobNotify() {
        final Properties mailSessionProperties = new Properties();
        mailSessionProperties.setProperty("mail.from", "dataio@dbc.dk");

        final PgJobNotify pgJobNotify = new PgJobNotify();
        pgJobNotify.entityManager = entityManager;
        pgJobNotify.sessionContext = sessionContext;
        pgJobNotify.mailSession = Session.getDefaultInstance(mailSessionProperties);
        when(sessionContext.getBusinessObject(PgJobNotify.class)).thenReturn(pgJobNotify);

        return pgJobNotify;
    }

    private NotificationEntity getNotificationEntity(JobNotification.Type type, JobSpecification jobSpecification) {
        final JobEntity jobEntity = new JobEntity();
        jobEntity.setSpecification(jobSpecification);

        final NotificationEntity notification = new NotificationEntity();
        notification.setStatus(JobNotification.Status.WAITING);
        notification.setType(type);
        notification.setJob(jobEntity);

        return notification;
    }
}