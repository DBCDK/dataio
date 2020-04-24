/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GNU GPLv3
 * See license text in LICENSE.txt
 */

package dk.dbc.dataio.sink.periodicjobs;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import dk.dbc.commons.jpa.ResultSet;
import dk.dbc.dataio.commons.conversion.ConversionMetadata;
import dk.dbc.dataio.commons.types.Chunk;
import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.utils.jobstore.ejb.JobStoreServiceConnectorBean;
import dk.dbc.dataio.filestore.service.connector.FileStoreServiceConnector;
import dk.dbc.dataio.filestore.service.connector.FileStoreServiceConnectorException;
import dk.dbc.dataio.filestore.service.connector.ejb.FileStoreServiceConnectorBean;
import dk.dbc.dataio.harvester.types.HttpPickup;
import dk.dbc.dataio.sink.types.SinkException;
import dk.dbc.util.Timed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

@Stateless
public class PeriodicJobsHttpFinalizerBean extends PeriodicJobsPickupFinalizer {
    private static final Logger LOGGER = LoggerFactory.getLogger(PeriodicJobsHttpFinalizerBean.class);

    public static final String ORIGIN = "dataio/sink/periodic-jobs";

    @PersistenceContext(unitName = "periodic-jobs_PU")
    EntityManager entityManager;

    @EJB public FileStoreServiceConnectorBean fileStoreServiceConnectorBean;
    @EJB public JobStoreServiceConnectorBean jobStoreServiceConnectorBean;

    @Timed
    @Override
    public Chunk deliver(Chunk chunk, PeriodicJobsDelivery delivery) throws SinkException {
        final boolean isEmptyJob = isEmptyJob(chunk, jobStoreServiceConnectorBean.getConnector());
        final FileStoreServiceConnector fileStoreServiceConnector = fileStoreServiceConnectorBean.getConnector();
        final HttpPickup httpPickup = (HttpPickup) delivery.getConfig().getContent().getPickup();
        final ConversionMetadata fileMetadata = new ConversionMetadata(ORIGIN)
                .withJobId(delivery.getJobId())
                .withAgencyId(Integer.valueOf(httpPickup.getReceivingAgency()))
                .withFilename(getFilename(delivery));

        if (isEmptyJob) {
            fileMetadata.withFilename(fileMetadata.getFilename() + ".EMPTY");
        }

        String fileId;
        final Optional<ExistingFile> existingFile =
                fileAlreadyExists(fileStoreServiceConnector, delivery.getJobId(), fileMetadata);
        if (existingFile.isPresent()) {
            fileId = existingFile.get().getId();
        } else {
            if (isEmptyJob) {
                fileId = uploadEmptyFile(fileStoreServiceConnector, delivery);
            } else {
                fileId = uploadDatablocks(fileStoreServiceConnector, delivery);
            }
            if (fileId != null) {
                uploadMetadata(fileStoreServiceConnector, fileId, fileMetadata, delivery);
            }
        }
        return newResultChunk(fileStoreServiceConnector, chunk, fileId, fileMetadata);
    }

    private Optional<ExistingFile> fileAlreadyExists(FileStoreServiceConnector fileStoreServiceConnector,
                                                     Integer jobId, ConversionMetadata metadata) throws SinkException {
        // A file may already exist if something forced a rollback after
        // the PeriodicJobsHttpFinalizerBean.deliver() method returned.
        // If so we must use this existing file since it has already been
        // exposed to the end users.
        try {
            final List<ExistingFile> files = fileStoreServiceConnector.searchByMetadata(metadata, ExistingFile.class);
            if (files.isEmpty()) {
                return Optional.empty();
            }
            return Optional.of(files.get(0));
        } catch (FileStoreServiceConnectorException | RuntimeException e) {
            throw new SinkException(String.format("Failed check for existing file for periodic job %d", jobId), e);
        }
    }

