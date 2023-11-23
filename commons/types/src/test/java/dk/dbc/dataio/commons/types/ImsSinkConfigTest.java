package dk.dbc.dataio.commons.types;

import dk.dbc.commons.jsonb.JSONBContext;
import dk.dbc.commons.jsonb.JSONBException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static dk.dbc.commons.testutil.Assert.assertThat;
import static dk.dbc.commons.testutil.Assert.isThrowing;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class ImsSinkConfigTest {

    private static final String ENDPOINT = "endpoint";

    @Test
    public void constructor_endpointArgIsNull_throws() {
        assertThat(() -> new ImsSinkConfig().withEndpoint(null), isThrowing(NullPointerException.class));
    }

    @Test
    public void constructor_allArgsAreValid_returnsNewInstanceWithDefaultValuesSet() {
        ImsSinkConfig sinkConfig = new ImsSinkConfig().withEndpoint(ENDPOINT);
        assertEquals(sinkConfig.getEndpoint(), ENDPOINT);
    }

    @Test
    public void marshalling() throws JSONBException {
        JSONBContext jsonbContext = new JSONBContext();
        ImsSinkConfig sinkConfig = new ImsSinkConfig();
        ImsSinkConfig unmarshalled = jsonbContext.unmarshall(jsonbContext.marshall(sinkConfig), ImsSinkConfig.class);
        Assertions.assertEquals(unmarshalled, sinkConfig);
    }
}
