/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GNU GPLv3
 * See license text in LICENSE.txt
 */

package dk.dbc.dataio.sink.periodicjobs;

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
import java.io.IOException;
import java.nio.charset.StandardCharsets;

@MessageDriven
public class MessageConsumerBean extends AbstractSinkMessageConsumerBean {
    private static final Logger LOGGER = LoggerFactory.getLogger(MessageConsumerBean.class);

    private final ConversionFactory conversionFactory = new ConversionFactory();
    private final JSONBContext jsonbContext = new JSONBContext();

    @PersistenceContext(unitName = "periodic-jobs_PU")
    EntityManager entityManager;

    @EJB PeriodicJobsConfigurationBean periodicJobsConfigurationBean;
    @EJB PeriodicJobsFinalizerBean periodicJobsFinalizerBean;

    @Override
    public void handleConsumedMessage(ConsumedMessage consumedMessage)
            throws InvalidMessageException, NullPointerException, SinkException {
        final Chunk chunk = unmarshallPayload(consumedMessage);
        LOGGER.info("Received chunk {}/{}", chunk.getJobId(), chunk.getChunkId());

        final Chunk result;
        if (chunk.isJobEnd()) {
            try {
                // Give the before-last message enough time to commit
                // its datablocks to the database before initiating
                // the finalization process.
                // (The result is uploaded to the job-store before the
                // implicit commit, so without the sleep pause, there was a
                // small risk that the end-chunk would reach this bean
                // before all data was available.)
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                throw new SinkException(e);
            }
            result = periodicJobsFinalizerBean.handleTerminationChunk(chunk);
        } else {
            final PeriodicJobsDelivery delivery = periodicJobsConfigurationBean.getDelivery(chunk);
            result = handleChunk(chunk, delivery);
        }
        uploadChunk(result);
    }

    Chunk handleChunk(Chunk chunk, PeriodicJobsDelivery delivery) {
        final Chunk result = new Chunk(chunk.getJobId(), chunk.getChunkId(), Chunk.Type.DELIVERED);
        try {
            for (ChunkItem chunkItem : chunk.getItems()) {
                DBCTrackedLogContext.setTrackingId(chunkItem.getTrackingId());
                final PeriodicJobsDataBlock.Key key = new PeriodicJobsDataBlock.Key((int) chunk.getJobId(),
                        getRecordNumber((int) chunk.getChunkId(), (int) chunkItem.getId()));
                result.insertItem(handleChunkItem(chunkItem, key, delivery));
            }
        } finally {
            DBCTrackedLogContext.remove();
        }
        return result;
    }

    private ChunkItem handleChunkItem(ChunkItem chunkItem, PeriodicJobsDataBlock.Key key, PeriodicJobsDelivery delivery) {
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
                    convertChunkItem(chunkItem, key, delivery);
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

    private void convertChunkItem(ChunkItem chunkItem, PeriodicJobsDataBlock.Key key, PeriodicJobsDelivery delivery) {
        final AddiReader addiReader = new AddiReader(new ByteArrayInputStream(chunkItem.getData()));
        try {
            while (addiReader.hasNext()) {
                // Extract parameters from ADDI metadata
                final AddiRecord addiRecord = addiReader.next();
                final ConversionParam conversionParam = jsonbContext.unmarshall(
                        StringUtil.asString(addiRecord.getMetaData()), ConversionParam.class);

                // Convert the ADDI content data
                // TODO: 16/01/2020 Currently the ConversionFactory only handles ISO2709 conversion - more conversions may be needed.
                final Conversion conversion = conversionFactory.newConversion(conversionParam);
                // TODO: 15/01/2020 In a future version specialized conversion steps are needed based on pickupType, e.g. for email formatting.
                final byte[] convertedData = conversion.apply(addiRecord.getContentData());

                if (convertedData == null || convertedData.length == 0) {
                    // TODO: 17/01/2020 Getting jobId from delivery instead of key is a hack to avoid unused formal parameter warnings - delivery will be used when email delivery is implemented.
                    LOGGER.warn("Conversion for job {} item {} produced empty result",
                            delivery.getJobId(), key.getRecordNumber());
                    throw new ConversionException("Conversion produced empty result");
                }

                // Persists result of conversion as datablock
                final PeriodicJobsDataBlock datablock = new PeriodicJobsDataBlock();
                datablock.setKey(key);
                // TODO: 15/01/2020 In a future version custom sort keys can be passed via ConversionParam
                datablock.setSortkey(getDefaultSortKey(key.getRecordNumber()));
                datablock.setBytes(convertedData);

                storeDataBlock(datablock);
            }
        } catch (IOException | JSONBException e) {
            throw new ConversionException(e);
        }
    }

    private int getRecordNumber(int chunkId, int itemId) {
        return 10*chunkId + itemId;
    }

    private String getDefaultSortKey(int recordNumber) {
        // Record number as zero padded string of length 9
        return String.format("%09d", recordNumber);
    }

    private void storeDataBlock(PeriodicJobsDataBlock datablock) {
        PeriodicJobsDataBlock existingDatablock =
                entityManager.find(PeriodicJobsDataBlock.class, datablock.getKey());
        if (existingDatablock == null) {
            entityManager.persist(datablock);
        } else {
            // This should only happen if something by
            // accident caused multiple messages referencing
            // the same chunk to be enqueued.
            entityManager.merge(datablock);
        }
    }
}
