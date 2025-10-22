package dk.dbc.dataio.harvester.ticklerepo;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dk.dbc.dataio.commons.types.JobSpecification;
import dk.dbc.dataio.harvester.types.HarvesterException;
import dk.dbc.dataio.harvester.types.TickleRepoHarvesterConfig;
import dk.dbc.ticklerepo.dto.Batch;
import dk.dbc.ticklerepo.dto.DataSet;

import java.util.Optional;

class JobSpecificationTemplate {
    private final static ObjectMapper mapper = new ObjectMapper();

    static JobSpecification create(TickleRepoHarvesterConfig config, DataSet dataSet, Batch batch, Integer basedOnJob) throws HarvesterException {
        try {
            TickleRepoHarvesterConfig.Content configFields = config.getContent();
            JobSpecification specification = getSpecificationBasedOnBatch(config, batch)
                    .orElse(new JobSpecification()
                            .withMailForNotificationAboutVerification(JobSpecification.EMPTY_MAIL_FOR_NOTIFICATION_ABOUT_VERIFICATION)
                            .withMailForNotificationAboutProcessing(JobSpecification.EMPTY_MAIL_FOR_NOTIFICATION_ABOUT_PROCESSING)
                            .withResultmailInitials(JobSpecification.EMPTY_RESULT_MAIL_INITIALS)
                            .withAncestry(new JobSpecification.Ancestry().withHarvesterToken(config.getHarvesterToken(getBatchId(batch)))))
                    .withPackaging("addi-xml") // TODO: 12/21/16 figure out where to get this from
                    .withFormat(configFields.getFormat())
                    .withCharset("utf8")
                    .withDestination(configFields.getDestination())
                    .withSubmitterId(dataSet.getAgencyId())
                    .withDataFile("placeholder")
                    .withType(configFields.getType());
            specification.getAncestry().withPreviousJobId(basedOnJob);
            return specification;
        } catch (RuntimeException | JsonProcessingException e) {
            throw new HarvesterException("Unable to create job specification template", e);
        }
    }

    private static Optional<JobSpecification> getSpecificationBasedOnBatch(TickleRepoHarvesterConfig config, Batch batch) throws JsonProcessingException {
        if (config.getContent().hasNotificationsEnabled() && batch != null && batch.getMetadata() != null) {
            JobSpecification jobSpecification = mapper.readValue(batch.getMetadata(), JobSpecification.class);

            JobSpecification.Ancestry ancestry;
            if (jobSpecification.getAncestry() != null) {
                ancestry = jobSpecification.getAncestry();
            } else {
                ancestry = new JobSpecification.Ancestry();
            }
            return Optional.of(jobSpecification.withAncestry(ancestry.withHarvesterToken(config.getHarvesterToken(getBatchId(batch)))));
        }
        return Optional.empty();
    }

    private static int getBatchId(Batch batch) {
        if (batch != null) {
            return batch.getId();
        }
        return 0;  // zero excludes remainder from token
    }
}
