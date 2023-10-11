package dk.dbc.dataio.sink.vip.connector;

import dk.dbc.httpclient.FailSafeHttpClient;
import dk.dbc.httpclient.HttpClient;
import dk.dbc.httpclient.HttpPost;
import dk.dbc.invariant.InvariantUtil;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.core.Response;
import net.jodah.failsafe.RetryPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;

/**
 * VIP-CORE service connector class
 * <p>
 * To use this class, you construct an instance, specifying a web resources client as well as
 * a base URL for the VIP-CORE service endpoint you will be communicating with.
 * </p>
 * <p>
 * This class is thread safe, as long as the given web resources client remains thread safe.
 * </p>
 */
public class VipCoreConnector {
    private static final Logger LOGGER = LoggerFactory.getLogger(VipCoreConnector.class);

    private static final RetryPolicy<Response> RETRY_POLICY = new RetryPolicy<Response>()
            .handle(ProcessingException.class)
            .handleResultIf(response ->
                    response.getStatus() == 404
                            || response.getStatus() == 500)
            .withDelay(Duration.ofSeconds(10))
            .withMaxRetries(6);

    public static class Error {
        private String message;
        private String exception;

        public String getMessage() {
            return message;
        }

        public String getException() {
            return exception;
        }

        @Override
        public String toString() {
            return "Error{" +
                    "message='" + message + '\'' +
                    ", exception='" + exception + '\'' +
                    '}';
        }
    }

    private final FailSafeHttpClient failSafeHttpClient;
    private final String baseUrl;

    /**
     * Class constructor
     *
     * @param httpClient web resources client
     * @param baseUrl    base URL for job-store service endpoint
     * @throws NullPointerException     if given null-valued argument
     * @throws IllegalArgumentException if given empty-valued {@code baseUrl} argument
     */
    public VipCoreConnector(Client httpClient, String baseUrl)
            throws NullPointerException, IllegalArgumentException {
        this(FailSafeHttpClient.create(httpClient, RETRY_POLICY), baseUrl);
    }

    VipCoreConnector(FailSafeHttpClient failSafeHttpClient, String baseUrl) {
        this.failSafeHttpClient = InvariantUtil.checkNotNullOrThrow(failSafeHttpClient, "failSafeHttpClient");
        this.baseUrl = InvariantUtil.checkNotNullNotEmptyOrThrow(baseUrl, "baseUrl");
    }

    public void close() {
        HttpClient.closeClient(failSafeHttpClient.getClient());
    }

    /**
     * Uploads data from 'Kulturstyrelsen' to VIP
     *
     * @param kind   type of record
     * @param entity BIBBAS record as JSON string
     * @throws VipCoreConnectorException on failure to upload data
     */
    public void vipload(String kind, String entity) throws VipCoreConnectorException {
        InvariantUtil.checkNotNullNotEmptyOrThrow(kind, "kind");
        InvariantUtil.checkNotNullNotEmptyOrThrow(entity, "entity");
        final Response response = new HttpPost(failSafeHttpClient)
                .withBaseUrl(baseUrl)
                .withPathElements("api", "vipload", kind)
                .withJsonData(entity)
                .execute();
        try {
            verifyResponseStatus(response, Response.Status.OK);
        } finally {
            response.close();
        }
    }

    private void verifyResponseStatus(Response response, Response.Status expectedStatus)
            throws VipCoreConnectorException {
        final Response.Status actualStatus = Response.Status.fromStatusCode(response.getStatus());
        if (actualStatus != expectedStatus) {
            final VipCoreConnectorUnexpectedStatusCodeException exception =
                    new VipCoreConnectorUnexpectedStatusCodeException(String.format(
                            "VIP-CORE service returned with unexpected status code: %s",
                            actualStatus), actualStatus.getStatusCode());

            if (response.hasEntity()) {
                try {
                    exception.setError(readResponseEntity(response, VipCoreConnector.Error.class));
                } catch (VipCoreConnectorException | ProcessingException e) {
                    try {
                        LOGGER.error("Request sent to {} returned: {}",
                                HttpClient.getRemoteHostAddress(baseUrl),
                                readResponseEntity(response, String.class));
                    } catch (VipCoreConnectorException fatal) {
                        LOGGER.warn("Unable to extract entity from response", fatal);
                    }
                    LOGGER.warn("Unable to extract error from response", e);
                }
            }
            throw exception;
        }
    }

    private <T> T readResponseEntity(Response response, Class<T> tClass)
            throws VipCoreConnectorException {
        response.bufferEntity();
        final T entity = response.readEntity(tClass);
        if (entity == null) {
            throw new VipCoreConnectorException(String.format(
                    "VIP-CORE service returned with null-valued %s entity", tClass.getName()));
        }
        return entity;
    }
}
