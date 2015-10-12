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

import dk.dbc.dataio.commons.types.PingResponse;
import dk.dbc.dataio.commons.types.SinkContent;
import dk.dbc.dataio.commons.types.rest.SinkServiceConstants;
import dk.dbc.dataio.commons.utils.httpclient.HttpClient;
import dk.dbc.dataio.commons.utils.service.ServiceUtil;
import dk.dbc.dataio.gui.client.exceptions.ProxyError;
import dk.dbc.dataio.gui.client.exceptions.ProxyException;
import dk.dbc.dataio.gui.client.model.PingResponseModel;
import dk.dbc.dataio.gui.client.model.SinkModel;
import dk.dbc.dataio.gui.client.modelBuilders.SinkModelBuilder;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import javax.naming.NamingException;
import javax.ws.rs.client.Client;
import javax.ws.rs.core.Response;
import java.util.Arrays;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest({
    HttpClient.class,
    ServiceUtil.class
})
public class SinkServiceProxyImplTest {
    private final String sinkServiceUrl = "http://dataio/sink-service";
    private final Client client = mock(Client.class);

    @Before
    public void setup() throws Exception {
        mockStatic(ServiceUtil.class);
        mockStatic(HttpClient.class);
        when(ServiceUtil.getSinkServiceEndpoint()).thenReturn(sinkServiceUrl);
        when(HttpClient.newClient()).thenReturn(client);
    }

    @Test(expected = NullPointerException.class)
    public void ping_sinkModelArgIsNull_throws() throws Exception {
        final SinkServiceProxyImpl sinkServiceProxy = new SinkServiceProxyImpl();
        sinkServiceProxy.ping(null);
    }

    @Test(expected = ProxyException.class)
    public void ping_sinkServiceEndpointCanNotBeLookedUp_throws() throws Exception {
        when(ServiceUtil.getSinkServiceEndpoint()).thenThrow(new NamingException());

        final SinkServiceProxyImpl sinkServiceProxy = new SinkServiceProxyImpl();
    }

    @Test(expected = ProxyException.class)
    public void ping_remoteServiceReturnsHttpStatusBadRequest_throws() throws Exception {
        when(HttpClient.doPostWithJson(any(Client.class), any(SinkContent.class), eq(sinkServiceUrl), eq(SinkServiceConstants.PING)))
                .thenReturn(new MockedHttpClientResponse<String>(Response.Status.BAD_REQUEST.getStatusCode(), ""));

        final SinkServiceProxyImpl sinkServiceProxy = new SinkServiceProxyImpl();
        try {
            sinkServiceProxy.ping(getValidSinkModel());
        } catch (ProxyException e) {
            assertThat(e.getErrorCode(), is(ProxyError.BAD_REQUEST));
            throw e;
        }
    }

    @Test(expected = ProxyException.class)
    public void ping_remoteServiceReturnsHttpStatusNotFound_throws() throws Exception {
        when(HttpClient.doPostWithJson(any(Client.class), any(SinkContent.class), eq(sinkServiceUrl), eq(SinkServiceConstants.PING)))
                .thenReturn(new MockedHttpClientResponse<String>(Response.Status.NOT_FOUND.getStatusCode(), ""));

        final SinkServiceProxyImpl sinkServiceProxy = new SinkServiceProxyImpl();
        try {
            sinkServiceProxy.ping(getValidSinkModel());
        } catch (ProxyException e) {
            assertThat(e.getErrorCode(), is(ProxyError.SERVICE_NOT_FOUND));
            throw e;
        }
    }

    @Test(expected = ProxyException.class)
    public void ping_remoteServiceReturnsHttpStatusInternalServerError_throws() throws Exception {
        when(HttpClient.doPostWithJson(any(Client.class), any(SinkContent.class), eq(sinkServiceUrl), eq(SinkServiceConstants.PING)))
                .thenReturn(new MockedHttpClientResponse<String>(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), ""));

        final SinkServiceProxyImpl sinkServiceProxy = new SinkServiceProxyImpl();
        try {
            sinkServiceProxy.ping(getValidSinkModel());
        } catch (ProxyException e) {
            assertThat(e.getErrorCode(), is(ProxyError.INTERNAL_SERVER_ERROR));
            throw e;
        }
    }

    @Test
    public void ping_remoteServiceReturnsHttpStatusOk_returnPingResponseEntity() throws Exception {
        final PingResponse expectedPingResponse = new PingResponse(PingResponse.Status.OK, Arrays.asList("message"));
        when(HttpClient.doPostWithJson(any(Client.class), any(SinkContent.class), eq(sinkServiceUrl), eq(SinkServiceConstants.PING)))
                .thenReturn(new MockedHttpClientResponse<PingResponse>(Response.Status.OK.getStatusCode(), expectedPingResponse));

        final SinkServiceProxyImpl sinkServiceProxy = new SinkServiceProxyImpl();
        final PingResponseModel model = sinkServiceProxy.ping(getValidSinkModel());
        assertThat(model, is(notNullValue()));
        assertThat(model.getStatus().toString().equals(expectedPingResponse.getStatus().toString()), is(true));
    }

    private SinkModel getValidSinkModel() {
        return new SinkModelBuilder().setId(0).setVersion(0).setResource("dataio/resource").build();
    }

}
