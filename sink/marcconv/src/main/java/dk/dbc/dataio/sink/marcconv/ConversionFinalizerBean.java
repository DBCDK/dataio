package dk.dbc.dataio.sink.marcconv;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import dk.dbc.commons.jpa.ResultSet;
import dk.dbc.dataio.commons.conversion.ConversionMetadata;
import dk.dbc.dataio.commons.conversion.ConversionParam;
import dk.dbc.dataio.commons.types.Chunk;
import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.types.Diagnostic;
import dk.dbc.dataio.commons.types.JobSpecification;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnectorException;
import dk.dbc.dataio.commons.utils.jobstore.ejb.JobStoreServiceConnectorBean;
import dk.dbc.dataio.filestore.service.connector.FileStoreServiceConnector;
import dk.dbc.dataio.filestore.service.connector.FileStoreServiceConnectorException;
import dk.dbc.dataio.filestore.service.connector.ejb.FileStoreServiceConnectorBean;
import dk.dbc.dataio.jobstore.types.JobInfoSnapshot;
import dk.dbc.dataio.jobstore.types.criteria.JobListCriteria;
import dk.dbc.dataio.jobstore.types.criteria.ListFilter;
import dk.dbc.dataio.sink.types.SinkException;
import dk.dbc.util.Timed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceException;
import javax.persistence.Query;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import static java.lang.String.format;

/**
 * It is the responsibility of this class to expose the conversion result
 */
@Stateless
public class ConversionFinalizerBean {
    public static String ORIGIN = "dataio/sink/marcconv";

    private static final Logger LOGGER = LoggerFactory.getLogger(ConversionFinalizerBean.class);

    @PersistenceContext(unitName = "marcconv_PU")
    EntityManager entityManager;

    @EJB public FileStoreServiceConnectorBean fileStoreServiceConnectorBean;
    @EJB public JobStoreServiceConnectorBean jobStoreServiceConnectorBean;

    @Timed
    public Chunk handleTerminationChunk(Chunk chunk) throws SinkException {
        final Integer jobId = Math.toIntExact(chunk.getJobId());
        LOGGER.info("Finalizing conversion job {}", jobId);
        final JobListCriteria findJobCriteria = new JobListCriteria()
                .where(new ListFilter<>(JobListCriteria.Field.JOB_ID,
                        ListFilter.Op.EQUAL, jobId));
        JobInfoSnapshot jobInfoSnapshot;

        try {
             jobInfoSnapshot = jobStoreServiceConnectorBean.getConnector().listJobs(findJobCriteria).get(0);
        } catch (JobStoreServiceConnectorException e) {
            throw new SinkException(
                    format("Failed to find job %d", jobId), e);
        }
        final int agencyId = getConversionParam(jobId).getSubmitter()
                .orElse(Math.toIntExact(jobInfoSnapshot.getSpecification().getSubmitterId()));
        final ConversionMetadata conversionMetadata = new ConversionMetadata(ORIGIN)
                .withJobId(jobInfoSnapshot.getJobId())
                .withAgencyId(agencyId)
                .withFilename(getConversionFilename(jobInfoSnapshot));

        final FileStoreServiceConnector fileStoreServiceConnector = fileStoreServiceConnectorBean.getConnector();
        final Optional<ExistingFile> existingFile =
                fileAlreadyExists(fileStoreServiceConnector, jobId, conversionMetadata);
        String fileId;
        if (existingFile.isPresent()) {
            fileId = existingFile.get().getId();
        } else {
            fileId = uploadFile(fileStoreServiceConnector, chunk);
            if (fileId != null) {
                uploadMetadata(fileStoreServiceConnector, chunk, fileId, conversionMetadata);
            }
        }
        LOGGER.info("Deleted {} conversion blocks for job {}",
                deleteConversionBlocks(jobId), jobId);
        LOGGER.info("Deleted {} conversion params for job {}",
                deleteConversionParam(jobId), jobId);

        return newResultChunk(fileStoreServiceConnector, chunk, fileId);
    }

    private Optional<ExistingFile> fileAlreadyExists(FileStoreServiceConnector fileStoreServiceConnector,
                                                     Integer jobId, ConversionMetadata metadata) throws SinkException {
        // A file may already exist if something exploded after the call to the
        // ConversionFinalizerBean.handleTerminationChunk() method. If so we must
        // use this existing file since it has already been exposed to the end
        // users.
        try {
            final List<ExistingFile> files = fileStoreServiceConnector.searchByMetadata(metadata, ExistingFile.class);
            if (files.isEmpty()) {
                return Optional.empty();
            }
            return Optional.of(files.get(0));
        } catch (FileStoreServiceConnectorException
                | RuntimeException e) {
            throw new SinkException(
                    format("Failed check for existing file for job %d", jobId), e);
        }
    }

