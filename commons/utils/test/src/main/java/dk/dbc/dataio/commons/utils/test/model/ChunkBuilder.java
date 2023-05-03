package dk.dbc.dataio.commons.utils.test.model;

import dk.dbc.dataio.commons.types.Chunk;
import dk.dbc.dataio.commons.types.ChunkItem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ChunkBuilder {
    private int jobId = 3;
    private long chunkId = 1;
    private final Chunk.Type type;
    private List<ChunkItem> items = new ArrayList<>(Collections.singletonList(new ChunkItemBuilder().build()));
    private List<ChunkItem> next = null;

    public ChunkBuilder(Chunk.Type type) {
        this.type = type;
    }

    public ChunkBuilder setJobId(int jobId) {
        this.jobId = jobId;
        return this;
    }

    public ChunkBuilder setChunkId(long chunkId) {
        this.chunkId = chunkId;
        return this;
    }

    public ChunkBuilder setItems(List<ChunkItem> items) {
        this.items = items;
        return this;
    }

    public ChunkBuilder appendItem(ChunkItem item) {
        item.withId(this.items.size());
        this.items.add(item);
        return this;
    }

    public ChunkBuilder setNextItems(List<ChunkItem> nextItems) {
        this.next = nextItems;
        return this;
    }

    public Chunk build() {
        final Chunk chunk = new Chunk(jobId, chunkId, type);
        chunk.addAllItems(items, next);
        return chunk;
    }
}
