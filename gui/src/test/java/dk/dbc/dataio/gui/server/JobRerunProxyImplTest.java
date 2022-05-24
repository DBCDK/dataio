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
import dk.dbc.dataio.gui.client.model.JobModel;
import dk.dbc.dataio.gui.client.proxies.JobRerunProxy;
import dk.dbc.dataio.gui.server.jobrerun.JobRerunScheme;
import dk.dbc.dataio.jobstore.types.JobInfoSnapshot;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.EnvironmentVariables;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class JobRerunProxyImplTest {
    private static final String FLOWSTORE_URL = "http://dataio/flow-service";
    private final FlowStoreServiceConnector mockedFlowStoreServiceConnector = mock(FlowStoreServiceConnector.class);
    private final JobRerunSchemeParser mockedJobRerunSchemeParser = mock(JobRerunSchemeParser.class);

    @Rule
    public final EnvironmentVariables environmentVariables = new EnvironmentVariables();

    @Test(expected = NullPointerException.class)
    public void noArgs_jobRerunProxyConstructorEndpointCanNotBeLookedUp_throws() {
        new JobRerunProxyImpl();
    }

    @Test
    public void jobRerunProxyConstructor_ok() {
        environmentVariables.set("FLOWSTORE_URL", FLOWSTORE_URL);

        // Subject under test
        JobRerunProxyImpl jobRerunProxy = new JobRerunProxyImpl();

        // Verification
        assertThat(jobRerunProxy, is(notNullValue()));
        assertThat(jobRerunProxy.client, is(notNullValue()));
        assertThat(jobRerunProxy.endpoint, is(notNullValue()));
        assertThat(jobRerunProxy.jobRerunSchemeParser, is(notNullValue()));
    }

    @Test
    public void parse_success() throws FlowStoreServiceConnectorException {
        final JobRerunProxy jobRerunProxy = getJobRerunProxyImpl();

        // Subject under test
        when(mockedJobRerunSchemeParser.parse(any(JobInfoSnapshot.class)))
                .thenReturn(new JobRerunScheme());
        try {
            jobRerunProxy.parse(new JobModel()
                    .withSinkId(42)
                    .withSinkName("sinkName")
                    .withType(JobSpecification.Type.TEST));
        } catch (Exception e) {
            fail("Unexpected exception in parse");
        }
    }

    private JobRerunProxyImpl getJobRerunProxyImpl() {
        environmentVariables.set("FLOWSTORE_URL", FLOWSTORE_URL);
        JobRerunProxyImpl rerunProxy = new JobRerunProxyImpl();
        rerunProxy.flowStoreServiceConnector = mockedFlowStoreServiceConnector;
        rerunProxy.jobRerunSchemeParser = mockedJobRerunSchemeParser;
        return rerunProxy;
    }

}
