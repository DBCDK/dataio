package dk.dbc.dataio.flowstore.ejb;

import dk.dbc.dataio.commons.types.exceptions.ReferencedEntityNotFoundException;
import dk.dbc.dataio.commons.types.rest.FlowBinderFlowQuery;
import dk.dbc.dataio.commons.types.rest.FlowStoreServiceConstants;
import dk.dbc.dataio.commons.utils.invariant.InvariantUtil;
import dk.dbc.dataio.commons.utils.json.JsonException;
import dk.dbc.dataio.commons.utils.json.JsonUtil;
import dk.dbc.dataio.commons.utils.service.ServiceUtil;
import dk.dbc.dataio.flowstore.entity.Flow;
import dk.dbc.dataio.flowstore.entity.FlowBinder;
import dk.dbc.dataio.flowstore.entity.FlowBinderSearchIndexEntry;
import dk.dbc.dataio.flowstore.entity.Sink;
import dk.dbc.dataio.flowstore.entity.Submitter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceException;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static dk.dbc.dataio.flowstore.util.ServiceUtil.getResourceUriOfVersionedEntity;

/**
 * This Enterprise Java Bean (EJB) class acts as a JAX-RS root resource exposed
 * by the '/FlowStoreServiceConstants.FLOW_BINDERS' entry point
 */
@Stateless
@Path("/")
public class FlowBindersBean {

    private static final Logger log = LoggerFactory.getLogger(FlowBindersBean.class);
    private static final String FLOW_BINDER_CONTENT_DISPLAY_TEXT = "flowBinderContent";
    private static final String NOT_FOUND_MESSAGE = "resource not found";

    @PersistenceContext
    EntityManager entityManager;

    /**
     * Retrieves a flow binder from underlying data store through a named query retrieving data
     * from table: flow_binders_search_index
     *
     * @param packaging set for the flow binder
     * @param format set for the flow binder
     * @param charset set for the flow binder
     * @param submitter_number to identify the referenced submitter
     * @param destination set for the flow binder
     *
     * @return
     * a HTTP 200 OK response flow binder content as JSON,
     * a HTTP 404 NOT_FOUND response if flow binder is not found
     * a HTTP 500 INTERNAL_SERVER_ERROR response in case of general error.
     *
     * @throws JsonException when given invalid (null-valued, empty-valued or
     * non-json) JSON string
     */
    @GET
    @Path(FlowStoreServiceConstants.FLOW_BINDER_RESOLVE)
    @Produces({MediaType.APPLICATION_JSON})
    @SuppressWarnings("unchecked")
    public Response getFlowBinder(@QueryParam(FlowBinderFlowQuery.REST_PARAMETER_PACKAGING) String packaging,
            @QueryParam(FlowBinderFlowQuery.REST_PARAMETER_FORMAT) String format,
            @QueryParam(FlowBinderFlowQuery.REST_PARAMETER_CHARSET) String charset,
            @QueryParam(FlowBinderFlowQuery.REST_PARAMETER_SUBMITTER) Long submitter_number,
            @QueryParam(FlowBinderFlowQuery.REST_PARAMETER_DESTINATION) String destination) throws JsonException {

        InvariantUtil.checkNotNullNotEmptyOrThrow(packaging, FlowBinderFlowQuery.REST_PARAMETER_PACKAGING);
        InvariantUtil.checkNotNullNotEmptyOrThrow(format, FlowBinderFlowQuery.REST_PARAMETER_FORMAT);
        InvariantUtil.checkNotNullNotEmptyOrThrow(charset, FlowBinderFlowQuery.REST_PARAMETER_CHARSET);
        InvariantUtil.checkNotNullOrThrow(submitter_number, FlowBinderFlowQuery.REST_PARAMETER_SUBMITTER);
        InvariantUtil.checkNotNullNotEmptyOrThrow(destination, FlowBinderFlowQuery.REST_PARAMETER_DESTINATION);

        Query query = entityManager.createNamedQuery(FlowBinder.QUERY_FIND_FLOWBINDER);
        try {
            query.setParameter(FlowBinder.DB_QUERY_PARAMETER_PACKAGING, packaging);
            query.setParameter(FlowBinder.DB_QUERY_PARAMETER_FORMAT, format);
            query.setParameter(FlowBinder.DB_QUERY_PARAMETER_CHARSET, charset);
            query.setParameter(FlowBinder.DB_QUERY_PARAMETER_SUBMITTER, submitter_number);
            query.setParameter(FlowBinder.DB_QUERY_PARAMETER_DESTINATION, destination);
        } catch (IllegalArgumentException ex) {
            String errMsg = String.format("Error while setting parameters for database query: %s", ex.getMessage());
            log.warn(errMsg, ex);
            return ServiceUtil.buildResponse(Response.Status.NOT_FOUND, ServiceUtil.asJsonError(errMsg));
        }

        List<FlowBinder> flowBinders = query.getResultList();
        if (flowBinders.isEmpty()) {
            String msg = getNoFlowFoundMessage(query);
            log.info(msg);
            return ServiceUtil.buildResponse(Response.Status.NOT_FOUND, ServiceUtil.asJsonError(msg));
        }
        if(flowBinders.size() > 1) {
            String msg = getMoreThanOneFlowFoundMessage(query);
            log.warn(msg);
        }
        return ServiceUtil.buildResponse(Response.Status.OK, JsonUtil.toJson(flowBinders.get(0)));
    }

