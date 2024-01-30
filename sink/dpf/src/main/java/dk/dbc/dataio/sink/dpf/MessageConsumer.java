package dk.dbc.dataio.sink.dpf;

import dk.dbc.commons.addi.AddiReader;
import dk.dbc.commons.addi.AddiRecord;
import dk.dbc.commons.jsonb.JSONBContext;
import dk.dbc.commons.jsonb.JSONBException;
import dk.dbc.dataio.commons.types.Chunk;
import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.types.ConsumedMessage;
import dk.dbc.dataio.commons.types.Diagnostic;
import dk.dbc.dataio.commons.types.exceptions.InvalidMessageException;
import dk.dbc.dataio.commons.utils.lang.StringUtil;
import dk.dbc.dataio.jse.artemis.common.jms.MessageConsumerAdapter;
import dk.dbc.dataio.jse.artemis.common.service.ServiceHub;
import dk.dbc.dataio.sink.dpf.model.DpfRecord;
import dk.dbc.dataio.sink.dpf.model.ProcessingInstructions;
import dk.dbc.dataio.sink.dpf.transform.DpfRecordProcessor;
import dk.dbc.dataio.sink.dpf.transform.DpfRecordProcessorException;
import dk.dbc.dataio.sink.dpf.transform.MarcRecordFactory;
import dk.dbc.log.DBCTrackedLogContext;
import dk.dbc.marc.reader.MarcReaderException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class MessageConsumer extends MessageConsumerAdapter {
    private static final Logger LOGGER = LoggerFactory.getLogger(MessageConsumer.class);

    private final JSONBContext jsonbContext = new JSONBContext();
    private static final String QUEUE = SinkConfig.QUEUE.fqnAsQueue();
    private static final String ADDRESS = SinkConfig.QUEUE.fqnAsAddress();

    private final ConfigBean configBean;
    private final ServiceBroker serviceBroker;


    public MessageConsumer(ServiceHub serviceHub, ServiceBroker serviceBroker) {
        super(serviceHub);
        this.serviceBroker = serviceBroker;
        configBean = serviceBroker.configBean;
    }

    @Override
    public void handleConsumedMessage(ConsumedMessage consumedMessage) throws InvalidMessageException {
        Chunk chunk = unmarshallPayload(consumedMessage);
        LOGGER.info("Received chunk {}/{}", chunk.getJobId(), chunk.getChunkId());
        configBean.refresh(consumedMessage);
        sendResultToJobStore(handleChunk(chunk));
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
        Chunk result = new Chunk(chunk.getJobId(), chunk.getChunkId(), Chunk.Type.DELIVERED);
        try {
            for (ChunkItem chunkItem : chunk.getItems()) {
                DBCTrackedLogContext.setTrackingId(chunkItem.getTrackingId());
                String id = String.join(".",
                        Integer.toString(chunk.getJobId()),
                        Long.toString(chunk.getChunkId()),
                        Long.toString(chunkItem.getId()));
                result.insertItem(handleChunkItem(chunkItem, id));
            }
        } finally {
            DBCTrackedLogContext.remove();
        }
        return result;
    }

    private ChunkItem handleChunkItem(ChunkItem chunkItem, String id) {
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
                    return result
                            .withStatus(ChunkItem.Status.SUCCESS)
                            .withData(formatDpfRecordProcessorEvents(
                                    new DpfRecordProcessor(serviceBroker, configBean.getQueueProvider())
                                            .process(getDpfRecords(chunkItem, id))));
            }
        } catch (DpfRecordProcessorException | IOException | JSONBException | MarcReaderException e) {
            LOGGER.warn("Failed to handle chunk {}", chunkItem.getTrackingId(), e);
            return result
                    .withStatus(ChunkItem.Status.FAILURE)
                    .withDiagnostics(
                            new Diagnostic(Diagnostic.Level.FATAL, e.getMessage(), e))
                    .withData(e.getMessage());
        }
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
