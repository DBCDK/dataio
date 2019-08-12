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

package dk.dbc.dataio.harvester.ticklerepo;

import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnector;
import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnectorException;
import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnectorUnexpectedStatusCodeException;
import dk.dbc.dataio.harvester.types.HarvesterException;
import dk.dbc.dataio.harvester.types.TickleRepoHarvesterConfig;
import dk.dbc.ticklerepo.dto.Batch;
import org.junit.Before;
import org.junit.Test;

import static dk.dbc.commons.testutil.Assert.assertThat;
import static dk.dbc.commons.testutil.Assert.isThrowing;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ConfigUpdaterTest {
    private final FlowStoreServiceConnector flowStoreServiceConnector = mock(FlowStoreServiceConnector.class);
    private final ConfigUpdater configUpdater = ConfigUpdater.create(flowStoreServiceConnector);
    private final Batch batch = new Batch().withId(42);

    private TickleRepoHarvesterConfig config = new TickleRepoHarvesterConfig(1, 1, new TickleRepoHarvesterConfig.Content());

    @Before
    public void setupMocks() throws FlowStoreServiceConnectorException {
        when(flowStoreServiceConnector.updateHarvesterConfig(config)).thenReturn(config);
        when(flowStoreServiceConnector.getHarvesterConfig(anyLong(), eq(TickleRepoHarvesterConfig.class))).thenReturn(config);
    }

    @Before
    public void resetConfig() {
        config = new TickleRepoHarvesterConfig(1, 1, new TickleRepoHarvesterConfig.Content());
    }

    @Test
    public void updatesLastBatchHarvestedInConfig() throws HarvesterException {
        configUpdater.updateHarvesterConfig(config, batch);
        assertThat(config.getContent().getLastBatchHarvested(), is(batch.getId()));
    }

    @Test
    public void flowStoreReturnsNonOkNonConflict_throws() throws FlowStoreServiceConnectorException {
        when(flowStoreServiceConnector.updateHarvesterConfig(config))
                .thenThrow(new FlowStoreServiceConnectorUnexpectedStatusCodeException("Internal error", 500));

        assertThat(() -> configUpdater.updateHarvesterConfig(config, batch), isThrowing(HarvesterException.class));

        verify(flowStoreServiceConnector, times(1)).updateHarvesterConfig(config);
        verify(flowStoreServiceConnector, times(0)).getHarvesterConfig(anyLong(), eq(TickleRepoHarvesterConfig.class));
    }

    @Test
    public void flowStoreReturnsConflict_refreshesConfigAndRetriesUpdate() throws FlowStoreServiceConnectorException, HarvesterException {
        when(flowStoreServiceConnector.updateHarvesterConfig(config))
                .thenThrow(new FlowStoreServiceConnectorUnexpectedStatusCodeException("Conflict", 409))
                .thenReturn(config);

        configUpdater.updateHarvesterConfig(config, batch);
        assertThat(config.getContent().getLastBatchHarvested(), is(batch.getId()));

        verify(flowStoreServiceConnector, times(2)).updateHarvesterConfig(config);
        verify(flowStoreServiceConnector, times(1)).getHarvesterConfig(anyLong(), eq(TickleRepoHarvesterConfig.class));
    }

    @Test
    public void flowStoreReturnsNonOkOnRefreshAfterConflict_throws() throws FlowStoreServiceConnectorException {
        when(flowStoreServiceConnector.updateHarvesterConfig(config))
                .thenThrow(new FlowStoreServiceConnectorUnexpectedStatusCodeException("Conflict", 409));
        when(flowStoreServiceConnector.getHarvesterConfig(anyLong(), eq(TickleRepoHarvesterConfig.class)))
                .thenThrow(new FlowStoreServiceConnectorException("Died"));

        assertThat(() -> configUpdater.updateHarvesterConfig(config, batch), isThrowing(HarvesterException.class));

        verify(flowStoreServiceConnector, times(1)).updateHarvesterConfig(config);
        verify(flowStoreServiceConnector, times(1)).getHarvesterConfig(anyLong(), eq(TickleRepoHarvesterConfig.class));
    }
}