package dk.dbc.dataio.commons.utils.test.json;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ChunkJsonBuilder extends JsonBuilder {
    private long jobId = 2;
    private long chunkId = 1;
    private String flow = new FlowJsonBuilder().build();
    private String supplementaryProcessData = new SupplementaryProcessDataJsonBuilder().build();
    private List<String> items = new ArrayList<>(Arrays.asList(new ChunkItemJsonBuilder().build()));

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

    public ChunkJsonBuilder setSupplementaryProcessData(String supplementaryProcessData) {
        this.supplementaryProcessData = supplementaryProcessData;
        return this;
    }

    public ChunkJsonBuilder setItems(List<String> items) {
        this.items = items;
        return this;
    }

    public String build() {
        final StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(START_OBJECT);
        stringBuilder.append(asLongMember("jobId", jobId)); stringBuilder.append(MEMBER_DELIMITER);
        stringBuilder.append(asLongMember("chunkId", chunkId)); stringBuilder.append(MEMBER_DELIMITER);
        stringBuilder.append(asObjectMember("flow", flow)); stringBuilder.append(MEMBER_DELIMITER);
        stringBuilder.append(asObjectMember("supplementaryProcessData", supplementaryProcessData)); stringBuilder.append(MEMBER_DELIMITER);
        stringBuilder.append(asObjectArray("items", items));
        stringBuilder.append(END_OBJECT);
        return stringBuilder.toString();
    }
}
