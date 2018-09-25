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

import dk.dbc.dataio.commons.types.rest.FlowStoreServiceConstants;
import dk.dbc.dataio.commons.utils.service.ServiceUtil;
import dk.dbc.dataio.flowstore.entity.FlowBinderWithSubmitter;
import dk.dbc.dataio.flowstore.entity.Submitter;
import dk.dbc.dataio.jsonb.JSONBContext;
import dk.dbc.dataio.jsonb.JSONBException;
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
import java.util.List;

import static dk.dbc.dataio.flowstore.util.ServiceUtil.getResourceUriOfVersionedEntity;
import static dk.dbc.dataio.flowstore.util.ServiceUtil.saveAsVersionedEntity;

/**
 * This Enterprise Java Bean (EJB) class acts as a JAX-RS root resource
 * exposed by the '/{@code SUBMITTERS_ENTRY_POINT}' entry point
 */
@Stateless
@Path("/")
public class SubmittersBean {
    private static final Logger log = LoggerFactory.getLogger(SubmittersBean.class);
    private static final String SUBMITTER_CONTENT_DISPLAY_TEXT = "submitterContent";
    private static final String NULL_ENTITY = "";

    JSONBContext jsonbContext = new JSONBContext();

    @PersistenceContext
    EntityManager entityManager;

    /**
     * Creates new submitter with data POST'ed as JSON and persists it in the
     * underlying data store
     *
     * @param uriInfo application and request URI information
     * @param submitterContent submitter data as JSON string
     *
     * @return a HTTP 201 CREATED response response with submitter content as JSON,
     *         a HTTP 400 BAD_REQUEST response on invalid json content.
     *         a HTTP 406 NOT_ACCEPTABLE response if violating any uniqueness constraints.
     *         a HTTP 500 INTERNAL_SERVER_ERROR response in case of general error.
     *
     * @throws JSONBException when given invalid (null-valued, empty-valued or non-json)
     *                       JSON string, or if JSON object does not contain required
     *                       members
     */
    @POST
    @Path(FlowStoreServiceConstants.SUBMITTERS)
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({MediaType.APPLICATION_JSON})
    public Response createSubmitter(@Context UriInfo uriInfo, String submitterContent) throws JSONBException {
        log.trace("Called with: '{}'", submitterContent);
        InvariantUtil.checkNotNullNotEmptyOrThrow(submitterContent, SUBMITTER_CONTENT_DISPLAY_TEXT);

        final Submitter submitter = saveAsVersionedEntity(entityManager, Submitter.class, submitterContent);
        entityManager.flush();
        final String submitterJson = jsonbContext.marshall(submitter);
        return Response
                .created(getResourceUriOfVersionedEntity(uriInfo.getAbsolutePathBuilder(), submitter))
                .entity(submitterJson)
                .tag(submitter.getVersion().toString())
                .build();
    }

    /**
     * Retrieves submitter from underlying data store
     *
     * @param id submitter identifier
     *
     * @return a HTTP 200 response with submitter content as JSON,
     *         a HTTP 404 response with error content as JSON if not found,
     *         a HTTP 500 response in case of general error.
     *
     * @throws JSONBException on failure to create json submitter
     */
    @GET
    @Path(FlowStoreServiceConstants.SUBMITTER)
    @Produces({MediaType.APPLICATION_JSON})
    public Response getSubmitter(@PathParam(FlowStoreServiceConstants.ID_VARIABLE) Long id) throws JSONBException {
        final Submitter submitter = entityManager.find(Submitter.class, id);
        if (submitter == null) {
            return Response.status(Response.Status.NOT_FOUND).entity(NULL_ENTITY).build();
        }
        return Response
                .ok()
                .entity(jsonbContext.marshall(submitter))
                .tag(submitter.getVersion().toString())
                .build();
    }

    /**
     * Retrieves submitter from underlying data store
     *
     * @param number submitter identifier
     *
     * @return a HTTP 200 response with submitter content as JSON,
     *         a HTTP 404 response with error content as JSON if not found,
     *         a HTTP 500 response in case of general error.
     *
     * @throws JSONBException on failure to create json submitter
     */
    @GET
    @Path(FlowStoreServiceConstants.SUBMITTER_SEARCHES_NUMBER)
    @Produces({MediaType.APPLICATION_JSON})
    public Response getSubmitterBySubmitterNumber(@PathParam(FlowStoreServiceConstants.SUBMITTER_NUMBER_VARIABLE) Long number) throws JSONBException {
        final TypedQuery<Submitter> query = entityManager.createNamedQuery(Submitter.QUERY_FIND_BY_NUMBER, Submitter.class);
        query.setParameter(Submitter.DB_QUERY_PARAMETER_NUMBER, number);

        List results = query.getResultList();
        if(results.isEmpty()) {
            return Response.status(Response.Status.NOT_FOUND).entity(NULL_ENTITY).build();
        }
        Submitter submitter = query.getSingleResult();
        return Response
                .ok()
                .entity(jsonbContext.marshall(submitter))
                .tag(submitter.getVersion().toString())
                .build();
    }

