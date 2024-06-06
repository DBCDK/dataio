package dk.dbc.dataio.rrharvester.service.connector.ejb;

import dk.dbc.dataio.harvester.task.connector.HarvesterTaskServiceConnector;
import dk.dbc.httpclient.HttpClient;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.ejb.EJBException;
import jakarta.ejb.LocalBean;
import jakarta.ejb.Singleton;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// TODO: 10-07-19 replace EJB with @ApplicationScoped CDI producer

@Singleton
@LocalBean
public class RRHarvesterServiceConnectorBean {
    private static final Logger LOGGER = LoggerFactory.getLogger(RRHarvesterServiceConnectorBean.class);
    private Client client;

    HarvesterTaskServiceConnector harvesterTaskServiceConnector;

    public RRHarvesterServiceConnectorBean() {
    }

    public RRHarvesterServiceConnectorBean(String rawRepoHarvesterUrl) {
        init(rawRepoHarvesterUrl);
    }

    @PostConstruct
    public void initializeConnector() {
        init(System.getenv("RAWREPO_HARVESTER_URL"));
    }

    @SuppressWarnings("java:S2095")
    private void init(String endpoint) {
        Client client = ClientBuilder.newClient().register(new JacksonFeature());
        if (endpoint == null || endpoint.trim().isEmpty()) {
            throw new EJBException("RAWREPO_HARVESTER_URL must be set");
        }
        harvesterTaskServiceConnector = new HarvesterTaskServiceConnector(client, endpoint);
        LOGGER.info("Using service endpoint {}", endpoint);
    }

    public HarvesterTaskServiceConnector getConnector() {
        return harvesterTaskServiceConnector;
    }

    @PreDestroy
    public void tearDownConnector() {
        HttpClient.closeClient(harvesterTaskServiceConnector.getHttpClient());
    }

}
