package dk.dbc.dataio.common.utils.flowstore;

import dk.dbc.dataio.commons.types.FlowComponent;
import dk.dbc.dataio.commons.types.FlowComponentContent;
import dk.dbc.dataio.commons.types.Sink;
import dk.dbc.dataio.commons.types.SinkContent;
import dk.dbc.dataio.commons.types.Submitter;
import dk.dbc.dataio.commons.types.SubmitterContent;
import dk.dbc.dataio.commons.types.rest.FlowStoreServiceConstants;
import dk.dbc.dataio.commons.utils.httpclient.HttpClient;
import dk.dbc.dataio.commons.utils.invariant.InvariantUtil;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.Client;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by sma on 29/04/14.
 *
 * FlowStoreServiceConnector - dataIO flow-store REST service client.
 * <p>
 * To use this class, you construct an instance, specifying a web resources client as well as
 * a base URL for the flow-store service endpoint you will be communicating with.
 * </p>
 * <p>
 * This class is thread safe, as long as the given web resources client remains thread safe.
 * </p>
 */
public class FlowStoreServiceConnector {
    private static final String URL_PATH_SEPARATOR = "/";

    private final Client httpClient;
    private final String baseUrl;

    /**
     * Class constructor
     *
     * @param httpClient web resources client
     * @param baseUrl    base URL for flow-store service endpoint
     * @throws NullPointerException     if given null-valued argument
     * @throws IllegalArgumentException if given empty-valued {@code baseUrl} argument
     */
    public FlowStoreServiceConnector(Client httpClient, String baseUrl) throws NullPointerException, IllegalArgumentException {
        this.httpClient = InvariantUtil.checkNotNullOrThrow(httpClient, "httpClient");
        this.baseUrl = InvariantUtil.checkNotNullNotEmptyOrThrow(baseUrl, "baseUrl");
    }

    // ************************************************** Sink **************************************************

    /**
     * Creates new sink defined by the sink content
     *
     * @param sinkContent sink content
     * @return Sink
     * @throws NullPointerException                                   if given null-valued argument
     * @throws ProcessingException                                    on general communication error
     * @throws FlowStoreServiceConnectorUnexpectedStatusCodeException if sink creation failed due to invalid input data
     * @throws FlowStoreServiceConnectorException                     on general failure to create sink
     */
    public Sink createSink(SinkContent sinkContent) throws NullPointerException, ProcessingException, FlowStoreServiceConnectorException {
        InvariantUtil.checkNotNullOrThrow(sinkContent, "sinkContent");
        final Response response = HttpClient.doPostWithJson(httpClient, sinkContent, baseUrl, FlowStoreServiceConstants.SINKS);
        try {
            verifyResponseStatus(Response.Status.fromStatusCode(response.getStatus()), Response.Status.CREATED);
            return readResponseEntity(response, Sink.class);
        }finally {
            response.close();
        }
    }

    /**
     * Retrieves the specified sink from the flow-store
     *
     * @param sinkId Id of the sink
     * @return the sink found
     * @throws ProcessingException on general communication error
     * @throws FlowStoreServiceConnectorException on failure to retrieve the sink
     */
    public Sink getSink(long sinkId) throws ProcessingException, FlowStoreServiceConnectorException {
        final Map<String, String> pathVariables = new HashMap<>(1);
        pathVariables.put(FlowStoreServiceConstants.SINK_ID_VARIABLE, Long.toString(sinkId));
        final String path = HttpClient.interpolatePathVariables(FlowStoreServiceConstants.SINK, pathVariables);
        final Response response = HttpClient.doGet(httpClient, baseUrl, path);

        try {
            verifyResponseStatus(Response.Status.fromStatusCode(response.getStatus()), Response.Status.OK);
            return readResponseEntity(response, Sink.class);
        } finally {
            response.close();
        }
    }

    /**
     * Retrieves all sinks from the flow-store
     *
     * @return a list containing the sinks found
     * @throws ProcessingException on general communication error
     * @throws FlowStoreServiceConnectorException on failure to retrieve the sinks
     */
    public List<Sink> findAllSinks()throws ProcessingException, FlowStoreServiceConnectorException{
        final Response response = HttpClient.doGet(httpClient, baseUrl, FlowStoreServiceConstants.SINKS);
        try {
            verifyResponseStatus(Response.Status.fromStatusCode(response.getStatus()), Response.Status.OK);
            return readResponseGenericTypeEntity(response, new GenericType<List<Sink>>() { });
        } finally {
            response.close();
        }
    }

    /**
     * Updates an existing sink from the flow-store
     *
     * @param sinkContent the new sink content
     * @param sinkId the id of the sink to update
     * @param version the current version of the sink
     * @return the updated sink
     * @throws ProcessingException on general communication error
     * @throws FlowStoreServiceConnectorException on failure to update the sink
     */
    public Sink updateSink(SinkContent sinkContent, long sinkId, long version) throws ProcessingException, FlowStoreServiceConnectorException {
        InvariantUtil.checkNotNullOrThrow(sinkContent, "sinkContent");

        final Map<String, String> pathVariables = new HashMap<>(2);
        pathVariables.put(FlowStoreServiceConstants.SINK_ID_VARIABLE, Long.toString(sinkId));
        pathVariables.put(FlowStoreServiceConstants.SINK_VERSION_VARIABLE, Long.toString(version));

        final String path = HttpClient.interpolatePathVariables(FlowStoreServiceConstants.SINK_CONTENT, pathVariables);
        final Response response = HttpClient.doPostWithJson(httpClient, sinkContent, baseUrl, path.split(URL_PATH_SEPARATOR));
        try {
            verifyResponseStatus(Response.Status.fromStatusCode(response.getStatus()), Response.Status.OK);
            return readResponseEntity(response, Sink.class);
        }finally {
            response.close();
        }
    }

