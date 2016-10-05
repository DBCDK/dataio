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


import dk.dbc.dataio.commons.types.FlowContent;
import dk.dbc.dataio.commons.types.exceptions.ReferencedEntityNotFoundException;
import dk.dbc.dataio.commons.types.rest.FlowStoreServiceConstants;
import dk.dbc.dataio.commons.utils.invariant.InvariantUtil;
import dk.dbc.dataio.flowstore.entity.Flow;
import dk.dbc.dataio.flowstore.entity.FlowComponent;
import dk.dbc.dataio.jsonb.JSONBContext;
import dk.dbc.dataio.jsonb.JSONBException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
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

    /**
     * Retrieves flow from underlying data store
     *
     * @param id flow identifier
     *
     * @return a HTTP 200 response with flow content as JSON,
     *         a HTTP 404 response with error content as JSON if not found,
     *         a HTTP 500 response in case of general error.
     *
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
     *
     * @return a HTTP 200 response with flow content as JSON,
     *         a HTTP 404 response with error content as JSON if not found,
     *         a HTTP 500 response in case of general error.
     *
     * @throws JSONBException if unable to marshall value type into its JSON representation
     */
    @GET
    @Path(FlowStoreServiceConstants.FLOWS)
    @Produces({MediaType.APPLICATION_JSON})
    public Response findFlows(@QueryParam("name") String name) throws JSONBException {
        if(name != null) {
            return findFlowByName(name);
        } else {
            return findAll();
        }
    }

    private Response findFlowByName(String name) throws JSONBException {
        final Query query = entityManager.createNamedQuery(Flow.QUERY_FIND_BY_NAME)
                .setParameter(1, name);
        List<Flow> flows = query.getResultList();
        if (flows.isEmpty()) {
            return Response.status(Response.Status.NOT_FOUND).entity(NULL_ENTITY).build();
        }
        return Response.ok().entity(jsonbContext.marshall(flows)).build();
    }

    private Response findAll() throws JSONBException {
        final TypedQuery<Flow> query = entityManager.createNamedQuery(Flow.QUERY_FIND_ALL, Flow.class);
        return Response.ok().entity(jsonbContext.marshall(query.getResultList())).build();
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
     * @throws JSONBException when given invalid (null-valued, empty-valued or non-json)
     *                       JSON string, or if JSON object does not contain required
     *                       members
     */
    @POST
    @Path(FlowStoreServiceConstants.FLOWS)
    @Consumes({ MediaType.APPLICATION_JSON })
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
     * @throws JSONBException on failure to create json flow
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

        Response response;
        if(isRefresh != null && isRefresh) {
            response = refreshFlowComponents(uriInfo, id, version);
        }else {
            InvariantUtil.checkNotNullNotEmptyOrThrow(flowContent, FLOW_CONTENT_DISPLAY_TEXT);
            jsonbContext.unmarshall(flowContent, FlowContent.class);
            response = updateFlowContent(flowContent, id, version);
        }
        return response;
    }

    /**
     * Returns list of all versions of all stored flows sorted by name in ascending order
     *
     * @return a HTTP OK response with result list as JSON
     *
     * @throws JSONBException on failure to create result list as JSON
     */
