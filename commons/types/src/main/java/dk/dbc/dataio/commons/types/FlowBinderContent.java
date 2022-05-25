package dk.dbc.dataio.commons.types;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import dk.dbc.invariant.InvariantUtil;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * FlowBinderContent DTO class.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FlowBinderContent implements Serializable {
    private static final long serialVersionUID = 1106844598199379043L;

    private final String name;
    private final String description;
    private final String packaging;
    private final String format;
    private final String charset;
    private final String destination;
    @JsonProperty
    private final Priority priority;
    private final RecordSplitterConstants.RecordSplitter recordSplitter;
    private final long flowId;
    private final List<Long> submitterIds;
    private final long sinkId;
    private final String queueProvider;

    /**
     * Class constructor
     *
     * @param name           flowbinder name
     * @param description    flowbinder description
     * @param packaging      flowbinder packaging (rammeformat)
     * @param format         flowbinder format (indholdsformat)
     * @param charset        flowbinder character set
     * @param destination    flow binder destination
     * @param priority       priority
     * @param recordSplitter flow binder record splitter
     * @param flowId         id of flow attached to this flowbinder
     * @param submitterIds   ids of submitters attached to this flowbinder
     * @param sinkId         id of sink attached to this flowbinder
     * @param queueProvider  the queue provider to use for this flow binder
     * @throws NullPointerException     if given null-valued argument
     * @throws IllegalArgumentException if given empty valued String or List argument
     */

    @JsonCreator
    public FlowBinderContent(@JsonProperty("name") String name,
                             @JsonProperty("description") String description,
                             @JsonProperty("packaging") String packaging,
                             @JsonProperty("format") String format,
                             @JsonProperty("charset") String charset,
                             @JsonProperty("destination") String destination,
                             @JsonProperty("priority") Priority priority,
                             @JsonProperty("recordSplitter") RecordSplitterConstants.RecordSplitter recordSplitter,
                             @JsonProperty("flowId") long flowId,
                             @JsonProperty("submitterIds") List<Long> submitterIds,
                             @JsonProperty("sinkId") long sinkId,
                             @JsonProperty("queueProvider") String queueProvider) {

        this.name = InvariantUtil.checkNotNullNotEmptyOrThrow(name, "name");
        this.description = InvariantUtil.checkNotNullNotEmptyOrThrow(description, "description");
        this.packaging = InvariantUtil.checkNotNullNotEmptyOrThrow(packaging, "packaging");
        this.format = InvariantUtil.checkNotNullNotEmptyOrThrow(format, "format");
        this.charset = InvariantUtil.checkNotNullNotEmptyOrThrow(charset, "charset");
        this.destination = InvariantUtil.checkNotNullNotEmptyOrThrow(destination, "destination");
        this.priority = priority;
        this.recordSplitter = InvariantUtil.checkNotNullOrThrow(recordSplitter, "recordSplitter");
        this.flowId = InvariantUtil.checkLowerBoundOrThrow(flowId, "flowId", Constants.PERSISTENCE_ID_LOWER_BOUND);
        this.submitterIds = new ArrayList<>(InvariantUtil.checkNotNullOrThrow(submitterIds, "submitterIds"));
        if (this.submitterIds.size() == 0) {
            throw new IllegalArgumentException("submitterIds can not be empty");
        }
        this.sinkId = InvariantUtil.checkLowerBoundOrThrow(sinkId, "sinkId", Constants.PERSISTENCE_ID_LOWER_BOUND);
        this.queueProvider = queueProvider;  // No invariant check due to backwards compatibility issues
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getPackaging() {
        return packaging;
    }

    public String getFormat() {
        return format;
    }

    public String getCharset() {
        return charset;
    }

    public String getDestination() {
        return destination;
    }

    @JsonIgnore
    public Priority getPriority() {
        if (priority == null) {
            return Priority.NORMAL;
        } else {
            return priority;
        }
    }

    public RecordSplitterConstants.RecordSplitter getRecordSplitter() {
        return recordSplitter;
    }

    public long getFlowId() {
        return flowId;
    }

    public List<Long> getSubmitterIds() {
        return new ArrayList<>(submitterIds);
    }

    public long getSinkId() {
        return sinkId;
    }

    public String getQueueProvider() {
        return queueProvider;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FlowBinderContent)) return false;

        FlowBinderContent that = (FlowBinderContent) o;

        if (flowId != that.flowId) return false;
        if (sinkId != that.sinkId) return false;
        if (name != null ? !name.equals(that.name) : that.name != null) return false;
        if (description != null ? !description.equals(that.description) : that.description != null) return false;
        if (packaging != null ? !packaging.equals(that.packaging) : that.packaging != null) return false;
        if (format != null ? !format.equals(that.format) : that.format != null) return false;
        if (charset != null ? !charset.equals(that.charset) : that.charset != null) return false;
        if (destination != null ? !destination.equals(that.destination) : that.destination != null) return false;
        if (priority != that.priority) return false;
        if (recordSplitter != that.recordSplitter) return false;
        if (submitterIds != null ? !submitterIds.equals(that.submitterIds) : that.submitterIds != null) return false;
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
        result = 31 * result + (int) (flowId ^ (flowId >>> 32));
        result = 31 * result + (submitterIds != null ? submitterIds.hashCode() : 0);
        result = 31 * result + (int) (sinkId ^ (sinkId >>> 32));
        result = 31 * result + (queueProvider != null ? queueProvider.hashCode() : 0);
        return result;
    }
}
