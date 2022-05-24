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

import dk.dbc.dataio.harvester.types.HarvesterException;
import dk.dbc.dataio.harvester.types.TickleRepoHarvesterConfig;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ScheduledHarvestBeanTest {
    private final HarvesterBean harvesterBean = mock(HarvesterBean.class);
    private List<TickleRepoHarvesterConfig> configs = new ArrayList<>();

    @Before
    public void resetConfigs() {
        configs.clear();
    }

    @Test
    public void scheduleHarvests_emptyConfig_resultsInZeroRunningHarvests() {
        final ScheduledHarvestBean scheduledHarvestBean = getScheduledHarvestBean();
        scheduledHarvestBean.scheduleHarvests();
        assertThat("Number of running harvests", scheduledHarvestBean.runningHarvests.size(), is(0));
    }

    @Test
    public void scheduleHarvests_nonEmptyConfig_resultsInRunningHarvests() {
        injectConfigs(newConfigEligibleForExecution());

        final ScheduledHarvestBean scheduledHarvestBean = getScheduledHarvestBean();
        scheduledHarvestBean.scheduleHarvests();
        assertThat("Number of running harvests", scheduledHarvestBean.runningHarvests.size(), is(1));
    }

    @Test
    public void scheduleHarvests_harvestCompletes_reschedulesHarvest() throws HarvesterException {
        injectConfigs(newConfigEligibleForExecution());
        mockedHarvestCompletes();

        final ScheduledHarvestBean scheduledHarvestBean = getScheduledHarvestBean();
        scheduledHarvestBean.scheduleHarvests();
        scheduledHarvestBean.scheduleHarvests();
        assertThat("Number of running harvests", scheduledHarvestBean.runningHarvests.size(), is(1));
    }

    @Test
    public void scheduleHarvests_harvestCompletesAndIsNoLongerConfigured_removesHarvest() throws HarvesterException {
        injectConfigs(newConfigEligibleForExecution());
        mockedHarvestCompletes();

        final ScheduledHarvestBean scheduledHarvestBean = getScheduledHarvestBean();
        scheduledHarvestBean.scheduleHarvests();

        configs.clear();

        scheduledHarvestBean.scheduleHarvests();
        assertThat("Number of running harvests", scheduledHarvestBean.runningHarvests.size(), is(0));
    }

    @Test
    public void scheduleHarvests_harvestCompletesWithException_reschedulesHarvest() throws HarvesterException {
        injectConfigs(newConfigEligibleForExecution());
        mockedHarvestCompletesWithException();

        final ScheduledHarvestBean scheduledHarvestBean = getScheduledHarvestBean();
        scheduledHarvestBean.scheduleHarvests();
        scheduledHarvestBean.scheduleHarvests();
        assertThat("Number of running harvests", scheduledHarvestBean.runningHarvests.size(), is(1));
    }

    @Test
    public void scheduleHarvests_harvestCompletesWithExceptionAndIsNoLongerConfigured_removesHarvest() throws HarvesterException {
        injectConfigs(newConfigEligibleForExecution());
        mockedHarvestCompletesWithException();

        final ScheduledHarvestBean scheduledHarvestBean = getScheduledHarvestBean();
        scheduledHarvestBean.scheduleHarvests();

        configs.clear();

        scheduledHarvestBean.scheduleHarvests();
        assertThat("Number of running harvests", scheduledHarvestBean.runningHarvests.size(), is(0));
    }

    /*
     * private methods
     */

    private ScheduledHarvestBean getScheduledHarvestBean() {
        final HarvesterConfigurationBean harvesterConfigurationBean = mock(HarvesterConfigurationBean.class);
        when(harvesterConfigurationBean.get()).thenReturn(configs);
        final ScheduledHarvestBean scheduledHarvestBean = new ScheduledHarvestBean();
        scheduledHarvestBean.harvester = harvesterBean;
        scheduledHarvestBean.config = harvesterConfigurationBean;
        return scheduledHarvestBean;
    }

    private void injectConfigs(TickleRepoHarvesterConfig... entries) {
        configs.clear();
        Collections.addAll(configs, entries);
    }

    private TickleRepoHarvesterConfig newConfigEligibleForExecution() {
        final TickleRepoHarvesterConfig.Content tickleRepoHarvesterConfigContent = new TickleRepoHarvesterConfig.Content().withDatasetName("random");
        return new TickleRepoHarvesterConfig(1, 1, tickleRepoHarvesterConfigContent);
    }

    private void mockedHarvestCompletes() throws HarvesterException {
        when(harvesterBean.harvest(any(TickleRepoHarvesterConfig.class))).thenReturn(new MockedFuture());
    }

    private void mockedHarvestCompletesWithException() throws HarvesterException {
        final MockedFuture mockedFuture = new MockedFuture();
        mockedFuture.exception = new ExecutionException("DIED", new IllegalStateException());
        when(harvesterBean.harvest(any(TickleRepoHarvesterConfig.class))).thenReturn(mockedFuture);
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
