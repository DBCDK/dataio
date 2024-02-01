package dk.dbc.dataio.flowstore.ejb;

import dk.dbc.commons.jsonb.JSONBContext;
import dk.dbc.commons.jsonb.JSONBException;
import dk.dbc.dataio.commons.types.FlowBinderContent;
import dk.dbc.dataio.commons.types.FlowStoreError;
import dk.dbc.dataio.commons.types.rest.FlowBinderResolveQuery;
import dk.dbc.dataio.commons.types.rest.FlowStoreServiceConstants;
import dk.dbc.dataio.commons.utils.service.ServiceUtil;
import dk.dbc.dataio.flowstore.entity.FlowBinder;
import dk.dbc.dataio.flowstore.entity.Submitter;
import dk.dbc.dataio.flowstore.model.FlowBinderContentMatch;
import dk.dbc.dataio.flowstore.model.FlowBindersResolved;
import dk.dbc.dataio.querylanguage.DataIOQLParser;
import dk.dbc.dataio.querylanguage.ParseException;
import dk.dbc.invariant.InvariantUtil;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.Query;
import jakarta.persistence.TypedQuery;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static jakarta.ws.rs.core.Response.Status.BAD_REQUEST;
import static jakarta.ws.rs.core.Response.Status.NOT_ACCEPTABLE;
import static jakarta.ws.rs.core.Response.Status.NOT_FOUND;
import static jakarta.ws.rs.core.Response.Status.PRECONDITION_FAILED;

/**
 * This Enterprise Java Bean (EJB) class acts as a JAX-RS root resource exposed
 * by the '/binders' entry point
 */
@Stateless
@Path("/")
public class FlowBindersBean extends AbstractResourceBean {

    private static final Logger LOGGER = LoggerFactory.getLogger(FlowBindersBean.class);
    private static final String FLOW_BINDER_CONTENT_DISPLAY_TEXT = "flowBinderContent";
    private static final String EMPTY_ENTITY = "";
    JSONBContext jsonbContext = new JSONBContext();
    @Inject
    private SinksBean sinksBean;
    @Inject
    private SubmittersBean submittersBean;
    @Inject
    private FlowsBean flowsBean;

    @PersistenceContext
    EntityManager entityManager;

    /**
     * Resolves a flow binder given key parameters
     *
     * @param packaging       set for the flow binder
     * @param format          set for the flow binder
     * @param charset         set for the flow binder
     * @param submitterNumber to identify the referenced submitter
     * @param destination     set for the flow binder
     * @return a HTTP 200 OK response flow binder content as JSON,
     * a HTTP 404 NOT_FOUND response if flow binder is not found
     * a HTTP 409 CONFLICT response if multiple flow binders were resolved
     * a HTTP 500 INTERNAL_SERVER_ERROR response in case of general error.
     * @throws JSONBException on invalid json retrieved from data store
     */
    @GET
    @Path(FlowStoreServiceConstants.FLOW_BINDER_RESOLVE)
    @Produces({MediaType.APPLICATION_JSON})
    public Response resolveFlowBinder(@QueryParam(FlowBinderResolveQuery.REST_PARAMETER_PACKAGING) String packaging,
                                      @QueryParam(FlowBinderResolveQuery.REST_PARAMETER_FORMAT) String format,
                                      @QueryParam(FlowBinderResolveQuery.REST_PARAMETER_CHARSET) String charset,
                                      @QueryParam(FlowBinderResolveQuery.REST_PARAMETER_SUBMITTER) Long submitterNumber,
                                      @QueryParam(FlowBinderResolveQuery.REST_PARAMETER_DESTINATION) String destination)
            throws JSONBException {

        List<Long> submitterNumbers = null;
        if (submitterNumber != null) {
            submitterNumbers = Collections.singletonList(submitterNumber);
        }

        final FlowBinderContentMatch flowBinderContentMatch =
                getContentMatch(charset, destination, format, packaging, submitterNumbers);

        final List<FlowBinder> flowBinders = matchFlowBinder(flowBinderContentMatch);

        if (flowBinders.size() > 1) {
            return Response.status(Response.Status.CONFLICT)
                    .entity(String.format("More than one result was found matching flow binder with parameters: %s",
                            flowBinderContentMatch.toString())).build();
        }

        if (flowBinders.isEmpty()) {
            final FlowStoreError flowStoreError = getFlowBinderResolveError(submitterNumber, flowBinderContentMatch);
            // Return NOT_FOUND response with the FlowStoreError as entity
            return Response.status(NOT_FOUND).entity(jsonbContext.marshall(flowStoreError)).build();
        }
        return Response.ok().entity(jsonbContext.marshall(flowBinders.get(0))).build();
    }

