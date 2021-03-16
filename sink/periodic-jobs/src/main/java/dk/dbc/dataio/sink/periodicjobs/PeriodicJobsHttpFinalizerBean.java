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
import dk.dbc.dataio.commons.macroexpansion.MacroSubstitutor;
import dk.dbc.dataio.commons.types.Chunk;
import dk.dbc.dataio.commons.types.ChunkItem;
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
import javax.persistence.Query;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

@Stateless
public class PeriodicJobsHttpFinalizerBean extends PeriodicJobsPickupFinalizer {
    private static final Logger LOGGER = LoggerFactory.getLogger(PeriodicJobsHttpFinalizerBean.class);

    public static final String ORIGIN = "dataio/sink/periodic-jobs";

    @EJB public FileStoreServiceConnectorBean fileStoreServiceConnectorBean;

    @Timed
    @Override
    public Chunk deliver(Chunk chunk, PeriodicJobsDelivery delivery) throws SinkException {
        final boolean isEmptyJob = isEmptyJob(chunk);
        final FileStoreServiceConnector fileStoreServiceConnector = fileStoreServiceConnectorBean.getConnector();
        final HttpPickup httpPickup = (HttpPickup) delivery.getConfig().getContent().getPickup();
        final ConversionMetadata fileMetadata = new ConversionMetadata(ORIGIN)
                .withJobId(delivery.getJobId())
                .withAgencyId(Integer.valueOf(httpPickup.getReceivingAgency()))
                .withFilename(getRemoteFilename(delivery));

        if (isEmptyJob) {
            // prefixing empty jobs with "no_content." ensures that printing
            // of empty autoprint jobs will not be attempted.
            fileMetadata.withFilename("no_content." + fileMetadata.getFilename());
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
        final MacroSubstitutor macroSubstitutor = getMacroSubstitutor(delivery);
        String contentHeader = delivery.getConfig().getContent().getPickup().getContentHeader();
        String contentFooter = delivery.getConfig().getContent().getPickup().getContentFooter();

        if (contentHeader != null) {
            contentHeader = macroSubstitutor.replace(contentHeader);
        } else {
            contentHeader = "";
        }

        if (contentFooter != null) {
            contentFooter = macroSubstitutor.replace(contentFooter);
        } else {
            contentFooter = "";
        }

        final GroupHeaderIncludePredicate groupHeaderIncludePredicate = new GroupHeaderIncludePredicate();
        final Query getDataBlocksQuery = entityManager
                .createNamedQuery(PeriodicJobsDataBlock.GET_DATA_BLOCKS_QUERY_NAME)
                .setParameter(1, delivery.getJobId());

        String fileId = null;
        try (final ResultSet<PeriodicJobsDataBlock> datablocks = new ResultSet<>(entityManager, getDataBlocksQuery,
                new PeriodicJobsDataBlockResultSetMapping())) {
            fileId = fileStoreServiceConnector.addFile(new ByteArrayInputStream(contentHeader.getBytes()));
            for (PeriodicJobsDataBlock datablock : datablocks) {
                byte[] payload;
                if (groupHeaderIncludePredicate.test(datablock)) {
                    payload = new byte[datablock.getGroupHeader().length + datablock.getBytes().length];
                    System.arraycopy(datablock.getGroupHeader(), 0, payload, 0, datablock.getGroupHeader().length);
                    System.arraycopy(datablock.getBytes(), 0, payload, datablock.getGroupHeader().length, datablock.getBytes().length);
                } else {
                    payload = datablock.getBytes();
                }
                fileStoreServiceConnector.appendToFile(fileId, payload);
            }
            fileStoreServiceConnector.appendToFile(fileId, contentFooter.getBytes());
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
                .withType(ChunkItem.Type.JOB_END)
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
