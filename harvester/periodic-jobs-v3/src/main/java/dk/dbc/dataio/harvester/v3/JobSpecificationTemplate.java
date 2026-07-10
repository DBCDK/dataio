package dk.dbc.dataio.harvester.v3;

import dk.dbc.dataio.commons.types.JobSpecification;
import dk.dbc.dataio.harvester.types.HarvesterException;
import dk.dbc.dataio.harvester.types.PeriodicJobsV3HarvesterConfig;

class JobSpecificationTemplate {
    static JobSpecification create(PeriodicJobsV3HarvesterConfig config) throws HarvesterException {
        try {
            final PeriodicJobsV3HarvesterConfig.Content configFields = config.getContent();
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
