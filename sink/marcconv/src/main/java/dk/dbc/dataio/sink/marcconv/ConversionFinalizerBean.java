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
import dk.dbc.dataio.filestore.service.connector.FileStoreServiceConnectorException;
import dk.dbc.dataio.filestore.service.connector.ejb.FileStoreServiceConnectorBean;
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

    public Chunk handleTerminationChunk(Chunk chunk) {
        LOGGER.info("Finalizing conversion job {}", chunk.getJobId());
        final String fileId = uploadFile(chunk);
        return newResultChunk(chunk, fileId);
    }

    private String uploadFile(Chunk chunk) {
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
        } catch (FileStoreServiceConnectorException e) {
            throw new ConversionException(e);
        }
        return fileId;
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
