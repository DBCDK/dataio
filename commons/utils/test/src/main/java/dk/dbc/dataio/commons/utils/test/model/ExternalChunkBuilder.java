package dk.dbc.dataio.commons.utils.test.model;

import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.types.ExternalChunk;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ExternalChunkBuilder {
    private long jobId = 3;
    private long chunkId = 1;
    private final ExternalChunk.Type type;
    private List<ChunkItem> items = new ArrayList<>(Collections.singletonList(new ChunkItemBuilder().build()));
    private List<ChunkItem> next = null;

    public ExternalChunkBuilder(ExternalChunk.Type type) {
        this.type = type;
    }

    public ExternalChunkBuilder setJobId(long jobId) {
        this.jobId = jobId;
        return this;
    }

    public ExternalChunkBuilder setChunkId(long chunkId) {
        this.chunkId = chunkId;
        return this;
    }

    public ExternalChunkBuilder setItems(List<ChunkItem> items) {
        this.items = items;
        return this;
    }

    public ExternalChunkBuilder setNextItems(List<ChunkItem> nextItems) {
        this.next = nextItems;
        return this;
    }

    public ExternalChunk build() {
        final ExternalChunk chunk = new ExternalChunk(jobId, chunkId, type);
        chunk.addAllItems(items, next);
        return chunk;
    }
}
