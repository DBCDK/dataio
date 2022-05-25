package dk.dbc.dataio.common.utils.flowstore;

import dk.dbc.dataio.commons.time.StopWatch;
import dk.dbc.dataio.commons.types.Flow;
import dk.dbc.dataio.commons.types.FlowBinder;
import dk.dbc.dataio.commons.types.FlowBinderContent;
import dk.dbc.dataio.commons.types.FlowBinderIdent;
import dk.dbc.dataio.commons.types.FlowComponent;
import dk.dbc.dataio.commons.types.FlowComponentContent;
import dk.dbc.dataio.commons.types.FlowComponentView;
import dk.dbc.dataio.commons.types.FlowContent;
import dk.dbc.dataio.commons.types.FlowStoreError;
import dk.dbc.dataio.commons.types.FlowView;
import dk.dbc.dataio.commons.types.GatekeeperDestination;
import dk.dbc.dataio.commons.types.Sink;
import dk.dbc.dataio.commons.types.SinkContent;
import dk.dbc.dataio.commons.types.Submitter;
import dk.dbc.dataio.commons.types.SubmitterContent;
import dk.dbc.dataio.commons.types.rest.FlowBinderResolveQuery;
import dk.dbc.dataio.commons.types.rest.FlowStoreServiceConstants;
import dk.dbc.dataio.harvester.types.HarvesterConfig;
import dk.dbc.httpclient.FailSafeHttpClient;
import dk.dbc.httpclient.HttpClient;
import dk.dbc.httpclient.HttpDelete;
import dk.dbc.httpclient.HttpGet;
import dk.dbc.httpclient.HttpPost;
import dk.dbc.httpclient.PathBuilder;
import dk.dbc.invariant.InvariantUtil;
import net.jodah.failsafe.RetryPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.Client;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static javax.ws.rs.core.Response.Status.NO_CONTENT;


