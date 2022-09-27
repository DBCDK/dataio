package dk.dbc.dataio.sink.diff;

import dk.dbc.dataio.commons.types.Chunk;
import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.types.ConsumedMessage;
import dk.dbc.dataio.commons.types.Diagnostic;
import dk.dbc.dataio.commons.types.exceptions.InvalidMessageException;
import dk.dbc.dataio.commons.types.exceptions.ServiceException;
import dk.dbc.dataio.commons.utils.lang.StringUtil;
import dk.dbc.dataio.sink.types.AbstractSinkMessageConsumerBean;
import dk.dbc.dataio.sink.types.SinkException;
import dk.dbc.javascript.recordprocessing.FailRecord;
import dk.dbc.log.DBCTrackedLogContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.ActivationConfigProperty;
import javax.ejb.MessageDriven;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@MessageDriven(name = "diffListener", activationConfig = {
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

    ExternalToolDiffGenerator externalToolDiffGenerator = new ExternalToolDiffGenerator();
    AddiDiffGenerator addiDiffGenerator = new AddiDiffGenerator(externalToolDiffGenerator);

    @Override
    public void handleConsumedMessage(ConsumedMessage consumedMessage)
            throws ServiceException, InvalidMessageException {
        final Chunk chunk = unmarshallPayload(consumedMessage);
        LOGGER.info("Received chunk {}/{}", chunk.getJobId(), chunk.getChunkId());
        final Chunk result = handleChunk(chunk);
        uploadChunk(result);
    }

    /**
     * <br/> All 'current' input items have their status compared with the status of their 'next' counterpart.
     * <br/> If status differs, a item is created with status FAILURE in the result chunk.
     * <br/> If status of 'current' and 'next is identical:
     * <br/> All input items with status IGNORE are converted into IGNORE items in result.
     * <br/> All input items with status FAILURE are converted into IGNORE items in result.
     * <br/> All 'current' input items with status SUCCESS have their data compared with the data of their 'next' counterpart:
     * <br/>  - If the diff produces an empty string, the item is converted into a SUCCESS item in result.
     * <br/>  - If the diff produces a non-empty string, the item is converted into a FAILURE item in result.
     * <br/>  - If a DiffGeneratorException is thrown while comparing, the item is converted into a FAILURE item result.
     *
     * @param chunk processed chunk
     * @return result of diff
     * @throws SinkException on failure to produce diff
     */
    Chunk handleChunk(Chunk chunk) throws SinkException {
        if (!chunk.hasNextItems()) {
            return failWithMissingNextItem(chunk);
        }

        final Chunk result = new Chunk(chunk.getJobId(), chunk.getChunkId(), Chunk.Type.DELIVERED);
        try {
            for (final ChunkItemPair item : getChunkItemPairs(chunk)) {
                DBCTrackedLogContext.setTrackingId(item.current.getTrackingId());
                LOGGER.info("Handling item {}/{}/{}",
                        chunk.getJobId(), chunk.getChunkId(), item.current.getId());
                if (item.current.getStatus() != item.next.getStatus()) {
                    final String message = String.format("Different status %s -> %s\n%s",
                            statusToString(item.current.getStatus()),
                            statusToString(item.next.getStatus()),
                            StringUtil.asString(item.next.getData())
                    );
                    result.insertItem(ChunkItem.failedChunkItem()
                            .withId(item.current.getId())
                            .withData(message)
                            .withType(ChunkItem.Type.STRING)
                            .withTrackingId(item.current.getTrackingId())
                            .withDiagnostics(new Diagnostic(Diagnostic.Level.FATAL, message)));
                    continue;
                }

                switch (item.current.getStatus()) {
                    case SUCCESS:
                        result.insertItem(getChunkItemWithDiffResult(item));
                        break;
                    case FAILURE:
                        result.insertItem(compareFailedItems(item));
                        break;
                    case IGNORE:
                        result.insertItem(ChunkItem.ignoredChunkItem()
                                .withId(item.current.getId())
                                .withData("Ignored by diff sink")
                                .withTrackingId(item.current.getTrackingId()));
                        break;
                    default:
                        throw new SinkException("Unknown chunk item state: " +
                                item.current.getStatus().name());
                }
            }
        } finally {
            DBCTrackedLogContext.remove();
        }
        return result;
    }

    private ChunkItem compareFailedItems(ChunkItemPair item) {
        // We are only interested in chunk items with a single diagnostic
        if (item.current.getDiagnostics().size() == 1
                && item.next.getDiagnostics().size() == 1) {
            final Diagnostic currentDiagnostic = item.current.getDiagnostics().get(0);
            final Diagnostic nextDiagnostic = item.next.getDiagnostics().get(0);

            // PMD wants all these checks inside a single if even though readability suffers
            if (currentDiagnostic.getTag() != null
                    && currentDiagnostic.getTag().equals(FailRecord.class.getName())
                    && nextDiagnostic.getTag() != null
                    && nextDiagnostic.getTag().equals(FailRecord.class.getName())
                    && currentDiagnostic.getMessage().equals(nextDiagnostic.getMessage())) {
                return ChunkItem.successfulChunkItem()
                        .withId(item.current.getId())
                        .withData("Current and next output were identical")
                        .withType(ChunkItem.Type.STRING)
                        .withTrackingId(item.current.getTrackingId());
            }
        }
        return ChunkItem.ignoredChunkItem()
                .withId(item.current.getId())
                .withData("Failed by diff processor")
                .withType(ChunkItem.Type.STRING)
                .withTrackingId(item.current.getTrackingId());
    }

    private Chunk failWithMissingNextItem(Chunk chunk) {
        final Chunk result = new Chunk(chunk.getJobId(), chunk.getChunkId(), Chunk.Type.DELIVERED);

        for (final ChunkItem item : chunk) {
            result.insertItem(ChunkItem.failedChunkItem()
                    .withId(item.getId())
                    .withData("Missing next item")
                    .withType(ChunkItem.Type.STRING)
                    .withTrackingId(item.getTrackingId())
                    .withDiagnostics(new Diagnostic(
                            Diagnostic.Level.FATAL, "Missing next item")));
        }
        return result;

    }

    /*
     * This method creates an item containing the diff result.
     * If the diff produces an empty string the resulting item has status SUCCESS.
     * If the diff produces a non-empty string the resulting item has status FAILURE.
     */
    private ChunkItem getChunkItemWithDiffResult(ChunkItemPair pair) {
        if (Arrays.equals(pair.current.getData(), pair.next.getData())) {
            return ChunkItem.successfulChunkItem()
                    .withId(pair.current.getId())
                    .withData("Current and next output were identical")
                    .withType(ChunkItem.Type.STRING)
                    .withTrackingId(pair.current.getTrackingId());
        }

        String diff;
        try {
            try {
                diff = addiDiffGenerator.getDiff(pair.current.getData(), pair.next.getData());
            } catch (IllegalArgumentException e) {
                final ExternalToolDiffGenerator.Kind currentKind = DiffKindDetector.getKind(pair.current.getData());
                final ExternalToolDiffGenerator.Kind nextKind = DiffKindDetector.getKind(pair.next.getData());
                if (currentKind == nextKind) {
                    diff = externalToolDiffGenerator.getDiff(currentKind, pair.current.getData(), pair.next.getData());
                } else {
                    diff = externalToolDiffGenerator.getDiff(ExternalToolDiffGenerator.Kind.PLAINTEXT,
                            pair.current.getData(), pair.next.getData());
                }
            }
            if (diff.isEmpty()) {
                return ChunkItem.successfulChunkItem()
                        .withId(pair.current.getId())
                        .withData("Current and next output were identical")
                        .withType(ChunkItem.Type.STRING)
                        .withTrackingId(pair.current.getTrackingId());
            }
            return ChunkItem.failedChunkItem()
                    .withId(pair.current.getId())
                    .withData(diff)
                    .withType(ChunkItem.Type.STRING)
                    .withTrackingId(pair.current.getTrackingId())
                    .withDiagnostics(new Diagnostic(Diagnostic.Level.FATAL,
                            "Diff created: current and next output were not identical"));
        } catch (DiffGeneratorException e) {
            return ChunkItem.failedChunkItem()
                    .withId(pair.current.getId())
                    .withData(StringUtil.getStackTraceString(e, ""))
                    .withType(ChunkItem.Type.STRING)
                    .withTrackingId(pair.current.getTrackingId())
                    .withDiagnostics(new Diagnostic(Diagnostic.Level.FATAL,
                            "Exception occurred while comparing items", e));
        }
    }

    private static String statusToString(ChunkItem.Status status) {
        switch (status) {
            case FAILURE:
                return "Failure";
            case SUCCESS:
                return "Success";
            case IGNORE:
                return "Ignore";
            default:
                return "Internal Error: Unknown Status";
        }
    }

    private static class ChunkItemPair {
        public ChunkItemPair(ChunkItem current, ChunkItem next) {
            this.current = current;
            this.next = next;
        }

        public ChunkItem current;
        public ChunkItem next;
    }

    private List<ChunkItemPair> getChunkItemPairs(Chunk chunk) {
        final List<ChunkItem> items = chunk.getItems();
        final List<ChunkItem> next = chunk.getNext();
        if (items.size() != next.size()) {
            throw new IllegalArgumentException("Current and next size differ");
        }
        final List<ChunkItemPair> result = new ArrayList<>();
        for (int i = 0; i < items.size(); i++) {
            result.add(new ChunkItemPair(items.get(i), next.get(i)));
        }
        return result;
    }
}
