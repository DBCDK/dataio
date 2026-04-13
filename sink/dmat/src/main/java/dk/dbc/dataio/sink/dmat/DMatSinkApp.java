package dk.dbc.dataio.sink.dmat;

import dk.dbc.commons.useragent.UserAgent;
import dk.dbc.dataio.jse.artemis.common.app.MessageConsumerApp;
import dk.dbc.dataio.jse.artemis.common.service.ServiceHub;
import dk.dbc.dmat.service.connector.DMatServiceConnector;
import dk.dbc.dmat.service.connector.JacksonConfig;
import dk.dbc.httpclient.FailSafeHttpClient;
import dk.dbc.httpclient.HttpClient;
import jakarta.ws.rs.client.Client;
import net.jodah.failsafe.RetryPolicy;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.jackson.JacksonFeature;

import java.util.function.Supplier;

/**
 * This class defines the other classes that make up this JAX-RS application by
 * having the getClasses method return a specific set of resources.
 */
public class DMatSinkApp extends MessageConsumerApp {
    private static final ServiceHub serviceHub = ServiceHub.defaultHub();
    private static final DMatServiceConnector DMAT_SERVICE_CONNECTOR = createDmatServiceConnector(SinkConfig.DMAT_SERVICE_URL.asString());
    private static final Supplier<DMatMessageConsumer> messageConsumer = () -> new DMatMessageConsumer(serviceHub, DMAT_SERVICE_CONNECTOR);

    public static void main(String[] args) {
        new DMatSinkApp().go(serviceHub, messageConsumer);
    }

    private static DMatServiceConnector createDmatServiceConnector(String dmatServiceBaseUrl) {
        Client client = HttpClient.newClient((new ClientConfig()).register(new JacksonConfig()).register(new JacksonFeature()));
        return new DMatServiceConnector(FailSafeHttpClient.create(client, UserAgent.forInternalRequests(), new RetryPolicy()), dmatServiceBaseUrl);
    }
}
