package dk.dbc.dataio.cli.diff;

import dk.dbc.dataio.commons.types.Chunk;
import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.types.Diagnostic;
import dk.dbc.dataio.commons.types.exceptions.InvalidMessageException;
import dk.dbc.javascript.recordprocessing.FailRecord;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

class ChunkDifferTest extends AbstractDiffGeneratorTest {
    private final static String DBC_TRACKING_ID = "dataio_";
    private final static int JOB_ID = 0;
    private final static long CHUNK_ID = 0;

    @Test
    void failOnMissingNextItems() throws InvalidMessageException {
        List<ChunkItem> chunkItems = Arrays.asList(
                ChunkItem.failedChunkItem()
                        .withId(0L)
                        .withTrackingId(DBC_TRACKING_ID + 0),
                ChunkItem.successfulChunkItem()
                        .withId(1L)
                        .withTrackingId(DBC_TRACKING_ID + 1),
                ChunkItem.ignoredChunkItem()
                        .withId(2L)
                        .withTrackingId(DBC_TRACKING_ID + 2));
        Chunk result = newChunkDiffer().diff(JOB_ID, CHUNK_ID, chunkItems, List.of());
        assertThat("number of chunk items in result", result.size(), is(chunkItems.size()));

        Iterator<ChunkItem> iterator = result.iterator();

        ChunkItem item = iterator.next();
        assertThat("1st item status", item.getStatus(), is(ChunkItem.Status.FAILURE));
        assertThat("1st item diagnostic", item.getDiagnostics().size(), is(1));
        assertThat("1st item trackingId", item.getTrackingId(), is(DBC_TRACKING_ID + 0));

        item = iterator.next();
        assertThat("2nd item status", item.getStatus(), is(ChunkItem.Status.FAILURE));
        assertThat("2nd item diagnostics", item.getDiagnostics().size(), is(1));
        assertThat("2nd item trackingId", item.getTrackingId(), is(DBC_TRACKING_ID + 1));

        item = iterator.next();
        assertThat("3rd item status", item.getStatus(), is(ChunkItem.Status.FAILURE));
        assertThat("3rd item diagnostics", item.getDiagnostics().size(), is(1));
        assertThat("3rd item trackingId", item.getTrackingId(), is(DBC_TRACKING_ID + 2));
        assertThat(iterator.hasNext(), is(false));
    }

