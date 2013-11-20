package dk.dbc.dataio.flowstore.ejb;

import dk.dbc.dataio.commons.types.FlowStoreServiceEntryPoint;
import dk.dbc.dataio.commons.types.exceptions.ReferencedEntityNotFoundException;
import dk.dbc.dataio.commons.types.restparameters.FlowBinderFlowQuery;
import dk.dbc.dataio.commons.utils.invariant.InvariantUtil;
import dk.dbc.dataio.commons.utils.json.JsonException;
import dk.dbc.dataio.commons.utils.json.JsonUtil;
import dk.dbc.dataio.flowstore.entity.Flow;
import dk.dbc.dataio.flowstore.entity.FlowBinder;
import dk.dbc.dataio.flowstore.entity.FlowBinderSearchIndexEntry;
import dk.dbc.dataio.flowstore.entity.Sink;
import dk.dbc.dataio.flowstore.entity.Submitter;
import dk.dbc.dataio.flowstore.util.ServiceUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * This Enterprise Java Bean (EJB) class acts as a JAX-RS root resource exposed
 * by the '/FlowStoreServiceEntryPoint.FLOW_BINDERS' entry point
 */
@Stateless
@Path(FlowStoreServiceEntryPoint.FLOW_BINDERS)
public class FlowBindersBean {

    private static final Logger log = LoggerFactory.getLogger(FlowBindersBean.class);
    @PersistenceContext
    protected EntityManager entityManager; // protected for testing purposes
    // todo: Is it ok to have EntityManager protected for testing purposes?

    @GET
    @Path("/flow")
    @Produces({MediaType.APPLICATION_JSON})
    public Response getFlow(@QueryParam(FlowBinderFlowQuery.REST_PARAMETER_PACKAGING) String packaging,
            @QueryParam(FlowBinderFlowQuery.REST_PARAMETER_FORMAT) String format,
            @QueryParam(FlowBinderFlowQuery.REST_PARAMETER_CHARSET) String charset,
            @QueryParam(FlowBinderFlowQuery.REST_PARAMETER_SUBMITTER) Long submitter_number,
            @QueryParam(FlowBinderFlowQuery.REST_PARAMETER_DESTINATION) String destination) throws JsonException {

        InvariantUtil.checkNotNullNotEmptyOrThrow(packaging, FlowBinderFlowQuery.REST_PARAMETER_PACKAGING);
        InvariantUtil.checkNotNullNotEmptyOrThrow(format, FlowBinderFlowQuery.REST_PARAMETER_FORMAT);
        InvariantUtil.checkNotNullNotEmptyOrThrow(charset, FlowBinderFlowQuery.REST_PARAMETER_CHARSET);
        InvariantUtil.checkNotNullOrThrow(submitter_number, FlowBinderFlowQuery.REST_PARAMETER_SUBMITTER);
        InvariantUtil.checkNotNullNotEmptyOrThrow(destination, FlowBinderFlowQuery.REST_PARAMETER_DESTINATION);

        Query query = entityManager.createNamedQuery(FlowBinder.QUERY_FIND_FLOW);
        try {
            query.setParameter(FlowBinder.DB_QUERY_PARAMETER_PACKAGING, packaging);
            query.setParameter(FlowBinder.DB_QUERY_PARAMETER_FORMAT, format);
            query.setParameter(FlowBinder.DB_QUERY_PARAMETER_CHARSET, charset);
            query.setParameter(FlowBinder.DB_QUERY_PARAMETER_SUBMITTER, submitter_number);
            query.setParameter(FlowBinder.DB_QUERY_PARAMETER_DESTINATION, destination);
        } catch (IllegalArgumentException ex) {
            String errMsg = String.format("Error while setting parameters for database query: %s", ex.getMessage());
            log.warn(errMsg, ex);
            return dk.dbc.dataio.commons.utils.service.ServiceUtil.buildResponse(Response.Status.NOT_FOUND, dk.dbc.dataio.commons.utils.service.ServiceUtil.asJsonError(errMsg));
        }

        List<Flow> flows = query.getResultList();
        if (flows.isEmpty()) {
            String msg = getNoFlowFoundMessage(query);
            log.info(msg);
            return dk.dbc.dataio.commons.utils.service.ServiceUtil.buildResponse(Response.Status.NOT_FOUND, dk.dbc.dataio.commons.utils.service.ServiceUtil.asJsonError(msg));
        }
        if(flows.size() > 1) {
            String msg = getMoreThanOneFlowFoundMessage(query);
            log.warn(msg);
        }
        return dk.dbc.dataio.commons.utils.service.ServiceUtil.buildResponse(Response.Status.OK, JsonUtil.toJson(flows.get(0)));
    }

    @GET
    @Path("/resolve")
    @Produces({MediaType.APPLICATION_JSON})
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
            return dk.dbc.dataio.commons.utils.service.ServiceUtil.buildResponse(Response.Status.NOT_FOUND, dk.dbc.dataio.commons.utils.service.ServiceUtil.asJsonError(errMsg));
        }

        List<FlowBinder> flowBinders = query.getResultList();
        if (flowBinders.isEmpty()) {
            String msg = getNoFlowFoundMessage(query);
            log.info(msg);
            return dk.dbc.dataio.commons.utils.service.ServiceUtil.buildResponse(Response.Status.NOT_FOUND, dk.dbc.dataio.commons.utils.service.ServiceUtil.asJsonError(msg));
        }
        if(flowBinders.size() > 1) {
            String msg = getMoreThanOneFlowFoundMessage(query);
            log.warn(msg);
        }
        return dk.dbc.dataio.commons.utils.service.ServiceUtil.buildResponse(Response.Status.OK, JsonUtil.toJson(flowBinders.get(0)));
    }

    private String getNoFlowFoundMessage(Query query) {
        return getQueryParametersStringify("No flows found for query with parameters", query);
    }

    private String getMoreThanOneFlowFoundMessage(Query query) {
        return getQueryParametersStringify("More than one result was found for the query with parameters", query);
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

    /**
     * Creates new flow binder with data POST'ed as JSON and persists it in the
     * underlying data store.
     *
     * Note: this method updates multiple database tables assuming transactional
     * integrity
     *
     * @param uriInfo application and request URI information
     * @param flowBinderData flow binder data as JSON string
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
    @Consumes({MediaType.APPLICATION_JSON})
    public Response createFlowBinder(@Context UriInfo uriInfo, String flowBinderData) throws JsonException, ReferencedEntityNotFoundException {
        log.trace("Called with: '{}'", flowBinderData);

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

        final FlowBinder flowBinder = new FlowBinder();
        flowBinder.setContent(flowBinderData);
        flowBinder.setFlow(resolveFlow(flowBinder.getFlowId()));
        flowBinder.setSink(resolveSink(flowBinder.getSinkId()));
        flowBinder.setSubmitters(resolveSubmitters(flowBinder.getSubmitterIds()));
        entityManager.persist(flowBinder);

        for (FlowBinderSearchIndexEntry searchIndexEntry : FlowBinder.generateSearchIndexEntries(flowBinder)) {
            entityManager.persist(searchIndexEntry);
        }

        entityManager.flush();

        return Response.created(ServiceUtil.getResourceUriOfVersionedEntity(uriInfo.getAbsolutePathBuilder(), flowBinder)).build();
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

}
