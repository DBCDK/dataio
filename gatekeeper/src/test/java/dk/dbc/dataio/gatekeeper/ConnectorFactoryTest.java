package dk.dbc.dataio.gatekeeper;

import org.junit.Test;

public class ConnectorFactoryTest {
    @Test(expected = NullPointerException.class)
    public void constructor_fileStoreServiceEndpointArgIsNull_throws() {
        new ConnectorFactory(null, "jobStoreServiceEndpoint");
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructor_fileStoreServiceEndpointArgIsEmpty_throws() {
        new ConnectorFactory(" ", "jobStoreServiceEndpoint");
    }

    @Test(expected = NullPointerException.class)
    public void constructor_jobStoreServiceEndpointArgIsNull_throws() {
        new ConnectorFactory("fileStoreServiceEndpoint", null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructor_jobStoreServiceEndpointArgIsEmpty_throws() {
        new ConnectorFactory("fileStoreServiceEndpoint", " ");
    }

}
