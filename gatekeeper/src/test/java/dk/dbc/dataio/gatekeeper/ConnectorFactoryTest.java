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

package dk.dbc.dataio.gatekeeper;

import org.junit.Test;

public class ConnectorFactoryTest {
    @Test(expected = NullPointerException.class)
    public void constructor_fileStoreServiceEndpointArgIsNull_throws() {
        new ConnectorFactory(null, "jobStoreServiceEndpoint", "flowStoreServiceEndpoint");
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructor_fileStoreServiceEndpointArgIsEmpty_throws() {
        new ConnectorFactory(" ", "jobStoreServiceEndpoint", "flowStoreServiceEndpoint");
    }

    @Test(expected = NullPointerException.class)
    public void constructor_jobStoreServiceEndpointArgIsNull_throws() {
        new ConnectorFactory("fileStoreServiceEndpoint", null, "flowStoreServiceEndpoint");
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructor_jobStoreServiceEndpointArgIsEmpty_throws() {
        new ConnectorFactory("fileStoreServiceEndpoint", " ", "flowStoreServiceEndpoint");
    }

    @Test(expected = NullPointerException.class)
    public void constructor_flowStoreServiceEndpointArgIsNull_throws() {
        new ConnectorFactory("fileStoreServiceEndpoint", "jobStoreServiceEndpoint", null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructor_flowStoreServiceEndpointArgIsEmpty_throws() {
        new ConnectorFactory("fileStoreServiceEndpoint", "jobStoreServiceEndpoint", " ");
    }

}