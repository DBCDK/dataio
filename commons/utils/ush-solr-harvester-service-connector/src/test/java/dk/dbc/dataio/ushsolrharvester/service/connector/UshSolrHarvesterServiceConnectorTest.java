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

import dk.dbc.dataio.commons.types.rest.UshServiceConstants;
import dk.dbc.httpclient.HttpClient;
import dk.dbc.httpclient.PathBuilder;
import dk.dbc.dataio.commons.utils.test.rest.MockedResponse;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import javax.ws.rs.client.Client;
import javax.ws.rs.core.Response;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mockStatic;


@RunWith(PowerMockRunner.class)
@PrepareForTest({
        HttpClient.class,
})
public class UshSolrHarvesterServiceConnectorTest {
    private static final Client CLIENT = mock(Client.class);
    private static final String USH_SOLR_HARVESTER_URL = "http://dataio/ush-solr-harvester";

    @Before
    public void setup() throws Exception {
        mockStatic(HttpClient.class);
    }

    @Test(expected = NullPointerException.class)
    public void constructor_httpClientArgIsNull_throws() {
        new UshSolrHarvesterServiceConnector(null, USH_SOLR_HARVESTER_URL);
    }

    @Test(expected = NullPointerException.class)
    public void constructor_baseUrlArgIsNull_throws() {
        new UshSolrHarvesterServiceConnector(CLIENT, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructor_baseUrlArgIsEmpty_throws() {
        new UshSolrHarvesterServiceConnector(CLIENT, "");
    }

    @Test
    public void constructor_allArgsAreValid_returnsNewInstance() {
        final UshSolrHarvesterServiceConnector ushSolrHarvesterServiceConnector = newUshSolrHarvesterServiceConnector();
        assertThat(ushSolrHarvesterServiceConnector, is(notNullValue()));
        assertThat(ushSolrHarvesterServiceConnector.getHttpClient(), is(CLIENT));
        assertThat(ushSolrHarvesterServiceConnector.getBaseUrl(), is(USH_SOLR_HARVESTER_URL));
    }

    @Test(expected = UshSolrHarvesterServiceConnectorException.class)
    public void runTestHarvest_responseWithInternalServerErrorStatusCode_throws() throws UshSolrHarvesterServiceConnectorException {
        runTestHarvest_mockedHttpWithSpecifiedReturnErrorCode(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), "");
    }

    @Test(expected = UshSolrHarvesterServiceConnectorException.class)
    public void runTestHarvest_responseWithNotFoundStatusCode_throws() throws UshSolrHarvesterServiceConnectorException {
        runTestHarvest_mockedHttpWithSpecifiedReturnErrorCode(Response.Status.NOT_FOUND.getStatusCode(), "");
    }

    @Test
    public void runTestHarvest_logFound_returnsLog() throws UshSolrHarvesterServiceConnectorException {
        final String expectedUshSolrHarvesterId = "123";
        final String harvesterId = runTestHarvest_mockedHttpWithSpecifiedReturnErrorCode(Response.Status.CREATED.getStatusCode(), expectedUshSolrHarvesterId);
        assertThat(harvesterId, is(expectedUshSolrHarvesterId));
    }

    private String runTestHarvest_mockedHttpWithSpecifiedReturnErrorCode(int statusCode, Object returnValue) throws UshSolrHarvesterServiceConnectorException {
        final PathBuilder path = new PathBuilder(UshServiceConstants.HARVESTERS_USH_SOLR_TEST).bind(UshServiceConstants.ID_VARIABLE, 123);
        when(HttpClient.doPostWithJson(CLIENT, "", USH_SOLR_HARVESTER_URL, path.build())).thenReturn(new MockedResponse<>(statusCode, returnValue));
        final UshSolrHarvesterServiceConnector ushSolrHarvesterServiceConnector = newUshSolrHarvesterServiceConnector();
        return ushSolrHarvesterServiceConnector.runTestHarvest(123);
    }

    private UshSolrHarvesterServiceConnector newUshSolrHarvesterServiceConnector() {
        return new UshSolrHarvesterServiceConnector(CLIENT, USH_SOLR_HARVESTER_URL);
    }}