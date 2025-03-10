package dk.dbc.dataio.commons.utils.jobstore.ejb;

import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnector;
import dk.dbc.httpclient.HttpClient;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.ejb.LocalBean;
import jakarta.ejb.Singleton;
import jakarta.inject.Inject;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// TODO: 05-07-19 replace EJB with @ApplicationScoped CDI producer

@Singleton
@LocalBean
public class JobStoreServiceConnectorBean {
    private static final Logger LOGGER = LoggerFactory.getLogger(JobStoreServiceConnectorBean.class);
    JobStoreServiceConnector jobStoreServiceConnector;

    @Inject
    private MetricRegistry metricRegistry;

    public JobStoreServiceConnectorBean() {
    }

    public JobStoreServiceConnectorBean(JobStoreServiceConnector jobStoreServiceConnector) {
        this.jobStoreServiceConnector = jobStoreServiceConnector;
    }

    @PostConstruct
    public void initializeConnector() {
        LOGGER.debug("Initializing connector");
        final String endpoint = System.getenv("JOBSTORE_URL");
        jobStoreServiceConnector = new JobStoreServiceConnector(endpoint, metricRegistry);
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
