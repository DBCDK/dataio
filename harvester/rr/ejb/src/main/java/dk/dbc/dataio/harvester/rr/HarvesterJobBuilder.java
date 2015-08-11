package dk.dbc.dataio.harvester.rr;

import dk.dbc.dataio.bfs.api.BinaryFile;
import dk.dbc.dataio.bfs.api.BinaryFileStore;
import dk.dbc.dataio.commons.types.FileStoreUrn;
import dk.dbc.dataio.commons.types.JobSpecification;
import dk.dbc.dataio.commons.utils.invariant.InvariantUtil;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnector;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnectorException;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnectorUnexpectedStatusCodeException;
import dk.dbc.dataio.filestore.service.connector.FileStoreServiceConnector;
import dk.dbc.dataio.filestore.service.connector.FileStoreServiceConnectorException;
import dk.dbc.dataio.harvester.types.HarvesterException;
import dk.dbc.dataio.harvester.types.HarvesterXmlDataFile;
import dk.dbc.dataio.harvester.types.HarvesterXmlRecord;
import dk.dbc.dataio.jobstore.types.JobError;
import dk.dbc.dataio.jobstore.types.JobInfoSnapshot;
import dk.dbc.dataio.jobstore.types.JobInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.UUID;

public class HarvesterJobBuilder implements AutoCloseable {
    private static final Logger LOGGER = LoggerFactory.getLogger(HarvesterJobBuilder.class);

    private final BinaryFile tmpFile;
    private final OutputStream tmpFileOutputStream;
    private final HarvesterXmlDataFile dataFile;
    private final JobSpecification jobSpecificationTemplate;
    private final BinaryFileStore binaryFileStore;
    private final FileStoreServiceConnector fileStoreServiceConnector;
    private final JobStoreServiceConnector jobStoreServiceConnector;
    private int recordsAdded = 0;

    /**
     * Class constructor
     * @param binaryFileStore binaryFileStore implementation for tmp file writing
     * @param fileStoreServiceConnector file-store service connector for datafile uploads
     * @param jobStoreServiceConnector job-store service connector for job creation
     * @param jobSpecificationTemplate job specification template
     * @throws NullPointerException if given null-valued argument
     * @throws dk.dbc.dataio.harvester.types.HarvesterException on failure to create harvester data file
     * backed by temporary binary file
     */
    public HarvesterJobBuilder(BinaryFileStore binaryFileStore,
                               FileStoreServiceConnector fileStoreServiceConnector,
                               JobStoreServiceConnector jobStoreServiceConnector,
                               JobSpecification jobSpecificationTemplate) throws NullPointerException, HarvesterException {
        this.binaryFileStore = InvariantUtil.checkNotNullOrThrow(binaryFileStore, "binaryFileStore");
        this.fileStoreServiceConnector = InvariantUtil.checkNotNullOrThrow(fileStoreServiceConnector, "fileStoreServiceConnector");
        this.jobStoreServiceConnector = InvariantUtil.checkNotNullOrThrow(jobStoreServiceConnector, "jobStoreServiceConnector");
        this.jobSpecificationTemplate = InvariantUtil.checkNotNullOrThrow(jobSpecificationTemplate, "jobSpecificationTemplate");
        this.tmpFile = getTmpFile();
        this.tmpFileOutputStream = tmpFile.openOutputStream();
        this.dataFile = new HarvesterXmlDataFile(StandardCharsets.UTF_8, tmpFileOutputStream);
    }

    /**
     * Adds given record to harvester data file
     * @param harvesterRecord harvester record
     * @throws dk.dbc.dataio.harvester.types.HarvesterException if unable to add harvester record
     */
    public void addHarvesterRecord(HarvesterXmlRecord harvesterRecord) throws HarvesterException {
        dataFile.addRecord(harvesterRecord);
        recordsAdded++;
    }

    /**
     * Uploads harvester data file, as long as the data file contains
     * any records, to the file-store and creates job in the job-store
     * referencing the uploaded file
     * @return job info snapshot for created job, or null if no job was created
     * @throws HarvesterException on failure to upload to file-store or on
     * failure to create job in job-store
     */
    public JobInfoSnapshot build() throws HarvesterException {
        dataFile.close();
        try {
            closeTmpFileOutputStream();
        } catch (IllegalStateException e) {
            throw new HarvesterException(e);
        }
        if (recordsAdded > 0) {
            final String fileId = uploadToFileStore();
            if (fileId != null) {
                try {
                    return createJobInJobStore(fileId);
                } catch (HarvesterException e) {
                    removeFromFileStore(fileId);
                    throw e;
                }
            }
        }
        return null;
    }

