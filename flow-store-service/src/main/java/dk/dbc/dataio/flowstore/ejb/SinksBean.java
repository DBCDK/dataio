package dk.dbc.dataio.flowstore.ejb;

import dk.dbc.dataio.commons.types.rest.FlowStoreServiceConstants;
import dk.dbc.dataio.commons.utils.invariant.InvariantUtil;
import dk.dbc.dataio.commons.utils.json.JsonException;
import dk.dbc.dataio.commons.utils.json.JsonUtil;
import dk.dbc.dataio.commons.utils.service.ServiceUtil;
import dk.dbc.dataio.flowstore.entity.Sink;

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

@Stateless
@Path("/")
public class SinksBean {
    private static final String NOT_FOUND_MESSAGE = "resource not found";
    private static final String SINK_CONTENT_DISPLAY_TEXT = "sinkContent";

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
     * @throws JsonException on failure to create json sink
     */
    @GET
    @Path(FlowStoreServiceConstants.SINK)
    @Produces({MediaType.APPLICATION_JSON})
    public Response getSink(@PathParam(FlowStoreServiceConstants.SINK_ID_VARIABLE) Long id) throws JsonException {
        final Sink sink = entityManager.find(Sink.class, id);
        if (sink == null) {
            return ServiceUtil.buildResponse(Response.Status.NOT_FOUND, ServiceUtil.asJsonError(NOT_FOUND_MESSAGE));
        }
        return ServiceUtil.buildResponse(Response.Status.OK, JsonUtil.toJson(sink));
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
     * @throws JsonException on failure to create json sink
     */
    @POST
    @Path(FlowStoreServiceConstants.SINKS)
    @Consumes({MediaType.APPLICATION_JSON})
    @Produces({MediaType.APPLICATION_JSON})
    public Response createSink(@Context UriInfo uriInfo, String sinkContent) throws JsonException {
        InvariantUtil.checkNotNullNotEmptyOrThrow(sinkContent, SINK_CONTENT_DISPLAY_TEXT);

        final Sink sink = saveAsVersionedEntity(entityManager, Sink.class, sinkContent);
        entityManager.flush();
        final String sinkJson = JsonUtil.toJson(sink);
        return Response.created(getResourceUriOfVersionedEntity(uriInfo.getAbsolutePathBuilder(), sink)).entity(sinkJson).build();
    }

    /**
     * Updates an existing sink
     *
     * @param uriInfo URI information
     * @param sinkContent The content of the sink
     * @param id The Sink ID
     * @param version The version of the sink
     *
     * @return a HTTP 200 response with sink content as JSON,
     *         a HTTP 406 response in case of Unique Restraint of Primary Key Violation
     *         a HTTP 409 response in case of Concurrent Update error
     *         a HTTP 500 response in case of general error.
     *
     * @throws JsonException on failure to create json sink
     */
    @POST
    @Path(FlowStoreServiceConstants.SINK_CONTENT)
    @Consumes({MediaType.APPLICATION_JSON})
    @Produces({MediaType.APPLICATION_JSON})
    public Response updateSink(@Context UriInfo uriInfo, String sinkContent, @PathParam(FlowStoreServiceConstants.SINK_ID_VARIABLE) Long id,
        @HeaderParam(FlowStoreServiceConstants.IF_MATCH_HEADER) Long version) throws JsonException {

        InvariantUtil.checkNotNullNotEmptyOrThrow(sinkContent, SINK_CONTENT_DISPLAY_TEXT);
        final Sink sinkEntity = entityManager.find(Sink.class, id);
        if (sinkEntity == null) {
            return Response.status(Response.Status.NOT_FOUND.getStatusCode()).build();
        }
        entityManager.detach(sinkEntity);
        sinkEntity.setContent(sinkContent);
        sinkEntity.setVersion(version);
        entityManager.merge(sinkEntity);
        entityManager.flush();
        final Sink updatedSink = entityManager.find(Sink.class, id);
        final String sinkJson = JsonUtil.toJson(updatedSink);
        return Response.ok(getResourceUriOfVersionedEntity(uriInfo.getAbsolutePathBuilder(), updatedSink)).entity(sinkJson).build();
    }


    /**
     * Returns list of all stored sinks sorted by name in ascending order
     *
     * @return a HTTP OK response with result list as JSON
     *
     * @throws JsonException on failure to create result list as JSON
     */
    @GET
    @Path(FlowStoreServiceConstants.SINKS)
    @Produces({ MediaType.APPLICATION_JSON })
    public Response findAllSinks() throws JsonException {
        final TypedQuery<Sink> query = entityManager.createNamedQuery(Sink.QUERY_FIND_ALL, Sink.class);
        final List<Sink> results = query.getResultList();
        return ServiceUtil.buildResponse(Response.Status.OK, JsonUtil.toJson(results));
    }
}