    @Test
    void failOnXmlDiff() throws InvalidMessageException {
        List<ChunkItem> currentItems = Arrays.asList(
                ChunkItem.failedChunkItem()
                        .withId(0L)
                        .withTrackingId(DBC_TRACKING_ID + 0)
                        .withData("not xml")
                        .withDiagnostics(new Diagnostic(Diagnostic.Level.ERROR, "failed")),
                ChunkItem.successfulChunkItem()
                        .withId(1L)
                        .withTrackingId(DBC_TRACKING_ID + 1)
                        .withData("<data>1</data>"),
                ChunkItem.ignoredChunkItem()
                        .withId(2L)
                        .withTrackingId(DBC_TRACKING_ID + 2)
                        .withData("not xml"),
                ChunkItem.successfulChunkItem()
                        .withId(3L)
                        .withTrackingId(DBC_TRACKING_ID + 3)
                        .withData("<data>3</data>"));
        List<ChunkItem> nextItems = Arrays.asList(
                ChunkItem.failedChunkItem()
                        .withId(0L)
                        .withTrackingId(DBC_TRACKING_ID + 0)
                        .withData("not xml")
                        .withDiagnostics(new Diagnostic(Diagnostic.Level.ERROR, "failed")),
                ChunkItem.successfulChunkItem()
                        .withId(1L)
                        .withTrackingId(DBC_TRACKING_ID + 1)
                        .withData("<data>one</data>"),
                ChunkItem.ignoredChunkItem()
                        .withId(2L)
                        .withTrackingId(DBC_TRACKING_ID + 2)
                        .withData("not xml"),
                ChunkItem.successfulChunkItem()
                        .withId(3L)
                        .withTrackingId(DBC_TRACKING_ID + 3)
                        .withData("<data>3</data>"));
        Chunk result = newChunkDiffer().diff(JOB_ID, CHUNK_ID, currentItems, nextItems);
        assertThat("number of chunk items in result", result.size(), is(currentItems.size()));

        Iterator<ChunkItem> iterator = result.iterator();

        ChunkItem item = iterator.next();
        assertThat("1st item status", item.getStatus(), is(ChunkItem.Status.IGNORE));
        assertThat("1st item diagnostics", item.getDiagnostics(), is(nullValue()));
        assertThat("1st item trackingId", item.getTrackingId(), is(DBC_TRACKING_ID + 0));

        item = iterator.next();
        assertThat("2nd item status", item.getStatus(), is(ChunkItem.Status.FAILURE));
        assertThat("2nd item diagnostics", item.getDiagnostics().size(), is(1));
        assertThat("2nd item trackingId", item.getTrackingId(), is(DBC_TRACKING_ID + 1));

        item = iterator.next();
        assertThat("3rd item status", item.getStatus(), is(ChunkItem.Status.IGNORE));
        assertThat("3rd item diagnostics", item.getDiagnostics(), is(nullValue()));
        assertThat("3rd item trackingId", item.getTrackingId(), is(DBC_TRACKING_ID + 2));

        item = iterator.next();
        assertThat("4th item status", item.getStatus(), is(ChunkItem.Status.SUCCESS));
        assertThat("4th item diagnostics", item.getDiagnostics(), is(nullValue()));
        assertThat("4th item trackingId", item.getTrackingId(), is(DBC_TRACKING_ID + 3));

        assertThat(iterator.hasNext(), is(false));
    }

    @Test
    void failOnAddiDiff() throws InvalidMessageException {
        List<ChunkItem> currentItems = Arrays.asList(
                ChunkItem.successfulChunkItem()
                        .withId(0L)
                        .withTrackingId(DBC_TRACKING_ID + 0)
                        .withData("9\nmetadata0\n8\ncontent0\n"),
                ChunkItem.successfulChunkItem()
                        .withId(1L)
                        .withTrackingId(DBC_TRACKING_ID + 1)
                        .withData("9\nmetadata1\n8\ncontent1\n"),
                ChunkItem.successfulChunkItem()
                        .withId(2L)
                        .withTrackingId(DBC_TRACKING_ID + 2)
                        .withData("9\nmetadata2\n8\ncontent2\n"));
        List<ChunkItem> nextItems = Arrays.asList(
                ChunkItem.successfulChunkItem()
                        .withId(0L)
                        .withTrackingId(DBC_TRACKING_ID + 0)
                        .withData("9\nmetadata0\n8\ncontent0\n"),
                ChunkItem.successfulChunkItem()
                        .withId(1L)
                        .withTrackingId(DBC_TRACKING_ID + 1)
                        .withData("9\nmetadataB\n8\ncontentB\n"),
                ChunkItem.successfulChunkItem()
                        .withId(2L)
                        .withTrackingId(DBC_TRACKING_ID + 2)
                        .withData("9\nmetadata2\n8\ncontent2\n"));

        Chunk result = newChunkDiffer().diff(JOB_ID, CHUNK_ID, currentItems, nextItems);
        assertThat("number of chunk items in result", result.size(), is(currentItems.size()));

        Iterator<ChunkItem> iterator = result.iterator();

        ChunkItem item = iterator.next();
        assertThat("1st item status", item.getStatus(), is(ChunkItem.Status.SUCCESS));
        assertThat("1st item diagnostics", item.getDiagnostics(), is(nullValue()));
        assertThat("1st item trackingId", item.getTrackingId(), is(DBC_TRACKING_ID + 0));

        item = iterator.next();
        assertThat("2nd item status", item.getStatus(), is(ChunkItem.Status.FAILURE));
        assertThat("2nd item diagnostics", item.getDiagnostics().size(), is(1));
        assertThat("2nd item trackingId", item.getTrackingId(), is(DBC_TRACKING_ID + 1));

        item = iterator.next();
        assertThat("3rd item status", item.getStatus(), is(ChunkItem.Status.SUCCESS));
        assertThat("3rd item diagnostics", item.getDiagnostics(), is(nullValue()));
        assertThat("3rd item trackingId", item.getTrackingId(), is(DBC_TRACKING_ID + 2));
    }