    /**
     * Creates new flow binder with data POST'ed as JSON and persists it in the
     * underlying data store.
     *
     * @param uriInfo           application and request URI information
     * @param flowBinderContent flow binder data as JSON string
     * @return a HTTP 201 CREATED response with a Location header containing the URL value of the newly created resource,
     * a HTTP 400 BAD_REQUEST response on invalid json content,
     * a HTTP 406 NOT_ACCEPTABLE response if violating any uniqueness constraints,
     * a HTTP 412 PRECONDITION_FAILED if a referenced submitter or flow no longer exists,
     * a HTTP 500 INTERNAL_SERVER_ERROR response in case of general error.
     * @throws JSONBException when given invalid (null-valued, empty-valued or
     *                        non-json) JSON string, or if JSON object does not comply with model
     *                        schema
     */
    @POST
    @Path(FlowStoreServiceConstants.FLOW_BINDERS)
    @Consumes({MediaType.APPLICATION_JSON})
    @Produces({MediaType.APPLICATION_JSON})
    public Response createFlowBinder(@Context UriInfo uriInfo, String flowBinderContent) throws JSONBException {
        LOGGER.trace("Called with: '{}'", flowBinderContent);
        InvariantUtil.checkNotNullNotEmptyOrThrow(flowBinderContent, FLOW_BINDER_CONTENT_DISPLAY_TEXT);

        final FlowBinderContent content = jsonbContext.unmarshall(flowBinderContent, FlowBinderContent.class);

        final List<Long> unknownSubmitters = findUnknownSubmitters(content.getSubmitterIds());
        if (!unknownSubmitters.isEmpty()) {
            return Response.status(PRECONDITION_FAILED)
                    .entity(String.format("flow binder references unknown submitters: %s", unknownSubmitters))
                    .build();
        }

        // Test for uniqueness for each submitter
        for (long submitterId : content.getSubmitterIds()) {
            final FlowBinderContentMatch flowBinderContentMatch = new FlowBinderContentMatch()
                    .withCharset(content.getCharset())
                    .withDestination(content.getDestination())
                    .withFormat(content.getFormat())
                    .withPackaging(content.getPackaging())
                    .withSubmitterIds(Collections.singletonList(submitterId));

            final List<FlowBinder> flowBinders = matchFlowBinder(flowBinderContentMatch);
            if (!flowBinders.isEmpty()) {
                return Response.status(NOT_ACCEPTABLE).entity("Flow binder search keys already exists").build();
            }
        }

        /* We set the JSON content for a new FlowBinder instance causing the IDs of referenced
           flow and sink to be made available. */

        FlowBinder flowBinder = new FlowBinder();
        flowBinder.setContent(flowBinderContent);

        entityManager.persist(flowBinder);
        entityManager.flush();

        final String flowBinderJson = jsonbContext.marshall(flowBinder);
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
     * @param flowBinderContent flow binder data as JSON string
     * @param id                identifying the flow binder in the underlying data store
     * @param version           the current version of the persisted flow binder
     * @return a HTTP 200 OK response flow binder content as JSON,
     * a HTTP 400 BAD_REQUEST response on invalid json content,
     * a HTTP 404 NOT_FOUND response if flow binder is not found
     * a HTTP 406 NOT_ACCEPTABLE response if violating any uniqueness constraints,
     * a HTTP 409 response in case of Concurrent Update error
     * a HTTP 412 PRECONDITION_FAILED on failure to locate one or more of the referenced objects
     * a HTTP 500 INTERNAL_SERVER_ERROR response in case of general error.
     * @throws JSONBException when given invalid (null-valued, empty-valued or
     *                        non-json) JSON string, or if JSON object does not comply with model
     *                        schema
     */
    @POST
    @Path(FlowStoreServiceConstants.FLOW_BINDER_CONTENT)
    @Produces({MediaType.APPLICATION_JSON})
    @Consumes({MediaType.APPLICATION_JSON})
    public Response updateFlowBinder(String flowBinderContent,
                                     @PathParam(FlowStoreServiceConstants.ID_VARIABLE) Long id,
                                     @HeaderParam(FlowStoreServiceConstants.IF_MATCH_HEADER) Long version)
            throws JSONBException {

        LOGGER.trace("called with: '{}'", flowBinderContent);
        InvariantUtil.checkNotNullNotEmptyOrThrow(flowBinderContent, FLOW_BINDER_CONTENT_DISPLAY_TEXT);

        final FlowBinderContent content = jsonbContext.unmarshall(flowBinderContent, FlowBinderContent.class);

        final List<Long> unknownSubmitters = findUnknownSubmitters(content.getSubmitterIds());
        if (!unknownSubmitters.isEmpty()) {
            return Response.status(PRECONDITION_FAILED)
                    .entity(String.format("flow binder references unknown submitters: %s", unknownSubmitters))
                    .build();
        }

        // Test for uniqueness for each submitter
        for (long submitterId : content.getSubmitterIds()) {
            final FlowBinderContentMatch flowBinderContentMatch = new FlowBinderContentMatch()
                    .withCharset(content.getCharset())
                    .withDestination(content.getDestination())
                    .withFormat(content.getFormat())
                    .withPackaging(content.getPackaging())
                    .withSubmitterIds(Collections.singletonList(submitterId));

            final List<FlowBinder> flowBinders = matchFlowBinder(flowBinderContentMatch);
            if (!flowBinders.isEmpty()
                    && (flowBinders.size() > 1 || !flowBinders.get(0).getId().equals(id))) {
                return Response.status(NOT_ACCEPTABLE).entity("Flow binder search keys already exists").build();
            }
        }

        // Retrieve the existing flow binder
        final FlowBinder flowBinderEntity = entityManager.find(FlowBinder.class, id);
        if (flowBinderEntity == null) {
            return Response.status(NOT_FOUND).entity(EMPTY_ENTITY).build();
        }

        // Update the flow binder
        updateFlowBinderEntity(flowBinderEntity, flowBinderContent, version);

        // Retrieve the updated flow binder
        final FlowBinder updatedFlowBinderEntity = entityManager.find(FlowBinder.class, id);

        // Return the updated flow binder
        String updatedFlowBinderEntityJson = jsonbContext.marshall(updatedFlowBinderEntity);
        return Response
                .ok()
                .entity(updatedFlowBinderEntityJson)
                .tag(updatedFlowBinderEntity.getVersion().toString())
                .build();
    }

    /**
     * Deletes an existing flow binder
     *
     * @param flowBinderId The flow binder ID
     * @param version      The version of the flow binder
     * @return a HTTP 204 response with no content,
     * a HTTP 404 response in case of flow binder ID not found,
     * a HTTP 409 response in case an OptimisticLock or Constraint violation occurs,
     * a HTTP 500 response in case of general error.
     */
    @DELETE
    @Path(FlowStoreServiceConstants.FLOW_BINDER)
    public Response deleteFlowBinder(
            @PathParam(FlowStoreServiceConstants.ID_VARIABLE) Long flowBinderId,
            @HeaderParam(FlowStoreServiceConstants.IF_MATCH_HEADER) Long version) {

        final FlowBinder flowBinderEntity = entityManager.find(FlowBinder.class, flowBinderId);

        if (flowBinderEntity == null) {
            return Response.status(NOT_FOUND).entity(EMPTY_ENTITY).build();
        }

        // First we need to update the version no to see if any Optimistic Locking occurs!
        entityManager.detach(flowBinderEntity);
        flowBinderEntity.setVersion(version);
        FlowBinder versionUpdatedAndNoOptimisticLocking = entityManager.merge(flowBinderEntity);

        // If no Optimistic Locking - delete it!

        entityManager.remove(versionUpdatedAndNoOptimisticLocking);
        entityManager.flush();

        return Response.noContent().build();
    }

    /**
     * Returns list of stored flow binders sorted by name in ascending order
     *
     * @return a HTTP OK response with result list as JSON
     * @throws JSONBException on failure to create result list as JSON
     */
    @GET
    @Path(FlowStoreServiceConstants.FLOW_BINDERS)
    @Produces({MediaType.APPLICATION_JSON})
    public Response findAllFlowBinders() throws JSONBException {
        return Response.ok().entity(jsonbContext.marshall(findFlowBinders())).build();
    }

    private List<FlowBinder> findFlowBinders() {
        TypedQuery<FlowBinder> query = entityManager.createNamedQuery(FlowBinder.FIND_ALL_QUERY_NAME, FlowBinder.class);
        return query.getResultList();
    }

    @GET
    @Path(FlowStoreServiceConstants.FLOW_BINDERS_RESOLVED)
    @Produces({MediaType.APPLICATION_JSON})
    public Response findAllResolvedFlowBinders() throws JSONBException {
        List<FlowBindersResolved> resolvedList = findFlowBinders().stream().map(fb -> FlowBindersResolved.from(fb, sinksBean::getSinkName, submittersBean::getSubmitterName, flowsBean::getFlowName)).collect(Collectors.toList());
        return Response.ok().entity(jsonbContext.marshall(resolvedList)).build();
    }

    /**
     * Retrieves flow binder from underlying data store
     *
     * @param id flow binder identifier
     * @return a HTTP 200 response with flow binder as JSON,
     * a HTTP 404 response with error content as JSON if not found,
     * a HTTP 500 response in case of general error.
     * @throws JSONBException if unable to marshall value type into its JSON representation
     */
    @GET
    @Path(FlowStoreServiceConstants.FLOW_BINDER)
    @Produces({MediaType.APPLICATION_JSON})
    public Response getFlowBinderById(@PathParam(FlowStoreServiceConstants.ID_VARIABLE) Long id) throws JSONBException {
        final FlowBinder flowBinder = entityManager.find(FlowBinder.class, id);

        if (flowBinder == null) {
            return Response.status(NOT_FOUND).entity(EMPTY_ENTITY).build();
        }
        return Response.ok().entity(jsonbContext.marshall(flowBinder)).build();
    }

    /**
     * Returns list of flow binders found by executing POST'ed IOQL query
     *
     * @param query IOQL query
     * @return a HTTP OK response with result list as JSON,
     * a HTTP 400 BAD_REQUEST response on invalid query expression.
     * @throws JSONBException on failure to create result list as JSON
     */
    @POST
    @Path(FlowStoreServiceConstants.FLOW_BINDERS_QUERIES)
    @Consumes({MediaType.TEXT_PLAIN})
    @Produces({MediaType.APPLICATION_JSON})
    public Response queryFlowBindersByPost(String query) throws JSONBException {
        return listFlowBindersByIOQL(query);
    }

    /**
     * Returns list of flow binders found by executing IOQL query given by the 'q' query parameter
     *
     * @param query IOQL query
     * @return a HTTP OK response with result list as JSON,
     * a HTTP 400 BAD_REQUEST response on invalid query expression.
     * @throws JSONBException on failure to create result list as JSON
     */
    @GET
    @Path(FlowStoreServiceConstants.FLOW_BINDERS_QUERIES)
    @Produces({MediaType.APPLICATION_JSON})
    public Response queryFlowBindersByGet(@QueryParam("q") String query) throws JSONBException {
        return listFlowBindersByIOQL(query);
    }

    private Response listFlowBindersByIOQL(String query) throws JSONBException {
        LOGGER.info("listFlowBindersByIOQL(query): {}", query);
        try {
            final DataIOQLParser dataIOQLParser = new DataIOQLParser();
            final String sql = dataIOQLParser.parse(query);
            final Query q = entityManager.createNativeQuery(sql, FlowBinder.class);
            return Response.ok().entity(jsonbContext.marshall(q.getResultList())).build();
        } catch (RuntimeException | ParseException e) {
            return Response.status(BAD_REQUEST)
                    .entity(jsonbContext.marshall(
                            new FlowStoreError(
                                    FlowStoreError.Code.INVALID_QUERY,
                                    "Unable to process query: " + query,
                                    ServiceUtil.stackTraceToString(e))))
                    .build();
        }
    }

    /**
     * Finds specific cause of failure to resolve a flow binder
     *
     * @param submitterNumber submitter number
     * @param requestedMatch  match parameters of the resolve request
     * @return error containing the appropriate error message
     */
    private FlowStoreError getFlowBinderResolveError(Long submitterNumber, FlowBinderContentMatch requestedMatch) {
        if (requestedMatch.getSubmitterIds() == null || requestedMatch.getSubmitterIds().isEmpty()) {
            return new FlowStoreError(
                    FlowStoreError.Code.NONEXISTING_SUBMITTER,
                    "Intet biblioteksnummer angivet",
                    "");
        }

        final FlowBinderContentMatch submitterMatch = new FlowBinderContentMatch()
                .withSubmitterIds(requestedMatch.getSubmitterIds());

        final List<FlowBinder> flowBindersMatchedBySubmitter = entityManager
                .createNamedQuery(FlowBinder.MATCH_FLOWBINDER_QUERY_NAME, FlowBinder.class)
                .setParameter(1, submitterMatch.toString())
                .getResultList();

        if (flowBindersMatchedBySubmitter.isEmpty()) {
            return new FlowStoreError(
                    FlowStoreError.Code.NONEXISTING_SUBMITTER,
                    String.format("Biblioteksnummer %s kan ikke findes", submitterNumber),
                    "");
        }

        for (FlowBinder flowBinder : flowBindersMatchedBySubmitter) {
            try {
                final FlowBinderContent flowBinderContent = jsonbContext.unmarshall(
                        flowBinder.getContent(), FlowBinderContent.class);
                if (flowBinderContent.getDestination().equals(requestedMatch.getDestination())) {
                    // In the case of flow binder found for given submitter and destination
                    // but without one or more of the remaining values.
                    return new FlowStoreError(
                            FlowStoreError.Code.EXISTING_SUBMITTER_EXISTING_DESTINATION_NONEXISTING_TOC,
                            String.format("Én eller flere af de angivne værdier protokol(t): %s, format(o): %s, tegnsæt(c): %s," +
                                            "kan ikke findes i kombination med biblioteksnummer %s og baseparameter %s",
                                    requestedMatch.getPackaging(),
                                    requestedMatch.getFormat(),
                                    requestedMatch.getCharset(),
                                    submitterNumber,
                                    requestedMatch.getDestination()),
                            "");
                }
            } catch (JSONBException e) {
                throw new IllegalStateException(e);
            }
        }

        // In the case of flow binder found for submitter but without the given destination.
        return new FlowStoreError(
                FlowStoreError.Code.EXISTING_SUBMITTER_NONEXISTING_DESTINATION,
                String.format("Baseparameteren %s kan ikke findes i kombination med biblioteksnummer %s",
                        requestedMatch.getDestination(), submitterNumber),
                "");
    }

    /**
     * Updates the flow binder entity
     *
     * @param flowBinderEntity        the currently persisted flow binder entity
     * @param flowBinderContentString the new flow binder content as String
     * @param version                 the current version of the flow binder
     * @throws PersistenceException if the objects referenced by the flow binder, could not be resolved
     */
    private void updateFlowBinderEntity(FlowBinder flowBinderEntity, String flowBinderContentString, long version)
            throws JSONBException {
        entityManager.detach(flowBinderEntity);
        flowBinderEntity.setContent(flowBinderContentString);
        flowBinderEntity.setVersion(version);
        entityManager.merge(flowBinderEntity);
        entityManager.flush();
    }

    private FlowBinderContentMatch getContentMatch(String charset, String destination, String format, String packaging,
                                                   List<Long> submitterNumbers) {
        List<Long> submitterIds = null;
        if (submitterNumbers != null && !submitterNumbers.isEmpty()) {
            submitterIds = new ArrayList<>(submitterNumbers.size());
            for (Long submitterNumber : submitterNumbers) {
                final Submitter submitter = resolveSubmitterByNumber(submitterNumber);
                if (submitter != null) {
                    submitterIds.add(submitter.getId());
                } else {
                    submitterIds.add(0L); // Force submitter not found error message
                }
            }
            if (submitterIds.isEmpty()) {
                submitterIds = null;
            }
        }

        return new FlowBinderContentMatch()
                .withCharset(charset)
                .withDestination(destination)
                .withFormat(format)
                .withPackaging(packaging)
                .withSubmitterIds(submitterIds);
    }

    private List<FlowBinder> matchFlowBinder(FlowBinderContentMatch flowBinderContentMatch) {
        return entityManager
                .createNamedQuery(FlowBinder.MATCH_FLOWBINDER_QUERY_NAME, FlowBinder.class)
                .setParameter(1, flowBinderContentMatch.toString())
                .getResultList();
    }

    private Submitter resolveSubmitterByNumber(Long submitterNumber) {
        if (submitterNumber == null) {
            return null;
        }
        final List<Submitter> submitterList = entityManager
                .createNamedQuery(Submitter.QUERY_FIND_BY_CONTENT, Submitter.class)
                .setParameter(1, String.format("{\"number\": %d}", submitterNumber))
                .getResultList();

        if (submitterList.isEmpty()) {
            return null;
        }
        return submitterList.get(0);
    }

    private List<Long> findUnknownSubmitters(List<Long> submitterIds) {
        if (submitterIds == null) {
            return Collections.emptyList();
        }
        // Find unknown submitters using "set" difference operation
        final List<Long> listDifference = new ArrayList<>(submitterIds);
        final List<Long> knownSubmitters = entityManager
                .createNamedQuery(Submitter.QUERY_FIND_ALL_IDS, Long.class)
                .getResultList();
        listDifference.removeAll(knownSubmitters);
        return listDifference;
    }
}
