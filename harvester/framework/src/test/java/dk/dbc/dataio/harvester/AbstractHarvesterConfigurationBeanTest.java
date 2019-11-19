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

package dk.dbc.dataio.harvester;

import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnector;
import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnectorException;
import dk.dbc.dataio.common.utils.flowstore.ejb.FlowStoreServiceConnectorBean;
import dk.dbc.dataio.harvester.types.CoRepoHarvesterConfig;
import dk.dbc.dataio.harvester.types.HarvesterException;
import java.util.Arrays;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;

import static dk.dbc.commons.testutil.Assert.assertThat;
import static dk.dbc.commons.testutil.Assert.isThrowing;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AbstractHarvesterConfigurationBeanTest {
    private final FlowStoreServiceConnectorBean flowStoreServiceConnectorBean = mock(FlowStoreServiceConnectorBean.class);
    private final FlowStoreServiceConnector flowStoreServiceConnector = mock(FlowStoreServiceConnector.class);
    private final CoRepoHarvesterConfig config = new CoRepoHarvesterConfig(1, 1, new CoRepoHarvesterConfig.Content());
    private final CoRepoHarvesterConfig config2 = new CoRepoHarvesterConfig(2, 1, new CoRepoHarvesterConfig.Content());


    @Before
    public void setupMocks() {
        when(flowStoreServiceConnectorBean.getConnector()).thenReturn(flowStoreServiceConnector);
    }

    @Test
    public void flowStoreLookupThrowsDuringReload() throws FlowStoreServiceConnectorException {
        when(flowStoreServiceConnector.findEnabledHarvesterConfigsByType(CoRepoHarvesterConfig.class))
                .thenThrow(new FlowStoreServiceConnectorException("Died"));

        final AbstractHarvesterConfigurationBeanImpl harvesterConfigurationBean = getImplementation();
        assertThat(harvesterConfigurationBean::reload, isThrowing(HarvesterException.class));
    }

    @Test
    public void reloadingConfigurations() throws FlowStoreServiceConnectorException, HarvesterException {
        final List<CoRepoHarvesterConfig> expectedConfigs = Collections.singletonList(config);
        when(flowStoreServiceConnector.findEnabledHarvesterConfigsByType(CoRepoHarvesterConfig.class))
                .thenReturn(expectedConfigs);

        final AbstractHarvesterConfigurationBeanImpl harvesterConfigurationBean = getImplementation();
        harvesterConfigurationBean.reload();
        assertThat(harvesterConfigurationBean.getConfigs(), is(expectedConfigs));
    }

    @Test
    public void getConfigWithId() throws FlowStoreServiceConnectorException, HarvesterException {
        final List<CoRepoHarvesterConfig> configList = Arrays.asList(config, config2);
        when(flowStoreServiceConnector.findEnabledHarvesterConfigsByType(CoRepoHarvesterConfig.class))
                .thenReturn(configList);

        final AbstractHarvesterConfigurationBeanImpl harvesterConfigurationBean = getImplementation();
        harvesterConfigurationBean.reload();
        Optional configFromId = harvesterConfigurationBean.getConfig(1);
        assertThat("Config is present", configFromId.isPresent(), is(true));
        assertThat(configFromId.get(), is(config));
    }

    @Test
    public void nullConfigsReturnsEmptyList() {
        assertThat(getImplementation().getConfigs(), is(Collections.emptyList()));
    }

    private AbstractHarvesterConfigurationBeanImpl getImplementation() {
        return new AbstractHarvesterConfigurationBeanImpl(flowStoreServiceConnectorBean);
    }

    public static class AbstractHarvesterConfigurationBeanImpl extends AbstractHarvesterConfigurationBean<CoRepoHarvesterConfig> {
        public AbstractHarvesterConfigurationBeanImpl(FlowStoreServiceConnectorBean flowStoreServiceConnectorBean) {
            this.flowStoreServiceConnectorBean = flowStoreServiceConnectorBean;
        }

        @Override
        public Logger getLogger() {
            return LoggerFactory.getLogger(getClass());
        }

        @Override
        public Class<CoRepoHarvesterConfig> getConfigClass() {
            return CoRepoHarvesterConfig.class;
        }
    }
}