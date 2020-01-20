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

package dk.dbc.dataio.commons.utils.service;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * Interface for service status resources.
 * <p>
 * Services can simply have a bean implement this interface for default 200 OK /status and /howru responses,
 * or they can override the getStatus() method when a more elaborate approach is required.
 * </p>
 */
public interface ServiceStatus {
    String OK_ENTITY = new HowRU().toJson();

    @GET
    @Path("status")
    @Produces({MediaType.APPLICATION_JSON})
    default Response getStatus() throws Throwable {
        return Response.ok().entity(OK_ENTITY).build();
    }
    
    @GET
    @Path("howru")
    @Produces({MediaType.APPLICATION_JSON})
    default Response howru() {
        try {
            return ServiceStatus.this.getStatus();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new HowRU().withException(e).toJson())
                    .build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new HowRU())
                    .build();
        }
    }
}
