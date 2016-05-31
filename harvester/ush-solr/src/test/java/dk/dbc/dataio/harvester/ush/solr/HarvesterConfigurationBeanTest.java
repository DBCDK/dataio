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

package dk.dbc.dataio.harvester.ush.solr;

import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnector;
import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnectorException;
import dk.dbc.dataio.common.utils.flowstore.ejb.FlowStoreServiceConnectorBean;
import dk.dbc.dataio.commons.utils.ush.UshHarvesterConnector;
import dk.dbc.dataio.commons.utils.ush.UshHarvesterConnectorException;
import dk.dbc.dataio.commons.utils.ush.ejb.UshHarvesterConnectorBean;
import dk.dbc.dataio.harvester.types.HarvesterException;
import dk.dbc.dataio.harvester.types.UshHarvesterProperties;
import dk.dbc.dataio.harvester.types.UshSolrHarvesterConfig;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import javax.ejb.EJBException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static dk.dbc.commons.testutil.Assert.assertThat;
import static dk.dbc.commons.testutil.Assert.isThrowing;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class HarvesterConfigurationBeanTest {
    private final FlowStoreServiceConnectorBean flowStoreServiceConnectorBean = mock(FlowStoreServiceConnectorBean.class);
    private final FlowStoreServiceConnector flowStoreServiceConnector = mock(FlowStoreServiceConnector.class);
    private final UshHarvesterConnectorBean ushHarvesterConnectorBean = mock(UshHarvesterConnectorBean.class);
    private final UshHarvesterConnector ushHarvesterConnector = mock(UshHarvesterConnector.class);
    private final Class ushSolrHarvesterConfigurationType = UshSolrHarvesterConfig.class;

    @Before
    public void setupMocks() {
        when(flowStoreServiceConnectorBean.getConnector()).thenReturn(flowStoreServiceConnector);
        when(ushHarvesterConnectorBean.getConnector()).thenReturn(ushHarvesterConnector);
    }

    @Test
    public void initialize_flowStoreLookupThrows_throws() throws FlowStoreServiceConnectorException {
        when(flowStoreServiceConnector.findEnabledHarvesterConfigsByType(ushSolrHarvesterConfigurationType))
                .thenThrow(new FlowStoreServiceConnectorException("Died"));

        final HarvesterConfigurationBean bean = newHarvesterConfigurationBean();
        assertThat(bean::initialize, isThrowing(EJBException.class));
    }

    @Test
    public void initialize_ushHarvesterLookupThrows_throws() throws UshHarvesterConnectorException {
        when(ushHarvesterConnector.listIndexedUshHarvesterJobs())
                .thenThrow(new UshHarvesterConnectorException("Died"));

        final HarvesterConfigurationBean bean = newHarvesterConfigurationBean();
        assertThat(bean::initialize, isThrowing(EJBException.class));
    }

    @Test
    public void initialize_flowStoreLookupReturns_setsConfigs() throws FlowStoreServiceConnectorException {
        final List<UshSolrHarvesterConfig> configs = getUshSolrHarvesterConfigs(getUshSolrHarvestConfigContent(42));
        when(flowStoreServiceConnector.findEnabledHarvesterConfigsByType(ushSolrHarvesterConfigurationType))
                .thenReturn(configs);

        final HarvesterConfigurationBean bean = newHarvesterConfigurationBean();
        Assert.assertThat("config before initialize", bean.configs, is(nullValue()));
        bean.initialize();
        assertThat("config after initialize", bean.configs, is(notNullValue()));
    }

    @Test
    public void reload_flowStoreLookupReturns_setsConfigs() throws FlowStoreServiceConnectorException, HarvesterException {

        final HarvesterConfigurationBean bean = newHarvesterConfigurationBean();
        bean.configs = new ArrayList<>(Collections.singletonList(new UshSolrHarvesterConfig(1, 1, new UshSolrHarvesterConfig.Content())));

        final List<UshSolrHarvesterConfig> configsPostReload = new ArrayList<>(0);
        when(flowStoreServiceConnector.findEnabledHarvesterConfigsByType(ushSolrHarvesterConfigurationType))
                .thenReturn(configsPostReload);

        // Subject under test
        bean.reload();

        // Verification
        assertThat("config after initialize", bean.configs, is(configsPostReload));
    }

    @Test
    public void reload_ushHarvesterLookupReturns_populatesConfigContentIfMatchingUshHarvesterProperties()
            throws FlowStoreServiceConnectorException, HarvesterException, UshHarvesterConnectorException {

        final UshSolrHarvesterConfig.Content contentWithoutUshPropertiesMatch = getUshSolrHarvestConfigContent(41);
        final UshSolrHarvesterConfig.Content contentWithUshPropertiesMatch = getUshSolrHarvestConfigContent(42);
        final List<UshSolrHarvesterConfig> configs = getUshSolrHarvesterConfigs(contentWithoutUshPropertiesMatch, contentWithUshPropertiesMatch);
        final Map<Integer, UshHarvesterProperties> indexedUshHarvesterProperties = getIndexedUshHarvesterJobs(42);
        final HarvesterConfigurationBean bean = newHarvesterConfigurationBean();

        when(flowStoreServiceConnector.findEnabledHarvesterConfigsByType(ushSolrHarvesterConfigurationType))
                .thenReturn(configs);

        when(ushHarvesterConnector.listIndexedUshHarvesterJobs()).thenReturn(indexedUshHarvesterProperties);

        // Subject under test
        bean.reload();

        // Verification
        assertThat("config after initialize", bean.configs, is(configs));
        assertThat("configs.size()", bean.configs.size(), is(2));
        assertThat("contentWithoutUshPropertiesMatch.ushHarvesterProperties",
                bean.configs.get(0).getContent().getUshHarvesterProperties(), is(nullValue()));

        assertThat("contentWithUshPropertiesMatch.ushHarvesterProperties",
                bean.configs.get(1).getContent().getUshHarvesterProperties(), is(indexedUshHarvesterProperties.get(42)));
    }

    @Test
    public void get_returnsConfigs() {
        final List<UshSolrHarvesterConfig> configs = Collections.singletonList(new UshSolrHarvesterConfig(1, 1, new UshSolrHarvesterConfig.Content()));
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
        harvesterConfigurationBean.ushHarvesterConnectorBean = ushHarvesterConnectorBean;
        return harvesterConfigurationBean;
    }

    public static List<UshSolrHarvesterConfig> getUshSolrHarvesterConfigs(UshSolrHarvesterConfig.Content... entries) {
        final List<UshSolrHarvesterConfig> configs = new ArrayList<>(entries.length);
        long id = 1;
        for (UshSolrHarvesterConfig.Content content : entries) {
            configs.add(new UshSolrHarvesterConfig(id++, 1, content));
        }
        return configs;
    }

    public static UshSolrHarvesterConfig.Content getUshSolrHarvestConfigContent(int ushHarvesterJobId) {
        return new UshSolrHarvesterConfig.Content()
                .withUshHarvesterJobId(ushHarvesterJobId);
    }

    public static Map<Integer, UshHarvesterProperties> getIndexedUshHarvesterJobs(int... ushHarvesterJobIds) {
        final Map<Integer, UshHarvesterProperties> ushHarvesterPropertiesMap = new HashMap<>(ushHarvesterJobIds.length);
        for(int jobId : ushHarvesterJobIds) {
            ushHarvesterPropertiesMap.put(jobId, getUshHarvesterProperties(jobId));
        }
        return ushHarvesterPropertiesMap;
    }

    public static UshHarvesterProperties getUshHarvesterProperties(int jobId) {
        return new UshHarvesterProperties()
                .withJobId(jobId);
    }
}