/**
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
    private static final Logger log = LoggerFactory.getLogger(FlowStoreServiceConnector.class);

    private static final RetryPolicy<Response> RETRY_POLICY = new RetryPolicy<Response>()
            .handle(ProcessingException.class)
            .handleResultIf(response ->
                    response.getStatus() == 404
                            || response.getStatus() == 500
                            || response.getStatus() == 502)
            .withDelay(Duration.ofSeconds(10))
            .withMaxRetries(6);

    private final FailSafeHttpClient failSafeHttpClient;
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
        this(FailSafeHttpClient.create(httpClient, RETRY_POLICY), baseUrl);
    }

    public FlowStoreServiceConnector(FailSafeHttpClient failSafeHttpClient, String baseUrl) {
        this.failSafeHttpClient = InvariantUtil.checkNotNullOrThrow(failSafeHttpClient, "failSafeHttpClient");
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
        final StopWatch stopWatch = new StopWatch();
        try {
            log.trace("FlowStoreServiceConnector: createSink(\"{}\");",
                    InvariantUtil.checkNotNullOrThrow(sinkContent, "sinkContent").getName());
            try (Response response = new HttpPost(failSafeHttpClient)
                    .withBaseUrl(baseUrl)
                    .withPathElements(FlowStoreServiceConstants.SINKS)
                    .withJsonData(sinkContent)
                    .execute()) {
                verifyResponseStatus(response, Response.Status.CREATED);
                return readResponseEntity(response, Sink.class);
            }
        } finally {
            log.debug("FlowStoreServiceConnector: createSink took {} milliseconds", stopWatch.getElapsedTime());
        }
    }

    /**
     * Retrieves the specified sink from the flow-store
     *
     * @param sinkId Id of the sink
     * @return the sink found
     * @throws ProcessingException                on general communication error
     * @throws FlowStoreServiceConnectorException on failure to retrieve the sink
     */
    public Sink getSink(long sinkId) throws ProcessingException, FlowStoreServiceConnectorException {
        final StopWatch stopWatch = new StopWatch();
        try {
            log.trace("FlowStoreServiceConnector: getSink({});", sinkId);
            final PathBuilder path = new PathBuilder(FlowStoreServiceConstants.SINK)
                    .bind(FlowStoreServiceConstants.ID_VARIABLE, sinkId);
            try (Response response = new HttpGet(failSafeHttpClient)
                    .withBaseUrl(baseUrl)
                    .withPathElements(path.build())
                    .execute()) {
                verifyResponseStatus(response, Response.Status.OK);
                return readResponseEntity(response, Sink.class);
            }
        } finally {
            log.debug("FlowStoreServiceConnector: getSink took {} milliseconds", stopWatch.getElapsedTime());
        }
    }

    /**
     * Retrieves all sinks from the flow-store
     *
     * @return a list containing the sinks found
     * @throws ProcessingException                on general communication error
     * @throws FlowStoreServiceConnectorException on failure to retrieve the sinks
     */
    public List<Sink> findAllSinks() throws ProcessingException, FlowStoreServiceConnectorException {
        final StopWatch stopWatch = new StopWatch();
        try {
            log.trace("FlowStoreServiceConnector: findAllSinks();");
            try (Response response = new HttpGet(failSafeHttpClient)
                    .withBaseUrl(baseUrl)
                    .withPathElements(FlowStoreServiceConstants.SINKS)
                    .execute()) {
                verifyResponseStatus(response, Response.Status.OK);
                return readResponseGenericTypeEntity(response, new GenericType<List<Sink>>() {
                });
            }
        } finally {
            log.debug("FlowStoreServiceConnector: findAllSinks took {} milliseconds", stopWatch.getElapsedTime());
        }
    }

    /**
     * Updates an existing sink from the flow-store
     *
     * @param sinkContent the new sink content
     * @param sinkId      the id of the sink to update
     * @param version     the current version of the sink
     * @return the updated sink
     * @throws ProcessingException                on general communication error
     * @throws FlowStoreServiceConnectorException on failure to update the sink
     */
    public Sink updateSink(SinkContent sinkContent, long sinkId, long version) throws ProcessingException, FlowStoreServiceConnectorException {
        final StopWatch stopWatch = new StopWatch();
        try {
            log.trace("FlowStoreServiceConnector: updateSink({}, {});", sinkId, version);
            InvariantUtil.checkNotNullOrThrow(sinkContent, "sinkContent");
            final PathBuilder path = new PathBuilder(FlowStoreServiceConstants.SINK_CONTENT)
                    .bind(FlowStoreServiceConstants.ID_VARIABLE, Long.toString(sinkId));

            try (Response response = new HttpPost(failSafeHttpClient)
                    .withBaseUrl(baseUrl)
                    .withPathElements(path.build())
                    .withHeader(FlowStoreServiceConstants.IF_MATCH_HEADER, Long.toString(version))
                    .withJsonData(sinkContent)
                    .execute()) {
                verifyResponseStatus(response, Response.Status.OK);
                return readResponseEntity(response, Sink.class);
            }
        } finally {
            log.debug("FlowStoreServiceConnector: updateSink took {} milliseconds", stopWatch.getElapsedTime());
        }
    }

    /**
     * Deletes an existing sink from the flow-store
     *
     * @param sinkId,  the database related ID
     * @param version, the current JPA version of the sink - Optimistic Locking
     * @throws ProcessingException                                    on general communication error
     * @throws FlowStoreServiceConnectorUnexpectedStatusCodeException if an unexpected HTTP code is returned
     */
    public void deleteSink(long sinkId, long version) throws ProcessingException, FlowStoreServiceConnectorUnexpectedStatusCodeException {
        final StopWatch stopWatch = new StopWatch();
        try {
            log.trace("FlowStoreServiceConnector: deleteSink({})", sinkId);
            final PathBuilder pathBuilder = new PathBuilder(FlowStoreServiceConstants.SINK)
                    .bind(FlowStoreServiceConstants.ID_VARIABLE, Long.toString(sinkId));

            try (Response response = new HttpDelete(failSafeHttpClient)
                    .withBaseUrl(baseUrl)
                    .withPathElements(pathBuilder.build())
                    .withHeader(FlowStoreServiceConstants.IF_MATCH_HEADER, Long.toString(version))
                    .execute()) {
                verifyResponseStatus(response, NO_CONTENT);
            }
        } finally {
            log.debug("FlowStoreServiceConnector: deleteSink took {} milliseconds", stopWatch.getElapsedTime());
        }
    }

    // ************************************************* Submitter *************************************************

    /**
     * Deletes an existing submitter from the flow-store
     *
     * @param submitterId the database related ID
     * @param version     the current JPA version of the submitter - Optimistic Locking
     * @throws ProcessingException                                    on general communication error
     * @throws FlowStoreServiceConnectorUnexpectedStatusCodeException if an unexpected HTTP code is returned
     */
    public void deleteSubmitter(long submitterId, long version) throws ProcessingException, FlowStoreServiceConnectorUnexpectedStatusCodeException {
        final StopWatch stopWatch = new StopWatch();
        try {
            log.trace("FlowStoreServiceConnector: deleteSubmitter({})", submitterId);
            final PathBuilder pathBuilder = new PathBuilder(FlowStoreServiceConstants.SUBMITTER)
                    .bind(FlowStoreServiceConstants.ID_VARIABLE, Long.toString(submitterId));

            try (Response response = new HttpDelete(failSafeHttpClient)
                    .withBaseUrl(baseUrl)
                    .withPathElements(pathBuilder.build())
                    .withHeader(FlowStoreServiceConstants.IF_MATCH_HEADER, Long.toString(version))
                    .execute()) {
                verifyResponseStatus(response, NO_CONTENT);
            }
        } finally {
            log.debug("FlowStoreServiceConnector: deleteSubmitter took {} milliseconds", stopWatch.getElapsedTime());
        }
    }

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
        final StopWatch stopWatch = new StopWatch();
        try {
            log.trace("FlowStoreServiceConnector: createSubmitter(\"{}\");",
                    InvariantUtil.checkNotNullOrThrow(submitterContent, "submitterContent").getName());
            try (Response response = new HttpPost(failSafeHttpClient)
                    .withBaseUrl(baseUrl)
                    .withPathElements(FlowStoreServiceConstants.SUBMITTERS)
                    .withJsonData(submitterContent)
                    .execute()) {
                verifyResponseStatus(response, Response.Status.CREATED);
                return readResponseEntity(response, Submitter.class);
            }
        } finally {
            log.debug("FlowStoreServiceConnector: createSubmitter took {} milliseconds", stopWatch.getElapsedTime());
        }
    }

    /**
     * Retrieves the specified submitter from the flow-store
     *
     * @param submitterId Id of the submitter
     * @return the submitter found
     * @throws ProcessingException                on general communication error
     * @throws FlowStoreServiceConnectorException on failure to retrieve the submitter
     */
    public Submitter getSubmitter(long submitterId) throws ProcessingException, FlowStoreServiceConnectorException {
        final StopWatch stopWatch = new StopWatch();
        try {
            log.trace("FlowStoreServiceConnector: getSubmitter({});", submitterId);
            final PathBuilder path = new PathBuilder(FlowStoreServiceConstants.SUBMITTER)
                    .bind(FlowStoreServiceConstants.ID_VARIABLE, submitterId);

            try (Response response = new HttpGet(failSafeHttpClient)
                    .withBaseUrl(baseUrl)
                    .withPathElements(path.build())
                    .execute()) {
                verifyResponseStatus(response, Response.Status.OK);
                return readResponseEntity(response, Submitter.class);
            }
        } finally {
            log.debug("FlowStoreServiceConnector: getSubmitter took {} milliseconds", stopWatch.getElapsedTime());
        }
    }

    /**
     * Retrieves the specified submitter from the flow-store
     *
     * @param submitterNumber submitter number uniquely identifying the submitter
     * @return the submitter found
     * @throws ProcessingException                on general communication error
     * @throws FlowStoreServiceConnectorException on failure to retrieve the submitter
     */
    public Submitter getSubmitterBySubmitterNumber(long submitterNumber) throws ProcessingException, FlowStoreServiceConnectorException {
        final StopWatch stopWatch = new StopWatch();
        try {
            log.trace("FlowStoreServiceConnector: getSubmitterBySubmitterNumber({});", submitterNumber);
            final PathBuilder path = new PathBuilder(FlowStoreServiceConstants.SUBMITTER_SEARCHES_NUMBER)
                    .bind(FlowStoreServiceConstants.SUBMITTER_NUMBER_VARIABLE, submitterNumber);

            try (Response response = new HttpGet(failSafeHttpClient)
                    .withBaseUrl(baseUrl)
                    .withPathElements(path.build())
                    .execute()) {
                verifyResponseStatus(response, Response.Status.OK);
                return readResponseEntity(response, Submitter.class);
            }
        } finally {
            log.debug("FlowStoreServiceConnector: getSubmitterBySubmitterNumber took {} milliseconds", stopWatch.getElapsedTime());
        }
    }

    /**
     * Updates an existing submitter in the flow-store
     *
     * @param submitterContent the new submitter content
     * @param submitterId      the id of the submitter to update
     * @param version          the current version of the submitter
     * @return the updated submitter
     * @throws ProcessingException                on general communication error
     * @throws FlowStoreServiceConnectorException on failure to update the submitter
     */
    public Submitter updateSubmitter(SubmitterContent submitterContent, long submitterId, long version) throws ProcessingException, FlowStoreServiceConnectorException {
        final StopWatch stopWatch = new StopWatch();
        try {
            log.trace("FlowStoreServiceConnector: updateSubmitter({}, {});", submitterId, version);
            InvariantUtil.checkNotNullOrThrow(submitterContent, "submitterContent");
            final PathBuilder path = new PathBuilder(FlowStoreServiceConstants.SUBMITTER_CONTENT)
                    .bind(FlowStoreServiceConstants.ID_VARIABLE, submitterId);

            try (Response response = new HttpPost(failSafeHttpClient)
                    .withBaseUrl(baseUrl)
                    .withPathElements(path.build())
                    .withHeader(FlowStoreServiceConstants.IF_MATCH_HEADER, Long.toString(version))
                    .withJsonData(submitterContent)
                    .execute()) {
                verifyResponseStatus(response, Response.Status.OK);
                return readResponseEntity(response, Submitter.class);
            }
        } finally {
            log.debug("FlowStoreServiceConnector: updateSubmitter took {} milliseconds", stopWatch.getElapsedTime());
        }
    }

    /**
     * Retrieves all submitters from the flow-store
     *
     * @return a list containing the submitters found
     * @throws ProcessingException                on general communication error
     * @throws FlowStoreServiceConnectorException on failure to retrieve the submitters
     */
    public List<Submitter> findAllSubmitters() throws ProcessingException, FlowStoreServiceConnectorException {
        final StopWatch stopWatch = new StopWatch();
        try {
            log.trace("FlowStoreServiceConnector: findAllSubmitters();");
            try (Response response = new HttpGet(failSafeHttpClient)
                    .withBaseUrl(baseUrl)
                    .withPathElements(FlowStoreServiceConstants.SUBMITTERS)
                    .execute()) {
                verifyResponseStatus(response, Response.Status.OK);
                return readResponseGenericTypeEntity(response, new GenericType<List<Submitter>>() {
                });
            }
        } finally {
            log.debug("FlowStoreServiceConnector: findAllSubmitters took {} milliseconds", stopWatch.getElapsedTime());
        }
    }

    /**
     * Retrieves list of submitters by executing IOQL query
     *
     * @param query IOQL query
     * @return list of submitters
     * @throws FlowStoreServiceConnectorException on failure to execute query expression
     */
    public List<Submitter> querySubmitters(String query) throws FlowStoreServiceConnectorException {
        final StopWatch stopWatch = new StopWatch();
        try (Response response = new HttpPost(failSafeHttpClient)
                .withBaseUrl(baseUrl)
                .withPathElements(FlowStoreServiceConstants.SUBMITTERS_QUERIES)
                .withData(query, MediaType.TEXT_PLAIN)
                .execute()) {
            verifyResponseStatus(response, Response.Status.OK);
            return readResponseGenericTypeEntity(response, new GenericType<List<Submitter>>() {
            });
        } finally {
            log.debug("FlowStoreServiceConnector: querySubmitters took {} milliseconds", stopWatch.getElapsedTime());
        }
    }

    /**
     * Resolves given submitter ID into attached flow-binders
     *
     * @param submitterId submitter ID to resolve into attached flow-binders
     * @return list of {@link FlowBinderIdent}
     * @throws ProcessingException                on general communication error
     * @throws FlowStoreServiceConnectorException on failure to retrieve the submitters
     */
    public List<FlowBinderIdent> getFlowBindersForSubmitter(long submitterId)
            throws ProcessingException, FlowStoreServiceConnectorException {
        final StopWatch stopWatch = new StopWatch();
        try {
            final PathBuilder path = new PathBuilder(FlowStoreServiceConstants.SUBMITTER_FLOW_BINDERS)
                    .bind(FlowStoreServiceConstants.ID_VARIABLE, submitterId);

            try (Response response = new HttpGet(failSafeHttpClient)
                    .withBaseUrl(baseUrl)
                    .withPathElements(path.build())
                    .execute()) {
                verifyResponseStatus(response, Response.Status.OK);
                return readResponseGenericTypeEntity(response,
                        new GenericType<List<FlowBinderIdent>>() {
                        });
            }
        } finally {
            log.debug("getFlowBindersForSubmitter took {} milliseconds", stopWatch.getElapsedTime());
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
        final StopWatch stopWatch = new StopWatch();
        try {
            log.trace("FlowStoreServiceConnector: createFlowComponent(\"{}\");",
                    InvariantUtil.checkNotNullOrThrow(flowComponentContent, "flowComponentContent").getName());
            try (Response response = new HttpPost(failSafeHttpClient)
                    .withBaseUrl(baseUrl)
                    .withPathElements(FlowStoreServiceConstants.FLOW_COMPONENTS)
                    .withJsonData(flowComponentContent)
                    .execute()) {
                verifyResponseStatus(response, Response.Status.CREATED);
                return readResponseEntity(response, FlowComponent.class);
            }
        } finally {
            log.debug("FlowStoreServiceConnector: createFlowComponent took {} milliseconds", stopWatch.getElapsedTime());
        }
    }

    /**
     * Retrieves brief views of all flow components from the flow-store
     *
     * @return a list containing the flow components found
     * @throws ProcessingException                on general communication error
     * @throws FlowStoreServiceConnectorException on failure to retrieve the flows components
     */
    public List<FlowComponentView> findAllFlowComponents()
            throws ProcessingException, FlowStoreServiceConnectorException {
        final StopWatch stopWatch = new StopWatch();
        try (Response response = new HttpGet(failSafeHttpClient)
                .withBaseUrl(baseUrl)
                .withPathElements(FlowStoreServiceConstants.FLOW_COMPONENTS)
                .execute()) {
            verifyResponseStatus(response, Response.Status.OK);
            return readResponseGenericTypeEntity(response, new GenericType<List<FlowComponentView>>() {
            });
        } finally {
            log.debug("FlowStoreServiceConnector: findAllFlowComponents took {} milliseconds",
                    stopWatch.getElapsedTime());
        }
    }

    /**
     * Retrieves the specified flow component from the flow-store
     *
     * @param flowComponentId Id of the flow component
     * @return the flow component found
     * @throws ProcessingException                on general communication error
     * @throws FlowStoreServiceConnectorException on failure to retrieve the flow component
     */
    public FlowComponent getFlowComponent(long flowComponentId) throws ProcessingException, FlowStoreServiceConnectorException {
        final StopWatch stopWatch = new StopWatch();
        try {
            log.trace("FlowStoreServiceConnector: getFlowComponent({});", flowComponentId);
            final PathBuilder path = new PathBuilder(FlowStoreServiceConstants.FLOW_COMPONENT)
                    .bind(FlowStoreServiceConstants.ID_VARIABLE, flowComponentId);

            try (Response response = new HttpGet(failSafeHttpClient)
                    .withBaseUrl(baseUrl)
                    .withPathElements(path.build())
                    .execute()) {
                verifyResponseStatus(response, Response.Status.OK);
                return readResponseEntity(response, FlowComponent.class);
            }
        } finally {
            log.debug("FlowStoreServiceConnector: getFlowComponent took {} milliseconds", stopWatch.getElapsedTime());
        }
    }

    /**
     * Updates an existing flow component from the flow-store
     *
     * @param flowComponentContent the new flow component content
     * @param flowComponentId      the id of the flow component to update
     * @param version              the current version of the flow component
     * @return the updated flow component
     * @throws ProcessingException                on general communication error
     * @throws FlowStoreServiceConnectorException on failure to update the flow component
     */
    public FlowComponent updateFlowComponent(FlowComponentContent flowComponentContent, long flowComponentId, long version) throws ProcessingException, FlowStoreServiceConnectorException {
        final StopWatch stopWatch = new StopWatch();
        try {
            log.trace("FlowStoreServiceConnector: updateFlowComponent({}, {});", flowComponentId, version);
            InvariantUtil.checkNotNullOrThrow(flowComponentContent, "flowComponentContent");
            final PathBuilder path = new PathBuilder(FlowStoreServiceConstants.FLOW_COMPONENT_CONTENT)
                    .bind(FlowStoreServiceConstants.ID_VARIABLE, flowComponentId);

            try (Response response = new HttpPost(failSafeHttpClient)
                    .withBaseUrl(baseUrl)
                    .withPathElements(path.build())
                    .withHeader(FlowStoreServiceConstants.IF_MATCH_HEADER, Long.toString(version))
                    .withJsonData(flowComponentContent)
                    .execute()) {
                verifyResponseStatus(response, Response.Status.OK);
                return readResponseEntity(response, FlowComponent.class);
            }
        } finally {
            log.debug("FlowStoreServiceConnector: updateFlowComponent took {} milliseconds", stopWatch.getElapsedTime());
        }
    }

    /**
     * Updates an existing flow component from the flow-store by adding next flow component content
     *
     * @param next            flow component content
     * @param flowComponentId the id of the flow component to update with next
     * @param version         the current version of the flow component
     * @return the updated with next flow component
     * @throws ProcessingException                on general communication error
     * @throws FlowStoreServiceConnectorException on failure to update the flow component
     */
    public FlowComponent updateNext(FlowComponentContent next, long flowComponentId, long version) throws ProcessingException, FlowStoreServiceConnectorException {
        final StopWatch stopWatch = new StopWatch();
        try {
            log.trace("FlowStoreServiceConnector: updateNext({}, {});", flowComponentId, version);
            final PathBuilder path = new PathBuilder(FlowStoreServiceConstants.FLOW_COMPONENT_NEXT)
                    .bind(FlowStoreServiceConstants.ID_VARIABLE, flowComponentId);

            try (Response response = new HttpPost(failSafeHttpClient)
                    .withBaseUrl(baseUrl)
                    .withPathElements(path.build())
                    .withHeader(FlowStoreServiceConstants.IF_MATCH_HEADER, Long.toString(version))
                    .withJsonData(next)
                    .execute()) {
                verifyResponseStatus(response, Response.Status.OK);
                return readResponseEntity(response, FlowComponent.class);
            }
        } finally {
            log.debug("FlowStoreServiceConnector: updateNext took {} milliseconds", stopWatch.getElapsedTime());
        }
    }

    /**
     * Deletes an existing flow component from the flow-store
     *
     * @param flowComponentId, the database related ID
     * @param version,         the current JPA version of the sink - Optimistic Locking
     * @throws ProcessingException                                    on general communication error
     * @throws FlowStoreServiceConnectorUnexpectedStatusCodeException if an unexpected HTTP code is returned
     */
    public void deleteFlowComponent(long flowComponentId, long version) throws ProcessingException, FlowStoreServiceConnectorUnexpectedStatusCodeException {
        final StopWatch stopWatch = new StopWatch();
        try {
            log.trace("FlowStoreServiceConnector: deleteFlowComponent({})", flowComponentId);
            final PathBuilder pathBuilder = new PathBuilder(FlowStoreServiceConstants.FLOW_COMPONENT)
                    .bind(FlowStoreServiceConstants.ID_VARIABLE, Long.toString(flowComponentId));

            try (Response response = new HttpDelete(failSafeHttpClient)
                    .withBaseUrl(baseUrl)
                    .withPathElements(pathBuilder.build())
                    .withHeader(FlowStoreServiceConstants.IF_MATCH_HEADER, Long.toString(version))
                    .execute()) {
                verifyResponseStatus(response, NO_CONTENT);
            }
        } finally {
            log.debug("FlowStoreServiceConnector: deleteFlowComponent took {} milliseconds", stopWatch.getElapsedTime());
        }
    }


    // ************************************************** Flow **************************************************

    /**
     * Creates new flow defined by the flow content
     *
     * @param flowContent flow content
     * @return Flow
     * @throws NullPointerException                                   if given null-valued argument
     * @throws ProcessingException                                    on general communication error
     * @throws FlowStoreServiceConnectorUnexpectedStatusCodeException if flow creation failed due to invalid input data
     * @throws FlowStoreServiceConnectorException                     on general failure to create flow
     */
    public Flow createFlow(FlowContent flowContent) throws NullPointerException, ProcessingException, FlowStoreServiceConnectorException {
        final StopWatch stopWatch = new StopWatch();
        try {
            log.trace("FlowStoreServiceConnector: createFlow({});",
                    InvariantUtil.checkNotNullOrThrow(flowContent, "flowContent").getName());
            try (Response response = new HttpPost(failSafeHttpClient)
                    .withBaseUrl(baseUrl)
                    .withPathElements(FlowStoreServiceConstants.FLOWS)
                    .withJsonData(flowContent)
                    .execute()) {
                verifyResponseStatus(response, Response.Status.CREATED);
                return readResponseEntity(response, Flow.class);
            }
        } finally {
            log.debug("FlowStoreServiceConnector: createFlow took {} milliseconds", stopWatch.getElapsedTime());
        }
    }

    /**
     * Retrieves the specified flow from the flow-store
     *
     * @param flowId Id of the flow
     * @return the flow found
     * @throws ProcessingException                on general communication error
     * @throws FlowStoreServiceConnectorException on failure to retrieve the flow
     */
    public Flow getFlow(long flowId) throws ProcessingException, FlowStoreServiceConnectorException {
        final StopWatch stopWatch = new StopWatch();
        try {
            log.trace("FlowStoreServiceConnector: getFlow({});", flowId);
            final PathBuilder path = new PathBuilder(FlowStoreServiceConstants.FLOW)
                    .bind(FlowStoreServiceConstants.ID_VARIABLE, flowId);

            try (Response response = new HttpGet(failSafeHttpClient)
                    .withBaseUrl(baseUrl)
                    .withPathElements(path.build())
                    .execute()) {
                verifyResponseStatus(response, Response.Status.OK);
                return readResponseEntity(response, Flow.class);
            }
        } finally {
            log.debug("FlowStoreServiceConnector: getFlow took {} milliseconds", stopWatch.getElapsedTime());
        }
    }

    /**
     * Retrieves the specified flows from the flow-store
     *
     * @param queryParameters containing none or many flow criterias
     * @return the list of flows found
     * @throws ProcessingException                on general communication error
     * @throws FlowStoreServiceConnectorException on failure to retrieve flow
     */
    public List<Flow> findFlows(Map<String, Object> queryParameters) throws ProcessingException, FlowStoreServiceConnectorException {
        final StopWatch stopWatch = new StopWatch();
        try {
            final HttpGet httpGet = new HttpGet(failSafeHttpClient)
                    .withBaseUrl(baseUrl)
                    .withPathElements(FlowStoreServiceConstants.FLOWS);

            queryParameters.forEach(httpGet::withQueryParameter);

            try (Response response = httpGet.execute()) {
                verifyResponseStatus(response, Response.Status.OK);
                return readResponseGenericTypeEntity(response, new GenericType<List<Flow>>() {
                });
            }
        } finally {
            log.debug("findFlows took {} milliseconds", stopWatch.getElapsedTime());
        }
    }

    /**
     * Retrieves brief views of all flows from the flow-store
     *
     * @return a list containing the flows found
     * @throws ProcessingException                on general communication error
     * @throws FlowStoreServiceConnectorException on failure to retrieve the flows
     */
    public List<FlowView> findAllFlows() throws ProcessingException, FlowStoreServiceConnectorException {
        final StopWatch stopWatch = new StopWatch();
        try {
            final HttpGet httpGet = new HttpGet(failSafeHttpClient)
                    .withBaseUrl(baseUrl)
                    .withPathElements(FlowStoreServiceConstants.FLOWS);

            try (Response response = httpGet.execute()) {
                verifyResponseStatus(response, Response.Status.OK);
                return readResponseGenericTypeEntity(response, new GenericType<List<FlowView>>() {
                });
            }
        } finally {
            log.debug("findAllFlows took {} milliseconds", stopWatch.getElapsedTime());
        }
    }

    /**
     * Retrieves uniquely named flow from the flow-store
     *
     * @param name name of Flow to look lookup
     * @return the flow found
     * @throws ProcessingException                on general communication error
     * @throws FlowStoreServiceConnectorException on failure to retrieve the flows
     */
    public Flow findFlowByName(String name) throws ProcessingException, FlowStoreServiceConnectorException {
        final List<Flow> flows = findFlows(Collections.singletonMap("name", name));
        if (flows.isEmpty()) {
            return null;
        }
        return flows.get(0);
    }

    /**
     * Updates the versioned flow components contained within the flow, to latest version
     *
     * @param flowId  the id of the flow  to update
     * @param version the current version of the flow
     * @return the updated flow
     * @throws ProcessingException                on general communication error
     * @throws FlowStoreServiceConnectorException on failure to update the flow
     */
    public Flow refreshFlowComponents(long flowId, long version) throws ProcessingException, FlowStoreServiceConnectorException {
        final StopWatch stopWatch = new StopWatch();
        try {
            log.trace("FlowStoreServiceConnector: refreshFlowComponents({}, {});", flowId, version);
            final PathBuilder path = new PathBuilder(FlowStoreServiceConstants.FLOW_CONTENT)
                    .bind(FlowStoreServiceConstants.ID_VARIABLE, flowId);

            // An empty string is given as post data because post have to have some sort of input.
            // Nothing cannot be posted. An update is still desired, but the "real" data to post is not provided.
            try (Response response = new HttpPost(failSafeHttpClient)
                    .withBaseUrl(baseUrl)
                    .withPathElements(path.build())
                    .withQueryParameter(FlowStoreServiceConstants.QUERY_PARAMETER_REFRESH, true)
                    .withHeader(FlowStoreServiceConstants.IF_MATCH_HEADER, Long.toString(version))
                    .withJsonData("")
                    .execute()) {
                verifyResponseStatus(response, Response.Status.OK);
                return readResponseEntity(response, Flow.class);
            }
        } finally {
            log.debug("FlowStoreServiceConnector: refreshFlowComponents took {} milliseconds", stopWatch.getElapsedTime());
        }
    }

    /**
     * Updates an existing flow in the flow-store
     *
     * @param flowContent the new flow content
     * @param flowId      the id of the flow to update
     * @param version     the current version of the flow
     * @return the updated flow
     * @throws ProcessingException                on general communication error
     * @throws FlowStoreServiceConnectorException on failure to update the flow
     */
    public Flow updateFlow(FlowContent flowContent, long flowId, long version) throws ProcessingException, FlowStoreServiceConnectorException {
        final StopWatch stopWatch = new StopWatch();
        try {
            log.trace("FlowStoreServiceConnector: updateFlow({}, {});", flowId, version);
            InvariantUtil.checkNotNullOrThrow(flowContent, "flowContent");
            final PathBuilder path = new PathBuilder(FlowStoreServiceConstants.FLOW_CONTENT)
                    .bind(FlowStoreServiceConstants.ID_VARIABLE, flowId);

            try (Response response = new HttpPost(failSafeHttpClient)
                    .withBaseUrl(baseUrl)
                    .withPathElements(path.build())
                    .withQueryParameter(FlowStoreServiceConstants.QUERY_PARAMETER_REFRESH, false)
                    .withHeader(FlowStoreServiceConstants.IF_MATCH_HEADER, Long.toString(version))
                    .withJsonData(flowContent)
                    .execute()) {
                verifyResponseStatus(response, Response.Status.OK);
                return readResponseEntity(response, Flow.class);
            }
        } finally {
            log.debug("FlowStoreServiceConnector: updateFlow took {} milliseconds", stopWatch.getElapsedTime());
        }
    }

    /**
     * Deletes an existing flow from the flow-store
     *
     * @param flowId,  the database related ID
     * @param version, the current JPA version of the sink - Optimistic Locking
     * @throws ProcessingException                                    on general communication error
     * @throws FlowStoreServiceConnectorUnexpectedStatusCodeException if an unexpected HTTP code is returned
     */
    public void deleteFlow(long flowId, long version) throws ProcessingException, FlowStoreServiceConnectorUnexpectedStatusCodeException {
        final StopWatch stopWatch = new StopWatch();
        try {
            log.trace("FlowStoreServiceConnector: deleteFlow({})", flowId);
            final PathBuilder pathBuilder = new PathBuilder(FlowStoreServiceConstants.FLOW)
                    .bind(FlowStoreServiceConstants.ID_VARIABLE, Long.toString(flowId));

            try (Response response = new HttpDelete(failSafeHttpClient)
                    .withBaseUrl(baseUrl)
                    .withPathElements(pathBuilder.build())
                    .withHeader(FlowStoreServiceConstants.IF_MATCH_HEADER, Long.toString(version))
                    .execute()) {
                verifyResponseStatus(response, NO_CONTENT);
            }
        } finally {
            log.debug("FlowStoreServiceConnector: deleteFlow took {} milliseconds", stopWatch.getElapsedTime());
        }
    }


    // ************************************************** FlowBinder **************************************************

    /**
     * Creates new flow binder defined by the flow binder content
     *
     * @param flowBinderContent flow binder content
     * @return FlowBinder
     * @throws NullPointerException                                   if given null-valued argument
     * @throws ProcessingException                                    on general communication error
     * @throws FlowStoreServiceConnectorUnexpectedStatusCodeException if flow binder creation failed due to invalid input data
     * @throws FlowStoreServiceConnectorException                     on general failure to create flow binder
     */
    public FlowBinder createFlowBinder(FlowBinderContent flowBinderContent) throws NullPointerException, ProcessingException, FlowStoreServiceConnectorException {
        final StopWatch stopWatch = new StopWatch();
        try {
            log.trace("FlowStoreServiceConnector: createFlowBinder(\"{}\");",
                    InvariantUtil.checkNotNullOrThrow(flowBinderContent, "flowBinderContent").getName());
            try (Response response = new HttpPost(failSafeHttpClient)
                    .withBaseUrl(baseUrl)
                    .withPathElements(FlowStoreServiceConstants.FLOW_BINDERS)
                    .withJsonData(flowBinderContent)
                    .execute()) {
                verifyResponseStatus(response, Response.Status.CREATED);
                return readResponseEntity(response, FlowBinder.class);
            }
        } finally {
            log.debug("FlowStoreServiceConnector: createFlowBinder took {} milliseconds", stopWatch.getElapsedTime());
        }
    }

    /**
     * Retrieves all flow binders from the flow-store
     *
     * @return a list containing the flow binders found
     * @throws ProcessingException                on general communication error
     * @throws FlowStoreServiceConnectorException on failure to retrieve the flow binders
     */
    public List<FlowBinder> findAllFlowBinders() throws ProcessingException, FlowStoreServiceConnectorException {
        final StopWatch stopWatch = new StopWatch();
        try {
            log.trace("FlowStoreServiceConnector: findAllFlowBinders();");
            try (Response response = new HttpGet(failSafeHttpClient)
                    .withBaseUrl(baseUrl)
                    .withPathElements(FlowStoreServiceConstants.FLOW_BINDERS)
                    .execute()) {
                verifyResponseStatus(response, Response.Status.OK);
                return readResponseGenericTypeEntity(response, new GenericType<List<FlowBinder>>() {
                });
            }
        } finally {
            log.debug("FlowStoreServiceConnector: findAllFlowBinders took {} milliseconds", stopWatch.getElapsedTime());
        }
    }

    /**
     * Updates an existing flow binder from the flow-store
     *
     * @param flowBinderContent the new flow binder content
     * @param flowBinderId      the id of the flow binder to update
     * @param version           the current version of the flow binder
     * @return the updated flow binder
     * @throws ProcessingException                on general communication error
     * @throws FlowStoreServiceConnectorException on failure to update the flow binder
     */
    public FlowBinder updateFlowBinder(FlowBinderContent flowBinderContent, long flowBinderId, long version) throws ProcessingException, FlowStoreServiceConnectorException {
        final StopWatch stopWatch = new StopWatch();
        try {
            log.trace("FlowStoreServiceConnector: updateFlowBinder({}, {});", flowBinderId, version);
            InvariantUtil.checkNotNullOrThrow(flowBinderContent, "flowBinderContent");
            final PathBuilder path = new PathBuilder(FlowStoreServiceConstants.FLOW_BINDER_CONTENT)
                    .bind(FlowStoreServiceConstants.ID_VARIABLE, flowBinderId);

            try (Response response = new HttpPost(failSafeHttpClient)
                    .withBaseUrl(baseUrl)
                    .withPathElements(path.build())
                    .withHeader(FlowStoreServiceConstants.IF_MATCH_HEADER, Long.toString(version))
                    .withJsonData(flowBinderContent)
                    .execute()) {
                verifyResponseStatus(response, Response.Status.OK);
                return readResponseEntity(response, FlowBinder.class);
            }
        } finally {
            log.debug("FlowStoreServiceConnector: updateFlowBinder took {} milliseconds", stopWatch.getElapsedTime());
        }
    }

    /**
     * Deletes an existing flow binder from the flow-store
     *
     * @param flowBinderId, the database related ID
     * @param version,      the current JPA version of the flow binder - Optimistic Locking
     * @throws ProcessingException                                    on general communication error
     * @throws FlowStoreServiceConnectorUnexpectedStatusCodeException if an unexpected HTTP code is returned
     */
    public void deleteFlowBinder(long flowBinderId, long version) throws ProcessingException, FlowStoreServiceConnectorUnexpectedStatusCodeException {
        final StopWatch stopWatch = new StopWatch();
        try {
            log.trace("FlowStoreServiceConnector: deleteFlowBinder({})", flowBinderId);
            final PathBuilder pathBuilder = new PathBuilder(FlowStoreServiceConstants.FLOW_BINDER)
                    .bind(FlowStoreServiceConstants.ID_VARIABLE, Long.toString(flowBinderId));

            try (Response response = new HttpDelete(failSafeHttpClient)
                    .withBaseUrl(baseUrl)
                    .withPathElements(pathBuilder.build())
                    .withHeader(FlowStoreServiceConstants.IF_MATCH_HEADER, Long.toString(version))
                    .execute()) {
                verifyResponseStatus(response, NO_CONTENT);
            }
        } finally {
            log.debug("FlowStoreServiceConnector: deleteFlowBinder took {} milliseconds", stopWatch.getElapsedTime());
        }
    }

    /**
     * Retrieves a flow binder from the flow-store
     *
     * @param flowBinderId Id of the flow binder
     * @return the flow binder
     * @throws ProcessingException                on general communication error
     * @throws FlowStoreServiceConnectorException on failure to retrieve the flow binder
     */
    public FlowBinder getFlowBinder(long flowBinderId) throws ProcessingException, FlowStoreServiceConnectorException {
        final StopWatch stopWatch = new StopWatch();
        try {
            log.trace("FlowStoreServiceConnector: getFlowBinder({});", flowBinderId);
            final PathBuilder path = new PathBuilder(FlowStoreServiceConstants.FLOW_BINDER)
                    .bind(FlowStoreServiceConstants.ID_VARIABLE, flowBinderId);

            try (Response response = new HttpGet(failSafeHttpClient)
                    .withBaseUrl(baseUrl)
                    .withPathElements(path.build())
                    .execute()) {
                verifyResponseStatus(response, Response.Status.OK);
                return readResponseEntity(response, FlowBinder.class);
            }
        } finally {
            log.debug("FlowStoreServiceConnector: getFlowBinder took {} milliseconds", stopWatch.getElapsedTime());
        }
    }

    /**
     * Retrieves a flow binder through search indexes
     *
     * @param packaging       of the flow binder
     * @param format          of the flow binder
     * @param charset         of the flow binder
     * @param submitterNumber identifying the referenced submitter
     * @param destination     of the flow binder
     * @return the flow binder
     * @throws FlowStoreServiceConnectorException on failure to retrieve the flow binder
     */
    public FlowBinder getFlowBinder(String packaging, String format, String charset, long submitterNumber, String destination) throws FlowStoreServiceConnectorException {
        final StopWatch stopWatch = new StopWatch();
        try {
            log.trace("FlowStoreServiceConnector: getFlowBinder(\"{}\", \"{}\", \"{}\", {}, \"{}\");",
                    packaging, format, charset, submitterNumber, destination);
            try (Response response = new HttpGet(failSafeHttpClient)
                    .withBaseUrl(baseUrl)
                    .withPathElements(new String[]{FlowStoreServiceConstants.FLOW_BINDER_RESOLVE})
                    .withQueryParameter(FlowBinderResolveQuery.REST_PARAMETER_PACKAGING, packaging)
                    .withQueryParameter(FlowBinderResolveQuery.REST_PARAMETER_FORMAT, format)
                    .withQueryParameter(FlowBinderResolveQuery.REST_PARAMETER_CHARSET, charset)
                    .withQueryParameter(FlowBinderResolveQuery.REST_PARAMETER_SUBMITTER, Long.toString(submitterNumber))
                    .withQueryParameter(FlowBinderResolveQuery.REST_PARAMETER_DESTINATION, destination)
                    .execute()) {
                verifyResponseStatus(response, Response.Status.OK);
                return readResponseEntity(response, FlowBinder.class);
            }
        } finally {
            log.debug("FlowStoreServiceConnector: getFlowBinder took {} milliseconds", stopWatch.getElapsedTime());
        }
    }

    /**
     * Retrieves list of flow binders by executing IOQL query
     *
     * @param query IOQL query
     * @return list of flow binders
     * @throws FlowStoreServiceConnectorException on failure to execute query expression
     */
    public List<FlowBinder> queryFlowBinders(String query) throws FlowStoreServiceConnectorException {
        final StopWatch stopWatch = new StopWatch();
        try (Response response = new HttpPost(failSafeHttpClient)
                .withBaseUrl(baseUrl)
                .withPathElements(FlowStoreServiceConstants.FLOW_BINDERS_QUERIES)
                .withData(query, MediaType.TEXT_PLAIN)
                .execute()) {
            verifyResponseStatus(response, Response.Status.OK);
            return readResponseGenericTypeEntity(response, new GenericType<List<FlowBinder>>() {
            });
        } finally {
            log.debug("FlowStoreServiceConnector: queryFlowBinders took {} milliseconds", stopWatch.getElapsedTime());
        }
    }

    // ************************************************** GatekeeperDestinations *****************************************

    /**
     * persists the given GatekeeperDestination
     *
     * @param gatekeeperDestination to persist
     * @return GatekeeperDestination
     * @throws NullPointerException                                   if given null-valued argument
     * @throws ProcessingException                                    on general communication error
     * @throws FlowStoreServiceConnectorUnexpectedStatusCodeException if gatekeeperDestination creation failed due to invalid input data
     * @throws FlowStoreServiceConnectorException                     on general failure to create gatekeeperDestination
     */
    public GatekeeperDestination createGatekeeperDestination(GatekeeperDestination gatekeeperDestination) throws FlowStoreServiceConnectorException {
        final StopWatch stopWatch = new StopWatch();
        try {
            InvariantUtil.checkNotNullOrThrow(gatekeeperDestination, "gatekeeperDestination");
            log.trace("FlowStoreServiceConnector: createGatekeeperDestination({}, {}, {}, {})",
                    gatekeeperDestination.getSubmitterNumber(),
                    gatekeeperDestination.getDestination(),
                    gatekeeperDestination.getPackaging(),
                    gatekeeperDestination.getFormat());
            try (Response response = new HttpPost(failSafeHttpClient)
                    .withBaseUrl(baseUrl)
                    .withPathElements(FlowStoreServiceConstants.GATEKEEPER_DESTINATIONS)
                    .withJsonData(gatekeeperDestination)
                    .execute()) {
                verifyResponseStatus(response, Response.Status.CREATED);
                return readResponseEntity(response, GatekeeperDestination.class);
            }
        } finally {
            log.debug("FlowStoreServiceConnector: createGatekeeperDestination took {} milliseconds", stopWatch.getElapsedTime());
        }
    }

    /**
     * Retrieves all gatekeeperDestinations from the flow-store
     *
     * @return a list containing the gatekeeperDestinations found sorted by submitterNumber in ascending order
     * @throws ProcessingException                on general communication error
     * @throws FlowStoreServiceConnectorException on failure to retrieve gatekeeperDestinations
     */
    public List<GatekeeperDestination> findAllGatekeeperDestinations() throws ProcessingException, FlowStoreServiceConnectorException {
        final StopWatch stopWatch = new StopWatch();
        try {
            log.trace("FlowStoreServiceConnector: findAllGatekeeperDestinations();");
            try (Response response = new HttpGet(failSafeHttpClient)
                    .withBaseUrl(baseUrl)
                    .withPathElements(FlowStoreServiceConstants.GATEKEEPER_DESTINATIONS)
                    .execute()) {
                verifyResponseStatus(response, Response.Status.OK);
                return readResponseGenericTypeEntity(response, new GenericType<List<GatekeeperDestination>>() {
                });
            }
        } finally {
            log.debug("FlowStoreServiceConnector: findAllGatekeeperDestinations took {} milliseconds", stopWatch.getElapsedTime());
        }
    }

    /**
     * Deletes an existing gatekeeperDestination from the flow-store
     *
     * @param id, the database related ID
     * @throws ProcessingException                                    on general communication error
     * @throws FlowStoreServiceConnectorUnexpectedStatusCodeException if an unexpected HTTP code is returned
     */
    public void deleteGatekeeperDestination(long id) throws ProcessingException, FlowStoreServiceConnectorUnexpectedStatusCodeException {
        final StopWatch stopWatch = new StopWatch();
        try {
            log.trace("FlowStoreServiceConnector: deleteGatekeeperDestination({})", id);
            final PathBuilder pathBuilder = new PathBuilder(FlowStoreServiceConstants.GATEKEEPER_DESTINATION)
                    .bind(FlowStoreServiceConstants.ID_VARIABLE, Long.toString(id));

            try (Response response = new HttpDelete(failSafeHttpClient)
                    .withBaseUrl(baseUrl)
                    .withPathElements(pathBuilder.build())
                    .execute()) {
                verifyResponseStatus(response, NO_CONTENT);
            }
        } finally {
            log.debug("FlowStoreServiceConnector: deleteGatekeeperDestination took {} milliseconds", stopWatch.getElapsedTime());
        }
    }

    /**
     * Updates an existing gatekeeper destination from the flow-store
     *
     * @param gatekeeperDestination containing the updated values
     * @return the updated gatekeeperDestination
     * @throws ProcessingException                on general communication error
     * @throws FlowStoreServiceConnectorException on failure to update the gatekeeper destination
     */
    public GatekeeperDestination updateGatekeeperDestination(GatekeeperDestination gatekeeperDestination) throws ProcessingException, FlowStoreServiceConnectorException {
        final StopWatch stopWatch = new StopWatch();
        try {
            log.trace("FlowStoreServiceConnector: updateGatekeeperDestination()");
            InvariantUtil.checkNotNullOrThrow(gatekeeperDestination, "gatekeeperDestination");

            final PathBuilder path = new PathBuilder(FlowStoreServiceConstants.GATEKEEPER_DESTINATION)
                    .bind(FlowStoreServiceConstants.ID_VARIABLE, Long.toString(gatekeeperDestination.getId()));

            try (Response response = new HttpPost(failSafeHttpClient)
                    .withBaseUrl(baseUrl)
                    .withPathElements(path.build())
                    .withJsonData(gatekeeperDestination)
                    .execute()) {
                verifyResponseStatus(response, Response.Status.OK);
                return readResponseEntity(response, GatekeeperDestination.class);
            }
        } finally {
            log.debug("FlowStoreServiceConnector: updateGatekeeperDestination took {} milliseconds", stopWatch.getElapsedTime());
        }
    }


    // ************************************************** HarvesterConfig *********************************************

    /**
     * Creates new harvester config defined by the harvester config content and type
     *
     * @param configContent harvester config content
     * @param type          of harvester config
     * @param <T>           type parameter
     * @return the created harvester config
     * @throws NullPointerException                                   if given null-valued argument
     * @throws ProcessingException                                    on general communication error
     * @throws FlowStoreServiceConnectorUnexpectedStatusCodeException if harvester config creation failed due to invalid input data
     * @throws FlowStoreServiceConnectorException                     on general failure to create harvester config
     */
    public <T> T createHarvesterConfig(Object configContent, Class<T> type) throws ProcessingException, FlowStoreServiceConnectorException {
        final StopWatch stopWatch = new StopWatch();
        try {
            log.trace("FlowStoreServiceConnector: createHarvesterConfig called with content='{}', type='{}'",
                    InvariantUtil.checkNotNullOrThrow(configContent, "configContent"), type);

            final PathBuilder path = new PathBuilder(FlowStoreServiceConstants.HARVESTER_CONFIGS_TYPE)
                    .bind("type", type.getName());

            try (Response response = new HttpPost(failSafeHttpClient)
                    .withBaseUrl(baseUrl)
                    .withPathElements(path.build())
                    .withJsonData(configContent)
                    .execute()) {
                verifyResponseStatus(response, Response.Status.CREATED);
                return readResponseEntity(response, type);
            }
        } finally {
            log.debug("FlowStoreServiceConnector: createHarvesterConfig took {} milliseconds", stopWatch.getElapsedTime());
        }
    }

    /**
     * Updates an existing harvester config from the flow-store
     *
     * @param harvesterConfig holding the updated information
     * @param <T>             type parameter
     * @return the updated harvester config
     * @throws ProcessingException                on general communication error
     * @throws FlowStoreServiceConnectorException on failure to update the harvester config
     */
    public <T extends HarvesterConfig> T updateHarvesterConfig(T harvesterConfig) throws ProcessingException, FlowStoreServiceConnectorException {
        final StopWatch stopWatch = new StopWatch();
        try {
            log.trace("FlowStoreServiceConnector: updateHarvesterConfig called with harvesterConfig='{}'",
                    InvariantUtil.checkNotNullOrThrow(harvesterConfig, "harvesterConfig"));

            final PathBuilder path = new PathBuilder(FlowStoreServiceConstants.HARVESTER_CONFIG)
                    .bind(FlowStoreServiceConstants.ID_VARIABLE, Long.toString(harvesterConfig.getId()));

            try (Response response = new HttpPost(failSafeHttpClient)
                    .withBaseUrl(baseUrl)
                    .withPathElements(path.build())
                    .withHeader(FlowStoreServiceConstants.IF_MATCH_HEADER, Long.toString(harvesterConfig.getVersion()))
                    .withHeader(FlowStoreServiceConstants.RESOURCE_TYPE_HEADER, harvesterConfig.getType())
                    .withJsonData(harvesterConfig.getContent())
                    .execute()) {
                verifyResponseStatus(response, Response.Status.OK);
                return (T) readResponseEntity(response, harvesterConfig.getClass());
            }
        } finally {
            log.debug("FlowStoreServiceConnector: updateHarvesterConfig took {} milliseconds", stopWatch.getElapsedTime());
        }
    }

    /**
     * Retrieves harvester config identified by given id as given type
     *
     * @param id   harvester config ID
     * @param type of harvester config
     * @param <T>  type parameter
     * @return the retrieved harvester config
     * @throws NullPointerException                                   if given null-valued tyoe argument
     * @throws ProcessingException                                    on general transport protocol error
     * @throws FlowStoreServiceConnectorUnexpectedStatusCodeException if harvester config retrieval failed
     * @throws FlowStoreServiceConnectorException                     on connector internal error
     */
    public <T> T getHarvesterConfig(long id, Class<T> type) throws NullPointerException, ProcessingException, FlowStoreServiceConnectorException {
        final StopWatch stopWatch = new StopWatch();
        try {
            log.trace("FlowStoreServiceConnector: getHarvesterConfig called with id={}, type='{}'",
                    id, InvariantUtil.checkNotNullOrThrow(type, "type"));

            final PathBuilder path = new PathBuilder(FlowStoreServiceConstants.HARVESTER_CONFIG)
                    .bind(FlowStoreServiceConstants.ID_VARIABLE, id);

            try (Response response = new HttpGet(failSafeHttpClient)
                    .withBaseUrl(baseUrl)
                    .withPathElements(path.build())
                    .execute()) {
                verifyResponseStatus(response, Response.Status.OK);
                return readResponseEntity(response, type);
            }
        } finally {
            log.debug("FlowStoreServiceConnector: getHarvesterConfig took {} milliseconds", stopWatch.getElapsedTime());
        }
    }

    /**
     * Deletes an existing harvester config
     *
     * @param id      ID of config to be deleted
     * @param version current version of config at the time of deletion
     * @throws ProcessingException                                    on general transport protocol error
     * @throws FlowStoreServiceConnectorUnexpectedStatusCodeException if harvester config deletion failed
     * @throws FlowStoreServiceConnectorException                     on connector internal error
     */
    public void deleteHarvesterConfig(long id, long version) throws ProcessingException, FlowStoreServiceConnectorException {
        final StopWatch stopWatch = new StopWatch();
        try {
            log.trace("deleteHarvesterConfig({}, {})", id, version);

            final PathBuilder path = new PathBuilder(FlowStoreServiceConstants.HARVESTER_CONFIG)
                    .bind(FlowStoreServiceConstants.ID_VARIABLE, id);

            try (Response response = new HttpDelete(failSafeHttpClient)
                    .withBaseUrl(baseUrl)
                    .withPathElements(path.build())
                    .withHeader(FlowStoreServiceConstants.IF_MATCH_HEADER, Long.toString(version))
                    .execute()) {
                verifyResponseStatus(response, NO_CONTENT);
            }
        } finally {
            log.debug("Operation took {} milliseconds", stopWatch.getElapsedTime());
        }
    }

    /**
     * Returns list of all enabled harvester configs of given type
     *
     * @param type type of harvester configs to list
     * @param <T>  type parameter
     * @return list of harvester configs
     * @throws ProcessingException                on general communication error
     * @throws FlowStoreServiceConnectorException on failure to retrieve harvester configs
     */
    public <T> List<T> findEnabledHarvesterConfigsByType(Class<T> type) throws ProcessingException, FlowStoreServiceConnectorException {
        final StopWatch stopWatch = new StopWatch();
        try {
            InvariantUtil.checkNotNullOrThrow(type, "type");
            final PathBuilder path = new PathBuilder(FlowStoreServiceConstants.HARVESTER_CONFIGS_TYPE_ENABLED)
                    .bind(FlowStoreServiceConstants.TYPE_VARIABLE, type.getName());

            try (Response response = new HttpGet(failSafeHttpClient)
                    .withBaseUrl(baseUrl)
                    .withPathElements(path.build())
                    .execute()) {
                verifyResponseStatus(response, Response.Status.OK);
                return readResponseGenericTypeEntity(response, new GenericType<>(createListGenericType(type)));
            }
        } finally {
            log.debug("FlowStoreServiceConnector: findEnabledHarvesterConfigsByType took {} milliseconds", stopWatch.getElapsedTime());
        }
    }

    /**
     * Returns list of all harvester configs of given type
     *
     * @param type type of harvester configs to list
     * @param <T>  type parameter
     * @return list of harvester configs
     * @throws ProcessingException                on general communication error
     * @throws FlowStoreServiceConnectorException on failure to retrieve harvester configs
     */
    public <T> List<T> findHarvesterConfigsByType(Class<T> type) throws ProcessingException, FlowStoreServiceConnectorException {
        final StopWatch stopWatch = new StopWatch();
        try {
            InvariantUtil.checkNotNullOrThrow(type, "type");
            final PathBuilder path = new PathBuilder(FlowStoreServiceConstants.HARVESTER_CONFIGS_TYPE)
                    .bind(FlowStoreServiceConstants.TYPE_VARIABLE, type.getName());

            try (Response response = new HttpGet(failSafeHttpClient)
                    .withBaseUrl(baseUrl)
                    .withPathElements(path.build())
                    .execute()) {
                verifyResponseStatus(response, Response.Status.OK);
                return readResponseGenericTypeEntity(response, new GenericType<>(createListGenericType(type)));
            }
        } finally {
            log.debug("FlowStoreServiceConnector: findHarvesterConfigsByType took {} milliseconds", stopWatch.getElapsedTime());
        }
    }

    // ******************************************** Private helper methods ********************************************

    private void verifyResponseStatus(Response response, Response.Status expectedStatus) throws FlowStoreServiceConnectorUnexpectedStatusCodeException {
        final Response.Status actualStatus = Response.Status.fromStatusCode(response.getStatus());
        if (actualStatus != expectedStatus) {
            final FlowStoreServiceConnectorUnexpectedStatusCodeException exception =
                    new FlowStoreServiceConnectorUnexpectedStatusCodeException(String.format(
                            "flow-store service returned with unexpected status code: %s", actualStatus), actualStatus.getStatusCode());

            if (response.hasEntity()) {
                try {
                    exception.setFlowStoreError(readResponseEntity(response, FlowStoreError.class));
                } catch (FlowStoreServiceConnectorException | ProcessingException e) {
                    try {
                        log.error("request sent to {} returned: {}",
                                HttpClient.getRemoteHostAddress(baseUrl), readResponseEntity(response, String.class));
                    } catch (FlowStoreServiceConnectorException fssce) {
                        log.warn("Unable to extract entity from response", e);
                    }
                    log.warn("Unable to extract flow-store error from response", e);
                }
            }
            throw exception;
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
        final T entity = response.readEntity(tGenericType);
        if (entity == null) {
            throw new FlowStoreServiceConnectorException(
                    String.format("flow-store service returned with null-valued %s entity", tGenericType));
        }
        return entity;
    }

    public Client getClient() {
        return failSafeHttpClient.getClient();
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    /*
     * Generate a specific ParameterizedType for use with GenericType(Type) for use with Generics
     */
    private <T> ParameterizedType createListGenericType(final Class<T> clazz) {
        return new ParameterizedType() {
            private final Type[] actualType = {clazz};

            public Type[] getActualTypeArguments() {
                return actualType;
            }

            public Type getRawType() {
                return List.class;
            }

            public Type getOwnerType() {
                return null;
            }
        };
    }
}
