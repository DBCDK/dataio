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
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.ScheduleExpression;
import javax.ejb.Timer;
import java.util.Collections;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AbstractScheduledHarvestBeanTest {
    private final AbstractHarvesterBeanTest.AbstractHarvesterBeanImpl harvesterBean = mock(AbstractHarvesterBeanTest.AbstractHarvesterBeanImpl.class);
    private final FlowStoreServiceConnectorBean flowStoreServiceConnectorBean = mock(FlowStoreServiceConnectorBean.class);
    private final FlowStoreServiceConnector flowStoreServiceConnector = mock(FlowStoreServiceConnector.class);
    private final CoRepoHarvesterConfig config = new CoRepoHarvesterConfig(1, 1, new CoRepoHarvesterConfig.Content());
    private final Timer timer = null;

    @Before
    public void setupMocks() throws HarvesterException, FlowStoreServiceConnectorException {
        when(flowStoreServiceConnectorBean.getConnector())
                .thenReturn(flowStoreServiceConnector);
        when(flowStoreServiceConnector.findEnabledHarvesterConfigsByType(CoRepoHarvesterConfig.class))
                .thenReturn(Collections.singletonList(config));
        when(harvesterBean.harvest(config))
                .thenReturn(new MockedFuture()
                                    .withDone(true));
    }

    @Test
    public void noConfigurationsEntailsNoHarvests() throws FlowStoreServiceConnectorException {
        when(flowStoreServiceConnector.findEnabledHarvesterConfigsByType(CoRepoHarvesterConfig.class))
                .thenReturn(Collections.emptyList());

        final AbstractScheduledHarvestBeanImpl scheduledHarvestBean = getImplementation();
        scheduledHarvestBean.scheduleHarvests(timer);
        assertThat("Number of running harvests", scheduledHarvestBean.runningHarvests.size(), is(0));
    }

    @Test
    public void configurationsEntailsRunningHarvests() throws FlowStoreServiceConnectorException {
        final AbstractScheduledHarvestBeanImpl scheduledHarvestBean = getImplementation();
        scheduledHarvestBean.scheduleHarvests(timer);
        assertThat("Number of running harvests", scheduledHarvestBean.runningHarvests.size(), is(1));
    }

    @Test
    public void completionOfHarvestEntailsRescheduling() throws FlowStoreServiceConnectorException, HarvesterException {
        final AbstractScheduledHarvestBeanImpl scheduledHarvestBean = getImplementation();
        scheduledHarvestBean.scheduleHarvests(timer);
        scheduledHarvestBean.scheduleHarvests(timer);
        assertThat("Number of running harvests", scheduledHarvestBean.runningHarvests.size(), is(1));
    }

    @Test
    public void completionOfHarvestNoLongerEnabledEntailsRemoval() throws HarvesterException, FlowStoreServiceConnectorException {
        when(flowStoreServiceConnector.findEnabledHarvesterConfigsByType(CoRepoHarvesterConfig.class))
                .thenReturn(Collections.singletonList(config))
                .thenReturn(Collections.emptyList());

        final AbstractScheduledHarvestBeanImpl scheduledHarvestBean = getImplementation();
        scheduledHarvestBean.scheduleHarvests(timer);
        scheduledHarvestBean.scheduleHarvests(timer);
        assertThat("Number of running harvests", scheduledHarvestBean.runningHarvests.size(), is(0));
    }

    @Test
    public void completionOfHarvestWithExceptionEntailsRescheduling() throws HarvesterException {
        when(harvesterBean.harvest(config)).thenReturn(new MockedFuture()
                .withException(new ExecutionException("DIED", new IllegalStateException())));

        final AbstractScheduledHarvestBeanImpl scheduledHarvestBean = getImplementation();
        scheduledHarvestBean.scheduleHarvests(timer);
        scheduledHarvestBean.scheduleHarvests(timer);
        assertThat("Number of running harvests", scheduledHarvestBean.runningHarvests.size(), is(1));
    }

    @Test
    public void completionOfHarvestNoLongerEnabledWithExceptionEntailsRemoval() throws HarvesterException, FlowStoreServiceConnectorException {
        when(flowStoreServiceConnector.findEnabledHarvesterConfigsByType(CoRepoHarvesterConfig.class))
                .thenReturn(Collections.singletonList(config))
                .thenReturn(Collections.emptyList());
        when(harvesterBean.harvest(config)).thenReturn(new MockedFuture()
                .withException(new ExecutionException("DIED", new IllegalStateException())));

        final AbstractScheduledHarvestBeanImpl scheduledHarvestBean = getImplementation();
        scheduledHarvestBean.scheduleHarvests(timer);
        scheduledHarvestBean.scheduleHarvests(timer);
        assertThat("Number of running harvests", scheduledHarvestBean.runningHarvests.size(), is(0));
    }

    private AbstractScheduledHarvestBeanImpl getImplementation() {
        final AbstractScheduledHarvestBeanImpl bean = new AbstractScheduledHarvestBeanImpl(harvesterBean, flowStoreServiceConnectorBean);
        bean.harvesterConfigurationBeanImpl = bean.getHarvesterConfigurationBeanImpl();
        return bean;
    }

    public static class AbstractScheduledHarvestBeanImpl extends AbstractScheduledHarvestBean<AbstractHarvesterBeanTest.AbstractHarvesterBeanImpl, CoRepoHarvesterConfig, AbstractHarvesterConfigurationBeanTest.AbstractHarvesterConfigurationBeanImpl> {
        private final FlowStoreServiceConnectorBean flowStoreServiceConnectorBean;

        public AbstractScheduledHarvestBeanImpl(AbstractHarvesterBeanTest.AbstractHarvesterBeanImpl harvesterBeanImpl, FlowStoreServiceConnectorBean flowStoreServiceConnectorBean) {
            this.harvesterBeanImpl = harvesterBeanImpl;
            this.flowStoreServiceConnectorBean = flowStoreServiceConnectorBean;
        }

        @Override
        public AbstractHarvesterBeanTest.AbstractHarvesterBeanImpl getHarvesterBeanImpl() {
            return harvesterBeanImpl;
        }

        @Override
        public AbstractHarvesterConfigurationBeanTest.AbstractHarvesterConfigurationBeanImpl getHarvesterConfigurationBeanImpl() {
            return new AbstractHarvesterConfigurationBeanTest.AbstractHarvesterConfigurationBeanImpl(flowStoreServiceConnectorBean);
        }

        @Override
        public ScheduleExpression getTimerSchedule() {
            return new ScheduleExpression();
        }

        @Override
        public Logger getLogger() {
            return LoggerFactory.getLogger(getClass());
        }
    }

    private static class MockedFuture implements Future<Integer> {
        private boolean done = true;
        private int result = 42;
        private ExecutionException exception = null;

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
            return done;
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

        public MockedFuture withDone(boolean done) {
            this.done = done;
            return this;
        }

        public MockedFuture withResult(int result) {
            this.result = result;
            return this;
        }

        public MockedFuture withException(ExecutionException exception) {
            this.exception = exception;
            return this;
        }
    }
}