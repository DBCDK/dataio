package dk.dbc.dataio.commons.utils.jobstore;

import dk.dbc.dataio.commons.time.StopWatch;
import dk.dbc.dataio.commons.types.rest.JobStoreServiceConstants;
import dk.dbc.dataio.commons.utils.httpclient.HttpClient;
import dk.dbc.dataio.commons.utils.invariant.InvariantUtil;
import dk.dbc.dataio.jobstore.types.JobInputStream;
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
     * @param jobInputStream containing the job specification, part number and information regarding
     *                       whether or not the job has finished
     *
     * @throws NullPointerException if given null-valued argument
     * @throws ProcessingException on general communication error
     * @throws JobStoreServiceConnectorException on general failure to create job
     */
    public void addJob(JobInputStream jobInputStream) throws NullPointerException, ProcessingException, JobStoreServiceConnectorException {
        final StopWatch stopWatch = new StopWatch();
        try {
            InvariantUtil.checkNotNullOrThrow(jobInputStream, "jobInputStream");
            final Response response = HttpClient.doPostWithJson(httpClient, jobInputStream, baseUrl, JobStoreServiceConstants.JOB_COLLECTION);
            try {
                verifyResponseStatus(Response.Status.fromStatusCode(response.getStatus()), Response.Status.CREATED);
                readResponseEntity(response, JobInputStream.class);
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

    private <T> T readResponseEntity(Response response, Class<T> tClass) throws JobStoreServiceConnectorException {
        response.bufferEntity(); // must be done in order to possible avoid a timeout-exception from readEntity.
        final T entity = response.readEntity(tClass);
        if (entity == null) {
            throw new JobStoreServiceConnectorException(
                    String.format("job-store service returned with null-valued %s entity", tClass.getName()));
        }
        return entity;
    }
}
