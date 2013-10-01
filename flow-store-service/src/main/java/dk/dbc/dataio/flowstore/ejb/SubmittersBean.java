package dk.dbc.dataio.flowstore.ejb;

import dk.dbc.dataio.commons.types.FlowStoreServiceEntryPoint;
import dk.dbc.dataio.commons.utils.json.JsonException;
import dk.dbc.dataio.commons.utils.json.JsonUtil;
import dk.dbc.dataio.flowstore.entity.Submitter;
import dk.dbc.dataio.flowstore.util.ServiceUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.util.List;

/**
 * This Enterprise Java Bean (EJB) class acts as a JAX-RS root resource
 * exposed by the '/{@code SUBMITTERS_ENTRY_POINT}' entry point
 */
@Stateless
@Path(FlowStoreServiceEntryPoint.SUBMITTERS)
public class SubmittersBean {
    private static final Logger log = LoggerFactory.getLogger(SubmittersBean.class);

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * Creates new submitter with data POST'ed as JSON and persists it in the
     * underlying data store
     *
     * @param uriInfo application and request URI information
     * @param submitterContent submitter data as JSON string
     *
     * @return a HTTP 201 CREATED response with a Location header containing the URL value of the newly created resource.
     *         a HTTP 400 BAD_REQUEST response on invalid json content.
     *         a HTTP 406 NOT_ACCEPTABLE response if violating any uniqueness constraints.
     *         a HTTP 500 INTERNAL_SERVER_ERROR response in case of general error.
     *
     * @throws JsonException when given invalid (null-valued, empty-valued or non-json)
     *                       JSON string, or if JSON object does not contain required
     *                       members
     */
    @POST
    @Consumes({ MediaType.APPLICATION_JSON })
    public Response createSubmitter(@Context UriInfo uriInfo, String submitterContent) throws JsonException {
        log.trace("Called with: '{}'", submitterContent);

        final Submitter submitter = ServiceUtil.saveAsVersionedEntity(entityManager, Submitter.class, submitterContent);
        entityManager.flush();

        return Response.created(ServiceUtil.getResourceUriOfVersionedEntity(uriInfo.getAbsolutePathBuilder(), submitter)).build();
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
    @Produces({ MediaType.APPLICATION_JSON })
    public Response findAllSubmitters() throws JsonException {
        final TypedQuery<Submitter> query = entityManager.createNamedQuery(Submitter.QUERY_FIND_ALL, Submitter.class);
        final List<Submitter> results = query.getResultList();
        return ServiceUtil.buildResponse(Response.Status.OK, JsonUtil.toJson(results));
    }
}
