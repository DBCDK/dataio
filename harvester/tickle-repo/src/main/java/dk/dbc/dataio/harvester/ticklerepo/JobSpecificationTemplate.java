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

package dk.dbc.dataio.harvester.ticklerepo;

import dk.dbc.dataio.commons.types.JobSpecification;
import dk.dbc.dataio.harvester.types.HarvesterException;
import dk.dbc.dataio.harvester.types.TickleRepoHarvesterConfig;
import dk.dbc.ticklerepo.dto.Batch;
import dk.dbc.ticklerepo.dto.DataSet;

import java.nio.charset.StandardCharsets;

class JobSpecificationTemplate {
    static JobSpecification create(TickleRepoHarvesterConfig config, DataSet dataSet, Batch batch) throws HarvesterException {
        try {
            final TickleRepoHarvesterConfig.Content configFields = config.getContent();
            return new JobSpecification(
                    "unknown",  // TODO: 12/21/16 figure out where to get this from
                    configFields.getFormat(),
                    StandardCharsets.UTF_8.name(),
                    configFields.getDestination(),
                    dataSet.getAgencyId(),
                    "placeholder",
                    "placeholder",
                    "placeholder",
                    "placeholder",
                    configFields.getType(),
                    new JobSpecification.Ancestry()
                            .withHarvesterToken(config.getHarvesterToken(batch.getId())));
        } catch (RuntimeException e) {
            throw new HarvesterException("Unable to create job specification template", e);
        }
    }
}
