package dk.dbc.dataio.harvester.infomedia;

import dk.dbc.dataio.bfs.api.BinaryFileStore;
import dk.dbc.dataio.commons.types.FileStoreUrn;
import dk.dbc.dataio.commons.types.JobSpecification;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnector;
import dk.dbc.dataio.filestore.service.connector.FileStoreServiceConnector;
import dk.dbc.dataio.harvester.types.HarvesterException;
import dk.dbc.dataio.harvester.utils.harvesterjobbuilder.AbstractHarvesterJobBuilder;

/**
 * Class used to build dataIO job from harvested records
 */
public class JobBuilder extends AbstractHarvesterJobBuilder {
    /**
     * Class constructor
     * @param binaryFileStore binaryFileStore implementation for tmp file writing
     * @param fileStoreServiceConnector file-store service connector for datafile uploads
     * @param jobStoreServiceConnector job-store service connector for job creation
     * @param jobSpecificationTemplate job specification template
     * @throws NullPointerException if given null-valued argument
     * @throws HarvesterException on failure to create harvester data file
     * backed by temporary binary file
     */
    public JobBuilder(BinaryFileStore binaryFileStore, FileStoreServiceConnector fileStoreServiceConnector,
                      JobStoreServiceConnector jobStoreServiceConnector, JobSpecification jobSpecificationTemplate)
            throws NullPointerException, HarvesterException {
        super(binaryFileStore, fileStoreServiceConnector, jobStoreServiceConnector, jobSpecificationTemplate);
    }

    /* Returns job specification for given file ID */
    @Override
    protected JobSpecification createJobSpecification(String fileId) {
        final FileStoreUrn fileStoreUrn = FileStoreUrn.create(fileId);
        return new JobSpecification()
                .withPackaging(jobSpecificationTemplate.getPackaging())
                .withFormat(jobSpecificationTemplate.getFormat())
                .withCharset(jobSpecificationTemplate.getCharset())
                .withDestination(jobSpecificationTemplate.getDestination())
                .withSubmitterId(jobSpecificationTemplate.getSubmitterId())
                .withMailForNotificationAboutVerification(
                        JobSpecification.EMPTY_MAIL_FOR_NOTIFICATION_ABOUT_VERIFICATION)
                .withMailForNotificationAboutProcessing(
                        JobSpecification.EMPTY_MAIL_FOR_NOTIFICATION_ABOUT_PROCESSING)
                .withResultmailInitials(JobSpecification.EMPTY_RESULT_MAIL_INITIALS)
                .withDataFile(fileStoreUrn.toString())
                .withType(jobSpecificationTemplate.getType())
                .withAncestry(jobSpecificationTemplate.getAncestry());
    }
}
