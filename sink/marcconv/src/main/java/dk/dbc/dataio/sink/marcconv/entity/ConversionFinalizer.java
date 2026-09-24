package dk.dbc.dataio.sink.marcconv.entity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import dk.dbc.commons.jpa.ResultSet;
import dk.dbc.dataio.commons.conversion.ConversionMetadata;
import dk.dbc.dataio.commons.conversion.ConversionParam;
import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.types.Diagnostic;
import dk.dbc.dataio.commons.types.JobSpecification;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnector;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnectorException;
import dk.dbc.dataio.filestore.service.connector.FileStoreServiceConnector;
import dk.dbc.dataio.filestore.service.connector.FileStoreServiceConnectorException;
import dk.dbc.dataio.jobstore.types.JobInfoSnapshot;
import dk.dbc.dataio.jobstore.types.criteria.JobListCriteria;
import dk.dbc.dataio.jobstore.types.criteria.ListFilter;
import dk.dbc.dataio.jse.artemis.common.service.ServiceHub;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import static java.lang.String.format;

/**
 * It is the responsibility of this class to expose the conversion result
 */
public class ConversionFinalizer {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConversionFinalizer.class);
    public static final String ORIGIN = "dataio/sink/marcconv";

    /**
     * Buffers conversion output until it holds at least this many bytes, then appends it
     * to the file being uploaded
     * <p>
     * A threshold, not a cap. Blocks are buffered whole, so the append that crosses it
     * carries the overshoot with it, a block larger than this is appended on its own, and
     * the last append of a job carries whatever is left over.
     * <p>
     * One block holds one item's conversion output, so a job of a hundred thousand records
     * has a hundred thousand of them. Appending each one on its own would make as many
     * calls to file-store, where buffering makes roughly one per megabyte.
     */
    static final int UPLOAD_BUFFER_SIZE = 1024 * 1024;

    FileStoreServiceConnector fileStoreServiceConnector;
    JobStoreServiceConnector jobStoreServiceConnector;

    public ConversionFinalizer(ServiceHub serviceHub, FileStoreServiceConnector fileStoreServiceConnector) {
        this.fileStoreServiceConnector = fileStoreServiceConnector;
        jobStoreServiceConnector = serviceHub.jobStoreServiceConnector;
    }

    /**
     * Uploads the conversion output of every item of the job to file-store as one file and
     * discards the job's blocks and conversion parameters
     * <p>
     * Runs on the job's termination item, which reaches the sink only once every data item
     * of the job has been reported, so every block the job will ever have is committed by
     * the time this is called.
     *
     * @param jobId           job to finalize
     * @param terminationItem item triggering the finalization, whose identity the outcome
     *                        carries back
     * @return delivering outcome for the termination item, naming the uploaded file
     */
    public ChunkItem finalizeJob(int jobId, ChunkItem terminationItem, EntityManager entityManager) {
        LOGGER.info("Finalizing conversion job {}", jobId);
        JobListCriteria findJobCriteria = new JobListCriteria().where(new ListFilter<>(JobListCriteria.Field.JOB_ID, ListFilter.Op.EQUAL, jobId));
        JobInfoSnapshot jobInfoSnapshot;

        try {
            jobInfoSnapshot = jobStoreServiceConnector.listJobs(findJobCriteria).get(0);
        } catch (JobStoreServiceConnectorException e) {
            throw new RuntimeException(format("Failed to find job %d", jobId), e);
        }
        int agencyId = getConversionParam(jobId, entityManager).getSubmitter().orElse(Math.toIntExact(jobInfoSnapshot.getSpecification().getSubmitterId()));
        ConversionMetadata conversionMetadata = new ConversionMetadata(ORIGIN).withJobId(jobInfoSnapshot.getJobId()).withAgencyId(agencyId).withFilename(getConversionFilename(jobInfoSnapshot));

        Optional<ExistingFile> existingFile = fileAlreadyExists(fileStoreServiceConnector, jobId, conversionMetadata);
        String fileId;
        if (existingFile.isPresent()) {
            fileId = existingFile.get().getId();
        } else {
            fileId = uploadFile(fileStoreServiceConnector, jobId, entityManager);
            if (fileId != null) {
                uploadMetadata(fileStoreServiceConnector, jobId, fileId, conversionMetadata);
            }
        }
        LOGGER.info("Deleted {} conversion blocks for job {}", deleteConversionBlocks(jobId, entityManager), jobId);
        LOGGER.info("Deleted {} conversion params for job {}", deleteConversionParam(jobId, entityManager), jobId);

        return newResultItem(fileStoreServiceConnector, terminationItem, fileId);
    }

    public void deleteJob(int jobId, EntityManager entityManager) {
        deleteConversionBlocks(jobId, entityManager);
        deleteConversionParam(jobId, entityManager);
    }

    private Optional<ExistingFile> fileAlreadyExists(FileStoreServiceConnector fileStoreServiceConnector, Integer jobId, ConversionMetadata metadata) {
        // A file may already exist if something exploded after the call to the
        // ConversionFinalizerBean.handleTerminationChunk() method. If so we must
        // use this existing file since it has already been exposed to the end
        // users.
        try {
            List<ExistingFile> files = fileStoreServiceConnector.searchByMetadata(metadata, ExistingFile.class);
            if (files.isEmpty()) {
                return Optional.empty();
            }
            return Optional.of(files.get(0));
        } catch (FileStoreServiceConnectorException | RuntimeException e) {
            throw new RuntimeException(format("Failed check for existing file for job %d", jobId), e);
        }
    }

    /**
     * Uploads the job's blocks to file-store as one file, in ascending chunk and item
     * order, which is the order the job's records were partitioned in
     *
     * @return id of the uploaded file, or null when the job produced no conversion output
     */
    private String uploadFile(FileStoreServiceConnector fileStoreServiceConnector, int jobId, EntityManager entityManager) {
        Query getConversionBlocksQuery = entityManager.createNamedQuery(ConversionBlock.GET_CONVERSION_BLOCKS_QUERY_NAME).setParameter(1, jobId);

        String fileId = null;
        try (ResultSet<ConversionBlock> blocks = new ResultSet<>(entityManager, getConversionBlocksQuery, new ConversionBlockResultSetMapping())) {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            for (ConversionBlock block : blocks) {
                if (block == null || block.getBytes().length == 0) {
                    continue;
                }
                buffer.writeBytes(block.getBytes());
                if (buffer.size() >= UPLOAD_BUFFER_SIZE) {
                    fileId = upload(fileStoreServiceConnector, fileId, buffer);
                }
            }
            if (buffer.size() > 0) {
                fileId = upload(fileStoreServiceConnector, fileId, buffer);
            }
            LOGGER.info("Uploaded conversion file {} for job {}", fileId, jobId);
        } catch (FileStoreServiceConnectorException | RuntimeException e) {
            deleteFile(fileStoreServiceConnector, fileId);
            throw new RuntimeException(e);
        }
        return fileId;
    }

    /**
     * Writes the buffered blocks to file-store and empties the buffer
     *
     * @return id of the file written to, created by this call when there was none yet
     */
    private String upload(FileStoreServiceConnector fileStoreServiceConnector, String fileId, ByteArrayOutputStream buffer)
            throws FileStoreServiceConnectorException {
        byte[] bytes = buffer.toByteArray();
        buffer.reset();
        if (fileId == null) {
            return fileStoreServiceConnector.addFile(new ByteArrayInputStream(bytes));
        }
        fileStoreServiceConnector.appendToFile(fileId, bytes);
        return fileId;
    }

    private void uploadMetadata(FileStoreServiceConnector fileStoreServiceConnector, int jobId, String fileId, ConversionMetadata conversionMetadata) {
        try {
            fileStoreServiceConnector.addMetadata(fileId, conversionMetadata);
            LOGGER.info("Uploaded conversion metadata {} for job {}", conversionMetadata, jobId);
        } catch (FileStoreServiceConnectorException | RuntimeException e) {
            deleteFile(fileStoreServiceConnector, fileId);
            throw new RuntimeException(e);
        }
    }

    private ConversionParam getConversionParam(Integer jobId, EntityManager entityManager) {
        StoredConversionParam storedConversionParam = entityManager.find(StoredConversionParam.class, jobId);
        if (storedConversionParam == null || storedConversionParam.getParam() == null) {
            return new ConversionParam();
        }
        return storedConversionParam.getParam();
    }

    private String getConversionFilename(JobInfoSnapshot jobInfoSnapshot) {
        JobSpecification jobSpecification = jobInfoSnapshot.getSpecification();
        if (jobSpecification.getAncestry() == null || jobSpecification.getAncestry().getDatafile() == null) {
            return "marcconv." + jobInfoSnapshot.getJobId();
        }
        return jobSpecification.getAncestry().getDatafile();
    }

    private int deleteConversionBlocks(Integer jobId, EntityManager entityManager) {
        return entityManager.createNamedQuery(ConversionBlock.DELETE_CONVERSION_BLOCKS_QUERY_NAME).setParameter("jobId", jobId).executeUpdate();
    }

    private int deleteConversionParam(Integer jobId, EntityManager entityManager) {
        return entityManager.createNamedQuery(StoredConversionParam.DELETE_CONVERSION_PARAM_QUERY_NAME).setParameter("jobId", jobId).executeUpdate();
    }

    private void deleteFile(FileStoreServiceConnector fileStoreServiceConnector, String fileId) {
        try {
            LOGGER.info("Removing file with id {} from file-store", fileId);
            fileStoreServiceConnector.deleteFile(fileId);
        } catch (FileStoreServiceConnectorException | RuntimeException e) {
            LOGGER.error("Failed to remove uploaded file with id {}", fileId, e);
        }
    }

    /**
     * The delivering outcome recorded for the termination item, naming the uploaded file
     * <p>
     * A job that produced no conversion output has no file to name and is reported failed,
     * which completes it and sets its fatal error flag.
     */
    private ChunkItem newResultItem(FileStoreServiceConnector fileStoreServiceConnector, ChunkItem terminationItem, String fileId) {
        ChunkItem chunkItem;
        if (fileId == null) {
            Diagnostic diagnostic = new Diagnostic(Diagnostic.Level.ERROR, "file-store file ID is null");
            chunkItem = ChunkItem.failedChunkItem().withDiagnostics(diagnostic).withData(diagnostic.getMessage());
        } else {
            chunkItem = ChunkItem.successfulChunkItem().withData(String.join("/", fileStoreServiceConnector.getBaseUrl(), "files", fileId));
        }
        return chunkItem
                .withId(terminationItem.getId())
                .withTrackingId(terminationItem.getTrackingId())
                .withType(ChunkItem.Type.JOB_END)
                .withEncoding(StandardCharsets.UTF_8);
    }

    private static class ConversionBlockResultSetMapping implements Function<java.sql.ResultSet, ConversionBlock> {
        @Override
        public ConversionBlock apply(java.sql.ResultSet resultSet) {
            if (resultSet != null) {
                try {
                    ConversionBlock conversionBlock = new ConversionBlock();
                    conversionBlock.setKey(new ConversionBlock.Key(resultSet.getInt("JOBID"), resultSet.getInt("CHUNKID"), resultSet.getInt("ITEMID")));
                    conversionBlock.setBytes(resultSet.getBytes("BYTES"));
                    return conversionBlock;
                } catch (SQLException e) {
                    throw new PersistenceException(e);
                }
            }
            return null;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    static class ExistingFile {
        private final String id;

        @JsonCreator
        public ExistingFile(@JsonProperty("id") String id) {
            this.id = id;
        }

        public String getId() {
            return id;
        }

        @Override
        public String toString() {
            return "ExistingFile{" + "id='" + id + '\'' + '}';
        }
    }
}
