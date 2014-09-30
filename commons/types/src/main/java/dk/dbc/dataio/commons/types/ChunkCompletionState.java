package dk.dbc.dataio.commons.types;

import java.io.Serializable;
import java.util.List;

public class ChunkCompletionState implements Serializable {
    private static final long serialVersionUID = -3227256592358768589L;

    private /* final */ long chunkId;
    private /* final */ List<ItemCompletionState> items;

    private ChunkCompletionState() {}

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
