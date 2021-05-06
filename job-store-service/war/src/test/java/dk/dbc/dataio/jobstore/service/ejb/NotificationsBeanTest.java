package dk.dbc.dataio.jobstore.service.ejb;

import dk.dbc.commons.jsonb.JSONBContext;
import dk.dbc.dataio.jobstore.service.entity.NotificationEntity;
import dk.dbc.dataio.jobstore.types.AddNotificationRequest;
import dk.dbc.dataio.jobstore.types.JobError;
import dk.dbc.dataio.jobstore.types.Notification;
import org.junit.Before;
import org.junit.Test;

import javax.ws.rs.core.Response;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class NotificationsBeanTest {
    private final JobNotificationRepository jobNotificationRepository = mock(JobNotificationRepository.class);
    private final JSONBContext jsonbContext = new JSONBContext();
    private NotificationsBean notificationsBean;

    @Before
    public void setup() {
        initializeNotificationBean();
    }

    private void initializeNotificationBean() {
        notificationsBean = new NotificationsBean();
        notificationsBean.jobNotificationRepository = jobNotificationRepository;
    }

    @Test
    public void addNotification_validNotificationsRequest_returnsResponseWithStatusOk() throws Exception {
        when(jobNotificationRepository.addNotification(eq(Notification.Type.INVALID_TRANSFILE), anyString(),
                any(JobNotificationRepositoryTest.NotificationContextImpl.class)))
                .thenReturn(new NotificationEntity());

        final AddNotificationRequest request = new AddNotificationRequest("mail@company.com",
                new JobNotificationRepositoryTest.NotificationContextImpl(), Notification.Type.INVALID_TRANSFILE);
        final Response notificationResponse = notificationsBean.addNotification(jsonbContext.marshall(request));
        assertThat("Response", notificationResponse, is(notNullValue()));
        assertThat("Response status", notificationResponse.getStatus(), is(Response.Status.OK.getStatusCode()));
        assertThat("ResponseEntity", notificationResponse.hasEntity(), is(true));
    }

    @Test
    public void addNotification_invalidNotificationRequest_returnsResponseWithStatusBadRequest() throws Exception {
        final String invalid = "{}";
        final Response notificationResponse = notificationsBean.addNotification(invalid);
        assertThat(notificationResponse, is(notNullValue()));
        assertThat(notificationResponse.getStatus(), is(Response.Status.BAD_REQUEST.getStatusCode()));
        assertThat(notificationResponse.hasEntity(), is(true));

        final JobError jobError = jsonbContext.unmarshall((String) notificationResponse.getEntity(), JobError.class);
        assertThat("JobError", jobError, is(notNullValue()));
        assertThat("JobError code", jobError.getCode(), is(JobError.Code.INVALID_JSON));
    }
}