    public int getRecordsAdded() {
        return recordsAdded;
    }

    /**
     * Closes associated file resources and deletes temporary file
     * @throws IllegalStateException if unable to close temporary file output stream
     */
    @Override
    public void close() throws IllegalStateException {
        closeTmpFileOutputStream();
        deleteTmpFile();
    }

    private void closeTmpFileOutputStream() throws IllegalStateException {
        try {
            tmpFileOutputStream.close();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private void deleteTmpFile() {
        try {
            tmpFile.delete();
        } catch (IllegalStateException e) {
            // We don't want to rollback the entire transaction, just
            // because we can't remove the temporary file.
            LOGGER.warn("Unable to delete temporary file {}", tmpFile.getPath(), e);
        }
    }

    /* Returns temporary binary file for harvested data
    */
    private BinaryFile getTmpFile() {
        return binaryFileStore.getBinaryFile(Paths.get(UUID.randomUUID().toString()));
    }

    /* Uploads harvester data file to the file-store
     */
    private String uploadToFileStore() throws HarvesterException {
        String fileId = null;
        try (final InputStream is = tmpFile.openInputStream()) {
            fileId = fileStoreServiceConnector.addFile(is);
            LOGGER.info("Added file with ID {} to file-store", fileId);
        } catch (FileStoreServiceConnectorException e) {
            throw new HarvesterException("Unable to add file to file-store", e);
        } catch (IOException e) {
            LOGGER.warn("Unable to close tmp file InputStream");
        }
        return fileId;
    }

    private void removeFromFileStore(String fileId) {
        try {
            LOGGER.info("Removing file with id {} from file-store", fileId);
            fileStoreServiceConnector.deleteFile(fileId);
        } catch (FileStoreServiceConnectorException e) {
            LOGGER.error("Failed to remove uploaded file with id {}", fileId, e);
        }
    }

    /* Creates new job in the job-store
    */
    private JobInfoSnapshot createJobInJobStore(String fileId) throws HarvesterException {
        final JobSpecification jobSpecification = createJobSpecification(fileId);
        try {
            final JobInfoSnapshot jobInfoSnapshot = jobStoreServiceConnector.addJob(new JobInputStream(jobSpecification, true, 0));
            LOGGER.info("Created job in job-store with ID {}", jobInfoSnapshot.getJobId());
            return jobInfoSnapshot;
        } catch (JobStoreServiceConnectorException e) {
            if (e instanceof JobStoreServiceConnectorUnexpectedStatusCodeException) {
                final JobError jobError = ((JobStoreServiceConnectorUnexpectedStatusCodeException) e).getJobError();
                if (jobError != null) {
                    LOGGER.error("job-store returned error: {}", jobError.getDescription());
                }
            }
            throw new HarvesterException("Unable to create job in job-store", e);
        }
    }

    /* Returns job specification for given file ID
    */
    private JobSpecification createJobSpecification(String fileId) throws HarvesterException {
        final FileStoreUrn fileStoreUrn;
        try {
            fileStoreUrn = FileStoreUrn.create(fileId);
        } catch (URISyntaxException e) {
            throw new HarvesterException("Unable to create FileStoreUrn", e);
        }
        return new JobSpecification(
                jobSpecificationTemplate.getPackaging(),
                jobSpecificationTemplate.getFormat(),
                jobSpecificationTemplate.getCharset(),
                jobSpecificationTemplate.getDestination(),
                jobSpecificationTemplate.getSubmitterId(),
                JobSpecification.EMPTY_MAIL_FOR_NOTIFICATION_ABOUT_VERIFICATION,
                JobSpecification.EMPTY_MAIL_FOR_NOTIFICATION_ABOUT_PROCESSING,
                JobSpecification.EMPTY_RESULT_MAIL_INITIALS,
                fileStoreUrn.toString(),
                jobSpecificationTemplate.getType());
    }
}
