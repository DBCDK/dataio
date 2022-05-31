package dk.dbc.dataio.flowstore.ejb;

import dk.dbc.commons.jsonb.JSONBContext;
import dk.dbc.commons.jsonb.JSONBException;
import dk.dbc.dataio.commons.types.SinkContent;
import dk.dbc.dataio.commons.types.rest.FlowStoreServiceConstants;
import dk.dbc.dataio.flowstore.entity.SinkEntity;
import dk.dbc.invariant.InvariantUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    private static final Logger log = LoggerFactory.getLogger(SinksBean.class);
    private static final String SINK_CONTENT_DISPLAY_TEXT = "sinkContent";
    private static final String NULL_ENTITY = "";

    JSONBContext jsonbContext = new JSONBContext();

    @PersistenceContext
    EntityManager entityManager;

    /**
     * Retrieves sink from underlying data store
     *
     * @param id sink identifier
     * @return a HTTP 200 response with sink content as JSON,
     * a HTTP 404 response with error content as JSON if not found,
     * a HTTP 500 response in case of general error.
     * @throws JSONBException on failure to create json sink
     */
    @GET
    @Path(FlowStoreServiceConstants.SINK)
    @Produces({MediaType.APPLICATION_JSON})
    public Response getSink(@PathParam(FlowStoreServiceConstants.ID_VARIABLE) Long id) throws JSONBException {
        log.debug("getSink called with: '{}'", id);
        final SinkEntity sinkEntity = entityManager.find(SinkEntity.class, id);
        if (sinkEntity == null) {
            return Response.status(Response.Status.NOT_FOUND).entity(NULL_ENTITY).build();
        }
        return Response
                .ok()
                .entity(jsonbContext.marshall(sinkEntity))
                .tag(sinkEntity.getVersion().toString())
                .build();
    }


    /**
     * Creates a new sink
     *
     * @param uriInfo     URI information
     * @param sinkContent The content of the Sink
     * @return a HTTP 201 response with sink content as JSON,
     * a HTTP 406 response in case of Unique Restraint of Primary Key Violation
     * a HTTP 500 response in case of general error.
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

        final SinkEntity sinkEntity = saveAsVersionedEntity(entityManager, SinkEntity.class, sinkContent);
        final String sinkJson = jsonbContext.marshall(sinkEntity);
        return Response
                .created(getResourceUriOfVersionedEntity(uriInfo.getAbsolutePathBuilder(), sinkEntity))
                .entity(sinkJson)
                .tag(sinkEntity.getVersion().toString())
                .build();
    }

    /**
     * Updates an existing sink
     *
     * @param sinkContent The content of the sink
     * @param id          The Sink ID
     * @param version     The version of the sink
     * @return a HTTP 200 response with sink content as JSON,
     * a HTTP 404 response in case of Sink ID is not found,
     * a HTTP 406 response in case of Unique Restraint of Primary Key Violation
     * a HTTP 409 response in case of Concurrent Update error
     * a HTTP 500 response in case of general error.
     * @throws JSONBException on failure to create json sink
     */
    @POST
    @Path(FlowStoreServiceConstants.SINK_CONTENT)
    @Consumes({MediaType.APPLICATION_JSON})
    @Produces({MediaType.APPLICATION_JSON})
    public Response updateSink(String sinkContent, @PathParam(FlowStoreServiceConstants.ID_VARIABLE) Long id,
                               @HeaderParam(FlowStoreServiceConstants.IF_MATCH_HEADER) Long version) throws JSONBException {

        InvariantUtil.checkNotNullNotEmptyOrThrow(sinkContent, SINK_CONTENT_DISPLAY_TEXT);

        // unmarshall to SinkContent to make sure the input is valid
        jsonbContext.unmarshall(sinkContent, SinkContent.class);

        final SinkEntity sinkEntity = entityManager.find(SinkEntity.class, id);
        if (sinkEntity == null) {
            return Response.status(Response.Status.NOT_FOUND).entity(NULL_ENTITY).build();
        }
        entityManager.detach(sinkEntity);
        sinkEntity.setContent(sinkContent);
        sinkEntity.setVersion(version);
        entityManager.merge(sinkEntity);
        entityManager.flush();
        final SinkEntity updatedSinkEntity = entityManager.find(SinkEntity.class, id);
        final String sinkJson = jsonbContext.marshall(updatedSinkEntity);
        return Response
                .ok()
                .entity(sinkJson)
                .tag(updatedSinkEntity.getVersion().toString())
                .build();
    }

    /**
     * Deletes an existing sink
     *
     * @param sinkId  The Sink ID
     * @param version The version of the sink
     * @return a HTTP 204 response with no content,
     * a HTTP 404 response in case of Sink ID not found,
     * a HTTP 409 response in case an OptimisticLock or Constraint violation occurs,
     * a HTTP 500 response in case of general error.
     */
    @DELETE
    @Path(FlowStoreServiceConstants.SINK)
    public Response deleteSink(
            @PathParam(FlowStoreServiceConstants.ID_VARIABLE) Long sinkId,
            @HeaderParam(FlowStoreServiceConstants.IF_MATCH_HEADER) Long version) {

        final SinkEntity entityEntity = entityManager.find(SinkEntity.class, sinkId);

        if (entityEntity == null) {
            return Response.status(Response.Status.NOT_FOUND).entity(NULL_ENTITY).build();
        }

        // First we need to update the version no to see if any Optimistic Locking occurs!
        entityManager.detach(entityEntity);
        entityEntity.setVersion(version);
        SinkEntity versionUpdatedAndNoOptimisticLocking = entityManager.merge(entityEntity);

        // If no Optimistic Locking - delete it!
        entityManager.remove(versionUpdatedAndNoOptimisticLocking);
        entityManager.flush();

        return Response.noContent().build();
    }


    /**
     * Returns list of all stored sinks sorted by name in ascending order
     *
     * @return a HTTP OK response with result list as JSON
     * @throws JSONBException on failure to create result list as JSON
     */
    @GET
    @Path(FlowStoreServiceConstants.SINKS)
    @Produces({MediaType.APPLICATION_JSON})
    public Response findAllSinks() throws JSONBException {
        final Query query = entityManager.createNamedQuery(SinkEntity.QUERY_FIND_ALL);
        final List<SinkEntity> results = query.getResultList();
        return Response
                .ok()
                .entity(jsonbContext.marshall(results))
                .build();
    }
}

