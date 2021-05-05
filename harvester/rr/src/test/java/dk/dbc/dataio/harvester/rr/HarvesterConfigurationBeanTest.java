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

package dk.dbc.dataio.harvester.rr;

import dk.dbc.commons.jsonb.JSONBException;
import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnector;
import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnectorException;
import dk.dbc.dataio.common.utils.flowstore.ejb.FlowStoreServiceConnectorBean;
import dk.dbc.dataio.harvester.types.HarvesterException;
import dk.dbc.dataio.harvester.types.RRHarvesterConfig;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static dk.dbc.commons.testutil.Assert.assertThat;
import static dk.dbc.commons.testutil.Assert.isThrowing;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class HarvesterConfigurationBeanTest {
    private final FlowStoreServiceConnectorBean flowStoreServiceConnectorBean = mock(FlowStoreServiceConnectorBean.class);
    private final FlowStoreServiceConnector flowStoreServiceConnector = mock(FlowStoreServiceConnector.class);
    private final Class rrHarvesterConfigurationType = RRHarvesterConfig.class;

    @Before
    public void setupMocks() {
        when(flowStoreServiceConnectorBean.getConnector()).thenReturn(flowStoreServiceConnector);
    }

    @Test
    public void reload_flowStoreLookupThrows_throws() throws FlowStoreServiceConnectorException, HarvesterException {
        when(flowStoreServiceConnector.findEnabledHarvesterConfigsByType(rrHarvesterConfigurationType))
                .thenThrow(new FlowStoreServiceConnectorException("Died"));

        final HarvesterConfigurationBean bean = newHarvesterConfigurationBean();
        assertThat(bean::reload, isThrowing(HarvesterException.class));
    }

    @Test
    public void reload_flowStoreLookupReturns_setsConfigs() throws FlowStoreServiceConnectorException, HarvesterException {
        final List<RRHarvesterConfig> configs = new ArrayList<>(0);
        when(flowStoreServiceConnector.findEnabledHarvesterConfigsByType(rrHarvesterConfigurationType))
                .thenReturn(configs);

        final HarvesterConfigurationBean bean = newHarvesterConfigurationBean();
        bean.configs = new ArrayList<>(Collections.singletonList(new RRHarvesterConfig(1, 1, new RRHarvesterConfig.Content())));
        bean.reload();
        assertThat("config after initialize", bean.configs, is(configs));
    }

    @Test
    public void get_returnsEmptyListOnNullConfigs() {
        final HarvesterConfigurationBean bean = newHarvesterConfigurationBean();
        assertThat(bean.get(), is(Collections.emptyList()));
    }

    @Test
    public void get_returnsConfigs() throws JSONBException {
        final List<RRHarvesterConfig> configs = Collections.singletonList(new RRHarvesterConfig(1, 1, new RRHarvesterConfig.Content()));
        final HarvesterConfigurationBean bean = newHarvesterConfigurationBean();
        bean.configs = configs;
        assertThat(bean.get(), is(configs));
    }

    private HarvesterConfigurationBean newHarvesterConfigurationBean() {
        final HarvesterConfigurationBean harvesterConfigurationBean = new HarvesterConfigurationBean();
        harvesterConfigurationBean.flowStoreServiceConnectorBean = flowStoreServiceConnectorBean;
        return harvesterConfigurationBean;
    }
}