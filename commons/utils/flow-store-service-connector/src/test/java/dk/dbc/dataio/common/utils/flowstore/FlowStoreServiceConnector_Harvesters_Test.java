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

import dk.dbc.dataio.commons.types.JobSpecification;
import dk.dbc.dataio.commons.types.rest.FlowStoreServiceConstants;
import dk.dbc.dataio.commons.utils.httpclient.HttpClient;
import dk.dbc.dataio.commons.utils.httpclient.PathBuilder;
import dk.dbc.dataio.commons.utils.test.rest.MockedResponse;
import dk.dbc.dataio.harvester.types.HarvesterConfig;
import dk.dbc.dataio.harvester.types.RRHarvesterConfig;
import dk.dbc.dataio.harvester.types.RawRepoHarvesterConfig;
import dk.dbc.dataio.jsonb.JSONBContext;
import dk.dbc.dataio.jsonb.JSONBException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import javax.ws.rs.core.Response;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static dk.dbc.commons.testutil.Assert.assertThat;
import static dk.dbc.commons.testutil.Assert.isThrowing;
import static dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnectorTestHelper.CLIENT;
import static dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnectorTestHelper.FLOW_STORE_URL;
import static dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnectorTestHelper.newFlowStoreServiceConnector;
import static org.hamcrest.CoreMatchers.notNullValue;
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

        Assert.assertThat(harvesterConfig, is(expectedHarvesterConfig));
        Assert.assertThat(harvesterConfig.getType(), is(RRHarvesterConfig.class.getName()));
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

    @Test(expected = FlowStoreServiceConnectorException.class)
    public void getHarvesterRrConfigs_responseWithUnexpectedStatusCode_throws() throws FlowStoreServiceConnectorException, JSONBException {
        getHarvesterRrConfigs_mockedHttpWithSpecifiedReturnErrorCode(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), "");
    }

    @Test(expected = FlowStoreServiceConnectorException.class)
    public void getHarvesterRrConfigs_nullJjson_throws() throws FlowStoreServiceConnectorException, JSONBException {
        getHarvesterRrConfigs_mockedHttpWithSpecifiedReturnErrorCode(Response.Status.OK.getStatusCode(), null);
    }

    @Test
    public void getHarvesterRrConfigs_noEntries_emptyListReturned() throws FlowStoreServiceConnectorException, JSONBException {
        RawRepoHarvesterConfig resultList = getHarvesterRrConfigs_mockedHttpWithSpecifiedReturnErrorCode(Response.Status.OK.getStatusCode(), new RawRepoHarvesterConfig());
        assertThat(resultList, is(notNullValue()));
        assertThat(resultList.getEntries().size(), is(0));
    }

    @Test(expected = FlowStoreServiceConnectorUnexpectedStatusCodeException.class)
    public void getHarvesterRrConfigs_responseWithNotFound_throws() throws FlowStoreServiceConnectorException, JSONBException {
        getHarvesterRrConfigs_mockedHttpWithSpecifiedReturnErrorCode(Response.Status.NOT_FOUND.getStatusCode(), null);
    }

    @Test
    public void getHarvesterRrConfigs_realJsonConfigsRetrieved_returnsCorrectConfigs() throws IOException, FlowStoreServiceConnectorException, JSONBException {
        // Prepare test
        final String relativePath = "src/test/java/dk/dbc/dataio/common/utils/flowstore/FlowStoreServiceConnector_Harvesters_TestData.json";
        String jsonConfig = new String(Files.readAllBytes(Paths.get(relativePath)));
        final RawRepoHarvesterConfig testConfig = new JSONBContext().unmarshall(jsonConfig, RawRepoHarvesterConfig.class);

        // Test
        final RawRepoHarvesterConfig resultRRConfig = getHarvesterRrConfigs_mockedHttpWithSpecifiedReturnErrorCode(Response.Status.OK.getStatusCode(), testConfig);

        // Verify Test
        Set<RawRepoHarvesterConfig.Entry> entries = resultRRConfig.getEntries();
        assertThat(entries.size(), is(10));

        for (RawRepoHarvesterConfig.Entry entry: entries) {
            switch (entry.getId()) {
                case "broend-sync":
                    assertThat(entry.getFormat(870970), is("basis"));
                    assertThat(entry.getResource(), is("jdbc/dataio/rawrepo"));
                    assertThat(entry.getBatchSize(), is(10000));
                    assertThat(entry.includeRelations(), is(true));
                    assertThat(entry.getFormat(), is("katalog"));
                    assertThat(entry.getOpenAgencyTarget().getUrl(), is("http://openagency.addi.dk/2.20/"));
                    assertThat(entry.getDestination(), is("testbroend-i01"));
                    assertThat(entry.getConsumerId(), is("broend-sync"));
                    assertThat(entry.getType(), is(JobSpecification.Type.TEST));
                    break;
                case "broend-sync-loadtest":
                    assertThat(entry.getFormat(870970), is("basis"));
                    assertThat(entry.getResource(), is("jdbc/dataio/rawrepo-loadtest"));
                    assertThat(entry.getBatchSize(), is(10000));
                    assertThat(entry.includeRelations(), is(true));
                    assertThat(entry.getFormat(), is("katalog"));
                    assertThat(entry.getOpenAgencyTarget().getUrl(), is("http://openagency.addi.dk/2.20/"));
                    assertThat(entry.getDestination(), is("broend3-loadtest"));
                    assertThat(entry.getConsumerId(), is("broend-sync"));
                    assertThat(entry.getType(), is(JobSpecification.Type.TEST));
                    break;
                case "basis-decentral":
                    assertThat(entry.getFormat(870970), is("basis"));
                    assertThat(entry.getResource(), is("jdbc/dataio/rawrepo"));
                    assertThat(entry.getBatchSize(), is(10000));
                    assertThat(entry.includeRelations(), is(true));
                    assertThat(entry.getFormat(), is("basis"));
                    assertThat(entry.getOpenAgencyTarget().getUrl(), is("http://openagency.addi.dk/2.20/"));
                    assertThat(entry.getDestination(), is("testbasis-i01"));
                    assertThat(entry.getConsumerId(), is("basis-decentral"));
                    assertThat(entry.getType(), is(JobSpecification.Type.TEST));
                    break;
                case "testbasis-sync-boble":
                    assertThat(entry.getFormat(870970), is("basis"));
                    assertThat(entry.getResource(), is("jdbc/dataio/rawrepo-boblebad"));
                    assertThat(entry.getBatchSize(), is(10000));
                    assertThat(entry.includeRelations(), is(true));
                    assertThat(entry.getFormat(), is("basis"));
                    assertThat(entry.getOpenAgencyTarget().getUrl(), is("http://openagency.addi.dk/2.20/"));
                    assertThat(entry.getDestination(), is("testbasis-boblebad"));
                    assertThat(entry.getConsumerId(), is("basis-decentral"));
                    assertThat(entry.getType(), is(JobSpecification.Type.TEST));
                    break;
                case "broend-sync-boble":
                    assertThat(entry.getFormat(870970), is("basis"));
                    assertThat(entry.getResource(), is("jdbc/dataio/rawrepo-boblebad"));
                    assertThat(entry.getBatchSize(), is(10000));
                    assertThat(entry.includeRelations(), is(true));
                    assertThat(entry.getFormat(), is("katalog"));
                    assertThat(entry.getOpenAgencyTarget().getUrl(), is("http://openagency.addi.dk/2.20/"));
                    assertThat(entry.getDestination(), is("broend-boblebad"));
                    assertThat(entry.getConsumerId(), is("broend-sync"));
                    break;
                case "fbs-sync":
                    assertThat(entry.getResource(), is("jdbc/dataio/rawrepo"));
                    assertThat(entry.getBatchSize(), is(10000));
                    assertThat(entry.includeRelations(), is(false));
                    assertThat(entry.getFormat(), is("katalog"));
                    assertThat(entry.getDestination(), is("fbs-i01"));
                    assertThat(entry.getConsumerId(), is("fbs-sync"));
                    assertThat(entry.getType(), is(JobSpecification.Type.TEST));
                    assertThat(entry.getOpenAgencyTarget().getUrl(), is("http://openagency.addi.dk/2.20/"));
                    break;
                case "cicero-sync":
                    assertThat(entry.getResource(), is("jdbc/dataio/rawrepo-boblebad"));
                    assertThat(entry.getBatchSize(), is(10000));
                    assertThat(entry.includeRelations(), is(false));
                    assertThat(entry.getFormat(), is("katalog"));
                    assertThat(entry.getDestination(), is("cicero-boblebad"));
                    assertThat(entry.getConsumerId(), is("fbs-sync"));
                    assertThat(entry.getOpenAgencyTarget().getUrl(), is("http://openagency.addi.dk/2.20/"));
                    break;
                case "broend-sync-cisterne":
                    assertThat(entry.getFormat(870970), is("basis"));
                    assertThat(entry.getResource(), is("jdbc/dataio/rawrepo-cisterne"));
                    assertThat(entry.getBatchSize(), is(10000));
                    assertThat(entry.includeRelations(), is(true));
                    assertThat(entry.getFormat(), is("katalog"));
                    assertThat(entry.getOpenAgencyTarget().getUrl(), is("http://openagency.addi.dk/2.20/"));
                    assertThat(entry.getDestination(), is("broend-cisterne"));
                    assertThat(entry.getConsumerId(), is("broend-sync"));
                    break;
                case "fbs-sync-cisterne":
                    assertThat(entry.getResource(), is("jdbc/dataio/rawrepo-cisterne"));
                    assertThat(entry.getBatchSize(), is(10000));
                    assertThat(entry.includeRelations(), is(false));
                    assertThat(entry.getFormat(), is("katalog"));
                    assertThat(entry.getDestination(), is("cicero-cisterne"));
                    assertThat(entry.getConsumerId(), is("broend-sync-forkert"));
                    assertThat(entry.getOpenAgencyTarget().getUrl(), is("http://openagency.addi.dk/2.20/"));
                    break;
                case "basis-sync-cisterne":
                    assertThat(entry.getFormat(870970), is("basis"));
                    assertThat(entry.getResource(), is("jdbc/dataio/rawrepo-cisterne"));
                    assertThat(entry.getBatchSize(), is(10000));
                    assertThat(entry.includeRelations(), is(true));
                    assertThat(entry.getFormat(), is("basis"));
                    assertThat(entry.getOpenAgencyTarget().getUrl(), is("http://openagency.addi.dk/2.20/"));
                    assertThat(entry.getDestination(), is("basis-cisterne"));
                    assertThat(entry.getConsumerId(), is("basis-decentral"));
                    break;
                default:
                    fail("Test data contains unknown data");
                    break;
            }
        }
    }

    /*
     * Private methods
     */
    
    private HarvesterConfig createHarvesterConfig_mockedHttpWithSpecifiedReturnErrorCode(int statusCode, Object returnValue) throws FlowStoreServiceConnectorException {
        final RRHarvesterConfig.Content configContent = new RRHarvesterConfig.Content();
        final PathBuilder path = new PathBuilder(FlowStoreServiceConstants.HARVESTER_CONFIGS_TYPE)
                .bind("type", RRHarvesterConfig.class.getName());
        Mockito.when(HttpClient.doPostWithJson(CLIENT, configContent, FLOW_STORE_URL, path.build()))
                .thenReturn(new MockedResponse<>(statusCode, returnValue));

        final FlowStoreServiceConnector instance = newFlowStoreServiceConnector();
        return instance.createHarvesterConfig(configContent, RRHarvesterConfig.class);
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

    private RawRepoHarvesterConfig getHarvesterRrConfigs_mockedHttpWithSpecifiedReturnErrorCode(int statusCode, Object returnValue) throws FlowStoreServiceConnectorException, JSONBException {
        when(HttpClient.doGet(CLIENT, FLOW_STORE_URL, FlowStoreServiceConstants.HARVESTERS_RR_CONFIG)).thenReturn(new MockedResponse<>(statusCode, returnValue));
        final FlowStoreServiceConnector instance = newFlowStoreServiceConnector();
        return instance.getHarvesterRrConfigs();
    }


}
