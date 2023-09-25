package dk.dbc.dataio.gatekeeper.operation;

import dk.dbc.dataio.common.utils.io.ByteCountingInputStream;
import dk.dbc.dataio.commons.types.Constants;
import dk.dbc.dataio.commons.types.JobSpecification;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnector;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnectorUnexpectedStatusCodeException;
import dk.dbc.dataio.filestore.service.connector.FileStoreServiceConnector;
import dk.dbc.dataio.gatekeeper.Metric;
import dk.dbc.dataio.gatekeeper.transfile.TransFile;
import dk.dbc.dataio.jobstore.types.JobError;
import dk.dbc.dataio.jobstore.types.JobInfoSnapshot;
import dk.dbc.dataio.jobstore.types.JobInputStream;
import dk.dbc.invariant.InvariantUtil;
import jakarta.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static dk.dbc.dataio.gatekeeper.Metric.TAG_FAILED;

public class CreateJobOperation implements Operation {
    private static final Opcode OPCODE = Opcode.CREATE_JOB;

    private static final Logger LOGGER = LoggerFactory.getLogger(CreateJobOperation.class);

    private final JobStoreServiceConnector jobStoreServiceConnector;
    private final FileStoreServiceConnector fileStoreServiceConnector;
    private final Path workingDir;
    private final String transfileName;
    private final String transfileData;

    public CreateJobOperation(JobStoreServiceConnector jobStoreServiceConnector,
                              FileStoreServiceConnector fileStoreServiceConnector,
                              Path workingDir,
                              String transfileName,
                              String transfileData)
            throws NullPointerException, IllegalArgumentException {
        this.jobStoreServiceConnector = InvariantUtil.checkNotNullOrThrow(jobStoreServiceConnector, "jobStoreServiceConnector");
        this.fileStoreServiceConnector = InvariantUtil.checkNotNullOrThrow(fileStoreServiceConnector, "fileStoreServiceConnector");
        this.workingDir = InvariantUtil.checkNotNullOrThrow(workingDir, "workingDir");
        this.transfileName = InvariantUtil.checkNotNullNotEmptyOrThrow(transfileName, "transfileName");
        this.transfileData = InvariantUtil.checkNotNullNotEmptyOrThrow(transfileData, "transfileData");
    }

    @Override
    public Opcode getOpcode() {
        return OPCODE;
    }

    @Override
    public void execute() throws OperationExecutionException {
        LOGGER.info("Creating job for transfile entry: '{}'", transfileData);
        final TransFile.Line transfileLine = new TransFile.Line(transfileData);
        final String fileStoreId = uploadToFileStore(transfileLine.getField("f"));

        final JobSpecification jobSpecification = JobSpecificationFactory.createJobSpecification(
                transfileLine, transfileName, fileStoreId, readAllTransfileBytes(fileStoreId));

        createJobInJobStore(jobSpecification, fileStoreId);
    }

    public JobStoreServiceConnector getJobStoreServiceConnector() {
        return jobStoreServiceConnector;
    }

    public FileStoreServiceConnector getFileStoreServiceConnector() {
        return fileStoreServiceConnector;
    }

    public Path getWorkingDir() {
        return workingDir;
    }

    public String getTransfileName() {
        return transfileName;
    }

    public String getTransfileData() {
        return transfileData;
    }

    private String uploadToFileStore(String dataFileName) throws OperationExecutionException {
        if (dataFileName == null || dataFileName.trim().isEmpty()) {
            LOGGER.warn("Datafile name was undefined");
            return Constants.MISSING_FIELD_VALUE;
        }
        final Path dataFile = workingDir.resolve(dataFileName);
        if (!Files.exists(dataFile)) {
            LOGGER.warn("Datafile '{}' does not exist", dataFile);
            return Constants.MISSING_FIELD_VALUE;
        }

        try (InputStream is = new FileInputStream(dataFile.toFile())) {
            ByteCountingInputStream countingInputStream = new ByteCountingInputStream(is);
            final String fileStoreId = fileStoreServiceConnector.addFile(countingInputStream);
            LOGGER.info("Added file with ID {} to file-store", fileStoreId);
            Metric.DATA_BYTES_UPLOADED.counter().inc(countingInputStream.getBytesRead());
            Metric.DATA_FILES_UPLOADED.counter().inc();
            return fileStoreId;
        } catch (Exception e) {
            Metric.DATA_FILES_UPLOADED.counter(TAG_FAILED).inc();
            throw new OperationExecutionException(e);
        }
    }

    private void removeFromFileStore(String fileStoreId) {
        try {
            LOGGER.info("Removing file with id {} from file-store", fileStoreId);
            fileStoreServiceConnector.deleteFile(fileStoreId);
            Metric.DATA_FILES_REMOVED.counter().inc();
        } catch (Exception e) {
            LOGGER.error("Failed to remove uploaded file with id {}", fileStoreId, e);
            Metric.DATA_FILES_REMOVED.counter(TAG_FAILED).inc();
        }
    }

    private JobInfoSnapshot createJobInJobStore(JobSpecification jobSpecification, String fileStoreId)
            throws OperationExecutionException {
        try {
            LOGGER.info("Creating job with spec:{}", jobSpecification);
            final JobInfoSnapshot jobInfoSnapshot = jobStoreServiceConnector.addJob(new JobInputStream(jobSpecification, true, 0));
            LOGGER.info("Created job in job-store with ID {} for trans file {}", jobInfoSnapshot.getJobId(), transfileName);
            Metric.CREATE_JOB.counter().inc();
            return jobInfoSnapshot;
        } catch (Exception e) {
            LOGGER.error("Caught exception from job-store communication", e);
            Metric.CREATE_JOB.counter(TAG_FAILED).inc();
            if (canRemoveFromFileStore(e)) {
                removeFromFileStore(fileStoreId);
            }
            throw new OperationExecutionException(e);
        }
    }

    private boolean canRemoveFromFileStore(Exception e) {
        boolean doRemoveFromFileStore = true;
        if (e instanceof JobStoreServiceConnectorUnexpectedStatusCodeException) {
            JobStoreServiceConnectorUnexpectedStatusCodeException statusCodeException
                    = (JobStoreServiceConnectorUnexpectedStatusCodeException) e;
            final JobError jobError = statusCodeException.getJobError();
            if (jobError != null) {
                LOGGER.error("job-store returned error: {}", jobError.getDescription());
            }
            if (statusCodeException.getStatusCode() == Response.Status.INTERNAL_SERVER_ERROR.getStatusCode()) {
                doRemoveFromFileStore = false;
            }
        }
        return doRemoveFromFileStore;
    }

    private byte[] readAllTransfileBytes(String fileStoreId) throws OperationExecutionException {
        try {
            return Files.readAllBytes(workingDir.resolve(transfileName));
        } catch (IOException e) {
            removeFromFileStore(fileStoreId);
            throw new OperationExecutionException(e);
        }
    }
}
