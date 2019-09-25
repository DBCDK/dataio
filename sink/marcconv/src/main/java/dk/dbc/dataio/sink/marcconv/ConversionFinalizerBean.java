/*
 * DataIO - Data IO
 *
 * Copyright (C) 2018 Dansk Bibliotekscenter a/s, Tempovej 7-11, DK-2750 Ballerup,
 * Denmark. CVR: 15149043
 *
 * This file is part of DataIO.
 *
 * DataIO is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * DataIO is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with DataIO.  If not, see <http://www.gnu.org/licenses/>.
 */

package dk.dbc.dataio.sink.marcconv;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import dk.dbc.commons.jpa.ResultSet;
import dk.dbc.dataio.commons.conversion.ConversionMetadata;
import dk.dbc.dataio.commons.conversion.ConversionParam;
import dk.dbc.dataio.commons.types.Chunk;
import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.types.HarvesterToken;
import dk.dbc.dataio.commons.types.JobSpecification;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnectorException;
import dk.dbc.dataio.commons.utils.jobstore.ejb.JobStoreServiceConnectorBean;
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
    public enum Origin {
        MARCCONV("dataio/sink/marcconv"),
        PERIODIC_JOBS("dataio/sink/marcconv/periodicjobs");

        private final String variantName;

        Origin(String variantName) {
            this.variantName = variantName;
        }

        @Override
        public String toString() {
            return variantName;
        }

        public static Origin of(String variantName) {
            switch (variantName) {
                case "dataio/sink/marcconv/periodicjobs": return PERIODIC_JOBS;
                case "dataio/sink/marcconv": return MARCCONV;
                default: throw new IllegalArgumentException("Unknown variant " + variantName);
            }
        }
    }


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
        final String origin = getOrigin(jobInfoSnapshot);
        final ConversionMetadata conversionMetadata = new ConversionMetadata(origin)
                .withJobId(jobInfoSnapshot.getJobId())
                .withAgencyId(agencyId)
                .withFilename(getConversionFilename(jobInfoSnapshot));

        final Optional<ExistingFile> existingFile = fileAlreadyExists(jobId, conversionMetadata);
        String fileId;
        if (existingFile.isPresent()) {
            fileId = existingFile.get().getId();
        } else {
            fileId = uploadFile(chunk);
            uploadMetadata(chunk, fileId, conversionMetadata);
        }
        LOGGER.info("Deleted {} conversion blocks for job {}",
                deleteConversionBlocks(jobId), jobId);
        LOGGER.info("Deleted {} conversion params for job {}",
                deleteConversionParam(jobId), jobId);

        return newResultChunk(chunk, fileId);
    }

    private String getOrigin(JobInfoSnapshot jobInfoSnapshot){
        final JobSpecification.Ancestry ancestry = jobInfoSnapshot.getSpecification().getAncestry();
        Origin origin = Origin.MARCCONV;
        if (ancestry != null) {
            final String harvesterToken = ancestry.getHarvesterToken();
            if (harvesterToken != null){
                switch (HarvesterToken.of(harvesterToken).getHarvesterVariant().name()){
                    case "periodic-jobs":
                        origin = Origin.PERIODIC_JOBS; break;
                    default: origin = Origin.MARCCONV;
                }
            }
        }
        return origin.variantName;
    }

    private Optional<ExistingFile> fileAlreadyExists(Integer jobId, ConversionMetadata metadata) throws SinkException {
        // A file may already exist if something exploded after the call to the
        // ConversionFinalizerBean.handleTerminationChunk() method. If so we must
        // use this existing file since it has already been exposed to the end
        // users.
        try {
            final List<ExistingFile> files = fileStoreServiceConnectorBean.getConnector()
                    .searchByMetadata(metadata, ExistingFile.class);
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

    private String uploadFile(Chunk chunk) throws SinkException {
        final Integer jobId = Math.toIntExact(chunk.getJobId());
        final Query getConversionBlocksQuery = entityManager
                .createNamedQuery(ConversionBlock.GET_CONVERSION_BLOCKS_QUERY_NAME)
                .setParameter(1, jobId);

        String fileId = null;
        try (final ResultSet<ConversionBlock> blocks = new ResultSet<>(
                entityManager, getConversionBlocksQuery,
                new ConversionBlockResultSetMapping())) {
            for (ConversionBlock block : blocks) {
                if (block == null || block.getBytes().length == 0) {
                    continue;
                }
                if (fileId == null) {
                    fileId = fileStoreServiceConnectorBean.getConnector()
                            .addFile(new ByteArrayInputStream(block.getBytes()));
                } else {
                    fileStoreServiceConnectorBean.getConnector()
                            .appendToFile(fileId, block.getBytes());
                }
            }
            LOGGER.info("Uploaded conversion file {} for job {}", fileId, jobId);
        } catch (FileStoreServiceConnectorException
                | RuntimeException e) {
            deleteFile(fileId);
            throw new SinkException(e);
        }
        return fileId;
    }

    private void uploadMetadata(Chunk chunk, String fileId, ConversionMetadata conversionMetadata) throws SinkException {
        try {
            fileStoreServiceConnectorBean.getConnector().addMetadata(fileId, conversionMetadata);
            LOGGER.info("Uploaded conversion metadata {} for job {}",
                    conversionMetadata, chunk.getJobId());
        } catch (FileStoreServiceConnectorException
                | RuntimeException e) {
            deleteFile(fileId);
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

    private void deleteFile(String fileId) {
        try {
            LOGGER.info("Removing file with id {} from file-store", fileId);
            fileStoreServiceConnectorBean.getConnector().deleteFile(fileId);
        } catch (FileStoreServiceConnectorException | RuntimeException e) {
            LOGGER.error("Failed to remove uploaded file with id {}", fileId, e);
        }
    }

    private Chunk newResultChunk(Chunk chunk, String fileId) {
        final Chunk result = new Chunk(chunk.getJobId(), chunk.getChunkId(), Chunk.Type.DELIVERED);
        result.insertItem(ChunkItem.successfulChunkItem()
                .withId(0)
                .withType(ChunkItem.Type.STRING)
                .withEncoding(StandardCharsets.UTF_8)
                .withData(String.join("/",
                        fileStoreServiceConnectorBean.getConnector().getBaseUrl(),
                        "files", fileId)));
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
