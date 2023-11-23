package dk.dbc.dataio.jobstore.types;

import dk.dbc.commons.jsonb.JSONBContext;
import dk.dbc.commons.jsonb.JSONBException;
import org.junit.jupiter.api.Test;

import java.util.Date;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class NotificationTest {
    @Test
    public void jsonMarshallingUnmarshalling() throws JSONBException {
        final JSONBContext jsonbContext = new JSONBContext();
        final Notification notification = new Notification()
                .withId(42)
                .withTimeOfCreation(new Date())
                .withContext(new InvalidTransfileNotificationContext(
                        "filename", "content", "invalid"))
                .withContent("test");
        final String marshalled = jsonbContext.marshall(notification);
        final Notification unmarshalled = jsonbContext.unmarshall(marshalled, Notification.class);
        assertThat(unmarshalled, is(notification));
    }

    @Test
    public void getTypeFromValue() {
        assertThat("Value 1", Notification.Type.of((short) 1), is(Notification.Type.JOB_CREATED));
        assertThat("Value 2", Notification.Type.of((short) 2), is(Notification.Type.JOB_COMPLETED));
        assertThat("Value 4", Notification.Type.of((short) 4), is(Notification.Type.INVALID_TRANSFILE));
    }

    @Test
    public void getStatusFromValue() {
        assertThat("Value 1", Notification.Status.of((short) 1), is(Notification.Status.WAITING));
        assertThat("Value 2", Notification.Status.of((short) 2), is(Notification.Status.COMPLETED));
        assertThat("Value 3", Notification.Status.of((short) 3), is(Notification.Status.FAILED));
    }
}
