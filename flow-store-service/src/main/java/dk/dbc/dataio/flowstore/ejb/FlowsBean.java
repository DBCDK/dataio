package dk.dbc.dataio.flowstore.ejb;


import dk.dbc.dataio.commons.types.FlowContent;
import dk.dbc.dataio.commons.types.exceptions.ReferencedEntityNotFoundException;
import dk.dbc.dataio.commons.types.rest.FlowStoreServiceConstants;
import dk.dbc.dataio.commons.utils.invariant.InvariantUtil;
import dk.dbc.dataio.commons.utils.json.JsonException;
import dk.dbc.dataio.commons.utils.json.JsonUtil;
import dk.dbc.dataio.commons.utils.service.ServiceUtil;
import dk.dbc.dataio.flowstore.entity.Flow;
import dk.dbc.dataio.flowstore.entity.FlowComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
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
import java.util.ArrayList;
import java.util.List;

import static dk.dbc.dataio.flowstore.util.ServiceUtil.getResourceUriOfVersionedEntity;
import static dk.dbc.dataio.flowstore.util.ServiceUtil.saveAsVersionedEntity;

/**
 * This Enterprise Java Bean (EJB) class acts as a JAX-RS root resource
 * exposed by the '/{@code FLOWS_ENTRY_POINT}' entry point
 */
@Stateless
@Path("/")
public class FlowsBean {
    private static final Logger log = LoggerFactory.getLogger(FlowsBean.class);

    private static final String NOT_FOUND_MESSAGE = "resource not found";
    private static final String FLOW_CONTENT_DISPLAY_TEXT = "flowContent";

    @PersistenceContext
    EntityManager entityManager;

    /**
     * Retrieves flow from underlying data store
     *
     * @param id flow identifier
     *
     * @return a HTTP 200 response with flow content as JSON,
     *         a HTTP 404 response with error content as JSON if not found,
     *         a HTTP 500 response in case of general error.
     */
    @GET
    @Path(FlowStoreServiceConstants.FLOW)
    @Produces({MediaType.APPLICATION_JSON})
    public Response getFlow(@PathParam(FlowStoreServiceConstants.FLOW_ID_VARIABLE) Long id) throws JsonException {
        final Flow flow = entityManager.find(Flow.class, id);
        if (flow == null) {
            return ServiceUtil.buildResponse(Response.Status.NOT_FOUND, ServiceUtil.asJsonError(NOT_FOUND_MESSAGE));
        }
        return ServiceUtil.buildResponse(Response.Status.OK, JsonUtil.toJson(flow));
    }

    /**
     * Creates new flow with data POSTed as JSON and persists it in the
     * underlying data store
     *
     * @param uriInfo application and request URI information
     * @param flowContent flow data as JSON string
     *
     * @return a HTTP 201 response with flow content as JSON
     *         a HTTP 400 BAD_REQUEST response on invalid json content.
     *         a HTTP 406 NOT_ACCEPTABLE response if violating any uniqueness constraints.
     *         a HTTP 500 response in case of general error.
     *
     * @throws JsonException when given invalid (null-valued, empty-valued or non-json)
     *                       JSON string, or if JSON object does not contain required
     *                       members
     */
    @POST
    @Path(FlowStoreServiceConstants.FLOWS)
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({MediaType.APPLICATION_JSON})
    public Response createFlow(@Context UriInfo uriInfo, String flowContent) throws JsonException {
        log.trace("Called with: '{}'", flowContent);

        InvariantUtil.checkNotNullNotEmptyOrThrow(flowContent, FLOW_CONTENT_DISPLAY_TEXT);

        final Flow flow = saveAsVersionedEntity(entityManager, Flow.class, flowContent);
        entityManager.flush();
        final String flowJson = JsonUtil.toJson(flow);
        return Response.created(getResourceUriOfVersionedEntity(uriInfo.getAbsolutePathBuilder(), flow)).entity(flowJson).build();
    }

