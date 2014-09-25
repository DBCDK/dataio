package dk.dbc.dataio.logstore.service.connector;

import dk.dbc.dataio.commons.types.rest.LogStoreServiceConstants;
import dk.dbc.dataio.commons.utils.httpclient.HttpClient;
import dk.dbc.dataio.commons.utils.httpclient.PathBuilder;
import dk.dbc.dataio.commons.utils.invariant.InvariantUtil;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.Client;
import javax.ws.rs.core.Response;

/**
 * LogStoreServiceConnector - dataIO log-store REST service client.
 * <p>
 * To use this class, you construct an instance, specifying a web resources client as well as
 * a base URL for the file-store service endpoint you will be communicating with.
 * </p>
 * <p>
 * This class is thread safe, as long as the given web resources client remains thread safe.
 * </p>
 */
public class LogStoreServiceConnector {
    private final Client httpClient;
    private final String baseUrl;

    /**
     * Class constructor
     * @param httpClient web resources client
     * @param baseUrl base URL for log-store service endpoint
     * @throws NullPointerException if given null-valued argument
     * @throws IllegalArgumentException if given empty-valued {@code baseUrl} argument
     */
    public LogStoreServiceConnector(Client httpClient, String baseUrl) throws NullPointerException, IllegalArgumentException {
        this.httpClient = InvariantUtil.checkNotNullOrThrow(httpClient, "httpClient");
        this.baseUrl = InvariantUtil.checkNotNullNotEmptyOrThrow(baseUrl, "baseUrl");
    }

    /**
     * Retrieves log for given item in given chunk in given job
     * @param jobId ID of job
     * @param chunkId ID of chunk in job
     * @param itemId ID of item in chunk
     * @return log as text
     * @throws NullPointerException if given null-valued {@code jobId} argument
     * @throws IllegalArgumentException if given empty-valued {@code jobId} argument
     * @throws ProcessingException on general communication error
     * @throws LogStoreServiceConnectorException on failure to retrieve log
     */
    public String getItemLog(final String jobId, final long chunkId, final long itemId)
            throws NullPointerException, IllegalArgumentException, ProcessingException, LogStoreServiceConnectorException {
        InvariantUtil.checkNotNullNotEmptyOrThrow(jobId, "jobId");
        final PathBuilder path = new PathBuilder(LogStoreServiceConstants.ITEM_LOG_ENTRY_COLLECTION)
                .bind(LogStoreServiceConstants.JOB_ID_VARIABLE, jobId)
                .bind(LogStoreServiceConstants.CHUNK_ID_VARIABLE, chunkId)
                .bind(LogStoreServiceConstants.ITEM_ID_VARIABLE, itemId);
        final Response response = HttpClient.doGet(httpClient, baseUrl, path.build());
        try {
            verifyResponseStatus(Response.Status.fromStatusCode(response.getStatus()), Response.Status.OK);
            return readResponseEntity(response, String.class);
        } finally {
            response.close();
        }
    }

    public Client getHttpClient() {
        return httpClient;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    private void verifyResponseStatus(Response.Status actualStatus, Response.Status expectedStatus)
            throws LogStoreServiceConnectorUnexpectedStatusCodeException {
        if (actualStatus != expectedStatus) {
            throw new LogStoreServiceConnectorUnexpectedStatusCodeException(
                    String.format("log-store service returned with unexpected status code: %s", actualStatus),
                    actualStatus.getStatusCode());
        }
    }

    private <T> T readResponseEntity(Response response, Class<T> tClass) throws LogStoreServiceConnectorException {
        response.bufferEntity(); // must be done in order to possible avoid a timeout-exception from readEntity.
        final T entity = response.readEntity(tClass);
        if (entity == null) {
            throw new LogStoreServiceConnectorException(
                    String.format("log-store service returned with null-valued %s entity", tClass.getName()));
        }
        return entity;
    }
}
