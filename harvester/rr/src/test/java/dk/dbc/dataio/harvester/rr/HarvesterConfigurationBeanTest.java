package dk.dbc.dataio.harvester.rr;

import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnector;
import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnectorException;
import dk.dbc.dataio.common.utils.flowstore.ejb.FlowStoreServiceConnectorBean;
import dk.dbc.dataio.harvester.types.HarvesterException;
import dk.dbc.dataio.harvester.types.RRHarvesterConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static dk.dbc.commons.testutil.Assert.assertThat;
import static dk.dbc.commons.testutil.Assert.isThrowing;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class HarvesterConfigurationBeanTest {
    private final FlowStoreServiceConnectorBean flowStoreServiceConnectorBean = mock(FlowStoreServiceConnectorBean.class);
    private final FlowStoreServiceConnector flowStoreServiceConnector = mock(FlowStoreServiceConnector.class);
    private final Class<RRHarvesterConfig> rrHarvesterConfigurationType = RRHarvesterConfig.class;

    @BeforeEach
    public void setupMocks() {
        when(flowStoreServiceConnectorBean.getConnector()).thenReturn(flowStoreServiceConnector);
    }

    @Test
    public void reload_flowStoreLookupThrows_throws() throws FlowStoreServiceConnectorException {
        when(flowStoreServiceConnector.findEnabledHarvesterConfigsByType(rrHarvesterConfigurationType)).thenThrow(new FlowStoreServiceConnectorException("Died"));

        HarvesterConfigurationBean bean = newHarvesterConfigurationBean();
        assertThat(bean::reload, isThrowing(HarvesterException.class));
    }

    @Test
    public void reload_flowStoreLookupReturns_setsConfigs() throws FlowStoreServiceConnectorException, HarvesterException {
        List<RRHarvesterConfig> configs = new ArrayList<>(0);
        when(flowStoreServiceConnector.findEnabledHarvesterConfigsByType(rrHarvesterConfigurationType)).thenReturn(configs);

        HarvesterConfigurationBean bean = newHarvesterConfigurationBean();
        bean.configs = new ArrayList<>(Collections.singletonList(new RRHarvesterConfig(1, 1, new RRHarvesterConfig.Content())));
        bean.reload();
        assertThat("config after initialize", bean.configs, is(configs));
    }

    @Test
    public void get_returnsEmptyListOnNullConfigs() {
        HarvesterConfigurationBean bean = newHarvesterConfigurationBean();
        assertThat(bean.get(), is(Collections.emptyList()));
    }

    @Test
    public void get_returnsConfigs() {
        List<RRHarvesterConfig> configs = Collections.singletonList(new RRHarvesterConfig(1, 1, new RRHarvesterConfig.Content()));
        HarvesterConfigurationBean bean = newHarvesterConfigurationBean();
        bean.configs = configs;
        assertThat(bean.get(), is(configs));
    }

    private HarvesterConfigurationBean newHarvesterConfigurationBean() {
        HarvesterConfigurationBean harvesterConfigurationBean = new HarvesterConfigurationBean();
        harvesterConfigurationBean.flowStoreServiceConnectorBean = flowStoreServiceConnectorBean;
        return harvesterConfigurationBean;
    }
}
