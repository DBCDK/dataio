/*
 * DataIO - Data IO
 * Copyright (C) 2015 Dansk Bibliotekscenter a/s, Tempovej 7-11, DK-2750 Ballerup,
 * Denmark. CVR: 15149043
 *
 * This file is part of DataIO.
 *
 * DataIO is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * DataIO is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with DataIO.  If not, see <http://www.gnu.org/licenses/>.
 */

package dk.dbc.dataio.flowstore.ejb;

import dk.dbc.dataio.commons.types.jndi.JndiConstants;
import dk.dbc.dataio.commons.types.rest.FlowStoreServiceConstants;
import dk.dbc.dataio.commons.utils.service.ServiceUtil;

import javax.ejb.Stateless;
import javax.naming.NamingException;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Stateless
@Path("/")
public class HarvestersBean extends AbstractResourceBean {

    /**
     * Returns list of all stored harvester RR configs
     *
     * @return a HTTP OK response with result list as JSON
     *
     * @throws NamingException on failure to create result list as JSON
     */
    @GET
    @Path(FlowStoreServiceConstants.HARVESTERS_RR_CONFIG)
    @Produces({ MediaType.APPLICATION_JSON })
    public Response getHarvesterRrConfigs() throws NamingException {
        final String jsonConfig = ServiceUtil.getStringValueFromResource(JndiConstants.CONFIG_RESOURCE_HARVESTER_RR);
        return Response
                .ok()
                .entity(jsonConfig)
                .build();
    }
}

