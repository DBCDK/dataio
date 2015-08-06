package dk.dbc.dataio.commons.types;

import dk.dbc.dataio.commons.utils.json.JsonException;
import dk.dbc.dataio.commons.utils.json.JsonUtil;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

public class ExternalChunkTest {
    private static final ChunkItem CHUNK_ITEM = new ChunkItem(0L, "data".getBytes(), ChunkItem.Status.SUCCESS);
    private ExternalChunk chunk;

    @Before
    public void newChunk() {
        chunk = new ExternalChunk(1L, 1L, ExternalChunk.Type.PARTITIONED);
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructor_negativeJobId_throws() {
        new ExternalChunk(-1L, 1L, ExternalChunk.Type.PARTITIONED);
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructor_negativeChunkId_throws() {
        new ExternalChunk(1L, -1L, ExternalChunk.Type.PARTITIONED);
    }

    @Test
    public void constructor_lowestLegalArguments_success() {
        ExternalChunk chunk = new ExternalChunk(0L, 0L, ExternalChunk.Type.PARTITIONED);
        assertThat("job id", chunk.getJobId(), is(0L));
        assertThat("chunk id", chunk.getChunkId(), is(0L));
        assertThat("type", chunk.getType(), is(ExternalChunk.Type.PARTITIONED));
    }

    @Test(expected = IllegalArgumentException.class)
    public void insertItem_itemArgIsNull_throws() {
        chunk.insertItem(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void insertItem_outOfOrderItemId_throws() {
        chunk.insertItem(new ChunkItem(1L, "data".getBytes(), ChunkItem.Status.IGNORE));
    }

    @Test
    public void insertItem_itemArgIsValid_addsItemToChunk() {
        chunk.insertItem(CHUNK_ITEM);
        assertThat("is chunk empty?", chunk.isEmpty(), is(false));
        assertThat("chunk has next items?", chunk.hasNextItems(), is(false));
    }

    @Test(expected = IllegalArgumentException.class)
    public void insertItem_2arg_currentItemArgIsNull_throws() {
        chunk.insertItem(ChunkItem.UNDEFINED, CHUNK_ITEM);
    }

    @Test
    public void insertItem_2arg_nextItemArgIsNull_addsItemToChunk() {
        chunk.insertItem(CHUNK_ITEM, ChunkItem.UNDEFINED);
        assertThat("chunk size", chunk.size(), is(1));
        assertThat("is chunk empty?", chunk.isEmpty(), is(false));
        assertThat("chunk has next items?", chunk.hasNextItems(), is(false));
    }

    @Test
    public void insertItem_2arg_nextItemArgIsNonNull_addsItemsToChunk() {
        chunk.insertItem(CHUNK_ITEM, CHUNK_ITEM);
        assertThat("chunk size", chunk.size(), is(1));
        assertThat("is chunk empty?", chunk.isEmpty(), is(false));
        assertThat("chunk has next items?", chunk.hasNextItems(), is(true));
    }

    @Test(expected = IllegalArgumentException.class)
    public void insertItem_2arg_nextItemIdDiffersFromCurrentItemId_throws() {
        final ChunkItem nextChunkItem = new ChunkItem(1L, "data".getBytes(), ChunkItem.Status.SUCCESS);
        chunk.insertItem(CHUNK_ITEM, nextChunkItem);
    }

    @Test
    public void addAllItems_addsItemsToChunk() {
        chunk.addAllItems(Collections.singletonList(CHUNK_ITEM));
        assertThat("chunk size", chunk.size(), is(1));
        assertThat("is chunk empty?", chunk.isEmpty(), is(false));
        assertThat("chunk has next items?", chunk.hasNextItems(), is(false));
    }

    @Test
    public void addAllItems_2arg_addsItemsToChunk() {
        chunk.addAllItems(Collections.singletonList(CHUNK_ITEM), Collections.singletonList(CHUNK_ITEM));
        assertThat("chunk size", chunk.size(), is(1));
        assertThat("is chunk empty?", chunk.isEmpty(), is(false));
        assertThat("chunk has next items?", chunk.hasNextItems(), is(true));
    }

    @Test
    public void addAllItems_2arg_nextArgIsNull_addsItemsToChunk() {
        chunk.addAllItems(Collections.singletonList(CHUNK_ITEM), null);
        assertThat("chunk size", chunk.size(), is(1));
        assertThat("is chunk empty?", chunk.isEmpty(), is(false));
        assertThat("chunk has next items?", chunk.hasNextItems(), is(false));
    }

    @Test(expected = IllegalArgumentException.class)
    public void addAllItems_2arg_listsDifferInSize_throws() {
        final ChunkItem firstItem = new ChunkItem(0L, "First".getBytes(), ChunkItem.Status.IGNORE);
        final ChunkItem secondItem = new ChunkItem(1L, "Second".getBytes(), ChunkItem.Status.SUCCESS);
        chunk.addAllItems(Arrays.asList(firstItem, secondItem), Collections.singletonList(firstItem));
    }

    @Test
    public void chunk_iterator() {
        final ChunkItem firstItem = new ChunkItem(0L, "First".getBytes(), ChunkItem.Status.IGNORE);
        final ChunkItem secondItem = new ChunkItem(1L, "Second".getBytes(), ChunkItem.Status.SUCCESS);
        final ChunkItem thirdItem = new ChunkItem(2L, "Third".getBytes(), ChunkItem.Status.FAILURE);
        chunk.insertItem(firstItem);
        chunk.insertItem(secondItem);
        chunk.insertItem(thirdItem);

        assertThat("chunk size", chunk.size(), is(3));

        final Iterator<ChunkItem> it = chunk.iterator();
        assertThat("chunk has first item", it.hasNext(), is(true));
        assertThat("first item", it.next(), is(firstItem));
        assertThat("chunk has second item", it.hasNext(), is(true));
        assertThat("second item", it.next(), is(secondItem));
        assertThat("chunk has third item", it.hasNext(), is(true));
        assertThat("third item", it.next(), is(thirdItem));
        assertThat("chunk has fourth item", it.hasNext(), is(false));
    }

    @Test
    public void convertToJsonAndBackAgain() throws JsonException {
        final ChunkItem firstItem = new ChunkItem(0L, "First".getBytes(), ChunkItem.Status.IGNORE);
        final ChunkItem secondItem = new ChunkItem(1L, "Second".getBytes(), ChunkItem.Status.SUCCESS);
        final ChunkItem thirdItem = new ChunkItem(2L, "Third".getBytes(), ChunkItem.Status.FAILURE);
        chunk.insertItem(firstItem);
        chunk.insertItem(secondItem);
        chunk.insertItem(thirdItem);

        final ExternalChunk unmarshalledChunk = JsonUtil.fromJson(JsonUtil.toJson(chunk), ExternalChunk.class);
        final Iterator<ChunkItem> it = unmarshalledChunk.iterator();
        assertThat("chunk has first item", it.hasNext(), is(true));
        assertThat("first item", it.next(), is(firstItem));
        assertThat("chunk has second item", it.hasNext(), is(true));
        assertThat("second item", it.next(), is(secondItem));
        assertThat("chunk has third item", it.hasNext(), is(true));
        assertThat("third item", it.next(), is(thirdItem));
        assertThat("chunk has fourth item", it.hasNext(), is(false));
    }

    @Test(expected = JsonException.class)
    public void unmarshallFromJsonWhichDoNotUpholdInvariant() throws JsonException {
        final String illegalJson = "{\"jobId\":1,\"chunkId\":1,\"type\":\"PROCESSED\",\"items\":[{\"id\":1,\"data\":\"ZGF0YQ==\",\"status\":\"SUCCESS\"},{\"id\":0,\"data\":\"Second\",\"status\":\"SUCCESS\"}]}";
        JsonUtil.fromJson(illegalJson, ExternalChunk.class);
    }

    @Test
    public void unmarshallFromJsonWithoutNext() throws JsonException {
        final String json = "{\"jobId\":1,\"chunkId\":1,\"type\":\"PROCESSED\",\"items\":[{\"id\":0,\"data\":\"ZGF0YQ==\",\"status\":\"SUCCESS\"}]}";
        final ExternalChunk chunk = JsonUtil.fromJson(json, ExternalChunk.class);
        assertThat(chunk, is(notNullValue()));
    }
}
