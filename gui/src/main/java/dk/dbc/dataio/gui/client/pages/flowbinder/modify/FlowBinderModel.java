package dk.dbc.dataio.gui.client.pages.flowbinder.modify;

import dk.dbc.dataio.gui.client.model.GenericBackendModel;
import dk.dbc.dataio.gui.client.pages.flow.modify.FlowModel;
import dk.dbc.dataio.gui.client.pages.sink.modify.SinkModel;
import dk.dbc.dataio.gui.client.pages.submitter.modify.SubmitterModel;

import java.util.ArrayList;
import java.util.List;

public class FlowBinderModel extends GenericBackendModel {
    private String name;
    private String description;
    private String packaging;
    private String format;
    private String charset;
    private String destination;
    private String recordSplitter;
    private FlowModel flowModel;
    private List<SubmitterModel> submitterModels;
    private SinkModel sinkModel;


    public FlowBinderModel() {
        super(0L, 0L);
        this.name = "";
        this.description = "";
        this.packaging = "";
        this.format = "";
        this.charset = "";
        this.destination = "";
        this.recordSplitter = "";
        this.flowModel = new FlowModel();
        this.submitterModels = new ArrayList<SubmitterModel>();
        this.sinkModel = new SinkModel();
    }

    /**
     * @param id The ID of the Flow Binder
     * @param version The version of the Flow Binder
     * @param name The name of the Flow Binder
     * @param description The description for the Flow Binder
     * @param packaging The packaging of the Flow Binder
     * @param format The format of the Flow Binder
     * @param charset The charset for the Flow Binder
     * @param destination The destination of the Flow Binder
     * @param recordSplitter The record splitter of the Flow Binder
     * @param flowModel The flow model of the Flow Binder
     * @param submitterModels The submitter models for the Flow Binder
     * @param sinkModel The sink model of the Flow Binder
     */
    public FlowBinderModel(long id, long version, String name, String description, String packaging, String format, String charset, String destination, String recordSplitter, FlowModel flowModel, List<SubmitterModel> submitterModels, SinkModel sinkModel) {
        super(id, version);
        this.name = name;
        this.description = description;
        this.packaging = packaging;
        this.format = format;
        this.charset = charset;
        this.destination = destination;
        this.recordSplitter = recordSplitter;
        this.flowModel = flowModel;
        this.submitterModels = submitterModels;
        this.sinkModel = sinkModel;
    }

    /**
     * @param model The model to clone
     */
    public FlowBinderModel(FlowBinderModel model) {
        super(model.getId(), model.getVersion());
        this.name = model.getName();
        this.description = model.getDescription();
        this.packaging = model.getPackaging();
        this.format = model.getFormat();
        this.charset = model.getCharset();
        this.destination = model.getDestination();
        this.recordSplitter = model.getRecordSplitter();
        this.flowModel = model.getFlowModel();  // Please note, that the flow itself is not cloned
        this.submitterModels = model.getSubmitterModels();  // Please note, that the submitters themselves are not cloned
        this.sinkModel = model.getSinkModel();  // Please note, that the sink itself is not cloned
    }

    /**
     * Get name of Flow Binder
     *
     * @return Name of Flow Binder
     */
    public String getName() {
        return name;
    }

    /**
     * Set name of Flow Binder
     *
     * @param name Name of Flow Binder
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Get the description of the Flow Binder
     *
     * @return The description of the Flow Binder
     */
    public String getDescription() {
        return description;
    }

    /**
     * Set the description of the Flow Binder
     *
     * @param description The description of the Flow Binder
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Packaging of the Flow Binder
     *
     * @return Packaging of the Flow Binder
     */
    public String getPackaging() {
        return packaging;
    }

    /**
     * Set the packaging of the Flow Binder
     *
     * @param packaging The packaging of the Flow Binder
     */
    public void setPackaging(String packaging) {
        this.packaging = packaging;
    }

    /**
     * Get the format of the Flow Binder
     *
     * @return The format of the Flow Binder
     */
    public String getFormat() {
        return format;
    }

    /**
     * Set the format of the Flow Binder
     *
     * @param format The format of the Flow Binder
     */
    public void setFormat(String format) {
        this.format = format;
    }

    /**
     * Get the charset of the Flow Binder
     *
     * @return The charset of the Flow Binder
     */
    public String getCharset() {
        return charset;
    }

    /**
     * Set the charset of the Flow Binder
     *
     * @param charset The charset of the Flow Binder
     */
    public void setCharset(String charset) {
        this.charset = charset;
    }

    /**
     * Get the destination of the Flow Binder
     *
     * @return The destination of the Flow Binder
     */
    public String getDestination() {
        return destination;
    }

    /**
     * Set the destination of the Flow Binder
     *
     * @param destination The destination of the Flow Binder
     */
    public void setDestination(String destination) {
        this.destination = destination;
    }

    /**
     * Get the record splitter of the Flow Binder
     *
     * @return The record splitter of the Flow Binder
     */
    public String getRecordSplitter() {
        return recordSplitter;
    }

    /**
     * Set the record splitter of the Flow Binder
     *
     * @param recordSplitter The record splitter of the Flow Binder
     */
    public void setRecordSplitter(String recordSplitter) {
        this.recordSplitter = recordSplitter;
    }

    /**
     * Get the flow model of the Flow Binder
     *
     * @return The flow model of the Flow Binder
     */
    public FlowModel getFlowModel() {
        return flowModel;
    }

    /**
     * Set the flow model of the Flow Binder
     *
     * @param flowModel The flow model of the Flow Binder
     */
    public void setFlowModel(FlowModel flowModel) {
        this.flowModel = flowModel;
    }

    /**
     * Get all submitter models of the Flow Binder
     *
     * @return All submitter models of the Flow Binder
     */
    public List<SubmitterModel> getSubmitterModels() {
        return submitterModels;
    }

    /**
     * Set all submitter models of the Flow Binder
     *
     * @param submitterModels All submitter models of the Flow Binder
     */
    public void setSubmitterModels(List<SubmitterModel> submitterModels) {
        this.submitterModels = submitterModels;
    }

    /**
     * Get the sink model of the Flow Binder
     *
     * @return The sink model of the Flow Binder
     */
    public SinkModel getSinkModel() {
        return sinkModel;
    }

    /**
     * Set the sink model of the Flow Binder
     *
     * @param sinkModel The sink model of the Flow Binder
     */
    public void setSinkModel(SinkModel sinkModel) {
        this.sinkModel = sinkModel;
    }

    /**
     * Checks for empty String values
     */
    public boolean isInputFieldsEmpty() {
        return name == null
                || name.isEmpty()
                || description == null
                || description.isEmpty()
                || packaging == null
                || packaging.isEmpty()
                || format == null
                || format.isEmpty()
                || charset == null
                || charset.isEmpty()
                || destination == null
                || destination.isEmpty()
                || recordSplitter == null
                || recordSplitter.isEmpty()
                || flowModel == null
                || flowModel.isInputFieldsEmpty()
                || submitterModels == null
                || submitterModels.isEmpty()
                || sinkModel == null
                || sinkModel.isInputFieldsEmpty();
    }

}
