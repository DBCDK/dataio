package dk.dbc.dataio.harvester.periodicjobs.rest;

import dk.dbc.dataio.harvester.periodicjobs.HarvesterBean;
import dk.dbc.dataio.harvester.periodicjobs.HarvesterConfigurationBean;
import dk.dbc.dataio.harvester.types.HarvesterException;
import dk.dbc.dataio.harvester.types.PeriodicJobsHarvesterConfig;
import java.util.Optional;
import javax.ejb.EJB;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

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
        }
        else {
            harvesterBean.asyncExecuteFor(config.get());
            return Response.ok().build();
        }
    }
}
