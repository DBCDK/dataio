package dk.dbc.dataio.harvester.ticklerepo;

import dk.dbc.dataio.harvester.types.HarvesterException;
import dk.dbc.dataio.harvester.types.TickleRepoHarvesterConfig;
import jakarta.ejb.SessionContext;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class HarvesterBeanTest {
    private final SessionContext sessionContext = mock(SessionContext.class);
    private final HarvestOperation harvestOperation = mock(HarvestOperation.class);
    private final HarvestOperationFactoryBean harvestOperationFactory = mock(HarvestOperationFactoryBean.class);

    @Test
    public void harvest_harvestOperationCompletes_returnsNumberOfItemsHarvested() throws HarvesterException, ExecutionException, InterruptedException {
        HarvesterBean harvesterBean = getHarvesterBean();
        TickleRepoHarvesterConfig config = createConfig();
        int expectedNumberOfItemsHarvested = 42;
        when(harvestOperation.execute()).thenReturn(expectedNumberOfItemsHarvested);

        Future<Integer> harvestResult = harvesterBean.harvest(config);
        assertThat("Items harvested", harvestResult.get(), is(expectedNumberOfItemsHarvested));
    }

    private HarvesterBean getHarvesterBean() {
        HarvesterBean harvesterBean = Mockito.spy(new HarvesterBean());
        harvesterBean.sessionContext = sessionContext;
        harvesterBean.harvestOperationFactory = harvestOperationFactory;
        when(sessionContext.getBusinessObject(HarvesterBean.class)).thenReturn(harvesterBean);
        when(harvestOperationFactory.createFor(any(TickleRepoHarvesterConfig.class))).thenReturn(harvestOperation);
        return harvesterBean;
    }

    private TickleRepoHarvesterConfig createConfig() {
        return new TickleRepoHarvesterConfig(1, 1, new TickleRepoHarvesterConfig.Content());
    }
}