//    @GET
//    @Path(FlowStoreServiceConstants.FLOWS)
//    @Produces({ MediaType.APPLICATION_JSON })
//    public Response findAll() throws JSONBException {
//        final TypedQuery<Flow> query = entityManager.createNamedQuery(Flow.QUERY_FIND_ALL, Flow.class);
//        return Response.ok().entity(jsonbContext.marshall(query.getResultList())).build();
//    }

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
     * @throws JSONBException on failure to create json flow
     * @throws ReferencedEntityNotFoundException on failure to locate the flow component in the underlying database
     */
    @SuppressWarnings("PMD.UnusedPrivateMethod")
    private Response refreshFlowComponents(UriInfo uriInfo, Long id, Long version) throws JSONBException, ReferencedEntityNotFoundException {

        List<dk.dbc.dataio.commons.types.FlowComponent> flowComponentsWithLatestVersion = new ArrayList<>();

        final Flow flowEntity = entityManager.find(Flow.class, id);
        if (flowEntity == null) {
            return Response.status(Response.Status.NOT_FOUND.getStatusCode()).entity(NULL_ENTITY).build();
        }
        entityManager.detach(flowEntity);
        FlowContent flowContent = jsonbContext.unmarshall(flowEntity.getContent(), FlowContent.class);

        for(dk.dbc.dataio.commons.types.FlowComponent flowComponent : flowContent.getComponents()){
            FlowComponent flowComponentWithLatestVersion = entityManager.find(FlowComponent.class, flowComponent.getId());
            if (flowComponentWithLatestVersion == null) {
                throw new ReferencedEntityNotFoundException("Flow component with id: " + flowComponent.getId() + "could not be found in the underlying database");
            }
            String flowComponentWithLatestVersionJson = jsonbContext.marshall(flowComponentWithLatestVersion);
            dk.dbc.dataio.commons.types.FlowComponent updatedFlowComponent = jsonbContext.unmarshall(flowComponentWithLatestVersionJson, dk.dbc.dataio.commons.types.FlowComponent.class);
            flowComponentsWithLatestVersion.add(updatedFlowComponent);
        }

        FlowContent updatedFlowContent = new FlowContent(flowContent.getName(), flowContent.getDescription(), flowComponentsWithLatestVersion);

        flowEntity.setContent(jsonbContext.marshall(updatedFlowContent));
        flowEntity.setVersion(version);
        entityManager.merge(flowEntity);
        entityManager.flush();
        final Flow updatedFlow = entityManager.find(Flow.class, id);
        final String flowJson = jsonbContext.marshall(updatedFlow);
        return Response.ok(getResourceUriOfVersionedEntity(uriInfo.getAbsolutePathBuilder(), updatedFlow)).entity(flowJson).build();
    }

    /**
     * Updates an existing flow
     *
     * @param flowContent the flow content containing the changes
     * @param id The flow ID
     * @param version The version of the flow
     *
     * @return a HTTP 200 response with flow content as JSON,
     *         a HTTP 409 response in case of Concurrent Update error,
     *         a HTTP 500 response in case of general error.
     *
     * @throws JSONBException JsonException on failure to create json flow
     */
    @SuppressWarnings("PMD.UnusedPrivateMethod")
    private Response updateFlowContent(String flowContent, Long id, Long version) throws JSONBException {

        InvariantUtil.checkNotNullNotEmptyOrThrow(flowContent, FLOW_CONTENT_DISPLAY_TEXT);
        final Flow flowEntity = entityManager.find(Flow.class, id);
        if (flowEntity == null) {
            return Response.status(Response.Status.NOT_FOUND.getStatusCode()).entity(NULL_ENTITY).build();
        }
        entityManager.detach(flowEntity);
        flowEntity.setContent(flowContent);
        flowEntity.setVersion(version);
        entityManager.merge(flowEntity);
        entityManager.flush();
        final Flow updatedFlow = entityManager.find(Flow.class, id);
        final String flowJson = jsonbContext.marshall(updatedFlow);
        return Response
                .ok()
                .entity(flowJson)
                .tag(updatedFlow.getVersion().toString())
                .build();
    }

    /**
     * Deletes an existing flow
     *
     * @param flowId The flow ID
     * @param version The version of the flow
     *
     * @return a HTTP 204 response with no content,
     *         a HTTP 404 response in case of flow ID not found,
     *         a HTTP 409 response in case an OptimisticLock or Constraint violation occurs,
     *         a HTTP 500 response in case of general error.
     */
    @DELETE
    @Path(FlowStoreServiceConstants.FLOW)
    public Response deleteFlow(
            @PathParam(FlowStoreServiceConstants.ID_VARIABLE) Long flowId,
            @HeaderParam(FlowStoreServiceConstants.IF_MATCH_HEADER) Long version) {

        final Flow flowEntity = entityManager.find(Flow.class, flowId);

        if(flowEntity == null) {
            return Response.status(Response.Status.NOT_FOUND).entity(NULL_ENTITY).build();
        }

        // First we need to update the version no to see if any Optimistic Locking occurs!
        entityManager.detach(flowEntity);
        flowEntity.setVersion(version);
        Flow versionUpdatedAndNoOptimisticLocking = entityManager.merge(flowEntity);

        // If no Optimistic Locking - delete it!
        entityManager.remove(versionUpdatedAndNoOptimisticLocking);
        entityManager.flush();

        return Response.noContent().build();
    }

}
