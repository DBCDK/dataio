package dk.dbc.dataio.commons.types;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import dk.dbc.dataio.commons.utils.invariant.InvariantUtil;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * FlowBinderContent DTO class.
 */
public class FlowBinderContent implements Serializable {
    private static final long serialVersionUID = 1106844598199379043L;

    private final String name;
    private final String description;
    private final String packaging;
    private final String format;
    private final String charset;
    private final String destination;
    private final String recordSplitter;
    private final boolean sequenceAnalysis;
    private final long flowId;
    private final List<Long> submitterIds;
    private final long sinkId;

    /**
     * Class constructor
     *
     * @param name flowbinder name
     * @param description flowbinder description
     * @param packaging flowbinder packaging (rammeformat)
     * @param format flowbinder format (indholdsformat)
     * @param charset flowbinder character set
     * @param destination flow binder destination
     * @param recordSplitter flow binder record splitter
     * @param sequenceAnalysis boolean for telling whether sequence analysis is on or off for the flowbinder.
     * @param flowId id of flow attached to this flowbinder
     * @param submitterIds ids of submitters attached to this flowbinder
     *
     * @throws NullPointerException if given null-valued argument
     * @throws IllegalArgumentException if given empty valued String or List argument
     */

    @JsonCreator
    public FlowBinderContent(@JsonProperty("name") String name,
                             @JsonProperty("description") String description,
                             @JsonProperty("packaging") String packaging,
                             @JsonProperty("format") String format,
                             @JsonProperty("charset") String charset,
                             @JsonProperty("destination") String destination,
                             @JsonProperty("recordSplitter") String recordSplitter,
                             @JsonProperty("sequenceAnalysis") boolean sequenceAnalysis,
                             @JsonProperty("flowId") long flowId,
                             @JsonProperty("submitterIds") List<Long> submitterIds,
                             @JsonProperty("sinkId") long sinkId) {

        this.name = InvariantUtil.checkNotNullNotEmptyOrThrow(name, "name");
        this.description = InvariantUtil.checkNotNullNotEmptyOrThrow(description, "description");
        this.packaging = InvariantUtil.checkNotNullNotEmptyOrThrow(packaging, "packaging");
        this.format = InvariantUtil.checkNotNullNotEmptyOrThrow(format, "format");
        this.charset = InvariantUtil.checkNotNullNotEmptyOrThrow(charset, "charset");
        this.destination = InvariantUtil.checkNotNullNotEmptyOrThrow(destination, "destination");
        this.recordSplitter = InvariantUtil.checkNotNullNotEmptyOrThrow(recordSplitter, "recordSplitter");
        this.sequenceAnalysis = sequenceAnalysis;
        this.flowId = InvariantUtil.checkLowerBoundOrThrow(flowId, "flowId", Constants.PERSISTENCE_ID_LOWER_BOUND);
        this.submitterIds = new ArrayList<Long>(InvariantUtil.checkNotNullOrThrow(submitterIds, "submitterIds"));
        this.sinkId = InvariantUtil.checkLowerBoundOrThrow(sinkId, "sinkId", Constants.PERSISTENCE_ID_LOWER_BOUND);
        if (this.submitterIds.size() == 0) {
            throw new IllegalArgumentException("submitterIds can not be empty");
        }
    }

    public String getCharset() {
        return charset;
    }

    public String getDescription() {
        return description;
    }

    public String getDestination() {
        return destination;
    }

    public long getFlowId() {
        return flowId;
    }

    public String getFormat() {
        return format;
    }

    public String getName() {
        return name;
    }

    public String getPackaging() {
        return packaging;
    }

    public String getRecordSplitter() {
        return recordSplitter;
    }

    public boolean getSequenceAnalysis() {
        return sequenceAnalysis;
    }

    public long getSinkId() {
        return sinkId;
    }

    public List<Long> getSubmitterIds() {
        return new ArrayList<Long>(submitterIds);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FlowBinderContent)) return false;

        FlowBinderContent that = (FlowBinderContent) o;

        if (sequenceAnalysis != that.sequenceAnalysis) return false;
        if (flowId != that.flowId) return false;
        if (sinkId != that.sinkId) return false;
        if (!name.equals(that.name)) return false;
        if (!description.equals(that.description)) return false;
        if (!packaging.equals(that.packaging)) return false;
        if (!format.equals(that.format)) return false;
        if (!charset.equals(that.charset)) return false;
        if (!destination.equals(that.destination)) return false;
        if (!recordSplitter.equals(that.recordSplitter)) return false;
        return submitterIds.equals(that.submitterIds);

    }

    @Override
    public int hashCode() {
        int result = name.hashCode();
        result = 31 * result + description.hashCode();
        result = 31 * result + packaging.hashCode();
        result = 31 * result + format.hashCode();
        result = 31 * result + charset.hashCode();
        result = 31 * result + destination.hashCode();
        result = 31 * result + recordSplitter.hashCode();
        result = 31 * result + (sequenceAnalysis ? 1 : 0);
        result = 31 * result + (int) (flowId ^ (flowId >>> 32));
        result = 31 * result + submitterIds.hashCode();
        result = 31 * result + (int) (sinkId ^ (sinkId >>> 32));
        return result;
    }
}
