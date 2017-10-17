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

import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnector;
import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnectorException;
import dk.dbc.dataio.commons.types.JobSpecification;
import dk.dbc.dataio.commons.utils.httpclient.HttpClient;
import dk.dbc.dataio.commons.utils.service.ServiceUtil;
import dk.dbc.dataio.gui.client.model.JobModel;
import dk.dbc.dataio.gui.client.proxies.JobRerunProxy;
import dk.dbc.dataio.gui.server.jobrerun.JobRerunScheme;
import dk.dbc.dataio.jobstore.types.JobInfoSnapshot;
import org.glassfish.jersey.client.ClientConfig;
import org.junit.Assert;
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
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest({
        HttpClient.class,
        ServiceUtil.class
})
public class JobRerunProxyImplTest {
    private final Client client = mock(Client.class);
    private final FlowStoreServiceConnector mockedFlowStoreServiceConnector = mock(FlowStoreServiceConnector.class);
    private final JobRerunSchemeParser mockedJobRerunSchemeParser = mock(JobRerunSchemeParser.class);


    @Before
    public void setup() throws Exception {
        mockStatic(ServiceUtil.class);
        mockStatic(HttpClient.class);
        when(HttpClient.newClient(any(ClientConfig.class))).thenReturn(client);
        when(ServiceUtil.getFlowStoreServiceEndpoint()).thenReturn("http://dataio/flow-service");
    }

    @Test
    public void noArgs_jobRerunProxyConstructorEndpointCanNotBeLookedUp_throws() throws Exception {
        when(ServiceUtil.getFlowStoreServiceEndpoint()).thenThrow(new NamingException());
        assertThat(() -> new JobRerunProxyImpl(), isThrowing(NamingException.class));
    }

    @Test
    public void jobRerunProxyConstructor_ok() throws Exception {
        // Subject under test
        JobRerunProxyImpl jobRerunProxy = new JobRerunProxyImpl();

        // Verification
        Assert.assertThat(jobRerunProxy, is(notNullValue()));
        Assert.assertThat(jobRerunProxy.client, is(notNullValue()));
        Assert.assertThat(jobRerunProxy.endpoint, is(notNullValue()));
        Assert.assertThat(jobRerunProxy.jobRerunSchemeParser, is(notNullValue()));
    }

    @Test
    public void parse_success() throws FlowStoreServiceConnectorException, NamingException {
        final JobRerunProxy jobRerunProxy = getJobRerunProxyImpl();

        // Subject under test
        when(mockedJobRerunSchemeParser.parse(any(JobInfoSnapshot.class))).thenReturn(new JobRerunScheme());
        try {
            jobRerunProxy.parse(new JobModel().withSinkId(42).withSinkName("sinkName").withType(JobSpecification.Type.TEST));
        } catch (Exception e) {
            fail("Unexpected exception in parse");
        }
    }

    private JobRerunProxyImpl getJobRerunProxyImpl() throws NamingException {
        JobRerunProxyImpl rerunProxy = new JobRerunProxyImpl();
        rerunProxy.flowStoreServiceConnector = mockedFlowStoreServiceConnector;
        rerunProxy.jobRerunSchemeParser = mockedJobRerunSchemeParser;
        return rerunProxy;
    }

}
