package dk.dbc.dataio.flowstore.ejb;

import dk.dbc.dataio.commons.types.FlowStoreServiceEntryPoint;
import dk.dbc.dataio.commons.utils.json.JsonException;
import dk.dbc.dataio.commons.utils.json.JsonUtil;
import dk.dbc.dataio.flowstore.entity.FlowComponent;
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

import static dk.dbc.dataio.flowstore.util.ServiceUtil.buildResponse;
import static dk.dbc.dataio.flowstore.util.ServiceUtil.getResourceUriOfVersionedEntity;
import static dk.dbc.dataio.flowstore.util.ServiceUtil.saveAsVersionedEntity;

/**
 * This Enterprise Java Bean (EJB) class acts as a JAX-RS root resource
 * exposed by the '/FlowStoreServiceEntryPoint.FLOW_COMPONENTS' entry point
 */
@Stateless
@Path(FlowStoreServiceEntryPoint.FLOW_COMPONENTS)
public class FlowComponentsBean {
    private static final Logger log = LoggerFactory.getLogger(FlowComponentsBean.class);

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * Creates new flow component with data POSTed as JSON and persists it in the
     * underlying data store
     *
     * @param componentContent component data as JSON string
     *
     * @return a HTTP 201 response with a Location header containing the URL value of the newly created resource
     *         a HTTP 400 BAD_REQUEST response on invalid json content.
     *         a HTTP 406 NOT_ACCEPTABLE response if violating any uniqueness constraints.
     *         a HTTP 500 response in case of general error.
     */
    @POST
    @Consumes({ MediaType.APPLICATION_JSON })
    public Response createComponent(@Context UriInfo uriInfo, String componentContent) throws JsonException {
        log.trace("Called with: '{}'", componentContent);

        final FlowComponent component = saveAsVersionedEntity(entityManager, FlowComponent.class, componentContent);
        entityManager.flush();

        return Response.created(getResourceUriOfVersionedEntity(uriInfo.getAbsolutePathBuilder(), component)).build();
    }

    /**
     * Returns list of all versions of all stored flow components sorted by name in ascending order
     *
     * @return a HTTP OK response with result list as JSON
     *
     * @throws JsonException on failure to create result list as JSON
     */
    @GET
    @Produces({ MediaType.APPLICATION_JSON })
    public Response findAllComponents() throws JsonException {
        final TypedQuery<FlowComponent> query = entityManager.createNamedQuery(FlowComponent.QUERY_FIND_ALL, FlowComponent.class);
        final List<FlowComponent> results = query.getResultList();
        return buildResponse(Response.Status.OK, JsonUtil.toJson(results));
    }
}
