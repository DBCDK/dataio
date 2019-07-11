/*
 * DataIO - Data IO
 *
 * Copyright (C) 2017 Dansk Bibliotekscenter a/s, Tempovej 7-11, DK-2750 Ballerup,
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

package dk.dbc.dataio.harvester.connector.ejb;

import dk.dbc.dataio.harvester.connector.TickleHarvesterServiceConnector;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.EnvironmentVariables;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;


public class TickleHarvesterConnectorBeanTest {
    @Rule
    public final EnvironmentVariables environmentVariables = new EnvironmentVariables();

    @Test(expected = NullPointerException.class)
    public void initializeConnector_environmentNotSet_throws() {
        final TickleHarvesterServiceConnectorBean bean = new TickleHarvesterServiceConnectorBean();
        bean.initializeConnector();
    }

    @Test
    public void initializeConnector() {
        environmentVariables.set("TICKLE_REPO_HARVESTER_URL", "http://test");
        final TickleHarvesterServiceConnectorBean bean = new TickleHarvesterServiceConnectorBean();
        bean.initializeConnector();

        assertThat(bean.getConnector(), not(nullValue()));
    }

    @Test
    public void getConnector() {
        final TickleHarvesterServiceConnector tickleHarvesterServiceConnector =
                mock(TickleHarvesterServiceConnector.class);
        final TickleHarvesterServiceConnectorBean bean = new TickleHarvesterServiceConnectorBean();
        bean.connector = tickleHarvesterServiceConnector;

        assertThat(bean.getConnector(), is(tickleHarvesterServiceConnector));
    }
}