    @Test
    void failOnStatusDiff() throws InvalidMessageException {
        List<ChunkItem> currentItems = Arrays.asList(
                ChunkItem.failedChunkItem()
                        .withId(0L)
                        .withTrackingId(DBC_TRACKING_ID + 0)
                        .withData("data0"),
                ChunkItem.ignoredChunkItem()
                        .withId(1L)
                        .withTrackingId(DBC_TRACKING_ID + 1)
                        .withData("data1"));
        List<ChunkItem> nextItems = Arrays.asList(
                ChunkItem.successfulChunkItem()
                        .withId(0L)
                        .withTrackingId(DBC_TRACKING_ID + 0)
                        .withData("data0"),
                ChunkItem.successfulChunkItem()
                        .withId(1L)
                        .withTrackingId(DBC_TRACKING_ID + 1)
                        .withData("data1"));
        Chunk result = newChunkDiffer().diff(JOB_ID, CHUNK_ID, currentItems, nextItems);
        assertThat("number of chunk items in result", result.size(), is(currentItems.size()));

        Iterator<ChunkItem> iterator = result.iterator();

        ChunkItem item = iterator.next();
        assertThat("1st item status", item.getStatus(), is(ChunkItem.Status.FAILURE));
        assertThat("1st item diagnostics", item.getDiagnostics().size(), is(1));
        assertThat("1st item trackingId", item.getTrackingId(), is(DBC_TRACKING_ID + 0));

        item = iterator.next();
        assertThat("2nd item status", item.getStatus(), is(ChunkItem.Status.FAILURE));
        assertThat("2nd item diagnostics", item.getDiagnostics().size(), is(1));
        assertThat("2nd item trackingId", item.getTrackingId(), is(DBC_TRACKING_ID + 1));
        assertThat(iterator.hasNext(), is(false));
    }

    @Test
    void diffExpectedFailures() throws InvalidMessageException {
        ChunkItem currentItem = ChunkItem.failedChunkItem()
                .withId(0L)
                .withTrackingId(DBC_TRACKING_ID + 0)
                .withDiagnostics(new Diagnostic(Diagnostic.Level.FATAL, "expected failure")
                        .withTag(FailRecord.class.getName()));
        ChunkItem nextItem = ChunkItem.failedChunkItem()
                .withId(0L)
                .withDiagnostics(new Diagnostic(Diagnostic.Level.FATAL, "expected failure")
                        .withTag(FailRecord.class.getName()));
        Chunk result = newChunkDiffer().diff(JOB_ID, CHUNK_ID, Collections.singletonList(currentItem), Collections.singletonList(nextItem));
        assertThat("number of chunk items", result.size(), is(1));

        ChunkItem item = result.iterator().next();
        assertThat("status", item.getStatus(), is(ChunkItem.Status.SUCCESS));
        assertThat("diagnostics", item.getDiagnostics(), is(nullValue()));
        assertThat("trackingId", item.getTrackingId(), is(DBC_TRACKING_ID + 0));
    }

    private ChunkDiffer newChunkDiffer() {
        ExternalToolDiffGenerator externalToolDiffGenerator = newExternalToolDiffGenerator();
        return new ChunkDiffer(externalToolDiffGenerator, new AddiDiffGenerator(externalToolDiffGenerator));
    }
}
