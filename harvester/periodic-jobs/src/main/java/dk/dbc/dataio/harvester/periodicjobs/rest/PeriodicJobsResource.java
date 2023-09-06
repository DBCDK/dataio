package dk.dbc.dataio.harvester.periodicjobs.rest;

import dk.dbc.dataio.harvester.periodicjobs.HarvesterBean;
import dk.dbc.dataio.harvester.periodicjobs.HarvesterConfigurationBean;
import dk.dbc.dataio.harvester.types.HarvesterException;
import dk.dbc.dataio.harvester.types.PeriodicJobsHarvesterConfig;
import jakarta.ejb.EJB;
import jakarta.ejb.EJBException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.Optional;

@Path("/jobs")
public class PeriodicJobsResource {
    @EJB
    public HarvesterConfigurationBean harvesterConfigurationBean;

    @EJB
    public HarvesterBean harvesterBean;

    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    public Response createPeriodicJob(Long id) throws HarvesterException {
        Optional<PeriodicJobsHarvesterConfig> config = harvesterConfigurationBean.getConfig(id);
        if (!config.isPresent()) {
            return Response.status(Response.Status.NOT_FOUND).build();
        } else {
            harvesterBean.asyncExecuteFor(config.get());
            return Response.ok().build();
        }
    }

    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Path("/validate")
    public Response validatePeriodicJob(Long id) throws HarvesterException {
        Optional<PeriodicJobsHarvesterConfig> config = harvesterConfigurationBean.getConfig(id);
        if (!config.isPresent()) {
            return Response.status(Response.Status.NOT_FOUND).build();
        } else {
            try {
                final String res = harvesterBean.validateQuery(config.get());
                return Response.ok(res).build();
            } catch (EJBException ex) {
                return Response.ok(ex.getMessage()).build();
            }
        }
    }
}
