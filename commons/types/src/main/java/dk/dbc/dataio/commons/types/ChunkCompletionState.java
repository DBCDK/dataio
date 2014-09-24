package dk.dbc.dataio.commons.types;

import java.util.List;

public class ChunkCompletionState {

    private /* final */ long chunkId;
    private /* final */ List<ItemCompletionState> items;

    public ChunkCompletionState(long chunkId, List<ItemCompletionState> items) {
        this.chunkId = chunkId;
        this.items = items;
    }

    public long getChunkId() {
        return chunkId;
    }

    public List<ItemCompletionState> getItems() {
        return items;
    }
}