    /**
     * Creates new flow binder with data POST'ed as JSON and persists it in the
     * underlying data store.
     *
     * Note: this method updates multiple database tables assuming transactional
     * integrity
     *
     * @param uriInfo application and request URI information
     * @param flowBinderContent flow binder data as JSON string
     *
     * @return
     * a HTTP 201 CREATED response with a Location header containing the URL value of the newly created resource,
     * a HTTP 400 BAD_REQUEST response on invalid json content,
     * a HTTP 406 NOT_ACCEPTABLE response if violating any uniqueness constraints,
     * a HTTP 412 PRECONDITION_FAILED if a referenced submitter or flow no longer exists,
     * a HTTP 500 INTERNAL_SERVER_ERROR response in case of general error.
     *
     * @throws JsonException when given invalid (null-valued, empty-valued or
     * non-json) JSON string, or if JSON object does not comply with model
     * schema
     * @throws ReferencedEntityNotFoundException when unable to resolve any
     * attached flow or submitters
     */
    @POST
    @Path(FlowStoreServiceConstants.FLOW_BINDERS)
    @Consumes({MediaType.APPLICATION_JSON})
    @Produces({MediaType.APPLICATION_JSON})
    public Response createFlowBinder(@Context UriInfo uriInfo, String flowBinderContent) throws JsonException, ReferencedEntityNotFoundException {
        log.trace("Called with: '{}'", flowBinderContent);
        InvariantUtil.checkNotNullNotEmptyOrThrow(flowBinderContent, FLOW_BINDER_CONTENT_DISPLAY_TEXT);
        /* ATTENTION:
         Below we rely on the transactional integrity provided by the underlying relational
         database system and Java EE, so that if the persisting of a search index entry fails
         the persisted flow binder will be automatically rolled back. This will have to be
         handled differently in case the underlying data store no longer supports transactions.
         */

        /* We set the JSON content for a new FlowBinder instance causing the IDs of referenced
         flow, sink and submitters to be made available. We then resolve these references into
         entities and attaches them to the flow binder causing foreign key relations to be
         created. Finally we generate the search index entries generated by this flow binder
         and persists them in the data store.
         */

        FlowBinder flowBinder = new FlowBinder();
        flowBinder.setContent(flowBinderContent);
        flowBinder.setFlow(resolveFlow(flowBinder.getFlowId()));
        flowBinder.setSink(resolveSink(flowBinder.getSinkId()));
        flowBinder.setSubmitters(resolveSubmitters(flowBinder.getSubmitterIds()));

        entityManager.persist(flowBinder);

        for (FlowBinderSearchIndexEntry searchIndexEntry : FlowBinder.generateSearchIndexEntries(flowBinder)) {
            entityManager.persist(searchIndexEntry);
        }

        entityManager.flush();

        final String flowBinderJson = JsonUtil.toJson(flowBinder);
        return Response
                .created(getResourceUriOfVersionedEntity(uriInfo.getAbsolutePathBuilder(), flowBinder))
                .entity(flowBinderJson)
                .tag(flowBinder.getVersion().toString())
                .build();
    }

