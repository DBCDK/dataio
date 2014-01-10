package dk.dbc.dataio.commons.utils.test.json;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SinkChunkResultJsonBuilder extends JsonBuilder {
    private long jobId = 42;
    private long chunkId = 1;
    private String encoding = "UTF-8";
    private List<String> results = new ArrayList<>(Arrays.asList("diagnostics"));

    public SinkChunkResultJsonBuilder setChunkId(long chunkId) {
        this.chunkId = chunkId;
        return this;
    }

    public SinkChunkResultJsonBuilder setEncoding(String encoding) {
        this.encoding = encoding;
        return this;
    }

    public SinkChunkResultJsonBuilder setJobId(long jobId) {
        this.jobId = jobId;
        return this;
    }

    public SinkChunkResultJsonBuilder setResults(List<String> results) {
        this.results = results;
        return this;
    }

    public String build() {
        final StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(START_OBJECT);
        stringBuilder.append(asLongMember("jobId", jobId)); stringBuilder.append(MEMBER_DELIMITER);
        stringBuilder.append(asLongMember("chunkId", chunkId)); stringBuilder.append(MEMBER_DELIMITER);
        stringBuilder.append(asTextMember("encoding", encoding)); stringBuilder.append(MEMBER_DELIMITER);
        stringBuilder.append(asTextArray("results", results));
        stringBuilder.append(END_OBJECT);
        return stringBuilder.toString();
    }
}
