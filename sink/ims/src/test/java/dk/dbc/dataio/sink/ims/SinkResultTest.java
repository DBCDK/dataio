package dk.dbc.dataio.sink.ims;

import dk.dbc.dataio.commons.types.Chunk;
import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.utils.lang.StringUtil;
import dk.dbc.dataio.commons.utils.test.model.ChunkBuilder;
import dk.dbc.oss.ns.updatemarcxchange.UpdateMarcXchangeResult;
import dk.dbc.oss.ns.updatemarcxchange.UpdateMarcXchangeStatusEnum;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import static dk.dbc.commons.testutil.Assert.assertThat;
import static dk.dbc.commons.testutil.Assert.isThrowing;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

public class SinkResultTest {

    private final String collection =
            "<marcx:collection xmlns:marcx=\"info:lc/xmlns/marcxchange-v1\">" +
                    "<marcx:record format=\"danMARC2\"><marcx:datafield ind1=\"0\" ind2=\"0\" tag=\"245\">" +
                    "<marcx:subfield code=\"a\">title1</marcx:subfield></marcx:datafield>" +
                    "</marcx:record>" +
                    "</marcx:collection>";

    private ChunkBuilder chunkBuilder;

    private final MarcXchangeRecordUnmarshaller unmarshaller = new MarcXchangeRecordUnmarshaller();
    private final ChunkItem successfulItem1 = new ChunkItem().withData(collection).withStatus(ChunkItem.Status.SUCCESS);
    private final ChunkItem successfulItem2 = new ChunkItem().withData(collection).withStatus(ChunkItem.Status.SUCCESS);
    private final ChunkItem failedItem = new ChunkItem().withStatus(ChunkItem.Status.FAILURE);
    private final ChunkItem ignoredItem = new ChunkItem().withStatus(ChunkItem.Status.IGNORE);
    private final ChunkItem invalidDataItem = new ChunkItem().withData("invalid").withStatus(ChunkItem.Status.SUCCESS);

    @Before
    public void setup() {
        chunkBuilder = new ChunkBuilder(Chunk.Type.PROCESSED).setItems(new ArrayList<>());
    }

    @Test
    public void constructor_chunkArgIsNull_throws() {
        assertThat(()-> new SinkResult(null, unmarshaller), isThrowing(NullPointerException.class));
    }

    @Test
    public void constructor_unmarshallerArgIsNull_throws() {
        assertThat(()-> new SinkResult(chunkBuilder.appendItem(successfulItem1.withId(0)).build(), null), isThrowing(NullPointerException.class));
    }

    @Test
    public void constructor_allArgsAreValid_returns() {
        final Chunk chunk = chunkBuilder.setItems(Arrays.asList(successfulItem1.withId(0), failedItem.withId(1), ignoredItem.withId(2), invalidDataItem.withId(3))).build();

        // Subject under test
        SinkResult sinkResult = new SinkResult(chunk, unmarshaller);

        // Verification
        assertThat(sinkResult.getMarcXchangeRecords().size(), is(1));
        assertThat(sinkResult.getMarcXchangeRecords().get(0).getMarcXchangeRecordId(), is(String.valueOf(successfulItem1.getId())));

        assertThat(sinkResult.chunkItems.length, is(4));
        assertThat(sinkResult.chunkItems[0], is(nullValue()));
        assertThat(sinkResult.chunkItems[1].getStatus(), is(ChunkItem.Status.IGNORE));
        assertThat(sinkResult.chunkItems[2].getStatus(), is(ChunkItem.Status.IGNORE));
        final ChunkItem chunkItem3 = sinkResult.chunkItems[3];
        assertThat(chunkItem3.getStatus(), is(ChunkItem.Status.FAILURE));
        assertThat(chunkItem3.getDiagnostics().size(), is(1));
    }

    @Test
    public void toChunk_itemListContainsNull_throws() {
        final Chunk chunk = chunkBuilder.appendItem(successfulItem1).build();
        assertThat(() -> new SinkResult(chunk, unmarshaller).toChunk(), isThrowing(IllegalArgumentException.class));
    }

    @Test
    public void toChunk_itemListDoesNotContainNull_returns() {
        final Chunk chunk = chunkBuilder.appendItem(ignoredItem).build();
        SinkResult sinkResult = new SinkResult(chunk, unmarshaller);

        // Subject under test
        Chunk result = sinkResult.toChunk();

        // Verification
        assertThat(result.getType(), is(Chunk.Type.DELIVERED));
        assertThat(result.getItems().size(), is(1));
    }

