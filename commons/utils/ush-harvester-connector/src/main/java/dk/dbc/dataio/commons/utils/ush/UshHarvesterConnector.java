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

package dk.dbc.dataio.commons.utils.ush;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import dk.dbc.dataio.commons.time.StopWatch;
import dk.dbc.httpclient.HttpClient;
import dk.dbc.invariant.InvariantUtil;
import dk.dbc.dataio.commons.utils.ush.bindings.Harvestables;
import dk.dbc.dataio.harvester.types.UshHarvesterProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.client.Client;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class UshHarvesterConnector {
    private static final Logger LOGGER = LoggerFactory.getLogger(UshHarvesterConnector.class);
    private final Client httpClient;
    private final String baseUrl;
    private final XmlMapper mapper;

    /**
     * Class constructor
     *
     * @param httpClient web resources client
     * @param baseUrl    base URL for ush harvester endpoint
     * @throws NullPointerException     if given null-valued argument
     * @throws IllegalArgumentException if given empty-valued {@code baseUrl} argument
     */
    public UshHarvesterConnector(Client httpClient, String baseUrl) throws NullPointerException, IllegalArgumentException {
        this.httpClient = InvariantUtil.checkNotNullOrThrow(httpClient, "client");
        this.baseUrl = InvariantUtil.checkNotNullNotEmptyOrThrow(baseUrl, "baseUrl");
        this.mapper = new XmlMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    public Client getHttpClient() {
        return httpClient;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    /**
     * Retrieves list of ush harvester properties from the harvester service
     * @return list of selected ush harvester properties
     * @throws UshHarvesterConnectorException on general failure to produce ush harvester properties listing
     */
    public List<UshHarvesterProperties> listUshHarvesterJobs() throws UshHarvesterConnectorException {
        LOGGER.trace("UshHarvesterConnector: listUshHarvesterJobs();");
        final StopWatch stopWatch = new StopWatch();
        try {
            final Response response = HttpClient.doGet(httpClient, baseUrl, "records", "harvestables");
            try {
                verifyResponseStatus(Response.Status.fromStatusCode(response.getStatus()), Response.Status.OK);
                String stringEntity = readResponseEntity(response, String.class);
                return getUshHarvesterProperties(stringEntity);
            } catch (UshHarvesterConnectorException e) {
                throw e;
            } catch (Exception e) {
                throw new UshHarvesterConnectorException("Unexpected error caught: " + e.toString());
            } finally {
                response.close();
            }
        } finally {
            LOGGER.debug("UshHarvesterConnector: listUshHarvesterJobs took {} milliseconds", stopWatch.getElapsedTime());
        }
    }

    /**
     * Indexes retrieved list of ush harvester properties by (key)jobId - (value)UshHarvesterProperties
     * @return indexed map
     * @throws UshHarvesterConnectorException on general failure to produce ush harvester properties listing
     */
    public Map<Integer, UshHarvesterProperties> listIndexedUshHarvesterJobs() throws UshHarvesterConnectorException {
        return Collections.unmodifiableMap(listUshHarvesterJobs().stream().collect(Collectors.toMap(UshHarvesterProperties::getId, c -> c)));
    }

    /*
     * Private methods
     */

    private void verifyResponseStatus(Response.Status actualStatus, Response.Status expectedStatus) throws UshHarvesterConnectorUnexpectedStatusCodeException {
        if (actualStatus != expectedStatus) {
            throw new UshHarvesterConnectorUnexpectedStatusCodeException(
                    String.format("ush-harvester returned with unexpected status code: %s", actualStatus),
                    actualStatus.getStatusCode());
        }
    }

    private <T> T readResponseEntity(Response response, Class<T> tClass) throws UshHarvesterConnectorException {
        response.bufferEntity(); // must be done in order to possible avoid a timeout-exception from readEntity.
        final T entity = response.readEntity(tClass);
        if (entity == null) {
            throw new UshHarvesterConnectorException(
                    String.format("ush harvester service returned with null-valued %s entity", tClass.getName()));
        }
        return entity;
    }


    private List<UshHarvesterProperties> getUshHarvesterProperties(String entityString) throws UshHarvesterConnectorException {
        try {
            return mapper.readValue(entityString, Harvestables.class).getUshHarvesterProperties();
        } catch (IOException e) {
            throw new UshHarvesterConnectorException(String.format("invalid input xml:{%s} could not be read", entityString));
        }
    }
}
