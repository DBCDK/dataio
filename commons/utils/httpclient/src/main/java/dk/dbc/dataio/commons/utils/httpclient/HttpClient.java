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

import dk.dbc.dataio.commons.utils.invariant.InvariantUtil;
import org.glassfish.jersey.client.ClientConfig;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import java.util.HashMap;
import java.util.Map;

/**
 * This utility class provides convenience methods for accessing web resources via HTTP
 */
public class HttpClient {
    protected final Client client;

    public static HttpClient create(Client client) throws NullPointerException {
        return new HttpClient(client);
    }

    HttpClient(Client client) throws NullPointerException {
        this.client = InvariantUtil.checkNotNullOrThrow(client, "client");
    }

    public Response execute(HttpRequest<? extends HttpRequest> request) {
        try {
            return request.call();
        } catch (Exception e) {
            if (!(e instanceof ProcessingException)) {
                throw new ProcessingException(e);
            } else {
                throw (ProcessingException) e;
            }
        }
    }

    public Client getClient() {
        return client;
    }

    // Old util methods - will be refactored in the near future

    private final static Map<String, String> NO_HEADERS = null;

    private static void setHeadersOnRequest(Map<String, String> headers, Invocation.Builder request) {
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            request.header(entry.getKey(), entry.getValue());
        }
    }

    private static WebTarget setPathParametersOnWebTarget(WebTarget target, String[] pathElements) {
        for (String pathElement : pathElements) {
            target = target.path(pathElement);
        }
        return target;
    }

    private static WebTarget setQueryParametersOnWebTarget(Map<String, Object> queryParameters, WebTarget target) {
        for (Map.Entry<String, Object> queryParameter : queryParameters.entrySet()) {
            target = target.queryParam(queryParameter.getKey(), queryParameter.getValue());
        }
        return target;
    }

    private static boolean headerExists(Map<String, String> headers) {
        return headers != NO_HEADERS;
    }

    /**
     * @return new web resources client
     */
    public static Client newClient() {
       return ClientBuilder.newClient();
    }


    /**
     * @param config the client config
     * @return new web resources client with given configuration
     */
    public static Client newClient(ClientConfig config) {
       return ClientBuilder.newClient(config);
    }

    /**
     * Closes given client instance thereby releasing all resources held
     *
     * @param client web resource client (can be null)
     */
    public static void closeClient(Client client) {
        if (client != null) {
            client.close();
        }
    }

    /**
     * Issues HTTP GET request to endpoint constructed using given baseurl and path elements
     *
     * @param client web resource client
     * @param queryParameters query parameters to be added to request
     * @param baseUrl base URL on the form http(s)://host:port/path
     * @param pathElements additional path elements to be added to base URL
     *
     * @return server response
     */
    public static Response doGet(Client client, Map<String, Object> queryParameters, String baseUrl, String... pathElements)  {

        WebTarget target = client.target(baseUrl);

        target = setPathParametersOnWebTarget(target, pathElements);

        target = setQueryParametersOnWebTarget(queryParameters, target);

        return target.request().get();
    }


    /**
     * Issues HTTP GET request to endpoint constructed using given baseurl and path elements
     *
     * @param client web resource client
     * @param baseUrl base URL on the form http(s)://host:port/path
     * @param pathElements additional path elements to be added to base URL
     *
     * @return server response
     */
    public static Response doGet(Client client, String baseUrl, String... pathElements)  {
        return doGet(client, new HashMap<String, Object>(), baseUrl, pathElements);
    }


    /**
     * HTTP POSTs given data entity to endpoint constructed using given queryParameters, headers, baseurl and path elements
     *
     * @param client web resource client
     * @param queryParameters the query parameters
     * @param headers HTTP headers
     * @param data data entity
     * @param baseUrl base URL on the form http(s)://host:port/path
     * @param pathElements additional path elements to be added to base URL
     *
     * @return server response
     */
    public static Response doPost(Client client, Map<String, Object> queryParameters, Map<String, String> headers, Entity data, String baseUrl, String... pathElements) {

        WebTarget target = client.target(baseUrl);

        target = setPathParametersOnWebTarget(target, pathElements);

        target = setQueryParametersOnWebTarget(queryParameters, target);

        Invocation.Builder request = target.request();

        if (headerExists(headers)) {
            setHeadersOnRequest(headers, request);
        }

        return request.post(data);
    }

    /**
     * HTTP POSTs given data entity to endpoint constructed using given headers, baseurl and path elements
     *
     * @param client web resource client
     * @param headers HTTP headers
     * @param data data entity
     * @param baseUrl base URL on the form http(s)://host:port/path
     * @param pathElements additional path elements to be added to base URL
     *
     * @return server response
     */
    public static Response doPost(Client client, Map<String, String> headers, Entity data, String baseUrl, String... pathElements) {

        WebTarget target = client.target(baseUrl);

        target = setPathParametersOnWebTarget(target, pathElements);

        Invocation.Builder request = target.request();

        if (headerExists(headers)) {
            setHeadersOnRequest(headers, request);
        }

        return request.post(data);
    }

    public static Response doPost(Client client, Entity data, String baseUrl, String... pathElements) {
        return doPost(client, NO_HEADERS, data, baseUrl, pathElements);
    }

    /**
     * HTTP POSTs given data as application/json to endpoint constructed using given headers, baseurl and path elements
     *
     * @param client web resource client
     * @param headers HTTP headers
     * @param data JSON data
     * @param baseUrl base URL on the form http(s)://host:port/path
     * @param pathElements additional path elements to be added to base URL
     *
     * @return server response
     */
    public static Response doPostWithJson(Client client, Map<String, String> headers, String data, String baseUrl, String... pathElements) {
        return doPost(client, headers, Entity.entity(data, MediaType.APPLICATION_JSON), baseUrl, pathElements);
    }
    public static <T> Response doPostWithJson(Client client, Map<String, String> headers, T data, String baseUrl, String... pathElements) {
        return doPost(client, headers, Entity.entity(data, MediaType.APPLICATION_JSON), baseUrl, pathElements);
    }
    public static Response doPostWithJson(Client client, String data, String baseUrl, String... pathElements) {
        return doPost(client, NO_HEADERS, Entity.entity(data, MediaType.APPLICATION_JSON), baseUrl, pathElements);
    }
    public static <T> Response doPostWithJson(Client client, T data, String baseUrl, String... pathElements) {
        return doPost(client, NO_HEADERS,  Entity.entity(data, MediaType.APPLICATION_JSON), baseUrl, pathElements);
    }
    public static <T> Response doPostWithJson(Client client, Map<String, Object> queryParameters, Map<String, String> headers, T data, String baseUrl, String... pathElements) {
        return doPost(client, queryParameters, headers, Entity.entity(data, MediaType.APPLICATION_JSON), baseUrl, pathElements);
    }

    /**
     * HTTP POSTs given data as application/x-www-form-urlencoded to endpoint constructed using given baseurl and path elements
     *
     * @param client web resource client
     * @param headers HTTP headers
     * @param formData form data
     * @param baseUrl base URL on the form http(s)://host:port/path
     * @param pathElements additional path elements to be added to base URL
     *
     * @return server response
     */
    public static Response doPostWithFormData(Client client, Map<String, String> headers, MultivaluedMap<String, String> formData, String baseUrl, String... pathElements) {
        return doPost(client, headers, Entity.form(formData), baseUrl, pathElements);
    }
    public static Response doPostWithFormData(Client client, MultivaluedMap<String, String> formData, String baseUrl, String... pathElements) {
        return doPost(client, NO_HEADERS, Entity.form(formData), baseUrl, pathElements);
    }

    /**
     * Issues HTTP DELETE request to endpoint constructed using given baseurl and path elements
     *
     * @param client web resource client
     * @param baseUrl base URL on the form http(s)://host:port/path
     * @param pathElements additional path elements to be added to base URL
     *
     * @return server response
     */
    public static Response doDelete(Client client, String baseUrl, String... pathElements) {
        return doDelete(client, NO_HEADERS, baseUrl, pathElements);
    }

    /**
     * Issues HTTP DELETE request to endpoint constructed using given baseurl and path elements
     *
     * @param client web resource client
     * @param headers HTTP headers
     * @param baseUrl base URL on the form http(s)://host:port/path
     * @param pathElements additional path elements to be added to base URL
     *
     * @return server response
     */
    public static Response doDelete(Client client, Map<String, String> headers, String baseUrl, String... pathElements) {

        WebTarget target = client.target(baseUrl);

        target = setPathParametersOnWebTarget(target, pathElements);

        Invocation.Builder request = target.request();

        if (headerExists(headers)) {
            setHeadersOnRequest(headers, request);
        }

        return request.delete();
    }
}
