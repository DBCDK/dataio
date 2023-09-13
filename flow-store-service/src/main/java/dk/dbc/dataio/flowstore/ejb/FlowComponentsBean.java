package dk.dbc.dataio.flowstore.ejb;

import dk.dbc.commons.jsonb.JSONBContext;
import dk.dbc.commons.jsonb.JSONBException;
import dk.dbc.dataio.commons.types.FlowComponentContent;
import dk.dbc.dataio.commons.types.rest.FlowStoreServiceConstants;
import dk.dbc.dataio.flowstore.entity.FlowComponent;
import dk.dbc.invariant.InvariantUtil;
import jakarta.ejb.Stateless;
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
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This Enterprise Java Bean (EJB) class acts as a JAX-RS root resource
 * exposed by the '/FlowStoreServiceConstants.FLOW_COMPONENTS' entry point
 */
@Stateless
@Path("/")
public class FlowComponentsBean extends AbstractResourceBean {
    private static final String FLOW_COMPONENT_CONTENT_DISPLAY_TEXT = "flowComponentContent";
    private static final Logger log = LoggerFactory.getLogger(FlowComponentsBean.class);
    private static final String NULL_ENTITY = "";

    JSONBContext jsonbContext = new JSONBContext();

    @PersistenceContext
    EntityManager entityManager;

    /**
     * Retrieves flow component from underlying data store
     *
     * @param id flow component identifier
     * @return a HTTP 200 response with flowComponent content as JSON,
     * a HTTP 404 response with error content as JSON if not found,
     * a HTTP 500 response in case of general error.
     * @throws JSONBException on failure to create json flowComponent
     */
    @GET
    @Path(FlowStoreServiceConstants.FLOW_COMPONENT)
    @Produces({MediaType.APPLICATION_JSON})
    public Response getFlowComponent(@PathParam(FlowStoreServiceConstants.ID_VARIABLE) Long id) throws JSONBException {
        final FlowComponent flowComponent = entityManager.find(FlowComponent.class, id);
        if (flowComponent == null) {
            return Response.status(Response.Status.NOT_FOUND).entity(NULL_ENTITY).build();
        }
        return Response.ok().entity(jsonbContext.marshall(flowComponent)).build();
    }

    /**
     * Creates new flow component with data POSTed as JSON and persists it in the
     * underlying data store
     *
     * @param uriInfo          the uri info
     * @param componentContent component data as JSON string
     * @return a HTTP 201 response with a Location header containing the URL value of the newly created resource
     * a HTTP 400 BAD_REQUEST response on invalid json content.
     * a HTTP 406 NOT_ACCEPTABLE response if violating any uniqueness constraints.
     * a HTTP 500 response in case of general error.
     * @throws JSONBException if unable to marshall value type into its JSON representation
     */
    @POST
    @Path(FlowStoreServiceConstants.FLOW_COMPONENTS)
    @Consumes({MediaType.APPLICATION_JSON})
    @Produces({MediaType.APPLICATION_JSON})
    public Response createComponent(@Context UriInfo uriInfo, String componentContent) throws JSONBException {
        log.trace("Called with: '{}'", componentContent);
        InvariantUtil.checkNotNullNotEmptyOrThrow(componentContent, FLOW_COMPONENT_CONTENT_DISPLAY_TEXT);
        jsonbContext.unmarshall(componentContent, FlowComponentContent.class);

        final FlowComponent component = saveAsVersionedEntity(entityManager, FlowComponent.class, componentContent);
        entityManager.flush();
        final String componentJson = jsonbContext.marshall(component);
        return Response.created(getResourceUriOfVersionedEntity(uriInfo.getAbsolutePathBuilder(), component)).entity(componentJson).build();
    }

    /**
     * Returns list of brief views of all stored flow components sorted by name in ascending order
     *
     * @return a HTTP OK response with result list as JSON
     */
    @GET
    @Path(FlowStoreServiceConstants.FLOW_COMPONENTS)
    @Produces({MediaType.APPLICATION_JSON})
    public Response findAllComponents() {
        final TypedQuery<String> query = entityManager.createNamedQuery(FlowComponent.QUERY_FIND_ALL, String.class);
        return Response.ok().entity(query.getResultList().toString()).build();
    }

