package dk.dbc.dataio.sink.dpf;

import dk.dbc.commons.addi.AddiReader;
import dk.dbc.commons.addi.AddiRecord;
import dk.dbc.commons.jsonb.JSONBContext;
import dk.dbc.commons.jsonb.JSONBException;
import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.types.ConsumedMessage;
import dk.dbc.dataio.commons.types.Diagnostic;
import dk.dbc.dataio.commons.types.jms.JMSHeader;
import dk.dbc.dataio.commons.utils.lang.StringUtil;
import dk.dbc.dataio.jobstore.types.ItemDeliveryResult;
import dk.dbc.dataio.jse.artemis.common.jms.SinkMessageConsumerAdapter;
import dk.dbc.dataio.jse.artemis.common.service.ServiceHub;
import dk.dbc.dataio.sink.dpf.model.DpfRecord;
import dk.dbc.dataio.sink.dpf.model.ProcessingInstructions;
import dk.dbc.dataio.sink.dpf.transform.DpfRecordProcessor;
import dk.dbc.dataio.sink.dpf.transform.DpfRecordProcessorException;
import dk.dbc.dataio.sink.dpf.transform.MarcRecordFactory;
import dk.dbc.marc.reader.MarcReaderException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class MessageConsumer extends SinkMessageConsumerAdapter {
    private static final Logger LOGGER = LoggerFactory.getLogger(MessageConsumer.class);

    private final JSONBContext jsonbContext = new JSONBContext();
    private static final String QUEUE = SinkConfig.QUEUE.fqnAsQueue();
    private static final String ADDRESS = SinkConfig.QUEUE.fqnAsAddress();

    private final ConfigBean configBean;
    private final ServiceBroker serviceBroker;

    public MessageConsumer(ServiceHub serviceHub, ServiceBroker serviceBroker) {
        this(serviceHub, serviceBroker, serviceBroker.configBean);
    }

    /**
     * Creates a consumer over a given config bean, for tests delivering through a mocked service
     * broker
     * <p>
     * A mock carries no config bean of its own, where the broker this sink runs on creates one and
     * publishes it as a field.
     *
     * @param configBean config bean to refresh per item, normally the broker's own
     */
    MessageConsumer(ServiceHub serviceHub, ServiceBroker serviceBroker, ConfigBean configBean) {
        super(serviceHub);
        this.serviceBroker = serviceBroker;
        this.configBean = configBean;
    }

    /**
     * Processes a successfully processed item's DPF records, and passes any other item through as
     * ignored
     * <p>
     * An item the processor failed or ignored is reported as ignored rather than as delivered, so
     * that it counts towards the job's ignored items and advances no delivery watermark for a
     * record nothing was sent for.
     */
    @Override
    protected ItemDeliveryResult deliverItem(ConsumedMessage message, ChunkItem item) {
        String queueProvider = configBean.refresh(message);
        switch (item.getStatus()) {
            case FAILURE:
                return ignored(item, "Failed by processor");
            case IGNORE:
                return ignored(item, "Ignored by processor");
            default:
                return process(message, item, queueProvider);
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
     * Runs an item's DPF records through the record processor and reports what became of them
     * <p>
     * A processor run that produced events sent the item's records to one of this sink's two target
     * systems, the update service or lobby, since every error path ends by sending every record of
     * the item to lobby. Both are deliveries, and the event log in the outcome names which of the
     * two each record took.
     * <p>
     * An item holding no readable addi record, and one whose record state gave the processor
     * nothing to do, are reported ignored. Delivered is the one verdict that advances the record's
     * delivery watermark, and advancing it here would claim this version of the record as
     * delivered when neither target received it, leaving a genuinely older version arriving
     * afterwards to be judged stale and skipped.
     * <p>
     * A record the processor could not read, and a target that rejected one in a way a second
     * attempt would not change, are reported failed rather than thrown, so that the item is counted
     * and not redelivered for as long as the broker allows. A number roll that cannot hand out a
     * faust still throws, since the next attempt may well succeed.
     */
    private ItemDeliveryResult process(ConsumedMessage message, ChunkItem item, String queueProvider) {
        try {
            List<DpfRecord> dpfRecords = getDpfRecords(item, recordId(message, item));
            if (dpfRecords.isEmpty()) {
                return ignored(item, "No addi records could be read from the item");
            }
            List<DpfRecordProcessor.Event> events =
                    new DpfRecordProcessor(serviceBroker, queueProvider).process(dpfRecords);
            if (events.isEmpty()) {
                return ignored(item, "Nothing was sent, the record state gave the processor nothing to do");
            }
            return ItemDeliveryResult.of(ItemDeliveryResult.Status.DELIVERED,
                    outcome(item)
                            .withStatus(ChunkItem.Status.SUCCESS)
                            .withData(formatDpfRecordProcessorEvents(events)));
        } catch (DpfRecordProcessorException | IOException | JSONBException | MarcReaderException e) {
            LOGGER.warn("Failed to handle item {}", item.getTrackingId(), e);
            return ItemDeliveryResult.of(ItemDeliveryResult.Status.FAILED,
                    outcome(item)
                            .withStatus(ChunkItem.Status.FAILURE)
                            .withDiagnostics(new Diagnostic(Diagnostic.Level.FATAL, e.getMessage(), e))
                            .withData(e.getMessage()));
        }
    }

    /**
     * Names the item the DPF records of one delivery came from, as job id, chunk id and item id
     * <p>
     * The composed value is suffixed per addi record and becomes the id the record is stored under
     * in lobby and the tracking id it is sent to the update service with, so the format is read
     * outside this sink and is kept as the chunk this item was carried in composed it. The job and
     * chunk ids come off the message, being what the framework already reads the item's version
     * tuple from.
     */
    private String recordId(ConsumedMessage message, ChunkItem item) {
        return String.join(".",
                Integer.toString(JMSHeader.jobId.getHeader(message, Integer.class)),
                Long.toString(JMSHeader.chunkId.getHeader(message, Long.class)),
                Long.toString(item.getId()));
    }

    private ItemDeliveryResult ignored(ChunkItem item, String reason) {
        return ItemDeliveryResult.of(ItemDeliveryResult.Status.IGNORED,
                outcome(item)
                        .withStatus(ChunkItem.Status.IGNORE)
                        .withData(reason));
    }

    /**
     * Creates the delivering outcome item of a delivered item, without the status and data naming
     * what became of it
     */
    private ChunkItem outcome(ChunkItem item) {
        return new ChunkItem()
                .withId(item.getId())
                .withTrackingId(item.getTrackingId())
                .withType(ChunkItem.Type.STRING)
                .withEncoding(StandardCharsets.UTF_8);
    }

    private List<DpfRecord> getDpfRecords(ChunkItem chunkItem, String id)
            throws IOException, JSONBException, MarcReaderException {
        List<DpfRecord> dpfRecords = new ArrayList<>();
        AddiReader addiReader = new AddiReader(new ByteArrayInputStream(chunkItem.getData()));
        int idx = 1;
        while (addiReader.hasNext()) {
            AddiRecord addiRecord = addiReader.next();
            ProcessingInstructions processingInstructions = jsonbContext.unmarshall(
                            StringUtil.asString(addiRecord.getMetaData()), ProcessingInstructions.class)
                    .withId(id + "-" + idx);
            dpfRecords.add(new DpfRecord(processingInstructions,
                    MarcRecordFactory.fromMarcXchange(addiRecord.getContentData())));
            idx++;
        }
        return dpfRecords;
    }

    @SuppressWarnings("PMD.UnusedPrivateMethod")
    private String formatDpfRecordProcessorEvents(List<DpfRecordProcessor.Event> events) {
        return events.stream()
                .map(DpfRecordProcessor.Event::toString)
                .collect(Collectors.joining("\n"));
    }
}
