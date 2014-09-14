package dk.dbc.dataio.commons.types;

import java.util.List;

public abstract class AbstractChunk {
    protected /* final */ long jobId;
    protected /* final */ long chunkId;
    protected /* final */ List<ChunkItem> items;

    public long getJobId() {
        return jobId;
    }

    public long getChunkId() {
        return chunkId;
    }

    public void addItem(ChunkItem item) {
        items.add(item);
    }

    public List<ChunkItem> getItems() {
        return items;
    }
}