    private String uploadFile(FileStoreServiceConnector fileStoreServiceConnector, Chunk chunk) throws SinkException {
        final Integer jobId = Math.toIntExact(chunk.getJobId());
        final Query getConversionBlocksQuery = entityManager
                .createNamedQuery(ConversionBlock.GET_CONVERSION_BLOCKS_QUERY_NAME)
                .setParameter(1, jobId);

        String fileId = null;
        try (ResultSet<ConversionBlock> blocks = new ResultSet<>(
                entityManager, getConversionBlocksQuery,
                new ConversionBlockResultSetMapping())) {
            for (ConversionBlock block : blocks) {
                if (block == null || block.getBytes().length == 0) {
                    continue;
                }
                if (fileId == null) {
                    fileId = fileStoreServiceConnector.addFile(new ByteArrayInputStream(block.getBytes()));
                } else {
                    fileStoreServiceConnector.appendToFile(fileId, block.getBytes());
                }
            }
            LOGGER.info("Uploaded conversion file {} for job {}", fileId, jobId);
        } catch (FileStoreServiceConnectorException
                | RuntimeException e) {
            deleteFile(fileStoreServiceConnector, fileId);
            throw new SinkException(e);
        }
        return fileId;
    }

    private void uploadMetadata(FileStoreServiceConnector fileStoreServiceConnector, Chunk chunk, String fileId,
                                ConversionMetadata conversionMetadata) throws SinkException {
        try {
            fileStoreServiceConnector.addMetadata(fileId, conversionMetadata);
            LOGGER.info("Uploaded conversion metadata {} for job {}",
                    conversionMetadata, chunk.getJobId());
        } catch (FileStoreServiceConnectorException
                | RuntimeException e) {
            deleteFile(fileStoreServiceConnector, fileId);
            throw new SinkException(e);
        }
    }

    private ConversionParam getConversionParam(Integer jobId) {
        final StoredConversionParam storedConversionParam =
                entityManager.find(StoredConversionParam.class, jobId);
        if (storedConversionParam == null || storedConversionParam.getParam() == null) {
            return new ConversionParam();
        }
        return storedConversionParam.getParam();
    }

    private String getConversionFilename(JobInfoSnapshot jobInfoSnapshot) {
        final JobSpecification jobSpecification = jobInfoSnapshot.getSpecification();
        if (jobSpecification.getAncestry() == null
                || jobSpecification.getAncestry().getDatafile() == null) {
            return "marcconv." + jobInfoSnapshot.getJobId();
        }
        return jobSpecification.getAncestry().getDatafile();
    }

    private int deleteConversionBlocks(Integer jobId) {
        return entityManager
                .createNamedQuery(ConversionBlock.DELETE_CONVERSION_BLOCKS_QUERY_NAME)
                .setParameter("jobId", jobId)
                .executeUpdate();
    }

    private int deleteConversionParam(Integer jobId) {
        return entityManager
                .createNamedQuery(StoredConversionParam.DELETE_CONVERSION_PARAM_QUERY_NAME)
                .setParameter("jobId", jobId)
                .executeUpdate();
    }

    private void deleteFile(FileStoreServiceConnector fileStoreServiceConnector, String fileId) {
        try {
            LOGGER.info("Removing file with id {} from file-store", fileId);
            fileStoreServiceConnector.deleteFile(fileId);
        } catch (FileStoreServiceConnectorException | RuntimeException e) {
            LOGGER.error("Failed to remove uploaded file with id {}", fileId, e);
        }
    }

    private Chunk newResultChunk(FileStoreServiceConnector fileStoreServiceConnector, Chunk chunk, String fileId) {
        final Chunk result = new Chunk(chunk.getJobId(), chunk.getChunkId(), Chunk.Type.DELIVERED);
        final ChunkItem chunkItem;
        if (fileId == null) {
            final Diagnostic diagnostic = new Diagnostic(
                    Diagnostic.Level.ERROR, "file-store file ID is null");
            chunkItem = ChunkItem.failedChunkItem()
                    .withDiagnostics(diagnostic)
                    .withData(diagnostic.getMessage());
        } else {
            chunkItem = ChunkItem.successfulChunkItem()
                    .withData(String.join("/", fileStoreServiceConnector.getBaseUrl(),
                        "files", fileId));
        }
        result.insertItem(chunkItem
                .withId(0)
                .withType(ChunkItem.Type.JOB_END)
                .withEncoding(StandardCharsets.UTF_8));
        return result;
    }

    private static class ConversionBlockResultSetMapping
            implements Function<java.sql.ResultSet, ConversionBlock> {
        @Override
        public ConversionBlock apply(java.sql.ResultSet resultSet) {
            if (resultSet != null) {
                try {
                    final ConversionBlock conversionBlock = new ConversionBlock();
                    conversionBlock.setKey(new ConversionBlock.Key(
                            resultSet.getInt("JOBID"),
                            resultSet.getInt("CHUNKID")));
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
            return "ExistingFile{" +
                    "id='" + id + '\'' +
                    '}';
        }
    }
}
