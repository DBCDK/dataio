package dk.dbc.dataio.jobprocessor.ejb;

import dk.dbc.dataio.commons.types.Chunk;
import dk.dbc.dataio.commons.types.json.mixins.MixIns;
import dk.dbc.dataio.commons.types.rest.JobStoreServiceConstants;
import dk.dbc.dataio.commons.utils.httpclient.HttpClient;
import dk.dbc.dataio.commons.utils.json.JsonException;
import dk.dbc.dataio.commons.utils.json.JsonUtil;
import dk.dbc.dataio.commons.utils.service.ServiceUtil;
import dk.dbc.dataio.jobprocessor.exception.JobProcessorException;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.ejb.LocalBean;
import javax.ejb.Lock;
import javax.ejb.LockType;
import javax.ejb.Singleton;
import javax.naming.NamingException;
import javax.ws.rs.client.Client;
import javax.ws.rs.core.Response;
import java.util.HashMap;
import java.util.Map;

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
        // performance: we should consider adding single jackson ObjectMapper to be used by all threads
        client = HttpClient.newClient(new ClientConfig()
                .register(new JacksonFeature()));
    }

    /**
     * Retrieves chunk from the job-store
     *
     * @param jobId Id of job containing chunk
     * @param chunkId Id of chunk
     *
     * @return chunk
     *
     * @throws JobProcessorException on failure to retrieve chunk
     */
    @Lock(LockType.READ)
    public Chunk getChunk(long jobId, long chunkId) throws JobProcessorException {
        LOGGER.debug("Fetching chunk {} for job {}", chunkId, jobId);
        try {
            // performance: consider JNDI lookup cache or service-locator pattern
            final String baseUrl = ServiceUtil.getJobStoreServiceEndpoint();

            final Map<String, String> pathVariables = new HashMap<>();
            pathVariables.put(JobStoreServiceConstants.JOB_ID_VARIABLE, Long.toString(jobId));
            pathVariables.put(JobStoreServiceConstants.CHUNK_ID_VARIABLE, Long.toString(chunkId));
            final String path = HttpClient.interpolatePathVariables(JobStoreServiceConstants.JOB_CHUNK, pathVariables);
            final Response response = HttpClient.doGet(client, baseUrl, path.split("/"));
            verifyStatusCode(response, Response.Status.OK);

            return JsonUtil.fromJson(response.readEntity(String.class), Chunk.class, MixIns.getMixIns());
        } catch (NamingException | JsonException | JobProcessorException e) {
            throw new JobProcessorException(String.format(
                    "Unable to fetch chunk %d for job %d", chunkId, jobId), e);
        }
    }

    @Lock(LockType.READ)
    private void verifyStatusCode(Response response, Response.Status expectedStatus) throws JobProcessorException {
        final Response.Status status = Response.Status.fromStatusCode(response.getStatus());
        if (status != expectedStatus) {
            switch (status) {
                case NOT_FOUND: throw new JobProcessorException("Not found");
                case INTERNAL_SERVER_ERROR: throw new JobProcessorException("Endpoint internal error");
                default: throw new JobProcessorException(String.format("Unexpected response status code %d",
                        status.getStatusCode()));
            }
        }
    }
}
