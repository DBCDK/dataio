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

package dk.dbc.dataio.urlresolver.service.connector;

import dk.dbc.httpclient.HttpClient;
import dk.dbc.httpclient.PathBuilder;
import dk.dbc.dataio.commons.utils.test.rest.MockedResponse;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import javax.ws.rs.client.Client;
import javax.ws.rs.core.Response;
import java.util.HashMap;
import java.util.Map;

import static dk.dbc.commons.testutil.Assert.assertThat;
import static dk.dbc.commons.testutil.Assert.isThrowing;
import static org.junit.Assert.assertThat;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.fail;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest({
        HttpClient.class
})
public class UrlResolverServiceConnectorTest {

    private static final Client CLIENT = mock(Client.class);
    private static final String GUI_URL = "http://dataio.dbc.dk";

    @Before
    public void setup() throws Exception {
        mockStatic(HttpClient.class);
    }

    @Test()
    public void constructor_httpClientArgIsNull_throws() {
        assertThat(() -> new UrlResolverServiceConnector(null, GUI_URL), isThrowing(NullPointerException.class));
    }

    @Test
    public void constructor_baseUrlArgIsNull_throws() {
        assertThat(() -> new UrlResolverServiceConnector(CLIENT, null), isThrowing(NullPointerException.class));
    }

    @Test
    public void constructor_baseUrlArgIsEmpty_throws() {
        assertThat(() -> new UrlResolverServiceConnector(CLIENT, ""), isThrowing(IllegalArgumentException.class));
    }

    @Test
    public void constructor_allArgsAreValid_returnsNewInstance() {
        final UrlResolverServiceConnector instance = newUrlResolverServiceConnector();
        Assert.assertThat(instance, is(notNullValue()));
        Assert.assertThat(instance.getHttpClient(), is(CLIENT));
        Assert.assertThat(instance.getBaseUrl(), is(GUI_URL));
    }

    @Test
    public void getUrls_returns() throws UrlResolverServiceConnectorException {
        Map<String, String> urls = new HashMap<>();
        urls.put("flowStore", "flowStoreUrl");
        urls.put("jobStore", "jobStoreUrl");
        final PathBuilder path = new PathBuilder("urls");
        when(HttpClient.doGet(CLIENT, GUI_URL, path.build())).thenReturn(new MockedResponse<>(Response.Status.OK.getStatusCode(), urls));
        final UrlResolverServiceConnector instance = newUrlResolverServiceConnector();

        Map<String, String> retrievedUrls = instance.getUrls();
        assertThat(retrievedUrls, is(urls));
    }


    private static UrlResolverServiceConnector newUrlResolverServiceConnector() {
        try {
            return new UrlResolverServiceConnector(CLIENT, GUI_URL);
        } catch (Exception e) {
            fail("Caught unexpected exception " + e.getClass().getCanonicalName() + ": " + e.getMessage());
            throw new IllegalStateException(e);
        }
    }

}