    private String uploadEmptyFile(FileStoreServiceConnector fileStoreServiceConnector, PeriodicJobsDelivery delivery)
            throws SinkException {
        String fileId = null;
        try {
            fileId = fileStoreServiceConnector.addFile(new ByteArrayInputStream(new byte[0]));
            LOGGER.info("Uploaded empty file {} for periodic job {}", fileId, delivery.getJobId());
            return fileId;
        } catch (RuntimeException | FileStoreServiceConnectorException e) {
            deleteFile(fileStoreServiceConnector, fileId);
            throw new SinkException(e);
        }
    }

    private String uploadDatablocks(FileStoreServiceConnector fileStoreServiceConnector, PeriodicJobsDelivery delivery)
            throws SinkException {
        final Query getDataBlocksQuery = entityManager
                .createNamedQuery(PeriodicJobsDataBlock.GET_DATA_BLOCKS_QUERY_NAME)
                .setParameter(1, delivery.getJobId());

        String fileId = null;
        try (final ResultSet<PeriodicJobsDataBlock> datablocks = new ResultSet<>(entityManager, getDataBlocksQuery,
                new PeriodicJobsDataBlockResultSetMapping())) {
            for (PeriodicJobsDataBlock datablock : datablocks) {
                if (fileId == null) {
                    fileId = fileStoreServiceConnector.addFile(new ByteArrayInputStream(datablock.getBytes()));
                } else {
                    fileStoreServiceConnector.appendToFile(fileId, datablock.getBytes());
                }
            }
            LOGGER.info("Uploaded file {} for periodic job {}", fileId, delivery.getJobId());
            return fileId;
        } catch (RuntimeException | FileStoreServiceConnectorException e) {
            deleteFile(fileStoreServiceConnector, fileId);
            throw new SinkException(e);
        }
    }

    private void deleteFile(FileStoreServiceConnector fileStoreServiceConnector, String fileId) {
        try {
            LOGGER.info("Removing file with id {} from file-store", fileId);
            fileStoreServiceConnector.deleteFile(fileId);
        } catch (RuntimeException | FileStoreServiceConnectorException  e) {
            LOGGER.error("Failed to remove uploaded file with id {}", fileId, e);
        }
    }

    private void uploadMetadata(FileStoreServiceConnector fileStoreServiceConnector, String fileId,
                                ConversionMetadata fileMetadata, PeriodicJobsDelivery delivery)
            throws SinkException {
        try {
            fileStoreServiceConnector.addMetadata(fileId, fileMetadata);
            LOGGER.info("Uploaded file metadata {} for periodic job {}", fileMetadata, delivery.getJobId());
        } catch (RuntimeException | FileStoreServiceConnectorException e) {
            deleteFile(fileStoreServiceConnector, fileId);
            throw new SinkException(e);
        }
    }

    private Chunk newResultChunk(FileStoreServiceConnector fileStoreServiceConnector, Chunk chunk,
                                 String fileId, ConversionMetadata fileMetadata) {
        final Chunk result = new Chunk(chunk.getJobId(), chunk.getChunkId(), Chunk.Type.DELIVERED);
        final ChunkItem chunkItem = ChunkItem.successfulChunkItem()
                .withId(0)
                .withType(ChunkItem.Type.STRING)
                .withEncoding(StandardCharsets.UTF_8);

        if (fileId != null) {
            final String fileUrl = String.join("/", fileStoreServiceConnector.getBaseUrl(), "files", fileId);
            chunkItem.withData(String.format("%s exposed as %s", fileUrl, fileMetadata.getFilename()));
        } else {
            chunkItem.withData("No file uploaded");
        }
        result.insertItem(chunkItem);
        return result;
    }

    private String getFilename(PeriodicJobsDelivery delivery) {
        return delivery.getConfig().getContent()
                .getName()
                .toLowerCase()
                .replaceAll("[^\\p{ASCII}]", "")
                .replaceAll("\\s+","_") + "." + delivery.getJobId();
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
