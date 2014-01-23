package dk.dbc.dataio.commons.utils.test.model;

import dk.dbc.dataio.commons.types.Chunk;
import dk.dbc.dataio.commons.types.Flow;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ChunkBuilder {
    private long id = 1;
    private Flow flow = new FlowBuilder().build();
    private List<String> records = new ArrayList<>(Arrays.asList("record"));

    public ChunkBuilder setId(long id) {
        this.id = id;
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
        return new Chunk(id, flow, records);
    }
}
