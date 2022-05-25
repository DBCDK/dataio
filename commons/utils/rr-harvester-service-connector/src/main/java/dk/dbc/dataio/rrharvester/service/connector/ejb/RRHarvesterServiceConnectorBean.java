package dk.dbc.dataio.rrharvester.service.connector.ejb;

import dk.dbc.dataio.harvester.task.connector.HarvesterTaskServiceConnector;
import dk.dbc.httpclient.HttpClient;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.EJBException;
import javax.ejb.LocalBean;
import javax.ejb.Singleton;
import javax.ws.rs.client.Client;

// TODO: 10-07-19 replace EJB with @ApplicationScoped CDI producer

@Singleton
@LocalBean
public class RRHarvesterServiceConnectorBean {
    private static final Logger LOGGER = LoggerFactory.getLogger(RRHarvesterServiceConnectorBean.class);

    HarvesterTaskServiceConnector harvesterTaskServiceConnector;

    @PostConstruct
    public void initializeConnector() {
        final Client client = HttpClient.newClient(
                new ClientConfig()
                        .register(new JacksonFeature()));
        final String endpoint = System.getenv("RAWREPO_HARVESTER_URL");
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
