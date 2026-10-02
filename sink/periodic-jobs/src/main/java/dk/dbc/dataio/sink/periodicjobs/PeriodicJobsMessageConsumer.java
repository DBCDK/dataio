package dk.dbc.dataio.sink.periodicjobs;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dk.dbc.commons.addi.AddiReader;
import dk.dbc.commons.addi.AddiRecord;
import dk.dbc.commons.useragent.UserAgent;
import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnector;
import dk.dbc.dataio.commons.conversion.Conversion;
import dk.dbc.dataio.commons.conversion.ConversionException;
import dk.dbc.dataio.commons.conversion.ConversionFactory;
import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.types.ConsumedMessage;
import dk.dbc.dataio.commons.types.Diagnostic;
import dk.dbc.dataio.commons.types.exceptions.InvalidMessageException;
import dk.dbc.dataio.commons.types.jms.JMSHeader;
import dk.dbc.dataio.commons.utils.lang.StringUtil;
import dk.dbc.dataio.filestore.service.connector.FileStoreServiceConnector;
import dk.dbc.dataio.jobstore.types.ItemDeliveryResult;
import dk.dbc.dataio.jse.artemis.common.jms.SinkMessageConsumerAdapter;
import dk.dbc.dataio.jse.artemis.common.service.ServiceHub;
import dk.dbc.dataio.sink.periodicjobs.mail.MailSession;
import dk.dbc.dataio.sink.periodicjobs.pickup.PeriodicJobsFtpFinalizerBean;
import dk.dbc.dataio.sink.periodicjobs.pickup.PeriodicJobsHttpFinalizerBean;
import dk.dbc.dataio.sink.periodicjobs.pickup.PeriodicJobsMailFinalizerBean;
import dk.dbc.dataio.sink.periodicjobs.pickup.PeriodicJobsSFtpFinalizerBean;
import dk.dbc.httpclient.FailSafeHttpClient;
import dk.dbc.proxy.ProxyBean;
import dk.dbc.weekresolver.connector.WeekResolverConnector;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.core.Response;
import net.jodah.failsafe.RetryPolicy;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Set;

public class PeriodicJobsMessageConsumer extends SinkMessageConsumerAdapter {

    private static final RetryPolicy<Response> RETRY_POLICY = new RetryPolicy<Response>()
            .handle(ProcessingException.class)
            .handleResultIf(response ->
                    response.getStatus() == 404
                            || response.getStatus() == 500
                            || response.getStatus() == 502)
            .withDelay(Duration.ofSeconds(5))
            .withMaxRetries(3);

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
        this.flowStoreServiceConnector = new FlowStoreServiceConnector(ClientBuilder.newClient().register(new JacksonFeature()),
                UserAgent.forInternalRequests(), SinkConfig.FLOWSTORE_URL.asString());
        this.fileStoreServiceConnector = new FileStoreServiceConnector(ClientBuilder.newClient().register(new JacksonFeature()),
                UserAgent.forInternalRequests(), SinkConfig.FILESTORE_URL.asString());

        final Client client = ClientBuilder.newClient().register(new JacksonFeature());
        final FailSafeHttpClient failSafeHttpClient = FailSafeHttpClient.create(client, UserAgent.forInternalRequests(), RETRY_POLICY);
        this.weekResolverConnector = new WeekResolverConnector(failSafeHttpClient, SinkConfig.WEEKRESOLVER_SERVICE_URL.asString());

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

    /**
     * An item here is converted and persisted as a datablock rather than sent to a target
     * system, and nothing leaves this sink until the job's termination item triggers the
     * pickup, so there is no "a newer version of this record was already delivered"
     * question to ask about a single item
     * <p>
     * See docs/chunk-scheduling-redesign.md, Watermark opt-out.
     */
    @Override
    protected boolean usesDeliveryWatermark() {
        return false;
    }

    /**
     * Converts one item into datablocks, or, for the job's termination item, delivers the
     * datablocks accumulated by every preceding item to the job's pickup destination
     * <p>
     * The transaction is committed before this method returns, and only then does
     * {@link SinkMessageConsumerAdapter} report the result. That order is what lets the
     * job-end finalization run against complete data: a reported item is an item whose
     * datablocks are durable, and the termination chunk is released only once every data
     * item of the job has reported.
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
                    ? periodicJobsFinalizerBean.finalizeJob(jobId, chunkId, entityManager)
                    : convertItem(item, jobId, chunkId, itemId, entityManager);
            transaction.commit();
            return ItemDeliveryResult.of(verdictOf(outcome), outcome);
        } catch (InvalidMessageException e) {
            // Thrown by the job-end finalization alone, and reported as failed rather than
            // rethrown. A failed termination item completes the job and sets its fatal error
            // flag. Nothing is committed, since the delivery it was rejected by did not happen.
            LOGGER.error("Finalization of periodic job {} was rejected", jobId, e);
            transaction.rollback();
            return ItemDeliveryResult.of(ItemDeliveryResult.Status.FAILED, jobEndFailure(item, e));
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
        transaction.begin();
        try {
            periodicJobsFinalizerBean.deleteDelivery(jobId, entityManager);
            periodicJobsFinalizerBean.deleteDataBlocks(jobId, entityManager);
            LOGGER.info("Aborted job {}", jobId);
        } finally {
            if(transaction.isActive()) transaction.commit();
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

    /**
     * The delivering outcome recorded for a job whose finalization was rejected, keeping
     * the item's JOB_END type so the job view still shows it for what it is
     */
    private static ChunkItem jobEndFailure(ChunkItem item, Exception cause) {
        return new ChunkItem()
                .withId(item.getId())
                .withStatus(ChunkItem.Status.FAILURE)
                .withType(ChunkItem.Type.JOB_END)
                .withTrackingId(item.getTrackingId())
                .withDiagnostics(new Diagnostic(Diagnostic.Level.FATAL, cause.getMessage(), cause))
                .withData(cause.getMessage());
    }

    /**
     * Converts one processed item into datablocks, to be delivered when the job ends
     *
     * @return delivering outcome for the item, which {@link #verdictOf(ChunkItem)} turns
     * into the verdict reported for it
     */
    ChunkItem convertItem(ChunkItem item, int jobId, int chunkId, short itemId, EntityManager entityManager) {
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
                    convertToDataBlocks(item, jobId, chunkId, itemId, entityManager);
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

    private void convertToDataBlocks(ChunkItem item, int jobId, int chunkId, short itemId,
                                     EntityManager entityManager) {
        int recordNumber = getRecordNumber(chunkId, itemId);
        try {
            AddiReader addiReader = new AddiReader(new ByteArrayInputStream(item.getData()));
            byte[] data;
            int recordPart = 0;
            while (addiReader != null && addiReader.hasNext()) {
                PeriodicJobsDataBlock.Key key = new PeriodicJobsDataBlock.Key(jobId, recordNumber, recordPart);
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
                    data = item.getData();
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
