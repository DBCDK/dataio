package dk.dbc.dataio.commons.utils.jobstore;

import dk.dbc.dataio.commons.types.JobErrorCode;
import dk.dbc.dataio.commons.types.JobInfo;
import dk.dbc.dataio.commons.types.JobSpecification;
import dk.dbc.dataio.commons.types.rest.JobStoreServiceConstants;
import dk.dbc.dataio.commons.utils.httpclient.HttpClient;
import dk.dbc.dataio.commons.utils.invariant.InvariantUtil;

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
    }

    private void verifyResponseStatus(Response.Status actualStatus, Response.Status expectedStatus) throws JobStoreServiceConnectorException {
        if (actualStatus != expectedStatus) {
            throw new JobStoreServiceConnectorException(
                    String.format("job-store service returned with unexpected status code: %s", actualStatus));
        }
    }

    private <T> T readResponseEntity(Response response, Class<T> tClass) throws JobStoreServiceConnectorException {
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
