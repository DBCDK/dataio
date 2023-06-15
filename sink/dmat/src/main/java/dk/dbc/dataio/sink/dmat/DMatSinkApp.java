package dk.dbc.dataio.sink.dmat;

import dk.dbc.dataio.jse.artemis.common.app.MessageConsumerApp;
import dk.dbc.dataio.jse.artemis.common.service.ServiceHub;
import dk.dbc.dmat.service.connector.DMatServiceConnector;

import javax.ws.rs.client.ClientBuilder;
import java.util.function.Supplier;

/**
 * This class defines the other classes that make up this JAX-RS application by
 * having the getClasses method return a specific set of resources.
 */
public class DMatSinkApp extends MessageConsumerApp {
    private static final ServiceHub serviceHub = ServiceHub.defaultHub();
    private static final DMatServiceConnector DMAT_SERVICE_CONNECTOR = new DMatServiceConnector(ClientBuilder.newClient(), SinkConfig.DMAT_SERVICE_URL.asString());
    private static final Supplier<MessageConsumerBean> messageConsumer = () -> new MessageConsumerBean(serviceHub, DMAT_SERVICE_CONNECTOR);

    public static void main(String[] args) {
        new DMatSinkApp().go(serviceHub, messageConsumer);
    }
}
