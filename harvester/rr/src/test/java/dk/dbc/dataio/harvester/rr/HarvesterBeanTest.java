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

import dk.dbc.dataio.harvester.types.HarvesterException;
import dk.dbc.dataio.harvester.types.RRHarvesterConfig;
import dk.dbc.rawrepo.queue.ConfigurationException;
import dk.dbc.rawrepo.queue.QueueException;
import org.junit.Test;
import org.mockito.Mockito;

import javax.ejb.SessionContext;
import java.sql.SQLException;
import java.util.ConcurrentModificationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class HarvesterBeanTest {
    private SessionContext sessionContext = mock(SessionContext.class);
    private HarvestOperation harvestOperation = mock(HarvestOperation.class);
    private HarvestOperationFactoryBean harvestOperationFactory = mock(HarvestOperationFactoryBean.class);

    @Test
    public void harvest_harvestOperationCompletes_returnsNumberOfItemsHarvested()
            throws HarvesterException, ExecutionException, InterruptedException, QueueException, ConfigurationException, SQLException {
        final HarvesterBean harvesterBean = getHarvesterBean();
        final RRHarvesterConfig config = new RRHarvesterConfig(1, 1, new RRHarvesterConfig.Content());
        final int expectedNumberOfItemsHarvested = 42;
        when(harvestOperation.execute()).thenReturn(expectedNumberOfItemsHarvested);

        final Future<Integer> harvestResult = harvesterBean.harvest(config);
        assertThat("Items harvested", harvestResult.get(), is(expectedNumberOfItemsHarvested));
    }

    private HarvesterBean getHarvesterBean()
            throws QueueException, ConfigurationException, SQLException {
        final HarvesterBean harvesterBean = Mockito.spy(new HarvesterBean());
        harvesterBean.sessionContext = sessionContext;
        harvesterBean.harvestOperationFactory = harvestOperationFactory;
        when(sessionContext.getBusinessObject(HarvesterBean.class)).thenReturn(harvesterBean);
        when(harvestOperationFactory.createFor(any(RRHarvesterConfig.class))).thenReturn(harvestOperation);
        return harvesterBean;
    }
}