    // ************************************************* Submitter *************************************************

    /**
     * Creates new submitter defined by the submitter content
     *
     * @param submitterContent submitter content
     * @return Submitter
     * @throws NullPointerException                                   if given null-valued argument
     * @throws ProcessingException                                    on general communication error
     * @throws FlowStoreServiceConnectorUnexpectedStatusCodeException if submitter creation failed due to invalid input data
     * @throws FlowStoreServiceConnectorException                     on general failure to create submitter
     */
    public Submitter createSubmitter(SubmitterContent submitterContent) throws NullPointerException, ProcessingException, FlowStoreServiceConnectorException {
        InvariantUtil.checkNotNullOrThrow(submitterContent, "submitterContent");
        final Response response = HttpClient.doPostWithJson(httpClient, submitterContent, baseUrl, FlowStoreServiceConstants.SUBMITTERS);
        try {
            verifyResponseStatus(Response.Status.fromStatusCode(response.getStatus()), Response.Status.CREATED);
            return readResponseEntity(response, Submitter.class);
        }finally {
            response.close();
        }
    }

    /**
     * Retrieves all submitters from the flow-store
     *
     * @return a list containing the submitters found
     * @throws ProcessingException on general communication error
     * @throws FlowStoreServiceConnectorException on failure to retrieve the submitters
     */
    public List<Submitter> findAllSubmitters()throws ProcessingException, FlowStoreServiceConnectorException{
        final Response response = HttpClient.doGet(httpClient, baseUrl, FlowStoreServiceConstants.SUBMITTERS);
        try {
            verifyResponseStatus(Response.Status.fromStatusCode(response.getStatus()), Response.Status.OK);
            return readResponseGenericTypeEntity(response, new GenericType<List<Submitter>>() { });
        } finally {
            response.close();
        }
    }

    // ********************************************* Flow component *********************************************

    /**
     * Creates new flow component defined by the flow component content
     *
     * @param flowComponentContent flow component content
     * @return FlowComponent
     * @throws NullPointerException                                   if given null-valued argument
     * @throws ProcessingException                                    on general communication error
     * @throws FlowStoreServiceConnectorUnexpectedStatusCodeException if flow component creation failed due to invalid input data
     * @throws FlowStoreServiceConnectorException                     on general failure to create flow component
     */
    public FlowComponent createFlowComponent(FlowComponentContent flowComponentContent) throws NullPointerException, ProcessingException, FlowStoreServiceConnectorException {
        InvariantUtil.checkNotNullOrThrow(flowComponentContent, "flowComponentContent");
        final Response response = HttpClient.doPostWithJson(httpClient, flowComponentContent, baseUrl, FlowStoreServiceConstants.FLOW_COMPONENTS);
        try {
            verifyResponseStatus(Response.Status.fromStatusCode(response.getStatus()), Response.Status.CREATED);
            return readResponseEntity(response, FlowComponent.class);
        }finally {
            response.close();
        }
    }

    /**
     * Retrieves all flow components from the flow-store
     *
     * @return a list containing the flow components found
     * @throws ProcessingException on general communication error
     * @throws FlowStoreServiceConnectorException on failure to retrieve the flow components
     */
    public List<FlowComponent> findAllFlowComponents()throws ProcessingException, FlowStoreServiceConnectorException{
        final Response response = HttpClient.doGet(httpClient, baseUrl, FlowStoreServiceConstants.FLOW_COMPONENTS);
        try {
            verifyResponseStatus(Response.Status.fromStatusCode(response.getStatus()), Response.Status.OK);
            return readResponseGenericTypeEntity(response, new GenericType<List<FlowComponent>>() { });
        } finally {
            response.close();
        }
    }

    // ******************************************** Private helper methods ********************************************

    private void verifyResponseStatus(Response.Status actualStatus, Response.Status expectedStatus) throws FlowStoreServiceConnectorUnexpectedStatusCodeException {
        if (actualStatus != expectedStatus) {
            throw new FlowStoreServiceConnectorUnexpectedStatusCodeException(
                    String.format("flow-store service returned with unexpected status code: %s", actualStatus), actualStatus.getStatusCode());
        }
    }

    private <T> T readResponseEntity(Response response, Class<T> tClass) throws FlowStoreServiceConnectorException {
        response.bufferEntity(); // must be done in order to possible avoid a timeout-exception from readEntity.
        final T entity = response.readEntity(tClass);
        if (entity == null) {
            throw new FlowStoreServiceConnectorException(
                    String.format("flow-store service returned with null-valued %s entity", tClass.getName()));
        }
        return entity;
    }

        private <T> T readResponseGenericTypeEntity(Response response, GenericType<T> tGenericType) throws FlowStoreServiceConnectorException {
        response.bufferEntity(); // must be done in order to possible avoid a timeout-exception from readEntity.
        final T entity =response.readEntity(tGenericType);
        if (entity == null) {
            throw new FlowStoreServiceConnectorException(
                    String.format("flow-store service returned with null-valued %s entity", tGenericType));
        }
        return entity;
    }

    public Client getHttpClient() {
        return httpClient;
    }

    public String getBaseUrl() {
        return baseUrl;
    }
}
