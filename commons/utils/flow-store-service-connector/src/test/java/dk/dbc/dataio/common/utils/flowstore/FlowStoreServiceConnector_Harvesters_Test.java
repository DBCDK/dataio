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
import dk.dbc.dataio.jsonb.JSONBException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static dk.dbc.commons.testutil.Assert.assertThat;
import static dk.dbc.commons.testutil.Assert.isThrowing;
import static dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnectorTestHelper.CLIENT;
import static dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnectorTestHelper.FLOW_STORE_URL;
import static dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnectorTestHelper.newFlowStoreServiceConnector;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.fail;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest({HttpClient.class})
public class FlowStoreServiceConnector_Harvesters_Test {
    private final Class rrHarvesterConfig = dk.dbc.dataio.harvester.types.RRHarvesterConfig.class;

    @Before
    public void setup() throws Exception {
        mockStatic(HttpClient.class);
    }

    // ****************************************** create harvester config tests ******************************************

    @Test
    public void createHarvesterConfig_harvesterConfigContentArgIsNull_throws() throws FlowStoreServiceConnectorException {
        final FlowStoreServiceConnector connector = newFlowStoreServiceConnector();
        assertThat(() -> connector.createHarvesterConfig(null, RRHarvesterConfig.class), isThrowing(NullPointerException.class));
    }

