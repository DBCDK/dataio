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

import javax.ws.rs.core.Response;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;

/**
 * Base class for HTTP requests
 * @param <T> recursive request type parameter
 */
@SuppressWarnings("unchecked")
public abstract class HttpRequest<T extends HttpRequest<T>> implements Callable<Response> {
    protected final HttpClient httpClient;
    protected final Map<String, String> headers = new HashMap<>();
    protected final Map<String, Object> queryParameters = new HashMap<>();
    protected String baseUrl;
    protected String[] pathElements = new String[] {"/"};

    public HttpRequest(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    public Response execute() {
        return httpClient.execute(this);
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public T withHeader(String name, String value) {
        headers.put(name, value);
        return (T) this;
    }

    public Map<String, Object> getQueryParameters() {
        return queryParameters;
    }

    public T withQueryParameter(String name, Object value) {
        queryParameters.put(name, value);
        return (T) this;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public T withBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
        return (T) this;
    }

    public String[] getPathElements() {
        return pathElements;
    }

    public T withPathElements(String... pathElements) {
        this.pathElements = pathElements;
        return (T) this;
    }

    public HttpClient getHttpClient() {
        return httpClient;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        HttpRequest<?> that = (HttpRequest<?>) o;

        if (!headers.equals(that.headers)) {
            return false;
        }
        if (!queryParameters.equals(that.queryParameters)) {
            return false;
        }
        if (baseUrl != null ? !baseUrl.equals(that.baseUrl) : that.baseUrl != null) {
            return false;
        }
        // Probably incorrect - comparing Object[] arrays with Arrays.equals
        return Arrays.equals(pathElements, that.pathElements);
    }

    @Override
    public int hashCode() {
        int result = headers.hashCode();
        result = 31 * result + queryParameters.hashCode();
        result = 31 * result + (baseUrl != null ? baseUrl.hashCode() : 0);
        result = 31 * result + Arrays.hashCode(pathElements);
        return result;
    }
}
