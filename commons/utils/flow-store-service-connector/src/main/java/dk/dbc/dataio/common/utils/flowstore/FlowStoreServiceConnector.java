package dk.dbc.dataio.common.utils.flowstore;

import dk.dbc.dataio.commons.types.Sink;
import dk.dbc.dataio.commons.types.rest.FlowStoreServiceConstants;
import dk.dbc.dataio.commons.utils.httpclient.HttpClient;
import dk.dbc.dataio.commons.utils.invariant.InvariantUtil;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.Client;
import javax.ws.rs.core.Response;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by sma on 29/04/14.
 */
public class FlowStoreServiceConnector {

    private static final String URL_PATH_SEPARATOR = "/";

    private final Client httpClient;
    private final String baseUrl;

    /**
     * Class constructor
     * @param httpClient web resources client
     * @param baseUrl base URL for job-store service endpoint
     * @throws NullPointerException if given null-valued argument
     * @throws IllegalArgumentException if given empty-valued {@code baseUrl} argument
     */
    public FlowStoreServiceConnector(Client httpClient, String baseUrl) throws NullPointerException, IllegalArgumentException {
        this.httpClient = InvariantUtil.checkNotNullOrThrow(httpClient, "httpClient");
        this.baseUrl = InvariantUtil.checkNotNullNotEmptyOrThrow(baseUrl, "baseUrl");
    }

    /**
     * Retrieves the specified sink from the flow-store
     * @param sinkId Id of the sink
     * @return the sink found
     * @throws ProcessingException on general communication error
     * @throws FlowStoreServiceConnectorException on failure to retrieve the sink
     */
    public Sink getSink(long sinkId) throws ProcessingException, FlowStoreServiceConnectorException {
        final Map<String, String> pathVariables = new HashMap<>(1);
        pathVariables.put(FlowStoreServiceConstants.SINK_ID_VARIABLE, Long.toString(sinkId));
        final String path = HttpClient.interpolatePathVariables(FlowStoreServiceConstants.SINK_ID, pathVariables);
        final Response response = HttpClient.doGet(httpClient, baseUrl, path.split(URL_PATH_SEPARATOR));
        try {
            verifyResponseStatus(Response.Status.fromStatusCode(response.getStatus()), Response.Status.OK);
            return readResponseEntity(response, Sink.class);
        } finally {
            response.close();
        }
    }

    private void verifyResponseStatus(Response.Status actualStatus, Response.Status expectedStatus) throws FlowStoreServiceConnectorUnexpectedStatusCodeException {
        if (actualStatus != expectedStatus) {
            throw new FlowStoreServiceConnectorUnexpectedStatusCodeException(
                    String.format("flow-store service returned with unexpected status code: %s", actualStatus), actualStatus.getStatusCode());
        }
    }

    private <T> T readResponseEntity(Response response, Class<T> tClass) throws FlowStoreServiceConnectorException {
        response.bufferEntity(); // must be done in order to possible avoid a timeout-exception from readEntity.
        final T entity = response.readEntity(tClass);
        if (entity == null) {
            throw new FlowStoreServiceConnectorException(
                    String.format("flow-store service returned with null-valued %s entity", tClass.getName()));
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
