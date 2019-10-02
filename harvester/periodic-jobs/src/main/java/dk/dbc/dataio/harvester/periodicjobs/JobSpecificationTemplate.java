/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GNU GPLv3
 * See license text in LICENSE.txt
 */

package dk.dbc.dataio.harvester.periodicjobs;

import dk.dbc.dataio.commons.types.JobSpecification;
import dk.dbc.dataio.harvester.types.HarvesterException;
import dk.dbc.dataio.harvester.types.PeriodicJobsHarvesterConfig;

class JobSpecificationTemplate {
    static JobSpecification create(PeriodicJobsHarvesterConfig config) throws HarvesterException {
        try {
            final PeriodicJobsHarvesterConfig.Content configFields = config.getContent();
            final JobSpecification.Ancestry ancestry = new JobSpecification.Ancestry()
                .withHarvesterToken(config.getHarvesterToken());
            return new JobSpecification()
                    .withPackaging("addi-xml")
                    .withFormat(configFields.getFormat())
                    .withCharset("utf8")
                    .withDestination(configFields.getDestination())
                    .withSubmitterId(Long.parseLong(configFields.getSubmitterNumber()))
                    .withMailForNotificationAboutVerification("placeholder")
                    .withMailForNotificationAboutProcessing("placeholder")
                    .withResultmailInitials("placeholder")
                    .withDataFile("placeholder")
                    .withAncestry(ancestry)
                    .withType(JobSpecification.Type.PERIODIC);
        } catch (RuntimeException e) {
            throw new HarvesterException("Unable to create job specification template", e);
        }
    }
}
