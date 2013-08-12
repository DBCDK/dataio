package dk.dbc.dataio.flowstore.ejb;

import dk.dbc.dataio.flowstore.entity.*;
import dk.dbc.dataio.flowstore.entity.Error;
import dk.dbc.dataio.flowstore.util.json.JsonException;
import dk.dbc.dataio.flowstore.util.json.JsonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.Date;

/**
 * This Enterprise Java Bean (EJB) class acts as a JAX-RS root resource
 * exposed by the '/flows' entry point
 */
@Stateless
@Path("flows")
public class FlowsBean {
    private static final Logger log = LoggerFactory.getLogger(FlowsBean.class);

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * Retrieves flow from underlying data store
     *
     * @param id flow identifier
     * @param version flow version
     *
     * @return a HTTP 200 response with flow content as JSON,
     *         a HTTP 404 response with error content as JSON if not found,
     *         a HTTP 500 response in case of general error.
     */
    @GET
    @Path("/{id}/{version}")
    @Produces({MediaType.APPLICATION_JSON})
    public Response getFlow(@PathParam("id") Long id, @PathParam("version") Long version) throws JsonException {
        EntityPrimaryKey pk = new EntityPrimaryKey(id, new Date(version));
        final Flow flow = entityManager.find(Flow.class, pk);
        if (flow == null) {
            return buildResponse(Response.Status.NOT_FOUND, JsonUtil.toJson(new Error("not found")));
        }
        return buildResponse(Response.Status.OK, JsonUtil.toJson(flow));
    }

    /**
     * Creates new flow with data POST'ed as JSON and persists it in the
     * underlying data store
     *
     * @param flowContent flow data as JSON string
     *
     * @return a HTTP 201 response with a Location header containing the
     * URL value of the newly created resource
     *
     * @throws JsonException when given invalid (null-valued, empty-valued or non-json)
     *                       JSON string, or if JSON object does not contain required
     *                       members
     */
    @POST
    @Consumes({MediaType.APPLICATION_JSON})
    public Response createFlow(String flowContent) throws JsonException {
        log.trace("Called with: '{}'", flowContent);

        final Flow flow = new Flow();
        flow.setContent(flowContent);
        entityManager.persist(flow);
        entityManager.flush();

        return Response.created(URI.create(String.format("/%s/%d",
                flow.getId(), flow.getVersion().getTime()))).build();
    }

    private static <T> Response buildResponse(Response.Status status, T entity) {
        return Response.status(status).entity(entity).build();
    }
}
