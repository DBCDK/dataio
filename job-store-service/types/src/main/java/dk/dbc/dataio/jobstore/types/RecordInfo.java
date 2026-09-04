package dk.dbc.dataio.jobstore.types;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import dk.dbc.dataio.commons.types.SinkContent;
import dk.dbc.dataio.commons.utils.lang.StringUtil;

import java.util.HashSet;
import java.util.Set;

/**
 * This class contains information about a bibliographic record.
 * Some time in the future this will also encompass keys for sequence analysis.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "@class")
public class RecordInfo {
    protected final String id;
    @JsonProperty
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String pid;

    @JsonCreator
    public RecordInfo(@JsonProperty("id") String id) {
        this.id = id != null ? StringUtil.removeWhitespace(id) : null;
    }

    public String getId() {
        return id;
    }

    public String getPid() {
        return pid;
    }

    public RecordInfo withPid(String pid) {
        this.pid = pid;
        return this;
    }

    @JsonIgnore
    public Set<String> getKeys(SinkContent.SequenceAnalysisOption sequenceAnalysisOption) {
        final Set<String> keys = new HashSet<>();
        if (id != null) {
            keys.add(id);
        }
        return keys;
    }

    /**
     * Returns the key used by the sink delivery layer to serialise deliveries of this
     * record via the message broker's {@code JMSXGroupID} mechanism (see
     * docs/chunk-scheduling-redesign.md, sections "Record Identity — correlationKey"
     * and "JMSXGroupID vs. Watermark Key").
     * <p>
     * <b>This is not the watermark key.</b> The watermark, which detects and skips a
     * stale, already-superseded delivery, is keyed differently, and never by this
     * method. Several distinct records can legitimately share one correlationKey
     * (e.g. every record in a head/section/volume hierarchy shares one constant, see
     * {@link MarcRecordInfo#getCorrelationKey()}), and treating that shared key as a
     * watermark key would make delivering any one of them advance a watermark shared
     * by all of them, wrongly marking the others stale. correlationKey only ever
     * controls broker-side serialisation of delivery order and plays no part in
     * watermark comparison. See docs/chunk-scheduling-redesign.md, "JMSXGroupID vs.
     * Watermark Key" and "Delivery Watermark", for how the watermark key is actually
     * derived and composed.
     * <p>
     * Two chunks whose items share the same correlationKey are delivered strictly one
     * item at a time, in scheduler dispatch order. This is what lets ordering between
     * related records be preserved despite concurrent consumers. Chunks with different
     * correlationKeys carry no such ordering guarantee between each other and may be
     * delivered concurrently.
     * <p>
     * For a plain, non-MARC {@code RecordInfo} the record's own {@code id} is the
     * natural default for serialising a record against older/newer versions of
     * itself.
     *
     * @return the correlation key, or {@code null} if this record must not be
     * serialised against any other record (it is then distributed freely by the broker)
     */
    @JsonIgnore
    public String getCorrelationKey() {
        return id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        RecordInfo that = (RecordInfo) o;

        return id != null ? id.equals(that.id) : that.id == null;

    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "RecordInfo{" + "id='" + id + '\'' + ", pid='" + pid + '\'' + '}';
    }
}
