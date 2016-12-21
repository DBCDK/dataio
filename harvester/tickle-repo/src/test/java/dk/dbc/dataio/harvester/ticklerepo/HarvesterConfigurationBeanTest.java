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
import dk.dbc.dataio.common.utils.flowstore.ejb.FlowStoreServiceConnectorBean;
import dk.dbc.dataio.harvester.types.HarvesterException;
import dk.dbc.dataio.harvester.types.TickleRepoHarvesterConfig;
import dk.dbc.ticklerepo.TickleRepo;
import dk.dbc.ticklerepo.dto.DataSet;
import org.junit.Before;
import org.junit.Test;

import javax.ejb.EJBException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static dk.dbc.commons.testutil.Assert.assertThat;
import static dk.dbc.commons.testutil.Assert.isThrowing;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class HarvesterConfigurationBeanTest {
    private final FlowStoreServiceConnectorBean flowStoreServiceConnectorBean = mock(FlowStoreServiceConnectorBean.class);
    private final FlowStoreServiceConnector flowStoreServiceConnector = mock(FlowStoreServiceConnector.class);
    private final TickleRepo tickleRepo = mock(TickleRepo.class);
    private final static String DATA_SET_NAME = "random";

    @Before
    public void setupMocks() {
        when(flowStoreServiceConnectorBean.getConnector()).thenReturn(flowStoreServiceConnector);
    }

    @Test
    public void initialize_flowStoreLookupThrows_throws() throws FlowStoreServiceConnectorException {
        when(flowStoreServiceConnector.findEnabledHarvesterConfigsByType(TickleRepoHarvesterConfig.class))
                .thenThrow(new FlowStoreServiceConnectorException("Died"));

        final HarvesterConfigurationBean bean = newHarvesterConfigurationBean();
        assertThat(bean::initialize, isThrowing(EJBException.class));
    }

    @Test
    public void initialize_flowStoreLookupReturns_setsConfigs() throws FlowStoreServiceConnectorException {
        final List<TickleRepoHarvesterConfig> configs = getTickleRepoHarvesterConfigs(getTickleRepoHarvesterConfig(DATA_SET_NAME));
        when(flowStoreServiceConnector.findEnabledHarvesterConfigsByType(TickleRepoHarvesterConfig.class))
                .thenReturn(configs);

        when(tickleRepo.lookupDataSet(any(DataSet.class))).thenReturn(Optional.ofNullable(new DataSet().withName(DATA_SET_NAME)));
        final HarvesterConfigurationBean bean = newHarvesterConfigurationBean();
        assertThat("config before initialize", bean.configs, is(nullValue()));
        bean.initialize();
        assertThat("config after initialize", bean.configs, is(notNullValue()));
        assertThat(bean.configs.get(0).getDataSet(), is(notNullValue()));
    }

    @Test
    public void reload_flowStoreLookupReturns_setsConfigs() throws FlowStoreServiceConnectorException, HarvesterException {

        final HarvesterConfigurationBean bean = newHarvesterConfigurationBean();
        final DataSet dataSet = new DataSet().withName(DATA_SET_NAME).withAgencyId(42);
        final TickleRepoHarvesterConfig tickleRepoHarvesterConfig = new TickleRepoHarvesterConfig(1,1, new TickleRepoHarvesterConfig.Content().withDatasetName(DATA_SET_NAME));
        final List<TickleRepoHarvesterConfig> configsPostReload = Collections.singletonList(tickleRepoHarvesterConfig);

        when(tickleRepo.lookupDataSet(any(DataSet.class))).thenReturn(Optional.of(dataSet));
        when(flowStoreServiceConnector.findEnabledHarvesterConfigsByType(TickleRepoHarvesterConfig.class))
                .thenReturn(configsPostReload);

        // Subject under test
        bean.reload();

        // Verification
        assertThat("extendedTickleRepoConfig after reload", bean.configs, is(notNullValue()));
        final ExtendedTickleRepoHarvesterConfig extendedTickleRepoHarvesterConfig = bean.configs.get(0);
        assertThat("dataSet after reload", extendedTickleRepoHarvesterConfig.getDataSet(), is(dataSet));
        assertThat("tickleRepoConfig after reload", extendedTickleRepoHarvesterConfig.getTickleRepoHarvesterConfig(), is(tickleRepoHarvesterConfig));
    }

    @Test
    public void get_returnsConfigs() {
        final List<ExtendedTickleRepoHarvesterConfig> configs = Collections.singletonList(new ExtendedTickleRepoHarvesterConfig()
                .withTickleRepoHarvesterConfig(new TickleRepoHarvesterConfig(1,1, new TickleRepoHarvesterConfig.Content()))
                .withDataSet(new DataSet()));

        final HarvesterConfigurationBean bean = newHarvesterConfigurationBean();
        bean.configs = configs;
        assertThat(bean.get(), is(configs));
    }

    /*
     * Private methods
     */
    private HarvesterConfigurationBean newHarvesterConfigurationBean() {
        final HarvesterConfigurationBean harvesterConfigurationBean = new HarvesterConfigurationBean();
        harvesterConfigurationBean.flowStoreServiceConnectorBean = flowStoreServiceConnectorBean;
        harvesterConfigurationBean.tickleRepo = tickleRepo;
        return harvesterConfigurationBean;
    }

    private List<TickleRepoHarvesterConfig> getTickleRepoHarvesterConfigs(TickleRepoHarvesterConfig.Content... entries) {
        final List<TickleRepoHarvesterConfig> configs = new ArrayList<>(entries.length);
        long id = 1;
        for (TickleRepoHarvesterConfig.Content content : entries) {
            configs.add(new TickleRepoHarvesterConfig(id++, 1, content));
        }
        return configs;
    }

    private TickleRepoHarvesterConfig.Content getTickleRepoHarvesterConfig(String dataSetName) {
        return new TickleRepoHarvesterConfig.Content()
                .withDatasetName(dataSetName);
    }
}
