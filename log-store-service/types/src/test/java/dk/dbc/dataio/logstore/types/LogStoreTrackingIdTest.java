package dk.dbc.dataio.logstore.types;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

public class LogStoreTrackingIdTest {
    private static final String JOB_ID = "jobId";
    private static final long CHUNK_ID = 1;
    private static final long ITEM_ID = 2;
    private static final String TRACKING_ID_FORMAT = "%s-%s-%s";
    private static final String TRACKING_ID = String.format(TRACKING_ID_FORMAT, JOB_ID, CHUNK_ID, ITEM_ID);

    @Test(expected = NullPointerException.class)
    public void constructor_trackingIdArgIsNull_throws() {
        new LogStoreTrackingId(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructor_trackingIdArgIsEmpty_throws() {
        new LogStoreTrackingId("");
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructor_trackingIdArgIsInvalid_throws() {
        new LogStoreTrackingId("invalid");
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructor_trackingIdArgJobIdIsInvalid_throws() {
        new LogStoreTrackingId(String.format(TRACKING_ID_FORMAT, "", CHUNK_ID, ITEM_ID));
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructor_trackingIdArgChunkIdIsInvalid_throws() {
        new LogStoreTrackingId(String.format(TRACKING_ID_FORMAT, JOB_ID, "invalid", ITEM_ID));
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructor_trackingIdArgItemIdIsInvalid_throws() {
        new LogStoreTrackingId(String.format(TRACKING_ID_FORMAT, JOB_ID, CHUNK_ID, "invalid"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructor_trackingIdArgChunkIdIsEmpty_throws() {
        new LogStoreTrackingId(String.format(TRACKING_ID_FORMAT, JOB_ID, "", ITEM_ID));
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructor_trackingIdArgItemIdIsEmpty_throws() {
        new LogStoreTrackingId(String.format(TRACKING_ID_FORMAT, JOB_ID, CHUNK_ID, ""));
    }

    @Test
    public void constructor_trackingIdArgIsValid_returnsNewInstance() {
        final LogStoreTrackingId logStoreTrackingId = new LogStoreTrackingId(TRACKING_ID);
        assertThat(logStoreTrackingId, is(notNullValue()));
        assertThat(logStoreTrackingId.getTrackingId(), is(TRACKING_ID));
        assertThat(logStoreTrackingId.getJobId(), is(JOB_ID));
        assertThat(logStoreTrackingId.getChunkId(), is(CHUNK_ID));
        assertThat(logStoreTrackingId.getItemId(), is(ITEM_ID));
    }

    @Test(expected = NullPointerException.class)
    public void create_jobIdArgIsNull_throws() {
        LogStoreTrackingId.create(null, CHUNK_ID, ITEM_ID);
    }

    @Test(expected = IllegalArgumentException.class)
    public void create_jobIdArgIsEmpty_throws() {
        LogStoreTrackingId.create("", CHUNK_ID, ITEM_ID);
    }

    @Test
    public void create_jobIdArgIsValid_returnsNewInstance() {
        final LogStoreTrackingId logStoreTrackingId = LogStoreTrackingId.create(JOB_ID, CHUNK_ID, ITEM_ID);
        assertThat(logStoreTrackingId, is(notNullValue()));
        assertThat(logStoreTrackingId.getTrackingId(), is(TRACKING_ID));
        assertThat(logStoreTrackingId.getJobId(), is(JOB_ID));
        assertThat(logStoreTrackingId.getChunkId(), is(CHUNK_ID));
        assertThat(logStoreTrackingId.getItemId(), is(ITEM_ID));
    }
}
