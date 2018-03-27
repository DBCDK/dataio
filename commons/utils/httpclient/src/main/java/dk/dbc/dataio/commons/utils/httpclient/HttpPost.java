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

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Arrays;

/**
 * HTTP POST request representation
 *
 * <p>
 * Example:
 * <pre>
 * {@code
 * final HttpPost httpPost = new HttpPost(client)
 *          .withBaseUrl("http://localhost:8080")
 *          .withPathElements("path", "to", "resource")
 *          .withHeader("User-Agent", "AwesomeJavaClient")
 *          .withData(object, MediaType.APPLICATION_JSON);
 * }
 * </pre>
 */
public class HttpPost extends HttpRequest<HttpPost> {
    private Entity entity;

    public HttpPost(HttpClient httpClient) {
        super(httpClient);
    }

    public Entity getEntity() {
        return entity;
    }

    public <T> HttpPost withData(T data, String mediaType) {
        this.entity = Entity.entity(data, mediaType);
        return this;
    }

    public <T> HttpPost withJsonData(T data) {
        return withData(data, MediaType.APPLICATION_JSON);
    }

    @Override
    public Response call() {
        return HttpClient.doPost(this);
    }

    @Override
    public String toString() {
        return "HttpPost{" +
                "headers=" + headers +
                ", queryParameters=" + queryParameters +
                ", baseUrl='" + baseUrl + '\'' +
                ", pathElements=" + Arrays.toString(pathElements) +
                ", entity=" + entity +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }

        HttpPost httpPost = (HttpPost) o;

        return entity != null ? entity.equals(httpPost.entity) : httpPost.entity == null;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (entity != null ? entity.hashCode() : 0);
        return result;
    }
}
