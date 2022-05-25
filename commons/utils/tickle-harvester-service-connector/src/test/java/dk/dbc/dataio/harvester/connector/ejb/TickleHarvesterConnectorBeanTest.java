package dk.dbc.dataio.harvester.connector.ejb;

import dk.dbc.dataio.harvester.connector.TickleHarvesterServiceConnector;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.EnvironmentVariables;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.mockito.Mockito.mock;


public class TickleHarvesterConnectorBeanTest {
    @Rule
    public final EnvironmentVariables environmentVariables = new EnvironmentVariables();

    @Test(expected = NullPointerException.class)
    public void initializeConnector_environmentNotSet_throws() {
        final TickleHarvesterServiceConnectorBean bean = new TickleHarvesterServiceConnectorBean();
        bean.initializeConnector();
    }

    @Test
    public void initializeConnector() {
        environmentVariables.set("TICKLE_REPO_HARVESTER_URL", "http://test");
        final TickleHarvesterServiceConnectorBean bean = new TickleHarvesterServiceConnectorBean();
        bean.initializeConnector();

        assertThat(bean.getConnector(), not(nullValue()));
    }

    @Test
    public void getConnector() {
        final TickleHarvesterServiceConnector tickleHarvesterServiceConnector =
                mock(TickleHarvesterServiceConnector.class);
        final TickleHarvesterServiceConnectorBean bean = new TickleHarvesterServiceConnectorBean();
        bean.connector = tickleHarvesterServiceConnector;

        assertThat(bean.getConnector(), is(tickleHarvesterServiceConnector));
    }
}
