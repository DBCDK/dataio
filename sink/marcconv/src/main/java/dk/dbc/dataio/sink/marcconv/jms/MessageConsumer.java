package dk.dbc.dataio.sink.marcconv.jms;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
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
import dk.dbc.dataio.filestore.service.connector.FileStoreServiceConnector;
import dk.dbc.dataio.jse.artemis.common.jms.MessageConsumerAdapter;
import dk.dbc.dataio.jse.artemis.common.service.ServiceHub;
import dk.dbc.dataio.sink.marcconv.SinkConfig;
import dk.dbc.dataio.sink.marcconv.entity.ConversionBlock;
import dk.dbc.dataio.sink.marcconv.entity.ConversionFinalizer;
import dk.dbc.dataio.sink.marcconv.entity.StoredConversionParam;
import dk.dbc.log.DBCTrackedLogContext;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

public class MessageConsumer extends MessageConsumerAdapter {
    private static final String QUEUE = SinkConfig.QUEUE.fqnAsQueue();
    private static final String ADDRESS = SinkConfig.QUEUE.fqnAsAddress();
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private final ConversionFactory conversionFactory = new ConversionFactory();
    private final Cache<Integer, Conversion> conversionCache = CacheBuilder.newBuilder().expireAfterAccess(Duration.ofMinutes(30)).maximumSize(10).build();
    private final EntityManager entityManager;
    private final ConversionFinalizer conversionFinalizer;

    public MessageConsumer(ServiceHub serviceHub, FileStoreServiceConnector fileStore, EntityManager entityManager) {
        super(serviceHub);
        this.entityManager = entityManager;
        conversionFinalizer = new ConversionFinalizer(serviceHub, fileStore, entityManager);
    }

    @Override
    public void handleConsumedMessage(ConsumedMessage consumedMessage) throws InvalidMessageException {
        Chunk chunk = unmarshallPayload(consumedMessage);
        EntityTransaction transaction = entityManager.getTransaction();
        try {
            Chunk result;
            transaction.begin();
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
                    throw new RuntimeException(e);
                }
                result = conversionFinalizer.handleTerminationChunk(chunk);
            } else {
                result = handleChunk(chunk);
            }
            sendResultToJobStore(result);
            transaction.commit();
        } finally {
            if(transaction.isActive()) transaction.rollback();
        }
    }

    @Override
    public void abortJob(int jobId) {
        EntityTransaction transaction = entityManager.getTransaction();
        transaction.begin();
        conversionFinalizer.deleteJob(jobId);
        transaction.commit();
    }

    @Override
    public String getQueue() {
        return QUEUE;
    }

    @Override
    public String getAddress() {
        return ADDRESS;
    }

    Chunk handleChunk(Chunk chunk) {
        Integer jobId = Math.toIntExact(chunk.getJobId());
        long chunkId = chunk.getChunkId();
        Chunk result = new Chunk(jobId, chunkId, Chunk.Type.DELIVERED);
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        try {
            for (ChunkItem chunkItem : chunk.getItems()) {
                DBCTrackedLogContext.setTrackingId(chunkItem.getTrackingId());
                result.insertItem(handleChunkItem(jobId, chunkItem, buffer));
            }
            storeConversion(jobId, Math.toIntExact(chunkId), buffer.toByteArray());
            Conversion cachedConversion = conversionCache.getIfPresent(jobId);
            if (cachedConversion != null) {
                ConversionParam param = cachedConversion.getParam();
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
        ChunkItem result = new ChunkItem().withId(chunkItem.getId()).withTrackingId(chunkItem.getTrackingId()).withType(ChunkItem.Type.STRING).withEncoding(StandardCharsets.UTF_8);
        try {
            switch (chunkItem.getStatus()) {
                case FAILURE:
                    return result.withStatus(ChunkItem.Status.IGNORE).withData("Failed by processor");
                case IGNORE:
                    return result.withStatus(ChunkItem.Status.IGNORE).withData("Ignored by processor");
                default:
                    appendToBuffer(buffer, convertChunkItem(jobId, chunkItem));
                    return result.withStatus(ChunkItem.Status.SUCCESS).withData("Converted");
            }
        } catch (RuntimeException e) {
            return result.withStatus(ChunkItem.Status.FAILURE).withDiagnostics(new Diagnostic(Diagnostic.Level.FATAL, e.getMessage(), e)).withData(e.getMessage());
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
        AddiReader addiReader = new AddiReader(new ByteArrayInputStream(chunkItem.getData()));
        try {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            while (addiReader.hasNext()) {
                AddiRecord addiRecord = addiReader.next();
                ConversionParam conversionParam = MAPPER.readValue(new String(addiRecord.getMetaData(), StandardCharsets.UTF_8), ConversionParam.class);
                Conversion conversion = getConversion(jobId, conversionParam);
                appendToBuffer(buffer, conversion.apply(addiRecord.getContentData()));
            }
            return buffer.toByteArray();
        } catch (IOException e) {
            throw new ConversionException(e);
        }
    }

    private Conversion getConversion(Integer jobId, ConversionParam conversionParam) {
        return conversionCache.asMap().computeIfAbsent(jobId, id -> conversionFactory.newConversion(conversionParam));
    }

    private void storeConversion(Integer jobId, Integer chunkId, byte[] conversionBytes) {
        if (conversionBytes.length != 0) {
            ConversionBlock.Key key = new ConversionBlock.Key(jobId, chunkId);
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
        StoredConversionParam storedConversionParam = entityManager.find(StoredConversionParam.class, jobId);
        if (storedConversionParam == null) {
            storedConversionParam = new StoredConversionParam(jobId);
            storedConversionParam.setParam(param);
            entityManager.persist(storedConversionParam);
            entityManager.flush();
        }
    }
}
