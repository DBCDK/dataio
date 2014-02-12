package dk.dbc.dataio.commons.utils.test.model;

import dk.dbc.dataio.commons.types.Chunk;
import dk.dbc.dataio.commons.types.Flow;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ChunkBuilder {
    private long jobId = 3;
    private long chunkId = 1;
    private Flow flow = new FlowBuilder().build();
    private List<String> records = new ArrayList<>(Arrays.asList("record"));

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

    public ChunkBuilder setRecords(List<String> records) {
        this.records = records;
        return this;
    }

    public Chunk build() {
        return new Chunk(jobId, chunkId, flow, records);
    }
}
