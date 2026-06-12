package dk.dbc.dataio.harvester.retriever;

import dk.dbc.dataio.commons.types.JobSpecification;
import dk.dbc.dataio.harvester.types.HarvesterException;
import dk.dbc.dataio.harvester.types.RetrieverHarvesterConfig;

class JobSpecificationTemplate {
    public static final int SUBMITTER_NUMBER = 190002;

    static JobSpecification create(RetrieverHarvesterConfig config) throws HarvesterException {
        try {
            final RetrieverHarvesterConfig.Content configFields = config.getContent();
            return new JobSpecification()
                    .withPackaging("addi-json")
                    .withFormat(configFields.getFormat())
                    .withCharset("utf8")
                    .withDestination(configFields.getDestination())
                    .withSubmitterId(SUBMITTER_NUMBER)
                    .withMailForNotificationAboutVerification("placeholder")
                    .withMailForNotificationAboutProcessing("placeholder")
                    .withResultmailInitials("placeholder")
                    .withDataFile("placeholder")
                    .withType(JobSpecification.Type.RETRIEVER);
        } catch (RuntimeException e) {
            throw new HarvesterException("Unable to create job specification template", e);
        }
    }
}
