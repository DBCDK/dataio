package dk.dbc.dataio.common.utils.flowstore.ejb;

import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnector;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.EnvironmentVariables;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.mockito.Mockito.mock;

public class FlowStoreServiceConnectorBeanTest {
    @Rule
    public final EnvironmentVariables environmentVariables = new EnvironmentVariables();

    @Test(expected = NullPointerException.class)
    public void initializeConnector_environmentNotSet_throws() {
        final FlowStoreServiceConnectorBean jobStoreServiceConnectorBean = newFlowStoreServiceConnectorBean();
        jobStoreServiceConnectorBean.initializeConnector();
    }

    @Test
    public void initializeConnector() {
        environmentVariables.set("FLOWSTORE_URL", "http://test");
        final FlowStoreServiceConnectorBean flowStoreServiceConnectorBean =
                newFlowStoreServiceConnectorBean();
        flowStoreServiceConnectorBean.initializeConnector();

        assertThat(flowStoreServiceConnectorBean.getConnector(), not(nullValue()));
    }

    @Test
    public void getConnector() {
        final FlowStoreServiceConnector flowStoreServiceConnector =
                mock(FlowStoreServiceConnector.class);
        final FlowStoreServiceConnectorBean flowStoreServiceConnectorBean =
                newFlowStoreServiceConnectorBean();
        flowStoreServiceConnectorBean.flowStoreServiceConnector = flowStoreServiceConnector;

        assertThat(flowStoreServiceConnectorBean.getConnector(),
                is(flowStoreServiceConnector));
    }

    private FlowStoreServiceConnectorBean newFlowStoreServiceConnectorBean() {
        return new FlowStoreServiceConnectorBean();
    }
}