    @Test
    public void update_updateMarcXchangeResultRecordIdIsNull_insertsFailedItem() {
        SinkResult sinkResult = new SinkResult(chunkBuilder.appendItem(successfulItem1).build(), unmarshaller);

        final UpdateMarcXchangeResult updateMarcXchangeResult = new UpdateMarcXchangeResult();
        updateMarcXchangeResult.setUpdateMarcXchangeStatus(UpdateMarcXchangeStatusEnum.UPDATE_FAILED_INVALID_RECORD);
        updateMarcXchangeResult.setUpdateMarcXchangeMessage("message");

        // Subject under test
        sinkResult.update(Collections.singletonList(updateMarcXchangeResult));

        // Verification
        ChunkItem chunkItem0 = sinkResult.chunkItems[0];
        assertThat(StringUtil.asString(chunkItem0.getData()).contains(UpdateMarcXchangeStatusEnum.UPDATE_FAILED_INVALID_RECORD.value()), is(true));
        assertThat(chunkItem0.getStatus(), is(ChunkItem.Status.FAILURE));
        assertThat(chunkItem0.getDiagnostics().size(), is(1));
    }

    @Test
    public void update_unexpectedNumberOfUpdateMarcXchangeResults_insertsFailedItems() {
        SinkResult sinkResult = new SinkResult(chunkBuilder.setItems(Arrays.asList(successfulItem1.withId(0), successfulItem2.withId(1), ignoredItem.withId(2))).build(), unmarshaller);

        final UpdateMarcXchangeResult updateMarcXchangeResult = new UpdateMarcXchangeResult();
        updateMarcXchangeResult.setMarcXchangeRecordId("0");
        updateMarcXchangeResult.setUpdateMarcXchangeStatus(UpdateMarcXchangeStatusEnum.UPDATE_FAILED_PLEASE_RESEND_LATER);
        updateMarcXchangeResult.setUpdateMarcXchangeMessage("message");

        // Subject under test
        sinkResult.update(Collections.singletonList(updateMarcXchangeResult));

        // Verification
        final ChunkItem chunkItem0 = sinkResult.chunkItems[0];
        assertThat(StringUtil.asString(chunkItem0.getData()).contains(UpdateMarcXchangeStatusEnum.UPDATE_FAILED_PLEASE_RESEND_LATER.value()), is(false));
        assertThat(chunkItem0.getStatus(), is(ChunkItem.Status.FAILURE));
        assertThat(chunkItem0.getDiagnostics().size(), is(1));

        final ChunkItem chunkItem1 = sinkResult.chunkItems[1];
        assertThat(StringUtil.asString(chunkItem1.getData()).contains(UpdateMarcXchangeStatusEnum.UPDATE_FAILED_PLEASE_RESEND_LATER.value()), is(false));
        assertThat(chunkItem1.getStatus(), is(ChunkItem.Status.FAILURE));
        assertThat(chunkItem1.getDiagnostics().size(), is(1));

        // third item not overwritten
        final ChunkItem chunkItem2 = sinkResult.chunkItems[2];
        assertThat(chunkItem2.getStatus(), is(ChunkItem.Status.IGNORE));
    }

    @Test
    public void update_expectedNumberOfUpdateMarcXchangeResults_insertsItems() {
        SinkResult sinkResult = new SinkResult(chunkBuilder.setItems(Arrays.asList(successfulItem1.withId(0), successfulItem2.withId(1))).build(), unmarshaller);

        final UpdateMarcXchangeResult updateMarcXchangeResult1 = new UpdateMarcXchangeResult();
        updateMarcXchangeResult1.setMarcXchangeRecordId("0");
        updateMarcXchangeResult1.setUpdateMarcXchangeStatus(UpdateMarcXchangeStatusEnum.UPDATE_FAILED_PLEASE_RESEND_LATER);

        final UpdateMarcXchangeResult updateMarcXchangeResult2 = new UpdateMarcXchangeResult();
        updateMarcXchangeResult2.setMarcXchangeRecordId("1");
        updateMarcXchangeResult2.setUpdateMarcXchangeStatus(UpdateMarcXchangeStatusEnum.OK);

        // Subject under test
        sinkResult.update(Arrays.asList(updateMarcXchangeResult1, updateMarcXchangeResult2));

        // Verification
        final ChunkItem chunkItem0 = sinkResult.chunkItems[0];
        assertThat(StringUtil.asString(chunkItem0.getData()).contains(UpdateMarcXchangeStatusEnum.UPDATE_FAILED_PLEASE_RESEND_LATER.value()), is(true));
        assertThat(chunkItem0.getStatus(), is(ChunkItem.Status.FAILURE));
        assertThat(chunkItem0.getDiagnostics().size(), is(1));

        final ChunkItem chunkItem1 = sinkResult.chunkItems[1];
        assertThat(StringUtil.asString(chunkItem1.getData()).contains(UpdateMarcXchangeStatusEnum.OK.value()), is(true));
        assertThat(chunkItem1.getStatus(), is(ChunkItem.Status.SUCCESS));
    }
}
