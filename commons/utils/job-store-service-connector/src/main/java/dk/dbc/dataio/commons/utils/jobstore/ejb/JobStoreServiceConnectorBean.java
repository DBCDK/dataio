package dk.dbc.dataio.commons.utils.jobstore.ejb;

import dk.dbc.dataio.commons.types.Chunk;
import dk.dbc.dataio.commons.types.JobCompletionState;
import dk.dbc.dataio.commons.types.JobInfo;
import dk.dbc.dataio.commons.types.JobSpecification;
import dk.dbc.dataio.commons.types.JobState;
import dk.dbc.dataio.commons.types.Sink;
import dk.dbc.dataio.commons.types.SinkChunkResult;
import dk.dbc.dataio.commons.utils.httpclient.HttpClient;
import dk.dbc.dataio.commons.utils.jersey.jackson.Jackson2xFeature;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnector;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnectorException;
import dk.dbc.dataio.commons.utils.service.ServiceUtil;
import org.glassfish.jersey.client.ClientConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.EJBException;
import javax.ejb.LocalBean;
import javax.ejb.Singleton;
import javax.naming.NamingException;
import javax.ws.rs.client.Client;

/**
 * This Enterprise Java Bean (EJB) singleton is used as a connector
 * to the job-store REST interface.
 */
@Singleton
@LocalBean
public class JobStoreServiceConnectorBean {
    private static final Logger LOGGER = LoggerFactory.getLogger(JobStoreServiceConnectorBean.class);

    Client client;

    @PostConstruct
    public void initializeConnector() {
        LOGGER.debug("Initializing connector");
        client = HttpClient.newClient(new ClientConfig()
                .register(new Jackson2xFeature()));
    }

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

    public Sink getSink(long jobId) throws JobStoreServiceConnectorException {
        LOGGER.debug("Retrieving sink for job[{}]", jobId);
        try {
            // performance: consider JNDI lookup cache or service-locator pattern
            final String baseUrl = ServiceUtil.getJobStoreServiceEndpoint();
            final JobStoreServiceConnector jobStoreServiceConnector = new JobStoreServiceConnector(client, baseUrl);
            return jobStoreServiceConnector.getSink(jobId);
        } catch (NamingException e) {
            throw new EJBException(e);
        }
    }

    public JobState getState(long jobId) throws JobStoreServiceConnectorException {
        LOGGER.debug("Retrieving state for job[{}]", jobId);
        try {
            // performance: consider JNDI lookup cache or service-locator pattern
            final String baseUrl = ServiceUtil.getJobStoreServiceEndpoint();
            final JobStoreServiceConnector jobStoreServiceConnector = new JobStoreServiceConnector(client, baseUrl);
            return jobStoreServiceConnector.getState(jobId);
        } catch (NamingException e) {
            throw new EJBException(e);
        }
    }

    public JobCompletionState getJobCompletionState(long jobId) throws JobStoreServiceConnectorException {
        LOGGER.debug("Retrieving JobCompletinoState for job[{}]", jobId);
        try {
            // performance: consider JNDI lookup cache or service-locator pattern
            final String baseUrl = ServiceUtil.getJobStoreServiceEndpoint();
            final JobStoreServiceConnector jobStoreServiceConnector = new JobStoreServiceConnector(client, baseUrl);
            return jobStoreServiceConnector.getJobCompletionState(jobId);
        } catch (NamingException e) {
            throw new EJBException(e);
        }
    }

    public Chunk getChunk(long jobId, long chunkId) throws JobStoreServiceConnectorException {
        LOGGER.debug("Retrieving chunk[{}] for job[{}]", chunkId, jobId);
        try {
            // performance: consider JNDI lookup cache or service-locator pattern
            final String baseUrl = ServiceUtil.getJobStoreServiceEndpoint();
            final JobStoreServiceConnector jobStoreServiceConnector = new JobStoreServiceConnector(client, baseUrl);
            return jobStoreServiceConnector.getChunk(jobId, chunkId);
        } catch (NamingException e) {
            throw new EJBException(e);
        }
    }

    public SinkChunkResult getSinkChunkResult(long jobId, long chunkId) throws JobStoreServiceConnectorException {
        LOGGER.debug("Retrieving sink result chunk[{}] for job[{}]", chunkId, jobId);
        try {
            // performance: consider JNDI lookup cache or service-locator pattern
            final String baseUrl = ServiceUtil.getJobStoreServiceEndpoint();
            final JobStoreServiceConnector jobStoreServiceConnector = new JobStoreServiceConnector(client, baseUrl);
            return jobStoreServiceConnector.getSinkChunkResult(jobId, chunkId);
        } catch (NamingException e) {
            throw new EJBException(e);
        }
    }

    @PreDestroy
    public void tearDownConnector() {
        HttpClient.closeClient(client);
    }
}
