package dk.dbc.dataio.jobstore.types;

import dk.dbc.commons.jsonb.JSONBContext;
import dk.dbc.commons.jsonb.JSONBException;
import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.jobstore.types.ItemDeliveryResult.Status;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;

class ItemDeliveryResultTest {
    private final JSONBContext jsonbContext = new JSONBContext();
    private final ChunkItem chunkItem = new ChunkItem()
            .withId(3)
            .withStatus(ChunkItem.Status.SUCCESS)
            .withType(ChunkItem.Type.STRING)
            .withData("delivered");

    @Test
    void of_leavesTheWatermarkRowIdentityForTheFramework() {
        ItemDeliveryResult result = ItemDeliveryResult.of(Status.DELIVERED, chunkItem);

        assertThat("sinkId", result.sinkId(), is(0L));
        assertThat("recordKey", result.recordKey(), is(nullValue()));
        assertThat("status", result.status(), is(Status.DELIVERED));
        assertThat("chunkItem", result.chunkItem(), is(chunkItem));
    }

    @Test
    void withWatermarkKey_replacesTheWatermarkRowIdentityOnly() {
        ItemDeliveryResult result = ItemDeliveryResult.of(Status.DELIVERED, chunkItem).withWatermarkKey(42, "870970:123");

        assertThat(result, is(new ItemDeliveryResult(42, "870970:123", Status.DELIVERED, chunkItem)));
    }

    @Test
    void withWatermarkKey_nullRecordKeyIsCarried() {
        ItemDeliveryResult result = ItemDeliveryResult.of(Status.SUPERSEDED, chunkItem).withWatermarkKey(42, null);

        assertThat("recordKey", result.recordKey(), is(nullValue()));
    }

    /**
     * The delivery endpoint unmarshalls this record from the request body, so the static
     * factory must stay invisible to the JSON binding, unlike a second constructor which
     * would become a competing creator candidate.
     */
    @Test
    void marshalling() throws JSONBException {
        ItemDeliveryResult result = ItemDeliveryResult.of(Status.FAILED, chunkItem).withWatermarkKey(42, "870970:123");

        assertThat(jsonbContext.unmarshall(jsonbContext.marshall(result), ItemDeliveryResult.class), is(result));
    }

    /**
     * IGNORED travels over the delivery endpoint like any other verdict, and a sink
     * reporting it names the watermark row it declines to advance, exactly as a DELIVERED
     * one names the row it does advance.
     */
    @Test
    void marshalling_ignored() throws JSONBException {
        ItemDeliveryResult result = ItemDeliveryResult.of(Status.IGNORED, chunkItem).withWatermarkKey(42, "870970:123");

        assertThat(jsonbContext.unmarshall(jsonbContext.marshall(result), ItemDeliveryResult.class), is(result));
    }
}
