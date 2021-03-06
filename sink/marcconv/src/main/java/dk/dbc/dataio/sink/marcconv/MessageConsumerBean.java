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

import dk.dbc.commons.addi.AddiReader;
import dk.dbc.commons.addi.AddiRecord;
import dk.dbc.dataio.commons.conversion.Conversion;
import dk.dbc.dataio.commons.conversion.ConversionException;
import dk.dbc.dataio.commons.conversion.ConversionFactory;
import dk.dbc.dataio.commons.conversion.ConversionParam;
import dk.dbc.dataio.commons.types.Chunk;
import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.types.ConsumedMessage;
import dk.dbc.dataio.commons.types.Diagnostic;
import dk.dbc.dataio.commons.types.exceptions.InvalidMessageException;
import dk.dbc.dataio.commons.utils.cache.Cache;
import dk.dbc.dataio.commons.utils.cache.CacheManager;
import dk.dbc.dataio.commons.utils.lang.StringUtil;
import dk.dbc.dataio.sink.types.AbstractSinkMessageConsumerBean;
import dk.dbc.dataio.sink.types.SinkException;
import dk.dbc.jsonb.JSONBContext;
import dk.dbc.jsonb.JSONBException;
import dk.dbc.log.DBCTrackedLogContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.EJB;
import javax.ejb.MessageDriven;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

@MessageDriven
public class MessageConsumerBean extends AbstractSinkMessageConsumerBean {
    private static final Logger LOGGER = LoggerFactory.getLogger(MessageConsumerBean.class);

    @PersistenceContext(unitName = "marcconv_PU")
    EntityManager entityManager;

    @EJB ConversionFinalizerBean conversionFinalizerBean;

    private final JSONBContext jsonbContext = new JSONBContext();
    private final ConversionFactory conversionFactory = new ConversionFactory();
    private final Cache<Integer, Conversion> conversionCache = CacheManager.createLRUCache(10);

    @Override
    public void handleConsumedMessage(ConsumedMessage consumedMessage)
            throws InvalidMessageException, NullPointerException, SinkException {
        final Chunk chunk = unmarshallPayload(consumedMessage);
        LOGGER.info("Received chunk {}/{}", chunk.getJobId(), chunk.getChunkId());

        final Chunk result;
        if (chunk.isTerminationChunk()) {
            try {
                // Give the before-last message enough time to commit
                // its blocks to the database before initiating
                // the finalization process.
                // (The result is uploaded to the job-store before the
                // implicit commit, so without the sleep pause, there was a
                // small risk that the end-chunk would reach this bean
                // before all data was available.)
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                throw new SinkException(e);
            }
            result = conversionFinalizerBean.handleTerminationChunk(chunk);
        } else {
            result = handleChunk(chunk);
        }
        uploadChunk(result);
    }

    Chunk handleChunk(Chunk chunk) {
        final Integer jobId = Math.toIntExact(chunk.getJobId());
        final Integer chunkId = Math.toIntExact(chunk.getChunkId());
        final Chunk result = new Chunk(jobId, chunkId, Chunk.Type.DELIVERED);
        final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        try {
            for (ChunkItem chunkItem : chunk.getItems()) {
                DBCTrackedLogContext.setTrackingId(chunkItem.getTrackingId());
                result.insertItem(handleChunkItem(jobId, chunkItem, buffer));
            }
            storeConversion(jobId, chunkId, buffer.toByteArray());
            final Conversion cachedConversion = conversionCache.get(jobId);
            if (cachedConversion != null) {
                final ConversionParam param = cachedConversion.getParam();
                if (param != null) {
                    storeConversionParam(jobId, param);
                }
            }
        } finally {
            DBCTrackedLogContext.remove();
        }
        return result;
    }

    private ChunkItem handleChunkItem(Integer jobId, ChunkItem chunkItem, ByteArrayOutputStream buffer) {
        final ChunkItem result = new ChunkItem()
                .withId(chunkItem.getId())
                .withTrackingId(chunkItem.getTrackingId())
                .withType(ChunkItem.Type.STRING)
                .withEncoding(StandardCharsets.UTF_8);
        try {
            switch (chunkItem.getStatus()) {
                case FAILURE:
                    return result
                            .withStatus(ChunkItem.Status.IGNORE)
                            .withData("Failed by processor");
                case IGNORE:
                    return result
                            .withStatus(ChunkItem.Status.IGNORE)
                            .withData("Ignored by processor");
                default:
                    appendToBuffer(buffer, convertChunkItem(jobId, chunkItem));
                    return result
                            .withStatus(ChunkItem.Status.SUCCESS)
                            .withData("Converted");
            }
        } catch (RuntimeException e) {
            return result
                    .withStatus(ChunkItem.Status.FAILURE)
                    .withDiagnostics(new Diagnostic(Diagnostic.Level.FATAL, e.getMessage(), e))
                    .withData(e.getMessage());
        }
    }

    private void appendToBuffer(ByteArrayOutputStream buffer, byte[] bytes) {
        try {
            if (bytes.length != 0) {
                buffer.write(bytes);
            }
        } catch (IOException e) {
            throw new ConversionException("Unable to write to output buffer", e);
        }
    }

    private byte[] convertChunkItem(Integer jobId, ChunkItem chunkItem) {
        final AddiReader addiReader = new AddiReader(new ByteArrayInputStream(chunkItem.getData()));
        try {
            final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            while (addiReader.hasNext()) {
                final AddiRecord addiRecord = addiReader.next();
                final ConversionParam conversionParam = jsonbContext.unmarshall(
                        StringUtil.asString(addiRecord.getMetaData()), ConversionParam.class);
                final Conversion conversion = getConversion(jobId, conversionParam);
                appendToBuffer(buffer, conversion.apply(addiRecord.getContentData()));
            }
            return buffer.toByteArray();
        } catch (IOException | JSONBException e) {
            throw new ConversionException(e);
        }
    }

    private Conversion getConversion(Integer jobId, ConversionParam conversionParam) {
        if (conversionCache.containsKey(jobId)) {
            return conversionCache.get(jobId);
        }
        final Conversion conversion = conversionFactory.newConversion(conversionParam);
        conversionCache.put(jobId, conversion);
        return conversion;
    }

    private void storeConversion(Integer jobId, Integer chunkId, byte[] conversionBytes) {
        if (conversionBytes.length != 0) {
            final ConversionBlock.Key key = new ConversionBlock.Key(jobId, chunkId);
            ConversionBlock conversionBlock = entityManager.find(ConversionBlock.class, key);
            if (conversionBlock == null) {
                conversionBlock = new ConversionBlock();
                conversionBlock.setKey(key);
                conversionBlock.setBytes(conversionBytes);
                entityManager.persist(conversionBlock);
            } else {
                // This should only happen if something by
                // accident caused multiple messages referencing
                // the same chunk to be enqueued.
                conversionBlock.setBytes(conversionBytes);
            }
        }
    }

    private void storeConversionParam(Integer jobId, ConversionParam param) {
        StoredConversionParam storedConversionParam =
                entityManager.find(StoredConversionParam.class, jobId);
        if (storedConversionParam == null) {
            storedConversionParam = new StoredConversionParam(jobId);
            storedConversionParam.setParam(param);
            entityManager.persist(storedConversionParam);
            entityManager.flush();
        }
    }
}
