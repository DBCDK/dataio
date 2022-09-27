package dk.dbc.dataio.commons.utils.jobstore.ejb;

import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnector;
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

// TODO: 05-07-19 replace EJB with @ApplicationScoped CDI producer

@Singleton
@LocalBean
public class JobStoreServiceConnectorBean {
    private static final Logger LOGGER = LoggerFactory.getLogger(JobStoreServiceConnectorBean.class);

    JobStoreServiceConnector jobStoreServiceConnector;

    @PostConstruct
    public void initializeConnector() {
        try {
            LOGGER.debug("Initializing connector");
            final Client client = HttpClient.newClient(new ClientConfig()
                    .register(new JacksonFeature()));
            final String endpoint = System.getenv("JOBSTORE_URL");
            jobStoreServiceConnector = new JobStoreServiceConnector(client, endpoint);
            LOGGER.info("Using service endpoint {}", endpoint);
        } catch (Exception e) {
            LOGGER.error("Shit happened", e);
        }
    }

    public JobStoreServiceConnector getConnector() {
        return jobStoreServiceConnector;
    }

    @PreDestroy
    public void tearDownConnector() {
        HttpClient.closeClient(jobStoreServiceConnector.getClient());
    }
}
