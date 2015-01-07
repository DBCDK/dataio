package dk.dbc.dataio.commons.types;

import dk.dbc.dataio.commons.utils.json.JsonException;
import dk.dbc.dataio.commons.utils.json.JsonUtil;
import java.util.Iterator;
import static org.hamcrest.CoreMatchers.is;
import org.junit.Test;
import static org.junit.Assert.*;

public class ExternalChunkTest {

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
        assertThat(chunk.getJobId(), is(0L));
        assertThat(chunk.getChunkId(), is(0L));
        assertThat(chunk.getType(), is(ExternalChunk.Type.PARTITIONED));
    }

    // Test upper limmit
    @Test
    public void constructor_someLegalArgumentsLargerThanZero_success() {
        ExternalChunk chunk = new ExternalChunk(424242L, 1234567L, ExternalChunk.Type.PARTITIONED);
        assertThat(chunk.getJobId(), is(424242L));
        assertThat(chunk.getChunkId(), is(1234567L));
        assertThat(chunk.getType(), is(ExternalChunk.Type.PARTITIONED));
    }

    @Test
    public void insertItem_nonConsecutiveItemId_throws() {
        ExternalChunk chunk = new ExternalChunk(1L, 1L, ExternalChunk.Type.PARTITIONED);
        try {
            // The item with id 0L should have been inserted befor the item with id 1L.
            chunk.insertItem(new ChunkItem(1L, "", ChunkItem.Status.IGNORE));
            fail("Exception expected");
        } catch (IllegalArgumentException e) {
            // expected
        }
    }

    @Test
    public void insertItemsAndIterateOverThem() {
        ExternalChunk chunk = new ExternalChunk(1L, 1L, ExternalChunk.Type.PARTITIONED);
        chunk.insertItem(new ChunkItem(0L, "First", ChunkItem.Status.IGNORE));
        chunk.insertItem(new ChunkItem(1L, "Second", ChunkItem.Status.SUCCESS));
        chunk.insertItem(new ChunkItem(2L, "Third", ChunkItem.Status.FAILURE));

        Iterator<ChunkItem> it = chunk.iterator();
        assertThat(it.hasNext(), is(true));
        ChunkItem item = it.next();
        assertThat(item.getId(), is(0L));
        assertThat(item.getData(), is("First"));
        assertThat(item.getStatus(), is(ChunkItem.Status.IGNORE));
        assertThat(it.hasNext(), is(true));
        item = it.next();
        assertThat(item.getId(), is(1L));
        assertThat(item.getData(), is("Second"));
        assertThat(item.getStatus(), is(ChunkItem.Status.SUCCESS));
        assertThat(it.hasNext(), is(true));
        item = it.next();
        assertThat(item.getId(), is(2L));
        assertThat(item.getData(), is("Third"));
        assertThat(item.getStatus(), is(ChunkItem.Status.FAILURE));
        assertThat(it.hasNext(), is(false));
    }

    @Test
    public void convertToJsonAndBackAgain() throws JsonException {
        ExternalChunk chunk = new ExternalChunk(1L, 1L, ExternalChunk.Type.PROCESSED);
        chunk.insertItem(new ChunkItem(0L, "First", ChunkItem.Status.IGNORE));
        chunk.insertItem(new ChunkItem(1L, "Second", ChunkItem.Status.SUCCESS));
        chunk.insertItem(new ChunkItem(2L, "Third", ChunkItem.Status.FAILURE));

        String json = JsonUtil.toJson(chunk);
        System.err.println(json);

        ExternalChunk newChunk = JsonUtil.fromJson(json, ExternalChunk.class);
        Iterator<ChunkItem> it = newChunk.iterator();
        assertThat(it.hasNext(), is(true));
        ChunkItem item = it.next();
        assertThat(item.getId(), is(0L));
        assertThat(item.getData(), is("First"));
        assertThat(item.getStatus(), is(ChunkItem.Status.IGNORE));
        assertThat(it.hasNext(), is(true));
        item = it.next();
        assertThat(item.getId(), is(1L));
        assertThat(item.getData(), is("Second"));
        assertThat(item.getStatus(), is(ChunkItem.Status.SUCCESS));
        assertThat(it.hasNext(), is(true));
        item = it.next();
        assertThat(item.getId(), is(2L));
        assertThat(item.getData(), is("Third"));
        assertThat(item.getStatus(), is(ChunkItem.Status.FAILURE));
        assertThat(it.hasNext(), is(false));
    }

    @Test(expected = JsonException.class)
    public void convertFromJsonWhichDoNotUpholdInvariant() throws JsonException {
        String illegalJson = "{\"jobId\":1,\"chunkId\":1,\"type\":\"PROCESSED\",\"items\":[{\"id\":1,\"data\":\"Second\",\"status\":\"SUCCESS\"},{\"id\":0,\"data\":\"Second\",\"status\":\"SUCCESS\"}]}";
        JsonUtil.fromJson(illegalJson, ExternalChunk.class);
    }
}
