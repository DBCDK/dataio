package dk.dbc.dataio.flowstore.ejb;

import dk.dbc.dataio.commons.types.rest.FlowStoreServiceConstants;
import dk.dbc.dataio.commons.utils.invariant.InvariantUtil;
import dk.dbc.dataio.commons.utils.json.JsonException;
import dk.dbc.dataio.commons.utils.json.JsonUtil;
import dk.dbc.dataio.commons.utils.service.ServiceUtil;
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
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.util.List;

import static dk.dbc.dataio.flowstore.util.ServiceUtil.getResourceUriOfVersionedEntity;
import static dk.dbc.dataio.flowstore.util.ServiceUtil.saveAsVersionedEntity;

/**
 * This Enterprise Java Bean (EJB) class acts as a JAX-RS root resource
 * exposed by the '/FlowStoreServiceConstants.FLOW_COMPONENTS' entry point
 */
@Stateless
@Path("/")
public class FlowComponentsBean {
    private static final String FLOW_COMPONENT_CONTENT_DISPLAY_TEXT = "flowComponentContent";
    private static final String NOT_FOUND_MESSAGE = "resource not found";
    private static final Logger log = LoggerFactory.getLogger(FlowComponentsBean.class);

    @PersistenceContext
    EntityManager entityManager;

    /**
     * Retrieves flow component from underlying data store
     *
     * @param id flow component identifier
     *
     * @return a HTTP 200 response with flowComponent content as JSON,
     *         a HTTP 404 response with error content as JSON if not found,
     *         a HTTP 500 response in case of general error.
     *
     * @throws JsonException on failure to create json flowComponent
     */
    @GET
    @Path(FlowStoreServiceConstants.FLOW_COMPONENT)
    @Produces({MediaType.APPLICATION_JSON})
    public Response getFlowComponent(@PathParam(FlowStoreServiceConstants.FLOW_COMPONENT_ID_VARIABLE) Long id) throws JsonException {
        final FlowComponent flowComponent = entityManager.find(FlowComponent.class, id);
        if (flowComponent == null) {
            return ServiceUtil.buildResponse(Response.Status.NOT_FOUND, ServiceUtil.asJsonError(NOT_FOUND_MESSAGE));
        }
        return ServiceUtil.buildResponse(Response.Status.OK, JsonUtil.toJson(flowComponent));
    }

    /**
     * Creates new flow component with data POSTed as JSON and persists it in the
     * underlying data store
     *
     * @param uriInfo the uri info
     * @param componentContent component data as JSON string
     *
     * @return a HTTP 201 response with a Location header containing the URL value of the newly created resource
     *         a HTTP 400 BAD_REQUEST response on invalid json content.
     *         a HTTP 406 NOT_ACCEPTABLE response if violating any uniqueness constraints.
     *         a HTTP 500 response in case of general error.
     *
     * @throws JsonException if unable to marshall value type into its JSON representation
     */
    @POST
    @Path(FlowStoreServiceConstants.FLOW_COMPONENTS)
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({MediaType.APPLICATION_JSON})
    public Response createComponent(@Context UriInfo uriInfo, String componentContent) throws JsonException {
        log.trace("Called with: '{}'", componentContent);

        final FlowComponent component = saveAsVersionedEntity(entityManager, FlowComponent.class, componentContent);
        entityManager.flush();
        final String componentJson = JsonUtil.toJson(component);
        return Response.created(getResourceUriOfVersionedEntity(uriInfo.getAbsolutePathBuilder(), component)).entity(componentJson).build();
    }

    /**
     * Returns list of all versions of all stored flow components sorted by name in ascending order
     *
     * @return a HTTP OK response with result list as JSON
     *
     * @throws JsonException on failure to create result list as JSON
     */
    @GET
    @Path(FlowStoreServiceConstants.FLOW_COMPONENTS)
    @Produces({ MediaType.APPLICATION_JSON })
    public Response findAllComponents() throws JsonException {
        final TypedQuery<FlowComponent> query = entityManager.createNamedQuery(FlowComponent.QUERY_FIND_ALL, FlowComponent.class);
        final List<FlowComponent> results = query.getResultList();
        return ServiceUtil.buildResponse(Response.Status.OK, JsonUtil.toJson(results));
    }

    /**
     * Updates an existing flow component
     *
     * @param uriInfo URI information
     * @param flowComponentContent The content of the flow component
     * @param id The flow component ID
     * @param version The version of the flow component
     *
     * @return a HTTP 200 response with flow component content as JSON
     *         a HTTP 406 NOT_ACCEPTABLE response if violating any uniqueness constraints.
     *         a HTTP 409 response in case of Concurrent Update error
     *         a HTTP 500 response in case of general error.
     *
     * @throws JsonException on failure to create json component
     */
    @POST
    @Path(FlowStoreServiceConstants.FLOW_COMPONENT_CONTENT)
    @Consumes({MediaType.APPLICATION_JSON})
    @Produces({MediaType.APPLICATION_JSON})
    public Response updateFlowComponent(@Context UriInfo uriInfo, String flowComponentContent, @PathParam(FlowStoreServiceConstants.FLOW_COMPONENT_ID_VARIABLE) Long id,
                               @HeaderParam(FlowStoreServiceConstants.IF_MATCH_HEADER) Long version) throws JsonException {

        InvariantUtil.checkNotNullNotEmptyOrThrow(flowComponentContent, FLOW_COMPONENT_CONTENT_DISPLAY_TEXT);
        final FlowComponent flowComponentEntity = entityManager.find(FlowComponent.class, id);
        if (flowComponentEntity == null) {
            return Response.status(Response.Status.NOT_FOUND.getStatusCode()).build();
        }
        entityManager.detach(flowComponentEntity);
        flowComponentEntity.setContent(flowComponentContent);
        flowComponentEntity.setVersion(version);
        entityManager.merge(flowComponentEntity);
        entityManager.flush();
        final FlowComponent updatedFlowComponent = entityManager.find(FlowComponent.class, id);
        final String flowComponentJson = JsonUtil.toJson(updatedFlowComponent);
        return Response.ok(getResourceUriOfVersionedEntity(uriInfo.getAbsolutePathBuilder(), updatedFlowComponent)).entity(flowComponentJson).build();
    }

}
