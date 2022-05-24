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

import dk.dbc.dataio.gui.client.exceptions.ProxyException;
import dk.dbc.dataio.harvester.connector.TickleHarvesterServiceConnector;
import dk.dbc.dataio.harvester.task.connector.HarvesterTaskServiceConnectorException;
import dk.dbc.dataio.harvester.types.HarvestRecordsRequest;
import dk.dbc.dataio.harvester.types.TickleRepoHarvesterConfig;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.EnvironmentVariables;

import static dk.dbc.commons.testutil.Assert.assertThat;
import static dk.dbc.commons.testutil.Assert.isThrowing;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TickleHarvesterProxyImplTest {
    private static final String TICKLE_REPO_HARVESTER_URL = "http://dataio/tickle-repo-harvester";
    private final TickleHarvesterServiceConnector mockedTickleHarvesterServiceConnector = mock(TickleHarvesterServiceConnector.class);
    private final TickleRepoHarvesterConfig tickleRepoHarvesterConfig = new TickleRepoHarvesterConfig(1, 1, new TickleRepoHarvesterConfig.Content().withDatasetName("value"));

    @Rule
    public final EnvironmentVariables environmentVariables = new EnvironmentVariables();

    @Test(expected = NullPointerException.class)
    public void noArgs_tickleHarvesterProxyConstructorEndpointCanNotBeLookedUp_throws() {
        new TickleHarvesterProxyImpl();
    }

    @Test
    public void tickleHarvesterProxyConstructor_ok() {
        environmentVariables.set("TICKLE_REPO_HARVESTER_URL", TICKLE_REPO_HARVESTER_URL);

        // Subject under test
        TickleHarvesterProxyImpl tickleHarvesterProxy = new TickleHarvesterProxyImpl();

        // Verification
        assertThat(tickleHarvesterProxy, is(notNullValue()));
        assertThat(tickleHarvesterProxy.client, is(notNullValue()));
        assertThat(tickleHarvesterProxy.endpoint, is(notNullValue()));
        assertThat(tickleHarvesterProxy.tickleHarvesterServiceConnector, is(notNullValue()));
    }

    @Test
    public void createHarvestTask_success() throws HarvesterTaskServiceConnectorException {
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
    public void getDataSetSizeEstimate_failure() throws HarvesterTaskServiceConnectorException {
        final TickleHarvesterProxyImpl tickleHarvesterProxy = getTickleHarvesterProxyImpl();

        // Subject under test
        when(mockedTickleHarvesterServiceConnector.getDataSetSizeEstimate(anyString())).thenThrow(new HarvesterTaskServiceConnectorException("error"));

        // Verification
        assertThat(() -> tickleHarvesterProxy.getDataSetSizeEstimate(tickleRepoHarvesterConfig.getContent().getDatasetName()), isThrowing(ProxyException.class));
    }

    @Test
    public void getDataSetSizeEstimate_success() throws HarvesterTaskServiceConnectorException {
        final TickleHarvesterProxyImpl tickleHarvesterProxy = getTickleHarvesterProxyImpl();

        // Subject under test
        when(mockedTickleHarvesterServiceConnector.getDataSetSizeEstimate(anyString())).thenReturn(534);
        try {
            assertThat(tickleHarvesterProxy.getDataSetSizeEstimate(tickleRepoHarvesterConfig.getContent().getDatasetName()), is(534));
        } catch (ProxyException e) {
            fail("Unexpected exception in createHarvestTask");
        }
    }

    private TickleHarvesterProxyImpl getTickleHarvesterProxyImpl() {
        environmentVariables.set("TICKLE_REPO_HARVESTER_URL", TICKLE_REPO_HARVESTER_URL);
        TickleHarvesterProxyImpl tickleHarvesterProxy = new TickleHarvesterProxyImpl();
        tickleHarvesterProxy.tickleHarvesterServiceConnector = mockedTickleHarvesterServiceConnector;
        return tickleHarvesterProxy;
    }
}
