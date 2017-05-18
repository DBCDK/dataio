package dk.dbc.dataio.jobstore.service.ejb;

import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnector;
import dk.dbc.dataio.common.utils.flowstore.ejb.FlowStoreServiceConnectorBean;
import dk.dbc.dataio.flowstore.service.connector.ejb.TestFlowStoreServiceConnector;

import javax.enterprise.inject.Alternative;

/**
 * Test FlowstoreConenctorBean for ArquillianTests
 */
@Alternative
public class TestFlowStoreServiceConnectorBean extends FlowStoreServiceConnectorBean {
    @Override
    public FlowStoreServiceConnector getConnector() {
        return new TestFlowStoreServiceConnector();
    }
}
