package dk.dbc.dataio.commons.types;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import dk.dbc.invariant.InvariantUtil;

import java.io.Serializable;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Chunk item DTO class.
 * <p>
 * This class is NOT thread safe.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ChunkItem implements Serializable {
    public static final ChunkItem UNDEFINED = null;
    private static final long serialVersionUID = -7214362358523195493L;

    public enum Status {SUCCESS, FAILURE, IGNORE}

    public enum Type {
        UNKNOWN,
        ADDI,
        DANMARC2_LINEFORMAT,
        MARC21_LINEFORMAT,
        LINEFORMAT,
        GENERICXML,
        DATACONTAINER,
        MARCXCHANGE,
        STRING,
        BYTES,
        JOB_END,
        JSON
    }

    private long id;
    private byte[] data;
    private Status status;
    private List<Type> type;
    private ArrayList<Diagnostic> diagnostics;
    private Charset encoding;
    private String trackingId;

    public static ChunkItem successfulChunkItem() {
        return new ChunkItem()
                .withStatus(Status.SUCCESS);
    }

    public static ChunkItem failedChunkItem() {
        return new ChunkItem()
                .withStatus(Status.FAILURE);
    }

    public static ChunkItem ignoredChunkItem() {
        return new ChunkItem()
                .withStatus(Status.IGNORE);
    }

    public ChunkItem() {
        this.encoding = StandardCharsets.UTF_8;
    }

    /**
     * Class constructor (deprecated)
     *
     * @param id       item identifier, must be larger than {@value Constants#CHUNK_ITEM_ID_LOWER_BOUND}
     * @param data     item data, can be empty, but not null
     * @param status   item status can not be null
     * @param type     item type as list to support Embeddable formats, can also be null or empty
     * @param encoding item charset encoding, can also be null
     */
    public ChunkItem(long id, byte[] data, Status status, List<Type> type, Charset encoding) {
        this(id, data, status);
        if (type != null) {
            withType(type.toArray(new Type[type.size()]));
        }
        if (encoding != null) {
            withEncoding(encoding);
        }
    }

    /**
     * Class constructor (used to enforce invariant checks during deserialization)
     *
     * @param id     item identifier, must be larger than {@value Constants#CHUNK_ITEM_ID_LOWER_BOUND}
     * @param data   item data, can be empty, but not null
     * @param status item status can not be null
     */
    @JsonCreator
    private ChunkItem(
            @JsonProperty("id") long id,
            @JsonProperty("data") byte[] data,
            @JsonProperty("status") Status status) {
        this();
        withId(id);
        withData(data);
        withStatus(status);
    }

    public ChunkItem withId(long id) throws IllegalArgumentException {
        this.id = InvariantUtil.checkLowerBoundOrThrow(id, "id", Constants.CHUNK_ITEM_ID_LOWER_BOUND);
        return this;
    }

    public long getId() {
        return id;
    }

    public ChunkItem withTrackingId(String trackingId) {
        this.trackingId = trackingId;
        return this;
    }

    public String getTrackingId() {
        return trackingId;
    }

    public ChunkItem withData(byte[] data) {
        this.data = InvariantUtil.checkNotNullOrThrow(data, "data");
        return this;
    }

    public ChunkItem withData(String data) {
        InvariantUtil.checkNotNullOrThrow(data, "data");
        return withData(data.getBytes(StandardCharsets.UTF_8));
    }

    public byte[] getData() {
        return data;
    }

    public ChunkItem withStatus(Status status) {
        this.status = InvariantUtil.checkNotNullOrThrow(status, "status");
        return this;
    }

    public Status getStatus() {
        return status;
    }

    public ChunkItem withType(Type... types) {
        if (types != null && types.length != 0) {
            if (this.type == null) {
                this.type = new ArrayList<>(types.length);
            }
            Collections.addAll(this.type, types);
        }
        return this;
    }

    public List<Type> getType() {
        return type;
    }

    @JsonIgnore
    public boolean isTyped() {
        return type != null && !type.isEmpty();
    }

    /**
     * If given list of diagnostics is not null or empty:
     * - If any one of the diagnostics has a level different from WARNING, then status of this chunk item is set to FAILURE.
     * - Given diagnostics are appended to this chunk item.
     *
     * @param diagnostics containing list of descriptions of the reasons for failure
     * @return reference to self
     */
    public ChunkItem withDiagnostics(Diagnostic... diagnostics) {
        if (diagnostics != null && diagnostics.length != 0) {
            if (Arrays.stream(diagnostics).anyMatch(
                    diagnostic -> diagnostic.getLevel() != Diagnostic.Level.WARNING)) {
                this.status = Status.FAILURE;
            }
            if (this.diagnostics == null) {
                this.diagnostics = new ArrayList<>(diagnostics.length);
            }
            Collections.addAll(this.diagnostics, diagnostics);
        }
        return this;
    }

    /**
     * Alias for withDiagnostic() with a single diagnostic argument.
     *
     * @param diagnostic diagnostic to be appended to this chunk item
     */
    public void appendDiagnostics(Diagnostic diagnostic) {
        withDiagnostics(diagnostic);
    }

    /**
     * Alias for withDiagnostic() with a multiple diagnostic arguments.
     *
     * @param diagnostics diagnostics to be appended to this chunk item
     */
    public void appendDiagnostics(List<Diagnostic> diagnostics) {
        if (diagnostics != null) {
            withDiagnostics(diagnostics.toArray(new Diagnostic[diagnostics.size()]));
        }
    }

    public ArrayList<Diagnostic> getDiagnostics() {
        return diagnostics;
    }

    public ChunkItem withEncoding(Charset encoding) {
        this.encoding = encoding;
        return this;
    }

    public Charset getEncoding() {
        return encoding;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        ChunkItem chunkItem = (ChunkItem) o;

        if (id != chunkItem.id) {
            return false;
        }
        if (!Arrays.equals(data, chunkItem.data)) {
            return false;
        }
        if (status != chunkItem.status) {
            return false;
        }
        if (type != null ? !type.equals(chunkItem.type) : chunkItem.type != null) {
            return false;
        }
        if (diagnostics != null ? !diagnostics.equals(chunkItem.diagnostics) : chunkItem.diagnostics != null) {
            return false;
        }
        if (encoding != null ? !encoding.equals(chunkItem.encoding) : chunkItem.encoding != null) {
            return false;
        }
        return trackingId != null ? trackingId.equals(chunkItem.trackingId) : chunkItem.trackingId == null;

    }

    @Override
    public int hashCode() {
        int result = (int) (id ^ (id >>> 32));
        result = 31 * result + Arrays.hashCode(data);
        result = 31 * result + status.hashCode();
        result = 31 * result + (type != null ? type.hashCode() : 0);
        result = 31 * result + (diagnostics != null ? diagnostics.hashCode() : 0);
        result = 31 * result + (encoding != null ? encoding.hashCode() : 0);
        result = 31 * result + (trackingId != null ? trackingId.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "ChunkItem{" +
                "id=" + id +
                ", data=" + Arrays.toString(data) +
                ", status=" + status +
                ", type=" + type +
                ", diagnostics=" + diagnostics +
                ", encoding=" + encoding +
                ", trackingId='" + trackingId + '\'' +
                '}';
    }
}
