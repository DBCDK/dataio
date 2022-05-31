package dk.dbc.dataio.jobprocessor.rest;

import dk.dbc.dataio.commons.utils.service.ServiceStatus;
import dk.dbc.dataio.jobprocessor.ejb.CapacityBean;
import dk.dbc.dataio.jobprocessor.ejb.HealthBean;
import dk.dbc.dataio.jobprocessor.exception.JobProcessorCapacityExceededException;
import dk.dbc.dataio.jobprocessor.exception.JobProcessorTerminallyIllException;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

@Stateless
@Path("/")
public class StatusBean implements ServiceStatus {
    @EJB
    CapacityBean capacityBean;

    @EJB
    HealthBean healthBean;

    @Override
    public Response getStatus() throws JobProcessorTerminallyIllException {
        if (capacityBean.isCapacityExceeded()) {
            throw new JobProcessorCapacityExceededException(String.format(
                    "Processor on shard '%s' has exceeded its capacity, forcing restart", capacityBean.getShardId()));
        }
        if (healthBean.isTerminallyIll()) {
            throw new JobProcessorTerminallyIllException(String.format(
                    "Processor on shard '%s' has reported itself terminally ill, forcing restart",
                    healthBean.getShardId()), healthBean.getCause());
        }
        return Response.ok().build();
    }
}
