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

import dk.dbc.dataio.commons.utils.invariant.InvariantUtil;
import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.RetryPolicy;

import javax.ws.rs.client.Client;
import javax.ws.rs.core.Response;

/**
 * Class for executing HTTP requests in a fail safe manner with automatic retry functionality
 *
 * <p>
 * Example:
 * <pre>
 * {@code
 *
 * final Client client = HttpClient.newClient();
 * final RetryPolicy retryPolicy = new RetryPolicy()
 *          .retryOn(Collections.singletonList(ProcessingException.class))
 *          .retryIf((Response response) -> response.getStatus() == 404 || response.getStatus() == 500)
 *          .withDelay(1, TimeUnit.SECONDS)
 *          .withMaxRetries(3);
 *
 * final FailSafeHttpClient failSafeHttpClient = FailSafeHttpClient.create(client, retryPolicy);
 * final HttpGet httpGet = failSafeHttpClient.createHttpGet()
 *          .withBaseUrl("http://localhost:8080")
 *          .withPathElements("path", "to", "resource");
 *
 * failSafeHttpClient.execute(httpGet);
 *
 * }
 * </pre>
 */
public class FailSafeHttpClient extends HttpClient {
    private final RetryPolicy retryPolicy;

    public static FailSafeHttpClient create(Client httpClient, RetryPolicy retryPolicy) throws NullPointerException {
        return new FailSafeHttpClient(httpClient, retryPolicy);
    }

    private FailSafeHttpClient(Client client, RetryPolicy retryPolicy) throws NullPointerException {
        super(client);
        this.retryPolicy = InvariantUtil.checkNotNullOrThrow(retryPolicy, "retryPolicy");
    }

    @Override
    public Response execute(HttpRequest<? extends HttpRequest> request) {
        return Failsafe.with(retryPolicy)
                // To ensure no leaking connections
                .onRetry((response, failure) -> {
                    if (response != null)
                        ((Response) response).close();
                })
                .get(request);
    }
}
