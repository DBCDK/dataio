package dk.dbc.dataio.commons.utils.test.model;

import dk.dbc.dataio.commons.types.ChunkCompletionState;
import dk.dbc.dataio.commons.types.ItemCompletionState;

import java.util.ArrayList;
import java.util.List;

public class ChunkCompletionStateBuilder {
    public static long DEFAULT_CHUNK_ID = 34L;
    private long chunkId = DEFAULT_CHUNK_ID;
    private List<ItemCompletionState> items = new ArrayList<ItemCompletionState>();

    public ChunkCompletionStateBuilder setChunkId(long chunkId) {
        this.chunkId = chunkId;
        return this;
    }

    public ChunkCompletionStateBuilder addItem(ItemCompletionState item) {
        items.add(item);
        return this;
    }

    public ChunkCompletionState build() {
        final ChunkCompletionState chunkCompletionState = new ChunkCompletionState(chunkId, items);
        return chunkCompletionState;
    }
}
