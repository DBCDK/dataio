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

package dk.dbc.dataio.ushsolrharvester.service.connector;

import dk.dbc.dataio.commons.time.StopWatch;
import dk.dbc.dataio.commons.types.rest.UshServiceConstants;
import dk.dbc.httpclient.HttpClient;
import dk.dbc.httpclient.PathBuilder;
import dk.dbc.invariant.InvariantUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.Client;
import javax.ws.rs.core.Response;

/**
 * UshSolrHarvesterServiceConnector - dataIO Ush Solr Harvester REST service client.
 * <p>
 * To use this class, you construct an instance, specifying a web resources client as well as
 * a base URL for the Ush Solr Harvester service endpoint you will be communicating with.
 * </p>
 * <p>
 * This class is thread safe, as long as the given web resources client remains thread safe.
 * </p>
 */
public class UshSolrHarvesterServiceConnector {
    private static final Logger log = LoggerFactory.getLogger(UshSolrHarvesterServiceConnector.class);

    private final Client httpClient;
    private final String baseUrl;

    /**
     * Class constructor
     * @param httpClient web resources client
     * @param baseUrl base URL for ush-solr-harvester service endpoint
     * @throws NullPointerException if given null-valued argument
     * @throws IllegalArgumentException if given empty-valued {@code baseUrl} argument
     */
    public UshSolrHarvesterServiceConnector(Client httpClient, String baseUrl) throws NullPointerException, IllegalArgumentException {
        this.httpClient = InvariantUtil.checkNotNullOrThrow(httpClient, "client");
        this.baseUrl = InvariantUtil.checkNotNullNotEmptyOrThrow(baseUrl, "baseUrl");
    }

    /**
     * Runs a Solr test harvest
     * @param id the id of the harvest to test run
     * @return the harvester id
     * @throws ProcessingException on general communication error
     * @throws UshSolrHarvesterServiceConnectorException on failure
     */
    public String runTestHarvest(final long id) throws ProcessingException, UshSolrHarvesterServiceConnectorException {
        final String NO_DATA = "";
        log.trace("UshSolrHarvesterServiceConnector: runTestHarvest({});", id);
        final StopWatch stopWatch = new StopWatch();
        try {
            final PathBuilder path = new PathBuilder(UshServiceConstants.HARVESTERS_USH_SOLR_TEST).bind(UshServiceConstants.ID_VARIABLE, id);
            final Response response = HttpClient.doPostWithJson(httpClient, NO_DATA, baseUrl, path.build());
            try {
                verifyResponseStatus(Response.Status.fromStatusCode(response.getStatus()), Response.Status.CREATED);
                final String jobId = readResponseEntity(response, String.class);
                if (jobId == null) {
                    throw new UshSolrHarvesterServiceConnectorException("Unable to run a Solr test harvest");
                }
                return jobId;
            } finally {
                response.close();
            }
        } finally {
            log.debug("UshSolrHarvesterServiceConnector: runTestHarvest took {} milliseconds", stopWatch.getElapsedTime());
        }
    }


    public Client getHttpClient() {
        return httpClient;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    private void verifyResponseStatus(Response.Status actualStatus, Response.Status expectedStatus) throws UshSolrHarvesterServiceConnectorException {
        if (actualStatus != expectedStatus) {
            throw new UshSolrHarvesterServiceConnectorUnexpectedStatusCodeException (
                    String.format("ush-solr-harvester service returned with unexpected status code: %s", actualStatus),
                    actualStatus.getStatusCode());
        }
    }

    private <T> T readResponseEntity(Response response, Class<T> tClass) throws UshSolrHarvesterServiceConnectorException {
        response.bufferEntity(); // must be done in order to possible avoid a timeout-exception from readEntity.
        final T entity = response.readEntity(tClass);
        if (entity == null) {
            throw new UshSolrHarvesterServiceConnectorException(
                    String.format("ush-solr-harvester service returned with null-valued %s entity", tClass.getName()));
        }
        return entity;
    }

}
