/*
 * DataIO - Data IO
 * Copyright (C) 2015 Dansk Bibliotekscenter a/s, Tempovej 7-11, DK-2750 Ballerup,
 * Denmark. CVR: 15149043
 *
 * This file is part of DataIO.
 *
 * DataIO is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * DataIO is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with DataIO.  If not, see <http://www.gnu.org/licenses/>.
 */

package dk.dbc.dataio.flowstore.ejb;

import dk.dbc.commons.jsonb.JSONBContext;
import dk.dbc.commons.jsonb.JSONBException;
import dk.dbc.dataio.commons.types.FlowComponentContent;
import dk.dbc.dataio.commons.types.rest.FlowStoreServiceConstants;
import dk.dbc.dataio.flowstore.entity.FlowComponent;
import dk.dbc.invariant.InvariantUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
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
     *
     * @return a HTTP 200 response with flowComponent content as JSON,
     *         a HTTP 404 response with error content as JSON if not found,
     *         a HTTP 500 response in case of general error.
     *
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
     * @param uriInfo the uri info
     * @param componentContent component data as JSON string
     *
     * @return a HTTP 201 response with a Location header containing the URL value of the newly created resource
     *         a HTTP 400 BAD_REQUEST response on invalid json content.
     *         a HTTP 406 NOT_ACCEPTABLE response if violating any uniqueness constraints.
     *         a HTTP 500 response in case of general error.
     *
     * @throws JSONBException if unable to marshall value type into its JSON representation
     */
    @POST
    @Path(FlowStoreServiceConstants.FLOW_COMPONENTS)
    @Consumes({ MediaType.APPLICATION_JSON })
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
     * @return a HTTP OK response with result list as JSON
     */
    @GET
    @Path(FlowStoreServiceConstants.FLOW_COMPONENTS)
    @Produces({ MediaType.APPLICATION_JSON })
    public Response findAllComponents() {
        final TypedQuery<String> query = entityManager.createNamedQuery(FlowComponent.QUERY_FIND_ALL, String.class);
        return Response.ok().entity(query.getResultList().toString()).build();
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
     * @param version The version of the flow
     *
     * @return a HTTP 204 response with no content,
     *         a HTTP 404 response in case of flow ID not found,
     *         a HTTP 409 response in case an OptimisticLock or Constraint violation occurs,
     *         a HTTP 500 response in case of general error.
     */
    @DELETE
    @Path(FlowStoreServiceConstants.FLOW_COMPONENT)
    public Response deleteFlowComponent(
            @PathParam(FlowStoreServiceConstants.ID_VARIABLE) Long flowComponentId,
            @HeaderParam(FlowStoreServiceConstants.IF_MATCH_HEADER) Long version) {

        final FlowComponent flowComponent = entityManager.find(FlowComponent.class, flowComponentId);

        if(flowComponent== null) {
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
     * @param uriInfo URI information
     * @param flowComponentContent The content of the next flow component
     * @param id The flow component ID
     * @param version The version of the flow component
     *
     * @return a HTTP 200 response with flow component content as JSON
     *         a HTTP 406 NOT_ACCEPTABLE response if violating any uniqueness constraints.
     *         a HTTP 409 response in case of Concurrent Update error
     *         a HTTP 500 response in case of general error.
     *
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