    /**
     * Updates an existing submitter
     *
     * @param submitterContent The content of the submitter
     * @param id The Submitter ID
     * @param version The version of the submitter
     *
     * @return a HTTP 200 response with submitter content as JSON,
     *         a HTTP 404 response in case of Submitter ID not found,
     *         a HTTP 406 response in case of Unique Restraint of Primary Key Violation,
     *         a HTTP 409 response in case of Concurrent Update error,
     *         a HTTP 500 response in case of general error.
     *
     * @throws JSONBException on failure to create json submitter
     */
    @POST
    @Path(FlowStoreServiceConstants.SUBMITTER_CONTENT)
    @Consumes({MediaType.APPLICATION_JSON})
    @Produces({MediaType.APPLICATION_JSON})
    public Response updateSubmitter(String submitterContent, @PathParam(FlowStoreServiceConstants.ID_VARIABLE) Long id,
        @HeaderParam(FlowStoreServiceConstants.IF_MATCH_HEADER) Long version) throws JSONBException {

        InvariantUtil.checkNotNullNotEmptyOrThrow(submitterContent, SUBMITTER_CONTENT_DISPLAY_TEXT);
        final Submitter submitterEntity = entityManager.find(Submitter.class, id);
        if (submitterEntity == null) {
            return Response.status(Response.Status.NOT_FOUND).entity(NULL_ENTITY).build();
        }
        entityManager.detach(submitterEntity);
        submitterEntity.setContent(submitterContent);
        submitterEntity.setVersion(version);
        entityManager.merge(submitterEntity);
        entityManager.flush();
        final Submitter updatedSubmitter = entityManager.find(Submitter.class, id);
        final String submitterJson = jsonbContext.marshall(updatedSubmitter);
        return Response
                .ok()
                .entity(submitterJson)
                .tag(updatedSubmitter.getVersion().toString())
                .build();
    }


    /**
     * Deletes an existing submitter
     *
     * @param submitterId The Submitter ID
     * @param version The version of the submitter
     *
     * @return a HTTP 200 response with submitter content as JSON,
     *         a HTTP 404 response in case of Submitter ID not found,
     *         a HTTP 406 response in case of Unique Restraint of Primary Key Violation,
     *         a HTTP 409 response in case an OptimisticLock or Constraint violation occurs,
     *         a HTTP 500 response in case of general error.
     */
    @DELETE
    @Path(FlowStoreServiceConstants.SUBMITTER)
    public Response deleteSubmitter(
            @PathParam(FlowStoreServiceConstants.ID_VARIABLE) Long submitterId,
            @HeaderParam(FlowStoreServiceConstants.IF_MATCH_HEADER) Long version) {

        final Submitter submitterEntity = entityManager.find(Submitter.class, submitterId);

        if(submitterEntity == null) {
            return Response.status(Response.Status.NOT_FOUND).entity(NULL_ENTITY).build();
        }

        // First we need to update the version no to see if any Optimistic Locking occurs!
        entityManager.detach(submitterEntity);
        submitterEntity.setVersion(version);
        Submitter versionUpdatedAndNoOptimisticLocking = entityManager.merge(submitterEntity);

        // If no Optimistic Locking - delete it!
        entityManager.remove(versionUpdatedAndNoOptimisticLocking);
        entityManager.flush();

        return Response.noContent().build();
    }

    /**
     * Returns list of all stored submitters sorted by name in ascending order
     *
     * @return a HTTP 200 OK response with result list as JSON.
     *         a HTTP 500 INTERNAL_SERVER_ERROR response in case of general error.
     *
     * @throws JSONBException on failure to create result list as JSON
     */
    @GET
    @Path(FlowStoreServiceConstants.SUBMITTERS)
    @Produces({ MediaType.APPLICATION_JSON })
    public Response findAllSubmitters() throws JSONBException {
        final TypedQuery<Submitter> query = entityManager.createNamedQuery(Submitter.QUERY_FIND_ALL, Submitter.class);
        final List<Submitter> results = query.getResultList();
        return ServiceUtil.buildResponse(Response.Status.OK, jsonbContext.marshall(results));
    }

    /**
     * Returns list of (flow-binder name, flow-binder ID, submitter ID) tuples
     * for all flow-binders where given submitter ID is attached
     * @param submitterId submitter ID to resolve into attached flow-binders
     *
     * @return a HTTP 200 OK response with result list as JSON
     *
     * @throws JSONBException on failure to marshall result list as JSON
     */
    @GET
    @Path(FlowStoreServiceConstants.SUBMITTER_FLOW_BINDERS)
    @Produces(MediaType.APPLICATION_JSON)
    public Response findAllFlowBindersForSubmitter(
            @PathParam(FlowStoreServiceConstants.ID_VARIABLE) Long submitterId) throws JSONBException {
        final TypedQuery<FlowBinderWithSubmitter> query = entityManager.createNamedQuery(
                FlowBinderWithSubmitter.FIND_BY_SUBMITTER, FlowBinderWithSubmitter.class)
                .setParameter("submitterId", submitterId);
        return Response.ok(jsonbContext.marshall(query.getResultList())).build();
    }
}
