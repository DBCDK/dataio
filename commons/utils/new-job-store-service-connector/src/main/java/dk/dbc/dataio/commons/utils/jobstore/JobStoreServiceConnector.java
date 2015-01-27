package dk.dbc.dataio.commons.utils.jobstore;

import dk.dbc.dataio.commons.time.StopWatch;
import dk.dbc.dataio.commons.types.ExternalChunk;
import dk.dbc.dataio.commons.types.rest.JobStoreServiceConstants;
import dk.dbc.dataio.commons.utils.httpclient.HttpClient;
import dk.dbc.dataio.commons.utils.httpclient.PathBuilder;
import dk.dbc.dataio.commons.utils.invariant.InvariantUtil;
import dk.dbc.dataio.jobstore.types.JobInfoSnapshot;
import dk.dbc.dataio.jobstore.types.JobInputStream;
import dk.dbc.dataio.jobstore.types.criteria.JobListCriteria;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.Client;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;
import java.util.List;

/**
 * JobStoreServiceConnector - dataIO job-store REST service client.
 * <p>
 * To use this class, you construct an instance, specifying a web resources client as well as
 * a base URL for the job-store service endpoint you will be communicating with.
 * </p>
 * <p>
 * This class is thread safe, as long as the given web resources client remains thread safe.
 * </p>
 */
public class JobStoreServiceConnector {
    private static final Logger LOGGER = LoggerFactory.getLogger(JobStoreServiceConnector.class);

    private final Client httpClient;
    private final String baseUrl;

    /**
     * Class constructor
     * @param httpClient web resources client
     * @param baseUrl base URL for job-store service endpoint
     * @throws NullPointerException if given null-valued argument
     * @throws IllegalArgumentException if given empty-valued {@code baseUrl} argument
     */
    public JobStoreServiceConnector(Client httpClient, String baseUrl) throws NullPointerException, IllegalArgumentException {
        this.httpClient = InvariantUtil.checkNotNullOrThrow(httpClient, "httpClient");
        this.baseUrl = InvariantUtil.checkNotNullNotEmptyOrThrow(baseUrl, "baseUrl");
    }

    /**
     * Creates new job defined by given job specification in the job-store
     *
     * @param jobInputStream containing the job specification
     * @return JobInfoSnapshot displaying job information from one exact moment in time.
     *
     * @throws NullPointerException if given null-valued argument
     * @throws ProcessingException on general communication error
     * @throws JobStoreServiceConnectorException on general failure to create job
     */
    public JobInfoSnapshot addJob(JobInputStream jobInputStream) throws NullPointerException, ProcessingException, JobStoreServiceConnectorException {
        final StopWatch stopWatch = new StopWatch();
        try {
            InvariantUtil.checkNotNullOrThrow(jobInputStream, "jobInputStream");
            final Response response = HttpClient.doPostWithJson(httpClient, jobInputStream, baseUrl, JobStoreServiceConstants.JOB_COLLECTION);
            try {
                verifyResponseStatus(Response.Status.fromStatusCode(response.getStatus()), Response.Status.CREATED);
                return readResponseEntity(response, JobInfoSnapshot.class);
            } finally {
                response.close();
            }
        } finally {
            LOGGER.debug("JobStoreConnector operation took {} milliseconds", stopWatch.getElapsedTime());
        }
    }

