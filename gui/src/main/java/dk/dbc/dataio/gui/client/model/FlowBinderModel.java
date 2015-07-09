package dk.dbc.dataio.gui.client.model;

import dk.dbc.dataio.gui.client.util.Format;

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
    private boolean sequenceAnalysis;
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
        this.sequenceAnalysis = true;
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
     * @param sequenceAnalysis boolean for telling whether sequence analysis is on or off for the flowbinder
     * @param flowModel The flow model of the Flow Binder
     * @param submitterModels The submitter models for the Flow Binder
     * @param sinkModel The sink model of the Flow Binder
     */
    public FlowBinderModel(long id, long version, String name, String description, String packaging, String format, String charset, String destination, String recordSplitter, boolean sequenceAnalysis, FlowModel flowModel, List<SubmitterModel> submitterModels, SinkModel sinkModel) {
        super(id, version);
        this.name = name;
        this.description = description;
        this.packaging = packaging;
        this.format = format;
        this.charset = charset;
        this.destination = destination;
        this.recordSplitter = recordSplitter;
        this.sequenceAnalysis = sequenceAnalysis;
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
        this.sequenceAnalysis = model.getSequenceAnalysis();
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
     * Get the sequence analysis boolean of the Flow Binder
     *
     * @return The sequence analysis boolean of the Flow Binder
     */
    public boolean getSequenceAnalysis() {
        return sequenceAnalysis;
    }

    /**
     * Set the sequence analysis boolean of the Flow Binder
     *
     * @param sequenceAnalysis The sequence analysis boolean of the Flow Binder
     */
    public void setSequenceAnalysis(boolean sequenceAnalysis) {
        this.sequenceAnalysis = sequenceAnalysis;
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
     * @return true if no empty String values were found, otherwise false
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
                || submitterModels == null
                || submitterModels.isEmpty()
                || sinkModel == null;
    }

    /**
     * Checks if the flow binder name contains illegal characters.
     * A-Å, 0-9, - (minus), + (plus), _ (underscore) and space is valid
     * @return a list containing illegal characters found. Empty list if none found.
     */
    public List<String> getDataioPatternMatches() {
        return Format.getDataioPatternMatches(name);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FlowBinderModel)) return false;

        FlowBinderModel that = (FlowBinderModel) o;

        if (sequenceAnalysis != that.sequenceAnalysis) return false;
        if (name != null ? !name.equals(that.name) : that.name != null) return false;
        if (description != null ? !description.equals(that.description) : that.description != null) return false;
        if (packaging != null ? !packaging.equals(that.packaging) : that.packaging != null) return false;
        if (format != null ? !format.equals(that.format) : that.format != null) return false;
        if (charset != null ? !charset.equals(that.charset) : that.charset != null) return false;
        if (destination != null ? !destination.equals(that.destination) : that.destination != null) return false;
        if (recordSplitter != null ? !recordSplitter.equals(that.recordSplitter) : that.recordSplitter != null)
            return false;
        if (flowModel != null ? !flowModel.equals(that.flowModel) : that.flowModel != null) return false;
        if (submitterModels != null ? !submitterModels.equals(that.submitterModels) : that.submitterModels != null)
            return false;
        return !(sinkModel != null ? !sinkModel.equals(that.sinkModel) : that.sinkModel != null);

    }

    @Override
    public int hashCode() {
        int result = name != null ? name.hashCode() : 0;
        result = 31 * result + (description != null ? description.hashCode() : 0);
        result = 31 * result + (packaging != null ? packaging.hashCode() : 0);
        result = 31 * result + (format != null ? format.hashCode() : 0);
        result = 31 * result + (charset != null ? charset.hashCode() : 0);
        result = 31 * result + (destination != null ? destination.hashCode() : 0);
        result = 31 * result + (recordSplitter != null ? recordSplitter.hashCode() : 0);
        result = 31 * result + (sequenceAnalysis ? 1 : 0);
        result = 31 * result + (flowModel != null ? flowModel.hashCode() : 0);
        result = 31 * result + (submitterModels != null ? submitterModels.hashCode() : 0);
        result = 31 * result + (sinkModel != null ? sinkModel.hashCode() : 0);
        return result;
    }
}
