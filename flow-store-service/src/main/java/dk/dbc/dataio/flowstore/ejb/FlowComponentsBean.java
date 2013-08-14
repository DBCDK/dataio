package dk.dbc.dataio.flowstore.ejb;

import dk.dbc.dataio.flowstore.entity.FlowComponent;
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
import java.net.URI;

@Stateless
@Path("components")
public class FlowComponentsBean {
    private static final Logger log = LoggerFactory.getLogger(FlowComponentsBean.class);

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * Creates new flow component with data POST'ed as JSON and persists it in the
     * underlying data store
     *
     * @param componentContent component data as JSON string
     *
     * @return a HTTP 201 response with a Location header containing the
     * URL value of the newly created resource
     */
    @POST
    @Consumes({ MediaType.APPLICATION_JSON })
    public Response createComponent(@Context UriInfo uriInfo, String componentContent) throws JsonException {
        log.trace("Called with: '{}'", componentContent);

        final FlowComponent component = new FlowComponent();
        component.setContent(componentContent);
        entityManager.persist(component);
        entityManager.flush();

        final URI createdUri =  uriInfo.getAbsolutePathBuilder()
                .path(String.valueOf(component.getId()))
                .path(String.valueOf(component.getVersion().getTime()))
                .build();
        return Response.created(createdUri).build();
    }
}
