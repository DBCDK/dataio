/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GPLv3
 * See license text in LICENSE.txt or at https://opensource.dbc.dk/licenses/gpl-3.0/
 */

package dk.dbc.dataio.jobrerunrules.service.rest;

import dk.dbc.serviceutils.ServiceStatus;

import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import javax.ws.rs.Path;

@Stateless
@LocalBean
@Path("")
public class StatusBean implements ServiceStatus {
}
