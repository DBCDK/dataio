package dk.dbc.dataio.harvester.connector.ejb;

import dk.dbc.dataio.harvester.connector.TickleHarvesterServiceConnector;
import dk.dbc.dataio.harvester.task.connector.HarvesterTaskServiceConnector;
import dk.dbc.httpclient.HttpClient;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.LocalBean;
import javax.ejb.Singleton;
import javax.ws.rs.client.Client;

// TODO: 11-07-19 replace EJB with @ApplicationScoped CDI producer

@Singleton
@LocalBean
public class TickleHarvesterServiceConnectorBean {
    private static final Logger LOGGER = LoggerFactory.getLogger(TickleHarvesterServiceConnectorBean.class);

    TickleHarvesterServiceConnector connector;

    @PostConstruct
    public void initializeConnector() {
        final Client client = HttpClient.newClient(new ClientConfig()
                .register(new JacksonFeature()));
        final String endpoint = System.getenv("TICKLE_REPO_HARVESTER_URL");
        connector = new TickleHarvesterServiceConnector(client, endpoint);
        LOGGER.info("Using service endpoint {}", endpoint);
    }

    public HarvesterTaskServiceConnector getConnector() {
        return connector;
    }

    @PreDestroy
    public void tearDownConnector() {
        HttpClient.closeClient(connector.getHttpClient());
    }
}
