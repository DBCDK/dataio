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
import dk.dbc.jsonb.JSONBContext;
import dk.dbc.jsonb.JSONBException;
import dk.dbc.ticklerepo.dto.Batch;
import dk.dbc.ticklerepo.dto.DataSet;

class JobSpecificationTemplate {
    private final static JSONBContext JSONB_CONTEXT = new JSONBContext();

    static JobSpecification create(TickleRepoHarvesterConfig config, DataSet dataSet, Batch batch) throws HarvesterException {
        try {
            final TickleRepoHarvesterConfig.Content configFields = config.getContent();
            return new JobSpecification()
                    .withPackaging("addi-xml") // TODO: 12/21/16 figure out where to get this from
                    .withFormat(configFields.getFormat())
                    .withCharset("utf8")
                    .withDestination(configFields.getDestination())
                    .withSubmitterId(dataSet.getAgencyId())
                    .withMailForNotificationAboutVerification("placeholder")
                    .withMailForNotificationAboutProcessing("placeholder")
                    .withResultmailInitials("placeholder")
                    .withDataFile("placeholder")
                    .withType(configFields.getType())
                    .withAncestry(getAncestry(config, batch)
                            .withHarvesterToken(config
                                    .getHarvesterToken(getBatchId(batch))));
        } catch (RuntimeException | JSONBException e) {
            throw new HarvesterException("Unable to create job specification template", e);
        }
    }

    private static JobSpecification.Ancestry getAncestry(TickleRepoHarvesterConfig config, Batch batch)
            throws JSONBException {
        if (config.getContent().hasNotificationsEnabled()
                && batch != null
                && batch.getMetadata() != null) {
            return JSONB_CONTEXT.unmarshall(batch.getMetadata(), JobSpecification.Ancestry.class);
        }
        return new JobSpecification.Ancestry();
    }

    private static int getBatchId(Batch batch) {
        if (batch != null) {
            return batch.getId();
        }
        return 0;  // zero excludes remainder from token
    }
}
