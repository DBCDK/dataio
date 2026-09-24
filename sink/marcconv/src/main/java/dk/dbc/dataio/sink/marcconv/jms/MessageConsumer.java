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
import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.types.ConsumedMessage;
import dk.dbc.dataio.commons.types.Diagnostic;
import dk.dbc.dataio.commons.types.jms.JMSHeader;
import dk.dbc.dataio.filestore.service.connector.FileStoreServiceConnector;
import dk.dbc.dataio.jobstore.types.ItemDeliveryResult;
import dk.dbc.dataio.jse.artemis.common.jms.SinkMessageConsumerAdapter;
import dk.dbc.dataio.jse.artemis.common.service.ServiceHub;
import dk.dbc.dataio.sink.marcconv.SinkConfig;
import dk.dbc.dataio.sink.marcconv.entity.ConversionBlock;
import dk.dbc.dataio.sink.marcconv.entity.ConversionFinalizer;
import dk.dbc.dataio.sink.marcconv.entity.StoredConversionParam;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

public class MessageConsumer extends SinkMessageConsumerAdapter {
    private static final Logger LOGGER = LoggerFactory.getLogger(MessageConsumer.class);
    private static final String QUEUE = SinkConfig.QUEUE.fqnAsQueue();
    private static final String ADDRESS = SinkConfig.QUEUE.fqnAsAddress();
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private final ConversionFactory conversionFactory = new ConversionFactory();
    private final Cache<Integer, Conversion> conversionCache = CacheBuilder.newBuilder().expireAfterAccess(Duration.ofMinutes(30)).maximumSize(10).build();
    private final EntityManagerFactory entityManagerFactory;
    private final ConversionFinalizer conversionFinalizer;

    public MessageConsumer(ServiceHub serviceHub, FileStoreServiceConnector fileStore, EntityManagerFactory entityManagerFactory) {
        super(serviceHub);
        this.entityManagerFactory = entityManagerFactory;
        conversionFinalizer = new ConversionFinalizer(serviceHub, fileStore);
    }

    /**
     * An item here is converted and persisted as a conversion block rather than sent to a
     * target system, and nothing leaves this sink until the job's termination item uploads
     * the job's blocks to file-store as one file, so there is no "a newer version of this
     * record was already delivered" question to ask about a single item
     * <p>
     * See docs/chunk-scheduling-redesign.md, Watermark opt-out.
     */
    @Override
    protected boolean usesDeliveryWatermark() {
        return false;
    }

    /**
     * Converts one item into a conversion block, or, for the job's termination item,
     * uploads the blocks accumulated by every preceding item to file-store
     * <p>
     * The transaction is committed before this method returns, and only then does
     * {@link SinkMessageConsumerAdapter} report the result. That order is what lets the
     * job-end finalization run against complete data: a reported item is an item whose
     * block is durable, and the termination item is released only once every data item of
     * the job has reported.
     */
    @Override
    protected ItemDeliveryResult deliverItem(ConsumedMessage message, ChunkItem item) {
        // Not null-checked: SinkMessageConsumerAdapter has already rejected the message as
        // invalid if any of the three is missing.
        int jobId = JMSHeader.jobId.getHeader(message, Integer.class);
        int chunkId = JMSHeader.chunkId.getHeader(message, Long.class).intValue();
        short itemId = JMSHeader.itemId.getHeader(message, Short.class);

        EntityManager entityManager = entityManagerFactory.createEntityManager();
        EntityTransaction transaction = entityManager.getTransaction();
        try {
            transaction.begin();
            ChunkItem outcome = isTerminationItem(item)
                    ? conversionFinalizer.finalizeJob(jobId, item, entityManager)
                    : convertItem(item, jobId, chunkId, itemId, entityManager);
            transaction.commit();
            return ItemDeliveryResult.of(verdictOf(outcome), outcome);
        } finally {
            if (transaction.isActive()) {
                transaction.rollback();
            }
            entityManager.close();
        }
    }

    @Override
    public void abortJob(int jobId) {
        EntityManager entityManager = entityManagerFactory.createEntityManager();
        EntityTransaction transaction = entityManager.getTransaction();
        try {
            transaction.begin();
            conversionFinalizer.deleteJob(jobId, entityManager);
            transaction.commit();
            LOGGER.info("Aborted job {}", jobId);
        } finally {
            if (transaction.isActive()) {
                transaction.rollback();
            }
            entityManager.close();
        }
    }

    @Override
    public String getQueue() {
        return QUEUE;
    }

    @Override
    public String getAddress() {
        return ADDRESS;
    }