    /**
     * Updates an existing flow binder with data POST'ed as JSON and persists it in the
     * underlying data store.
     *
     * Note: this method updates multiple database tables assuming transactional
     * integrity
     *
     * @param flowBinderContent flow binder data as JSON string
     * @param id identifying the flow binder in the underlying data store
     * @param version the current version of the persisted flow binder
     *
     * @return
     * a HTTP 200 OK response flow binder content as JSON,
     * a HTTP 400 BAD_REQUEST response on invalid json content,
     * a HTTP 404 NOT_FOUND response if flow binder is not found
     * a HTTP 406 NOT_ACCEPTABLE response if violating any uniqueness constraints,
     * a HTTP 409 response in case of Concurrent Update error
     * a HTTP 412 PRECONDITION_FAILED on failure to locate one or more of the referenced objects
     * a HTTP 500 INTERNAL_SERVER_ERROR response in case of general error.
     *
     * @throws JsonException when given invalid (null-valued, empty-valued or
     * non-json) JSON string, or if JSON object does not comply with model
     * schema
     *
     * @throws ReferencedEntityNotFoundException if one or more of the referenced entities was not found
     */
    @POST
    @Path(FlowStoreServiceConstants.FLOW_BINDER_CONTENT)
    @Produces({MediaType.APPLICATION_JSON})
    @Consumes({MediaType.APPLICATION_JSON})
    public Response updateFlowBinder(String flowBinderContent,
                                     @PathParam(FlowStoreServiceConstants.FLOW_BINDER_ID_VARIABLE) Long id,
                                     @HeaderParam(FlowStoreServiceConstants.IF_MATCH_HEADER) Long version) throws JsonException, ReferencedEntityNotFoundException {

        log.trace("called with: '{}'", flowBinderContent);
        InvariantUtil.checkNotNullNotEmptyOrThrow(flowBinderContent, FLOW_BINDER_CONTENT_DISPLAY_TEXT);

        // Retrieve the existing flow binder
        final FlowBinder flowBinderEntity = entityManager.find(FlowBinder.class, id);
        if (flowBinderEntity == null) {
            return buildResponseNotFound(String.format("Error retrieving existing flow binder with id; %s", id));
        }
        // Delete the existing search indexes
        Response response = findAndDeleteSearchIndexesForFlowBinder(flowBinderEntity.getId());
        if (response != null) {
            return response;
        }
        // Update the flow binder
        updateFlowBinderEntity(flowBinderEntity, flowBinderContent, version);

        // Create new search indexes for the flow binder
        createSearchIndexesForFlowBinder(flowBinderEntity);

        // Retrieve the updated flow binder
        final FlowBinder updatedFlowBinderEntity = entityManager.find(FlowBinder.class, id);

        // Return the updated flow binder
        String updatedFlowBinderEntityJson = JsonUtil.toJson(updatedFlowBinderEntity);
        return Response
                .ok()
                .entity(updatedFlowBinderEntityJson)
                .tag(updatedFlowBinderEntity.getVersion().toString())
                .build();
    }

    /**
     * Returns list of stored flow binders sorted by name in ascending order
     *
     * @return a HTTP OK response with result list as JSON
     *
     * @throws JsonException on failure to create result list as JSON
     */
    @GET
    @Path(FlowStoreServiceConstants.FLOW_BINDERS)
    @Produces({ MediaType.APPLICATION_JSON })
    public Response findAllFlowBinders() throws JsonException {
        final TypedQuery<dk.dbc.dataio.commons.types.FlowBinder> query = entityManager.createNamedQuery(FlowBinder.QUERY_FIND_ALL, dk.dbc.dataio.commons.types.FlowBinder.class);
        final List<dk.dbc.dataio.commons.types.FlowBinder> results = query.getResultList();
        return ServiceUtil.buildResponse(Response.Status.OK, JsonUtil.toJson(results));
    }

