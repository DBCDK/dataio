package dk.dbc.dataio.flowstore.ejb;

import dk.dbc.dataio.commons.types.rest.FlowStoreServiceConstants;
import dk.dbc.dataio.commons.utils.invariant.InvariantUtil;
import dk.dbc.dataio.commons.utils.json.JsonException;
import dk.dbc.dataio.commons.utils.json.JsonUtil;
import dk.dbc.dataio.commons.utils.service.ServiceUtil;
import dk.dbc.dataio.flowstore.entity.Submitter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import javax.ws.rs.*;
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
     * @throws JsonException when given invalid (null-valued, empty-valued or non-json)
     *                       JSON string, or if JSON object does not contain required
     *                       members
     */
    @POST
    @Path(FlowStoreServiceConstants.SUBMITTERS)
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({MediaType.APPLICATION_JSON})
    public Response createSubmitter(@Context UriInfo uriInfo, String submitterContent) throws JsonException {
        log.trace("Called with: '{}'", submitterContent);
        InvariantUtil.checkNotNullNotEmptyOrThrow(submitterContent, SUBMITTER_CONTENT_DISPLAY_TEXT);

        final Submitter submitter = saveAsVersionedEntity(entityManager, Submitter.class, submitterContent);
        entityManager.flush();
        final String submitterJson = JsonUtil.toJson(submitter);
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
     * @throws JsonException on failure to create json submitter
     */
    @GET
    @Path(FlowStoreServiceConstants.SUBMITTER)
    @Produces({MediaType.APPLICATION_JSON})
    public Response getSubmitter(@PathParam(FlowStoreServiceConstants.SUBMITTER_ID_VARIABLE) Long id) throws JsonException {
        final Submitter submitter = entityManager.find(Submitter.class, id);
        if (submitter == null) {
            return Response
                    .status(Response.Status.NOT_FOUND)
                    .entity(ServiceUtil.asJsonError("Submitter with id: " + id + " not "))
                    .build();
        }
        return Response
                .ok()
                .entity(JsonUtil.toJson(submitter))
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
     * @throws JsonException on failure to create json submitter
     */
    @GET
    @Path(FlowStoreServiceConstants.SUBMITTER_SEARCHES_NUMBER)
    @Produces({MediaType.APPLICATION_JSON})
    public Response getSubmitterBySubmitterNumber(@PathParam(FlowStoreServiceConstants.SUBMITTER_NUMBER_VARIABLE) Long number) throws JsonException {
        final TypedQuery<Submitter> query = entityManager.createNamedQuery(Submitter.QUERY_FIND_BY_NUMBER, Submitter.class);
        query.setParameter(Submitter.DB_QUERY_PARAMETER_NUMBER, number);

        List results = query.getResultList();
        if(results.isEmpty()) {
            return Response
                    .status(Response.Status.NOT_FOUND)
                    .entity(ServiceUtil.asJsonError("Submitter with submitter number: " + number + " not found."))
                    .build();
        }
        Submitter submitter = query.getSingleResult();
        return Response
                .ok()
                .entity(JsonUtil.toJson(submitter))
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
     * @throws JsonException on failure to create json submitter
     */
    @POST
    @Path(FlowStoreServiceConstants.SUBMITTER_CONTENT)
    @Consumes({MediaType.APPLICATION_JSON})
    @Produces({MediaType.APPLICATION_JSON})
    public Response updateSubmitter(String submitterContent, @PathParam(FlowStoreServiceConstants.SUBMITTER_ID_VARIABLE) Long id,
        @HeaderParam(FlowStoreServiceConstants.IF_MATCH_HEADER) Long version) throws JsonException {

        InvariantUtil.checkNotNullNotEmptyOrThrow(submitterContent, SUBMITTER_CONTENT_DISPLAY_TEXT);
        final Submitter submitterEntity = entityManager.find(Submitter.class, id);
        if (submitterEntity == null) {
            return Response
                    .status(Response.Status.NOT_FOUND.getStatusCode())
                    .build();
        }
        entityManager.detach(submitterEntity);
        submitterEntity.setContent(submitterContent);
        submitterEntity.setVersion(version);
        entityManager.merge(submitterEntity);
        entityManager.flush();
        final Submitter updatedSubmitter = entityManager.find(Submitter.class, id);
        final String submitterJson = JsonUtil.toJson(updatedSubmitter);
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
     *         a HTTP 500 response in case of general error.
     */
    @DELETE
    @Path(FlowStoreServiceConstants.SUBMITTER_DELETE)
    public Response deleteSubmitter(
            @PathParam(FlowStoreServiceConstants.SUBMITTER_ID_VARIABLE) Long submitterId,
            @HeaderParam(FlowStoreServiceConstants.IF_MATCH_HEADER) Long version) {

        final Submitter submitterEntity = entityManager.find(Submitter.class, submitterId);

        if(submitterEntity == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
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
     * @throws JsonException on failure to create result list as JSON
     */
    @GET
    @Path(FlowStoreServiceConstants.SUBMITTERS)
    @Produces({ MediaType.APPLICATION_JSON })
    public Response findAllSubmitters() throws JsonException {
        final TypedQuery<Submitter> query = entityManager.createNamedQuery(Submitter.QUERY_FIND_ALL, Submitter.class);
        final List<Submitter> results = query.getResultList();
        return ServiceUtil.buildResponse(Response.Status.OK, JsonUtil.toJson(results));
    }

}
