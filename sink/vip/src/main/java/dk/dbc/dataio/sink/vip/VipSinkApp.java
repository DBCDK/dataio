package dk.dbc.dataio.sink.vip;

import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnector;
import dk.dbc.dataio.jse.artemis.common.app.MessageConsumerApp;
import dk.dbc.dataio.jse.artemis.common.service.ServiceHub;
import jakarta.ws.rs.client.ClientBuilder;
import org.glassfish.jersey.jackson.JacksonFeature;

import java.util.function.Supplier;

public class VipSinkApp extends MessageConsumerApp {
    private static final ServiceHub serviceHub = ServiceHub.defaultHub();
    private static final  FlowStoreServiceConnector FLOW_STORE = new FlowStoreServiceConnector(ClientBuilder.newClient().register(new JacksonFeature()),
            SinkConfig.FLOWSTORE_URL.asString());
    private static final ConfigBean CONFIG_BEAN = new ConfigBean(FLOW_STORE);
    private static final Supplier<VipMessageConsumer> messageConsumer = () -> new VipMessageConsumer(serviceHub, CONFIG_BEAN);

    public static void main(String[] args) {
        new VipSinkApp().go(serviceHub, messageConsumer);
    }
}
