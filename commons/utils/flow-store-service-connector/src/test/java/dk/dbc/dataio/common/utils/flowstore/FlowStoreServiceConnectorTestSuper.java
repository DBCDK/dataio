package dk.dbc.dataio.common.utils.flowstore;


import javax.ws.rs.client.Client;
import static org.powermock.api.mockito.PowerMockito.mock;


public class FlowStoreServiceConnectorTestSuper {
    static final Client CLIENT = mock(Client.class);
    static final String FLOW_STORE_URL = "http://dataio/flow-store";
    static final long ID = 1;
    static final long VERSION = 1;

    public static FlowStoreServiceConnector newFlowStoreServiceConnector() {
        return new FlowStoreServiceConnector(CLIENT, FLOW_STORE_URL);
    }
}
