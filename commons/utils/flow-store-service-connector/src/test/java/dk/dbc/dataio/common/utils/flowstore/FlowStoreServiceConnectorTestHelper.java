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

import javax.ws.rs.client.Client;

import static org.powermock.api.mockito.PowerMockito.mock;

public class FlowStoreServiceConnectorTestHelper {
    public static final Client CLIENT = mock(Client.class);
    public static final String FLOW_STORE_URL = "http://dataio/flow-store";
    public static final long ID = 1;
    public static final long NUMBER = 1234567;
    public static final long VERSION = 1;

    public static FlowStoreServiceConnector newFlowStoreServiceConnector() {
        return new FlowStoreServiceConnector(CLIENT, FLOW_STORE_URL);
    }
}
