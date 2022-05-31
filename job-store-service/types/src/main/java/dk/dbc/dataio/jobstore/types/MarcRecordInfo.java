package dk.dbc.dataio.jobstore.types;

/**
 * This class contains information about a bibliographic record
 * extended with information deduced from the fact that we know it is a MARC record
 */

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import dk.dbc.dataio.commons.types.SinkContent;

import java.util.Set;

public class MarcRecordInfo extends RecordInfo {
    public enum RecordType {
        STANDALONE, HEAD, SECTION, VOLUME
    }

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
