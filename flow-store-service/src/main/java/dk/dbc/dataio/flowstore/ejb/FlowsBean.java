package dk.dbc.dataio.flowstore.ejb;


import dk.dbc.commons.jsonb.JSONBContext;
import dk.dbc.commons.jsonb.JSONBException;
import dk.dbc.dataio.commons.types.FlowContent;
import dk.dbc.dataio.commons.types.rest.FlowStoreServiceConstants;
import dk.dbc.dataio.flowstore.entity.Flow;
import dk.dbc.invariant.InvariantUtil;
import jakarta.annotation.Resource;
import jakarta.ejb.SessionContext;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This Enterprise Java Bean (EJB) class acts as a JAX-RS root resource
 * exposed by the '/{@code FLOWS_ENTRY_POINT}' entry point
 */
@Stateless
@Path("/")
public class FlowsBean extends AbstractResourceBean {
    private static final Logger log = LoggerFactory.getLogger(FlowsBean.class);
    private static final String FLOW_CONTENT_DISPLAY_TEXT = "flowContent";
    private static final String NULL_ENTITY = "";
    private static final Map<Long, String> NAME_CACHE = new ConcurrentHashMap<>();

    JSONBContext jsonbContext = new JSONBContext();

    @PersistenceContext
    EntityManager entityManager;

    @Resource
    SessionContext sessionContext;
    @ConfigProperty(name = "FLOWSTORE_FALLBACK")
    @Inject
    Optional<String> flowstoreFallback;

    /**
     * Retrieves flow from underlying data store
     *
     * @param id flow identifier
     * @return a HTTP 200 response with flow content as JSON,
     * a HTTP 404 response with error content as JSON if not found,
     * a HTTP 500 response in case of general error.
     * @throws JSONBException if unable to marshall value type into its JSON representation
     */
    @GET
    @Path(FlowStoreServiceConstants.FLOW)
    @Produces({MediaType.APPLICATION_JSON})
    public Response getFlow(@PathParam(FlowStoreServiceConstants.ID_VARIABLE) Long id) throws JSONBException {
        log.debug("getFlow called with: '{}'", id);
        final Flow flow = entityManager.find(Flow.class, id);
        if (flow == null) {
            return Response.status(Response.Status.NOT_FOUND).entity(NULL_ENTITY).build();
        }
        return Response.ok().entity(jsonbContext.marshall(flow)).build();
    }

    public String getFlowName(Long id) {
        return NAME_CACHE.computeIfAbsent(id, k -> {
            try {
                Flow flow = entityManager.find(Flow.class, id);
                return jsonbContext.unmarshall(flow.getContent(), FlowContent.class).getName();
            } catch (Exception e) {
                return id.toString();
            }
        });
    }

    /**
     * Retrieves flow from underlying data store
     *
     * @param name flow identifier
     * @return a HTTP 200 response with flow content as JSON,
     * a HTTP 404 response with error content as JSON if not found,
     * a HTTP 500 response in case of general error.
     * @throws JSONBException if unable to marshall value type into its JSON representation
     */

    @GET
    @Path(FlowStoreServiceConstants.FLOWS)
    @Produces({MediaType.APPLICATION_JSON})
    public Response findFlows(@QueryParam("name") String name) throws JSONBException {
        if (name != null) {
            return findFlowByName(name);
        } else {
            return findAll();
        }
    }

    /**
     * Creates new flow with data POSTed as JSON and persists it in the
     * underlying data store
     *
     * @param uriInfo     application and request URI information
     * @param flowContent flow data as JSON string
     * @return a HTTP 201 response with flow content as JSON
     * a HTTP 400 BAD_REQUEST response on invalid json content.
     * a HTTP 406 NOT_ACCEPTABLE response if violating any uniqueness constraints.
     * a HTTP 500 response in case of general error.
     * @throws JSONBException when given invalid (null-valued, empty-valued or non-json)
     *                        JSON string, or if JSON object does not contain required
     *                        members
     */
    @POST
    @Path(FlowStoreServiceConstants.FLOWS)
    @Consumes({MediaType.APPLICATION_JSON})
    @Produces({MediaType.APPLICATION_JSON})
    public Response createFlow(@Context UriInfo uriInfo, String flowContent) throws JSONBException {
        log.trace("Called with: '{}'", flowContent);

        InvariantUtil.checkNotNullNotEmptyOrThrow(flowContent, FLOW_CONTENT_DISPLAY_TEXT);

        jsonbContext.unmarshall(flowContent, FlowContent.class);

        final Flow flow = saveAsVersionedEntity(entityManager, Flow.class, flowContent);
        entityManager.flush();
        final String flowJson = jsonbContext.marshall(flow);
        return Response.created(getResourceUriOfVersionedEntity(uriInfo.getAbsolutePathBuilder(), flow)).entity(flowJson).build();
    }

