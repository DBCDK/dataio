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

package dk.dbc.httpclient;

import net.jodah.failsafe.RetryPolicy;
import org.junit.Test;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.Client;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

import static dk.dbc.commons.testutil.Assert.assertThat;
import static dk.dbc.commons.testutil.Assert.isThrowing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class FailSafeHttpClientTest {
    @Test
    public void retriesIfPolicyDictatesIt() {
        final int numberOfRetries = 3;
        final String baseurl = "http://no.such.host";
        final Client client = mock(Client.class);
        when(client.target(baseurl)).thenThrow(new ProcessingException("err"));

        final RetryPolicy retryPolicy = new RetryPolicy()
                .retryOn(Collections.singletonList(ProcessingException.class))
                .withDelay(1, TimeUnit.MILLISECONDS)
                .withMaxRetries(numberOfRetries);

        final FailSafeHttpClient failSafeHttpClient = FailSafeHttpClient.create(client, retryPolicy);
        final HttpGet httpGet = new HttpGet(failSafeHttpClient)
                .withBaseUrl(baseurl);

        assertThat(() -> failSafeHttpClient.execute(httpGet), isThrowing(ProcessingException.class));

        verify(client, times(numberOfRetries + 1)).target(baseurl);
    }
}