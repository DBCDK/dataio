package dk.dbc.dataio.commons.utils.test.model;

import dk.dbc.dataio.commons.types.ChunkResult;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ChunkResultBuilder {
    private long jobId = 42;
    private long chunkId = 1;
    private Charset encoding = StandardCharsets.UTF_8;
    private List<String> results = new ArrayList<>(Arrays.asList("record"));

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

    public ChunkResultBuilder setResults(List<String> results) {
        this.results = results;
        return this;
    }

    public ChunkResult build() {
        return new ChunkResult(jobId, chunkId, encoding, results);
    }
}
