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

import dk.dbc.dataio.commons.types.SinkContent;
import dk.dbc.dataio.commons.types.rest.FlowStoreServiceConstants;
import dk.dbc.dataio.commons.utils.invariant.InvariantUtil;
import dk.dbc.dataio.flowstore.entity.Sink;
import dk.dbc.dataio.jsonb.JSONBContext;
import dk.dbc.dataio.jsonb.JSONBException;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
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

@Stateless
@Path("/")
public class SinksBean extends AbstractResourceBean {
    private static final String SINK_CONTENT_DISPLAY_TEXT = "sinkContent";
    private static final String NULL_ENTITY = "";

    JSONBContext jsonbContext = new JSONBContext();

    @PersistenceContext
    EntityManager entityManager;

    /**
     * Retrieves sink from underlying data store
     *
     * @param id sink identifier
     *
     * @return a HTTP 200 response with sink content as JSON,
     *         a HTTP 404 response with error content as JSON if not found,
     *         a HTTP 500 response in case of general error.
     *
     * @throws JSONBException on failure to create json sink
     */
    @GET
    @Path(FlowStoreServiceConstants.SINK)
    @Produces({MediaType.APPLICATION_JSON})
    public Response getSink(@PathParam(FlowStoreServiceConstants.SINK_ID_VARIABLE) Long id) throws JSONBException {
        final Sink sink = entityManager.find(Sink.class, id);
        if (sink == null) {
            return Response.status(Response.Status.NOT_FOUND).entity(NULL_ENTITY).build();
        }
        return Response
                .ok()
                .entity(jsonbContext.marshall(sink))
                .tag(sink.getVersion().toString())
                .build();
    }


    /**
     * Creates a new sink
     *
     * @param uriInfo URI information
     * @param sinkContent The content of the Sink
     *
     * @return a HTTP 201 response with sink content as JSON,
     *         a HTTP 406 response in case of Unique Restraint of Primary Key Violation
     *         a HTTP 500 response in case of general error.
     *
     * @throws JSONBException on failure to create json sink
     */
    @POST
    @Path(FlowStoreServiceConstants.SINKS)
    @Consumes({MediaType.APPLICATION_JSON})
    @Produces({MediaType.APPLICATION_JSON})
    public Response createSink(@Context UriInfo uriInfo, String sinkContent) throws JSONBException {
        InvariantUtil.checkNotNullNotEmptyOrThrow(sinkContent, SINK_CONTENT_DISPLAY_TEXT);

        // unmarshall to SinkContent to make sure the input is valid
        jsonbContext.unmarshall(sinkContent, SinkContent.class);

        final Sink sink = saveAsVersionedEntity(entityManager, Sink.class, sinkContent);
        entityManager.flush();
        final String sinkJson = jsonbContext.marshall(sink);
        return Response
                .created(getResourceUriOfVersionedEntity(uriInfo.getAbsolutePathBuilder(), sink))
                .entity(sinkJson)
                .tag(sink.getVersion().toString())
                .build();
    }

    /**
     * Updates an existing sink
     *
     * @param sinkContent The content of the sink
     * @param id The Sink ID
     * @param version The version of the sink
     *
     * @return a HTTP 200 response with sink content as JSON,
     *         a HTTP 404 response in case of Sink ID is not found,
     *         a HTTP 406 response in case of Unique Restraint of Primary Key Violation
     *         a HTTP 409 response in case of Concurrent Update error
     *         a HTTP 500 response in case of general error.
     *
     * @throws JSONBException on failure to create json sink
     */
    @POST
    @Path(FlowStoreServiceConstants.SINK_CONTENT)
    @Consumes({MediaType.APPLICATION_JSON})
    @Produces({MediaType.APPLICATION_JSON})
    public Response updateSink(String sinkContent, @PathParam(FlowStoreServiceConstants.SINK_ID_VARIABLE) Long id,
        @HeaderParam(FlowStoreServiceConstants.IF_MATCH_HEADER) Long version) throws JSONBException {

        InvariantUtil.checkNotNullNotEmptyOrThrow(sinkContent, SINK_CONTENT_DISPLAY_TEXT);

        // unmarshall to SinkContent to make sure the input is valid
        jsonbContext.unmarshall(sinkContent, SinkContent.class);

        final Sink sinkEntity = entityManager.find(Sink.class, id);
        if (sinkEntity == null) {
            return Response.status(Response.Status.NOT_FOUND).entity(NULL_ENTITY).build();
        }
        entityManager.detach(sinkEntity);
        sinkEntity.setContent(sinkContent);
        sinkEntity.setVersion(version);
        entityManager.merge(sinkEntity);
        entityManager.flush();
        final Sink updatedSink = entityManager.find(Sink.class, id);
        final String sinkJson = jsonbContext.marshall(updatedSink);
        return Response
                .ok()
                .entity(sinkJson)
                .tag(updatedSink.getVersion().toString())
                .build();
    }

    /**
     * Deletes an existing sink
     *
     * @param sinkId The Sink ID
     * @param version The version of the sink
     *
     * @return a HTTP 204 response with no content,
     *         a HTTP 404 response in case of Sink ID not found,
     *         a HTTP 409 response in case an OptimisticLock or Constraint violation occurs,
     *         a HTTP 500 response in case of general error.
     */
    @DELETE
    @Path(FlowStoreServiceConstants.SINK)
    public Response deleteSink(
            @PathParam(FlowStoreServiceConstants.SINK_ID_VARIABLE) Long sinkId,
            @HeaderParam(FlowStoreServiceConstants.IF_MATCH_HEADER) Long version) {

        final Sink sinkEntity = entityManager.find(Sink.class, sinkId);

        if(sinkEntity == null) {
            return Response.status(Response.Status.NOT_FOUND).entity(NULL_ENTITY).build();
        }

        // First we need to update the version no to see if any Optimistic Locking occurs!
        entityManager.detach(sinkEntity);
        sinkEntity.setVersion(version);
        Sink versionUpdatedAndNoOptimisticLocking = entityManager.merge(sinkEntity);

        // If no Optimistic Locking - delete it!
        entityManager.remove(versionUpdatedAndNoOptimisticLocking);
        entityManager.flush();

        return Response.noContent().build();
    }


    /**
     * Returns list of all stored sinks sorted by name in ascending order
     *
     * @return a HTTP OK response with result list as JSON
     *
     * @throws JSONBException on failure to create result list as JSON
     */
    @GET
    @Path(FlowStoreServiceConstants.SINKS)
    @Produces({ MediaType.APPLICATION_JSON })
    public Response findAllSinks() throws JSONBException {
        final Query query = entityManager.createNamedQuery(Sink.QUERY_FIND_ALL);
        final List<Sink> results = query.getResultList();
        return Response
                .ok()
                .entity(jsonbContext.marshall(results))
                .build();
    }
}

