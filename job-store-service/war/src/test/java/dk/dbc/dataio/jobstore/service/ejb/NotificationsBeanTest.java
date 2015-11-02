package dk.dbc.dataio.jobstore.service.ejb;

import dk.dbc.dataio.jobstore.types.JobError;
import dk.dbc.dataio.jsonb.JSONBContext;
import org.junit.Before;
import org.junit.Test;

import javax.ws.rs.core.Response;
import java.net.URISyntaxException;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Created by ThomasBerg on 30/10/15.
 */
public class NotificationsBeanTest {

    private NotificationsBean notificationsBean;
    private JSONBContext jsonbContext;

    private final String notificationRequestAsStringValid =
        "{\n" +
            "\t\"incompleteTransfileNotificationContext\": \n" +
            "\t{\n" +
                "\t\t\"transfileName\": \"Transfil navn\",\n" +
                "\t\t\"transfileContent\": \"Transfil indhold\"\n" +
            "\t},\n" +
            "\t\"destinationEmail\": \"tdabberg@gmail.com\", \n" +
            "\t\"notificationType\": \"INCOMPLETE_TRANSFILE\"\n" +
        "}";

    private final String notificationRequestAsStringInValid =
        "{\n" +
            "\t\"incompleteTransfileNotificationContext_ATTRIBUTE_INCOMPATIBLE_NAME\": \n" +
            "\t{\n" +
                "\t\t\"transfileName\": \"Transfil navn\",\n" +
                "\t\t\"transfileContent\": \"Transfil indhold\"\n" +
            "\t},\n" +
            "\t\"destinationEmail\": \"tdabberg@gmail.com\", \n" +
            "\t\"notificationType\": \"INCOMPLETE_TRANSFILE\"\n" +
        "}";

    @Before
    public void setup() throws URISyntaxException {
        initializeNotificationBean();
        jsonbContext = new JSONBContext();
    }

    private void initializeNotificationBean() {
        notificationsBean = new NotificationsBean();
        notificationsBean.jobNotificationRepository = mock(JobNotificationRepository.class);
    }

    @Test
    public void addNotification_returnsResponseWithStatusOk() throws Exception {

        final Response notificationResponse = notificationsBean.addNotification(notificationRequestAsStringValid);
        assertThat(notificationResponse, is(notNullValue()));
        assertThat(notificationResponse.getStatus(), is(Response.Status.OK.getStatusCode()));
        assertThat(notificationResponse.hasEntity(), is(false));
    }

    @Test
    public void addNotificationInvalidNotificationRequestJson_returnsResponseWithStatusBadRequest() throws Exception {

        final Response notificationResponse = notificationsBean.addNotification(notificationRequestAsStringInValid);
        assertThat(notificationResponse, is(notNullValue()));
        assertThat(notificationResponse.getStatus(), is(Response.Status.BAD_REQUEST.getStatusCode()));
        assertThat(notificationResponse.hasEntity(), is(true));

        final JobError jobError = jsonbContext.unmarshall((String) notificationResponse.getEntity(), JobError.class);
        assertThat("JobError", jobError, is(notNullValue()));
        assertThat("JobError code", jobError.getCode(), is(JobError.Code.INVALID_JSON));
    }
}