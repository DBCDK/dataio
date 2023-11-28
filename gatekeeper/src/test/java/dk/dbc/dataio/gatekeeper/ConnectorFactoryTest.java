package dk.dbc.dataio.gatekeeper;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

public class ConnectorFactoryTest {
    @Test
    public void constructor_fileStoreServiceEndpointArgIsNull_throws() {
        assertThrows(NullPointerException.class, () -> new ConnectorFactory(null, "jobStoreServiceEndpoint"));
    }

    @Test
    public void constructor_fileStoreServiceEndpointArgIsEmpty_throws() {
        assertThrows(IllegalArgumentException.class, () -> new ConnectorFactory(" ", "jobStoreServiceEndpoint"));
    }

    @Test
    public void constructor_jobStoreServiceEndpointArgIsNull_throws() {
        assertThrows(NullPointerException.class, () -> new ConnectorFactory("fileStoreServiceEndpoint", null));
    }

    @Test
    public void constructor_jobStoreServiceEndpointArgIsEmpty_throws() {
        assertThrows(IllegalArgumentException.class, () -> new ConnectorFactory("fileStoreServiceEndpoint", " "));
    }

}
