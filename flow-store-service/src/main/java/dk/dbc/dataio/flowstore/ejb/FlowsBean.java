package dk.dbc.dataio.flowstore.ejb;

import dk.dbc.dataio.flowstore.entity.Flow;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
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
     * GET method simply returning a plain test. Direct browser to
     * http://localhost:port/flow-store/flows to test.
     * This method exists for debug means only and will be removed in
     * the near future.
     * @return test string
     */
    /*
    @GET
    @Produces({MediaType.TEXT_PLAIN})
    public String getText() {
        return "a very testable value";
    }
    */

    @GET
    @Path("/{id}")
    @Produces({MediaType.APPLICATION_JSON})
    public Flow getFlow(@PathParam("id") Long id) {
        return entityManager.getReference(Flow.class, id);
    }

    /**
     * Creates new flow with data POST'ed as JSON and persists it in the
     * underlying data store
     *
     * @param flowData flow data as JSON string
     *
     * @return a HTTP 201 response with a Location header containing the
     * URL value of the newly created resource
     */
    @POST
    @Consumes({MediaType.APPLICATION_JSON})
    public Response createFlow(String flowData) {
        log.trace("Called with: '{}'", flowData);

        Flow flow = new Flow();
        flow.setData(flowData);
        flow.setFlownameIndexValue(extractFlownameIndexValue(flowData));
        entityManager.persist(flow);
        entityManager.flush();

        return Response.created(URI.create("/" + flow.getId())).build();
    }

    private String extractFlownameIndexValue(String flowData) {
        JSONObject obj = (JSONObject) JSONValue.parse(flowData);
        return obj.get("flowname").toString();
    }

}
