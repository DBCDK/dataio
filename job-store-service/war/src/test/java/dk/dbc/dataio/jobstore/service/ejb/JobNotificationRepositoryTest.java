package dk.dbc.dataio.jobstore.service.ejb;

import dk.dbc.dataio.commons.types.JobSpecification;
import dk.dbc.dataio.jobstore.service.entity.JobEntity;
import dk.dbc.dataio.jobstore.service.entity.NotificationEntity;
import dk.dbc.dataio.jobstore.service.util.MailDestination;
import dk.dbc.dataio.jobstore.service.util.MailNotification;
import dk.dbc.dataio.jobstore.types.JobStoreException;
import dk.dbc.dataio.jobstore.types.Notification;
import dk.dbc.dataio.jobstore.types.NotificationContext;
import dk.dbc.dataio.jobstore.types.State;
import dk.dbc.vipcore.service.VipCoreServiceConnector;
import jakarta.ejb.SessionContext;
import jakarta.mail.Session;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import jakarta.persistence.TypedQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class JobNotificationRepositoryTest {
    private final SessionContext sessionContext = mock(SessionContext.class);
    private final EntityManager entityManager = mock(EntityManager.class);
    private final Session session = mock(Session.class);
    private final VipCoreServiceConnector vipCoreServiceConnector = mock(VipCoreServiceConnector.class);
    private final String destination = "mail@example.com";
    private final String mailFrom = "dataio@dbc.dk";

    @BeforeEach
    public void setupExpectations() {
        when(entityManager.merge(any(NotificationEntity.class))).then(returnsFirstArg());
        Properties mailProps = new Properties();
        mailProps.setProperty("mail.from", MailNotification.FROM_ADDRESS_PERSONAL_NAME);
        when(session.getProperties()).thenReturn(mailProps);
    }

    @Test
    public void processNotification_notificationHasNonWaitingStatus_returnsFalse() {
        final NotificationEntity notification = new NotificationEntity();
        notification.setStatus(Notification.Status.COMPLETED);

        final JobNotificationRepository jobNotificationRepository = createJobNotificationRepository();
        assertThat("processNotification() return value", jobNotificationRepository.processNotification(notification), is(false));
        assertThat("notification status", notification.getStatus(), is(Notification.Status.COMPLETED));
        assertThat("notification destination", notification.getDestination(), is(nullValue()));
    }

    @Test
    public void processNotification_sendingOfNotificationFails_failsNotificationAndReturnsFalse() {
        final JobSpecification jobSpecification = new JobSpecification()
                .withMailForNotificationAboutVerification("verification@company.com");
        final NotificationEntity notification = getNotificationEntity(Notification.Type.JOB_CREATED, jobSpecification);

        final JobNotificationRepository jobNotificationRepository = createJobNotificationRepository();
        jobNotificationRepository.mailSession = null; // force transport layer to fail
        assertThat("processNotification() return value", jobNotificationRepository.processNotification(notification), is(false));
        assertThat("notification status", notification.getStatus(), is(Notification.Status.FAILED));
        assertThat("notification statusMessage", notification.getStatusMessage(), is(notNullValue()));
    }

    @Test
    public void processNotification_sendingOfNotificationSucceeds_completesNotificationAndReturnsTrue() {
        final JobSpecification jobSpecification = new JobSpecification()
                .withMailForNotificationAboutVerification("verification@company.com");
        final NotificationEntity notification = getNotificationEntity(Notification.Type.JOB_CREATED, jobSpecification);

        final JobNotificationRepository jobNotificationRepository = createJobNotificationRepository();
        assertThat("processNotification() return value", jobNotificationRepository.processNotification(notification), is(true));
        assertThat("notification status", notification.getStatus(), is(Notification.Status.COMPLETED));
    }

    @Test
    public void flushNotifications_transportLayerFails_allNotificationsAreProcessed() {
        final JobSpecification jobSpecification = new JobSpecification()
                .withMailForNotificationAboutVerification("verification@company.com");
        final List<NotificationEntity> notifications = Arrays.asList(
                getNotificationEntity(Notification.Type.JOB_CREATED, jobSpecification),
                getNotificationEntity(Notification.Type.JOB_CREATED, jobSpecification),
                getNotificationEntity(Notification.Type.JOB_CREATED, jobSpecification)
        );

        final Query query = mock(Query.class);
        when(entityManager.createQuery(anyString())).thenReturn(query);
        when(query.setParameter(anyString(), any())).thenReturn(query);
        when(query.setMaxResults(anyInt())).thenReturn(query);
        when(query.getResultList()).thenReturn(notifications);

        final JobNotificationRepository jobNotificationRepository = createJobNotificationRepository();
        jobNotificationRepository.mailSession = null; // force transport layer to fail
        jobNotificationRepository.flushNotifications();

        for (NotificationEntity notification : notifications) {
            assertThat("notification status", notification.getStatus(), is(Notification.Status.FAILED));
        }
    }

    @Test
    public void flushNotifications_processingThrowsRuntimeException_allNotificationsAreProcessed() {
        final JobSpecification jobSpecification = new JobSpecification()
                .withMailForNotificationAboutVerification("verification@company.com");
        final List<NotificationEntity> notifications = Arrays.asList(
                getNotificationEntity(Notification.Type.JOB_CREATED, jobSpecification),
                getNotificationEntity(Notification.Type.JOB_CREATED, (JobSpecification) null),
                getNotificationEntity(Notification.Type.JOB_CREATED, jobSpecification)
        );

        final Query query = mock(Query.class);
        when(entityManager.createQuery(anyString())).thenReturn(query);
        when(query.setParameter(anyString(), any())).thenReturn(query);
        when(query.setMaxResults(anyInt())).thenReturn(query);
        when(query.getResultList()).thenReturn(notifications);

        final JobNotificationRepository jobNotificationRepository = createJobNotificationRepository();
        jobNotificationRepository.flushNotifications();

        assertThat("1st notification status", notifications.get(0).getStatus(), is(Notification.Status.COMPLETED));
        assertThat("2nd notification status", notifications.get(1).getStatus(), is(Notification.Status.WAITING));
        assertThat("3rd notification status", notifications.get(2).getStatus(), is(Notification.Status.COMPLETED));
    }

    @Test
    public void getNotifications_repositoryQueryReturnsEmptyResultList_returnsEmptyList() {
        final TypedQuery query = mock(TypedQuery.class);
        when(entityManager.createQuery(anyString(), eq(NotificationEntity.class))).thenReturn(query);
        when(query.setParameter(anyString(), any())).thenReturn(query);
        when(query.getResultList()).thenReturn(Collections.emptyList());

        final JobNotificationRepository jobNotificationRepository = createJobNotificationRepository();
        final List<NotificationEntity> notifications = jobNotificationRepository.getNotificationsForJob(42);
        assertThat("Return value", notifications, is(notNullValue()));
        assertThat("Number of notifications", notifications.size(), is(0));
    }

    @Test
    public void getNotifications_repositoryQueryReturnsNonEmptyResultList_returns() {
        final JobSpecification jobSpecification = new JobSpecification();
        final List<NotificationEntity> entities = Arrays.asList(
                getNotificationEntity(Notification.Type.JOB_CREATED, jobSpecification),
                getNotificationEntity(Notification.Type.JOB_COMPLETED, jobSpecification)
        );

        final TypedQuery query = mock(TypedQuery.class);
        when(entityManager.createQuery(anyString(), eq(NotificationEntity.class))).thenReturn(query);
        when(query.setParameter(anyString(), any())).thenReturn(query);
        when(query.getResultList()).thenReturn(entities);

        final JobNotificationRepository jobNotificationRepository = createJobNotificationRepository();
        final List<NotificationEntity> notifications = jobNotificationRepository.getNotificationsForJob(42);
        assertThat("Return value", notifications, is(notNullValue()));
        assertThat("Number of notifications", notifications.size(), is(2));

        int i = 0;
        for (NotificationEntity notification : notifications) {
            assertThat("Notification " + i, notification, is(entities.get(i)));
            i++;
        }
    }

    @Test
    public void addNotification_withJob_persistsAndReturnsEntityInWaitingState() {
        final JobEntity jobEntity = new JobEntity();
        final JobNotificationRepository jobNotificationRepository = createJobNotificationRepository();
        final NotificationEntity notificationEntity = jobNotificationRepository.addNotification(
                Notification.Type.JOB_COMPLETED, jobEntity);
        assertThat("getStatus()", notificationEntity.getStatus(), is(Notification.Status.WAITING));
        assertThat("getType()", notificationEntity.getType(), is(Notification.Type.JOB_COMPLETED));
        assertThat("getJob()", notificationEntity.getJob(), is(jobEntity));

        verify(entityManager).persist(notificationEntity);
    }

    @Test
    public void addNotification_withContext_persistsAndReturnsEntityInWaitingState() throws JobStoreException {
        final JobNotificationRepository jobNotificationRepository = createJobNotificationRepository();
        final NotificationEntity notificationEntity = jobNotificationRepository.addNotification(
                Notification.Type.INVALID_TRANSFILE, destination, new NotificationContext() {
                });

        assertThat("getStatus()", notificationEntity.getStatus(), is(Notification.Status.WAITING));
        assertThat("getType()", notificationEntity.getType(), is(Notification.Type.INVALID_TRANSFILE));
        //JobEntity does not and should not exist for type INVALID_TRANSFILE
        assertThat("getJob()", notificationEntity.getJob(), is(nullValue()));
        assertThat("getDestination()", notificationEntity.getDestination(), equalTo(destination));

        verify(entityManager).persist(notificationEntity);
    }

    private JobNotificationRepository createJobNotificationRepository() {
        final Properties mailSessionProperties = new Properties();
        mailSessionProperties.setProperty("mail.from", mailFrom);

        final JobNotificationRepository jobNotificationRepository = new JobNotificationRepository() {
            @Override
            protected MailNotification newMailNotification(MailDestination destination, NotificationEntity notification) throws JobStoreException {
                return new MailNotification(destination, notification) {
                    @Override
                    public void send() throws JobStoreException {
                        if(mailSession == null) throw new JobStoreException("mailSession is null");
                    }
                };
            }
        };
        jobNotificationRepository.entityManager = entityManager;
        jobNotificationRepository.mailSession = session;
        jobNotificationRepository.sessionContext = sessionContext;
        jobNotificationRepository.vipCoreServiceConnector = vipCoreServiceConnector;
        when(sessionContext.getBusinessObject(JobNotificationRepository.class)).thenReturn(jobNotificationRepository);

        return jobNotificationRepository;
    }

    public static NotificationEntity getNotificationEntity(Notification.Type type) {
        final NotificationEntity notification = new NotificationEntity();
        notification.setStatus(Notification.Status.WAITING);
        notification.setType(type);
        return notification;
    }

    public static NotificationEntity getNotificationEntity(Notification.Type type, JobSpecification jobSpecification) {
        final JobEntity jobEntity = new JobEntity();
        jobEntity.setSpecification(jobSpecification);
        jobEntity.setState(new State());
        return getNotificationEntity(type, jobEntity);
    }

    public static NotificationEntity getNotificationEntity(Notification.Type type, JobEntity jobEntity) {
        final NotificationEntity notification = getNotificationEntity(type);
        notification.setJob(jobEntity);
        return notification;
    }

    public static class NotificationContextImpl implements NotificationContext {
    }
}
