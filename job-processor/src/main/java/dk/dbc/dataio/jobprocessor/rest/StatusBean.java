package dk.dbc.dataio.jobprocessor.rest;

import dk.dbc.dataio.commons.utils.service.ServiceStatus;
import dk.dbc.dataio.jobprocessor.ejb.CapacityBean;
import dk.dbc.dataio.jobprocessor.ejb.HealthBean;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ws.rs.Path;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Response;

@Stateless
@Path("/")
public class StatusBean implements ServiceStatus {
    @EJB
    CapacityBean capacityBean;

    @EJB
    HealthBean healthBean;

    @Override
    public Response getStatus() {
        if (capacityBean.isTimeout()) {
            return Response.status(Response.Status.REQUEST_TIMEOUT).entity(Entity.text("Processor on shard '%s' has exceeded its allotted time, forcing restart")).build();
        }
        if (healthBean.isTerminallyIll()) {
            Throwable cause = healthBean.getCause();
            String msg = "Processor on shard " + healthBean.getShardId() + " is marked down, forcing restart." + (cause == null ? "" :  " Reason: " + cause.getMessage());
            return Response.status(Response.Status.GONE).entity(Entity.text(msg)).build();
        }
        return Response.ok().build();
    }
}
