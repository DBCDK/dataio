package dk.dbc.dataio.commons.utils.jobstore;

import dk.dbc.dataio.commons.time.StopWatch;
import dk.dbc.dataio.jobstore.types.Chunk;
import dk.dbc.dataio.commons.types.ExternalChunk;
import dk.dbc.dataio.commons.types.Flow;
import dk.dbc.dataio.commons.types.JobCompletionState;
import dk.dbc.dataio.commons.types.JobErrorCode;
import dk.dbc.dataio.commons.types.JobInfo;
import dk.dbc.dataio.commons.types.JobSpecification;
import dk.dbc.dataio.commons.types.JobState;
import dk.dbc.dataio.commons.types.Sink;
import dk.dbc.dataio.jobstore.types.SinkChunkResult;
import dk.dbc.dataio.commons.types.SupplementaryProcessData;
import dk.dbc.dataio.commons.types.rest.JobStoreServiceConstants;
import dk.dbc.dataio.commons.utils.httpclient.HttpClient;
import dk.dbc.dataio.commons.utils.httpclient.PathBuilder;
import dk.dbc.dataio.commons.utils.invariant.InvariantUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.Client;
import javax.ws.rs.core.Response;

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
     * @param jobSpecification job specification
     * @return job info
     * @throws NullPointerException if given null-valued argument
     * @throws ProcessingException on general communication error
     * @throws JobStoreServiceConnectorJobCreationFailedException if job creation failed due to invalid input data
     * @throws JobStoreServiceConnectorException on general failure to create job
     */
    public JobInfo createJob(JobSpecification jobSpecification) throws NullPointerException, ProcessingException, JobStoreServiceConnectorException {
        final StopWatch stopWatch = new StopWatch();
        try {
            InvariantUtil.checkNotNullOrThrow(jobSpecification, "jobSpecification");
            final Response response = HttpClient.doPostWithJson(httpClient, jobSpecification, baseUrl, JobStoreServiceConstants.JOB_COLLECTION);
            try {
                verifyResponseStatus(Response.Status.fromStatusCode(response.getStatus()), Response.Status.CREATED);
                final JobInfo jobInfo = readResponseEntity(response, JobInfo.class);
                if (jobInfo.getJobErrorCode() != JobErrorCode.NO_ERROR) {
                    throw new JobStoreServiceConnectorJobCreationFailedException(
                            String.format("job-store service was unable to finish creation of job<%s> due to %s", jobInfo.getJobId(), jobInfo.getJobErrorCode()),
                            jobInfo.getJobErrorCode());
                }
                return jobInfo;
            } finally {
                response.close();
            }
        } finally {
            LOGGER.debug("JobStoreConnector operation took {} milliseconds", stopWatch.getElapsedTime());
        }
    }

    /**
     * Retrieves sink associated with job from job-store
     * @param jobId Id of job
     * @return job sink
     * @throws ProcessingException on general communication error
     * @throws JobStoreServiceConnectorException on failure to retrieve sink
     */
    public Sink getSink(long jobId) throws ProcessingException, JobStoreServiceConnectorException {
        final StopWatch stopWatch = new StopWatch();
        try {
            final PathBuilder path = new PathBuilder(JobStoreServiceConstants.JOB_SINK)
                    .bind(JobStoreServiceConstants.JOB_ID_VARIABLE, jobId);
            final Response response = HttpClient.doGet(httpClient, baseUrl, path.build());
            try {
                verifyResponseStatus(Response.Status.fromStatusCode(response.getStatus()), Response.Status.OK);
                return readResponseEntity(response, Sink.class);
            } finally {
                response.close();
            }
        } finally {
            LOGGER.debug("JobStoreConnector operation took {} milliseconds", stopWatch.getElapsedTime());
        }
    }

    /**
     * Retrieves job state from job-store
     * @param jobId Id of job
     * @return job state
     * @throws ProcessingException on general communication error
     * @throws JobStoreServiceConnectorException on failure to retrieve state
     */
    public JobState getState(long jobId) throws ProcessingException, JobStoreServiceConnectorException {
        final StopWatch stopWatch = new StopWatch();
        try {
            final PathBuilder path = new PathBuilder(JobStoreServiceConstants.JOB_STATE)
                    .bind(JobStoreServiceConstants.JOB_ID_VARIABLE, jobId);
            final Response response = HttpClient.doGet(httpClient, baseUrl, path.build());
            try {
                verifyResponseStatus(Response.Status.fromStatusCode(response.getStatus()), Response.Status.OK);
                return readResponseEntity(response, JobState.class);
            } finally {
                response.close();
            }
        } finally {
            LOGGER.debug("JobStoreConnector operation took {} milliseconds", stopWatch.getElapsedTime());
        }
    }

    /**
     * Retrieves chunk from job-store
     * @param jobId Id of job containing chunk
     * @param chunkId Id of chunk
     * @param type The chunk-type
     * @return chunk
     * @throws ProcessingException on general communication error
     * @throws JobStoreServiceConnectorException on failure to retrieve chunk
     */
    public ExternalChunk getChunk(long jobId, long chunkId, ExternalChunk.Type type) throws ProcessingException, JobStoreServiceConnectorException {
        
        switch(type) {
            case PARTITIONED:
                return Chunk.convertToExternalChunk(getChunk(jobId, chunkId));
            case PROCESSED:
                break;
            case DELIVERED:
                return SinkChunkResult.convertToExternalChunk(getSinkChunkResult(jobId, chunkId));
            default:
                   
        }
        String errMsg = String.format("Unknown type requested for getChunk: %s", type);
        LOGGER.warn(errMsg);
        throw new JobStoreServiceConnectorException(errMsg);
    }

    /**
     * Retrieves chunk from job-store
     * @param jobId Id of job containing chunk
     * @param chunkId Id of chunk
     * @return chunk
     * @throws ProcessingException on general communication error
     * @throws JobStoreServiceConnectorException on failure to retrieve chunk
     */
    private Chunk getChunk(long jobId, long chunkId) throws ProcessingException, JobStoreServiceConnectorException {
        final StopWatch stopWatch = new StopWatch();
        try {
            final PathBuilder path = new PathBuilder(JobStoreServiceConstants.JOB_CHUNK)
                    .bind(JobStoreServiceConstants.JOB_ID_VARIABLE, jobId)
                    .bind(JobStoreServiceConstants.CHUNK_ID_VARIABLE, chunkId);
            final Response response = HttpClient.doGet(httpClient, baseUrl, path.build());
            try {
                verifyResponseStatus(Response.Status.fromStatusCode(response.getStatus()), Response.Status.OK);
                return readResponseEntity(response, Chunk.class);
            } finally {
                response.close();
                LOGGER.debug("JobStoreConnector operation took {} milliseconds", stopWatch.getElapsedTime());
            }
        } finally {
            LOGGER.debug("JobStoreConnector operation took {} milliseconds", stopWatch.getElapsedTime());
        }
    }

    /**
     * Retrieves sink chunk result from job-store
     * @param jobId Id of job containing chunk
     * @param chunkId Id of chunk for which to retrieve sink result
     * @return sink chunk result
     * @throws ProcessingException on general communication error
     * @throws JobStoreServiceConnectorException on failure to retrieve sink chunk result
     */
    private SinkChunkResult getSinkChunkResult(long jobId, long chunkId) throws ProcessingException, JobStoreServiceConnectorException {
        final StopWatch stopWatch = new StopWatch();
        try {
            final PathBuilder path = new PathBuilder(JobStoreServiceConstants.JOB_DELIVERED)
                    .bind(JobStoreServiceConstants.JOB_ID_VARIABLE, jobId)
                    .bind(JobStoreServiceConstants.CHUNK_ID_VARIABLE, chunkId);
            final Response response = HttpClient.doGet(httpClient, baseUrl, path.build());
            try {
                verifyResponseStatus(Response.Status.fromStatusCode(response.getStatus()), Response.Status.OK);
                return readResponseEntity(response, SinkChunkResult.class);
            } finally {
                response.close();
            }
        } finally {
            LOGGER.debug("JobStoreConnector operation took {} milliseconds", stopWatch.getElapsedTime());
        }
    }

    /**
     * Retrieves JobCompletionState from job-store
     * @param jobId Id of job
     * @return A JobCompletionState object
     * @throws ProcessingException on general communication error
     * @throws JobStoreServiceConnectorException on failure to retrieve JobCompletionState
     */
    public JobCompletionState getJobCompletionState(long jobId) throws ProcessingException, JobStoreServiceConnectorException {
        final StopWatch stopWatch = new StopWatch();
        try {
            final PathBuilder path = new PathBuilder(JobStoreServiceConstants.JOB_COMPLETIONSTATE)
                    .bind(JobStoreServiceConstants.JOB_ID_VARIABLE, jobId);
            final Response response = HttpClient.doGet(httpClient, baseUrl, path.build());
            try {
                verifyResponseStatus(Response.Status.fromStatusCode(response.getStatus()), Response.Status.OK);
                return readResponseEntity(response, JobCompletionState.class);
            } finally {
                response.close();
            }
        } finally {
            LOGGER.debug("JobStoreConnector operation took {} milliseconds", stopWatch.getElapsedTime());
        }
    }

    /**
     * Retrieves flow associated with job from job-store
     * @param jobId Id of job
     * @return flow
     * @throws ProcessingException on general communication error
     * @throws JobStoreServiceConnectorException on failure to retrieve flow
     */
    public Flow getFlow(long jobId) throws ProcessingException, JobStoreServiceConnectorException {
        final StopWatch stopWatch = new StopWatch();
        try {
            final PathBuilder path = new PathBuilder(JobStoreServiceConstants.JOB_FLOW)
                    .bind(JobStoreServiceConstants.JOB_ID_VARIABLE, jobId);
            final Response response = HttpClient.doGet(httpClient, baseUrl, path.build());
            try {
                verifyResponseStatus(Response.Status.fromStatusCode(response.getStatus()), Response.Status.OK);
                return readResponseEntity(response, Flow.class);
            } finally {
                response.close();
            }
        } finally {
            LOGGER.debug("JobStoreConnector operation took {} milliseconds", stopWatch.getElapsedTime());
        }
    }

    /**
     * Retrieves the SupplementaryProcessData associated with job from job-store
     * @param jobId Id of job
     * @return SupplementaryProcessData
     * @throws ProcessingException on general communication error
     * @throws JobStoreServiceConnectorException on failure to retrieve SupplementaryProcessData
     */
    public SupplementaryProcessData getSupplementaryProcessData(long jobId) throws ProcessingException, JobStoreServiceConnectorException {
        final StopWatch stopWatch = new StopWatch();
        try {
            final PathBuilder path = new PathBuilder(JobStoreServiceConstants.JOB_SUPPLEMENTARYPROCESSDATA)
                    .bind(JobStoreServiceConstants.JOB_ID_VARIABLE, jobId);
            final Response response = HttpClient.doGet(httpClient, baseUrl, path.build());
            try {
                verifyResponseStatus(Response.Status.fromStatusCode(response.getStatus()), Response.Status.OK);
                return readResponseEntity(response, SupplementaryProcessData.class);
            } finally {
                response.close();
            }
        } finally {
            LOGGER.debug("JobStoreConnector operation took {} milliseconds", stopWatch.getElapsedTime());
        }
    }


    private void verifyResponseStatus(Response.Status actualStatus, Response.Status expectedStatus) throws JobStoreServiceConnectorException {
        if (actualStatus != expectedStatus) {
            throw new JobStoreServiceConnectorException(
                    String.format("job-store service returned with unexpected status code: %s", actualStatus));
        }
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

    public Client getHttpClient() {
        return httpClient;
    }

    public String getBaseUrl() {
        return baseUrl;
    }
}
