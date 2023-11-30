package dk.dbc.dataio.common.utils.flowstore.ejb;

import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnector;
import dk.dbc.httpclient.HttpClient;
import jakarta.annotation.PreDestroy;
import jakarta.ejb.LocalBean;
import jakarta.ejb.Singleton;
import jakarta.ws.rs.client.Client;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// TODO: 05-07-19 replace EJB with @ApplicationScoped CDI producer

@Singleton
@LocalBean
public class FlowStoreServiceConnectorBean {
    private static final Logger LOGGER = LoggerFactory.getLogger(FlowStoreServiceConnectorBean.class);
    private final FlowStoreServiceConnector flowStoreServiceConnector;

    public FlowStoreServiceConnectorBean() {
        this(System.getenv("FLOWSTORE_URL"));
    }

    public FlowStoreServiceConnectorBean(String flowsStoreUrl) {
        LOGGER.debug("Initializing connector");
        Client client = HttpClient.newClient(new ClientConfig().register(new JacksonFeature()));
        flowStoreServiceConnector = new FlowStoreServiceConnector(client, flowsStoreUrl);
        LOGGER.info("Using service endpoint {}", flowsStoreUrl);
    }

    public FlowStoreServiceConnector getConnector() {
        return flowStoreServiceConnector;
    }

    @PreDestroy
    public void tearDownConnector() {
        HttpClient.closeClient(flowStoreServiceConnector.getClient());
    }
}
