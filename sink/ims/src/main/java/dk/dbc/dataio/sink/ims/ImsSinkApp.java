package dk.dbc.dataio.sink.ims;

import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnector;
import dk.dbc.dataio.jse.artemis.common.app.MessageConsumerApp;
import dk.dbc.dataio.jse.artemis.common.service.ServiceHub;

import javax.ws.rs.client.ClientBuilder;
import java.util.function.Supplier;

import static dk.dbc.dataio.sink.ims.SinkConfig.FLOWSTORE_URL;

public class ImsSinkApp extends MessageConsumerApp {
    private static final ServiceHub serviceHub = ServiceHub.defaultHub();
    private static final ImsConfig imsConfig = new ImsConfig(new FlowStoreServiceConnector(ClientBuilder.newClient(), FLOWSTORE_URL.asString()));
    private static final Supplier<ImsMessageConsumer> messageConsumer = () -> new ImsMessageConsumer(serviceHub, imsConfig);

    public static void main(String[] args) {
        new ImsSinkApp().go(serviceHub, messageConsumer);
    }
}
