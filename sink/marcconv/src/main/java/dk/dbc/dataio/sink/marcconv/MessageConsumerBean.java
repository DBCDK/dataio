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

import javax.ejb.ActivationConfigProperty;
import javax.ejb.EJB;
import javax.ejb.MessageDriven;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

@MessageDriven(name = "marcConvListener", activationConfig = {
        // Please see the following url for a explanation of the available settings.
        // The message selector variable is defined in the dataio-secrets project
        // https://activemq.apache.org/activation-spec-properties
        @ActivationConfigProperty(propertyName = "destination", propertyValue = "jms/dataio/sinks"),
        @ActivationConfigProperty(propertyName = "useJndi", propertyValue = "true"),
        @ActivationConfigProperty(propertyName = "destinationType", propertyValue = "javax.jms.Queue"),
        @ActivationConfigProperty(propertyName = "resourceAdapter", propertyValue = "artemis"),
        @ActivationConfigProperty(propertyName = "messageSelector", propertyValue = "resource = '${ENV=MESSAGE_NAME_FILTER}'"),
        @ActivationConfigProperty(propertyName = "initialRedeliveryDelay", propertyValue = "5000"),
        @ActivationConfigProperty(propertyName = "redeliveryBackOffMultiplier", propertyValue = "4"),
        @ActivationConfigProperty(propertyName = "maximumRedeliveries", propertyValue = "3"),
        @ActivationConfigProperty(propertyName = "redeliveryUseExponentialBackOff", propertyValue = "true")
})
public class MessageConsumerBean extends AbstractSinkMessageConsumerBean {
    private static final Logger LOGGER = LoggerFactory.getLogger(MessageConsumerBean.class);

    @PersistenceContext(unitName = "marcconv_PU")
    EntityManager entityManager;

    @EJB
    ConversionFinalizerBean conversionFinalizerBean;

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
