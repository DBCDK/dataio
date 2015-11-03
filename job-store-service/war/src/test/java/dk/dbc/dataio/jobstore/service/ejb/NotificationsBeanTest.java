package dk.dbc.dataio.jobstore.service.ejb;

import dk.dbc.dataio.jobstore.service.entity.NotificationEntity;
import dk.dbc.dataio.jobstore.types.AddNotificationRequest;
import dk.dbc.dataio.jobstore.types.IncompleteTransfileNotificationContext;
import dk.dbc.dataio.jobstore.types.JobError;
import dk.dbc.dataio.jobstore.types.JobNotification;
import dk.dbc.dataio.jsonb.JSONBContext;
import org.junit.Before;
import org.junit.Test;

import javax.ws.rs.core.Response;
import java.net.URISyntaxException;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class NotificationsBeanTest {
    private final JobNotificationRepository jobNotificationRepository = mock(JobNotificationRepository.class);
    private final JSONBContext jsonbContext = new JSONBContext();
    private NotificationsBean notificationsBean;

    @Before
    public void setup() throws URISyntaxException {
        initializeNotificationBean();
    }

    private void initializeNotificationBean() {
        notificationsBean = new NotificationsBean();
        notificationsBean.jobNotificationRepository = jobNotificationRepository;
    }

    @Test
    public void addNotification_validNotificationsRequest_returnsResponseWithStatusOk() throws Exception {
        when(jobNotificationRepository.addNotification(eq(JobNotification.Type.INCOMPLETE_TRANSFILE), anyString(), anyString()))
                .thenReturn(new NotificationEntity());

        final IncompleteTransfileNotificationContext context = new IncompleteTransfileNotificationContext("name", "content");
        final AddNotificationRequest request = new AddNotificationRequest("mail@company.com", context, JobNotification.Type.INCOMPLETE_TRANSFILE);
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