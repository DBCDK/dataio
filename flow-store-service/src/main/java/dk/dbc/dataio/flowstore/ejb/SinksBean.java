package dk.dbc.dataio.flowstore.ejb;

import dk.dbc.dataio.commons.types.FlowStoreServiceEntryPoint;
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
@Path(FlowStoreServiceEntryPoint.SINKS)
public class SinksBean {
    private static final String NOT_FOUND_MESSAGE = "resource not found";

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
     */
    @GET
    @Path("/{id}")
    @Produces({MediaType.APPLICATION_JSON})
    public Response getSink(@PathParam("id") Long id) throws JsonException {
        final Sink sink = entityManager.find(Sink.class, id);
        if (sink == null) {
            return ServiceUtil.buildResponse(Response.Status.NOT_FOUND, ServiceUtil.asJsonError(NOT_FOUND_MESSAGE));
        }
        return ServiceUtil.buildResponse(Response.Status.OK, JsonUtil.toJson(sink));
    }

    @POST
    @Consumes({MediaType.APPLICATION_JSON})
    public Response createSink(@Context UriInfo uriInfo, String sinkContent) throws JsonException {
        InvariantUtil.checkNotNullNotEmptyOrThrow(sinkContent, "sinkContent");

        final Sink sink = saveAsVersionedEntity(entityManager, Sink.class, sinkContent);
        entityManager.flush();

        return Response.created(getResourceUriOfVersionedEntity(uriInfo.getAbsolutePathBuilder(), sink)).build();
    }

    /**
     * Returns list of all stored sinks sorted by name in ascending order
     *
     * @return a HTTP OK response with result list as JSON
     *
     * @throws JsonException on failure to create result list as JSON
     */
    @GET
    @Produces({ MediaType.APPLICATION_JSON })
    public Response findAllSinks() throws JsonException {
        final TypedQuery<Sink> query = entityManager.createNamedQuery(Sink.QUERY_FIND_ALL, Sink.class);
        final List<Sink> results = query.getResultList();
        return ServiceUtil.buildResponse(Response.Status.OK, JsonUtil.toJson(results));
    }
}
