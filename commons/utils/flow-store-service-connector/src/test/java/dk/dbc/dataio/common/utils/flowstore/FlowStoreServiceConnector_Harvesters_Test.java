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
import dk.dbc.dataio.commons.utils.httpclient.FailSafeHttpClient;
import dk.dbc.dataio.commons.utils.httpclient.HttpDelete;
import dk.dbc.dataio.commons.utils.httpclient.HttpGet;
import dk.dbc.dataio.commons.utils.httpclient.HttpPost;
import dk.dbc.dataio.commons.utils.httpclient.PathBuilder;
import dk.dbc.dataio.commons.utils.test.rest.MockedResponse;
import dk.dbc.dataio.harvester.types.HarvesterConfig;
import dk.dbc.dataio.harvester.types.RRHarvesterConfig;
import org.junit.Test;

import javax.ws.rs.core.Response;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static dk.dbc.commons.testutil.Assert.assertThat;
import static dk.dbc.commons.testutil.Assert.isThrowing;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class FlowStoreServiceConnector_Harvesters_Test {
    private static final String FLOW_STORE_URL = "http://dataio/flow-store";
    private final FailSafeHttpClient failSafeHttpClient = mock(FailSafeHttpClient.class);

    private final FlowStoreServiceConnector flowStoreServiceConnector =
            new FlowStoreServiceConnector(failSafeHttpClient, FLOW_STORE_URL);

    // ****************************************** create harvester config tests ******************************************

    @Test
    public void createHarvesterConfig_RRHarvesterConfigIsCreated_returnsHarvesterConfig() throws FlowStoreServiceConnectorException {
        final RRHarvesterConfig.Content configContent = new RRHarvesterConfig.Content();
        final HarvesterConfig expectedHarvesterConfig = new RRHarvesterConfig(42, 1, configContent);
        final HarvesterConfig harvesterConfig = createHarvesterConfig_mockedHttpWithSpecifiedReturnErrorCode(
                Response.Status.CREATED.getStatusCode(), expectedHarvesterConfig);
        assertThat(harvesterConfig, is(expectedHarvesterConfig));
        assertThat(harvesterConfig.getType(), is(RRHarvesterConfig.class.getName()));
    }

    @Test
    public void createHarvesterConfig_responseWithUnexpectedStatusCode_throws() throws FlowStoreServiceConnectorException {
        assertThat(() -> createHarvesterConfig_mockedHttpWithSpecifiedReturnErrorCode(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), ""),
                isThrowing(FlowStoreServiceConnectorUnexpectedStatusCodeException.class));
    }

    @Test
    public void createHarvesterConfig_responseWithNullEntity_throws() throws FlowStoreServiceConnectorException {
        assertThat(() -> createHarvesterConfig_mockedHttpWithSpecifiedReturnErrorCode(Response.Status.CREATED.getStatusCode(), null),
                isThrowing(FlowStoreServiceConnectorException.class));
    }

    private HarvesterConfig createHarvesterConfig_mockedHttpWithSpecifiedReturnErrorCode(int statusCode, Object returnValue) throws FlowStoreServiceConnectorException {
        final RRHarvesterConfig.Content configContent = new RRHarvesterConfig.Content();

        final PathBuilder path = new PathBuilder(FlowStoreServiceConstants.HARVESTER_CONFIGS_TYPE)
                .bind("type", RRHarvesterConfig.class.getName());

        final HttpPost httpPost = new HttpPost(failSafeHttpClient)
                .withBaseUrl(FLOW_STORE_URL)
                .withPathElements(path.build())
                .withJsonData(configContent);

        when(failSafeHttpClient.execute(httpPost))
                .thenReturn(new MockedResponse<>(statusCode, returnValue));

        return flowStoreServiceConnector.createHarvesterConfig(configContent, RRHarvesterConfig.class);
    }

    // ****************************************** update harvester config tests ******************************************

    @Test
    public void updateHarvesterConfig_harvesterConfigIsUpdated_returnsHarvesterConfig() throws FlowStoreServiceConnectorException {
        final RRHarvesterConfig harvesterConfig = new RRHarvesterConfig(1, 1, new RRHarvesterConfig.Content());
        HarvesterConfig updatedHarvesterConfig = updateHarvesterConfig_mockedHttpWithSpecifiedReturnErrorCode(
                Response.Status.OK.getStatusCode(), harvesterConfig);

        assertThat(updatedHarvesterConfig, is(harvesterConfig));
    }

    @Test
    public void updateHarvesterConfig_responseWithUnexpectedStatusCode_throws() throws FlowStoreServiceConnectorException {
        assertThat(() -> updateHarvesterConfig_mockedHttpWithSpecifiedReturnErrorCode(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), ""),
                isThrowing(FlowStoreServiceConnectorUnexpectedStatusCodeException.class));
    }

    private HarvesterConfig updateHarvesterConfig_mockedHttpWithSpecifiedReturnErrorCode(int statusCode, Object returnValue) throws FlowStoreServiceConnectorException {
        final RRHarvesterConfig.Content content = new RRHarvesterConfig.Content();
        final HarvesterConfig config = new RRHarvesterConfig(1, 1, content);

        final PathBuilder path = new PathBuilder(FlowStoreServiceConstants.HARVESTER_CONFIG)
                .bind(FlowStoreServiceConstants.ID_VARIABLE, Long.toString(config.getId()));

        final HttpPost httpPost = new HttpPost(failSafeHttpClient)
                .withBaseUrl(FLOW_STORE_URL)
                .withPathElements(path.build())
                .withHeader(FlowStoreServiceConstants.IF_MATCH_HEADER, Long.toString(config.getVersion()))
                .withHeader(FlowStoreServiceConstants.RESOURCE_TYPE_HEADER, config.getType())
                .withJsonData(config.getContent());

        when(failSafeHttpClient.execute(httpPost))
                .thenReturn(new MockedResponse<>(statusCode, returnValue));

        return flowStoreServiceConnector.updateHarvesterConfig(config);
    }

    // ************************************** find harvester configs by type tests ***************************************

    @Test
    public void findHarvesterConfigsByType_noHarvesterConfigsFound_returnsEmptyList() throws FlowStoreServiceConnectorException {
        assertThat(findHarvesterConfigsByType_mockedHttpWithSpecifiedReturnErrorCode(Response.Status.OK.getStatusCode(), Collections.emptyList()),
                is(Collections.emptyList()));
    }

    @Test
    public void findHarvesterConfigsByType_harvesterConfigsFound_returnsList() throws FlowStoreServiceConnectorException {
        final List<RRHarvesterConfig> expected = Arrays.asList(
                new RRHarvesterConfig(1, 1, new RRHarvesterConfig.Content()),
                new RRHarvesterConfig(2, 1, new RRHarvesterConfig.Content()));
        final List<RRHarvesterConfig> configs = findHarvesterConfigsByType_mockedHttpWithSpecifiedReturnErrorCode(Response.Status.OK.getStatusCode(), expected);
        assertThat(configs, is(expected));
    }

    @Test
    public void findHarvesterConfigsByType_responseWithUnexpectedStatusCode_throws() {
        assertThat(() -> findHarvesterConfigsByType_mockedHttpWithSpecifiedReturnErrorCode(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), ""),
                isThrowing(FlowStoreServiceConnectorUnexpectedStatusCodeException.class));
    }

    @Test
    public void findHarvesterConfigsByType_serviceReturnsNullEntity_throws() throws FlowStoreServiceConnectorException {
        assertThat(() -> findHarvesterConfigsByType_mockedHttpWithSpecifiedReturnErrorCode(Response.Status.OK.getStatusCode(), null),
                isThrowing(FlowStoreServiceConnectorException.class));
    }

    private List<RRHarvesterConfig> findHarvesterConfigsByType_mockedHttpWithSpecifiedReturnErrorCode(int statusCode, Object returnValue) throws FlowStoreServiceConnectorException {
        final PathBuilder path = new PathBuilder(FlowStoreServiceConstants.HARVESTER_CONFIGS_TYPE)
                .bind(FlowStoreServiceConstants.TYPE_VARIABLE, RRHarvesterConfig.class.getName());

        final HttpGet httpGet = new HttpGet(failSafeHttpClient)
                .withBaseUrl(FLOW_STORE_URL)
                .withPathElements(path.build());

        when(failSafeHttpClient.execute(httpGet))
                .thenReturn(new MockedResponse<>(statusCode, returnValue));

        return flowStoreServiceConnector.findHarvesterConfigsByType(RRHarvesterConfig.class);
    }

    // **************************************** get harvester config tests ***********************************************

    @Test
    public void getHarvesterConfig_responseWithUnexpectedStatusCode_throws() throws FlowStoreServiceConnectorException {
        assertThat(() -> getHarvesterConfig_mockedHttpWithSpecifiedReturnErrorCode(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), ""),
                isThrowing(FlowStoreServiceConnectorUnexpectedStatusCodeException.class));
    }

    @Test
    public void getHarvesterConfig_responseWithNullEntity_throws() throws FlowStoreServiceConnectorException {
        assertThat(() -> getHarvesterConfig_mockedHttpWithSpecifiedReturnErrorCode(Response.Status.OK.getStatusCode(), null),
                isThrowing(FlowStoreServiceConnectorException.class));
    }

    private RRHarvesterConfig getHarvesterConfig_mockedHttpWithSpecifiedReturnErrorCode(int statusCode, Object returnValue) throws FlowStoreServiceConnectorException {
        final PathBuilder path = new PathBuilder(FlowStoreServiceConstants.HARVESTER_CONFIG)
                .bind(FlowStoreServiceConstants.ID_VARIABLE, 1);

        final HttpGet httpGet = new HttpGet(failSafeHttpClient)
                .withBaseUrl(FLOW_STORE_URL)
                .withPathElements(path.build());

        when(failSafeHttpClient.execute(httpGet))
                .thenReturn(new MockedResponse<>(statusCode, returnValue));

        return flowStoreServiceConnector.getHarvesterConfig(1, RRHarvesterConfig.class);
    }

    // ************************************* delete harvester config tests ***********************************************

    @Test
    public void deleteHarvesterConfig_responseWithUnexpectedStatusCode_throws() throws FlowStoreServiceConnectorException {
        final PathBuilder path = new PathBuilder(FlowStoreServiceConstants.HARVESTER_CONFIG)
                .bind(FlowStoreServiceConstants.ID_VARIABLE, 1);

        final HttpDelete httpDelete = new HttpDelete(failSafeHttpClient)
                .withBaseUrl(FLOW_STORE_URL)
                .withPathElements(path.build())
                .withHeader(FlowStoreServiceConstants.IF_MATCH_HEADER, Long.toString(1));

        when(failSafeHttpClient.execute(httpDelete))
                .thenReturn(new MockedResponse<>(Response.Status.NOT_FOUND.getStatusCode(), null));

        assertThat(() -> flowStoreServiceConnector.deleteHarvesterConfig(1, 1),
                isThrowing(FlowStoreServiceConnectorUnexpectedStatusCodeException.class));
    }

    // ********************************* find enabled harvester configs by type tests ************************************

    @Test
    public void findEnabledHarvesterConfigsByType_harvesterConfigsFound_returnsList() throws FlowStoreServiceConnectorException {
        final List<RRHarvesterConfig> expected = Arrays.asList(
                new RRHarvesterConfig(1, 1, new RRHarvesterConfig.Content()),
                new RRHarvesterConfig(2, 1, new RRHarvesterConfig.Content()));
        assertThat(findEnabledHarvesterConfigsByType_mockedHttpWithSpecifiedReturnErrorCode(Response.Status.OK.getStatusCode(), expected),
                is(expected));
    }

    @Test
    public void findEnabledHarvesterConfigsByType_responseWithNullEntity_throws() {
        assertThat(() -> findEnabledHarvesterConfigsByType_mockedHttpWithSpecifiedReturnErrorCode(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), ""),
                isThrowing(FlowStoreServiceConnectorUnexpectedStatusCodeException.class));
    }

    @Test
    public void findEnabledHarvesterConfigsByType_serviceReturnsNullEntity_throws() throws FlowStoreServiceConnectorException {
        assertThat(() -> findEnabledHarvesterConfigsByType_mockedHttpWithSpecifiedReturnErrorCode(Response.Status.OK.getStatusCode(), null),
                isThrowing(FlowStoreServiceConnectorException.class));
    }

    private List<RRHarvesterConfig> findEnabledHarvesterConfigsByType_mockedHttpWithSpecifiedReturnErrorCode(int statusCode, Object returnValue) throws FlowStoreServiceConnectorException {
        final PathBuilder path = new PathBuilder(FlowStoreServiceConstants.HARVESTER_CONFIGS_TYPE_ENABLED)
                .bind(FlowStoreServiceConstants.TYPE_VARIABLE, RRHarvesterConfig.class.getName());

        final HttpGet httpGet = new HttpGet(failSafeHttpClient)
                .withBaseUrl(FLOW_STORE_URL)
                .withPathElements(path.build());

        when(failSafeHttpClient.execute(httpGet))
                .thenReturn(new MockedResponse<>(statusCode, returnValue));

        return flowStoreServiceConnector.findEnabledHarvesterConfigsByType(RRHarvesterConfig.class);
    }
}
