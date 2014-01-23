package dk.dbc.dataio.commons.utils.test.json;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ChunkJsonBuilder extends JsonBuilder {
    private long id = 1;
    private String flow = new FlowJsonBuilder().build();
    private List<String> records = new ArrayList<>(Arrays.asList("record"));

    public ChunkJsonBuilder setId(long chunkId) {
        this.id = chunkId;
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
        stringBuilder.append(asLongMember("id", id)); stringBuilder.append(MEMBER_DELIMITER);
        stringBuilder.append(asObjectMember("flow", flow)); stringBuilder.append(MEMBER_DELIMITER);
        stringBuilder.append(asTextArray("records", records));
        stringBuilder.append(END_OBJECT);
        return stringBuilder.toString();
    }
}
