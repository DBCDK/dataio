package dk.dbc.dataio.commons.utils.jobstore.ejb;

import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnector;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.EnvironmentVariables;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.mock;

public class JobStoreServiceConnectorBeanTest {
    @Rule
    public final EnvironmentVariables environmentVariables = new EnvironmentVariables();

    @Test(expected = NullPointerException.class)
    public void initializeConnector_environmentNotSet_throws() {
        newJobStoreServiceConnectorBean().initializeConnector();
    }

    @Test
    public void initializeConnector() {
        environmentVariables.set("JOBSTORE_URL", "http://test");
        final JobStoreServiceConnectorBean jobStoreServiceConnectorBean =
                newJobStoreServiceConnectorBean();
        jobStoreServiceConnectorBean.initializeConnector();

        assertThat(jobStoreServiceConnectorBean.getConnector(), not(nullValue()));
    }

    @Test
    public void getConnector() {
        final JobStoreServiceConnector jobStoreServiceConnector =
                mock(JobStoreServiceConnector.class);
        final JobStoreServiceConnectorBean jobStoreServiceConnectorBean =
                newJobStoreServiceConnectorBean();
        jobStoreServiceConnectorBean.jobStoreServiceConnector = jobStoreServiceConnector;

        assertThat(jobStoreServiceConnectorBean.getConnector(),
                is(jobStoreServiceConnector));
    }

    private JobStoreServiceConnectorBean newJobStoreServiceConnectorBean() {
        return new JobStoreServiceConnectorBean();
    }
}
