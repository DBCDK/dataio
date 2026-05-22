package dk.dbc.dataio.harvester.dmatdm3;

import dk.dbc.dataio.commons.types.JobSpecification;
import dk.dbc.dataio.harvester.types.DMatDM3HarvesterConfig;
import dk.dbc.dataio.harvester.types.HarvesterException;

class JobSpecificationTemplate {

    private JobSpecificationTemplate() {}

    public static final int SUBMITTER_NUMBER_RR = 190015;
    private static final String PLACEHOLDER = "placeholder";

    static JobSpecification create(DMatDM3HarvesterConfig config) throws HarvesterException {
        try {
            final DMatDM3HarvesterConfig.Content configFields = config.getContent();
            return new JobSpecification()
                    .withPackaging("addi-xml")
                    .withFormat(configFields.getFormat())
                    .withCharset("utf8")
                    .withDestination(configFields.getDestination())
                    .withSubmitterId(SUBMITTER_NUMBER_RR)
                    .withMailForNotificationAboutVerification(PLACEHOLDER)
                    .withMailForNotificationAboutProcessing(PLACEHOLDER)
                    .withResultmailInitials(PLACEHOLDER)
                    .withDataFile(PLACEHOLDER)
                    .withType(JobSpecification.Type.TRANSIENT);
        } catch (RuntimeException e) {
            throw new HarvesterException("Unable to create job specification template", e);
        }
    }
}
