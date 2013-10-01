package dk.dbc.dataio.flowstore.ejb;

import dk.dbc.dataio.commons.types.FlowStoreServiceEntryPoint;
import dk.dbc.dataio.commons.utils.json.JsonException;
import dk.dbc.dataio.commons.utils.json.JsonUtil;
import dk.dbc.dataio.commons.utils.service.ServiceUtil;
import dk.dbc.dataio.flowstore.entity.Flow;
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
 * exposed by the '/{@code FLOWS_ENTRY_POINT}' entry point
 */
@Stateless
@Path(FlowStoreServiceEntryPoint.FLOWS)
public class FlowsBean {
    private static final Logger log = LoggerFactory.getLogger(FlowsBean.class);

    private static final String NOT_FOUND_MESSAGE = "resource not found";

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * Retrieves flow from underlying data store
     *
     * @param id flow identifier
     *
     * @return a HTTP 200 response with flow content as JSON,
     *         a HTTP 404 response with error content as JSON if not found,
     *         a HTTP 500 response in case of general error.
     */
    @GET
    @Path("/{id}")
    @Produces({MediaType.APPLICATION_JSON})
    public Response getFlow(@PathParam("id") Long id) throws JsonException {
        final Flow flow = entityManager.find(Flow.class, id);
        if (flow == null) {
            return ServiceUtil.buildResponse(Response.Status.NOT_FOUND, ServiceUtil.asJsonError(NOT_FOUND_MESSAGE));
        }
        return ServiceUtil.buildResponse(Response.Status.OK, JsonUtil.toJson(flow));
    }

    /**
     * Creates new flow with data POSTed as JSON and persists it in the
     * underlying data store
     *
     * @param uriInfo application and request URI information
     * @param flowContent flow data as JSON string
     *
     * @return a HTTP 201 response with a Location header containing the URL value of the newly created resource
     *         a HTTP 400 BAD_REQUEST response on invalid json content.
     *         a HTTP 406 NOT_ACCEPTABLE response if violating any uniqueness constraints.
     *         a HTTP 500 response in case of general error.
     *
     * @throws JsonException when given invalid (null-valued, empty-valued or non-json)
     *                       JSON string, or if JSON object does not contain required
     *                       members
     */
    @POST
    @Consumes({ MediaType.APPLICATION_JSON })
    public Response createFlow(@Context UriInfo uriInfo, String flowContent) throws JsonException {
        log.trace("Called with: '{}'", flowContent);

        final Flow flow = saveAsVersionedEntity(entityManager, Flow.class, flowContent);
        entityManager.flush();

        return Response.created(getResourceUriOfVersionedEntity(uriInfo.getAbsolutePathBuilder(), flow)).build();
    }

    /**
     * Returns list of all versions of all stored flows sorted by name in ascending order
     *
     * @return a HTTP OK response with result list as JSON
     *
     * @throws JsonException on failure to create result list as JSON
     */
    @GET
    @Produces({ MediaType.APPLICATION_JSON })
    public Response findAllFlows() throws JsonException {
        final TypedQuery<Flow> query = entityManager.createNamedQuery(Flow.QUERY_FIND_ALL, Flow.class);
        final List<Flow> results = query.getResultList();
        return ServiceUtil.buildResponse(Response.Status.OK, JsonUtil.toJson(results));
    }
}
