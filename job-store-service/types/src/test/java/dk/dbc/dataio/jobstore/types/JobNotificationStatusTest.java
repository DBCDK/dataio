package dk.dbc.dataio.jobstore.types;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class JobNotificationStatusTest {
    @Test
    public void getStatusFromValue() {
        assertThat("Value 1", JobNotification.Status.getStatusFromValue((short) 1), is(JobNotification.Status.WAITING));
        assertThat("Value 2", JobNotification.Status.getStatusFromValue((short) 2), is(JobNotification.Status.COMPLETED));
        assertThat("Value 3", JobNotification.Status.getStatusFromValue((short) 3), is(JobNotification.Status.FAILED));
    }
}