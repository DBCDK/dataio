package dk.dbc.dataio.sink.es;

import dk.dbc.commons.addi.AddiRecord;
import dk.dbc.commons.testutil.Assert;
import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnectorException;
import dk.dbc.dataio.commons.types.Chunk;
import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.types.EsSinkConfig;
import dk.dbc.dataio.commons.utils.lang.StringUtil;
import dk.dbc.dataio.commons.utils.test.model.ChunkBuilder;
import dk.dbc.dataio.commons.utils.test.model.ChunkItemBuilder;
import dk.dbc.dataio.sink.es.entity.es.TaskSpecificUpdateEntity;
import dk.dbc.dataio.sink.types.SinkException;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;

import static dk.dbc.commons.testutil.Assert.isThrowing;
import static dk.dbc.dataio.commons.utils.lang.StringUtil.asString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

public class EsWorkloadTest {
    private final int userId = 42;
    private final TaskSpecificUpdateEntity.UpdateAction action = TaskSpecificUpdateEntity.UpdateAction.INSERT;
    private final AddiRecordPreprocessor addiRecordPreprocessor = new AddiRecordPreprocessor();
    private static final String ADDI_OK = "1\na\n1\nb\n";

    private final ChunkItem chunkItem = new ChunkItemBuilder().setId(0).setData(StringUtil.asBytes(ADDI_OK)).build();
    private final Chunk chunk = new ChunkBuilder(Chunk.Type.PROCESSED).setItems(Collections.singletonList(chunkItem)).build();
    private final String trackingId = "rr:1234io:5353";
    private final String processingTag = "dataio:sink-processing";
    private EsSinkConfig sinkConfig = new EsSinkConfig().withUserId(42).withDatabaseName("dbname");

    @Test
    public void constructor_chunkResultArgIsNull_throws() {
        Assert.assertThat(() -> new EsWorkload(null, new ArrayList<>(0), userId, action), isThrowing(NullPointerException.class));
    }

    @Test
    public void constructor_addiRecordsArgIsNull_throws() {
        Assert.assertThat(() -> new EsWorkload(new ChunkBuilder(Chunk.Type.DELIVERED).build(), null, userId, action), isThrowing(NullPointerException.class));
    }

    @Test
    public void constructor_actionArgIsNull_throws() {
        Assert.assertThat(() -> new EsWorkload(new ChunkBuilder(Chunk.Type.DELIVERED).build(), new ArrayList<>(0),
                userId, null), isThrowing(NullPointerException.class));
    }

    @Test
    public void constructor_allArgsAreValid_returnsNewInstance() {
        assertThat(new EsWorkload(new ChunkBuilder(Chunk.Type.DELIVERED).build(), new ArrayList<>(0),
                userId, action), is(notNullValue()));
    }

    @Test
    public void create_chunkResultArgIsNull_throws() throws SinkException {
        Assert.assertThat(() -> EsWorkload.create(null, sinkConfig, addiRecordPreprocessor), isThrowing(NullPointerException.class));
    }

    @Test
    public void create_sinkConfigArgIsNull_throws() throws SinkException {
        Assert.assertThat(() -> EsWorkload.create(chunk, null, addiRecordPreprocessor), isThrowing(NullPointerException.class));
    }

    @Test
    public void create_addiRecordPreprocessorArgIsNull_returnsEsWorkloadWithFailedItem() throws SinkException {
        Assert.assertThat(() -> EsWorkload.create(chunk, sinkConfig, null), isThrowing(NullPointerException.class));
    }

