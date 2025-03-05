package dk.dbc.dataio.urlresolver.service.connector;

import dk.dbc.dataio.commons.time.StopWatch;
import dk.dbc.httpclient.HttpClient;
import dk.dbc.httpclient.HttpGet;
import dk.dbc.invariant.InvariantUtil;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class UrlResolverServiceConnector {

    private static final Logger log = LoggerFactory.getLogger(UrlResolverServiceConnector.class);

    private final HttpClient httpClient;
    private final String baseUrl;

    public UrlResolverServiceConnector(Client httpClient, String baseUrl) throws NullPointerException, IllegalArgumentException {
        this.httpClient = HttpClient.create(InvariantUtil.checkNotNullOrThrow(httpClient, "client"));
        this.baseUrl = InvariantUtil.checkNotNullNotEmptyOrThrow(baseUrl, "baseUrl");
    }

    /**
     * Retrieves all urls used by the system.
     *
     * @return map containing all known urls mapped by: (url name, url value (can be null))
     * @throws UrlResolverServiceConnectorException                     on general communication failure
     * @throws UrlResolverServiceConnectorUnexpectedStatusCodeException on unexpected response status code
     */
    public Map<String, String> getUrls() throws UrlResolverServiceConnectorException {
        final StopWatch stopWatch = new StopWatch();
        try {
            final Response response = new HttpGet(httpClient).withBaseUrl(baseUrl).withPathElements("urls").execute();
            try {
                verifyResponseStatus(response, Response.Status.OK);
                return readResponseEntity(response, new GenericType<Map<String, String>>() {
                });
            } finally {
                response.close();
            }
        } finally {
            log.debug("getUrls took {} milliseconds", stopWatch.getElapsedTime());
        }
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    /*
     * Private methods
     */

    private <T> T readResponseEntity(Response response, GenericType<T> genericType) throws UrlResolverServiceConnectorException {
        response.bufferEntity(); // must be done in order to possible avoid a timeout-exception from readEntity.
        final T entity = response.readEntity(genericType);
        if (entity == null) {
            throw new UrlResolverServiceConnectorException(String.format("url-resolver service returned with null-valued %s entity", genericType.getRawType().getName()));
        }
        return entity;
    }

    private void verifyResponseStatus(Response response, Response.Status expectedStatus) throws UrlResolverServiceConnectorUnexpectedStatusCodeException {
        final Response.Status actualStatus = Response.Status.fromStatusCode(response.getStatus());
        if (actualStatus != expectedStatus) {
            throw new UrlResolverServiceConnectorUnexpectedStatusCodeException(String.format(
                    "url-resolver service returned with unexpected status code: %s", actualStatus), actualStatus.getStatusCode());
        }
    }
}
