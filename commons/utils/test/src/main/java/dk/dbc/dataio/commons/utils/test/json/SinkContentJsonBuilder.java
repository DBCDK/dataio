package dk.dbc.dataio.commons.utils.test.json;

import dk.dbc.dataio.commons.types.SinkContent;

public class SinkContentJsonBuilder extends JsonBuilder {
    private String name = "defaultSinkName";
    private String resource = "defaultResource";
    private String description = "defaultDescription";
    private SinkContent.SinkType sinkType = null;
    private String sinkConfig = null;
    private SinkContent.SequenceAnalysisOption sequenceAnalysisOption = SinkContent.SequenceAnalysisOption.ALL;

    public SinkContentJsonBuilder setName(String name) {
        this.name = name;
        return this;
    }

    public SinkContentJsonBuilder setResource(String resource) {
        this.resource = resource;
        return this;
    }

    public SinkContentJsonBuilder setDescription(String description) {
        this.description = description;
        return this;
    }

    public SinkContentJsonBuilder setSinkType(SinkContent.SinkType sinkType) {
        this.sinkType = sinkType;
        return this;
    }

    public SinkContentJsonBuilder setSinkConfig(String sinkConfig) {
        this.sinkConfig = sinkConfig;
        return this;
    }

    public SinkContentJsonBuilder setSequenceAnalysisOption(SinkContent.SequenceAnalysisOption sequenceAnalysisOption) {
        this.sequenceAnalysisOption = sequenceAnalysisOption;
        return this;
    }

    public String build() {
        final StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(START_OBJECT);
        stringBuilder.append(asTextMember("name", name));
        stringBuilder.append(MEMBER_DELIMITER);
        stringBuilder.append(asTextMember("resource", resource));
        stringBuilder.append(MEMBER_DELIMITER);
        stringBuilder.append(asTextMember("description", description));
        stringBuilder.append(MEMBER_DELIMITER);
        stringBuilder.append(asTextMember("sinkType", sinkType == null ? null : sinkType.name()));
        stringBuilder.append(MEMBER_DELIMITER);
        stringBuilder.append(asObjectMember("sinkConfig", sinkConfig));
        stringBuilder.append(MEMBER_DELIMITER);
        stringBuilder.append(asTextMember("sequenceAnalysisOption", sequenceAnalysisOption.name()));
        stringBuilder.append(END_OBJECT);
        return stringBuilder.toString();
    }
}
