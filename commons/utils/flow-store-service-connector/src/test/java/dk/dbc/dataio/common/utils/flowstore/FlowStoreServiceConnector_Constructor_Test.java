package dk.dbc.dataio.common.utils.flowstore;

import static dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnectorTestSuper.FLOW_STORE_URL;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import org.junit.Test;

public class FlowStoreServiceConnector_Constructor_Test extends FlowStoreServiceConnectorTestSuper {

    @Test(expected = NullPointerException.class)
    public void constructor_httpClientArgIsNull_throws() {
        new FlowStoreServiceConnector(null, FLOW_STORE_URL);
    }

    @Test(expected = NullPointerException.class)
    public void constructor_baseUrlArgIsNull_throws() {
        new FlowStoreServiceConnector(CLIENT, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructor_baseUrlArgIsEmpty_throws() {
        new FlowStoreServiceConnector(CLIENT, "");
    }

    @Test
    public void constructor_allArgsAreValid_returnsNewInstance() {
        final FlowStoreServiceConnector instance = newFlowStoreServiceConnector();
        assertThat(instance, is(notNullValue()));
        assertThat(instance.getHttpClient(), is(CLIENT));
        assertThat(instance.getBaseUrl(), is(FLOW_STORE_URL));
    }

}