    /**
     * Converts one processed item, persisting the conversion output as the block the job's
     * termination item later uploads
     *
     * @return delivering outcome for the item, which {@link #verdictOf(ChunkItem)} turns
     * into the verdict reported for it
     */
    ChunkItem convertItem(ChunkItem item, int jobId, int chunkId, short itemId, EntityManager entityManager) {
        ChunkItem outcome = convert(item, jobId, chunkId, itemId, entityManager);
        if (outcome.getStatus() == ChunkItem.Status.SUCCESS) {
            // Kept outside the conversion's own error handling, since a failure to store
            // the job's conversion parameters is not this item's outcome. It rolls the
            // item back to be redelivered rather than reporting a converted item failed.
            storeConversionParam(jobId, entityManager);
        }
        return outcome;
    }

    private ChunkItem convert(ChunkItem item, int jobId, int chunkId, short itemId, EntityManager entityManager) {
        ChunkItem outcome = new ChunkItem()
                .withId(itemId)
                .withTrackingId(item.getTrackingId())
                .withType(ChunkItem.Type.STRING)
                .withEncoding(StandardCharsets.UTF_8);
        try {
            return switch (item.getStatus()) {
                case FAILURE -> outcome
                        .withStatus(ChunkItem.Status.IGNORE)
                        .withData("Failed by processor");
                case IGNORE -> outcome
                        .withStatus(ChunkItem.Status.IGNORE)
                        .withData("Ignored by processor");
                case SUCCESS -> {
                    storeConversion(jobId, chunkId, itemId, convertChunkItem(jobId, item), entityManager);
                    yield outcome
                            .withStatus(ChunkItem.Status.SUCCESS)
                            .withData("Converted");
                }
            };
        } catch (RuntimeException e) {
            return outcome
                    .withStatus(ChunkItem.Status.FAILURE)
                    .withDiagnostics(new Diagnostic(Diagnostic.Level.FATAL, e.getMessage(), e))
                    .withData(e.getMessage());
        }
    }

    /**
     * Recognizes the job termination item the same way job-store does on its own side of
     * the protocol ({@code PgJobStore.isTerminationItem})
     */
    private static boolean isTerminationItem(ChunkItem item) {
        return item.isTyped() && item.getType().getFirst() == ChunkItem.Type.JOB_END;
    }

    /**
     * Maps a delivering outcome onto the verdict job-store counts the item by
     * <p>
     * The mapping belongs here rather than in job-store, which reads the verdict alone:
     * this sink owns both the outcome item and the verdict and is free to derive one from
     * the other.
     */
    private static ItemDeliveryResult.Status verdictOf(ChunkItem outcome) {
        return switch (outcome.getStatus()) {
            case SUCCESS -> ItemDeliveryResult.Status.DELIVERED;
            case IGNORE -> ItemDeliveryResult.Status.IGNORED;
            case FAILURE -> ItemDeliveryResult.Status.FAILED;
        };
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

    private void storeConversion(int jobId, int chunkId, short itemId, byte[] conversionBytes, EntityManager entityManager) {
        if (conversionBytes.length != 0) {
            ConversionBlock.Key key = new ConversionBlock.Key(jobId, chunkId, itemId);
            ConversionBlock conversionBlock = entityManager.find(ConversionBlock.class, key);
            if (conversionBlock == null) {
                conversionBlock = new ConversionBlock();
                conversionBlock.setKey(key);
                conversionBlock.setBytes(conversionBytes);
                entityManager.persist(conversionBlock);
            } else {
                // This happens when an item is redelivered, either because the message was
                // redelivered before its session committed or because the same item was
                // dispatched twice. The conversion is deterministic, so the row is simply
                // written again.
                conversionBlock.setBytes(conversionBytes);
            }
        }
    }

    /**
     * Stores the conversion parameters of the job, once, for the finalization to read the
     * submitter off
     * <p>
     * Attempted for every converted item rather than once per job: the parameters come
     * from the first record this instance converted for the job, and there is no point in
     * the per-item protocol at which an instance knows it is holding that first record.
     * Which of the racing writers stores its parameters is therefore not decided here, and
     * never was. The finalization reads the submitter alone off them and falls back to the
     * job specification.
     */
    private void storeConversionParam(int jobId, EntityManager entityManager) {
        Conversion cachedConversion = conversionCache.getIfPresent(jobId);
        if (cachedConversion == null || cachedConversion.getParam() == null) {
            return;
        }
        StoredConversionParam.insertIfAbsent(entityManager, jobId, cachedConversion.getParam());
    }
}
