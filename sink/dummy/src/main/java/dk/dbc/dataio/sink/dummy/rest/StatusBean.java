package dk.dbc.dataio.sink.dummy.rest;

import dk.dbc.dataio.commons.utils.service.ServiceStatus;

import javax.ejb.Stateless;
import javax.ws.rs.Path;

@Stateless
@Path("/")
public class StatusBean implements ServiceStatus {
}
