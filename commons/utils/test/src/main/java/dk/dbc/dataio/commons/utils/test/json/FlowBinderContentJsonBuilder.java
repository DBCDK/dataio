package dk.dbc.dataio.commons.utils.test.json;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class FlowBinderContentJsonBuilder extends JsonBuilder {
    private String name = "name";
    private String packaging = "packaging";
    private String format = "format";
    private String destination = "destination";
    private String charset = "charset";
    private String description = "description";
    private String recordSplitter = "recordSplitter";
    private Long flowId = 42L;
    private List<Long> submitterIds = new ArrayList<>(Arrays.asList(43L));

    public FlowBinderContentJsonBuilder setCharset(String charset) {
        this.charset = charset;
        return this;
    }

    public FlowBinderContentJsonBuilder setDescription(String description) {
        this.description = description;
        return this;
    }

    public FlowBinderContentJsonBuilder setDestination(String destination) {
        this.destination = destination;
        return this;
    }

    public FlowBinderContentJsonBuilder setFlowId(Long flowId) {
        this.flowId = flowId;
        return this;
    }

    public FlowBinderContentJsonBuilder setFormat(String format) {
        this.format = format;
        return this;
    }

    public FlowBinderContentJsonBuilder setName(String name) {
        this.name = name;
        return this;
    }

    public FlowBinderContentJsonBuilder setPackaging(String packaging) {
        this.packaging = packaging;
        return this;
    }

    public FlowBinderContentJsonBuilder setRecordSplitter(String recordSplitter) {
        this.recordSplitter = recordSplitter;
        return this;
    }

    public FlowBinderContentJsonBuilder setSubmitterIds(List<Long> submitterIds) {
        this.submitterIds = new ArrayList<>(submitterIds);
        return this;
    }

    public String build() {
        final StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(START_OBJECT);
        stringBuilder.append(asTextMember("name", name)); stringBuilder.append(MEMBER_DELIMITER);
        stringBuilder.append(asTextMember("description", description)); stringBuilder.append(MEMBER_DELIMITER);
        stringBuilder.append(asTextMember("packaging", packaging)); stringBuilder.append(MEMBER_DELIMITER);
        stringBuilder.append(asTextMember("format", format)); stringBuilder.append(MEMBER_DELIMITER);
        stringBuilder.append(asTextMember("charset", charset)); stringBuilder.append(MEMBER_DELIMITER);
        stringBuilder.append(asTextMember("destination", destination)); stringBuilder.append(MEMBER_DELIMITER);
        stringBuilder.append(asTextMember("recordSplitter", recordSplitter)); stringBuilder.append(MEMBER_DELIMITER);
        stringBuilder.append(asLongMember("flowId", flowId)); stringBuilder.append(MEMBER_DELIMITER);
        stringBuilder.append(asLongArray("submitterIds", submitterIds));
        stringBuilder.append(END_OBJECT);
        return stringBuilder.toString();
    }
}
