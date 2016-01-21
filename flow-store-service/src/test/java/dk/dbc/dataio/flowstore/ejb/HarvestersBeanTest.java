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

package dk.dbc.dataio.flowstore.ejb;

import dk.dbc.dataio.commons.types.jndi.JndiConstants;
import dk.dbc.dataio.commons.utils.test.jndi.InMemoryInitialContextFactory;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.naming.Context;
import javax.naming.NamingException;
import javax.ws.rs.core.Response;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;


public class HarvestersBeanTest {

    @BeforeClass
    public static void setInitialContextFactory() {
        // sets up the InMemoryInitialContextFactory as default factory.
        System.setProperty(Context.INITIAL_CONTEXT_FACTORY, InMemoryInitialContextFactory.class.getName());
    }

    @After
    public void clearInitialContext() {
        InMemoryInitialContextFactory.clear();
    }

//----------------------------------------------------------------------------------------------------------------------

    @Test(expected = NamingException.class)
    public void getHarvesterRrConfigs_resourceNotAvailable_throws() throws NamingException {
        final HarvestersBean harvestersBean = new HarvestersBean();

        // Test subject under test
        harvestersBean.getHarvesterRrConfigs();  // No resource available => NamingException
    }

    @Test
    public void getHarvesterRrConfigs_resourceAvailable_ok() throws NamingException {
        final String HARVESTER_CONFIG = "Harvester Config";
        final HarvestersBean harvestersBean = new HarvestersBean();

        // Test preparation
        InMemoryInitialContextFactory.bind(JndiConstants.CONFIG_RESOURCE_HARVESTER_RR, HARVESTER_CONFIG);

        // Test subject under test
        Response result = harvestersBean.getHarvesterRrConfigs();

        // Test validation
        assertThat(result.getEntity(), is(HARVESTER_CONFIG));
    }


}