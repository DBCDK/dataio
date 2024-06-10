package dk.dbc.dataio.sink.periodicjobs;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dk.dbc.commons.addi.AddiReader;
import dk.dbc.commons.addi.AddiRecord;
import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnector;
import dk.dbc.dataio.commons.conversion.Conversion;
import dk.dbc.dataio.commons.conversion.ConversionException;
import dk.dbc.dataio.commons.conversion.ConversionFactory;
import dk.dbc.dataio.commons.types.Chunk;
import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.types.ConsumedMessage;
import dk.dbc.dataio.commons.types.Diagnostic;
import dk.dbc.dataio.commons.types.Tools;
import dk.dbc.dataio.commons.types.exceptions.InvalidMessageException;
import dk.dbc.dataio.commons.utils.lang.StringUtil;
import dk.dbc.dataio.filestore.service.connector.FileStoreServiceConnector;
import dk.dbc.dataio.jse.artemis.common.jms.MessageConsumerAdapter;
import dk.dbc.dataio.jse.artemis.common.service.ServiceHub;
import dk.dbc.dataio.sink.periodicjobs.mail.MailSession;
import dk.dbc.dataio.sink.periodicjobs.pickup.PeriodicJobsFtpFinalizerBean;
import dk.dbc.dataio.sink.periodicjobs.pickup.PeriodicJobsHttpFinalizerBean;
import dk.dbc.dataio.sink.periodicjobs.pickup.PeriodicJobsMailFinalizerBean;
import dk.dbc.dataio.sink.periodicjobs.pickup.PeriodicJobsSFtpFinalizerBean;
import dk.dbc.log.DBCTrackedLogContext;
import dk.dbc.proxy.ProxyBean;
import dk.dbc.weekresolver.connector.WeekResolverConnector;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;
import jakarta.ws.rs.client.ClientBuilder;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;

public class PeriodicJobsMessageConsumer extends MessageConsumerAdapter {
    private static final Logger LOGGER = LoggerFactory.getLogger(PeriodicJobsMessageConsumer.class);
    private static final String QUEUE = SinkConfig.QUEUE.fqnAsQueue();
    private static final String ADDRESS = SinkConfig.QUEUE.fqnAsAddress();
    private final ConversionFactory conversionFactory = new ConversionFactory();
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private final FlowStoreServiceConnector flowStoreServiceConnector;
    private final FileStoreServiceConnector fileStoreServiceConnector;

    EntityManagerFactory entityManagerFactory;

    PeriodicJobsFinalizerBean periodicJobsFinalizerBean;

    ProxyBean proxyBean;

    WeekResolverConnector weekResolverConnector;

    @SuppressWarnings("java:S2095")
    public PeriodicJobsMessageConsumer(ServiceHub serviceHub, EntityManagerFactory entityManagerFactory) {
        super(serviceHub);
        this.entityManagerFactory = entityManagerFactory;
        this.flowStoreServiceConnector = new FlowStoreServiceConnector(ClientBuilder.newClient().register(new JacksonFeature()), SinkConfig.FLOWSTORE_URL.asString());
        this.fileStoreServiceConnector = new FileStoreServiceConnector(ClientBuilder.newClient().register(new JacksonFeature()), SinkConfig.FILESTORE_URL.asString());
        this.weekResolverConnector = new WeekResolverConnector(ClientBuilder.newClient().register(new JacksonFeature()), SinkConfig.WEEKRESOLVER_SERVICE_URL.asString());

        this.proxyBean = SinkConfig.PROXY_HOSTNAME.asOptionalString()
                .map(s -> new ProxyBean(s)
                        .withProxyUsername(SinkConfig.PROXY_USERNAME.asString())
                        .withProxyPassword(SinkConfig.PROXY_PASSWORD.asString())
                        .withNonProxyHosts(Set.of(SinkConfig.NON_PROXY_HOSTS.asString().split(","))))
                .orElse(null);
        if (proxyBean != null) {
            proxyBean.init();
        }
        initializeFinalizers(serviceHub);
    }