    @Test
    public void createHarvesterConfig_harvesterTypeArgIsNull_throws() throws FlowStoreServiceConnectorException {
        final FlowStoreServiceConnector connector = newFlowStoreServiceConnector();
        assertThat(() -> connector.createHarvesterConfig(new RRHarvesterConfig.Content(), null), isThrowing(NullPointerException.class));
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
        createHarvesterConfig_mockedHttpWithSpecifiedReturnErrorCode(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), "");
    }

    @Test(expected = FlowStoreServiceConnectorException.class)
    public void createHarvesterConfig_responseWithNullEntity_throws() throws FlowStoreServiceConnectorException {
        createHarvesterConfig_mockedHttpWithSpecifiedReturnErrorCode(Response.Status.CREATED.getStatusCode(), null);
    }

    @Test(expected = FlowStoreServiceConnectorUnexpectedStatusCodeException.class)
    public void createHarvesterConfig_responseUnexpectedType_throws() throws FlowStoreServiceConnectorException {
        createHarvesterConfig_mockedHttpWithSpecifiedReturnErrorCode(
                Response.Status.BAD_REQUEST.getStatusCode(), "");
    }

    // ****************************************** update harvester config tests ******************************************

    @Test
    public void updateHarvesterConfig_harvesterConfigIsUpdated_returnsHarvesterConfig() throws FlowStoreServiceConnectorException, JSONBException {

        final RRHarvesterConfig harvesterConfig = new RRHarvesterConfig(1, 1, new RRHarvesterConfig.Content());
        HarvesterConfig updatedHarvesterConfig = updateHarvesterConfig_mockedHttpWithSpecifiedReturnErrorCode(
                Response.Status.OK.getStatusCode(), harvesterConfig);

        assertThat(updatedHarvesterConfig, is(harvesterConfig));
    }

    @Test
    public void updateHarvesterConfig_responseWithUnexpectedStatusCode_throws() throws FlowStoreServiceConnectorException, JSONBException {
        try {
            updateHarvesterConfig_mockedHttpWithSpecifiedReturnErrorCode(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), "");
            fail("Exception not thrown");
        } catch (FlowStoreServiceConnectorException ignored) { }
    }

    @Test
    public void updateHarvesterConfig_responseWithMultipleUpdatesConflict_throws() throws FlowStoreServiceConnectorException, JSONBException {
        try {
            updateHarvesterConfig_mockedHttpWithSpecifiedReturnErrorCode(Response.Status.CONFLICT.getStatusCode(), "");
            fail("Exception not thrown");
        } catch (FlowStoreServiceConnectorException ignored) { }
    }

    @Test
    public void updateHarvesterConfig_responseWithHarvesterConfigIdNotFound_throws() throws FlowStoreServiceConnectorException, JSONBException {
        try {
            updateHarvesterConfig_mockedHttpWithSpecifiedReturnErrorCode(Response.Status.NOT_FOUND.getStatusCode(), "");
            fail("Exception not thrown");
        } catch (FlowStoreServiceConnectorException ignored) { }
    }

    // ************************************** find harvester configs by type tests ***************************************

    @Test
    public void findHarvesterConfigsByType_typeArgIsNull_throws() throws FlowStoreServiceConnectorException {
        final FlowStoreServiceConnector connector = newFlowStoreServiceConnector();
        assertThat(() -> connector.findHarvesterConfigsByType(null), isThrowing(NullPointerException.class));
    }

    @Test
    public void findHarvesterConfigsByType_noHarvesterConfigsFound_returnsEmptyList() throws FlowStoreServiceConnectorException {
        when(HttpClient.doGet(CLIENT, FLOW_STORE_URL, getfindHarvesterConfigsByTypePath(rrHarvesterConfig)))
                .thenReturn(new MockedResponse<>(Response.Status.OK.getStatusCode(), new ArrayList<RRHarvesterConfig>()));

        final FlowStoreServiceConnector connector = newFlowStoreServiceConnector();
        assertThat(connector.findHarvesterConfigsByType(rrHarvesterConfig).isEmpty(), is(true));
    }

    @Test
    public void findHarvesterConfigsByType_harvesterConfigsFound_returnsList() throws FlowStoreServiceConnectorException {
        final List<RRHarvesterConfig> configs = Arrays.asList(
                new RRHarvesterConfig(1, 1, new RRHarvesterConfig.Content()),
                new RRHarvesterConfig(2, 1, new RRHarvesterConfig.Content()));
        when(HttpClient.doGet(CLIENT, FLOW_STORE_URL, getfindHarvesterConfigsByTypePath(rrHarvesterConfig)))
                .thenReturn(new MockedResponse<>(Response.Status.OK.getStatusCode(), configs));

        final FlowStoreServiceConnector connector = newFlowStoreServiceConnector();
        assertThat(connector.findHarvesterConfigsByType(rrHarvesterConfig), is(configs));
    }

    @Test
    public void findHarvesterConfigsByType_harvesterConfigsFound_throws() {
        when(HttpClient.doGet(CLIENT, FLOW_STORE_URL, getfindHarvesterConfigsByTypePath(rrHarvesterConfig)))
                .thenReturn(new MockedResponse<>(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), ""));

        final FlowStoreServiceConnector connector = newFlowStoreServiceConnector();
        assertThat(() -> connector.findHarvesterConfigsByType(rrHarvesterConfig), isThrowing(FlowStoreServiceConnectorUnexpectedStatusCodeException.class));
    }

    @Test
    public void findHarvesterConfigsByType_serviceReturnsNullEntity_throws() throws FlowStoreServiceConnectorException {
        when(HttpClient.doGet(CLIENT, FLOW_STORE_URL, getfindHarvesterConfigsByTypePath(rrHarvesterConfig)))
                .thenReturn(new MockedResponse<>(Response.Status.OK.getStatusCode(), null));

        final FlowStoreServiceConnector connector = newFlowStoreServiceConnector();
        assertThat(() -> connector.findHarvesterConfigsByType(rrHarvesterConfig), isThrowing(FlowStoreServiceConnectorException.class));
    }

    @Test
    public void findEnabledHarvesterConfigsByType_typeArgIsNull_throws() throws FlowStoreServiceConnectorException {
        final FlowStoreServiceConnector connector = newFlowStoreServiceConnector();
        assertThat(() -> connector.findEnabledHarvesterConfigsByType(null), isThrowing(NullPointerException.class));
    }

    @Test
    public void findEnabledHarvesterConfigsByType_noHarvesterConfigsFound_returnsEmptyList() throws FlowStoreServiceConnectorException {
        when(HttpClient.doGet(CLIENT, FLOW_STORE_URL, getfindEnabledHarvesterConfigsByTypePath(rrHarvesterConfig)))
                .thenReturn(new MockedResponse<>(Response.Status.OK.getStatusCode(), new ArrayList<RRHarvesterConfig>()));

        final FlowStoreServiceConnector connector = newFlowStoreServiceConnector();
        assertThat(connector.findEnabledHarvesterConfigsByType(rrHarvesterConfig).isEmpty(), is(true));
    }

    // **************************************** get harvester config tests ***********************************************

    @Test
    public void getHarvesterConfig_typeArgIsNull_throws() throws FlowStoreServiceConnectorException {
        final FlowStoreServiceConnector connector = newFlowStoreServiceConnector();
        assertThat(() -> connector.getHarvesterConfig(42L, null), isThrowing(NullPointerException.class));
    }

    @Test
    public void getHarvesterConfig_responseWithUnexpectedStatusCode_throws() throws FlowStoreServiceConnectorException {
        when(HttpClient.doGet(CLIENT, FLOW_STORE_URL, pathForGetHarvesterConfig(42L)))
                .thenReturn(new MockedResponse<>(Response.Status.NOT_FOUND.getStatusCode(), null));

        final FlowStoreServiceConnector connector = newFlowStoreServiceConnector();
        assertThat(() -> connector.getHarvesterConfig(42L, RRHarvesterConfig.class), isThrowing(FlowStoreServiceConnectorUnexpectedStatusCodeException.class));
    }

    @Test
    public void getHarvesterConfig_responseWithNullEntity_throws() throws FlowStoreServiceConnectorException {
        when(HttpClient.doGet(CLIENT, FLOW_STORE_URL, pathForGetHarvesterConfig(42L)))
                .thenReturn(new MockedResponse<>(Response.Status.OK.getStatusCode(), null));

        final FlowStoreServiceConnector connector = newFlowStoreServiceConnector();
        assertThat(() -> connector.getHarvesterConfig(42L, RRHarvesterConfig.class), isThrowing(FlowStoreServiceConnectorException.class));
    }

    // ********************************* find enabled harvester configs by type tests ************************************

    @Test
    public void findEnabledHarvesterConfigsByType_harvesterConfigsFound_returnsList() throws FlowStoreServiceConnectorException {
        final List<RRHarvesterConfig> configs = Arrays.asList(
                new RRHarvesterConfig(1, 1, new RRHarvesterConfig.Content()),
                new RRHarvesterConfig(2, 1, new RRHarvesterConfig.Content()));
        when(HttpClient.doGet(CLIENT, FLOW_STORE_URL, getfindEnabledHarvesterConfigsByTypePath(rrHarvesterConfig)))
                .thenReturn(new MockedResponse<>(Response.Status.OK.getStatusCode(), configs));

        final FlowStoreServiceConnector connector = newFlowStoreServiceConnector();
        assertThat(connector.findEnabledHarvesterConfigsByType(rrHarvesterConfig), is(configs));
    }

    @Test
    public void findEnabledHarvesterConfigsByType_harvesterConfigsFound_throws() {
        when(HttpClient.doGet(CLIENT, FLOW_STORE_URL, getfindEnabledHarvesterConfigsByTypePath(rrHarvesterConfig)))
                .thenReturn(new MockedResponse<>(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), ""));

        final FlowStoreServiceConnector connector = newFlowStoreServiceConnector();
        assertThat(() -> connector.findEnabledHarvesterConfigsByType(rrHarvesterConfig), isThrowing(FlowStoreServiceConnectorUnexpectedStatusCodeException.class));
    }

    @Test
    public void findEnabledHarvesterConfigsByType_serviceReturnsNullEntity_throws() throws FlowStoreServiceConnectorException {
        when(HttpClient.doGet(CLIENT, FLOW_STORE_URL, getfindEnabledHarvesterConfigsByTypePath(rrHarvesterConfig)))
                .thenReturn(new MockedResponse<>(Response.Status.OK.getStatusCode(), null));

        final FlowStoreServiceConnector connector = newFlowStoreServiceConnector();
        assertThat(() -> connector.findEnabledHarvesterConfigsByType(rrHarvesterConfig), isThrowing(FlowStoreServiceConnectorException.class));
    }

    // *******************************************************************************************************************

    private HarvesterConfig createHarvesterConfig_mockedHttpWithSpecifiedReturnErrorCode(int statusCode, Object returnValue) throws FlowStoreServiceConnectorException {
        final RRHarvesterConfig.Content configContent = new RRHarvesterConfig.Content();
        final PathBuilder path = new PathBuilder(FlowStoreServiceConstants.HARVESTER_CONFIGS_TYPE)
                .bind("type", RRHarvesterConfig.class.getName());
        Mockito.when(HttpClient.doPostWithJson(CLIENT, configContent, FLOW_STORE_URL, path.build()))
                .thenReturn(new MockedResponse<>(statusCode, returnValue));

        final FlowStoreServiceConnector instance = newFlowStoreServiceConnector();
        return instance.createHarvesterConfig(configContent, RRHarvesterConfig.class);
    }

    private HarvesterConfig updateHarvesterConfig_mockedHttpWithSpecifiedReturnErrorCode(int statusCode, Object returnValue) throws FlowStoreServiceConnectorException, JSONBException {
        final RRHarvesterConfig.Content content = new RRHarvesterConfig.Content();
        final HarvesterConfig config = new RRHarvesterConfig(1, 1, content);

        final Map<String, String> headers = new HashMap<>(2);
        headers.put(FlowStoreServiceConstants.IF_MATCH_HEADER, Long.toString(config.getVersion()));
        headers.put(FlowStoreServiceConstants.RESOURCE_TYPE_HEADER, config.getType());

        final PathBuilder path = new PathBuilder(FlowStoreServiceConstants.HARVESTER_CONFIG)
                .bind(FlowStoreServiceConstants.ID_VARIABLE, Long.toString(config.getId()));
        when(HttpClient.doPostWithJson(CLIENT, headers, content, FLOW_STORE_URL, path.build()))
                .thenReturn(new MockedResponse<>(statusCode, returnValue));

        final FlowStoreServiceConnector instance = newFlowStoreServiceConnector();
        return instance.updateHarvesterConfig(config);
    }

    private String[] getfindHarvesterConfigsByTypePath(Class type) {
        return new PathBuilder(FlowStoreServiceConstants.HARVESTER_CONFIGS_TYPE)
                .bind(FlowStoreServiceConstants.TYPE_VARIABLE, type.getName())
                .build();
    }

    private String[] getfindEnabledHarvesterConfigsByTypePath(Class type) {
        return new PathBuilder(FlowStoreServiceConstants.HARVESTER_CONFIGS_TYPE_ENABLED)
                .bind(FlowStoreServiceConstants.TYPE_VARIABLE, type.getName())
                .build();
    }

    private String[] pathForGetHarvesterConfig(long id) {
        return new PathBuilder(FlowStoreServiceConstants.HARVESTER_CONFIG)
                .bind(FlowStoreServiceConstants.ID_VARIABLE, id)
                .build();
    }
}
