package dk.dbc.dataio.jobstore.service.partitioner;

import dk.dbc.commons.addi.AddiRecord;
import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.utils.lang.StringUtil;
import dk.dbc.dataio.jobstore.types.InvalidEncodingException;
import dk.dbc.dataio.jobstore.types.PrematureEndOfDataException;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;

import static dk.dbc.commons.testutil.Assert.assertThat;
import static dk.dbc.commons.testutil.Assert.isThrowing;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

public class AddiDataPartitionerTest {
    private final static InputStream EMPTY_STREAM = StringUtil.asInputStream("");
    private final static String UTF_8_ENCODING = "UTF-8";

    @Test(expected = NullPointerException.class)
    public void constructor_inputStreamArgIsNull_throws() {
        AddiDataPartitioner.newInstance(null, UTF_8_ENCODING);
    }

    @Test(expected = NullPointerException.class)
    public void constructor_encodingNameArgIsNull_throws() {
        AddiDataPartitioner.newInstance(EMPTY_STREAM, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructor_encodingNameArgIsEmpty_throws() {
        AddiDataPartitioner.newInstance(EMPTY_STREAM, " ");
    }

    @Test(expected = InvalidEncodingException.class)
    public void constructor_encodingNameArgIsInvalid_throws() {
        AddiDataPartitioner.newInstance(EMPTY_STREAM, "no-such-encoding");
    }

    @Test
    public void partitioner_readingNextRecordFromEmptyStream_returnsEmptyResult() {
        final AddiDataPartitioner partitioner = AddiDataPartitioner.newInstance(EMPTY_STREAM, UTF_8_ENCODING);
        final Iterator<DataPartitionerResult> iterator = partitioner.iterator();
        final DataPartitionerResult dataPartitionerResult = iterator.next();
        assertThat(dataPartitionerResult, is(DataPartitionerResult.EMPTY));
    }

    @Test
    public void partitioner_readingInvalidAddiFormat_throws() {
        final InputStream addiStream = StringUtil.asInputStream("2\n{}\n");
        final AddiDataPartitioner partitioner = AddiDataPartitioner.newInstance(addiStream, UTF_8_ENCODING);
        final Iterator<DataPartitionerResult> iterator = partitioner.iterator();
        assertThat(iterator::next, isThrowing(PrematureEndOfDataException.class));
    }

    @Test
    public void partitioner_readingValidRecord_returnsResultWithChunkItemWithStatusSuccess() {
        final String addiRecord = "2\n{}\n7\ncontent\n";
        final InputStream addiStream = StringUtil.asInputStream(addiRecord);
        final AddiDataPartitioner partitioner = AddiDataPartitioner.newInstance(addiStream, UTF_8_ENCODING);
        final Iterator<DataPartitionerResult> iterator = partitioner.iterator();
        final DataPartitionerResult dataPartitionerResult = iterator.next();
        final ChunkItem chunkItem = dataPartitionerResult.getChunkItem();
        assertThat("chunkItem", chunkItem, is(notNullValue()));
        assertThat("chunkItem.getStatus()", chunkItem.getStatus(), is(ChunkItem.Status.SUCCESS));
        assertThat("chunkItem.getDiagnostics()", chunkItem.getDiagnostics(), is(nullValue()));
        assertThat("chunkItem.getData", StringUtil.asString(chunkItem.getData()), is(addiRecord));
        assertThat("chunkItem.getType()", chunkItem.getType(), is(Arrays.asList(partitioner.getChunkItemType())));
        assertThat("recordInfo", dataPartitionerResult.getRecordInfo(), is(notNullValue()));
    }

    @Test
    public void partitioner_readingEmptyRecord_returnsResultWithChunkItemWithStatusIgnore() {
        final InputStream addiStream = StringUtil.asInputStream("0\n\n0\n\n");
        final AddiDataPartitioner partitioner = AddiDataPartitioner.newInstance(addiStream, UTF_8_ENCODING);
        final Iterator<DataPartitionerResult> iterator = partitioner.iterator();
        final DataPartitionerResult dataPartitionerResult = iterator.next();
        final ChunkItem chunkItem = dataPartitionerResult.getChunkItem();
        assertThat("chunkItem", chunkItem, is(notNullValue()));
        assertThat("chunkItem.getStatus()", chunkItem.getStatus(), is(ChunkItem.Status.IGNORE));
        assertThat("chunkItem.getType()", chunkItem.getType(), is(Collections.singletonList(ChunkItem.Type.STRING)));
    }

    @Test
    public void partitioner_readingRecordWithInvalidMetaData_returnsResultWithChunkItemWithStatusFailure() {
        final InputStream addiStream = StringUtil.asInputStream("8\nnot json\n7\ncontent\n");
        final AddiRecord expectedContent = new AddiRecord(StringUtil.asBytes("not json"), StringUtil.asBytes("content"));
        final AddiDataPartitioner partitioner = AddiDataPartitioner.newInstance(addiStream, UTF_8_ENCODING);
        final Iterator<DataPartitionerResult> iterator = partitioner.iterator();
        final DataPartitionerResult dataPartitionerResult = iterator.next();
        final ChunkItem chunkItem = dataPartitionerResult.getChunkItem();
        assertThat("chunkItem", chunkItem, is(notNullValue()));
        assertThat("chunkItem.getStatus()", chunkItem.getStatus(), is(ChunkItem.Status.FAILURE));
        assertThat("chunkItem.getDiagnostics()", chunkItem.getDiagnostics(), is(notNullValue()));
        assertThat("chunkItem.getData", StringUtil.asString(chunkItem.getData()), is(StringUtil.asString(expectedContent.getBytes())));
        assertThat("chunkItem.getType()", chunkItem.getType(), is(Collections.singletonList(ChunkItem.Type.BYTES)));
        assertThat("recordInfo", dataPartitionerResult.getRecordInfo(), is(nullValue()));
    }

    @Test
    public void partitioner_metaDataContainsTrackingId_returnsResultWithChunkItemWithTrackingId() {
        final InputStream addiStream = StringUtil.asInputStream("27\n{\"trackingId\": \"trackedAs\"}\n7\ncontent\n");
        final AddiDataPartitioner partitioner = AddiDataPartitioner.newInstance(addiStream, UTF_8_ENCODING);
        final Iterator<DataPartitionerResult> iterator = partitioner.iterator();
        final DataPartitionerResult dataPartitionerResult = iterator.next();
        final ChunkItem chunkItem = dataPartitionerResult.getChunkItem();
        assertThat("chunkItem", chunkItem, is(notNullValue()));
        assertThat("chunkItem.getTrackingId()", chunkItem.getTrackingId(), is("trackedAs"));
    }

    @Test
    public void partitioner_metaDataContainsPid_returnsRecordInfoWithPid() {
        final InputStream addiStream = StringUtil.asInputStream(
                "14\n{\"pid\": \"pid\"}\n7\ncontent\n");
        final AddiDataPartitioner partitioner = AddiDataPartitioner
                .newInstance(addiStream, UTF_8_ENCODING);
        final Iterator<DataPartitionerResult> iterator = partitioner.iterator();
        final DataPartitionerResult dataPartitionerResult = iterator.next();
        assertThat("pid", dataPartitionerResult.getRecordInfo().getPid(),
                is("pid"));
    }

    @Test
    public void partitioner_metaDataContainsDiagnostic_returnsResultWithChunkItemWithStatusFailure() {
        final AddiRecord addiRecord = new AddiRecord(
                "{\"diagnostic\":{\"level\":\"FATAL\",\"message\":\"error\"}}".getBytes(StandardCharsets.UTF_8),
                "content".getBytes(StandardCharsets.UTF_8));
        final AddiDataPartitioner partitioner = AddiDataPartitioner.newInstance(new ByteArrayInputStream(addiRecord.getBytes()), UTF_8_ENCODING);
        final Iterator<DataPartitionerResult> iterator = partitioner.iterator();
        final DataPartitionerResult dataPartitionerResult = iterator.next();
        final ChunkItem chunkItem = dataPartitionerResult.getChunkItem();
        assertThat("chunkItem", chunkItem, is(notNullValue()));
        assertThat("chunkItem.getStatus()", chunkItem.getStatus(), is(ChunkItem.Status.FAILURE));
        assertThat("chunkItem.getDiagnostics()", chunkItem.getDiagnostics(), is(notNullValue()));
        assertThat("chunkItem.getDiagnostics().get(0).getMessage()", chunkItem.getDiagnostics().get(0).getMessage(), is("error"));
        assertThat("chunkItem.getData()", StringUtil.asString(chunkItem.getData()), is(StringUtil.asString(addiRecord.getBytes())));
        assertThat("chunkItem.getType()", chunkItem.getType(), is(Arrays.asList(partitioner.getChunkItemType())));
        assertThat("recordInfo", dataPartitionerResult.getRecordInfo(), is(notNullValue()));
    }

    @Test
    public void partitioner_multipleIterations() {
        final InputStream addiStream = StringUtil.asInputStream("2\n{}\n8\ncontent1\n1\n{\n6\nfailed\n2\n{}\n8\ncontent2\n");
        final AddiDataPartitioner partitioner = AddiDataPartitioner.newInstance(addiStream, UTF_8_ENCODING);
        int chunkItemNo = 0;
        for (DataPartitionerResult dataPartitionerResult : partitioner) {
            assertThat("result" + chunkItemNo + " position in datafile",
                    dataPartitionerResult.getPositionInDatafile(), is(chunkItemNo));
            assertThat("Chunk item " + chunkItemNo++, dataPartitionerResult.getChunkItem(), is(notNullValue()));
        }
        assertThat("Number of chunk item created", chunkItemNo, is(3));
        assertThat("Number of bytes read", partitioner.getBytesRead(), is(45L));
    }

    @Test
    public void drainItems() {
        final InputStream addiStream = StringUtil.asInputStream("2\n{}\n8\ncontent1\n1\n{\n6\nfailed\n2\n{}\n8\ncontent2\n");
        final AddiDataPartitioner partitioner = AddiDataPartitioner.newInstance(addiStream, UTF_8_ENCODING);
        partitioner.drainItems(2);
        final Iterator<DataPartitionerResult> iterator = partitioner.iterator();
        assertThat("position in datafile", iterator.next().getPositionInDatafile(), is(2));
    }
}
