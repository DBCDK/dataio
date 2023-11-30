package dk.dbc.dataio.harvester.rr;

import dk.dbc.dataio.harvester.types.HarvesterException;
import dk.dbc.dataio.harvester.types.RRHarvesterConfig;
import dk.dbc.rawrepo.queue.ConfigurationException;
import dk.dbc.rawrepo.queue.QueueException;
import jakarta.ejb.SessionContext;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.sql.SQLException;
import java.util.Set;
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
    public void harvest_harvestOperationCompletes_returnsNumberOfItemsHarvested() throws HarvesterException, ExecutionException, InterruptedException, QueueException, ConfigurationException, SQLException {
        HarvesterBean harvesterBean = getHarvesterBean();
        RRHarvesterConfig config = new RRHarvesterConfig(1, 1, new RRHarvesterConfig.Content());
        final int expectedNumberOfItemsHarvested = 42;
        when(harvestOperation.execute()).thenReturn(expectedNumberOfItemsHarvested);

        Future<Integer> harvestResult = harvesterBean.harvest(config);
        assertThat("Items harvested", harvestResult.get(), is(expectedNumberOfItemsHarvested));
    }

    private HarvesterBean getHarvesterBean() {
        HarvesterBean harvesterBean = Mockito.spy(new HarvesterBean());
        harvesterBean.sessionContext = sessionContext;
        harvesterBean.harvestOperationFactory = harvestOperationFactory;
        harvesterBean.excludedHarvesterIds = Set.of();
        when(sessionContext.getBusinessObject(HarvesterBean.class)).thenReturn(harvesterBean);
        when(harvestOperationFactory.createFor(any(RRHarvesterConfig.class))).thenReturn(harvestOperation);
        return harvesterBean;
    }
}
