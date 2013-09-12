package dk.dbc.dataio.flowstore.ejb;

import dk.dbc.dataio.flowstore.entity.Submitter;
import dk.dbc.dataio.flowstore.util.ServiceUtil;
import dk.dbc.dataio.flowstore.util.json.JsonException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

/**
 * This Enterprise Java Bean (EJB) class acts as a JAX-RS root resource
 * exposed by the '/{@code SUBMITTERS_ENTRY_POINT}' entry point
 */
@Stateless
@Path(SubmittersBean.SUBMITTERS_ENTRY_POINT)
public class SubmittersBean {
    public static final String SUBMITTERS_ENTRY_POINT = "submitters";

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
     * @return a HTTP 201 response with a Location header containing the URL value of the newly created resource
     *         a HTTP 406 response on invalid json content
     *         a HTTP 409 response if violating any uniqueness constraints
     *         a HTTP 500 response in case of general error.
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
}
