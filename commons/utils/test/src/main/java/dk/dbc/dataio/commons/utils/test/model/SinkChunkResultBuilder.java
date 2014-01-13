package dk.dbc.dataio.commons.utils.test.model;

import dk.dbc.dataio.commons.types.SinkChunkResult;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SinkChunkResultBuilder {
    private long jobId = 42;
    private long chunkId = 1;
    private Charset encoding = StandardCharsets.UTF_8;
    private List<String> results = new ArrayList<>(Arrays.asList("diagnostic"));

    public SinkChunkResultBuilder setChunkId(long chunkId) {
        this.chunkId = chunkId;
        return this;
    }

    public SinkChunkResultBuilder setEncoding(Charset encoding) {
        this.encoding = encoding;
        return this;
    }

    public SinkChunkResultBuilder setJobId(long jobId) {
        this.jobId = jobId;
        return this;
    }

    public SinkChunkResultBuilder setResults(List<String> results) {
        this.results = results;
        return this;
    }

    public SinkChunkResult build() {
        return new SinkChunkResult(jobId, chunkId, encoding, results);
    }
}
