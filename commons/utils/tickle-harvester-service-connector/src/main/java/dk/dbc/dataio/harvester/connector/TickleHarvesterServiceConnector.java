/*
 * DataIO - Data IO
 *
 * Copyright (C) 2017 Dansk Bibliotekscenter a/s, Tempovej 7-11, DK-2750 Ballerup,
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

package dk.dbc.dataio.harvester.connector;

import dk.dbc.httpclient.FailSafeHttpClient;
import dk.dbc.httpclient.HttpGet;
import dk.dbc.dataio.harvester.task.connector.HarvesterTaskServiceConnector;
import dk.dbc.dataio.harvester.task.connector.HarvesterTaskServiceConnectorException;

import javax.ws.rs.client.Client;
import javax.ws.rs.core.Response;

public class TickleHarvesterServiceConnector extends HarvesterTaskServiceConnector {
    public TickleHarvesterServiceConnector(Client httpClient, String baseUrl)
            throws NullPointerException, IllegalArgumentException {
        super(httpClient, baseUrl);
    }

    public TickleHarvesterServiceConnector(FailSafeHttpClient failSafeHttpClient, String baseUrl) {
        super(failSafeHttpClient, baseUrl);
    }

    public int getDataSetSizeEstimate(int dataSetId) throws HarvesterTaskServiceConnectorException {
        return fetchDataSetSizeEstimate(String.valueOf(dataSetId));
    }

    public int getDataSetSizeEstimate(String dataSetName) throws HarvesterTaskServiceConnectorException {
        return fetchDataSetSizeEstimate(dataSetName);
    }

    private int fetchDataSetSizeEstimate(String idPathParam) throws HarvesterTaskServiceConnectorException {
        final Response response = new HttpGet(failSafeHttpClient)
                .withBaseUrl(baseUrl)
                .withPathElements("dataset", idPathParam, "size-estimate")
                .execute();
        try {
            verifyResponseStatus(Response.Status.fromStatusCode(response.getStatus()), Response.Status.OK);
            return readResponseEntity(response, Integer.class);
        } finally {
            response.close();
        }
    }
}