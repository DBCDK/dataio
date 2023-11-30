package dk.dbc.dataio.common.utils.flowstore.ejb;

import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class FlowStoreServiceConnectorBeanTest {
    @Test
    public void initializeConnector_flowstoreUrlNotSet_throws() {
        assertThrows(NullPointerException.class, FlowStoreServiceConnectorBean::new);
    }

    @Test
    public void initializeConnector() {
        FlowStoreServiceConnectorBean flowStoreServiceConnectorBean = newFlowStoreServiceConnectorBean();
        assertThat(flowStoreServiceConnectorBean.getConnector(), not(nullValue()));
    }

    private FlowStoreServiceConnectorBean newFlowStoreServiceConnectorBean() {
        return new FlowStoreServiceConnectorBean("http://test");
    }
}
