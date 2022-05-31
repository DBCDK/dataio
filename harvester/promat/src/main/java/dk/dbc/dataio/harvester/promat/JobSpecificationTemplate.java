package dk.dbc.dataio.harvester.promat;

import dk.dbc.dataio.commons.types.JobSpecification;
import dk.dbc.dataio.harvester.types.HarvesterException;
import dk.dbc.dataio.harvester.types.PromatHarvesterConfig;

class JobSpecificationTemplate {
    public static final int SUBMITTER_NUMBER = 190976;

    static JobSpecification create(PromatHarvesterConfig config) throws HarvesterException {
        try {
            final PromatHarvesterConfig.Content configFields = config.getContent();
            return new JobSpecification()
                    .withPackaging("addi-xml")
                    .withFormat(configFields.getFormat())
                    .withCharset("utf8")
                    .withDestination(configFields.getDestination())
                    .withSubmitterId(SUBMITTER_NUMBER)
                    .withMailForNotificationAboutVerification("placeholder")
                    .withMailForNotificationAboutProcessing("placeholder")
                    .withResultmailInitials("placeholder")
                    .withDataFile("placeholder")
                    .withType(JobSpecification.Type.TRANSIENT);
        } catch (RuntimeException e) {
            throw new HarvesterException("Unable to create job specification template", e);
        }
    }
}
