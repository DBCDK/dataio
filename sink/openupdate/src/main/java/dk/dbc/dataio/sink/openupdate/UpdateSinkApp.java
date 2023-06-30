package dk.dbc.dataio.sink.openupdate;

import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnector;
import dk.dbc.dataio.jse.artemis.common.app.MessageConsumerApp;
import dk.dbc.dataio.jse.artemis.common.service.ServiceHub;

import javax.ws.rs.client.ClientBuilder;
import java.util.function.Supplier;

public class UpdateSinkApp extends MessageConsumerApp {
    private static final ServiceHub serviceHub = ServiceHub.defaultHub();
    private static final FlowStoreServiceConnector FLOW_STORE_SERVICE_CONNECTOR = new FlowStoreServiceConnector(ClientBuilder.newClient(),
            SinkConfig.FLOWSTORE_URL.asString());
    private static final Supplier<OpenUpdateMessageProcessor> messageConsumer = () -> new OpenUpdateMessageProcessor(serviceHub, FLOW_STORE_SERVICE_CONNECTOR);

    public static void main(String[] args) {
        new UpdateSinkApp().go(serviceHub, messageConsumer);
    }
}