    /**
     * Adds chunk and updates existing job by updating existing items, chunk and job entities in the underlying data store.
     *
     * @param chunk external chunk
     * @param jobId job id
     * @param chunkId chunk id
     * @return JobInfoSnapshot displaying job information from one exact moment in time.
     *
     * @throws NullPointerException if given null-valued external chunk argument
     * @throws JobStoreServiceConnectorException on general failure to update job
     * @throws IllegalArgumentException on invalid external chunk type
     */
    public JobInfoSnapshot addChunk(ExternalChunk chunk, long jobId, long chunkId) throws NullPointerException, IllegalArgumentException, JobStoreServiceConnectorException {
        final StopWatch stopWatch = new StopWatch();
        try {
            InvariantUtil.checkNotNullOrThrow(chunk, "chunk");
            final PathBuilder path = new PathBuilder(chunkTypeToJobStorePath(chunk.getType()))
                    .bind(JobStoreServiceConstants.JOB_ID_VARIABLE, jobId)
                    .bind(JobStoreServiceConstants.CHUNK_ID_VARIABLE, chunkId);
            final Response response = HttpClient.doPostWithJson(httpClient, chunk, baseUrl, path.build());
            try {
                verifyResponseStatus(Response.Status.fromStatusCode(response.getStatus()), Response.Status.CREATED);
                return readResponseEntity(response, JobInfoSnapshot.class);
            } finally {
                response.close();
            }
        } finally {
            LOGGER.debug("JobStoreConnector operation took {} milliseconds", stopWatch.getElapsedTime());
        }
    }

    /**
     * Retrieves job listing determined by given search criteria from the job-store
     * @param criteria list criteria
     * @return list of selected job info snapshots
     * @throws NullPointerException when given null-valued criteria argument
     * @throws JobStoreServiceConnectorException on general failure to produce jobs listing
     */
    public List<JobInfoSnapshot> listJobs(JobListCriteria criteria) throws NullPointerException, JobStoreServiceConnectorException {
        final StopWatch stopWatch = new StopWatch();
        try {
            InvariantUtil.checkNotNullOrThrow(criteria, "criteria");
            final Response response = HttpClient.doPostWithJson(httpClient, criteria, baseUrl, JobStoreServiceConstants.JOB_COLLECTION_SEARCHES);
            try {
                verifyResponseStatus(Response.Status.fromStatusCode(response.getStatus()), Response.Status.OK);
                return readResponseEntity(response, new GenericType<List<JobInfoSnapshot>>() {});
            } finally {
                response.close();
            }
        } finally {
            LOGGER.debug("JobStoreConnector operation took {} milliseconds", stopWatch.getElapsedTime());
        }
    }

    public Client getHttpClient() {
        return httpClient;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    /*
     * Private methods
     */

    private void verifyResponseStatus(Response.Status actualStatus, Response.Status expectedStatus) throws JobStoreServiceConnectorUnexpectedStatusCodeException {
        if (actualStatus != expectedStatus) {
            throw new JobStoreServiceConnectorUnexpectedStatusCodeException(
                    String.format("job-store service returned with unexpected status code: %s", actualStatus), actualStatus.getStatusCode());
        }
    }

    private <T> T readResponseEntity(Response response, GenericType<T> genericType) throws JobStoreServiceConnectorException {
        response.bufferEntity(); // must be done in order to possible avoid a timeout-exception from readEntity.
        final T entity = response.readEntity(genericType);
        if (entity == null) {
            throw new JobStoreServiceConnectorException(
                    String.format("job-store service returned with null-valued %s entity", genericType.getRawType().getName()));
        }
        return entity;
    }

    private <T> T readResponseEntity(Response response, Class<T> tClass) throws JobStoreServiceConnectorException {
        response.bufferEntity(); // must be done in order to possible avoid a timeout-exception from readEntity.
        final T entity = response.readEntity(tClass);
        if (entity == null) {
            throw new JobStoreServiceConnectorException(
                    String.format("job-store service returned with null-valued %s entity", tClass.getName()));
        }
        return entity;
    }

    private String chunkTypeToJobStorePath(ExternalChunk.Type chunkType) throws IllegalArgumentException {
        switch (chunkType) {
            case PROCESSED:   return JobStoreServiceConstants.JOB_CHUNK_PROCESSED;
            case DELIVERED:   return JobStoreServiceConstants.JOB_CHUNK_DELIVERED;
            case PARTITIONED: throw new IllegalArgumentException("PARTITIONED is not a valid type");
            default:          throw new IllegalArgumentException("ExternalChunk.Type could not be identified");
        }
    }
}
