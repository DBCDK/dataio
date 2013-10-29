package dk.dbc.dataio.flowstore.ejb;

import dk.dbc.dataio.commons.types.FlowStoreServiceEntryPoint;
import dk.dbc.dataio.commons.utils.invariant.InvariantUtil;
import javax.ejb.Stateless;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;


@Stateless
@Path(FlowStoreServiceEntryPoint.SINKS)
public class SinkBean {

    @POST
    @Consumes({ MediaType.APPLICATION_JSON })
    public Response createSink(@Context UriInfo uriInfo, String sinkContent) {
        InvariantUtil.checkNotNullNotEmptyOrThrow(sinkContent, "sinkContent");
        return null;
    }
}
