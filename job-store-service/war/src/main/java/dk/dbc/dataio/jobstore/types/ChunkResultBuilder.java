package dk.dbc.dataio.jobstore.types;

import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.utils.test.model.ChunkItemBuilder;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ChunkResultBuilder {
    private long jobId = 42;
    private long chunkId = 1;
    private Charset encoding = StandardCharsets.UTF_8;
    private List<ChunkItem> items = new ArrayList<>(Arrays.asList(new ChunkItemBuilder().build()));

    public ChunkResultBuilder setChunkId(long chunkId) {
        this.chunkId = chunkId;
        return this;
    }

    public ChunkResultBuilder setEncoding(Charset encoding) {
        this.encoding = encoding;
        return this;
    }

    public ChunkResultBuilder setJobId(long jobId) {
        this.jobId = jobId;
        return this;
    }

    public ChunkResultBuilder setItems(List<ChunkItem> items) {
        this.items = items;
        return this;
    }

    public ChunkResult build() {
        return new ChunkResult(jobId, chunkId, encoding, items);
    }
}
