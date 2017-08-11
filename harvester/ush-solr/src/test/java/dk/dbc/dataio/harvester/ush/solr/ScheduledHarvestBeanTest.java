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

import dk.dbc.dataio.harvester.types.HarvesterException;
import dk.dbc.dataio.harvester.types.UshHarvesterProperties;
import dk.dbc.dataio.harvester.types.UshSolrHarvesterConfig;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
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
    private List<UshSolrHarvesterConfig> configs = new ArrayList<>();

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

    @Test
    public void scheduleHarvests_configHasNullValuedUshHarvesterProperties_harvestIsNotEligibleForExecution() {
        final UshSolrHarvesterConfig config = newConfig();
        config.getContent().withUshHarvesterProperties(null);
        injectConfigs(config);

        final ScheduledHarvestBean scheduledHarvestBean = getScheduledHarvestBean();
        scheduledHarvestBean.scheduleHarvests();
        assertThat("Number of running harvests", scheduledHarvestBean.runningHarvests.size(), is(0));
    }

    @Test
    public void scheduleHarvests_ushHarvesterPropertiesHasNonOkLastStatus_harvestIsNotEligibleForExecution() {
        final UshSolrHarvesterConfig config = newConfig();
        config.getContent()
                .withUshHarvesterProperties(
                        new UshHarvesterProperties()
                            .withCurrentStatus("FAILED"));
        injectConfigs(config);

        final ScheduledHarvestBean scheduledHarvestBean = getScheduledHarvestBean();
        scheduledHarvestBean.scheduleHarvests();
        assertThat("Number of running harvests", scheduledHarvestBean.runningHarvests.size(), is(0));
    }

    @Test
    public void scheduleHarvests_ushHarvesterPropertiesHasNoLastHarvested_harvestIsNotEligibleForExecution() {
        final UshSolrHarvesterConfig config = newConfig();
        config.getContent()
                .withUshHarvesterProperties(
                        new UshHarvesterProperties()
                            .withLastHarvestFinishedDate(null));
        injectConfigs(config);

        final ScheduledHarvestBean scheduledHarvestBean = getScheduledHarvestBean();
        scheduledHarvestBean.scheduleHarvests();
        assertThat("Number of running harvests", scheduledHarvestBean.runningHarvests.size(), is(0));
    }

    @Test
    public void scheduleHarvests_24HoursHasNotPassedSinceLastSolrHarvest_harvestIsNotEligibleForExecution() {
        final UshSolrHarvesterConfig config = newConfigEligibleForExecution();
        config.getContent().withTimeOfLastHarvest(new Date());
        injectConfigs(config);

        final ScheduledHarvestBean scheduledHarvestBean = getScheduledHarvestBean();
        scheduledHarvestBean.scheduleHarvests();
        assertThat("Number of running harvests", scheduledHarvestBean.runningHarvests.size(), is(0));
    }

    @Test
    public void scheduleHarvests_lastUshHarvestIsNotNewerThanLastSolrHarvest_harvestIsNotEligibleForExecution() {
        final Date timeOfLastHarvest = new Date();
        final UshSolrHarvesterConfig config = newConfigEligibleForExecution();
        config.getContent().withTimeOfLastHarvest(timeOfLastHarvest);
        config.getContent().getUshHarvesterProperties().withLastHarvestFinishedDate(timeOfLastHarvest);
        injectConfigs(config);

        final ScheduledHarvestBean scheduledHarvestBean = getScheduledHarvestBean();
        scheduledHarvestBean.scheduleHarvests();
        assertThat("Number of running harvests", scheduledHarvestBean.runningHarvests.size(), is(0));
    }

    private ScheduledHarvestBean getScheduledHarvestBean() {
        final HarvesterConfigurationBean harvesterConfigurationBean = mock(HarvesterConfigurationBean.class);
        when(harvesterConfigurationBean.get()).thenReturn(configs);
        final ScheduledHarvestBean scheduledHarvestBean = new ScheduledHarvestBean();
        scheduledHarvestBean.harvester = harvesterBean;
        scheduledHarvestBean.configs = harvesterConfigurationBean;
        return scheduledHarvestBean;
    }

    private void injectConfigs(UshSolrHarvesterConfig... entries) {
        configs.clear();
        Collections.addAll(configs, entries);
    }

    private UshSolrHarvesterConfig newConfig() {
        return newConfig(1);
    }

    private UshSolrHarvesterConfig newConfig(long id) {
       return new UshSolrHarvesterConfig(id, 1,
                new UshSolrHarvesterConfig.Content());
    }

    private UshSolrHarvesterConfig newConfigEligibleForExecution() {
        return newConfigEligibleForExecution(1);
    }

    private UshSolrHarvesterConfig newConfigEligibleForExecution(long id) {
        return new UshSolrHarvesterConfig(id, 1,
                new UshSolrHarvesterConfig.Content()
                    .withUshHarvesterProperties(
                            new UshHarvesterProperties()
                                .withCurrentStatus("OK")
                                .withLastHarvestFinishedDate(new Date())));
    }

    private void mockedHarvestCompletes() throws HarvesterException {
        when(harvesterBean.harvest(any(UshSolrHarvesterConfig.class))).thenReturn(new MockedFuture());
    }

    private void mockedHarvestCompletesWithException() throws HarvesterException {
        final MockedFuture mockedFuture = new MockedFuture();
        mockedFuture.exception = new ExecutionException("DIED", new IllegalStateException());
        when(harvesterBean.harvest(any(UshSolrHarvesterConfig.class))).thenReturn(mockedFuture);
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
