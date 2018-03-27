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

import dk.dbc.httpclient.HttpClient;
import dk.dbc.dataio.commons.utils.service.ServiceUtil;
import dk.dbc.dataio.gui.client.exceptions.ProxyError;
import dk.dbc.dataio.gui.client.exceptions.ProxyException;
import dk.dbc.dataio.ushsolrharvester.service.connector.UshSolrHarvesterServiceConnector;
import dk.dbc.dataio.ushsolrharvester.service.connector.UshSolrHarvesterServiceConnectorUnexpectedStatusCodeException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import javax.naming.NamingException;
import javax.ws.rs.client.Client;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.eq;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest({
        HttpClient.class,
        ServiceUtil.class
})
public class UshSolrHarvesterProxyImplTest {
    private final String ushSolrHarvesterServiceUrl;
    private final Client client = mock(Client.class);

    private static final long HARVEST_ID = 444;

    public UshSolrHarvesterProxyImplTest() {
        ushSolrHarvesterServiceUrl = "http://dataio/ush-solr-harvester";
    }

    @Before
    public void setup() throws Exception {
        mockStatic(ServiceUtil.class);
        mockStatic(HttpClient.class);
        when(ServiceUtil.getUshSolrHarvesterServiceEndpoint()).thenReturn(ushSolrHarvesterServiceUrl);
        when(HttpClient.newClient()).thenReturn(client);
    }

    @Test(expected = NamingException.class)
    public void noArgs_ushSolrHarvesterProxyConstructorUshSolrHarvesterService_EndpointCanNotBeLookedUp_throws() throws Exception {
        when(ServiceUtil.getUshSolrHarvesterServiceEndpoint()).thenThrow(new NamingException());
        new UshSolrHarvesterProxyImpl();
    }

    @Test(expected = NamingException.class)
    public void oneArg_ushSolrHarvesterProxyConstructorUshSolrHarvesterService_EndpointCanNotBeLookedUp_throws1() throws Exception {
        final UshSolrHarvesterServiceConnector ushSolrHarvesterServiceConnector = mock(UshSolrHarvesterServiceConnector.class);
        when(ServiceUtil.getUshSolrHarvesterServiceEndpoint()).thenThrow(new NamingException());
        new UshSolrHarvesterProxyImpl(ushSolrHarvesterServiceConnector);
    }

    /*
    * Test runTestHarvest
    */

    @Test
    public void runTestHarvest_remoteServiceReturnsHttpStatusOk_returnsLogAsString() throws Exception {
        final UshSolrHarvesterServiceConnector ushSolrHarvesterServiceConnector = mock(UshSolrHarvesterServiceConnector.class);
        final UshSolrHarvesterProxyImpl ushSolrHarvesterProxy = new UshSolrHarvesterProxyImpl(ushSolrHarvesterServiceConnector);
        String log = "\t something something \n";

        when(ushSolrHarvesterServiceConnector.runTestHarvest(eq(HARVEST_ID))).thenReturn(log);

        String actualLog = ushSolrHarvesterProxy.runTestHarvest(HARVEST_ID);
        assertThat(actualLog, is(log));
    }

    @Test
    public void runTestHarvest_remoteServiceReturnsHttpStatusInternalServerError_throws() throws Exception {
        runTestHarvest_genericTestImplForHttpErrors(500, ProxyError.INTERNAL_SERVER_ERROR, "INTERNAL_SERVER_ERROR");
    }

    @Test
    public void runTestHarvest_remoteServiceReturnsHttpStatusNotFound_throws() throws Exception {
        runTestHarvest_genericTestImplForHttpErrors(404, ProxyError.ENTITY_NOT_FOUND, "ENTITY_NOT_FOUND");
    }

    @Test
    public void runTestHarvest_remoteServiceReturnsHttpNoContent_throws() throws Exception {
        runTestHarvest_genericTestImplForHttpErrors(204, ProxyError.NO_CONTENT, "NO_CONTENT");
    }

    @Test
    public void runTestHarvest_zeroValuedJobId_throws() throws Exception{
        runTestHarvest_GenericTestImplForJobIdValidationErrors(0, ProxyError.BAD_REQUEST, "BAD_REQUEST");
    }

    private void runTestHarvest_genericTestImplForHttpErrors(int errorCodeToReturn, ProxyError expectedError, String expectedErrorName) throws Exception {
        final UshSolrHarvesterServiceConnector ushSolrHarvesterServiceConnector = mock(UshSolrHarvesterServiceConnector.class);
        final UshSolrHarvesterProxyImpl ushSolrHarvesterProxy = new UshSolrHarvesterProxyImpl(ushSolrHarvesterServiceConnector);
        when(ushSolrHarvesterServiceConnector.runTestHarvest(eq(HARVEST_ID))).thenThrow(new UshSolrHarvesterServiceConnectorUnexpectedStatusCodeException("DIED", errorCodeToReturn));

        try {
            ushSolrHarvesterProxy.runTestHarvest(HARVEST_ID);
            fail("No " + expectedErrorName + " error was thrown by runTestHarvest()");
        } catch (ProxyException e) {
            assertThat(e.getErrorCode(), is(expectedError));
        }
    }

    private void runTestHarvest_GenericTestImplForJobIdValidationErrors(long harvestId, ProxyError expectedError, String expectedErrorName) throws Exception {
        final UshSolrHarvesterServiceConnector ushSolrHarvesterServiceConnector = mock(UshSolrHarvesterServiceConnector.class);
        final UshSolrHarvesterProxyImpl ushSolrHarvesterProxy = new UshSolrHarvesterProxyImpl(ushSolrHarvesterServiceConnector);
        when(ushSolrHarvesterServiceConnector.runTestHarvest(eq(harvestId))).thenThrow(new IllegalArgumentException());

        try {
            ushSolrHarvesterProxy.runTestHarvest(harvestId);
            fail("No " + expectedErrorName + " error was thrown by runTestHarvest()");
        } catch(ProxyException e) {
            assertThat(e.getErrorCode(), is(expectedError));
        }
    }
}