    /**
     * Updates an existing flow
     *
     * @param uriInfo URI information
     * @param id The flow ID
     * @param version The version of the flow
     * @param isRefresh boolean value defining whether or not:
     *                  a) The update is to be performed on the flow
     *                  b) The versioned flow components, contained within the flow, are to be replaced with latest version
     *
     * @return a HTTP 200 response with flow content as JSON,
     *         a HTTP 409 response in case of Concurrent Update error,
     *         a HTTP 500 response in case of general error.
     *
     * @throws JsonException on failure to create json flow
     * @throws ReferencedEntityNotFoundException on failure to locate the flow component in the underlying database
     */
    @POST
    @Path(FlowStoreServiceConstants.FLOW_CONTENT)
    @Produces({MediaType.APPLICATION_JSON})
    @Consumes({MediaType.APPLICATION_JSON})
    public Response updateFlow(
            @Context UriInfo uriInfo,
            @PathParam(FlowStoreServiceConstants.FLOW_ID_VARIABLE) Long id,
            @HeaderParam(FlowStoreServiceConstants.IF_MATCH_HEADER) Long version,
            @QueryParam(FlowStoreServiceConstants.QUERY_PARAMETER_REFRESH) Boolean isRefresh) throws JsonException, ReferencedEntityNotFoundException {

        Response response;
        if(isRefresh != null && isRefresh) {
            response = refreshFlowComponents(uriInfo, id, version);
        }else {
            response = null;
        }
        return response;
    }

    /**
     * Returns list of all versions of all stored flows sorted by name in ascending order
     *
     * @return a HTTP OK response with result list as JSON
     *
     * @throws JsonException on failure to create result list as JSON
     */
    @GET
    @Path(FlowStoreServiceConstants.FLOWS)
    @Produces({ MediaType.APPLICATION_JSON })
    public Response findAllFlows() throws JsonException {
        final TypedQuery<Flow> query = entityManager.createNamedQuery(Flow.QUERY_FIND_ALL, Flow.class);
        final List<Flow> results = query.getResultList();
        return ServiceUtil.buildResponse(Response.Status.OK, JsonUtil.toJson(results));
    }

    // private methods

    /**
     * Updates the versioned flow components contained within the flow. Each is replaced with latest version
     *
     * @param uriInfo URI information
     * @param id The flow ID
     * @param version The version of the flow
     *
     * @return a HTTP 200 response with flow content as JSON,
     *         a HTTP 409 response in case of Concurrent Update error,
     *         a HTTP 500 response in case of general error.
     *
     * @throws JsonException on failure to create json flow
     * @throws ReferencedEntityNotFoundException on failure to locate the flow component in the underlying database
     */
    private Response refreshFlowComponents(UriInfo uriInfo, Long id, Long version) throws JsonException, ReferencedEntityNotFoundException {

        List<dk.dbc.dataio.commons.types.FlowComponent> flowComponentsWithLatestVersion = new ArrayList<>();

        final Flow flowEntity = entityManager.find(Flow.class, id);
        if (flowEntity == null) {
            return Response.status(Response.Status.NOT_FOUND.getStatusCode()).build();
        }
        entityManager.detach(flowEntity);
        FlowContent flowContent = JsonUtil.fromJson(flowEntity.getContent(), FlowContent.class);

        for(dk.dbc.dataio.commons.types.FlowComponent flowComponent : flowContent.getComponents()){
            FlowComponent flowComponentWithLatestVersion = entityManager.find(FlowComponent.class, flowComponent.getId());
            if (flowComponentWithLatestVersion == null) {
                throw new ReferencedEntityNotFoundException("Flow component with id: " + flowComponent.getId() + "could not be found in the underlying database");
            }
            String flowComponentWithLatestVersionJson = JsonUtil.toJson(flowComponentWithLatestVersion);
            dk.dbc.dataio.commons.types.FlowComponent updatedFlowComponent = JsonUtil.fromJson(flowComponentWithLatestVersionJson, dk.dbc.dataio.commons.types.FlowComponent.class);
            flowComponentsWithLatestVersion.add(updatedFlowComponent);
        }

        FlowContent updatedFlowContent = new FlowContent(flowContent.getName(), flowContent.getDescription(), flowComponentsWithLatestVersion);

        flowEntity.setContent(JsonUtil.toJson(updatedFlowContent));
        flowEntity.setVersion(version);
        entityManager.merge(flowEntity);
        entityManager.flush();
        final Flow updatedFlow = entityManager.find(Flow.class, id);
        final String flowJson = JsonUtil.toJson(updatedFlow);
        return Response.ok(getResourceUriOfVersionedEntity(uriInfo.getAbsolutePathBuilder(), updatedFlow)).entity(flowJson).build();
    }
}
