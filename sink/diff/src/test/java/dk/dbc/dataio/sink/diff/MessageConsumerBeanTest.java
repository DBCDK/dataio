package dk.dbc.dataio.sink.diff;

import dk.dbc.dataio.commons.types.Chunk;
import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.types.ConsumedMessage;
import dk.dbc.dataio.commons.types.Diagnostic;
import dk.dbc.dataio.commons.types.exceptions.InvalidMessageException;
import dk.dbc.dataio.commons.types.exceptions.ServiceException;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnector;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnectorException;
import dk.dbc.dataio.commons.utils.jobstore.ejb.JobStoreServiceConnectorBean;
import dk.dbc.dataio.commons.utils.test.model.ChunkBuilder;
import dk.dbc.dataio.sink.testutil.ObjectFactory;
import dk.dbc.dataio.sink.types.SinkException;
import dk.dbc.javascript.recordprocessing.FailRecord;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class MessageConsumerBeanTest extends AbstractDiffGeneratorTest {
    private final static String DBC_TRACKING_ID = "dataio_";

    private final JobStoreServiceConnectorBean jobStoreServiceConnectorBean = mock(JobStoreServiceConnectorBean.class);
    private final JobStoreServiceConnector jobStoreServiceConnector = mock(JobStoreServiceConnector.class);

    @Before
    public void setupMocks() {
        when(jobStoreServiceConnectorBean.getConnector()).thenReturn(jobStoreServiceConnector);
    }

    @Test
    public void sendsResultToJobStore() throws ServiceException, InvalidMessageException, JobStoreServiceConnectorException {
        final ConsumedMessage message = ObjectFactory.createConsumedMessage(new ChunkBuilder(Chunk.Type.PROCESSED).build());
        newMessageConsumerBean().handleConsumedMessage(message);

        verify(jobStoreServiceConnector).addChunkIgnoreDuplicates(any(Chunk.class), anyInt(), anyLong());
    }

    @Test
    public void failOnMissingNextItems() throws SinkException {
        final List<ChunkItem> chunkItems = Arrays.asList(
                ChunkItem.failedChunkItem()
                        .withId(0L)
                        .withTrackingId(DBC_TRACKING_ID + 0),
                ChunkItem.successfulChunkItem()
                        .withId(1L)
                        .withTrackingId(DBC_TRACKING_ID + 1),
                ChunkItem.ignoredChunkItem()
                        .withId(2L)
                        .withTrackingId(DBC_TRACKING_ID + 2));
        final Chunk chunk = new ChunkBuilder(Chunk.Type.PROCESSED)
                .setItems(chunkItems)
                .build();

        final Chunk result = newMessageConsumerBean().handleChunk(chunk);
        assertThat("number of chunk items in result", result.size(), is(chunkItems.size()));

        final Iterator<ChunkItem> iterator = result.iterator();

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
    public void failOnXmlDiff() throws SinkException {
        final List<ChunkItem> currentItems = Arrays.asList(
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
        final List<ChunkItem> nextItems = Arrays.asList(
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
        final Chunk chunk = new ChunkBuilder(Chunk.Type.PROCESSED)
                .setItems(currentItems)
                .setNextItems(nextItems)
                .build();

        final Chunk result = newMessageConsumerBean().handleChunk(chunk);
        assertThat("number of chunk items in result", result.size(), is(currentItems.size()));

        final Iterator<ChunkItem> iterator = result.iterator();

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
    public void failOnAddiDiff() throws SinkException {
        final List<ChunkItem> currentItems = Arrays.asList(
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
        final List<ChunkItem> nextItems = Arrays.asList(
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

        final Chunk chunk = new ChunkBuilder(Chunk.Type.PROCESSED)
                .setItems(currentItems)
                .setNextItems(nextItems)
                .build();

        final Chunk result = newMessageConsumerBean().handleChunk(chunk);
        assertThat("number of chunk items in result", result.size(), is(currentItems.size()));

        final Iterator<ChunkItem> iterator = result.iterator();

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
    public void failOnStatusDiff() throws SinkException {
        final List<ChunkItem> currentItems = Arrays.asList(
                ChunkItem.failedChunkItem()
                        .withId(0L)
                        .withTrackingId(DBC_TRACKING_ID + 0)
                        .withData("data0"),
                ChunkItem.ignoredChunkItem()
                        .withId(1L)
                        .withTrackingId(DBC_TRACKING_ID + 1)
                        .withData("data1"));
        final List<ChunkItem> nextItems = Arrays.asList(
                ChunkItem.successfulChunkItem()
                        .withId(0L)
                        .withTrackingId(DBC_TRACKING_ID + 0)
                        .withData("data0"),
                ChunkItem.successfulChunkItem()
                        .withId(1L)
                        .withTrackingId(DBC_TRACKING_ID + 1)
                        .withData("data1"));
        final Chunk chunk = new ChunkBuilder(Chunk.Type.PROCESSED)
                .setItems(currentItems)
                .setNextItems(nextItems)
                .build();

        final Chunk result = newMessageConsumerBean().handleChunk(chunk);
        assertThat("number of chunk items in result", result.size(), is(currentItems.size()));

        final Iterator<ChunkItem> iterator = result.iterator();

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
    public void diffExpectedFailures() throws SinkException {
        final ChunkItem currentItem = ChunkItem.failedChunkItem()
                .withId(0L)
                .withTrackingId(DBC_TRACKING_ID + 0)
                .withDiagnostics(new Diagnostic(Diagnostic.Level.FATAL, "expected failure")
                        .withTag(FailRecord.class.getName()));
        final ChunkItem nextItem = ChunkItem.failedChunkItem()
                .withId(0L)
                .withDiagnostics(new Diagnostic(Diagnostic.Level.FATAL, "expected failure")
                        .withTag(FailRecord.class.getName()));
        final Chunk chunk = new ChunkBuilder(Chunk.Type.PROCESSED)
                .setItems(Collections.singletonList(currentItem))
                .setNextItems(Collections.singletonList(nextItem))
                .build();

        final Chunk result = newMessageConsumerBean().handleChunk(chunk);
        assertThat("number of chunk items", result.size(), is(1));

        final ChunkItem item = result.iterator().next();
        assertThat("status", item.getStatus(), is(ChunkItem.Status.SUCCESS));
        assertThat("diagnostics", item.getDiagnostics(), is(nullValue()));
        assertThat("trackingId", item.getTrackingId(), is(DBC_TRACKING_ID + 0));
    }

    private MessageConsumerBean newMessageConsumerBean() {
        final MessageConsumerBean messageConsumerBean = new MessageConsumerBean();
        messageConsumerBean.externalToolDiffGenerator = newExternalToolDiffGenerator();
        messageConsumerBean.addiDiffGenerator =
                new AddiDiffGenerator(messageConsumerBean.externalToolDiffGenerator);
        messageConsumerBean.jobStoreServiceConnectorBean = jobStoreServiceConnectorBean;
        return messageConsumerBean;
    }
}
