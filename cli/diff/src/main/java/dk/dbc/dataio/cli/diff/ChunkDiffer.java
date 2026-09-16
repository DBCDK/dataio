package dk.dbc.dataio.cli.diff;

import dk.dbc.dataio.commons.types.Chunk;
import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.types.Diagnostic;
import dk.dbc.dataio.commons.types.exceptions.InvalidMessageException;
import dk.dbc.dataio.commons.utils.lang.StringUtil;
import dk.dbc.javascript.recordprocessing.FailRecord;
import dk.dbc.log.DBCTrackedLogContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static dk.dbc.dataio.cli.diff.Kind.detect;

/**
 * Compares two processing outcomes of the same records and reports the differences as a chunk.
 * <p>
 * The two outcomes come from running the same data through two revisions of a flow, which is what
 * the acceptance test runners in {@code cli/acc-test-runner} and {@code cli/flow-test-runner} do
 * locally. The current outcome is the reference and the next outcome is the candidate.
 * </p>
 */
public class ChunkDiffer {
    private static final Logger LOGGER = LoggerFactory.getLogger(ChunkDiffer.class);

    private final DiffGenerator diffGenerator;
    private final AddiDiffGenerator addiDiffGenerator;

    public ChunkDiffer() {
        diffGenerator = DiffConfig.USE_NATIVE_DIFF.asBoolean() ? new ExternalToolDiffGenerator() : new JavaDiffGenerator();
        addiDiffGenerator = new AddiDiffGenerator(diffGenerator);
    }

    public ChunkDiffer(DiffGenerator diffGenerator, AddiDiffGenerator addiDiffGenerator) {
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

    /**
     * Compares each current item with its next counterpart and returns the outcome as a chunk.
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
     * @param jobId   id carried onto the result chunk
     * @param chunkId id carried onto the result chunk
     * @param current reference outcome, one item per record
     * @param next    candidate outcome, one item per record, in the same order as current
     * @return result of diff
     * @throws InvalidMessageException on failure to produce diff
     */
    public Chunk diff(int jobId, long chunkId, List<ChunkItem> current, List<ChunkItem> next) throws InvalidMessageException {
        if (next == null || next.isEmpty()) {
            return failWithMissingNextItem(jobId, chunkId, current);
        }

        Chunk result = new Chunk(jobId, chunkId, Chunk.Type.DELIVERED);
        try {
            for (ChunkItemPair item : getChunkItemPairs(current, next)) {
                DBCTrackedLogContext.setTrackingId(item.current.getTrackingId());
                LOGGER.info("Handling item {}/{}/{}", jobId, chunkId, item.current.getId());
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
                                .withData("Ignored by diff")
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

    private Chunk failWithMissingNextItem(int jobId, long chunkId, List<ChunkItem> current) {
        Chunk result = new Chunk(jobId, chunkId, Chunk.Type.DELIVERED);

        for (ChunkItem item : current) {
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

    private List<ChunkItemPair> getChunkItemPairs(List<ChunkItem> current, List<ChunkItem> next) {
        if (current.size() != next.size()) {
            throw new IllegalArgumentException("Current and next size differ");
        }
        List<ChunkItemPair> result = new ArrayList<>();
        for (int i = 0; i < current.size(); i++) {
            result.add(new ChunkItemPair(current.get(i), next.get(i)));
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
