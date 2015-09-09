package dk.dbc.dataio.jobstore.types;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class JobNotificationTypeTest {
    @Test
    public void getTypeFromValue() {
        assertThat("Value 1", JobNotification.Type.getTypeFromValue((short) 1), is(JobNotification.Type.JOB_CREATED));
        assertThat("Value 2", JobNotification.Type.getTypeFromValue((short) 2), is(JobNotification.Type.JOB_COMPLETED));
    }
}