    /**
     * Retrieves flow binder from underlying data store
     *
     * @param id flow binder identifier
     *
     * @return a HTTP 200 response with flow binder as JSON,
     *         a HTTP 404 response with error content as JSON if not found,
     *         a HTTP 500 response in case of general error.
     *
     * @throws JsonException if unable to marshall value type into its JSON representation
     */
    @GET
    @Path(FlowStoreServiceConstants.FLOW_BINDER)
    @Produces({ MediaType.APPLICATION_JSON })
    public Response getFlowBinderById(@PathParam(FlowStoreServiceConstants.FLOW_BINDER_ID_VARIABLE) Long id) throws JsonException {
        final FlowBinder flowBinder = entityManager.find(FlowBinder.class, id);
        if (flowBinder == null) {
            return ServiceUtil.buildResponse(Response.Status.NOT_FOUND, ServiceUtil.asJsonError(NOT_FOUND_MESSAGE));
        }
        return ServiceUtil.buildResponse(Response.Status.OK, JsonUtil.toJson(flowBinder));
    }


     // Private methods


    /**
     * Builds a response with status not found and containing the errorMessage given as input
     * @param errorMessage the specific error message
     * @return Response.Status.NOT_FOUND
     */
    private Response buildResponseNotFound (String errorMessage) {
        return Response
                .status(Response.Status.NOT_FOUND)
                .entity(ServiceUtil.asJsonError(errorMessage))
                .build();
    }

    /**
     * This method locates and deletes all search indexes for a given flow binder
     * @param flowBinderId identifying which flowBinderSearchIndexEntries should be deleted
     * @return response, null if all went well
     */
    @SuppressWarnings("unchecked")
    private Response findAndDeleteSearchIndexesForFlowBinder(Long flowBinderId) {
        Response response = null;

        // Create named query
        Query query = entityManager.createNamedQuery(FlowBinder.QUERY_FIND_ALL_SEARCH_INDEXES_FOR_FLOWBINDER);
        try {
            query.setParameter(FlowBinder.DB_QUERY_PARAMETER_FLOWBINDER, flowBinderId);
        } catch (IllegalArgumentException e) {
            String errMsg = String.format("Error while setting parameters for database query: %s", e.getMessage());
            log.warn(errMsg, e);
            response = buildResponseNotFound(errMsg);
        }

        if(query.getResultList().isEmpty()) {
            log.warn(getNoFlowBinderSearchIndexEntryFoundMessage(query));
        }
        // Extract the results
        List<FlowBinderSearchIndexEntry> existingSearchIndexEntries = query.getResultList();

        // Delete the existing search indexes
        for(FlowBinderSearchIndexEntry flowBinderSearchIndexEntry : existingSearchIndexEntries) {
            entityManager.remove(flowBinderSearchIndexEntry);
        }
        return response;
    }

    /**
     * This method creates new search index entries for the flow binder given as input
     * @param flowBinderEntity, the flow binder to create new search indexes for
     *
     * @throws PersistenceException if a JsonException is thrown while generating the new SearchIndexEntries.
     * The conversion to an unchecked exception is to ensure that a transaction rollback is performed.
     */
    private void createSearchIndexesForFlowBinder (FlowBinder flowBinderEntity) throws PersistenceException {
        try {
            for (FlowBinderSearchIndexEntry newSearchIndexEntry : FlowBinder.generateSearchIndexEntries(flowBinderEntity)) {
                entityManager.persist(newSearchIndexEntry);
            }
        } catch (JsonException e) {
            throw new PersistenceException("flow binder contains invalid JSON content", e.getCause());
        }
    }

    /**
     * Updates the flow binder entity
     * @param flowBinderEntity the currently persisted flow binder entity
     * @param flowBinderContentString the new flow binder content as String
     * @param version the current version of the flow binder
     * @throws PersistenceException if the objects referenced by the flow binder, could not be resolved
     */
    private void updateFlowBinderEntity(FlowBinder flowBinderEntity, String flowBinderContentString, long version) throws JsonException, ReferencedEntityNotFoundException {
            entityManager.detach(flowBinderEntity);
            flowBinderEntity.setContent(flowBinderContentString);
            flowBinderEntity.setVersion(version);
            flowBinderEntity.setFlow(resolveFlow(flowBinderEntity.getFlowId()));
            flowBinderEntity.setSink(resolveSink(flowBinderEntity.getSinkId()));
            flowBinderEntity.setSubmitters(resolveSubmitters(flowBinderEntity.getSubmitterIds()));
            entityManager.merge(flowBinderEntity);
            entityManager.flush();
    }