    @POST
    @Path(FlowStoreServiceConstants.FLOW_JSAR_CREATE)
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_OCTET_STREAM)
    public Response createFlow(@PathParam(FlowStoreServiceConstants.LM_VARIABLE) long lastModified, byte[] jsArchive) throws JSONBException {
        FlowContent flowContent = new FlowContent(jsArchive, new Date(lastModified));
        Flow flow = self().createFlow(flowContent);
        return Response.ok()
                .entity(flow.getView())
                .tag(Long.toString(flow.getVersion()))
                .status(flow.getVersion().intValue() == 1 ? Response.Status.CREATED : Response.Status.OK)
                .build();
    }

    @POST
    @Path(FlowStoreServiceConstants.FLOW_JSAR_UPDATE)
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_OCTET_STREAM)
    public Response updateFlow(@PathParam(FlowStoreServiceConstants.ID_VARIABLE) Long id,  @PathParam(FlowStoreServiceConstants.LM_VARIABLE) long lastModified, byte[] jsArchive) throws JSONBException {
        FlowContent flowContent = new FlowContent(jsArchive, new Date(lastModified));
            Flow flow = self().updateFlow(id, flowContent);
            return Response.ok()
                    .entity(flow.getView())
                    .tag(Long.toString(flow.getVersion()))
                    .status(flow.getVersion().intValue() == 1 ? Response.Status.CREATED : Response.Status.OK)
                    .build();

    }

    @GET
    @Path(FlowStoreServiceConstants.FLOW_JSAR)
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public Response getJsar(@PathParam(FlowStoreServiceConstants.ID_VARIABLE) Long id) {
        Flow flow = entityManager.find(Flow.class, id);
        if(flow.getJsar() == null) {
            return flowstoreFallback
                    .filter(s -> !s.isBlank())
                    .map(url -> url + "/" + FlowStoreServiceConstants.FLOW_NAME_JSAR)
                    .map(f -> f.replaceFirst("\\{name}", getFlowName(flow.getId())))
                    .map(URI::create)
                    .map(Response::temporaryRedirect)
                    .map(Response.ResponseBuilder::build)
                    .orElseThrow(() -> new IllegalStateException("No JSar file found for flow " + id));
        }
        return Response.ok(flow.getJsar()).build();
    }

    @GET
    @Path(FlowStoreServiceConstants.FLOW_NAME_JSAR)
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public Response getJsarByName(@PathParam(FlowStoreServiceConstants.NAME_VARIABLE) String flowName) {
        TypedQuery<Flow> query = entityManager.createNamedQuery(Flow.QUERY_FIND_BY_NAME, Flow.class)
                .setParameter(1, flowName);
        try {
            Flow flow = query.getSingleResult();
            return Response.ok(flow.getJsar()).build();
        } catch (NoResultException nre) {
            throw new NotFoundException("Found no flow with name " + flowName);
        }
    }

    protected FlowsBean self() {
        return sessionContext.getBusinessObject(FlowsBean.class);
    }

    // private methods


    /**
     * Returns list containing one flow uniquely identified by the flow name given as input
     *
     * @return a HTTP OK response with result list as JSON
     * @throws JSONBException on failure to create result list as JSON
     */
    private Response findFlowByName(String name) throws JSONBException {
        final TypedQuery<Flow> query = entityManager.createNamedQuery(Flow.QUERY_FIND_BY_NAME, Flow.class)
                .setParameter(1, name);
        List<Flow> flows = query.getResultList();
        if (flows.isEmpty()) {
            return Response.status(Response.Status.NOT_FOUND).entity(NULL_ENTITY).build();
        }
        return Response.ok().entity(jsonbContext.marshall(flows)).build();
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public Flow updateFlow(long flowId, FlowContent flowContent) throws JSONBException {
        Flow flow = entityManager.find(Flow.class, flowId);
        return flow.updateContent(flowContent);
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public Flow createFlow(FlowContent flowContent) throws JSONBException {
        Flow flow = new Flow().updateContent(flowContent);
        entityManager.persist(flow);
        return flow;
    }

    /**
     * Returns list of brief views of all stored flows sorted by name in ascending order
     *
     * @return a HTTP OK response with result list as JSON
     */
    private Response findAll() {
        final TypedQuery<String> query = entityManager.createNamedQuery(Flow.QUERY_FIND_ALL, String.class);
        return Response.ok().entity(query.getResultList().toString()).build();
    }

    /**
     * Deletes an existing flow
     *
     * @param flowId The flow ID
     * @return a HTTP 204 response with no content,
     * a HTTP 404 response in case of flow ID not found,
     * a HTTP 409 response in case an OptimisticLock or Constraint violation occurs,
     * a HTTP 500 response in case of general error.
     */
    @DELETE
    @Path(FlowStoreServiceConstants.FLOW)
    public Response deleteFlow(
            @PathParam(FlowStoreServiceConstants.ID_VARIABLE) Long flowId,
            @HeaderParam(FlowStoreServiceConstants.IF_MATCH_HEADER) Long version) {

        final Flow flowEntity = entityManager.find(Flow.class, flowId);
        if (flowEntity == null) {
            return Response.status(Response.Status.NOT_FOUND).entity(NULL_ENTITY).build();
        }
        flowEntity.assertLatestVersion(version);
        entityManager.remove(flowEntity);
        entityManager.flush();
        NAME_CACHE.remove(flowId);
        return Response.noContent().build();
    }
}
