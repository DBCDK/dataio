package dk.dbc.dataio.harvester.rr;

import dk.dbc.dataio.harvester.types.HarvesterException;
import dk.dbc.dataio.harvester.types.RawRepoHarvesterConfig;
import org.junit.Test;
import org.mockito.Mockito;

import javax.ejb.SessionContext;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class HarvesterBeanTest {
    private SessionContext sessionContext = mock(SessionContext.class);
    private HarvestOperation harvestOperation = mock(HarvestOperation.class);

    @Test
    public void harvest_harvestOperationCompletes_returnsNumberOfItemsHarvested() throws HarvesterException, ExecutionException, InterruptedException {
        final HarvesterBean harvesterBean = getHarvesterBean();
        final RawRepoHarvesterConfig.Entry config = getConfig("id");
        final int expectedNumberOfItemsHarvested = 42;
        doReturn(harvestOperation).when(harvesterBean).getHarvestOperation(config);
        when(harvestOperation.execute()).thenReturn(expectedNumberOfItemsHarvested);

        final Future<Integer> harvestResult = harvesterBean.harvest(config);
        assertThat("Items harvested", harvestResult.get(), is(expectedNumberOfItemsHarvested));
    }

    @Test
    public void harvest_availableItemsExceedsBetchSize_multipleExecutionsOfHarvestOperation() throws HarvesterException, ExecutionException, InterruptedException {
        final HarvesterBean harvesterBean = getHarvesterBean();
        final RawRepoHarvesterConfig.Entry config = getConfig("id");
        final int expectedNumberOfItemsHarvested = config.getBatchSize()*3 - 1;
        doReturn(harvestOperation).when(harvesterBean).getHarvestOperation(config);
        when(harvestOperation.execute())
                .thenReturn(config.getBatchSize())
                .thenReturn(config.getBatchSize())
                .thenReturn(config.getBatchSize() - 1);


        final Future<Integer> harvestResult = harvesterBean.harvest(config);
        assertThat("Items harvested", harvestResult.get(), is(expectedNumberOfItemsHarvested));
    }

    private HarvesterBean getHarvesterBean() {
        final HarvesterBean harvesterBean = Mockito.spy(new HarvesterBean());
        harvesterBean.sessionContext = sessionContext;
        when(sessionContext.getBusinessObject(HarvesterBean.class)).thenReturn(harvesterBean);
        return harvesterBean;
    }

    private RawRepoHarvesterConfig.Entry getConfig(String id) {
        return new RawRepoHarvesterConfig.Entry()
                .setId(id)
                .setResource("resource");
    }

}