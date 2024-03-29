package dk.dbc.dataio.sink.diff;

import dk.dbc.dataio.commons.types.Chunk;
import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.types.ConsumedMessage;
import dk.dbc.dataio.commons.types.Diagnostic;
import dk.dbc.dataio.commons.types.exceptions.InvalidMessageException;
import dk.dbc.dataio.commons.utils.lang.StringUtil;
import dk.dbc.dataio.jse.artemis.common.jms.MessageConsumerAdapter;
import dk.dbc.dataio.jse.artemis.common.service.ServiceHub;
import dk.dbc.javascript.recordprocessing.FailRecord;
import dk.dbc.log.DBCTrackedLogContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static dk.dbc.dataio.sink.diff.Kind.detect;

public class MessageConsumerBean extends MessageConsumerAdapter {
    private static final Logger LOGGER = LoggerFactory.getLogger(MessageConsumerBean.class);
    private static final String QUEUE = SinkConfig.QUEUE.fqnAsQueue();
    private static final String ADDRESS = SinkConfig.QUEUE.fqnAsAddress();

    private final DiffGenerator diffGenerator;
    private final AddiDiffGenerator addiDiffGenerator;

    public MessageConsumerBean(ServiceHub serviceHub) {
        super(serviceHub);
        diffGenerator = SinkConfig.USE_NATIVE_DIFF.asBoolean() ? new ExternalToolDiffGenerator() : new JavaDiffGenerator();
        addiDiffGenerator = new AddiDiffGenerator(diffGenerator);
    }

    public MessageConsumerBean(ServiceHub serviceHub, ExternalToolDiffGenerator diffGenerator, AddiDiffGenerator addiDiffGenerator) {
        super(serviceHub);
        this.diffGenerator = diffGenerator;
        this.addiDiffGenerator = addiDiffGenerator;
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

    @Override
    public void handleConsumedMessage(ConsumedMessage consumedMessage) throws InvalidMessageException {
        Chunk chunk = unmarshallPayload(consumedMessage);
        Chunk result = handleChunk(chunk);
        sendResultToJobStore(result);
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
     * @throws InvalidMessageException on failure to produce diff
     */
    public Chunk handleChunk(Chunk chunk) throws InvalidMessageException {
        if (!chunk.hasNextItems()) {
            return failWithMissingNextItem(chunk);
        }

        Chunk result = new Chunk(chunk.getJobId(), chunk.getChunkId(), Chunk.Type.DELIVERED);
        try {
            for (ChunkItemPair item : getChunkItemPairs(chunk)) {
                DBCTrackedLogContext.setTrackingId(item.current.getTrackingId());
                LOGGER.info("Handling item {}/{}/{}",
                        chunk.getJobId(), chunk.getChunkId(), item.current.getId());
                if (item.current.getStatus() != item.next.getStatus()) {
                    String message = String.format("Different status %s -> %s\n%s",
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
                        throw new InvalidMessageException("Unknown chunk item state: " + item.current.getStatus().name());
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
            Diagnostic currentDiagnostic = item.current.getDiagnostics().get(0);
            Diagnostic nextDiagnostic = item.next.getDiagnostics().get(0);

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
        Chunk result = new Chunk(chunk.getJobId(), chunk.getChunkId(), Chunk.Type.DELIVERED);

        for (ChunkItem item : chunk) {
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
    private ChunkItem getChunkItemWithDiffResult(ChunkItemPair pair) throws InvalidMessageException {
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
                Kind currentKind = detect(pair.current.getData());
                Kind nextKind = detect(pair.next.getData());
                if (currentKind == nextKind) {
                    diff = diffGenerator.getDiff(currentKind, pair.current.getData(), pair.next.getData());
                } else {
                    diff = diffGenerator.getDiff(Kind.PLAINTEXT,
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

    private List<ChunkItemPair> getChunkItemPairs(Chunk chunk) {
        List<ChunkItem> items = chunk.getItems();
        List<ChunkItem> next = chunk.getNext();
        if (items.size() != next.size()) {
            throw new IllegalArgumentException("Current and next size differ");
        }
        List<ChunkItemPair> result = new ArrayList<>();
        for (int i = 0; i < items.size(); i++) {
            result.add(new ChunkItemPair(items.get(i), next.get(i)));
        }
        return result;
    }

    private static class ChunkItemPair {
        public ChunkItem current;
        public ChunkItem next;

        public ChunkItemPair(ChunkItem current, ChunkItem next) {
            this.current = current;
            this.next = next;
        }
    }
}