    /**
     * Updates an existing flow component
     *
     * @param uriInfo              URI information
     * @param flowComponentContent The content of the flow component
     * @param id                   The flow component ID
     * @param version              The version of the flow component
     * @return a HTTP 200 response with flow component content as JSON
     * a HTTP 406 NOT_ACCEPTABLE response if violating any uniqueness constraints.
     * a HTTP 409 response in case of Concurrent Update error
     * a HTTP 500 response in case of general error.
     * @throws JSONBException on failure to create json component
     */
    @POST
    @Path(FlowStoreServiceConstants.FLOW_COMPONENT_CONTENT)
    @Consumes({MediaType.APPLICATION_JSON})
    @Produces({MediaType.APPLICATION_JSON})
    public Response updateFlowComponent(@Context UriInfo uriInfo, String flowComponentContent, @PathParam(FlowStoreServiceConstants.ID_VARIABLE) Long id,
                                        @HeaderParam(FlowStoreServiceConstants.IF_MATCH_HEADER) Long version) throws JSONBException {

        InvariantUtil.checkNotNullNotEmptyOrThrow(flowComponentContent, FLOW_COMPONENT_CONTENT_DISPLAY_TEXT);
        jsonbContext.unmarshall(flowComponentContent, FlowComponentContent.class);

        final FlowComponent flowComponentEntity = entityManager.find(FlowComponent.class, id);
        if (flowComponentEntity == null) {
            return Response.status(Response.Status.NOT_FOUND).entity(NULL_ENTITY).build();
        }
        entityManager.detach(flowComponentEntity);
        flowComponentEntity.setContent(flowComponentContent);
        flowComponentEntity.setVersion(version);
        entityManager.merge(flowComponentEntity);
        entityManager.flush();
        final FlowComponent updatedFlowComponent = entityManager.find(FlowComponent.class, id);
        final String flowComponentJson = jsonbContext.marshall(updatedFlowComponent);
        return Response.ok(getResourceUriOfVersionedEntity(uriInfo.getAbsolutePathBuilder(), updatedFlowComponent)).entity(flowComponentJson).build();
    }


    /**
     * Deletes an existing flowComponent
     *
     * @param flowComponentId The flow ID
     * @param version         The version of the flow
     * @return a HTTP 204 response with no content,
     * a HTTP 404 response in case of flow ID not found,
     * a HTTP 409 response in case an OptimisticLock or Constraint violation occurs,
     * a HTTP 500 response in case of general error.
     */
    @DELETE
    @Path(FlowStoreServiceConstants.FLOW_COMPONENT)
    public Response deleteFlowComponent(
            @PathParam(FlowStoreServiceConstants.ID_VARIABLE) Long flowComponentId,
            @HeaderParam(FlowStoreServiceConstants.IF_MATCH_HEADER) Long version) {

        final FlowComponent flowComponent = entityManager.find(FlowComponent.class, flowComponentId);

        if (flowComponent == null) {
            return Response.status(Response.Status.NOT_FOUND).entity(NULL_ENTITY).build();
        }

        // First we need to update the version no to see if any Optimistic Locking occurs!
        entityManager.detach(flowComponent);
        flowComponent.setVersion(version);
        FlowComponent versionUpdatedAndNoOptimisticLocking = entityManager.merge(flowComponent);

        // If no Optimistic Locking - delete it!
        entityManager.remove(versionUpdatedAndNoOptimisticLocking);
        entityManager.flush();

        return Response.noContent().build();
    }


    /**
     * Updates an existing flow component with next
     *
     * @param uriInfo              URI information
     * @param flowComponentContent The content of the next flow component
     * @param id                   The flow component ID
     * @param version              The version of the flow component
     * @return a HTTP 200 response with flow component content as JSON
     * a HTTP 406 NOT_ACCEPTABLE response if violating any uniqueness constraints.
     * a HTTP 409 response in case of Concurrent Update error
     * a HTTP 500 response in case of general error.
     * @throws JSONBException on failure to create json component
     */
    @POST
    @Path(FlowStoreServiceConstants.FLOW_COMPONENT_NEXT)
    @Consumes({MediaType.APPLICATION_JSON})
    @Produces({MediaType.APPLICATION_JSON})
    public Response updateNext(@Context UriInfo uriInfo, String flowComponentContent, @PathParam(FlowStoreServiceConstants.ID_VARIABLE) Long id,
                               @HeaderParam(FlowStoreServiceConstants.IF_MATCH_HEADER) Long version) throws JSONBException {

        final FlowComponent flowComponentEntity = entityManager.find(FlowComponent.class, id);
        if (flowComponentEntity == null) {
            return Response.status(Response.Status.NOT_FOUND).entity(NULL_ENTITY).build();
        }
        entityManager.detach(flowComponentEntity);
        flowComponentEntity.setNext(flowComponentContent);
        flowComponentEntity.setVersion(version);
        entityManager.merge(flowComponentEntity);
        entityManager.flush();
        final FlowComponent updatedFlowComponent = entityManager.find(FlowComponent.class, id);
        final String flowComponentJson = jsonbContext.marshall(updatedFlowComponent);
        return Response.ok(getResourceUriOfVersionedEntity(uriInfo.getAbsolutePathBuilder(), updatedFlowComponent)).entity(flowComponentJson).build();
    }

}
