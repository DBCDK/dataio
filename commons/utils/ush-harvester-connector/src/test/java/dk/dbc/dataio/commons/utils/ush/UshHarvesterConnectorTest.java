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

package dk.dbc.dataio.commons.utils.ush;

import dk.dbc.dataio.commons.utils.httpclient.HttpClient;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import javax.ws.rs.client.Client;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.mockStatic;

@RunWith(PowerMockRunner.class)
@PrepareForTest({
        HttpClient.class
})
public class UshHarvesterConnectorTest {
    private static final Client CLIENT = mock(Client.class);
    private static final String USH_HARVESTER_URL = "http://dataio/ush/harvester";

    @Before
    public void setup() throws Exception {
        mockStatic(HttpClient.class);
    }

    @Test(expected = NullPointerException.class)
    public void constructor_httpClientArgIsNull_throws() {
        new UshHarvesterConnector(null, USH_HARVESTER_URL);
    }

    @Test(expected = NullPointerException.class)
    public void constructor_baseUrlArgIsNull_throws() {
        new UshHarvesterConnector(CLIENT, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructor_baseUrlArgIsEmpty_throws() {
        new UshHarvesterConnector(CLIENT, "");
    }

    @Test
    public void constructor_allArgsAreValid_returnsNewInstance() {
        final UshHarvesterConnector instance = newUshHarvesterConnector();
        assertThat(instance, is(notNullValue()));
        assertThat(instance.getHttpClient(), is(CLIENT));
        assertThat(instance.getBaseUrl(), is(USH_HARVESTER_URL));
    }

    /*
     * Private methods
     */
    private static UshHarvesterConnector newUshHarvesterConnector() {
        try {
            return new UshHarvesterConnector(CLIENT, USH_HARVESTER_URL);
        } catch (Exception e) {
            fail("Caught unexpected exception " + e.getClass().getCanonicalName() + ": " + e.getMessage());
            throw new IllegalStateException(e);
        }
    }
}
