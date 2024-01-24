package dk.dbc.dataio.flowstore.ejb;


import dk.dbc.commons.jsonb.JSONBContext;
import dk.dbc.commons.jsonb.JSONBException;
import dk.dbc.dataio.commons.types.FlowContent;
import dk.dbc.dataio.commons.types.exceptions.ReferencedEntityNotFoundException;
import dk.dbc.dataio.commons.types.rest.FlowStoreServiceConstants;
import dk.dbc.dataio.flowstore.entity.Flow;
import dk.dbc.dataio.flowstore.entity.FlowComponent;
import dk.dbc.invariant.InvariantUtil;
import jakarta.annotation.Resource;
import jakarta.ejb.SessionContext;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
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
import java.util.Date;
import java.util.List;

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

    JSONBContext jsonbContext = new JSONBContext();

    @PersistenceContext
    EntityManager entityManager;

    @Resource
    SessionContext sessionContext;

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

    /**
     * Updates an existing flow
     *
     * @param flowContent the flow content containing the changes
     * @param uriInfo     URI information
     * @param id          The flow ID
     * @param version     The version of the flow
     * @param isRefresh   boolean value defining whether or not:
     *                    a) The update is to be performed on the flow
     *                    b) The versioned flow components, contained within the flow, are to be replaced with latest version
     * @return a HTTP 200 response with flow content as JSON,
     * a HTTP 409 response in case of Concurrent Update error,
     * a HTTP 500 response in case of general error.
     * @throws JSONBException                    on failure to create json flow
     * @throws ReferencedEntityNotFoundException on failure to locate the flow component in the underlying database
     */
    @POST
    @Path(FlowStoreServiceConstants.FLOW_CONTENT)
    @Produces({MediaType.APPLICATION_JSON})
    @Consumes({MediaType.APPLICATION_JSON})
    public Response updateFlow(
            String flowContent,
            @Context UriInfo uriInfo,
            @PathParam(FlowStoreServiceConstants.ID_VARIABLE) Long id,
            @HeaderParam(FlowStoreServiceConstants.IF_MATCH_HEADER) Long version,
            @QueryParam(FlowStoreServiceConstants.QUERY_PARAMETER_REFRESH) Boolean isRefresh) throws JSONBException, ReferencedEntityNotFoundException {

        Flow flow = null;
        if (isRefresh != null && isRefresh) {
            flow = self().refreshFlowComponents(uriInfo, id, version);
            if (flow != null) return Response.ok(getResourceUriOfVersionedEntity(uriInfo.getAbsolutePathBuilder(), flow)).entity(flow.getContent()).build();
        } else {
            InvariantUtil.checkNotNullNotEmptyOrThrow(flowContent, FLOW_CONTENT_DISPLAY_TEXT);
            jsonbContext.unmarshall(flowContent, FlowContent.class);
            flow = self().updateFlowContent(flowContent, id, version);
        }
        if (flow == null) return Response.status(Response.Status.NOT_FOUND.getStatusCode()).entity(NULL_ENTITY).build();
        return Response.ok()
                .entity(jsonbContext.marshall(flow))
                .tag(Long.toString(flow.getVersion()))
                .build();
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
     * Updates the versioned flow components contained within the flow. Each is replaced with latest version
     *
     * @param uriInfo URI information
     * @param id      The flow ID
     * @param version The version of the flow
     * @return a HTTP 200 response with flow content as JSON,
     * a HTTP 409 response in case of Concurrent Update error,
     * a HTTP 500 response in case of general error.
     * @throws JSONBException                    on failure to create json flow
     * @throws ReferencedEntityNotFoundException on failure to locate the flow component in the underlying database
     */
    @SuppressWarnings("PMD.UnusedPrivateMethod")
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public Flow refreshFlowComponents(UriInfo uriInfo, Long id, Long version) throws JSONBException, ReferencedEntityNotFoundException {
        List<dk.dbc.dataio.commons.types.FlowComponent> flowComponentsWithLatestVersion = new ArrayList<>();
        boolean hasFlowComponentsChanged = false;

        final Flow flowEntity = entityManager.find(Flow.class, id);
        if (flowEntity == null) {
            return null;
        }
        flowEntity.assertLatestVersion(version);
        FlowContent flowContent = jsonbContext.unmarshall(flowEntity.getContent(), FlowContent.class);

        for (dk.dbc.dataio.commons.types.FlowComponent flowComponent : flowContent.getComponents()) {
            FlowComponent flowComponentWithLatestVersion = entityManager.find(FlowComponent.class, flowComponent.getId());
            if (flowComponentWithLatestVersion == null) {
                throw new ReferencedEntityNotFoundException("Flow component with id: " + flowComponent.getId() + "could not be found in the underlying database");
            }
            if (!hasFlowComponentsChanged && flowComponentWithLatestVersion.getVersion() != flowComponent.getVersion()) {
                hasFlowComponentsChanged = true;
            }
            String flowComponentWithLatestVersionJson = jsonbContext.marshall(flowComponentWithLatestVersion);
            dk.dbc.dataio.commons.types.FlowComponent updatedFlowComponent = jsonbContext.unmarshall(flowComponentWithLatestVersionJson, dk.dbc.dataio.commons.types.FlowComponent.class);
            flowComponentsWithLatestVersion.add(updatedFlowComponent);
        }

        FlowContent updatedFlowContent = new FlowContent(flowContent.getName(), flowContent.getDescription(), flowComponentsWithLatestVersion, flowContent.getTimeOfFlowComponentUpdate());
        if (hasFlowComponentsChanged) {
            updatedFlowContent.withTimeOfFlowComponentUpdate(new Date());
        }

        flowEntity.setContent(jsonbContext.marshall(updatedFlowContent));
        entityManager.flush();
        return flowEntity;
    }

    /**
     * Updates an existing flow
     *
     * @param flowContent the flow content containing the changes
     * @param id          The flow ID
     * @param version     The version of the flow
     * @return a HTTP 200 response with flow content as JSON,
     * a HTTP 409 response in case of Concurrent Update error,
     * a HTTP 500 response in case of general error.
     * @throws JSONBException JsonException on failure to create json flow
     */
    @SuppressWarnings("PMD.UnusedPrivateMethod")
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public Flow updateFlowContent(String flowContent, Long id, Long version) throws JSONBException {
        InvariantUtil.checkNotNullNotEmptyOrThrow(flowContent, FLOW_CONTENT_DISPLAY_TEXT);
        final Flow flowEntity = entityManager.find(Flow.class, id);
        if (flowEntity == null) {
            return null;
        }
        flowEntity.assertLatestVersion(version);
        flowContent = setTimeOfFlowComponentUpdate(flowContent, flowEntity.getContent());
        flowEntity.setContent(flowContent);
        entityManager.flush();
        return flowEntity;
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

        return Response.noContent().build();
    }

    private String setTimeOfFlowComponentUpdate(String newFlowContentJson, String existingFlowContentJson) throws JSONBException {
        final FlowContent existingFlowContent = jsonbContext.unmarshall(existingFlowContentJson, FlowContent.class);
        final FlowContent newFlowContent = jsonbContext.unmarshall(newFlowContentJson, FlowContent.class);
        if (!existingFlowContent.getComponents().equals(newFlowContent.getComponents())) {
            newFlowContent.withTimeOfFlowComponentUpdate(new Date());
            return jsonbContext.marshall(newFlowContent);
        }
        return newFlowContentJson;
    }

}
