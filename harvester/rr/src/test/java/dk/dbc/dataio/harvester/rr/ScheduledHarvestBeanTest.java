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

import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnector;
import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnectorException;
import dk.dbc.dataio.common.utils.flowstore.ejb.FlowStoreServiceConnectorBean;
import dk.dbc.dataio.harvester.types.HarvesterException;
import dk.dbc.dataio.harvester.types.RRHarvesterConfig;
import dk.dbc.rawrepo.queue.ConfigurationException;
import dk.dbc.rawrepo.queue.QueueException;
import org.junit.Before;
import org.junit.Test;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ScheduledHarvestBeanTest {
    private final FlowStoreServiceConnectorBean flowStoreServiceConnectorBean = mock(FlowStoreServiceConnectorBean.class);
    private final FlowStoreServiceConnector flowStoreServiceConnector = mock(FlowStoreServiceConnector.class);
    private final Class rrHarvesterConfigurationType = RRHarvesterConfig.class;
    private final HarvesterBean harvesterBean = mock(HarvesterBean.class);

    @Before
    public void setupMocks() {
        when(flowStoreServiceConnectorBean.getConnector()).thenReturn(flowStoreServiceConnector);
    }

    @Test
    public void scheduleHarvests_emptyConfig_resultsInZeroRunningHarvests() {
        final ScheduledHarvestBean scheduledHarvestBean = newScheduledHarvestBean(Collections.emptyList());
        scheduledHarvestBean.scheduleHarvests();
        assertThat("Number of running harvests", scheduledHarvestBean.runningHarvests.size(), is(0));
    }

    @Test
    public void scheduleHarvests_nonEmptyConfig_resultsInRunningHarvests() {
        final ScheduledHarvestBean scheduledHarvestBean = newScheduledHarvestBean();
        scheduledHarvestBean.scheduleHarvests();
        assertThat("Number of running harvests", scheduledHarvestBean.runningHarvests.size(), is(1));
    }

    @Test
    public void scheduleHarvests_harvestCompletes_reschedulesHarvest()
            throws HarvesterException, QueueException, ConfigurationException, SQLException {
        when(harvesterBean.harvest(any(RRHarvesterConfig.class))).thenReturn(new MockedFuture());
        final ScheduledHarvestBean scheduledHarvestBean = newScheduledHarvestBean();
        scheduledHarvestBean.scheduleHarvests();
        scheduledHarvestBean.scheduleHarvests();
        assertThat("Number of running harvests", scheduledHarvestBean.runningHarvests.size(), is(1));
    }

    @Test
    public void scheduleHarvests_harvestCompletesAndIsNoLongerConfigured_removesHarvest()
            throws HarvesterException, FlowStoreServiceConnectorException, QueueException, ConfigurationException, SQLException {
        when(harvesterBean.harvest(any(RRHarvesterConfig.class))).thenReturn(new MockedFuture());
        final ScheduledHarvestBean scheduledHarvestBean = newScheduledHarvestBean();
        when(flowStoreServiceConnector.findEnabledHarvesterConfigsByType(rrHarvesterConfigurationType))
                .thenReturn(HarvesterTestUtil.getRRHarvesterConfigs(HarvesterTestUtil.getRRHarvestConfigContent()))
                .thenReturn(new ArrayList<>(0));

        scheduledHarvestBean.scheduleHarvests();
        scheduledHarvestBean.scheduleHarvests();
        assertThat("Number of running harvests", scheduledHarvestBean.runningHarvests.size(), is(0));
    }

    @Test
    public void scheduleHarvests_harvestCompletesWithException_reschedulesHarvest()
            throws HarvesterException, QueueException, ConfigurationException, SQLException {
        final MockedFuture mockedFuture = new MockedFuture();
        mockedFuture.exception = new ExecutionException("DIED", new IllegalStateException());
        when(harvesterBean.harvest(any(RRHarvesterConfig.class))).thenReturn(mockedFuture);
        final ScheduledHarvestBean scheduledHarvestBean = newScheduledHarvestBean();
        scheduledHarvestBean.scheduleHarvests();
        scheduledHarvestBean.scheduleHarvests();
        assertThat("Number of running harvests", scheduledHarvestBean.runningHarvests.size(), is(1));
    }

    @Test
    public void scheduleHarvests_harvestCompletesWithExceptionAndIsNoLongerConfigured_removesHarvest()
            throws HarvesterException, FlowStoreServiceConnectorException, QueueException, ConfigurationException, SQLException {
        final MockedFuture mockedFuture = new MockedFuture();
        mockedFuture.exception = new ExecutionException("DIED", new IllegalStateException());
        when(harvesterBean.harvest(any(RRHarvesterConfig.class))).thenReturn(mockedFuture);
        final ScheduledHarvestBean scheduledHarvestBean = newScheduledHarvestBean();
        when(flowStoreServiceConnector.findEnabledHarvesterConfigsByType(rrHarvesterConfigurationType))
                .thenReturn(HarvesterTestUtil.getRRHarvesterConfigs(HarvesterTestUtil.getRRHarvestConfigContent()))
                .thenReturn(new ArrayList<>(0));

        scheduledHarvestBean.scheduleHarvests();
        scheduledHarvestBean.scheduleHarvests();
        assertThat("Number of running harvests", scheduledHarvestBean.runningHarvests.size(), is(0));
    }

    private ScheduledHarvestBean newScheduledHarvestBean() {
        return newScheduledHarvestBean(HarvesterTestUtil.getRRHarvesterConfigs(HarvesterTestUtil.getRRHarvestConfigContent()));
    }

    private ScheduledHarvestBean newScheduledHarvestBean(List<RRHarvesterConfig> configs) {
        try {
            when(flowStoreServiceConnector.findEnabledHarvesterConfigsByType(rrHarvesterConfigurationType)).thenReturn(configs);
        } catch (FlowStoreServiceConnectorException e) {
            throw new IllegalStateException(e);
        }
        final ScheduledHarvestBean scheduledHarvestBean = new ScheduledHarvestBean();
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
        public Integer get() throws InterruptedException, ExecutionException {
            if (exception != null) {
                throw exception;
            }
            return result;
        }

        @Override
        public Integer get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
            if (exception != null) {
                throw exception;
            }
            return result;
        }
    }
}
