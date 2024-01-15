package dk.dbc.dataio.commons.partioner;

import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.utils.lang.StringUtil;
import dk.dbc.dataio.jobstore.types.PrematureEndOfDataException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.Iterator;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class JsonDataPartitionerTest {
    private final static InputStream EMPTY_STREAM = StringUtil.asInputStream("");

    @Test
    public void testingNextRecordFromEmptyStream() {
        final JsonDataPartitioner partitioner = JsonDataPartitioner.newInstance(EMPTY_STREAM, "UTF-8");
        final Iterator<DataPartitionerResult> iterator = partitioner.iterator();
        assertThat(iterator.hasNext(), is(false));
    }

    @Test
    public void readingNextRecordFromEmptyStream() {
        final JsonDataPartitioner partitioner = JsonDataPartitioner.newInstance(EMPTY_STREAM, "UTF-8");
        final Iterator<DataPartitionerResult> iterator = partitioner.iterator();
        assertThat(iterator.next(), is(DataPartitionerResult.EMPTY));
    }

    @Test
    public void testingEmptyJsonArray() {
        final InputStream inputStream = StringUtil.asInputStream("[]");
        final JsonDataPartitioner partitioner = JsonDataPartitioner.newInstance(inputStream, "UTF-8");
        final Iterator<DataPartitionerResult> iterator = partitioner.iterator();
        assertThat(iterator.hasNext(), is(false));
    }

    @Test
    public void readingEmptyJsonArray() {
        final InputStream inputStream = StringUtil.asInputStream("[]");
        final JsonDataPartitioner partitioner = JsonDataPartitioner.newInstance(inputStream, "UTF-8");
        final Iterator<DataPartitionerResult> iterator = partitioner.iterator();
        assertThat(iterator.next(), is(DataPartitionerResult.EMPTY));
    }

    @Test
    public void testingInvalidJson() {
        final InputStream inputStream = StringUtil.asInputStream("not JSON");
        final JsonDataPartitioner partitioner = JsonDataPartitioner.newInstance(inputStream, "UTF-8");
        final Iterator<DataPartitionerResult> iterator = partitioner.iterator();
        Assertions.assertThrows(PrematureEndOfDataException.class, iterator::hasNext);
    }

    @Test
    public void readingInvalidJson() {
        final InputStream inputStream = StringUtil.asInputStream("not JSON");
        final JsonDataPartitioner partitioner = JsonDataPartitioner.newInstance(inputStream, "UTF-8");
        final Iterator<DataPartitionerResult> iterator = partitioner.iterator();
        Assertions.assertThrows(PrematureEndOfDataException.class, iterator::hasNext);
    }

    @Test
    public void testingJsonObjectWithoutArrayWrapper() {
        final InputStream inputStream = StringUtil.asInputStream("{\"id\": \"standalone\"}");
        final JsonDataPartitioner partitioner = JsonDataPartitioner.newInstance(inputStream, "UTF-8");
        final Iterator<DataPartitionerResult> iterator = partitioner.iterator();
        assertThat(iterator.hasNext(), is(true));
    }

    @Test
    public void readingJsonObjectWithoutArrayWrapper() {
        final InputStream inputStream = StringUtil.asInputStream("{\"id\": \"standalone\"}");
        final JsonDataPartitioner partitioner = JsonDataPartitioner.newInstance(inputStream, "UTF-8");
        final Iterator<DataPartitionerResult> iterator = partitioner.iterator();
        final DataPartitionerResult result = iterator.next();
        assertThat("chunk item", result.getChunkItem(),
                is(ChunkItem.successfulChunkItem()
                        .withId(0)
                        .withType(ChunkItem.Type.JSON)
                        .withData("{\"id\":\"standalone\"}")));
    }

    @Test
    public void hasNextSuccessiveCallsAreIdempotent() {
        final InputStream inputStream = StringUtil.asInputStream("[{\"id\": 0}, {\"id\": 1}, {\"id\": 2}]");
        final JsonDataPartitioner partitioner = JsonDataPartitioner.newInstance(inputStream, "UTF-8");
        final Iterator<DataPartitionerResult> iterator = partitioner.iterator();
        iterator.next();
        iterator.hasNext();
        iterator.hasNext();
        iterator.hasNext();
        iterator.hasNext();
        iterator.hasNext();
        assertThat("hasNext", iterator.hasNext(), is(true));
        assertThat("position in datafile", iterator.next().getPositionInDatafile(), is(1));
    }

    @Test
    public void multipleIterations() {
        final InputStream inputStream = StringUtil.asInputStream("[{\"id\": 0}, {\"id\": 1}, {\"id\": 2}]");
        final JsonDataPartitioner partitioner = JsonDataPartitioner.newInstance(inputStream, "UTF-8");
        int chunkItemNo = 0;
        for (DataPartitionerResult result : partitioner) {
            assertThat("result" + chunkItemNo + " position in datafile", result.getPositionInDatafile(),
                    is(chunkItemNo));
            assertThat("chunk item " + chunkItemNo, result.getChunkItem(),
                    is(ChunkItem.successfulChunkItem()
                            .withId(0)
                            .withType(ChunkItem.Type.JSON)
                            .withData("{\"id\":" + chunkItemNo + "}")));
            chunkItemNo++;
        }
        assertThat("number of chunk item created", chunkItemNo, is(3));
        assertThat("number of bytes read", partitioner.getBytesRead(), is(33L));
    }

    @Test
    public void drainItems() {
        final InputStream inputStream = StringUtil.asInputStream("[{\"id\": 0}, {\"id\": 1}, {\"id\": 2}]");
        final JsonDataPartitioner partitioner = JsonDataPartitioner.newInstance(inputStream, "UTF-8");
        partitioner.drainItems(2);
        final Iterator<DataPartitionerResult> iterator = partitioner.iterator();
        assertThat("hasNext", iterator.hasNext(), is(true));
        assertThat("position in datafile", iterator.next().getPositionInDatafile(), is(2));
    }
}
