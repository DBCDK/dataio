package dk.dbc.dataio.sink.dmatdm3;

import dk.dbc.commons.useragent.UserAgent;
import dk.dbc.dataio.jse.artemis.common.app.MessageConsumerApp;
import dk.dbc.dataio.jse.artemis.common.service.ServiceHub;
import dk.dbc.dmat.service.connector.DMatServiceConnector;
import dk.dbc.dmat.service.connector.JacksonConfig;
import dk.dbc.httpclient.FailSafeHttpClient;
import dk.dbc.httpclient.HttpClient;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.core.Response;
import net.jodah.failsafe.RetryPolicy;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.jackson.JacksonFeature;

import java.time.Duration;
import java.util.function.Supplier;

/**
 * Main application class for the DMat sink message consumer.
 *
 * This application extends MessageConsumerApp to consume messages from a message queue
 * and forward them to a DMat service. It sets up the necessary infrastructure including
 * service connections, HTTP client configuration with retry policies, and message consumer
 * instances.
 *
 * The application configures a fail-safe HTTP client that automatically retries requests
 * on certain failure conditions (404, 500, 502 status codes and processing exceptions)
 * with a delay of 5 seconds between retries, up to a maximum of 3 retry attempts.
 *
 * The DMat service URL is obtained from the SinkConfig.DMAT_SERVICE_URL configuration.
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
        return new DMatServiceConnector(FailSafeHttpClient
                .create(client, UserAgent.forInternalRequests(),
                        new RetryPolicy<Response>().handle(ProcessingException .class)
                                .handleResultIf(response ->
                                        response.getStatus() == 404
                                                || response.getStatus() == 500
                                                || response.getStatus() == 502)
                                .withDelay(Duration.ofSeconds(5))
                                .withMaxRetries(3)), dmatServiceBaseUrl);
    }
}