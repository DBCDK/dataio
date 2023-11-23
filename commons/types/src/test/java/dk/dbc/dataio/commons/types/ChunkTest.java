package dk.dbc.dataio.commons.types;

import dk.dbc.commons.jsonb.JSONBContext;
import dk.dbc.commons.jsonb.JSONBException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class ChunkTest {
    private static final ChunkItem CHUNK_ITEM = ChunkItem.successfulChunkItem()
            .withId(0)
            .withData("data");
    private Chunk chunk;
    private final JSONBContext jsonbContext = new JSONBContext();

    @BeforeEach
    public void newChunk() {
        chunk = new Chunk(1, 1L, Chunk.Type.PARTITIONED);
    }

    @Test
    public void constructor_negativeJobId_throws() {
        assertThrows(IllegalArgumentException.class, () -> new Chunk(-1, 1L, Chunk.Type.PARTITIONED));
    }

    @Test
    public void constructor_negativeChunkId_throws() {
        assertThrows(IllegalArgumentException.class, () -> new Chunk(1, -1L, Chunk.Type.PARTITIONED));
    }

    @Test
    public void constructor_lowestLegalArguments_success() {
        Chunk chunk = new Chunk(0, 0L, Chunk.Type.PARTITIONED);
        assertThat("job id", chunk.getJobId(), is(0));
        assertThat("chunk id", chunk.getChunkId(), is(0L));
        assertThat("type", chunk.getType(), is(Chunk.Type.PARTITIONED));
    }

    @Test
    public void insertItem_itemArgIsNull_throws() {
        assertThrows(IllegalArgumentException.class, () -> chunk.insertItem(null));
    }

    @Test
    public void insertItem_outOfOrderItemId_throws() {
        assertThrows(IllegalArgumentException.class, () -> chunk.insertItem(ChunkItem.ignoredChunkItem().withId(1).withData("data")));
    }

    @Test
    public void insertItem_itemArgIsValid_addsItemToChunk() {
        chunk.insertItem(CHUNK_ITEM);
        assertThat("is chunk empty?", chunk.isEmpty(), is(false));
        assertThat("chunk has next items?", chunk.hasNextItems(), is(false));
    }

    @Test
    public void insertItem_2arg_currentItemArgIsNull_throws() {
        assertThrows(IllegalArgumentException.class, () -> chunk.insertItem(ChunkItem.UNDEFINED, CHUNK_ITEM));
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

    @Test
    public void insertItem_2arg_nextItemIdDiffersFromCurrentItemId_throws() {
        ChunkItem nextChunkItem = ChunkItem.successfulChunkItem().withId(1).withData("data");
        assertThrows(IllegalArgumentException.class, () -> chunk.insertItem(CHUNK_ITEM, nextChunkItem));
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

    @Test
    public void addAllItems_2arg_listsDifferInSize_throws() {
        ChunkItem firstItem = ChunkItem.ignoredChunkItem().withId(0).withData("First");
        ChunkItem secondItem = ChunkItem.successfulChunkItem().withId(1).withData("Second");
        assertThrows(IllegalArgumentException.class, () -> chunk.addAllItems(List.of(firstItem, secondItem), List.of(firstItem)));
    }

    @Test
    public void chunk_iterator() {
        ChunkItem firstItem = ChunkItem.ignoredChunkItem().withId(0).withData("First");
        ChunkItem secondItem = ChunkItem.successfulChunkItem().withId(1).withData("Second");
        ChunkItem thirdItem = ChunkItem.failedChunkItem().withId(2).withData("Third");
        chunk.insertItem(firstItem);
        chunk.insertItem(secondItem);
        chunk.insertItem(thirdItem);

        assertThat("chunk size", chunk.size(), is(3));

        Iterator<ChunkItem> it = chunk.iterator();
        assertThat("chunk has first item", it.hasNext(), is(true));
        assertThat("first item", it.next(), is(firstItem));
        assertThat("chunk has second item", it.hasNext(), is(true));
        assertThat("second item", it.next(), is(secondItem));
        assertThat("chunk has third item", it.hasNext(), is(true));
        assertThat("third item", it.next(), is(thirdItem));
        assertThat("chunk has fourth item", it.hasNext(), is(false));
    }

    @Test
    public void convertToJsonAndBackAgain() throws JSONBException {
        ChunkItem firstItem = ChunkItem.ignoredChunkItem().withId(0).withData("First");
        ChunkItem secondItem = ChunkItem.successfulChunkItem().withId(1).withData("Second");
        ChunkItem thirdItem = ChunkItem.failedChunkItem().withId(2).withData("Third");
        chunk.insertItem(firstItem);
        chunk.insertItem(secondItem);
        chunk.insertItem(thirdItem);

        Chunk unmarshalledChunk = jsonbContext.unmarshall(jsonbContext.marshall(chunk), Chunk.class);
        Iterator<ChunkItem> it = unmarshalledChunk.iterator();
        assertThat("chunk has first item", it.hasNext(), is(true));
        assertThat("first item", it.next(), is(firstItem));
        assertThat("chunk has second item", it.hasNext(), is(true));
        assertThat("second item", it.next(), is(secondItem));
        assertThat("chunk has third item", it.hasNext(), is(true));
        assertThat("third item", it.next(), is(thirdItem));
        assertThat("chunk has fourth item", it.hasNext(), is(false));
    }

    @Test
    public void unmarshallFromJsonWhichDoNotUpholdInvariant() {
        final String illegalJson = "{\"jobId\":1,\"chunkId\":1,\"type\":\"PROCESSED\",\"items\":[{\"id\":1,\"data\":\"ZGF0YQ==\",\"status\":\"SUCCESS\"},{\"id\":0,\"data\":\"Second\",\"status\":\"SUCCESS\"}]}";
        assertThrows(JSONBException.class, () -> jsonbContext.unmarshall(illegalJson, Chunk.class));
    }

    @Test
    public void unmarshallFromJsonWithoutNext() throws JSONBException {
        final String json = "{\"jobId\":1,\"chunkId\":1,\"type\":\"PROCESSED\",\"items\":[{\"id\":0,\"data\":\"ZGF0YQ==\",\"status\":\"SUCCESS\", \"type\":[\"UNKNOWN\"],\"encoding\":\"UTF-8\"}]}";
        Chunk chunk = jsonbContext.unmarshall(json, Chunk.class);
        assertThat(chunk, is(notNullValue()));
    }
}
