package dk.dbc.dataio.flowstore.ejb;

import dk.dbc.dataio.flowstore.entity.Sink;
import dk.dbc.dataio.commons.types.FlowStoreServiceEntryPoint;
import dk.dbc.dataio.commons.utils.invariant.InvariantUtil;
import dk.dbc.dataio.commons.utils.json.JsonException;
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
import static dk.dbc.dataio.flowstore.util.ServiceUtil.getResourceUriOfVersionedEntity;
import static dk.dbc.dataio.flowstore.util.ServiceUtil.saveAsVersionedEntity;

@Stateless
@Path(FlowStoreServiceEntryPoint.SINKS)
public class SinksBean {

    @PersistenceContext
    EntityManager entityManager;

    @POST
    @Consumes({MediaType.APPLICATION_JSON})
    public Response createSink(@Context UriInfo uriInfo, String sinkContent) throws JsonException {
        InvariantUtil.checkNotNullNotEmptyOrThrow(sinkContent, "sinkContent");

        final Sink sink = saveAsVersionedEntity(entityManager, Sink.class, sinkContent);
        entityManager.flush();

        return Response.created(getResourceUriOfVersionedEntity(uriInfo.getAbsolutePathBuilder(), sink)).build();
    }
}
