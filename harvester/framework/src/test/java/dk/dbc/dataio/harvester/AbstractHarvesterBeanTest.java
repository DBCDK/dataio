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

package dk.dbc.dataio.harvester;

import dk.dbc.dataio.harvester.types.CoRepoHarvesterConfig;
import dk.dbc.dataio.harvester.types.HarvesterException;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class AbstractHarvesterBeanTest {
    @Test
    public void harvest_returnsResultOfExecuteForMethod() throws HarvesterException, ExecutionException, InterruptedException {
        final CoRepoHarvesterConfig config = new CoRepoHarvesterConfig(1, 1, new CoRepoHarvesterConfig.Content());

        final AbstractHarvesterBeanImpl harvesterBean = new AbstractHarvesterBeanImpl();
        final Future<Integer> harvestResult = harvesterBean.harvest(config);
        assertThat("Items harvested", harvestResult.get(), is(42));
    }

    public static class AbstractHarvesterBeanImpl extends AbstractHarvesterBean<AbstractHarvesterBeanImpl, CoRepoHarvesterConfig> {
        @Override
        public int executeFor(CoRepoHarvesterConfig config) throws HarvesterException {
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
