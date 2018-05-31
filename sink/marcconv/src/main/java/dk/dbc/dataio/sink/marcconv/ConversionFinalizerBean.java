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

import dk.dbc.commons.jpa.ResultSet;
import dk.dbc.dataio.commons.types.Chunk;
import dk.dbc.dataio.commons.types.ChunkItem;
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
import java.util.function.Function;

/**
 * It is the responsibility of this class to expose the conversion result
 */
@Stateless
public class ConversionFinalizerBean {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConversionFinalizerBean.class);

    @PersistenceContext(unitName = "marcconv_PU")
    EntityManager entityManager;

    @EJB public FileStoreServiceConnectorBean fileStoreServiceConnectorBean;
    @EJB public JobStoreServiceConnectorBean jobStoreServiceConnectorBean;

    @Timed
    public Chunk handleTerminationChunk(Chunk chunk) throws SinkException {
        LOGGER.info("Finalizing conversion job {}", chunk.getJobId());
        final String fileId = uploadFile(chunk);
        uploadMetadata(chunk, fileId);
        return newResultChunk(chunk, fileId);
    }

    private String uploadFile(Chunk chunk) throws SinkException {
        final Query getConversionBlocksQuery = entityManager
                .createNamedQuery(ConversionBlock.GET_CONVERSION_BLOCKS_QUERY_NAME)
                .setParameter(1, (int) chunk.getJobId());

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
            LOGGER.info("Uploaded conversion file {} for job {}",
                    fileId, chunk.getJobId());
        } catch (FileStoreServiceConnectorException
                | RuntimeException e) {
            // TODO: 30-05-18 delete file
            throw new SinkException(e);
        }
        return fileId;
    }

    private void uploadMetadata(Chunk chunk, String fileId) throws SinkException {
        try {
            final JobListCriteria findJobCriteria = new JobListCriteria()
                    .where(new ListFilter<>(JobListCriteria.Field.JOB_ID,
                            ListFilter.Op.EQUAL, chunk.getJobId()));
            final List<JobInfoSnapshot> snapshots = jobStoreServiceConnectorBean
                    .getConnector().listJobs(findJobCriteria);
            if (snapshots.size() != 1) {
                throw new ConversionException(String.format(
                        "Unable to retrieve job %d - expected 1 hit, got %d",
                        chunk.getJobId(), snapshots.size()));
            }
            final JobInfoSnapshot jobInfoSnapshot = snapshots.get(0);
            final ConversionMetadata conversionMetadata = new ConversionMetadata()
                    .withJobId(jobInfoSnapshot.getJobId())
                    .withAgencyId((int) jobInfoSnapshot.getSpecification().getSubmitterId())
                    .withFilename(getConversionFilename(jobInfoSnapshot));
            fileStoreServiceConnectorBean.getConnector().addMetadata(fileId, conversionMetadata);
            LOGGER.info("Uploaded conversion metadata {} for job {}",
                    conversionMetadata, chunk.getJobId());
        } catch (FileStoreServiceConnectorException
                | JobStoreServiceConnectorException
                | RuntimeException e) {
            // TODO: 30-05-18 delete file
            throw new SinkException(e);
        }
    }

    private String getConversionFilename(JobInfoSnapshot jobInfoSnapshot) {
        final JobSpecification jobSpecification = jobInfoSnapshot.getSpecification();
        if (jobSpecification.getAncestry() == null
                || jobSpecification.getAncestry().getDatafile() == null) {
            return "marcconv." + jobInfoSnapshot.getJobId();
        }
        return jobSpecification.getAncestry().getDatafile();
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
}
