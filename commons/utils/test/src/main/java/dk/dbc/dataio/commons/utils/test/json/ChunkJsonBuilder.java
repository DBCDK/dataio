package dk.dbc.dataio.commons.utils.test.json;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ChunkJsonBuilder extends JsonBuilder {
    private long jobId = 2;
    private long chunkId = 1;
    private String flow = new FlowJsonBuilder().build();
    private List<String> records = new ArrayList<>(Arrays.asList("record"));

    public ChunkJsonBuilder setJobId(long jobId) {
        this.jobId = jobId;
        return this;
    }

    public ChunkJsonBuilder setChunkId(long chunkId) {
        this.chunkId = chunkId;
        return this;
    }

    public ChunkJsonBuilder setFlow(String flow) {
        this.flow = flow;
        return this;
    }

    public ChunkJsonBuilder setRecords(List<String> records) {
        this.records = records;
        return this;
    }

    public String build() {
        final StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(START_OBJECT);
        stringBuilder.append(asLongMember("jobId", jobId)); stringBuilder.append(MEMBER_DELIMITER);
        stringBuilder.append(asLongMember("chunkId", chunkId)); stringBuilder.append(MEMBER_DELIMITER);
        stringBuilder.append(asObjectMember("flow", flow)); stringBuilder.append(MEMBER_DELIMITER);
        stringBuilder.append(asTextArray("records", records));
        stringBuilder.append(END_OBJECT);
        return stringBuilder.toString();
    }
}
