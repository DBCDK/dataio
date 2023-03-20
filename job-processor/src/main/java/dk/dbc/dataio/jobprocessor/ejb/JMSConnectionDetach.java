package dk.dbc.dataio.jobprocessor.ejb;

import dk.dbc.jms.artemis.AdminClient;
import dk.dbc.jms.artemis.AdminClientFactory;

import javax.annotation.PostConstruct;
import javax.ejb.Stateless;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

@Stateless
@Path("/")
public class JMSConnectionDetach {
    private AdminClient adminClient;
    @PostConstruct
    public void init() {
        adminClient = AdminClientFactory.getAdminClient();
    }

    @POST
    @Path("maintenance/JMS/connection/reset/{queueName}")
    public Response resetJMSConnections(@PathParam("queueName") String queueName, @QueryParam("sinkname") String sinkName) {
        String filter = null;
        if (sinkName != null && !sinkName.isEmpty()) {
            filter = String.format("resource = '%s'", sinkName);
        }
        adminClient.resetStaleConnections(queueName, filter);
        return Response.ok().build();
    }
}
