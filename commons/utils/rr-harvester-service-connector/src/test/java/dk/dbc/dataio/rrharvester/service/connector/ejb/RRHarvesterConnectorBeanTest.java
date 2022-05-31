package dk.dbc.dataio.rrharvester.service.connector.ejb;

import dk.dbc.dataio.harvester.task.connector.HarvesterTaskServiceConnector;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.EnvironmentVariables;

import javax.ejb.EJBException;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.mockito.Mockito.mock;


public class RRHarvesterConnectorBeanTest {
    @Rule
    public final EnvironmentVariables environmentVariables = new EnvironmentVariables();

    @Test(expected = EJBException.class)
    public void initializeConnector_environmentNotSet_throws() {
        newRRHarvesterServiceConnectorBean().initializeConnector();
    }

    @Test
    public void initializeConnector() {
        environmentVariables.set("RAWREPO_HARVESTER_URL", "http://test");
        final RRHarvesterServiceConnectorBean rrHarvesterServiceConnectorBean =
                newRRHarvesterServiceConnectorBean();
        rrHarvesterServiceConnectorBean.initializeConnector();

        assertThat(rrHarvesterServiceConnectorBean.getConnector(), not(nullValue()));
    }

    @Test
    public void getConnector() {
        final HarvesterTaskServiceConnector rrHarvesterServiceConnector =
                mock(HarvesterTaskServiceConnector.class);
        final RRHarvesterServiceConnectorBean rrHarvesterServiceConnectorBean =
                newRRHarvesterServiceConnectorBean();
        rrHarvesterServiceConnectorBean.harvesterTaskServiceConnector = rrHarvesterServiceConnector;

        assertThat(rrHarvesterServiceConnectorBean.getConnector(),
                is(rrHarvesterServiceConnector));
    }

    private RRHarvesterServiceConnectorBean newRRHarvesterServiceConnectorBean() {
        return new RRHarvesterServiceConnectorBean();
    }
}
