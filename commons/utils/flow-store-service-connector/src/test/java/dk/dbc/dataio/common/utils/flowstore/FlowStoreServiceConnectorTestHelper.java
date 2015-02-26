package dk.dbc.dataio.common.utils.flowstore;

import javax.ws.rs.client.Client;

import static org.powermock.api.mockito.PowerMockito.mock;

public class FlowStoreServiceConnectorTestHelper {
    public static final Client CLIENT = mock(Client.class);
    public static final String FLOW_STORE_URL = "http://dataio/flow-store";
    public static final long ID = 1;
    public static final long NUMBER = 1234567;
    public static final long VERSION = 1;

    public static FlowStoreServiceConnector newFlowStoreServiceConnector() {
        return new FlowStoreServiceConnector(CLIENT, FLOW_STORE_URL);
    }
}
