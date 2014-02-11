package dk.dbc.dataio.commons.utils.httpclient;

import org.glassfish.jersey.client.ClientConfig;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
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
    private HttpClient() { }

    /**
     * @return new web resources client
     */
    public static Client newClient() {
       return ClientBuilder.newClient();
    }

    /**
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
        for (String pathElement : pathElements) {
            target = target.path(pathElement);
        }
        for (Map.Entry<String, Object> queryParameter : queryParameters.entrySet()) {
            target = target.queryParam(queryParameter.getKey(), queryParameter.getValue());
        }
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
     * HTTP POSTs given data entity to endpoint constructed using given baseurl and path elements
     *
     * @param client web resource client
     * @param data data entity
     * @param baseUrl base URL on the form http(s)://host:port/path
     * @param pathElements additional path elements to be added to base URL
     *
     * @return server response
     */
    public static Response doPost(Client client, Entity data, String baseUrl, String... pathElements) {
        WebTarget target = client.target(baseUrl);
        for (String pathElement : pathElements) {
            target = target.path(pathElement);
        }
        return target.request().post(data);
    }

    /**
     * HTTP POSTs given data as application/json to endpoint constructed using given baseurl and path elements
     *
     * @param client web resource client
     * @param data JSON data
     * @param baseUrl base URL on the form http(s)://host:port/path
     * @param pathElements additional path elements to be added to base URL
     *
     * @return server response
     */
    public static Response doPostWithJson(Client client, String data, String baseUrl, String... pathElements) {
        return doPost(client, Entity.entity(data, MediaType.APPLICATION_JSON), baseUrl, pathElements);
    }
    public static <T> Response doPostWithJson(Client client, T data, String baseUrl, String... pathElements) {
        return doPost(client, Entity.entity(data, MediaType.APPLICATION_JSON), baseUrl, pathElements);
    }

    /**
     * HTTP POSTs given data as application/x-www-form-urlencoded to endpoint constructed using given baseurl and path elements
     *
     * @param client web resource client
     * @param formData form data
     * @param baseUrl base URL on the form http(s)://host:port/path
     * @param pathElements additional path elements to be added to base URL
     *
     * @return server response
     */
    public static Response doPostWithFormData(Client client, MultivaluedMap<String, String> formData, String baseUrl, String... pathElements) {
        return doPost(client, Entity.form(formData), baseUrl, pathElements);
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
        WebTarget target = client.target(baseUrl);
        for (String pathElement : pathElements) {
            target = target.path(pathElement);
        }
        return target.request().delete();
    }

    /**
     * Interpolates embedded path variables on the form {key} with value
     * found by doing values.get(key), if any.
     *
     * @param path path containing path variables on the form {key}, can be null
     * @param values key-to-value mappings, can be null
     *
     * @return path with values interpolated
     */
    public static String interpolatePathValues(String path, Map<String, String> values) {
        String interpolatedPath = path;
        if (interpolatedPath != null && values != null) {
            for (Map.Entry<String, String> entry : values.entrySet()) {
                interpolatedPath = interpolatedPath.replaceAll(String.format("\\{%s\\}", entry.getKey()), entry.getValue());
            }
        }
        return interpolatedPath;
    }
}
