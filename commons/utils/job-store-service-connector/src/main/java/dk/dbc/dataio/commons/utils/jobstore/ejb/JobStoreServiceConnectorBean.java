package dk.dbc.dataio.commons.utils.jobstore.ejb;

import dk.dbc.dataio.commons.types.JobInfo;
import dk.dbc.dataio.commons.types.JobSpecification;
import dk.dbc.dataio.commons.utils.httpclient.HttpClient;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnector;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnectorException;
import dk.dbc.dataio.commons.utils.service.ServiceUtil;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.EJBException;
import javax.ejb.LocalBean;
import javax.ejb.Lock;
import javax.ejb.LockType;
import javax.ejb.Singleton;
import javax.naming.NamingException;
import javax.ws.rs.client.Client;

/**
 * This Enterprise Java Bean (EJB) singleton is used as a connector
 * to the job-store REST interface.
 */
// ToDo: merge with functionality in dk.dbc.dataio.jobprocessor.ejb.JobStoreServiceConnectorBean class
@Singleton
@LocalBean
public class JobStoreServiceConnectorBean {
    private static final Logger LOGGER = LoggerFactory.getLogger(JobStoreServiceConnectorBean.class);

    Client client;

    @PostConstruct
    public void initializeConnector() {
        LOGGER.debug("Initializing connector");
        // performance: we should consider adding single jackson ObjectMapper to be used by all threads
        client = HttpClient.newClient(new ClientConfig()
                .register(new JacksonFeature()));
    }

    @Lock(LockType.READ)
    public JobInfo createJob(JobSpecification jobSpecification) throws JobStoreServiceConnectorException {
        LOGGER.debug("Creating new job");
        try {
            // performance: consider JNDI lookup cache or service-locator pattern
            final String baseUrl = ServiceUtil.getJobStoreServiceEndpoint();
            final JobStoreServiceConnector jobStoreServiceConnector = new JobStoreServiceConnector(client, baseUrl);
            return jobStoreServiceConnector.createJob(jobSpecification);
        } catch (NamingException e) {
            throw new EJBException(e);
        }
    }

    @PreDestroy
    public void tearDownConnector() {
        HttpClient.closeClient(client);
    }
}
