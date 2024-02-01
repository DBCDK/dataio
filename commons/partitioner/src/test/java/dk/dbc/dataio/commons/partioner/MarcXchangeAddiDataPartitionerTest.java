package dk.dbc.dataio.commons.partioner;

import dk.dbc.commons.addi.AddiRecord;
import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.types.Diagnostic;
import dk.dbc.dataio.commons.utils.lang.StringUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class MarcXchangeAddiDataPartitionerTest extends AbstractPartitionerTestBase {
    private final static InputStream EMPTY_STREAM = StringUtil.asInputStream("");
    private final static String UTF_8_ENCODING = "UTF-8";

    @Test
    public void newInstance_inputStreamArgIsNull_throws() {
        assertThrows(NullPointerException.class, () -> MarcXchangeAddiDataPartitioner.newInstance(null, UTF_8_ENCODING));
    }

    @Test
    public void newInstance_inputStreamArgIsInvalid_throws() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> MarcXchangeAddiDataPartitioner.newInstance(
                        new InputStream() {
                            @Override
                            public int read() {
                                return 0;
                            }
                        },
                        UTF_8_ENCODING));
    }

    @Test
    public void newInstance_encodingArgIsNull_throws() {
        assertThrows(NullPointerException.class, () -> MarcXchangeAddiDataPartitioner.newInstance(EMPTY_STREAM, null));
    }

    @Test
    public void newInstance_encodingArgIsEmpty_throws() {
        assertThrows(IllegalArgumentException.class, () -> MarcXchangeAddiDataPartitioner.newInstance(EMPTY_STREAM, " "));
    }

    @Test
    public void readingValidRecord() {
        final byte[] contentData = AbstractPartitionerTestBase.getResourceAsByteArray(
                "test-record-1-danmarc2.marcXChange");
        final AddiRecord addiRecord = new AddiRecord("{}".getBytes(), contentData);
        final ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(addiRecord.getBytes());
        final MarcXchangeAddiDataPartitioner partitioner =
                MarcXchangeAddiDataPartitioner.newInstance(byteArrayInputStream, UTF_8_ENCODING);
        final Iterator<DataPartitionerResult> iterator = partitioner.iterator();
        final DataPartitionerResult dataPartitionerResult = iterator.next();
        final ChunkItem chunkItem = dataPartitionerResult.getChunkItem();
        assertThat("chunkItem", chunkItem, is(notNullValue()));
        assertThat("chunkItem.getType()", chunkItem.getType(), is(Arrays.asList(ChunkItem.Type.ADDI, ChunkItem.Type.MARCXCHANGE)));
        assertThat("chunkItem.getData", StringUtil.asString(chunkItem.getData()), is(StringUtil.asString(addiRecord.getBytes())));
        assertThat("recordInfo", dataPartitionerResult.getRecordInfo(), is(notNullValue()));
    }

    @Test
    public void readingInvalidRecord() {
        final byte[] contentData = AbstractPartitionerTestBase.getResourceAsByteArray(
                "test-record-1-danmarc2-whitespace-as-subfield-code.marcXChange");
        final AddiRecord addiRecord = new AddiRecord("{}".getBytes(), contentData);
        final ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(addiRecord.getBytes());
        final MarcXchangeAddiDataPartitioner partitioner =
                MarcXchangeAddiDataPartitioner.newInstance(byteArrayInputStream, UTF_8_ENCODING);
        final Iterator<DataPartitionerResult> iterator = partitioner.iterator();
        final DataPartitionerResult dataPartitionerResult = iterator.next();
        final ChunkItem chunkItem = dataPartitionerResult.getChunkItem();
        assertThat("chunk item", chunkItem, is(notNullValue()));
        assertThat("chunk item type", chunkItem.getType(),
                is(Collections.singletonList(ChunkItem.Type.BYTES)));
        assertThat("chunk item data", StringUtil.asString(chunkItem.getData()),
                is(StringUtil.asString(addiRecord.getBytes())));
        assertThat("chunk item has diagnostic", !chunkItem.getDiagnostics().isEmpty(),
                is(true));
        assertThat("chunk item has ERROR level diagnostic", chunkItem.getDiagnostics().get(0).getLevel(),
                is(Diagnostic.Level.ERROR));
    }
}
