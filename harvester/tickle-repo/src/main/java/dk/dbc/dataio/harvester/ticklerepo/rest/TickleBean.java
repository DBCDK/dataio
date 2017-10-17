/*
 * DataIO - Data IO
 *
 * Copyright (C) 2017 Dansk Bibliotekscenter a/s, Tempovej 7-11, DK-2750 Ballerup,
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

package dk.dbc.dataio.harvester.ticklerepo.rest;

import dk.dbc.ticklerepo.TickleRepo;
import dk.dbc.ticklerepo.dto.DataSet;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Stateless
@Path("/")
public class TickleBean {
    @EJB TickleRepo tickleRepo;

    @GET
    @Path("dataset/{id:\\d+}/size-estimate")
    @Produces({ MediaType.TEXT_PLAIN })
    public Response dataSetSizeEstimateById(@PathParam("id") String dataSetId) {
        final DataSet dataSet = tickleRepo.lookupDataSet(new DataSet()
                .withId(Integer.parseInt(dataSetId)))
                .orElse(null);
        return Response.ok().entity(tickleRepo.estimateSizeOf(dataSet)).build();
    }

    @GET
    @Path("dataset/{id:.+}/size-estimate")
    @Produces({ MediaType.TEXT_PLAIN })
    public Response dataSetSizeEstimateByName(@PathParam("id") String dataSetName) {
        final DataSet dataSet = tickleRepo.lookupDataSet(new DataSet()
                .withName(dataSetName))
                .orElse(null);
        return Response.ok().entity(tickleRepo.estimateSizeOf(dataSet)).build();
    }
}
