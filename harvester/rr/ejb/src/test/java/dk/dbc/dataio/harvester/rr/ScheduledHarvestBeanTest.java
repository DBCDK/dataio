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

import dk.dbc.dataio.commons.types.jndi.JndiConstants;
import dk.dbc.dataio.commons.utils.test.jndi.InMemoryInitialContextFactory;
import dk.dbc.dataio.harvester.types.HarvesterException;
import dk.dbc.dataio.harvester.types.RawRepoHarvesterConfig;
import dk.dbc.dataio.jsonb.JSONBContext;
import dk.dbc.dataio.jsonb.JSONBException;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.ejb.Timer;
import javax.naming.Context;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ScheduledHarvestBeanTest {
    private final HarvesterBean harvesterBean = mock(HarvesterBean.class);
    private final Timer timer = null;

    @BeforeClass
    public static void setup() {
        // sets up the InMemoryInitialContextFactory as default factory.
        System.setProperty(Context.INITIAL_CONTEXT_FACTORY, InMemoryInitialContextFactory.class.getName());
    }

    @Test
    public void scheduleHarvests_emptyConfig_resultsInZeroRunningHarvests() {
        bindHarvesterConfig();
        final ScheduledHarvestBean scheduledHarvestBean = getScheduledHarvestBean();
        scheduledHarvestBean.scheduleHarvests(timer);
        assertThat("Number of running harvests", scheduledHarvestBean.runningHarvests.size(), is(0));
    }

    @Test
    public void scheduleHarvests_nonEmptyConfig_resultsInRunningHarvests() {
        bindHarvesterConfig(HarvesterTestUtil.getHarvestOperationConfigEntry());
        final ScheduledHarvestBean scheduledHarvestBean = getScheduledHarvestBean();
        scheduledHarvestBean.scheduleHarvests(timer);
        assertThat("Number of running harvests", scheduledHarvestBean.runningHarvests.size(), is(1));
    }

    @Test
    public void scheduleHarvests_harvestCompletes_reschedulesHarvest() throws HarvesterException {
        when(harvesterBean.harvest(any(RawRepoHarvesterConfig.Entry.class))).thenReturn(new MockedFuture());
        bindHarvesterConfig(HarvesterTestUtil.getHarvestOperationConfigEntry());
        final ScheduledHarvestBean scheduledHarvestBean = getScheduledHarvestBean();
        scheduledHarvestBean.scheduleHarvests(timer);
        scheduledHarvestBean.scheduleHarvests(timer);
        assertThat("Number of running harvests", scheduledHarvestBean.runningHarvests.size(), is(1));
    }

    @Test
    public void scheduleHarvests_harvestCompletesAndIsNoLongerConfigured_removesHarvest() throws HarvesterException {
        when(harvesterBean.harvest(any(RawRepoHarvesterConfig.Entry.class))).thenReturn(new MockedFuture());
        bindHarvesterConfig(HarvesterTestUtil.getHarvestOperationConfigEntry());
        final ScheduledHarvestBean scheduledHarvestBean = getScheduledHarvestBean();
        scheduledHarvestBean.scheduleHarvests(timer);
        bindHarvesterConfig();
        scheduledHarvestBean.scheduleHarvests(timer);
        assertThat("Number of running harvests", scheduledHarvestBean.runningHarvests.size(), is(0));
    }

    @Test
    public void scheduleHarvests_harvestCompletesWithException_reschedulesHarvest() throws HarvesterException {
        final MockedFuture mockedFuture = new MockedFuture();
        mockedFuture.exception = new ExecutionException("DIED", new IllegalStateException());
        when(harvesterBean.harvest(any(RawRepoHarvesterConfig.Entry.class))).thenReturn(mockedFuture);
        bindHarvesterConfig(HarvesterTestUtil.getHarvestOperationConfigEntry());
        final ScheduledHarvestBean scheduledHarvestBean = getScheduledHarvestBean();
        scheduledHarvestBean.scheduleHarvests(timer);
        scheduledHarvestBean.scheduleHarvests(timer);
        assertThat("Number of running harvests", scheduledHarvestBean.runningHarvests.size(), is(1));
    }

    @Test
    public void scheduleHarvests_harvestCompletesWithExceptionAndIsNoLongerConfigured_removesHarvest() throws HarvesterException {
        final MockedFuture mockedFuture = new MockedFuture();
        mockedFuture.exception = new ExecutionException("DIED", new IllegalStateException());
        when(harvesterBean.harvest(any(RawRepoHarvesterConfig.Entry.class))).thenReturn(mockedFuture);
        bindHarvesterConfig(HarvesterTestUtil.getHarvestOperationConfigEntry());
        final ScheduledHarvestBean scheduledHarvestBean = getScheduledHarvestBean();
        scheduledHarvestBean.scheduleHarvests(timer);
        bindHarvesterConfig();
        scheduledHarvestBean.scheduleHarvests(timer);
        assertThat("Number of running harvests", scheduledHarvestBean.runningHarvests.size(), is(0));
    }

    private ScheduledHarvestBean getScheduledHarvestBean() {
        final ScheduledHarvestBean scheduledHarvestBean = new ScheduledHarvestBean();
        scheduledHarvestBean.harvester = harvesterBean;
        scheduledHarvestBean.config = new HarvesterConfigurationBean();
        return scheduledHarvestBean;
    }

    private void bindHarvesterConfig(RawRepoHarvesterConfig.Entry... entries) {
        final RawRepoHarvesterConfig rawRepoHarvesterConfig = HarvesterTestUtil.getRawRepoHarvesterConfig(entries);
        try {
            InMemoryInitialContextFactory.bind(JndiConstants.CONFIG_RESOURCE_HARVESTER_RR,
                    new JSONBContext().marshall(rawRepoHarvesterConfig));
        } catch (JSONBException e) {
            throw new IllegalStateException(e);
        }
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