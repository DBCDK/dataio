package dk.dbc.dataio.commons.utils.test.model;

import dk.dbc.dataio.commons.types.Chunk;
import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.types.Flow;
import dk.dbc.dataio.commons.types.SupplementaryProcessData;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ChunkBuilder {
    private long jobId = 3;
    private long chunkId = 1;
    private Flow flow = new FlowBuilder().build();
    private SupplementaryProcessData supplementaryProcessData = new SupplementaryProcessData(424242L, "latin-1");
    private List<ChunkItem> items = new ArrayList<>(Arrays.asList(new ChunkItemBuilder().build()));
    private Set<String> keys = new HashSet<>(0);

    public ChunkBuilder setJobId(long jobId) {
        this.jobId = jobId;
        return this;
    }

    public ChunkBuilder setChunkId(long chunkId) {
        this.chunkId = chunkId;
        return this;
    }

    public ChunkBuilder setFlow(Flow flow) {
        this.flow = flow;
        return this;
    }

    public ChunkBuilder setSupplementaryProcessData(SupplementaryProcessData supplementaryProcessData) {
        this.supplementaryProcessData = supplementaryProcessData;
        return this;
    }

    public ChunkBuilder setItems(List<ChunkItem> items) {
        this.items = items;
        return this;
    }

    public ChunkBuilder setKeys(Set<String> keys) {
        this.keys = keys;
        return this;
    }

    public Chunk build() {
        final Chunk chunk = new Chunk(jobId, chunkId, flow, supplementaryProcessData, items);
        for (final String key : keys) {
            chunk.addKey(key);
        }
        return chunk;
    }
}
