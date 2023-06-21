package dk.dbc.dataio.sink.holdingsitems;

import dk.dbc.dataio.jse.artemis.common.app.MessageConsumerApp;
import dk.dbc.dataio.jse.artemis.common.service.ServiceHub;
import dk.dbc.solrdocstore.connector.SolrDocStoreConnector;

import javax.ws.rs.client.ClientBuilder;
import java.util.function.Supplier;

public class HoldingItemsSinkApp extends MessageConsumerApp {
    private static final ServiceHub SERVICE_HUB = ServiceHub.defaultHub();
    private static final SolrDocStoreConnector SOLR_DOC_STORE = new SolrDocStoreConnector(ClientBuilder.newClient(), SinkConfig.SOLR_DOC_STORE_SERVICE_URL.asPGJDBCUrl());
    private static final HoldingsItemsUnmarshaller HOLDINGS_ITEMS_UNMARSHALLER = new HoldingsItemsUnmarshaller(SOLR_DOC_STORE);
    private static final Supplier<MessageConsumer> MESSAGE_CONSUMER = () -> new MessageConsumer(SERVICE_HUB, SOLR_DOC_STORE, HOLDINGS_ITEMS_UNMARSHALLER);

    public static void main(String[] args) {
        new HoldingItemsSinkApp().go(SERVICE_HUB, MESSAGE_CONSUMER);
    }
}
