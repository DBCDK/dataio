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
import dk.dbc.dataio.sink.dpf.model.DpfRecord;
import dk.dbc.dataio.sink.dpf.model.ProcessingInstructions;
import dk.dbc.dataio.sink.types.AbstractSinkMessageConsumerBean;
import dk.dbc.dataio.sink.types.SinkException;
import dk.dbc.log.DBCTrackedLogContext;
import dk.dbc.marc.reader.MarcReaderException;
import dk.dbc.util.Timed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.ActivationConfigProperty;
import javax.ejb.EJB;
import javax.ejb.MessageDriven;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@MessageDriven(name = "dpfListener", activationConfig = {
        // Please see the following url for a explanation of the available settings.
        // The message selector variable is defined in the dataio-secrets project
        // https://activemq.apache.org/activation-spec-properties
        @ActivationConfigProperty(propertyName = "destination", propertyValue = "jms/dataio/sinks"),
        @ActivationConfigProperty(propertyName = "useJndi", propertyValue = "true"),
        @ActivationConfigProperty(propertyName = "destinationType", propertyValue = "javax.jms.Queue"),
        @ActivationConfigProperty(propertyName = "resourceAdapter", propertyValue = "artemis"),
        @ActivationConfigProperty(propertyName = "messageSelector", propertyValue = "resource = '${ENV=MESSAGE_NAME_FILTER}'"),
        @ActivationConfigProperty(propertyName = "initialRedeliveryDelay", propertyValue = "5000"),
        @ActivationConfigProperty(propertyName = "redeliveryUseExponentialBackOff", propertyValue = "true")
})
public class MessageConsumerBean extends AbstractSinkMessageConsumerBean {
    private static final Logger LOGGER = LoggerFactory.getLogger(MessageConsumerBean.class);

    private final JSONBContext jsonbContext = new JSONBContext();

    @EJB
    ConfigBean configBean;
    @EJB
    ServiceBroker serviceBroker;

    @Timed
    @Override
    public void handleConsumedMessage(ConsumedMessage consumedMessage) throws InvalidMessageException, SinkException {
        final Chunk chunk = unmarshallPayload(consumedMessage);
        LOGGER.info("Received chunk {}/{}", chunk.getJobId(), chunk.getChunkId());

        configBean.refresh(consumedMessage);

        uploadChunk(handleChunk(chunk));
    }

    Chunk handleChunk(Chunk chunk) {
        final Chunk result = new Chunk(chunk.getJobId(), chunk.getChunkId(), Chunk.Type.DELIVERED);
        try {
            for (ChunkItem chunkItem : chunk.getItems()) {
                DBCTrackedLogContext.setTrackingId(chunkItem.getTrackingId());
                final String id = String.join(".",
                        Long.toString(chunk.getJobId()),
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
                    return result
                            .withStatus(ChunkItem.Status.SUCCESS)
                            .withData(formatDpfRecordProcessorEvents(
                                    new DpfRecordProcessor(serviceBroker, configBean.getQueueProvider())
                                            .process(getDpfRecords(chunkItem, id))));
            }
        } catch (DpfRecordProcessorException | IOException | JSONBException | MarcReaderException e) {
            return result
                    .withStatus(ChunkItem.Status.FAILURE)
                    .withDiagnostics(
                            new Diagnostic(Diagnostic.Level.FATAL, e.getMessage(), e))
                    .withData(e.getMessage());
        }
    }

    private List<DpfRecord> getDpfRecords(ChunkItem chunkItem, String id)
            throws IOException, JSONBException, MarcReaderException {
        final List<DpfRecord> dpfRecords = new ArrayList<>();
        final AddiReader addiReader = new AddiReader(new ByteArrayInputStream(chunkItem.getData()));
        int idx = 1;
        while (addiReader.hasNext()) {
            final AddiRecord addiRecord = addiReader.next();
            final ProcessingInstructions processingInstructions = jsonbContext.unmarshall(
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
