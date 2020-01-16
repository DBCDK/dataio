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

package dk.dbc.dataio.jobprocessor.rest;

import dk.dbc.dataio.commons.utils.service.ServiceStatus;
import dk.dbc.dataio.jobprocessor.ejb.CapacityBean;
import dk.dbc.dataio.jobprocessor.ejb.HealthBean;
import dk.dbc.dataio.jobprocessor.exception.JobProcessorCapacityExceededException;
import dk.dbc.dataio.jobprocessor.exception.JobProcessorTerminallyIllException;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

@Stateless
@Path("/")
public class StatusBean implements ServiceStatus {
    @EJB CapacityBean capacityBean;

    @EJB
    HealthBean healthBean;

    @Override
    public Response getStatus() throws JobProcessorTerminallyIllException {
        if (capacityBean.isCapacityExceeded()) {
            throw new JobProcessorCapacityExceededException(String.format(
                    "Processor on shard '%s' has exceeded its capacity, forcing restart", capacityBean.getShardId()));
        }
        if (healthBean.isTerminallyIll()) {
            throw new JobProcessorTerminallyIllException(String.format(
                "Processor on shard '%s' has reported itself terminally ill, forcing restart",
                healthBean.getShardId()), healthBean.getCause());
        }
        return Response.ok().build();
    }
}
