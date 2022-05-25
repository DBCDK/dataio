package dk.dbc.dataio.gui.client.modelBuilders;

import dk.dbc.dataio.commons.types.RecordSplitterConstants;
import dk.dbc.dataio.gui.client.model.FlowBinderModel;
import dk.dbc.dataio.gui.client.model.FlowModel;
import dk.dbc.dataio.gui.client.model.SinkModel;
import dk.dbc.dataio.gui.client.model.SubmitterModel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class FlowBinderModelBuilder {

    private long id = 23L;
    private long version = 1L;
    private String name = "name";
    private String description = "description";
    private String packaging = "flowbinder-packaging";
    private String format = "flowbinder-format";
    private String charset = "flowbinder-charset";
    private String destination = "flowbinder-destination";
    private Integer priority = null;
    private String recordSplitter = RecordSplitterConstants.RecordSplitter.XML.name();
    private FlowModel flowModel = new FlowModelBuilder().build();
    private List<SubmitterModel> submitterModels = new ArrayList<>(Arrays.asList(new SubmitterModelBuilder().build()));
    private SinkModel sinkModel = new SinkModelBuilder().build();
    private String queueProvider = "queue-provider";

    public FlowBinderModelBuilder setId(long id) {
        this.id = id;
        return this;
    }

    public FlowBinderModelBuilder setVersion(long version) {
        this.version = version;
        return this;
    }

    public FlowBinderModelBuilder setName(String name) {
        this.name = name;
        return this;
    }

    public FlowBinderModelBuilder setDescription(String description) {
        this.description = description;
        return this;
    }

    public FlowBinderModelBuilder setPackaging(String packaging) {
        this.packaging = packaging;
        return this;
    }

    public FlowBinderModelBuilder setFormat(String format) {
        this.format = format;
        return this;
    }

    public FlowBinderModelBuilder setCharset(String charset) {
        this.charset = charset;
        return this;
    }

    public FlowBinderModelBuilder setDestination(String destination) {
        this.destination = destination;
        return this;
    }

    public FlowBinderModelBuilder setPriority(Integer priority) {
        this.priority = priority;
        return this;
    }

    public FlowBinderModelBuilder setRecordSplitter(RecordSplitterConstants.RecordSplitter recordSplitter) {
        this.recordSplitter = recordSplitter.name();
        return this;
    }

    public FlowBinderModelBuilder setFlowModel(FlowModel flowModel) {
        this.flowModel = flowModel;
        return this;
    }

    public FlowBinderModelBuilder setSubmitterModels(List<SubmitterModel> submitterModels) {
        this.submitterModels = submitterModels;
        return this;
    }

    public FlowBinderModelBuilder setSinkModel(SinkModel sinkModel) {
        this.sinkModel = sinkModel;
        return this;
    }

    public FlowBinderModelBuilder setQueueProvider(String queueProvider) {
        this.queueProvider = queueProvider;
        return this;
    }

    public FlowBinderModel build() {
        return new FlowBinderModel(id, version, name, description, packaging, format, charset, destination, priority, recordSplitter, flowModel, submitterModels, sinkModel, queueProvider);
    }
}
