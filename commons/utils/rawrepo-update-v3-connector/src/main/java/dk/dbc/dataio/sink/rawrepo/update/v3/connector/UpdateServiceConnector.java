package dk.dbc.dataio.sink.rawrepo.update.v3.connector;

import dk.dbc.commons.useragent.UserAgent;
import dk.dbc.httpclient.FailSafeHttpClient;
import dk.dbc.httpclient.HttpClient;
import dk.dbc.httpclient.HttpPost;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.core.Response;
import net.jodah.failsafe.RetryPolicy;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;

/**
 * Connector for the rawrepo update service REST API (v3).
 */
public class UpdateServiceConnector implements AutoCloseable {
    private static final Logger LOGGER = LoggerFactory.getLogger(UpdateServiceConnector.class);
    private static final String UPDATE_PATH = "/api/v2/update/";

    private static final RetryPolicy<Response> RETRY_POLICY = new RetryPolicy<Response>()
            .handle(ProcessingException.class)
            .handleResultIf(response -> response.getStatus() == 500
                    || response.getStatus() == 502
                    || response.getStatus() == 503)
            .withDelay(Duration.ofSeconds(5))
            .withMaxRetries(3);

    private final FailSafeHttpClient failSafeHttpClient;
    private final String baseUrl;

    /**
     * Creates a connector backed by a new JAX-RS client.
     * Expects the APP_NAME environment variable to be set for user-agent creation in requests.
     * @param baseUrl base URL of the update service, e.g. {@code "https://update.example.com"}
     */
    public UpdateServiceConnector(String baseUrl) {
        this(FailSafeHttpClient.create(
                HttpClient.newClient(new ClientConfig().register(new JacksonFeature())),
                UserAgent.forInternalRequests(),
                RETRY_POLICY), baseUrl);
    }

    /**
     * Creates a connector with a pre-built HTTP client — intended for testing and
     * environments where the client lifecycle is managed externally.
     *
     * @param failSafeHttpClient configured HTTP client with retry policy
     * @param baseUrl            base URL of the update service
     */
    public UpdateServiceConnector(FailSafeHttpClient failSafeHttpClient, String baseUrl) {
        this.failSafeHttpClient = failSafeHttpClient;
        this.baseUrl = baseUrl;
    }

    @Override
    public void close() {
        failSafeHttpClient.getClient().close();
    }

    /**
     * Submits a record as MarcJSON to the update service for persistence.
     * The URL path segment is taken from {@link UpdateRequest#getType()} (defaults to {@code "dbc"}).
     * The type field is never included in the JSON body sent to the service.
     * @param request update request carrying type, authentication, template name, and MarcJSON content
     * @return the service response; inspect {@link UpdateResponse#getStatus()} for outcome
     * @throws UpdateServiceConnectorException on HTTP 401, unexpected status codes, or I/O errors
     */
    public UpdateResponse update(UpdateRequest request) throws UpdateServiceConnectorException {
        return post(UPDATE_PATH + request.getType(), request);
    }

    /**
     * Submits a record as MarcJSON to the update service for validation only — no data is persisted.
     * The URL path segment is taken from {@link UpdateRequest#getType()} (defaults to {@code "dbc"}).
     * The type field is never included in the JSON body sent to the service.
     * @param request update request carrying type, authentication, template name, and MarcJSON content
     * @return the service response; inspect {@link UpdateResponse#getStatus()} for outcome
     * @throws UpdateServiceConnectorException on HTTP 401, unexpected status codes, or I/O errors
     */
    public UpdateResponse validate(UpdateRequest request) throws UpdateServiceConnectorException {
        return post(UPDATE_PATH + request.getType() + "/validate", request);
    }

    private UpdateResponse post(String path, UpdateRequest request) throws UpdateServiceConnectorException {
        LOGGER.debug("POST {}{}", baseUrl, path);
        try (Response response = new HttpPost(failSafeHttpClient)
                .withBaseUrl(baseUrl)
                .withPathElements(path)
                .withJsonData(request)
                .withHeader("Accept", "application/json")
                .execute()) {
            assertResponseStatus(response);
            return readResponseEntity(response);
        }
    }

    private void assertResponseStatus(Response response) throws UpdateServiceConnectorUnexpectedStatusCodeException {
        Response.Status status = Response.Status.fromStatusCode(response.getStatus());
        // HTTP 400 BAD_REQUEST is accepted: the service returns structured validation errors on that status
        if (status != Response.Status.OK && status != Response.Status.BAD_REQUEST) {
            throw new UpdateServiceConnectorUnexpectedStatusCodeException(
                    "Update service returned unexpected HTTP " + response.getStatus(),
                    response.getStatus());
        }
    }

    private UpdateResponse readResponseEntity(Response response) throws UpdateServiceConnectorException {
        UpdateResponse entity = response.readEntity(UpdateResponse.class);
        if (entity == null) {
            throw new UpdateServiceConnectorException(
                    String.format("Update service returned with null-valued %s entity",
                            UpdateResponse.class.getName()));
        }
        if (entity.getStatus() == null) {
            throw new UpdateServiceConnectorException(
                    "Update service returned response with missing status field");
        }
        return entity;
    }
}
