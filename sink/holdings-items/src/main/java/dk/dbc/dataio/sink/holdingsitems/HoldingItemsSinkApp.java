package dk.dbc.dataio.sink.holdingsitems;

import dk.dbc.commons.useragent.UserAgent;
import dk.dbc.dataio.jse.artemis.common.app.MessageConsumerApp;
import dk.dbc.dataio.jse.artemis.common.service.ServiceHub;
import dk.dbc.httpclient.FailSafeHttpClient;
import dk.dbc.solrdocstore.connector.SolrDocStoreConnector;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.core.Response;
import net.jodah.failsafe.RetryPolicy;
import org.glassfish.jersey.jackson.JacksonFeature;

import java.time.Duration;
import java.util.function.Supplier;

public class HoldingItemsSinkApp extends MessageConsumerApp {

    private static final RetryPolicy<Response> RETRY_POLICY = new RetryPolicy<Response>()
            .handle(ProcessingException.class)
            .handleResultIf(response ->
                    response.getStatus() == 404
                            || response.getStatus() == 500
                            || response.getStatus() == 502)
            .withDelay(Duration.ofSeconds(5))
            .withMaxRetries(3);

    private static final ServiceHub SERVICE_HUB = ServiceHub.defaultHub();
    private static final SolrDocStoreConnector SOLR_DOC_STORE = createSolrDocStoreConnector(SinkConfig.SOLR_DOC_STORE_SERVICE_URL.asString());
    private static final HoldingsItemsUnmarshaller HOLDINGS_ITEMS_UNMARSHALLER = new HoldingsItemsUnmarshaller(SOLR_DOC_STORE);
    private static final Supplier<MessageConsumer> MESSAGE_CONSUMER = () -> new MessageConsumer(SERVICE_HUB, SOLR_DOC_STORE, HOLDINGS_ITEMS_UNMARSHALLER);

    public static void main(String[] args) {
        new HoldingItemsSinkApp().go(SERVICE_HUB, MESSAGE_CONSUMER);
    }

    private static SolrDocStoreConnector createSolrDocStoreConnector(String solrDocStoreBaseUrl) {
        final Client client = ClientBuilder.newClient().register(new JacksonFeature());
        final FailSafeHttpClient failSafeHttpClient = FailSafeHttpClient.create(client, UserAgent.forInternalRequests(), RETRY_POLICY);
        return new SolrDocStoreConnector(failSafeHttpClient, solrDocStoreBaseUrl);
    }
}