    /**
     * Resolves each submitter referenced in given set by looking up the
     * corresponding submitter entity in the data store
     *
     * @param submitterIds set of submitter identifiers
     * @return set of submitter entities
     * @throws ReferencedEntityNotFoundException if unable to find a referenced
     * submitter entity in the data store
     */
    private Set<Submitter> resolveSubmitters(Set<Long> submitterIds) throws ReferencedEntityNotFoundException {
        final Set<Submitter> submitters = new HashSet<>(submitterIds.size());
        for (Long submitterId : submitterIds) {
            log.trace("Looking up Submitter entity for ID {}", submitterId);
            final Submitter submitter = entityManager.find(Submitter.class, submitterId);
            if (submitter == null) {
                throw new ReferencedEntityNotFoundException(String.format("Submitter(%d)", submitterId));
            }
            submitters.add(submitter);
        }
        log.debug("Resolved {} submitters from '{}' field", submitters.size(), FlowBinder.SUBMITTER_IDS_FIELD);
        return submitters;
    }

    /**
     * Resolves flow referenced by given id by looking up the corresponding flow
     * entity in the data store
     *
     * @param flowId flow identifier
     * @return flow entity
     * @throws ReferencedEntityNotFoundException if unable to find the
     * referenced flow entity in the data store
     */
    private Flow resolveFlow(Long flowId) throws ReferencedEntityNotFoundException {
        log.trace("Looking up Flow entity for ID {}", flowId);
        final Flow flow = entityManager.find(Flow.class, flowId);
        if (flow == null) {
            throw new ReferencedEntityNotFoundException(String.format("Flow(%d)", flowId));
        }
        return flow;
    }

    /**
     * Resolves sink referenced by given id by looking up the corresponding sink
     * entity in the data store
     *
     * @param sinkId sink identifier
     * @return sink entity
     * @throws ReferencedEntityNotFoundException if unable to find the
     * referenced sink entity in the data store
     */
    private Sink resolveSink(Long sinkId) throws ReferencedEntityNotFoundException {
        log.trace("Looking up Sink entity for ID {}", sinkId);
        final Sink sink = entityManager.find(Sink.class, sinkId);
        if (sink == null) {
            throw new ReferencedEntityNotFoundException(String.format("Sink(%d)", sinkId));
        }
        return sink;
    }

    private String getNoFlowFoundMessage(Query query) {
        return getQueryParametersStringify("No flows found for query with parameters", query);
    }

    private String getMoreThanOneFlowFoundMessage(Query query) {
        return getQueryParametersStringify("More than one result was found for the query with parameters", query);
    }

    private String getNoFlowBinderSearchIndexEntryFoundMessage(Query query) {
        return getQueryParametersStringify("No flowBinderSearchIndexEntry found for query with parameters", query);
    }

    private String getQueryParametersStringify(String message, Query query) {
        return String.format("%s: '%s'='%s' '%s'='%s' '%s'='%s' '%s'='%s' '%s'='%s'",
                message,
                FlowBinder.DB_QUERY_PARAMETER_PACKAGING, query.getParameterValue(FlowBinder.DB_QUERY_PARAMETER_PACKAGING),
                FlowBinder.DB_QUERY_PARAMETER_FORMAT, query.getParameterValue(FlowBinder.DB_QUERY_PARAMETER_FORMAT),
                FlowBinder.DB_QUERY_PARAMETER_CHARSET, query.getParameterValue(FlowBinder.DB_QUERY_PARAMETER_CHARSET),
                FlowBinder.DB_QUERY_PARAMETER_SUBMITTER, query.getParameterValue(FlowBinder.DB_QUERY_PARAMETER_SUBMITTER),
                FlowBinder.DB_QUERY_PARAMETER_DESTINATION, query.getParameterValue(FlowBinder.DB_QUERY_PARAMETER_DESTINATION));
    }

}
