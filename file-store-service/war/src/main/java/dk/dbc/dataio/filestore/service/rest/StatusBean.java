package dk.dbc.dataio.filestore.service.rest;

import dk.dbc.dataio.commons.utils.service.ServiceStatus;

import javax.ejb.Stateless;

@Stateless
@javax.ws.rs.Path("/")
public class StatusBean implements ServiceStatus {
}