    void initializeFinalizers(ServiceHub serviceHub) {
        periodicJobsFinalizerBean = new PeriodicJobsFinalizerBean()
                .withPeriodicJobsHttpFinalizerBean(new PeriodicJobsHttpFinalizerBean()
                        .withFileStoreServiceConnector(fileStoreServiceConnector))
                .withPeriodicJobsFtpFinalizerBean(new PeriodicJobsFtpFinalizerBean().withProxyBean(proxyBean))
                .withPeriodicJobsSFtpFinalizerBean(new PeriodicJobsSFtpFinalizerBean().withProxyBean(proxyBean))
                .withPeriodicJobsMailFinalizerBean(new PeriodicJobsMailFinalizerBean().withSession(MailSession.make()))
                .withPeriodicJobsConfigurationBean(new PeriodicJobsConfigurationBean()
                        .withFlowstoreConnector(flowStoreServiceConnector)
                        .withJobstoreConnector(jobStoreServiceConnector));

        List.of(periodicJobsFinalizerBean.periodicJobsHttpFinalizerBean,
                periodicJobsFinalizerBean.periodicJobsFtpFinalizerBean,
                periodicJobsFinalizerBean.periodicJobsSFtpFinalizerBean,
                periodicJobsFinalizerBean.periodicJobsMailFinalizerBean).forEach(finalizer ->
                finalizer.withJobStoreServiceConnector(serviceHub.jobStoreServiceConnector)
                        .withWeekResolverConnector(weekResolverConnector));
    }


    @Override
    public void handleConsumedMessage(ConsumedMessage consumedMessage)
            throws InvalidMessageException, NullPointerException {
        Chunk chunk = unmarshallPayload(consumedMessage);
        EntityManager entityManager = entityManagerFactory.createEntityManager();
        EntityTransaction transaction = entityManager.getTransaction();
        try {
            Chunk result;
            transaction.begin();
            if (chunk.isTerminationChunk()) {
                // Give the before-last message enough time to commit
                // its datablocks to the database before initiating
                // the finalization process.
                // (The result is uploaded to the job-store before the
                // implicit commit, so without the sleep pause, there was a
                // small risk that the end-chunk would reach this bean
                // before all data was available.)
                Tools.sleep(5000);
                result = periodicJobsFinalizerBean.handleTerminationChunk(chunk, entityManager);
            } else {
                result = handleChunk(chunk, entityManager);
            }
            sendResultToJobStore(result);
            transaction.commit();
        } finally {
            if(transaction.isActive()) transaction.rollback();
        }
    }

