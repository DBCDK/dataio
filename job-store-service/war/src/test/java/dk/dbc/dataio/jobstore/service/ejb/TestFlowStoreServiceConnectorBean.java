package dk.dbc.dataio.jobstore.service.ejb;

import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnector;
import dk.dbc.dataio.common.utils.flowstore.ejb.FlowStoreServiceConnectorBean;
import dk.dbc.dataio.flowstore.service.connector.ejb.TestFlowStoreServiceConnector;

import javax.annotation.PostConstruct;
import javax.ejb.LocalBean;
import javax.ejb.Singleton;
import javax.enterprise.inject.Specializes;

/**
 * Test FlowstoreConenctorBean for ArquillianTests
 */
@Specializes
@Singleton
@LocalBean
public class TestFlowStoreServiceConnectorBean extends FlowStoreServiceConnectorBean {
    @Override
    @PostConstruct
    public void initializeConnector() {}

    @Override
    public FlowStoreServiceConnector getConnector() {
        return new TestFlowStoreServiceConnector();
    }
}
