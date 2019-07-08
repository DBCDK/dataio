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

package dk.dbc.dataio.common.utils.flowstore.ejb;

import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnector;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.EnvironmentVariables;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.mockito.Mockito.mock;

public class FlowStoreServiceConnectorBeanTest {
    @Rule
    public final EnvironmentVariables environmentVariables = new EnvironmentVariables();

    @Test(expected = NullPointerException.class)
    public void initializeConnector_environmentNotSet_throws() {
        final FlowStoreServiceConnectorBean jobStoreServiceConnectorBean = newFlowStoreServiceConnectorBean();
        jobStoreServiceConnectorBean.initializeConnector();
    }

    @Test
    public void initializeConnector() {
        environmentVariables.set("FLOWSTORE_URL", "http://test");
        final FlowStoreServiceConnectorBean flowStoreServiceConnectorBean =
                newFlowStoreServiceConnectorBean();
        flowStoreServiceConnectorBean.initializeConnector();

        assertThat(flowStoreServiceConnectorBean.getConnector(), not(nullValue()));
    }

    @Test
    public void getConnector() {
        final FlowStoreServiceConnector flowStoreServiceConnector =
                mock(FlowStoreServiceConnector.class);
        final FlowStoreServiceConnectorBean flowStoreServiceConnectorBean =
                newFlowStoreServiceConnectorBean();
        flowStoreServiceConnectorBean.flowStoreServiceConnector = flowStoreServiceConnector;

        assertThat(flowStoreServiceConnectorBean.getConnector(),
                is(flowStoreServiceConnector));
    }

    private FlowStoreServiceConnectorBean newFlowStoreServiceConnectorBean() {
        return new FlowStoreServiceConnectorBean();
    }
}
