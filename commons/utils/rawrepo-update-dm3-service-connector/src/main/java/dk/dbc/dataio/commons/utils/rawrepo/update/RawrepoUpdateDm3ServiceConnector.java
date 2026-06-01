package dk.dbc.dataio.commons.utils.rawrepo.update;

import dk.dbc.commons.useragent.UserAgent;
import dk.dbc.dataio.commons.types.Constants;
import dk.dbc.httpclient.FailSafeHttpClient;
import dk.dbc.httpclient.HttpPost;
import dk.dbc.invariant.InvariantUtil;
import dk.dbc.updateservice.dto.UpdateRequest;
import dk.dbc.updateservice.dto.UpdateResponse;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.core.Response;
import net.jodah.failsafe.RetryPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;

/**
 * REST connector for the rawrepo-v3 update-service.
 * Instances of this class are thread safe as long as the underlying HTTP client is thread safe.
 */
public class RawrepoUpdateDm3ServiceConnector {
    private static final Logger LOGGER = LoggerFactory.getLogger(RawrepoUpdateDm3ServiceConnector.class);

    private static final String[] UPDATE_PATH = {"api", "v1", "update", "dbc"};
    private static final String[] VALIDATE_PATH = {"api", "v1", "update", "dbc", "validate"};

    private static final RetryPolicy<Response> RETRY_POLICY = new RetryPolicy<Response>()
            .handle(ProcessingException.class)
            .handleResultIf(response ->
                    response.getStatus() == 404
                            || response.getStatus() == 502
                            || response.getStatus() == 503)
            .withDelay(Duration.ofSeconds(10))
            .withMaxRetries(6);

    private final FailSafeHttpClient failSafeHttpClient;
    private final String baseUrl;
    private final boolean validateOnly;

    public RawrepoUpdateDm3ServiceConnector(Client httpClient, String baseUrl) {
        this(FailSafeHttpClient.create(httpClient, new UserAgent(RawrepoUpdateDm3ServiceConnector.class.getName()), RETRY_POLICY), baseUrl);
    }

    /* package-private for testing */
    RawrepoUpdateDm3ServiceConnector(FailSafeHttpClient failSafeHttpClient, String baseUrl) {
        this.failSafeHttpClient = InvariantUtil.checkNotNullOrThrow(failSafeHttpClient, "failSafeHttpClient");
        this.baseUrl = InvariantUtil.checkNotNullNotEmptyOrThrow(baseUrl, "baseUrl");
        final String validateOnlyEnv = System.getenv(Constants.UPDATE_VALIDATE_ONLY_FLAG);
        this.validateOnly = "true".equalsIgnoreCase(validateOnlyEnv);
        if (validateOnlyEnv != null && !validateOnly) {
            LOGGER.warn("{} had unexpected value {}", Constants.UPDATE_VALIDATE_ONLY_FLAG, validateOnlyEnv);
        }
    }

    /**
     * Sends an update request to the rawrepo update service.
     * Routes to the validate-only endpoint when UPDATE_VALIDATE_ONLY_FLAG is set.
     *
     * @param request the update request including authentication, template and record
     * @return the service response with status and any validation errors
     * @throws RawrepoUpdateServiceConnectorException on communication failure or unexpected HTTP status
     */
    public UpdateResponse updateRecord(UpdateRequest request) throws RawrepoUpdateServiceConnectorException {
        InvariantUtil.checkNotNullOrThrow(request, "request");
        LOGGER.trace("updateRecord to {}, validateOnly={}", baseUrl, validateOnly);
        final String[] path = validateOnly ? VALIDATE_PATH : UPDATE_PATH;
        try (Response response = new HttpPost(failSafeHttpClient)
                .withBaseUrl(baseUrl)
                .withPathElements(path)
                .withJsonData(request)
                .execute()) {
            verifyResponseStatus(response);
            return readResponseEntity(response, UpdateResponse.class);
        }
    }

    private void verifyResponseStatus(Response response)
            throws RawrepoUpdateServiceConnectorException {
        if (response.getStatus() != Response.Status.OK.getStatusCode()) {
            throw new RawrepoUpdateServiceConnectorException(String.format(
                    "Rawrepo update service returned HTTP %d", response.getStatus()));
        }
    }

    private <T> T readResponseEntity(Response response, Class<T> type)
            throws RawrepoUpdateServiceConnectorException {
        final T entity = response.readEntity(type);
        if (entity == null) {
            throw new RawrepoUpdateServiceConnectorException(
                    "Rawrepo update service returned null entity");
        }
        return entity;
    }
}