    @Override
    public void abortJob(int jobId) {
        EntityManager entityManager = entityManagerFactory.createEntityManager();
        EntityTransaction transaction = entityManager.getTransaction();
        transaction.begin();
        try {
            periodicJobsFinalizerBean.deleteDelivery(jobId, entityManager);
            periodicJobsFinalizerBean.deleteDataBlocks(jobId, entityManager);
            LOGGER.info("Aborted job {}", jobId);
        } finally {
            if(transaction.isActive()) transaction.commit();
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

    Chunk handleChunk(Chunk chunk, EntityManager entityManager) {
        Chunk result = new Chunk(chunk.getJobId(), chunk.getChunkId(), Chunk.Type.DELIVERED);
        try {
            for (ChunkItem chunkItem : chunk.getItems()) {
                DBCTrackedLogContext.setTrackingId(chunkItem.getTrackingId());
                result.insertItem(handleChunkItem(chunkItem, chunk, entityManager));
            }
        } finally {
            DBCTrackedLogContext.remove();
        }
        return result;
    }

    private ChunkItem handleChunkItem(ChunkItem chunkItem, Chunk chunk, EntityManager entityManager) {
        ChunkItem result = new ChunkItem()
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
                    convertChunkItem(chunkItem, chunk, entityManager);
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

    private void convertChunkItem(ChunkItem chunkItem, Chunk chunk, EntityManager entityManager) {
        try {
            AddiReader addiReader = new AddiReader(new ByteArrayInputStream(chunkItem.getData()));
            byte[] data;
            int recordPart = 0;
            while (addiReader != null && addiReader.hasNext()) {
                PeriodicJobsDataBlock.Key key = new PeriodicJobsDataBlock.Key(chunk.getJobId(),
                        getRecordNumber((int) chunk.getChunkId(), (int) chunkItem.getId()), recordPart);
                AddiRecord addiRecord;
                PeriodicJobsConversionParam conversionParam;
                String sortkey;
                byte[] groupHeader = null;
                try {
                    addiRecord = addiReader.next();
                    conversionParam = getConversionParam(addiRecord);
                    data = convertAddiRecord(addiRecord, conversionParam, key);
                    sortkey = conversionParam.getSortkey()
                            .orElse(getDefaultSortKey(key.getRecordNumber()));
                    groupHeader = conversionParam.getGroupHeader()
                            .map(header -> header.getBytes(conversionParam.getEncoding()
                                    .orElse(StandardCharsets.UTF_8)))
                            .orElse(null);
                } catch (IOException e) {
                    // We assume that the IOException was caused by non-addi chunk item content
                    addiReader = null;
                    data = chunkItem.getData();
                    if (data == null || data.length == 0) {
                        throw new IOException("Chunk item has empty data");
                    }
                    sortkey = getDefaultSortKey(key.getRecordNumber());
                }

                // Persists result of conversion as datablock
                PeriodicJobsDataBlock datablock = new PeriodicJobsDataBlock();
                datablock.setKey(key);
                datablock.setSortkey(sortkey);
                datablock.setBytes(data);
                datablock.setGroupHeader(groupHeader);

                storeDataBlock(datablock, entityManager);

                recordPart++;
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private byte[] convertAddiRecord(AddiRecord addiRecord, PeriodicJobsConversionParam conversionParam,
                                     PeriodicJobsDataBlock.Key key) {
        // Convert the ADDI content data
        // TODO: 16/01/2020 Currently the ConversionFactory only handles ISO2709 conversion - more conversions may be needed.
        Conversion conversion = conversionFactory.newConversion(conversionParam);
        byte[] data = conversion.apply(addiRecord.getContentData());

        if (data == null || data.length == 0) {
            LOGGER.warn("Conversion for job {} item {} produced empty result",
                    key.getJobId(), key.getRecordNumber());
            throw new ConversionException("Conversion produced empty result");
        }
        if (conversionParam.getRecordHeader().isPresent()) {
            data = prependRecordHeader(data, conversionParam);
        }
        return data;
    }

    private byte[] prependRecordHeader(byte[] data, PeriodicJobsConversionParam conversionParam) {
        String recordHeader = conversionParam.getRecordHeader().orElse("");
        byte[] recordHeaderBytes = recordHeader.getBytes(
                conversionParam.getEncoding().orElse(StandardCharsets.UTF_8));
        byte[] withHeader = new byte[recordHeaderBytes.length + data.length];
        System.arraycopy(recordHeaderBytes, 0, withHeader, 0, recordHeaderBytes.length);
        System.arraycopy(data, 0, withHeader, recordHeaderBytes.length, data.length);
        return withHeader;
    }

    private PeriodicJobsConversionParam getConversionParam(AddiRecord addiRecord) {
        try {
            // Extract parameters from ADDI metadata
            return MAPPER.readValue(StringUtil.asString(addiRecord.getMetaData()), PeriodicJobsConversionParam.class);
        } catch (JsonProcessingException e) {
            throw new ConversionException(e);
        }
    }

    private int getRecordNumber(int chunkId, int itemId) {
        return 10 * chunkId + itemId;
    }

    private String getDefaultSortKey(int recordNumber) {
        // Record number as zero padded string of length 9
        return String.format("%09d", recordNumber);
    }

    private void storeDataBlock(PeriodicJobsDataBlock datablock, EntityManager entityManager) {
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
