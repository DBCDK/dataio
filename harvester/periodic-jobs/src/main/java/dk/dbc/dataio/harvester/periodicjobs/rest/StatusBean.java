package dk.dbc.dataio.harvester.periodicjobs.rest;

import dk.dbc.dataio.commons.utils.service.ServiceStatus;
import jakarta.ejb.Stateless;
import jakarta.ws.rs.Path;

@Stateless
@Path("/")
public class StatusBean implements ServiceStatus {
}