    @Test
    public void create_allArgsAreValid_returnsEsWorkload() throws SinkException, FlowStoreServiceConnectorException {
        final byte[] validAddiProcessingFalse = AddiRecordPreprocessorTest.getValidAddiWithProcessingFalse();
        final byte[] validAddiProcessingTrue = AddiRecordPreprocessorTest.getValidAddiWithProcessingTrueAndValidMarcXContentData();
        final byte[] validAddiWithoutProcessing = AddiRecordPreprocessorTest.getValidAddiWithoutProcessing();
        final byte[] validAddiProcessingTrueInvalidMarcX = AddiRecordPreprocessorTest.getValidAddiWithProcessingTrueAndInvalidMarcXContentData();

        final ArrayList<ChunkItem> chunkItems = new ArrayList<>();
        chunkItems.add(new ChunkItemBuilder()               // processed successfully
                .setId(0)
                .setData(validAddiWithoutProcessing)
                .setStatus(ChunkItem.Status.SUCCESS)
                .setTrackingId(trackingId)
                .build());
        chunkItems.add(new ChunkItemBuilder()               // ignored by processor
                .setId(1)
                .setStatus(ChunkItem.Status.IGNORE)
                .setTrackingId(trackingId)
                .build());
        chunkItems.add(new ChunkItemBuilder()               // failed by processor
                .setId(2)
                .setStatus(ChunkItem.Status.FAILURE)
                .setTrackingId(trackingId)
                .build());
        chunkItems.add(new ChunkItemBuilder()               // processor produces invalid addi
                .setId(3)
                .setData(StringUtil.asBytes("invalid"))
                .setStatus(ChunkItem.Status.SUCCESS)
                .setTrackingId(trackingId)
                .build());
        chunkItems.add(new ChunkItemBuilder()               // processed successfully
                .setId(4)
                .setData(validAddiWithoutProcessing)
                .setStatus(ChunkItem.Status.SUCCESS)
                .build());
        chunkItems.add(new ChunkItemBuilder()               // processor produces empty addi
                .setId(5)
                .setData(StringUtil.asBytes(""))
                .setStatus(ChunkItem.Status.SUCCESS)
                .setTrackingId(trackingId)
                .build());
        chunkItems.add(new ChunkItemBuilder()               // sink processing removed from meta data and processed successfully
                .setId(6)
                .setData(validAddiProcessingFalse)
                .setStatus(ChunkItem.Status.SUCCESS)
                .setTrackingId(trackingId)
                .build());
        chunkItems.add(new ChunkItemBuilder()               //sink processing removed from meta data, content data converted to iso2709 and processed successfully
                .setId(7)
                .setData(validAddiProcessingTrue)
                .setStatus(ChunkItem.Status.SUCCESS)
                .build());
        chunkItems.add(new ChunkItemBuilder()
                .setId(8)
                .setData(validAddiProcessingTrueInvalidMarcX)
                .setStatus(ChunkItem.Status.SUCCESS)
                .build());

        final Chunk processedChunk = new ChunkBuilder(Chunk.Type.PROCESSED)
                .setItems(chunkItems)
                .build();

        final EsWorkload esWorkloadFromChunkResult = EsWorkload.create(processedChunk, sinkConfig, addiRecordPreprocessor);

        assertThat(esWorkloadFromChunkResult, is(notNullValue()));
        assertThat(esWorkloadFromChunkResult.getAddiRecords().size(), is(4));
        assertThat(esWorkloadFromChunkResult.getDeliveredChunk().size(), is(9));
        Iterator<ChunkItem> iterator = esWorkloadFromChunkResult.getDeliveredChunk().iterator();

        assertThat(iterator.hasNext(), is(true));
        ChunkItem item0 = iterator.next();
        assertThat("chunkItem 0 ID", item0.getId(), is(0L));
        assertThat("chunkItem 0 status", item0.getStatus(), is(ChunkItem.Status.SUCCESS));
        assertThat("chunkItem 0 data", asString(item0.getData()), is("1"));
        assertThat("chunkItem 0 diagnostics", item0.getDiagnostics(), is(nullValue()));
        assertThat("chunkItem 0 trackingId", item0.getTrackingId(), is(trackingId));

        assertThat(iterator.hasNext(), is(true));
        ChunkItem item1 = iterator.next();
        assertThat("chunkItem 1 ID", item1.getId(), is(1L));
        assertThat("chunkItem 1 status", item1.getStatus(), is(ChunkItem.Status.IGNORE));
        assertThat("chunkItem 1 diagnostics", item1.getDiagnostics(), is(nullValue()));
        assertThat("chunkItem 1 trackingId", item1.getTrackingId(), is(trackingId));

        assertThat(iterator.hasNext(), is(true));
        ChunkItem item2 = iterator.next();
        assertThat("chunkItem 2 ID", item2.getId(), is(2L));
        assertThat("chunkItem 2 status", item2.getStatus(), is(ChunkItem.Status.IGNORE));
        assertThat("chunkItem 2 diagnostics", item2.getDiagnostics(), is(nullValue()));
        assertThat("chunkItem 2 trackingId", item2.getTrackingId(), is(trackingId));

        assertThat(iterator.hasNext(), is(true));
        ChunkItem item3 = iterator.next();
        assertThat("chunkItem 3 ID", item3.getId(), is(3L));
        assertThat("chunkItem 3 status", item3.getStatus(), is(ChunkItem.Status.FAILURE));
        assertThat("chunkItem 3 diagnostics", item3.getDiagnostics().size(), is(1));
        assertThat("chunkItem 3 diagnostics.stacktrace", item3.getDiagnostics().get(0).getStacktrace(), is(notNullValue()));
        assertThat("chunkItem 3 trackingId", item3.getTrackingId(), is(trackingId));

        assertThat(iterator.hasNext(), is(true));
        ChunkItem item4 = iterator.next();
        assertThat("chunkItem 4 ID", item4.getId(), is(4L));
        assertThat("chunkItem 4 status", item4.getStatus(), is(ChunkItem.Status.SUCCESS));
        assertThat("chunkItem 4 data", asString(item4.getData()), is("1"));
        assertThat("chunkItem 4 diagnostics", item4.getDiagnostics(), is(nullValue()));
        assertThat("chunkItem 4 trackingId", item4.getTrackingId(), is(nullValue()));

        assertThat(iterator.hasNext(), is(true));
        ChunkItem item5 = iterator.next();
        assertThat("chunkItem 5 ID", item5.getId(), is(5L));
        assertThat("chunkItem 5 status", item5.getStatus(), is(ChunkItem.Status.FAILURE));
        assertThat("chunkItem 5 diagnostics", item5.getDiagnostics().size(), is(1));
        assertThat("chunkItem 5 diagnostics.stacktrace", item5.getDiagnostics().get(0).getStacktrace(), is(notNullValue()));
        assertThat("chunkItem 5 trackingId", item5.getTrackingId(), is(trackingId));

        assertThat(iterator.hasNext(), is(true));
        ChunkItem item6 = iterator.next();
        assertThat("chunkItem 6 ID", item6.getId(), is(6L));
        assertThat("chunkItem 6 status", item6.getStatus(), is(ChunkItem.Status.SUCCESS));
        assertThat("chunkItem 6 diagnostics", item6.getDiagnostics(), is(nullValue()));
        assertThat("chunkItem 6 data", asString(item6.getData()), is("1"));
        assertThat("chunkItem 6 trackingId", item6.getTrackingId(), is(trackingId));

        assertThat(iterator.hasNext(), is(true));
        ChunkItem item7 = iterator.next();
        assertThat("chunkItem 7 ID", item7.getId(), is(7L));
        assertThat("chunkItem 7 status", item7.getStatus(), is(ChunkItem.Status.SUCCESS));
        assertThat("chunkItem 7 diagnostics", item7.getDiagnostics(), is(nullValue()));
        assertThat("chunkItem 7 data", asString(item7.getData()), is("1"));
        assertThat("chunkItem 7 trackingId", item7.getTrackingId(), is(nullValue()));

        assertThat(iterator.hasNext(), is(true));
        ChunkItem item8 = iterator.next();
        assertThat("chunkItem 8 ID", item8.getId(), is(8L));
        assertThat("chunkItem 8 status", item8.getStatus(), is(ChunkItem.Status.FAILURE));
        assertThat("chunkItem 8 diagnostics", item8.getDiagnostics().size(), is(1));
        assertThat("chunkItem 8 diagnostics.stacktrace", item8.getDiagnostics().get(0).getStacktrace(), is(notNullValue()));
        assertThat("chunkItem 8 trackingId", item8.getTrackingId(), is(nullValue()));

        assertThat(iterator.hasNext(), is(false));

        // assert that the processing tag has been removed from the meta data for the 2 items
        // that successfully have been processed.
        AddiRecord addiRecord3 = esWorkloadFromChunkResult.getAddiRecords().get(2);
        String addiRecord3Metadata = StringUtil.asString(addiRecord3.getMetaData());
        assertThat("processing tag removed", addiRecord3Metadata.contains(processingTag), is(false));
        // assert that the tracking id attribute has been added.
        assertThat("tracking id added", addiRecord3Metadata.contains(trackingId), is(true));

        AddiRecord addiRecord4 = esWorkloadFromChunkResult.getAddiRecords().get(3);
        String addiRecord4Metadata = StringUtil.asString(addiRecord4.getMetaData());
        assertThat("processing tag removed", addiRecord4Metadata.contains(processingTag), is(false));
    }
}
