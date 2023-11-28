package dk.dbc.dataio.harvester;

import dk.dbc.dataio.harvester.types.CoRepoHarvesterConfig;
import dk.dbc.dataio.harvester.types.HarvesterException;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class AbstractHarvesterBeanTest {
    @Test
    public void harvest_returnsResultOfExecuteForMethod() throws HarvesterException, ExecutionException, InterruptedException {
        CoRepoHarvesterConfig config = new CoRepoHarvesterConfig(1, 1, new CoRepoHarvesterConfig.Content());

        AbstractHarvesterBeanImpl harvesterBean = new AbstractHarvesterBeanImpl();
        Future<Integer> harvestResult = harvesterBean.harvest(config);
        assertThat("Items harvested", harvestResult.get(), is(42));
    }

    public static class AbstractHarvesterBeanImpl extends AbstractHarvesterBean<AbstractHarvesterBeanImpl, CoRepoHarvesterConfig> {
        @Override
        public int executeFor(CoRepoHarvesterConfig config) {
            return 42;
        }

        @Override
        public AbstractHarvesterBeanImpl self() {
            return this;
        }

        @Override
        public Logger getLogger() {
            return LoggerFactory.getLogger(getClass());
        }
    }
}
