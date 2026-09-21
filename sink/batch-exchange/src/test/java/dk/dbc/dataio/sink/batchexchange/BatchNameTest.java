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
    void fromStringRoundTrip() {
        BatchName batchName = BatchName.fromString(name(RECORD_KEY).toString());
        assertThat("sinkId", batchName.getSinkId(), is(SINK_ID));
        assertThat("recordKey", batchName.getRecordKey(), is(RECORD_KEY));
        assertThat("jobId", batchName.getJobId(), is(JOB_ID));
        assertThat("chunkId", batchName.getChunkId(), is(CHUNK_ID));
        assertThat("itemId", batchName.getItemId(), is(ITEM_ID));
    }

    @Test
    void recordKeyContainingHyphensSurvivesRoundTrip() {
        BatchName batchName = BatchName.fromString(name("870970:abc-def-ghi").toString());
        assertThat("recordKey", batchName.getRecordKey(), is("870970:abc-def-ghi"));
        assertThat("jobId", batchName.getJobId(), is(JOB_ID));
        assertThat("itemId", batchName.getItemId(), is(ITEM_ID));
    }

    /* The three trailing fields are found from the right, so a record key that looks like
       them must not be mistaken for them. */
    @Test
    void recordKeyLookingLikeTheTrailingIdsSurvivesRoundTrip() {
        BatchName batchName = BatchName.fromString(name("870970:1-2-3").toString());
        assertThat("recordKey", batchName.getRecordKey(), is("870970:1-2-3"));
        assertThat("jobId", batchName.getJobId(), is(JOB_ID));
        assertThat("chunkId", batchName.getChunkId(), is(CHUNK_ID));
        assertThat("itemId", batchName.getItemId(), is(ITEM_ID));
    }

    @Test
    void absentRecordKeyReadsBackAsNullRatherThanEmpty() {
        BatchName batchName = BatchName.fromString(name(null).toString());
        assertThat("name", batchName.toString(), is("15--4242-2424-7"));
        assertThat("recordKey", batchName.getRecordKey(), is(nullValue()));
        assertThat("jobId", batchName.getJobId(), is(JOB_ID));
    }

    @Test
    void fromMessageReadsTheHeaders() {
        BatchName batchName = BatchName.fromMessage(message(RECORD_KEY));
        assertThat("name", batchName, is(name(RECORD_KEY)));
    }

    @Test
    void fromMessageWithoutRecordKey_recordKeyIsNull() {
        BatchName batchName = BatchName.fromMessage(message(null));
        assertThat("recordKey", batchName.getRecordKey(), is(nullValue()));
    }

    @Test
    void fromMessageWithoutSinkId_throws() {
        Map<String, Object> headers = headers(RECORD_KEY);
        headers.remove(JMSHeader.sinkId.name);

        assertThrows(IllegalArgumentException.class,
                () -> BatchName.fromMessage(new ConsumedMessage("id", headers, "")));
    }

    @Test
    void fromStringWithTooFewFields_throws() {
        assertThrows(IllegalArgumentException.class, () -> BatchName.fromString("42-0"));
    }

    @Test
    void fromStringWithNonNumericIds_throws() {
        assertThrows(IllegalArgumentException.class, () -> BatchName.fromString("15-key-one-two-three"));
    }

    @Test
    void prefixIsSharedByEveryVersionOfTheRecord() {
        BatchName other = new BatchName(SINK_ID, RECORD_KEY, JOB_ID + 1, 0, (short) 0);
        assertThat("prefix", name(RECORD_KEY).prefix(), is("15-870970:12345678-"));
        assertThat("other name starts with it", other.toString().startsWith(name(RECORD_KEY).prefix()), is(true));
    }

    @Test
    void prefixOfAnAbsentRecordKeyIsStillAPrefix() {
        assertThat(name(null).prefix(), is("15--"));
    }

    /* A record key is opaque, so it can carry the LIKE wildcards itself. */
    @Test
    void likePrefixPatternEscapesTheWildcards() {
        assertThat(name("870970:100%_a\\b").likePrefixPattern(), is("15-870970:100\\%\\_a\\\\b-%"));
    }

    @Test
    void likePrefixPatternAppendsTheWildcard() {
        assertThat(name(RECORD_KEY).likePrefixPattern(), is("15-870970:12345678-%"));
    }

    @Test
    void hasSameRecordAs_recordKeyThatIsAnotherWithMoreAppended_isNotTheSameRecord() {
        BatchName shortKey = new BatchName(SINK_ID, "870970:123", JOB_ID, CHUNK_ID, ITEM_ID);
        BatchName longKey = new BatchName(SINK_ID, "870970:123-456", JOB_ID, CHUNK_ID, ITEM_ID);

        assertThat("prefix does match", longKey.toString().startsWith(shortKey.prefix()), is(true));
        assertThat("but the record does not", shortKey.hasSameRecordAs(longKey), is(false));
    }

    @Test
    void hasSameRecordAs_sameRecordOfAnotherSink_isNotTheSameRecord() {
        BatchName otherSink = new BatchName(SINK_ID + 1, RECORD_KEY, JOB_ID, CHUNK_ID, ITEM_ID);

        assertThat(name(RECORD_KEY).hasSameRecordAs(otherSink), is(false));
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
