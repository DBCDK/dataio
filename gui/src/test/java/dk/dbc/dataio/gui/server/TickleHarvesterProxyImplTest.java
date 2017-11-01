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

package dk.dbc.dataio.gui.server;

import dk.dbc.dataio.commons.utils.httpclient.HttpClient;
import dk.dbc.dataio.commons.utils.service.ServiceUtil;
import dk.dbc.dataio.gui.client.exceptions.ProxyException;
import dk.dbc.dataio.harvester.connector.TickleHarvesterServiceConnector;
import dk.dbc.dataio.harvester.task.connector.HarvesterTaskServiceConnectorException;
import dk.dbc.dataio.harvester.types.HarvestRecordsRequest;
import dk.dbc.dataio.harvester.types.TickleRepoHarvesterConfig;
import org.glassfish.jersey.client.ClientConfig;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import javax.naming.NamingException;
import javax.ws.rs.client.Client;

import static dk.dbc.commons.testutil.Assert.assertThat;
import static dk.dbc.commons.testutil.Assert.isThrowing;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest({
    HttpClient.class,
    ServiceUtil.class
})
public class TickleHarvesterProxyImplTest {
    private final Client client = mock(Client.class);
    private final TickleHarvesterServiceConnector mockedTickleHarvesterServiceConnector = mock(TickleHarvesterServiceConnector.class);
    private final TickleRepoHarvesterConfig tickleRepoHarvesterConfig = new TickleRepoHarvesterConfig(1, 1, new TickleRepoHarvesterConfig.Content().withDatasetName("value"));


    @Before
    public void setup() throws Exception {
        mockStatic(ServiceUtil.class);
        mockStatic(HttpClient.class);
        String tickleHarvesterUrl = "http://dataio/harvester/tickle/rs";
        when(ServiceUtil.getTickleHarvesterServiceEndpoint()).thenReturn(tickleHarvesterUrl);
        when(HttpClient.newClient(any(ClientConfig.class))).thenReturn(client);
    }

    @Test
    public void noArgs_tickleHarvesterProxyConstructorEndpointCanNotBeLookedUp_throws() throws Exception {
        when(ServiceUtil.getTickleHarvesterServiceEndpoint()).thenThrow(new NamingException());
        assertThat(() -> new TickleHarvesterProxyImpl(), isThrowing(NamingException.class));
    }

    @Test
    public void tickleHarvesterProxyConstructor_ok() throws Exception {
        // Subject under test
        TickleHarvesterProxyImpl tickleHarvesterProxy = new TickleHarvesterProxyImpl();

        // Verification
        assertThat(tickleHarvesterProxy, is(notNullValue()));
        assertThat(tickleHarvesterProxy.client, is(notNullValue()));
        assertThat(tickleHarvesterProxy.endpoint, is(notNullValue()));
        assertThat(tickleHarvesterProxy.tickleHarvesterServiceConnector, is(notNullValue()));
    }

    @Test
    public void createHarvestTask_failure() throws NamingException, ProxyException, HarvesterTaskServiceConnectorException {
        final TickleHarvesterProxyImpl tickleHarvesterProxy = getTickleHarvesterProxyImpl();

        // Subject under test
        when(mockedTickleHarvesterServiceConnector.createHarvestTask(anyLong(), any(HarvestRecordsRequest.class))).thenThrow(new HarvesterTaskServiceConnectorException("error"));

        // Verification
        assertThat(() -> tickleHarvesterProxy.createHarvestTask(tickleRepoHarvesterConfig), isThrowing(ProxyException.class));
    }

    @Test
    public void createHarvestTask_success() throws NamingException, HarvesterTaskServiceConnectorException {
        final TickleHarvesterProxyImpl tickleHarvesterProxy = getTickleHarvesterProxyImpl();

        // Subject under test
        when(mockedTickleHarvesterServiceConnector.createHarvestTask(anyLong(), any(HarvestRecordsRequest.class))).thenReturn("ok");
        try {
            tickleHarvesterProxy.createHarvestTask(tickleRepoHarvesterConfig);
        } catch (ProxyException e) {
            fail("Unexpected exception in createHarvestTask");
        }
    }

    @Test
    public void getDataSetSizeEstimate_failure() throws NamingException, ProxyException, HarvesterTaskServiceConnectorException {
        final TickleHarvesterProxyImpl tickleHarvesterProxy = getTickleHarvesterProxyImpl();

        // Subject under test
        when(mockedTickleHarvesterServiceConnector.getDataSetSizeEstimate(anyString())).thenThrow(new HarvesterTaskServiceConnectorException("error"));

        // Verification
        assertThat(() -> tickleHarvesterProxy.getDataSetSizeEstimate(tickleRepoHarvesterConfig.getContent().getDatasetName()), isThrowing(ProxyException.class));
    }

    @Test
    public void getDataSetSizeEstimate_success() throws NamingException, HarvesterTaskServiceConnectorException {
        final TickleHarvesterProxyImpl tickleHarvesterProxy = getTickleHarvesterProxyImpl();

        // Subject under test
        when(mockedTickleHarvesterServiceConnector.getDataSetSizeEstimate(anyString())).thenReturn(534);
        try {
            assertThat(tickleHarvesterProxy.getDataSetSizeEstimate(tickleRepoHarvesterConfig.getContent().getDatasetName()), is(534));
        } catch (ProxyException e) {
            fail("Unexpected exception in createHarvestTask");
        }
    }

    private TickleHarvesterProxyImpl getTickleHarvesterProxyImpl() throws NamingException {
        TickleHarvesterProxyImpl tickleHarvesterProxy = new TickleHarvesterProxyImpl();
        tickleHarvesterProxy.tickleHarvesterServiceConnector = mockedTickleHarvesterServiceConnector;
        return tickleHarvesterProxy;
    }
}
