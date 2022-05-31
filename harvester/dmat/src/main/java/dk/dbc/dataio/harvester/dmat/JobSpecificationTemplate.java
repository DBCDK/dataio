package dk.dbc.dataio.harvester.dmat;

import dk.dbc.dataio.commons.types.JobSpecification;
import dk.dbc.dataio.harvester.types.DMatHarvesterConfig;
import dk.dbc.dataio.harvester.types.HarvesterException;

class JobSpecificationTemplate {
    public static final int SUBMITTER_NUMBER_RR = 190015;
    public static final int SUBMITTER_NUMBER_PUBLIZON = 150015;

    public enum JobSpecificationType {RR, PUBLISHER}

    public static int getSubmitterNumberFor(JobSpecificationType type) {
        return type == JobSpecificationType.PUBLISHER
                ? SUBMITTER_NUMBER_PUBLIZON : SUBMITTER_NUMBER_RR;
    }

    static JobSpecification create(DMatHarvesterConfig config, JobSpecificationType type) throws HarvesterException {
        try {
            final DMatHarvesterConfig.Content configFields = config.getContent();
            return new JobSpecification()
                    .withPackaging("addi-xml")
                    .withFormat(configFields.getFormat())
                    .withCharset("utf8")
                    .withDestination(type == JobSpecificationType.PUBLISHER
                            ? configFields.getPublizon() : configFields.getDestination())
                    .withSubmitterId(getSubmitterNumberFor(type))
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
