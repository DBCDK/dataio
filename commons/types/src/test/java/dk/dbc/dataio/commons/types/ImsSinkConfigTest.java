package dk.dbc.dataio.commons.types;

import dk.dbc.commons.jsonb.JSONBContext;
import dk.dbc.commons.jsonb.JSONBException;
import org.junit.Assert;
import org.junit.Test;

import static dk.dbc.commons.testutil.Assert.assertThat;
import static dk.dbc.commons.testutil.Assert.isThrowing;
import static org.hamcrest.CoreMatchers.is;

public class ImsSinkConfigTest {

    private static final String ENDPOINT = "endpoint";

    @Test
    public void constructor_endpointArgIsNull_throws() {
        assertThat(() -> new ImsSinkConfig().withEndpoint(null), isThrowing(NullPointerException.class));
    }

    @Test
    public void constructor_allArgsAreValid_returnsNewInstanceWithDefaultValuesSet() {
        final ImsSinkConfig sinkConfig = new ImsSinkConfig().withEndpoint(ENDPOINT);
        Assert.assertThat(sinkConfig.getEndpoint(), is(ENDPOINT));
    }

    @Test
    public void marshalling() throws JSONBException {
        final JSONBContext jsonbContext = new JSONBContext();
        final ImsSinkConfig sinkConfig = new ImsSinkConfig();
        final ImsSinkConfig unmarshalled = jsonbContext.unmarshall(jsonbContext.marshall(sinkConfig), ImsSinkConfig.class);
        Assert.assertThat(unmarshalled, is(sinkConfig));
    }
}
