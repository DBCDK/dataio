package dk.dbc.dataio.commons.creatordetector.connector;

import dk.dbc.commons.useragent.UserAgent;
import dk.dbc.httpclient.FailSafeHttpClient;
import dk.dbc.httpclient.HttpPost;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.core.Response;
import net.jodah.failsafe.RetryPolicy;

import java.time.Duration;

public class CreatorDetectorConnector implements AutoCloseable {

    private static final String DETECT_ENDPOINT = "/detect";

    private static final RetryPolicy<Response> RETRY_POLICY = new RetryPolicy<Response>()
            .handle(ProcessingException.class)
            .handleResultIf(response ->
                    response.getStatus() == 404
                            || response.getStatus() == 500
                            || response.getStatus() == 502)
            .withDelay(Duration.ofSeconds(5))
            .withMaxRetries(3);
    
    private final FailSafeHttpClient failSafeHttpClient;
    private final String baseUrl;

    public CreatorDetectorConnector(Client httpClient, UserAgent userAgent, String baseUrl) {
        this(FailSafeHttpClient.create(httpClient, userAgent, RETRY_POLICY), baseUrl);
    }

    /**
     * Returns new instance with custom retry policy
     * @param failSafeHttpClient web resources client with custom retry policy
     * @param baseUrl base URL for service endpoint
     */
    public CreatorDetectorConnector(FailSafeHttpClient failSafeHttpClient, String baseUrl) {
        if (failSafeHttpClient == null) {
            throw new NullPointerException("Value of parameter failSafeHttpClient cannot be null");
        }
        if (baseUrl == null) {
            throw new NullPointerException("Value of parameter baseUrl cannot be null");
        }
        if (baseUrl.isBlank()) {
            throw new IllegalArgumentException("Value of parameter baseUrl cannot be empty");
        }
        this.failSafeHttpClient = failSafeHttpClient;
        this.baseUrl = baseUrl;
    }

    /**
     * Detects creator names for a given text
     * @param request request body
     * @return creator name suggestions
     * @throws CreatorDetectorConnectorException on failure to retrieve suggestions
     */
    public CreatorNameSuggestions detectCreatorNames(DetectCreatorNamesRequest request) throws CreatorDetectorConnectorException {
        return postRequest(DETECT_ENDPOINT, request, CreatorNameSuggestions.class);
    }

    @Override
    public void close() {
        if (failSafeHttpClient != null) {
            failSafeHttpClient.getClient().close();
        }
    }

    private <S, T> T postRequest(String path, Object requestBody, Class<T> returnType) throws CreatorDetectorConnectorException {
        final HttpPost httpPost = new HttpPost(failSafeHttpClient)
                .withBaseUrl(baseUrl)
                .withPathElements(path)
                .withJsonData(requestBody)
                .withHeader("Accept", "application/json")
                .withHeader("Content-type", "application/json");
        final Response response = httpPost.execute();
        assertResponseStatus(response, Response.Status.OK);
        return readResponseEntity(response, returnType);
    }

    private <T> T readResponseEntity(Response response, Class<T> type) throws CreatorDetectorConnectorException {
        final T entity = response.readEntity(type);
        if (entity == null) {
            throw new CreatorDetectorConnectorException(
                    String.format("Creator-Detector service returned with null-valued %s entity",
                            type.getName()));
        }
        return entity;
    }

    private void assertResponseStatus(Response response, Response.Status expectedStatus) throws CreatorDetectorConnectorException {
        final Response.Status actualStatus = Response.Status.fromStatusCode(response.getStatus());
        if (actualStatus != expectedStatus) {
            throw new CreatorDetectorConnectorUnexpectedStatusCodeException(
                    String.format("Creator-Detector service returned with unexpected status code: %s", actualStatus),
                    actualStatus.getStatusCode());
        }
    }
}
