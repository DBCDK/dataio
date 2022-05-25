package dk.dbc.dataio.harvester.rr.rest;

import dk.dbc.dataio.commons.utils.service.ServiceStatus;
import dk.dbc.dataio.harvester.task.TaskRepo;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

@Stateless
@Path("/")
public class StatusBean implements ServiceStatus {
    @EJB TaskRepo taskRepo;

    @Override
    public Response getStatus() {
        healthCheckDatabase();
        return Response.ok().build();
    }

    private void healthCheckDatabase() {
        taskRepo.getEntityManager().createNativeQuery("SELECT 1").getSingleResult();
    }
}
