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

package dk.dbc.dataio.commons.utils.ush.solr;

import dk.dbc.dataio.commons.time.StopWatch;
import dk.dbc.dataio.commons.types.ServiceError;
import dk.dbc.dataio.commons.types.rest.UshServiceConstants;
import dk.dbc.dataio.commons.utils.httpclient.HttpClient;
import dk.dbc.dataio.commons.utils.httpclient.PathBuilder;
import dk.dbc.dataio.commons.utils.invariant.InvariantUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.client.Client;
import javax.ws.rs.core.Response;

public class UshSolrHarvesterConnector {
    private static final Logger LOGGER = LoggerFactory.getLogger(UshSolrHarvesterConnector.class);
    private final Client httpClient;
    private final String baseUrl;

    /**
     * Class constructor
     *
     * @param httpClient web resources client
     * @param baseUrl    base URL for ush harvester endpoint
     * @throws NullPointerException     if given null-valued argument
     * @throws IllegalArgumentException if given empty-valued {@code baseUrl} argument
     */
    public UshSolrHarvesterConnector(Client httpClient, String baseUrl) throws NullPointerException, IllegalArgumentException {
        this.httpClient = InvariantUtil.checkNotNullOrThrow(httpClient, "httpClient");
        this.baseUrl = InvariantUtil.checkNotNullNotEmptyOrThrow(baseUrl, "baseUrl");
    }

    public Client getHttpClient() {
        return httpClient;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    /**
     * Executes a test harvest based a ush solr configuration
     * @param ushSolrHarvesterConfigId the id of of the configuration to use for the test harvest
     * @return id of the job generated
     * @throws UshSolrHarvesterConnectorException on general failure to execute test harvest
     */
    public int runTestHarvest(int ushSolrHarvesterConfigId) throws UshSolrHarvesterConnectorException {
        LOGGER.trace("UshSolrHarvesterConnector: runTestHarvest();");
        final StopWatch stopWatch = new StopWatch();
        try {
            InvariantUtil.checkIntLowerBoundOrThrow(ushSolrHarvesterConfigId, "ushSolrHarvesterConfigId", 0);
            final Response response;
            final PathBuilder path = new PathBuilder(UshServiceConstants.HARVESTERS_USH_SOLR_TEST)
                    .bind(UshServiceConstants.ID_VARIABLE, Long.toString(ushSolrHarvesterConfigId));
                response = HttpClient.doPostWithJson(httpClient, "", baseUrl, path.build());
            try {
                verifyResponseStatus(response, Response.Status.CREATED);
                return Integer.valueOf(response.getLocation().toString());
            } finally {
                response.close();
            }
        } finally {
            LOGGER.debug("UshSolrHarvesterConnector: runTestHarvest took {} milliseconds", stopWatch.getElapsedTime());
        }
    }

    /*
     * Private methods
     */

    private void verifyResponseStatus(Response response, Response.Status expectedStatus) throws UshSolrHarvesterConnectorUnexpectedStatusCodeException {
        final Response.Status actualStatus = Response.Status.fromStatusCode(response.getStatus());
        if (actualStatus != expectedStatus) {
            final UshSolrHarvesterConnectorUnexpectedStatusCodeException exception =
                    new UshSolrHarvesterConnectorUnexpectedStatusCodeException(String.format(
                            "ush-solr harvester service returned with unexpected status code: %s", actualStatus), actualStatus.getStatusCode());
            if (actualStatus == Response.Status.INTERNAL_SERVER_ERROR) {
                try {
                    exception.setServiceError(readResponseEntity(response, ServiceError.class));
                } catch (UshSolrHarvesterConnectorException e) {
                    LOGGER.warn("Unable to extract service error from response", e);
                }
            }
            throw exception;
        }
    }

    private <T> T readResponseEntity(Response response, Class<T> tClass) throws UshSolrHarvesterConnectorException {
        response.bufferEntity(); // must be done in order to possible avoid a timeout-exception from readEntity.
        final T entity = response.readEntity(tClass);
        if (entity == null) {
            throw new UshSolrHarvesterConnectorException(
                    String.format("ush-solr harvester service returned with null-valued %s entity", tClass.getName()));
        }
        return entity;
    }
}
