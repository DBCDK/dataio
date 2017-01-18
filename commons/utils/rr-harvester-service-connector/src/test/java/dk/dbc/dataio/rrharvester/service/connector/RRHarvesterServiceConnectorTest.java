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

package dk.dbc.dataio.rrharvester.service.connector;

import dk.dbc.dataio.commons.types.AddiMetaData;
import dk.dbc.dataio.commons.types.rest.RRHarvesterServiceConstants;
import dk.dbc.dataio.commons.utils.httpclient.HttpClient;
import dk.dbc.dataio.commons.utils.httpclient.PathBuilder;
import dk.dbc.dataio.commons.utils.test.rest.MockedResponse;
import dk.dbc.dataio.harvester.types.HarvestRecordsRequest;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import javax.ws.rs.client.Client;
import javax.ws.rs.core.Response;
import java.util.Collections;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.when;


@RunWith(PowerMockRunner.class)
@PrepareForTest({
        HttpClient.class,
})
public class RRHarvesterServiceConnectorTest {
    private static final Client CLIENT = mock(Client.class);

    @Before
    public void setup() throws Exception {
        mockStatic(HttpClient.class);
    }

    @Test(expected = NullPointerException.class)
    public void constructor_httpClientArgIsNull_throws() {
        new RRHarvesterServiceConnector(null, RRHarvesterServiceConstants.HARVEST_TASKS);
    }

    @Test(expected = NullPointerException.class)
    public void constructor_baseUrlArgIsNull_throws() {
        new RRHarvesterServiceConnector(CLIENT, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructor_baseUrlArgIsEmpty_throws() {
        new RRHarvesterServiceConnector(CLIENT, "");
    }

    @Test
    public void constructor_allArgsAreValid_returnsNewInstance() {
        final RRHarvesterServiceConnector serviceConnector = newRRHarvesterServiceConnector();
        assertThat(serviceConnector, is(notNullValue()));
        assertThat(serviceConnector.getHttpClient(), is(CLIENT));
        assertThat(serviceConnector.getBaseUrl(), is(RRHarvesterServiceConstants.HARVEST_TASKS));
    }

    @Test(expected = RRHarvesterServiceConnectorException.class)
    public void createHarvestTask_responseWithInternalServerErrorStatusCode_throws() throws RRHarvesterServiceConnectorException {
        createHarvestTask_mockedHttpWithSpecifiedReturnErrorCode(Response.Status.CREATED.getStatusCode(), null);
    }

    @Test(expected = RRHarvesterServiceConnectorUnexpectedStatusCodeException.class)
    public void createHarvestTask_responseWithNotFoundStatusCode_throws() throws RRHarvesterServiceConnectorException {
        createHarvestTask_mockedHttpWithSpecifiedReturnErrorCode(Response.Status.NOT_FOUND.getStatusCode(), null);
    }

    @Test
    public void createHarvestTask_harvestTaskCreated_returnsUri() throws RRHarvesterServiceConnectorException {
        final String expectedTaskId = "123";
        final String harvesterId = createHarvestTask_mockedHttpWithSpecifiedReturnErrorCode(Response.Status.CREATED.getStatusCode(), expectedTaskId);
        assertThat(harvesterId, is(expectedTaskId));
    }


    // Helper method
    private String createHarvestTask_mockedHttpWithSpecifiedReturnErrorCode(int statusCode, Object returnValue) throws RRHarvesterServiceConnectorException {
        final AddiMetaData addiMetaData = new AddiMetaData().withOcn("ocn").withPid("pid");
        final HarvestRecordsRequest harvestRecordsRequest = new HarvestRecordsRequest(Collections.singletonList(addiMetaData)).withBasedOnJob(1);
        final PathBuilder path = new PathBuilder(RRHarvesterServiceConstants.HARVEST_TASKS).bind(RRHarvesterServiceConstants.HARVEST_ID_VARIABLE, 12L);

        when(HttpClient.doPostWithJson(CLIENT, harvestRecordsRequest, RRHarvesterServiceConstants.HARVEST_TASKS, path.build()))
                .thenReturn(new MockedResponse<>(statusCode, returnValue));
        final RRHarvesterServiceConnector serviceConnector = newRRHarvesterServiceConnector();
        return serviceConnector.createHarvestTask(12L, harvestRecordsRequest);
    }

    private RRHarvesterServiceConnector newRRHarvesterServiceConnector() {
        return new RRHarvesterServiceConnector(CLIENT, RRHarvesterServiceConstants.HARVEST_TASKS);
    }}