package dk.dbc.dataio.harvester.utils.jobstore;

import dk.dbc.dataio.bfs.api.BinaryFile;
import dk.dbc.dataio.bfs.api.BinaryFileStore;
import dk.dbc.dataio.commons.types.FileStoreUrn;
import dk.dbc.dataio.commons.types.JobInfo;
import dk.dbc.dataio.commons.types.JobSpecification;
import dk.dbc.dataio.commons.utils.invariant.InvariantUtil;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnector;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnectorException;
import dk.dbc.dataio.filestore.service.connector.FileStoreServiceConnector;
import dk.dbc.dataio.filestore.service.connector.FileStoreServiceConnectorException;
import dk.dbc.dataio.harvester.types.HarvesterException;
import dk.dbc.dataio.harvester.types.HarvesterXmlDataFile;
import dk.dbc.dataio.harvester.types.HarvesterXmlRecord;
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
     * @param binaryFileStore binaryFileStore for tmp file writing
     * @param fileStoreServiceConnector file-store service connector for datafile uploads
     * @param jobStoreServiceConnector job-store service connector for job creation
     * @param jobSpecificationTemplate job specification template
     * @throws NullPointerException if given null-valued argument
     * @throws HarvesterException on failure to create harvester data file
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
     * @throws HarvesterException if unable to add harvester record
     */
    public void addHarvesterRecord(HarvesterXmlRecord harvesterRecord) throws HarvesterException {
        dataFile.addRecord(harvesterRecord);
        recordsAdded++;
    }

    /**
     * Uploads harvester data file, as long as the data file contains
     * any records, to the file-store and creates job in the job-store
     * referencing the uploaded file
     * @return job info for created job, or null if no job was created
     * @throws HarvesterException on failure to upload to file-store or on
     * failure to create job in job-store
     */
    public JobInfo build() throws HarvesterException {
        dataFile.close();
        try {
            closeTmpFileOutputStream();
        } catch (IllegalStateException e) {
            throw new HarvesterException(e);
        }
        if (recordsAdded > 0) {
            final String fileId = uploadToFileStore();
            if (fileId != null) {
                return createJobInJobStore(fileId);
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

    /* Creates new job in the job-store
    */
    private JobInfo createJobInJobStore(String fileId) throws HarvesterException {
        final JobSpecification jobSpecification = createJobSpecification(fileId);
        try {
            final JobInfo jobInfo = jobStoreServiceConnector.createJob(jobSpecification);
            LOGGER.info("Created job in job-store with ID {}", jobInfo.getJobId());
            return jobInfo;
        } catch (JobStoreServiceConnectorException e) {
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
                "", "", "", fileStoreUrn.toString());
    }
}
