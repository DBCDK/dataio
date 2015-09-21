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

package dk.dbc.dataio.common.utils.flowstore;

import static dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnectorTestHelper.CLIENT;
import static dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnectorTestHelper.FLOW_STORE_URL;
import static dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnectorTestHelper.newFlowStoreServiceConnector;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import org.junit.Test;

public class FlowStoreServiceConnector_Constructor_Test {

    public FlowStoreServiceConnector_Constructor_Test() {
        super();
    }

    @Test(expected = NullPointerException.class)
    public void constructor_httpClientArgIsNull_throws() {
        new FlowStoreServiceConnector(null, FLOW_STORE_URL);
    }

    @Test(expected = NullPointerException.class)
    public void constructor_baseUrlArgIsNull_throws() {
        new FlowStoreServiceConnector(CLIENT, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructor_baseUrlArgIsEmpty_throws() {
        new FlowStoreServiceConnector(CLIENT, "");
    }

    @Test
    public void constructor_allArgsAreValid_returnsNewInstance() {
        final FlowStoreServiceConnector instance = newFlowStoreServiceConnector();
        assertThat(instance, is(notNullValue()));
        assertThat(instance.getHttpClient(), is(CLIENT));
        assertThat(instance.getBaseUrl(), is(FLOW_STORE_URL));
    }
}
