package dk.dbc.dataio.commons.retriever.connector;

import dk.dbc.dataio.commons.retriever.connector.model.ArticlesRequest;
import dk.dbc.dataio.commons.retriever.connector.model.ArticlesResponse;
import dk.dbc.dataio.commons.retriever.connector.model.ErrorResponse;
import dk.dbc.httpclient.FailSafeHttpClient;
import dk.dbc.httpclient.HttpPost;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.core.Response;
import net.jodah.failsafe.RetryPolicy;

import java.time.Duration;
import java.util.Optional;

/**
 * Connector for interacting with the Retriever service API.
 * <p>
 * This class provides a client interface for communicating with a remote Retriever service,
 * handling HTTP communication, authentication, and automatic retry logic for transient failures.
 * The connector automatically retries requests on specific HTTP status codes (404, 500, 502)
 * and processing exceptions, with a configurable delay and maximum retry count.
 * <p>
 * Thread-safety: This class is thread-safe if the underlying HTTP client is thread-safe.
 * <p>
 * The connector implements AutoCloseable to properly release HTTP client resources when no longer needed.
 */
public class RetrieverConnector implements AutoCloseable {

    private static final String ARTICLES_SEARCH_ENDPOINT = "/articles/search";

    private static final RetryPolicy<Response> RETRY_POLICY = new RetryPolicy<Response>()
            .handle(ProcessingException.class)
            .handleResultIf(response ->
                    response.getStatus() == 404
                            || response.getStatus() == 500
                            || response.getStatus() == 502)
            .withDelay(Duration.ofSeconds(2))
            .withMaxRetries(30);

    private final FailSafeHttpClient failSafeHttpClient;
    private final String baseUrl;
    private final String apiKey;

    public RetrieverConnector(Client httpClient, String baseUrl, String apiKey) {
        this(FailSafeHttpClient.create(httpClient, RETRY_POLICY), baseUrl, apiKey);
    }

    /**
     * Returns new instance with custom retry policy
     * @param failSafeHttpClient web resources client with custom retry policy
     * @param baseUrl base URL for service endpoint
     * @param apiKey API key for service endpoint
     */
    public RetrieverConnector(FailSafeHttpClient failSafeHttpClient, String baseUrl, String apiKey) {
        if (failSafeHttpClient == null) {
            throw new NullPointerException("Value of parameter failSafeHttpClient cannot be null");
        }
        if (baseUrl == null) {
            throw new NullPointerException("Value of parameter baseUrl cannot be null");
        }
        if (baseUrl.isBlank()) {
            throw new IllegalArgumentException("Value of parameter baseUrl cannot be empty");
        }
        if (apiKey == null) {
            throw new NullPointerException("Value of parameter apiKey cannot be null");
        }
        if (apiKey.isBlank()) {
            throw new IllegalArgumentException("Value of parameter apiKey cannot be empty");
        }
        this.failSafeHttpClient = failSafeHttpClient;
        this.baseUrl = baseUrl;
        this.apiKey =  String.format("Bearer %s", apiKey);
    }

    @Override
    public void close() {
        if (failSafeHttpClient != null) {
            failSafeHttpClient.getClient().close();
        }
    }

    /**
     * Search articles based on the provided request.
     * @param request search request
     * @return articles response
     * @throws RetrieverConnectorException if the request fails or the response is invalid
     */
    public ArticlesResponse searchArticles(ArticlesRequest request) throws RetrieverConnectorException {
        return postRequest(ARTICLES_SEARCH_ENDPOINT, request, ArticlesResponse.class);
    }

    private <S, T> T postRequest(String path, Object requestBody, Class<T> returnType) throws RetrieverConnectorException {
        final HttpPost httpPost = new HttpPost(failSafeHttpClient)
                .withBaseUrl(baseUrl)
                .withPathElements(path)
                .withJsonData(requestBody)
                .withHeader("Accept", "application/json")
                .withHeader("Content-type", "application/json")
                .withHeader("Authorization", apiKey);
        final Response response = httpPost.execute();
        assertResponseStatus(response, Response.Status.OK);
        return readResponseEntity(response, returnType);
    }

    private <T> T readResponseEntity(Response response, Class<T> type) throws RetrieverConnectorException {
        final T entity = response.readEntity(type);
        if (entity == null) {
            throw new RetrieverConnectorException(
                    String.format("Retriever service returned with null-valued %s entity",
                            type.getName()));
        }
        return entity;
    }

    private Optional<ErrorResponse> readErrorResponse(Response response) {
        try {
            return Optional.ofNullable(response.readEntity(ErrorResponse.class));
        } catch (RuntimeException e) {
            e.printStackTrace();
            return Optional.empty();
        }
    }

    private void assertResponseStatus(Response response, Response.Status expectedStatus) throws RetrieverConnectorException {
        final Response.Status actualStatus = Response.Status.fromStatusCode(response.getStatus());
        if (actualStatus != expectedStatus) {
            String errorMessage = readErrorResponse(response)
                .map(error -> " and message: " + error.message())
                .orElse("");
            throw new RetrieverConnectorUnexpectedStatusCodeException(
                    String.format("Retriever service returned with unexpected status code: <%s>%s", actualStatus, errorMessage),
                    actualStatus.getStatusCode());
        }
    }
}
