package dk.dbc.dataio.harvester.v3.rest;

import dk.dbc.dataio.commons.utils.service.ServiceStatus;
import jakarta.ejb.Stateless;
import jakarta.ws.rs.Path;

@Stateless
@Path("/")
public class StatusBean implements ServiceStatus {
}
