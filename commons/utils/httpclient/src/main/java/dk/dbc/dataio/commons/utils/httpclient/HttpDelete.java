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

package dk.dbc.dataio.commons.utils.httpclient;

import javax.ws.rs.client.Client;
import javax.ws.rs.core.Response;
import java.util.Arrays;

/**
 * HTTP DELETE request representation
 *
 * <p>
 * Example:
 * <pre>
 * {@code
 * final HttpDelete httpDelete = new HttpDelete(httpClient)
 *          .withBaseUrl("http://localhost:8080")
 *          .withPathElements(new String[] {"path", "to", "resource"});
 * }
 * </pre>
 * </p>
 */
public class HttpDelete extends HttpRequest<HttpDelete> {
    public HttpDelete(Client httpClient) {
        super(httpClient);
    }

    @Override
    public Response call() throws Exception {
        return HttpClient.doDelete(httpClient, headers, baseUrl, pathElements);
    }

    @Override
    public String toString() {
        return "HttpDelete{" +
                "headers=" + headers +
                ", queryParameters=" + queryParameters +
                ", baseUrl='" + baseUrl + '\'' +
                ", pathElements=" + Arrays.toString(pathElements) +
                '}';
    }
}
