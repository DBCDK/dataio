package dk.dbc.dataio.harvester.connector.ejb;

import dk.dbc.dataio.harvester.connector.TickleHarvesterServiceConnector;
import dk.dbc.dataio.harvester.task.connector.HarvesterTaskServiceConnector;
import dk.dbc.httpclient.HttpClient;
import jakarta.annotation.PreDestroy;
import jakarta.ejb.LocalBean;
import jakarta.ejb.Singleton;
import jakarta.ws.rs.client.Client;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// TODO: 11-07-19 replace EJB with @ApplicationScoped CDI producer

@Singleton
@LocalBean
public class TickleHarvesterServiceConnectorBean {
    private static final Logger LOGGER = LoggerFactory.getLogger(TickleHarvesterServiceConnectorBean.class);

    private final TickleHarvesterServiceConnector connector;

    public TickleHarvesterServiceConnectorBean() {
        this(System.getenv("TICKLE_REPO_HARVESTER_URL"));
    }

    public TickleHarvesterServiceConnectorBean(String tickleRepoHarvesterUrl) {
        Client client = HttpClient.newClient(new ClientConfig().register(new JacksonFeature()));
        connector = new TickleHarvesterServiceConnector(client, tickleRepoHarvesterUrl);
        LOGGER.info("Using service endpoint {}", tickleRepoHarvesterUrl);
    }

    public HarvesterTaskServiceConnector getConnector() {
        return connector;
    }

    @PreDestroy
    public void tearDownConnector() {
        HttpClient.closeClient(connector.getHttpClient());
    }
}
