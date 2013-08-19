package dk.dbc.dataio.flowstore.ejb;

import dk.dbc.dataio.flowstore.entity.EntityPrimaryKey;
import dk.dbc.dataio.flowstore.entity.Flow;
import dk.dbc.dataio.flowstore.entity.FlowComponent;
import dk.dbc.dataio.flowstore.util.json.JsonException;
import dk.dbc.dataio.flowstore.util.json.JsonUtil;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.node.ArrayNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.util.Date;

import static dk.dbc.dataio.flowstore.util.ServiceUtil.buildResponse;
import static dk.dbc.dataio.flowstore.util.ServiceUtil.getEntity;
import static dk.dbc.dataio.flowstore.util.ServiceUtil.getResourceUri;
import static dk.dbc.dataio.flowstore.util.ServiceUtil.newErrorAsJson;
import static dk.dbc.dataio.flowstore.util.ServiceUtil.saveAsEntity;
import static dk.dbc.dataio.flowstore.util.ServiceUtil.saveAsNewVersionOfEntity;

/**
 * This Enterprise Java Bean (EJB) class acts as a JAX-RS root resource
 * exposed by the '/{@code FLOWS_ENTRY_POINT}' entry point
 */
@Stateless
@Path("flows")
public class FlowsBean {
    /** Main entry point for the flows collection
     */
    // Must match the value of the @Path annotation for this class.
    public static final String FLOWS_ENTRY_POINT = "flows";

    private static final Logger log = LoggerFactory.getLogger(FlowsBean.class);

    private static final String NOT_FOUND_MESSAGE = "resource not found";

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * Retrieves flow from underlying data store
     *
     * @param id flow identifier
     * @param version flow version
     *
     * @return a HTTP 200 response with flow content as JSON,
     *         a HTTP 404 response with error content as JSON if not found,
     *         a HTTP 500 response in case of general error.
     */
    @GET
    @Path("/{id}/{version}")
    @Produces({MediaType.APPLICATION_JSON})
    public Response getFlow(@PathParam("id") Long id, @PathParam("version") Long version) throws JsonException {
        EntityPrimaryKey pk = new EntityPrimaryKey(id, new Date(version));
        final Flow flow = entityManager.find(Flow.class, pk);
        if (flow == null) {
            return buildResponse(Response.Status.NOT_FOUND, newErrorAsJson(NOT_FOUND_MESSAGE));
        }
        return buildResponse(Response.Status.OK, JsonUtil.toJson(flow));
    }

    /**
     * Creates new flow with data POST'ed as JSON and persists it in the
     * underlying data store
     *
     * @param uriInfo application and request URI information
     * @param flowContent flow data as JSON string
     *
     * @return a HTTP 201 response with a Location header containing the URL value of the newly created resource
     *         a HTTP 500 response in case of general error.
     *
     * @throws JsonException when given invalid (null-valued, empty-valued or non-json)
     *                       JSON string, or if JSON object does not contain required
     *                       members
     */
    @POST
    @Consumes({ MediaType.APPLICATION_JSON })
    public Response createFlow(@Context UriInfo uriInfo, String flowContent) throws JsonException {
        log.trace("Called with: '{}'", flowContent);

        final Flow flow = saveAsEntity(entityManager, Flow.class, flowContent);
        entityManager.flush();

        return Response.created(getResourceUri(uriInfo.getAbsolutePathBuilder(), flow)).build();
    }

    /**
     * Adds existing flow component, identified by form parameters POST'ed as
     * 'application/x-www-form-urlencoded' content, to flow given by path
     *
     * @param uriInfo application and request URI information
     * @param flowId flow identifier (extracted from path parameter id)
     * @param flowVersion flow version (extracted from path parameter version)
     * @param componentId flow component id (extracted from form parameter id)
     * @param componentVersion flow component version (extracted from form parameter version)
     *
     * @return a HTTP 201 response with a Location header containing the URL value of the newly created resource
     *         a HTTP 404 response with error content as JSON if unable to find specified flow
     *         a HTTP 412 response with error content as JSON if unable to find specified flow component
     *         a HTTP 500 response in case of general error.
     *
     * @throws JsonException when unable to handle entity content as JSON
     */
    @POST
    @Path("/{id}/{version}/components")
    @Produces({ MediaType.APPLICATION_JSON })
    public Response addFlowComponent(@Context UriInfo uriInfo,
                                     @PathParam("id") Long flowId, @PathParam("version") Long flowVersion,
                                     @FormParam("id") Long componentId, @FormParam("version") Long componentVersion) throws JsonException {

        // Get specified version of flow entity
        final Flow flow = getEntity(entityManager, Flow.class, flowId, flowVersion);
        if (flow == null) {
            return buildResponse(Response.Status.NOT_FOUND, newErrorAsJson(NOT_FOUND_MESSAGE));
        }

        // Get specified version of flow component entity to add to flow
        final FlowComponent component = getEntity(entityManager, FlowComponent.class, componentId, componentVersion);
        if (component == null) {
            return buildResponse(Response.Status.PRECONDITION_FAILED, newErrorAsJson(NOT_FOUND_MESSAGE));
        }

        // Add specified component to 'components' array of current flow content
        // Note: Consider using dataio DTO model objects instead of JSON tree model, when they are made available outside of engine module.
        final JsonNode flowContentNode = JsonUtil.getJsonRoot(flow.getContent());
        final ArrayNode flowContentComponentsNode = (ArrayNode) flowContentNode.get("components");
        flowContentComponentsNode.add(JsonUtil.getJsonRoot(JsonUtil.toJson(component)));

        final Flow newVersionOfFlow = saveAsNewVersionOfEntity(entityManager, Flow.class, flow.getId(), flowContentNode.toString());
        entityManager.flush();

        return Response.created(getResourceUri(uriInfo.getBaseUriBuilder().path(FLOWS_ENTRY_POINT), newVersionOfFlow))
                .build();
    }
}
