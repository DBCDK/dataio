/*
 * DataIO - Data IO
 * Copyright (C) 2015 Dansk Bibliotekscenter a/s, Tempovej 7-11, DK-2750 Ballerup,
 * Denmark. CVR: 15149043
 *
 * This file is part of DataIO.
 *
 * DataIO is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * DataIO is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with DataIO.  If not, see <http://www.gnu.org/licenses/>.
 */

package dk.dbc.dataio.urlresolver.service.connector;

import dk.dbc.dataio.commons.time.StopWatch;
import dk.dbc.httpclient.HttpClient;
import dk.dbc.dataio.commons.utils.invariant.InvariantUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.client.Client;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;
import java.util.Map;

public class UrlResolverServiceConnector {

    private static final Logger log = LoggerFactory.getLogger(UrlResolverServiceConnector.class);

    private final Client httpClient;
    private final String baseUrl;

    public UrlResolverServiceConnector(Client httpClient, String baseUrl) throws NullPointerException, IllegalArgumentException {
        this.httpClient = InvariantUtil.checkNotNullOrThrow(httpClient, "client");
        this.baseUrl = InvariantUtil.checkNotNullNotEmptyOrThrow(baseUrl, "baseUrl");
    }

    /**
     * Retrieves all urls used by the system.
     * @return map containing all known urls mapped by: (url name, url value (can be null))
     * @throws UrlResolverServiceConnectorException on general communication failure
     * @throws UrlResolverServiceConnectorUnexpectedStatusCodeException on unexpected response status code
     */
    public Map<String, String> getUrls() throws UrlResolverServiceConnectorException {
        final StopWatch stopWatch = new StopWatch();
        try {
            final Response response = HttpClient.doGet(httpClient, baseUrl, "urls");
            try {
                verifyResponseStatus(response, Response.Status.OK);
                return readResponseEntity(response, new GenericType<Map<String, String>>() {});
            } finally {
                response.close();
            }
        } finally {
            log.debug("getUrls took {} milliseconds", stopWatch.getElapsedTime());
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
