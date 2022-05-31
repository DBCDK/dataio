package dk.dbc.dataio.gui.client.model;

import dk.dbc.dataio.commons.types.Priority;
import dk.dbc.dataio.gui.client.util.Format;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class FlowBinderModel extends GenericBackendModel {
    private String name;
    private String description;
    private String packaging;
    private String format;
    private String charset;
    private String destination;
    private Integer priority;
    private String recordSplitter;
    private FlowModel flowModel;
    private List<SubmitterModel> submitterModels;
    private SinkModel sinkModel;
    private String queueProvider;


    public FlowBinderModel() {
        this(0L, 0L, "", "", "", "", "", "", Priority.NORMAL.getValue(), "", new FlowModel(), new ArrayList<>(), new SinkModel(), "");
    }

    /**
     * @param id              The ID of the Flow Binder
     * @param version         The version of the Flow Binder
     * @param name            The name of the Flow Binder
     * @param description     The description for the Flow Binder
     * @param packaging       The packaging of the Flow Binder
     * @param format          The format of the Flow Binder
     * @param charset         The charset for the Flow Binder
     * @param destination     The destination of the Flow Binder
     * @param priority        The priority of the Flow Binder
     * @param recordSplitter  The record splitter of the Flow Binder
     * @param flowModel       The flow model of the Flow Binder
     * @param submitterModels The submitter models for the Flow Binder
     * @param sinkModel       The sink model of the Flow Binder
     * @param queueProvider   The Queue Provider for the Flow Binder
     */
    public FlowBinderModel(long id, long version, String name, String description, String packaging, String format, String charset, String destination, Integer priority, String recordSplitter, FlowModel flowModel, List<SubmitterModel> submitterModels, SinkModel sinkModel, String queueProvider) {
        super(id, version);
        this.name = name;
        this.description = description;
        this.packaging = packaging;
        this.format = format;
        this.charset = charset;
        this.destination = destination;
        this.priority = priority;
        this.recordSplitter = recordSplitter;
        this.flowModel = flowModel;
        this.submitterModels = submitterModels;
        this.sinkModel = sinkModel;
        this.queueProvider = queueProvider;
    }

    /**
     * @param model The model to clone
     */
    public FlowBinderModel(FlowBinderModel model) {
        this(model.getId(),
                model.getVersion(),
                model.getName(),
                model.getDescription(),
                model.getPackaging(),
                model.getFormat(),
                model.getCharset(),
                model.getDestination(),
                model.getPriority(),
                model.getRecordSplitter(),
                model.getFlowModel(),  // Please note, that the flow itself is not cloned
                model.getSubmitterModels(),  // Please note, that the submitters themselves are not cloned
                model.getSinkModel(),  // Please note, that the sink itself is not cloned
                model.getQueueProvider());
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
     * Get the priority of the Flow Binder
     *
     * @return The priority of the Flow Binder
     */
    public Integer getPriority() {
        return priority;
    }

    /**
     * Set the priority of the Flow Binder
     *
     * @param priority The priority of the Flow Binder
     */
    public void setPriority(Integer priority) {
        this.priority = priority;
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
     * Get queue provider of Flow Binder
     *
     * @return Queue provider of Flow Binder
     */
    public String getQueueProvider() {
        return queueProvider;
    }

    /**
     * Set queue provider of Flow Binder
     *
     * @param queueProvider Queue provider of Flow Binder
     */
    public void setQueueProvider(String queueProvider) {
        this.queueProvider = queueProvider;
    }

    /**
     * Checks for empty String values
     *
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
                || sinkModel == null
                || requiresQueueProvider() && (queueProvider == null || queueProvider.isEmpty());
    }

    public boolean requiresQueueProvider() {
        if (sinkModel != null) {
            switch (sinkModel.getSinkType()) {
                case OPENUPDATE:
                case DPF:
                    return true;
            }
        }
        return false;
    }

    public List<String> getAvailableQueueProviders() {
        if (requiresQueueProvider()) {
            switch (sinkModel.getSinkType()) {
                case OPENUPDATE:
                    return sinkModel.getOpenUpdateAvailableQueueProviders();
                case DPF:
                    return sinkModel.getDpfUpdateServiceAvailableQueueProviders();
            }
        }
        return Collections.emptyList();
    }

    /**
     * Checks if the flow binder name contains illegal characters.
     * A-Å, 0-9, - (minus), + (plus), _ (underscore) and space is valid
     *
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

        if (name != null ? !name.equals(that.name) : that.name != null) return false;
        if (description != null ? !description.equals(that.description) : that.description != null) return false;
        if (packaging != null ? !packaging.equals(that.packaging) : that.packaging != null) return false;
        if (format != null ? !format.equals(that.format) : that.format != null) return false;
        if (charset != null ? !charset.equals(that.charset) : that.charset != null) return false;
        if (destination != null ? !destination.equals(that.destination) : that.destination != null) return false;
        if (priority != null ? !priority.equals(that.priority) : that.priority != null) return false;
        if (recordSplitter != null ? !recordSplitter.equals(that.recordSplitter) : that.recordSplitter != null)
            return false;
        if (flowModel != null ? !flowModel.equals(that.flowModel) : that.flowModel != null) return false;
        if (submitterModels != null ? !submitterModels.equals(that.submitterModels) : that.submitterModels != null)
            return false;
        if (sinkModel != null ? !sinkModel.equals(that.sinkModel) : that.sinkModel != null) return false;
        return queueProvider != null ? queueProvider.equals(that.queueProvider) : that.queueProvider == null;
    }

    @Override
    public int hashCode() {
        int result = name != null ? name.hashCode() : 0;
        result = 31 * result + (description != null ? description.hashCode() : 0);
        result = 31 * result + (packaging != null ? packaging.hashCode() : 0);
        result = 31 * result + (format != null ? format.hashCode() : 0);
        result = 31 * result + (charset != null ? charset.hashCode() : 0);
        result = 31 * result + (destination != null ? destination.hashCode() : 0);
        result = 31 * result + (priority != null ? priority.hashCode() : 0);
        result = 31 * result + (recordSplitter != null ? recordSplitter.hashCode() : 0);
        result = 31 * result + (flowModel != null ? flowModel.hashCode() : 0);
        result = 31 * result + (submitterModels != null ? submitterModels.hashCode() : 0);
        result = 31 * result + (sinkModel != null ? sinkModel.hashCode() : 0);
        result = 31 * result + (queueProvider != null ? queueProvider.hashCode() : 0);
        return result;
    }
}
