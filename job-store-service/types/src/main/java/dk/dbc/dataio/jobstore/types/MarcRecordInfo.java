package dk.dbc.dataio.jobstore.types;


import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import dk.dbc.dataio.commons.types.SinkContent;

import java.util.Set;

/**
 * This class contains information about a bibliographic record
 * extended with information deduced from the fact that we know it is a MARC record
 */
public class MarcRecordInfo extends RecordInfo {
    public enum RecordType {
        STANDALONE, HEAD, SECTION, VOLUME
    }

    /**
     * Shared correlationKey for every head, section and volume record, regardless of
     * which hierarchy (i.e. which physical head record) they actually belong to (see
     * {@link #getCorrelationKey()}).
     * <p>
     * This is a deliberate simplification. Correctly serializing per-hierarchy would
     * require the correlationKey to be the head record's own id, which in turn would
     * require every section/volume record to resolve which head it belongs to, a
     * section-to-head lookup that today's MARC parsing does not perform and that this
     * design explicitly avoids (docs/chunk-scheduling-redesign.md, "Record Identity —
     * correlationKey"). Using one constant key for all hierarchy records instead
     * serializes delivery of every hierarchy on a given sink, one record at a time.
     * This is an accepted throughput trade-off, not a correctness compromise. Hierarchy
     * records are a small fraction of total workload, and per-hierarchy grouping can be
     * introduced later (switching this constant for the head record's id) without any
     * <i>protocol</i> change, should the single group ever become a measured bottleneck.
     */
    public static final String HIERARCHY_CORRELATION_KEY = "__hierarchy__";

    private final RecordType type;
    private final boolean delete;
    private final String parentRelation;

    /**
     * constructor
     *
     * @param id             identifier of marc record
     * @param type           type of marc record
     * @param isDelete       flag indicating if marc record is delete marked
     * @param parentRelation identifier of marc record parent, can be null or empty
     */
    @JsonCreator
    public MarcRecordInfo(
            @JsonProperty("id") String id,
            @JsonProperty("type") RecordType type,
            @JsonProperty("delete") boolean isDelete,
            @JsonProperty("parentRelation") String parentRelation) {
        super(id);
        this.type = type;
        this.delete = isDelete;
        if (parentRelation != null) {
            parentRelation = parentRelation.trim();
            if (parentRelation.isEmpty()) {
                parentRelation = null;
            }
        }
        this.parentRelation = parentRelation;
    }

    @Override
    public Set<String> getKeys(SinkContent.SequenceAnalysisOption sequenceAnalysisOption) {
        final Set<String> keys = super.getKeys(sequenceAnalysisOption);
        if (sequenceAnalysisOption == SinkContent.SequenceAnalysisOption.ALL && parentRelation != null) {
            keys.add(parentRelation);
        }
        return keys;
    }

    public boolean isDelete() {
        return delete;
    }

    public RecordType getType() {
        return type;
    }

    public String getParentRelation() {
        return parentRelation;
    }

    @JsonIgnore
    public boolean isHead() {
        return type == RecordType.HEAD;
    }

    @JsonIgnore
    public boolean isSection() {
        return type == RecordType.SECTION;
    }

    @JsonIgnore
    public boolean isVolume() {
        return type == RecordType.VOLUME;
    }

    public boolean hasParentRelation() {
        return parentRelation != null;
    }

    /**
     * Derives the correlationKey primarily from the record's own {@code type}:
     * <ul>
     *     <li>{@link RecordType#STANDALONE} → the record's own {@code id}, same as the
     *     {@link RecordInfo} base behaviour. A standalone record is serialized only
     *     against older/newer versions of itself</li>
     *     <li>{@link RecordType#HEAD}, {@link RecordType#SECTION},
     *     {@link RecordType#VOLUME} → {@link #HIERARCHY_CORRELATION_KEY}: every record
     *     that is part of a head/section/volume hierarchy shares this one constant
     *     key, so that all such records on a given sink are serialized into a single
     *     broker delivery group.</li>
     * </ul>
     *
     * @return {@link #HIERARCHY_CORRELATION_KEY} for hierarchy record types, or for an
     * unresolved type with a {@link #parentRelation}, the record's own {@code id}
     * otherwise
     */
    @Override
    @JsonIgnore
    public String getCorrelationKey() {
        if (type == null) {
            return hasParentRelation() ? HIERARCHY_CORRELATION_KEY : getId();
        }
        return switch (type) {
            case HEAD, SECTION, VOLUME -> HIERARCHY_CORRELATION_KEY;
            case STANDALONE -> getId();
        };
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }

        MarcRecordInfo that = (MarcRecordInfo) o;

        if (delete != that.delete) {
            return false;
        }
        if (type != that.type) {
            return false;
        }
        return parentRelation != null ? parentRelation.equals(that.parentRelation) : that.parentRelation == null;

    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (type != null ? type.hashCode() : 0);
        result = 31 * result + (delete ? 1 : 0);
        result = 31 * result + (parentRelation != null ? parentRelation.hashCode() : 0);
        return result;
    }
}
