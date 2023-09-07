package dk.dbc.dataio.harvester.rr.rest;

import dk.dbc.dataio.commons.utils.service.ServiceStatus;
import dk.dbc.dataio.harvester.task.TaskRepo;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;

@Stateless
@Path("/")
public class StatusBean implements ServiceStatus {
    @EJB
    TaskRepo taskRepo;

    @Override
    public Response getStatus() {
        healthCheckDatabase();
        return Response.ok().build();
    }

    private void healthCheckDatabase() {
        taskRepo.getEntityManager().createNativeQuery("SELECT 1").getSingleResult();
    }
}
