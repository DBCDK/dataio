package dk.dbc.dataio.commons.utils.test.model;

import dk.dbc.dataio.commons.types.FlowBinderContent;
import java.util.Arrays;
import java.util.List;

public class FlowBinderContentBuilder {
    private String name = "flowbinder-name";
    private String description = "flowbinder-description";
    private String packaging = "flowbinder-packaging";
    private String format = "flowbinder-format";
    private String charset = "flowbinder-charset";
    private String destination = "flowbinder-destination";
    private String recordSplitter = "flowbinder-recordsplitter";
    private long flowId = 47L;
    private List<Long> submitterIds = Arrays.asList(78L);
    private long sinkId = 24L;


    public FlowBinderContentBuilder setName(String name) {
        this.name = name;
        return this;
    }

    public FlowBinderContentBuilder setDescription(String description) {
        this.description = description;
        return this;
    }

    public FlowBinderContentBuilder setPackaging(String packaging) {
        this.packaging = packaging;
        return this;
    }

    public FlowBinderContentBuilder setFormat(String format) {
        this.format = format;
        return this;
    }

    public FlowBinderContentBuilder setCharset(String charset) {
        this.charset = charset;
        return this;
    }

    public FlowBinderContentBuilder setDestination(String destination) {
        this.destination = destination;
        return this;
    }

    public FlowBinderContentBuilder setRecordSplitter(String recordSplitter) {
        this.recordSplitter = recordSplitter;
        return this;
    }

    public FlowBinderContentBuilder setFlowId(long flowId) {
        this.flowId = flowId;
        return this;
    }

    public FlowBinderContentBuilder setSubmitterIds(List<Long> submitterIds) {
        this.submitterIds = submitterIds;
        return this;
    }

    public FlowBinderContentBuilder setSinkId(long sinkId) {
        this.sinkId = sinkId;
        return this;
    }

    public FlowBinderContent build() {
        return new FlowBinderContent(name, description, packaging, format, charset, destination, recordSplitter, flowId, submitterIds, sinkId);
    }

}
