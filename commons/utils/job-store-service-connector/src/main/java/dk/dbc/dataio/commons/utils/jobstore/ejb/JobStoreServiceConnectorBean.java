package dk.dbc.dataio.commons.utils.jobstore.ejb;

import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnector;
import dk.dbc.httpclient.HttpClient;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.LocalBean;
import javax.ejb.Singleton;
import javax.inject.Inject;
import javax.ws.rs.client.Client;

// TODO: 05-07-19 replace EJB with @ApplicationScoped CDI producer

@Singleton
@LocalBean
public class JobStoreServiceConnectorBean {
    private static final Logger LOGGER = LoggerFactory.getLogger(JobStoreServiceConnectorBean.class);

    JobStoreServiceConnector jobStoreServiceConnector;

    @Inject
    private MetricRegistry metricRegistry;

    @PostConstruct
    public void initializeConnector() {
        LOGGER.debug("Initializing connector");
        final Client client = HttpClient.newClient(new ClientConfig()
                .register(new JacksonFeature()));
        final String endpoint = System.getenv("JOBSTORE_URL");
        jobStoreServiceConnector = new JobStoreServiceConnector(client, endpoint, metricRegistry);
        LOGGER.info("Using service endpoint {}", endpoint);
    }

    public JobStoreServiceConnector getConnector() {
        return jobStoreServiceConnector;
    }

    @PreDestroy
    public void tearDownConnector() {
        HttpClient.closeClient(jobStoreServiceConnector.getClient());
    }
}
