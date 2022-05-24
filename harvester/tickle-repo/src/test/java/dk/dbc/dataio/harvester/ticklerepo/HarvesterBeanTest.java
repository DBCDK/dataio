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
import org.junit.Test;
import org.mockito.Mockito;

import javax.ejb.SessionContext;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class HarvesterBeanTest {
    private SessionContext sessionContext = mock(SessionContext.class);
    private HarvestOperation harvestOperation = mock(HarvestOperation.class);
    private HarvestOperationFactoryBean harvestOperationFactory = mock(HarvestOperationFactoryBean.class);

    @Test
    public void harvest_harvestOperationCompletes_returnsNumberOfItemsHarvested() throws HarvesterException, ExecutionException, InterruptedException {
        final HarvesterBean harvesterBean = getHarvesterBean();
        final TickleRepoHarvesterConfig config = createConfig();
        final int expectedNumberOfItemsHarvested = 42;
        when(harvestOperation.execute()).thenReturn(expectedNumberOfItemsHarvested);

        final Future<Integer> harvestResult = harvesterBean.harvest(config);
        assertThat("Items harvested", harvestResult.get(), is(expectedNumberOfItemsHarvested));
    }

    private HarvesterBean getHarvesterBean() {
        final HarvesterBean harvesterBean = Mockito.spy(new HarvesterBean());
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
