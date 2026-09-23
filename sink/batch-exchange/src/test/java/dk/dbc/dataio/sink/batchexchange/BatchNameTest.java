package dk.dbc.dataio.sink.batchexchange;

import dk.dbc.dataio.commons.types.ConsumedMessage;
import dk.dbc.dataio.commons.types.jms.JMSHeader;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BatchNameTest {
    private static final long SINK_ID = 15;
    private static final String RECORD_KEY = "870970:12345678";
    private static final int JOB_ID = 4242;
    private static final long CHUNK_ID = 2424;
    private static final short ITEM_ID = 7;

    @Test
    void toStringNamesEveryField() {
        assertThat(name(RECORD_KEY).toString(), is("15-870970:12345678-4242-2424-7"));
    }

    @Test
    void toStringLeavesAnAbsentRecordKeyEmpty() {
        assertThat(name(null).toString(), is("15--4242-2424-7"));
    }

    /* The name is written for operators and read by nothing, so a record key spelled like the
       other fields needs no special treatment. */
    @Test
    void toStringPassesAwkwardRecordKeysThrough() {
        assertThat(name("870970:1-2-3").toString(), is("15-870970:1-2-3-4242-2424-7"));
        assertThat(name("870970:100_30%").toString(), is("15-870970:100_30%-4242-2424-7"));
    }

    @Test
    void fromMessageReadsEveryField() {
        BatchName batchName = BatchName.fromMessage(message(RECORD_KEY));
        assertThat("sinkId", batchName.getSinkId(), is(SINK_ID));
        assertThat("recordKey", batchName.getRecordKey(), is(RECORD_KEY));
        assertThat("jobId", batchName.getJobId(), is(JOB_ID));
        assertThat("chunkId", batchName.getChunkId(), is(CHUNK_ID));
        assertThat("itemId", batchName.getItemId(), is(ITEM_ID));
    }

    @Test
    void fromMessageAcceptsAMessageWithoutARecordKey() {
        assertThat(BatchName.fromMessage(message(null)).getRecordKey(), is(nullValue()));
    }

    @Test
    void fromMessageRejectsAMessageMissingAnIdentifyingHeader() {
        Map<String, Object> headers = headers(RECORD_KEY);
        headers.remove(JMSHeader.itemId.name);
        ConsumedMessage message = new ConsumedMessage("id", headers, "");

        assertThrows(IllegalArgumentException.class, () -> BatchName.fromMessage(message));
    }

    @Test
    void isSameItemAsIgnoresTheRecordKey() {
        BatchName item = name(RECORD_KEY);

        assertThat("same ids, other record key", item.isSameItemAs(name("870970:87654321")), is(true));
        assertThat("same ids, no record key", item.isSameItemAs(name(null)), is(true));
        assertThat("other item of the same chunk", item.isSameItemAs(
                new BatchName(SINK_ID, RECORD_KEY, JOB_ID, CHUNK_ID, (short) (ITEM_ID + 1))), is(false));
        assertThat("other chunk of the same job", item.isSameItemAs(
                new BatchName(SINK_ID, RECORD_KEY, JOB_ID, CHUNK_ID + 1, ITEM_ID)), is(false));
        assertThat("other job", item.isSameItemAs(
                new BatchName(SINK_ID, RECORD_KEY, JOB_ID + 1, CHUNK_ID, ITEM_ID)), is(false));
    }

    @Test
    void isOlderThan_ordersOnJobThenChunkThenItem() {
        BatchName item = name(RECORD_KEY);

        assertThat("later job", item.isOlderThan(
                new BatchName(SINK_ID, RECORD_KEY, JOB_ID + 1, 0, (short) 0)), is(true));
        assertThat("earlier job", item.isOlderThan(
                new BatchName(SINK_ID, RECORD_KEY, JOB_ID - 1, Long.MAX_VALUE, Short.MAX_VALUE)), is(false));
        assertThat("later chunk of the same job", item.isOlderThan(
                new BatchName(SINK_ID, RECORD_KEY, JOB_ID, CHUNK_ID + 1, (short) 0)), is(true));
        assertThat("later item of the same chunk", item.isOlderThan(
                new BatchName(SINK_ID, RECORD_KEY, JOB_ID, CHUNK_ID, (short) (ITEM_ID + 1))), is(true));
        assertThat("itself", item.isOlderThan(name(RECORD_KEY)), is(false));
    }

    @Test
    void asTrackingIdNamesTheItemAlone() {
        assertThat(name(RECORD_KEY).asTrackingId(), is("io:4242-2424-7"));
    }

    @Test
    void asItemReferenceNamesTheItemAlone() {
        assertThat(name(RECORD_KEY).asItemReference(), is("4242/2424/7"));
    }

    private BatchName name(String recordKey) {
        return new BatchName(SINK_ID, recordKey, JOB_ID, CHUNK_ID, ITEM_ID);
    }

    private ConsumedMessage message(String recordKey) {
        return new ConsumedMessage("id", headers(recordKey), "");
    }

    private Map<String, Object> headers(String recordKey) {
        Map<String, Object> headers = new HashMap<>();
        headers.put(JMSHeader.sinkId.name, SINK_ID);
        headers.put(JMSHeader.jobId.name, JOB_ID);
        headers.put(JMSHeader.chunkId.name, CHUNK_ID);
        headers.put(JMSHeader.itemId.name, ITEM_ID);
        if (recordKey != null) {
            headers.put(JMSHeader.recordKey.name, recordKey);
        }
        return headers;
    }
}
