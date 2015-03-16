package dk.dbc.dataio.harvester.rr;

import dk.dbc.dataio.commons.types.jndi.JndiConstants;
import dk.dbc.dataio.commons.utils.test.jndi.InMemoryInitialContextFactory;
import dk.dbc.dataio.harvester.types.HarvesterException;
import dk.dbc.dataio.harvester.types.RawRepoHarvesterConfig;
import dk.dbc.dataio.jsonb.JSONBContext;
import dk.dbc.dataio.jsonb.JSONBException;
import dk.dbc.dataio.jsonb.ejb.JSONBBean;
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
        bindHarvesterConfig(new RawRepoHarvesterConfig.Entry().setId("id").setResource("resource"));
        final ScheduledHarvestBean scheduledHarvestBean = getScheduledHarvestBean();
        scheduledHarvestBean.scheduleHarvests(timer);
        assertThat("Number of running harvests", scheduledHarvestBean.runningHarvests.size(), is(1));
    }

    @Test
    public void scheduleHarvests_harvestCompletes_reschedulesHarvest() throws HarvesterException {
        when(harvesterBean.harvest(any(RawRepoHarvesterConfig.Entry.class))).thenReturn(new MockedFuture());
        bindHarvesterConfig(new RawRepoHarvesterConfig.Entry().setId("id").setResource("resource"));
        final ScheduledHarvestBean scheduledHarvestBean = getScheduledHarvestBean();
        scheduledHarvestBean.scheduleHarvests(timer);
        scheduledHarvestBean.scheduleHarvests(timer);
        assertThat("Number of running harvests", scheduledHarvestBean.runningHarvests.size(), is(1));
    }

    @Test
    public void scheduleHarvests_harvestCompletesAndIsNoLongerConfigured_removesHarvest() throws HarvesterException {
        when(harvesterBean.harvest(any(RawRepoHarvesterConfig.Entry.class))).thenReturn(new MockedFuture());
        bindHarvesterConfig(new RawRepoHarvesterConfig.Entry().setId("id").setResource("resource"));
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
        bindHarvesterConfig(new RawRepoHarvesterConfig.Entry().setId("id").setResource("resource"));
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
        bindHarvesterConfig(new RawRepoHarvesterConfig.Entry().setId("id").setResource("resource"));
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
        scheduledHarvestBean.config.jsonbBean = new JSONBBean();
        scheduledHarvestBean.config.jsonbBean.initialiseContext();
        return scheduledHarvestBean;
    }

    private void bindHarvesterConfig(RawRepoHarvesterConfig.Entry... entries) {
        final RawRepoHarvesterConfig rawRepoHarvesterConfig = new RawRepoHarvesterConfig();
        for (RawRepoHarvesterConfig.Entry entry : entries) {
            rawRepoHarvesterConfig.addEntry(entry);
        }
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