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

package dk.dbc.dataio.common.utils.flowstore;

import dk.dbc.dataio.commons.types.rest.FlowStoreServiceConstants;
import dk.dbc.dataio.commons.utils.httpclient.HttpClient;
import dk.dbc.dataio.commons.utils.httpclient.PathBuilder;
import dk.dbc.dataio.commons.utils.test.rest.MockedResponse;
import dk.dbc.dataio.harvester.types.HarvesterConfig;
import dk.dbc.dataio.harvester.types.RRHarvesterConfig;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import javax.ws.rs.core.Response;

import static dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnectorTestHelper.CLIENT;
import static dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnectorTestHelper.FLOW_STORE_URL;
import static dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnectorTestHelper.newFlowStoreServiceConnector;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mockStatic;


@RunWith(PowerMockRunner.class)
@PrepareForTest({
        HttpClient.class,
})
public class FlowStoreServiceConnector_HarvesterConfigs_Test {

    private static final String EMPTY = "";

    @Before
    public void setup() throws Exception {
        mockStatic(HttpClient.class);
    }

    // ****************************************** create harvester config tests ******************************************

    @Test(expected = NullPointerException.class)
    public void createHarvesterConfig_harvesterConfigContentArgIsNull_throws() throws FlowStoreServiceConnectorException {
        final FlowStoreServiceConnector instance = newFlowStoreServiceConnector();
        instance.createHarvesterConfig(null, RRHarvesterConfig.class);
    }

    @Test(expected = NullPointerException.class)
    public void createHarvesterConfig_harvesterTypeArgIsNull_throws() throws FlowStoreServiceConnectorException {
        final FlowStoreServiceConnector instance = newFlowStoreServiceConnector();
        instance.createHarvesterConfig(new RRHarvesterConfig.Content(), null);
    }

    @Test
    public void createHarvesterConfig_RRHarvesterConfigIsCreated_returnsHarvesterConfig() throws FlowStoreServiceConnectorException {
        final RRHarvesterConfig.Content configContent = new RRHarvesterConfig.Content();
        final HarvesterConfig expectedHarvesterConfig = new RRHarvesterConfig(42, 1, configContent);
        final HarvesterConfig harvesterConfig = createHarvesterConfig_mockedHttpWithSpecifiedReturnErrorCode(
                Response.Status.CREATED.getStatusCode(),
                expectedHarvesterConfig
        );

        assertThat(harvesterConfig, is(expectedHarvesterConfig));
        assertThat(harvesterConfig.getType(), is(RRHarvesterConfig.class.getName()));
    }

    @Test(expected = FlowStoreServiceConnectorException.class)
    public void createHarvesterConfig_responseWithUnexpectedStatusCode_throws() throws FlowStoreServiceConnectorException {
        createHarvesterConfig_mockedHttpWithSpecifiedReturnErrorCode(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), EMPTY);
    }

    @Test(expected = FlowStoreServiceConnectorException.class)
    public void createHarvesterConfig_responseWithNullEntity_throws() throws FlowStoreServiceConnectorException {
        createHarvesterConfig_mockedHttpWithSpecifiedReturnErrorCode(Response.Status.CREATED.getStatusCode(), null);
    }

    @Test(expected = FlowStoreServiceConnectorUnexpectedStatusCodeException.class)
    public void createHarvesterConfig_responseUnexpectedType_throws() throws FlowStoreServiceConnectorException {
        createHarvesterConfig_mockedHttpWithSpecifiedReturnErrorCode(
                Response.Status.BAD_REQUEST.getStatusCode(), EMPTY);
    }

    // Helper method
    private HarvesterConfig createHarvesterConfig_mockedHttpWithSpecifiedReturnErrorCode(int statusCode, Object returnValue) throws FlowStoreServiceConnectorException {
        final RRHarvesterConfig.Content configContent = new RRHarvesterConfig.Content();
        final PathBuilder path = new PathBuilder(FlowStoreServiceConstants.HARVESTER_CONFIGS_TYPE)
                .bind("type", RRHarvesterConfig.class.getName());
        when(HttpClient.doPostWithJson(CLIENT, configContent, FLOW_STORE_URL, path.build()))
                .thenReturn(new MockedResponse<>(statusCode, returnValue));

        final FlowStoreServiceConnector instance = newFlowStoreServiceConnector();
        return instance.createHarvesterConfig(configContent, RRHarvesterConfig.class);
    }
}
