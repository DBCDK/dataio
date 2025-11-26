package dk.dbc.dataio.harvester.rr_dm3;

import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnector;
import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnectorException;
import dk.dbc.dataio.common.utils.flowstore.ejb.FlowStoreServiceConnectorBean;
import dk.dbc.dataio.harvester.types.HarvesterException;
import dk.dbc.dataio.harvester.types.RRV3HarvesterConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ScheduledHarvestBeanTest {
    private final FlowStoreServiceConnectorBean flowStoreServiceConnectorBean = mock(FlowStoreServiceConnectorBean.class);
    private final FlowStoreServiceConnector flowStoreServiceConnector = mock(FlowStoreServiceConnector.class);
    private final Class<RRV3HarvesterConfig> RRV3HarvesterConfigurationType = RRV3HarvesterConfig.class;
    private final HarvesterBean harvesterBean = mock(HarvesterBean.class);

    @BeforeEach
    public void setupMocks() {
        when(flowStoreServiceConnectorBean.getConnector()).thenReturn(flowStoreServiceConnector);
    }

    @Test
    public void scheduleHarvests_emptyConfig_resultsInZeroRunningHarvests() {
        ScheduledHarvestBean scheduledHarvestBean = newScheduledHarvestBean(Collections.emptyList());
        scheduledHarvestBean.scheduleHarvests();
        assertThat("Number of running harvests", scheduledHarvestBean.runningHarvests.size(), is(0));
    }

    @Test
    public void scheduleHarvests_nonEmptyConfig_resultsInRunningHarvests() {
        ScheduledHarvestBean scheduledHarvestBean = newScheduledHarvestBean();
        scheduledHarvestBean.scheduleHarvests();
        assertThat("Number of running harvests", scheduledHarvestBean.runningHarvests.size(), is(3));
    }

    @Test
    public void scheduleHarvests_harvestCompletes_reschedulesHarvest() throws HarvesterException {
        when(harvesterBean.harvest(any(RRV3HarvesterConfig.class), anyString())).thenReturn(new MockedFuture());
        ScheduledHarvestBean scheduledHarvestBean = newScheduledHarvestBean();
        scheduledHarvestBean.scheduleHarvests();
        scheduledHarvestBean.scheduleHarvests();
        assertThat("Number of running harvests", scheduledHarvestBean.runningHarvests.size(), is(3));
    }

    @Test
    public void scheduleHarvests_harvestCompletesAndIsNoLongerConfigured_removesHarvest() throws HarvesterException, FlowStoreServiceConnectorException {
        when(harvesterBean.harvest(any(RRV3HarvesterConfig.class), anyString())).thenReturn(new MockedFuture());
        ScheduledHarvestBean scheduledHarvestBean = newScheduledHarvestBean();
        when(flowStoreServiceConnector.findEnabledHarvesterConfigsByType(RRV3HarvesterConfigurationType)).thenReturn(HarvesterTestUtil.getRRHarvesterConfigs(HarvesterTestUtil.getRRHarvestConfigContent())).thenReturn(new ArrayList<>(0));

        scheduledHarvestBean.scheduleHarvests();
        scheduledHarvestBean.scheduleHarvests();
        assertThat("Number of running harvests", scheduledHarvestBean.runningHarvests.size(), is(0));
    }

    @Test
    public void scheduleHarvests_harvestCompletesWithException_reschedulesHarvest() throws HarvesterException {
        MockedFuture mockedFuture = new MockedFuture();
        mockedFuture.exception = new ExecutionException("DIED", new IllegalStateException());
        when(harvesterBean.harvest(any(RRV3HarvesterConfig.class), anyString())).thenReturn(mockedFuture);
        ScheduledHarvestBean scheduledHarvestBean = newScheduledHarvestBean();
        scheduledHarvestBean.scheduleHarvests();
        scheduledHarvestBean.scheduleHarvests();
        assertThat("Number of running harvests", scheduledHarvestBean.runningHarvests.size(), is(3));
    }

    @Test
    public void scheduleHarvests_harvestCompletesWithExceptionAndIsNoLongerConfigured_removesHarvest() throws HarvesterException, FlowStoreServiceConnectorException {
        MockedFuture mockedFuture = new MockedFuture();
        mockedFuture.exception = new ExecutionException("DIED", new IllegalStateException());
        when(harvesterBean.harvest(any(RRV3HarvesterConfig.class), anyString())).thenReturn(mockedFuture);
        ScheduledHarvestBean scheduledHarvestBean = newScheduledHarvestBean();
        when(flowStoreServiceConnector.findEnabledHarvesterConfigsByType(RRV3HarvesterConfigurationType)).thenReturn(HarvesterTestUtil.getRRHarvesterConfigs(HarvesterTestUtil.getRRHarvestConfigContent())).thenReturn(new ArrayList<>(0));

        scheduledHarvestBean.scheduleHarvests();
        scheduledHarvestBean.scheduleHarvests();
        assertThat("Number of running harvests", scheduledHarvestBean.runningHarvests.size(), is(0));
    }

    private ScheduledHarvestBean newScheduledHarvestBean() {
        return newScheduledHarvestBean(HarvesterTestUtil.getRRHarvesterConfigs(HarvesterTestUtil.getRRHarvestConfigContent()));
    }

    private ScheduledHarvestBean newScheduledHarvestBean(List<RRV3HarvesterConfig> configs) {
        try {
            when(flowStoreServiceConnector.findEnabledHarvesterConfigsByType(RRV3HarvesterConfigurationType)).thenReturn(configs);
        } catch (FlowStoreServiceConnectorException e) {
            throw new IllegalStateException(e);
        }
        ScheduledHarvestBean scheduledHarvestBean = new ScheduledHarvestBean();
        scheduledHarvestBean.harvester = harvesterBean;
        scheduledHarvestBean.config = new HarvesterConfigurationBean();
        scheduledHarvestBean.config.flowStoreServiceConnectorBean = flowStoreServiceConnectorBean;
        return scheduledHarvestBean;
    }

    private static class MockedFuture implements Future<Integer> {
        boolean isDone = true;
        int result = 42;
        ExecutionException exception = null;

        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            return false;
        }

        @Override
        public boolean isCancelled() {
            return false;
        }

        @Override
        public boolean isDone() {
            return isDone;
        }

        @Override
        public Integer get() throws ExecutionException {
            if (exception != null) {
                throw exception;
            }
            return result;
        }

        @Override
        public Integer get(long timeout, TimeUnit unit) throws ExecutionException {
            if (exception != null) {
                throw exception;
            }
            return result;
        }
    }
}
