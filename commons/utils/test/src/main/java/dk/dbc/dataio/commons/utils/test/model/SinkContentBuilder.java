package dk.dbc.dataio.commons.utils.test.model;

import dk.dbc.dataio.commons.types.SinkConfig;
import dk.dbc.dataio.commons.types.SinkContent;

public class SinkContentBuilder {
    private String name = "name";
    private String queue = "queue";
    private String description = "description";
    private SinkContent.SinkType sinkType = SinkContent.SinkType.DUMMY;
    private SinkConfig sinkConfig = null;
    private SinkContent.SequenceAnalysisOption sequenceAnalysisOption = SinkContent.SequenceAnalysisOption.ALL;

    public SinkContentBuilder setName(String name) {
        this.name = name;
        return this;
    }

    public SinkContentBuilder setDescription(String description) {
        this.description = description;
        return this;
    }

    public SinkContentBuilder setSinkType(SinkContent.SinkType sinkType) {
        this.sinkType = sinkType;
        return this;
    }

    public SinkContentBuilder setSinkConfig(SinkConfig sinkConfig) {
        this.sinkConfig = sinkConfig;
        return this;
    }

    public SinkContentBuilder setQueue(String queue) {
        this.queue = queue;
        return this;
    }

    public SinkContent build() {
        return new SinkContent(name, queue, description, sinkType, sinkConfig, sequenceAnalysisOption);
    }
}
