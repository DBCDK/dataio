package dk.dbc.dataio.commons.types;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import dk.dbc.dataio.commons.utils.invariant.InvariantUtil;

import java.io.Serializable;

/**
 * Chunk item DTO class.
 */
public class ChunkItem implements Serializable {
    private static final long serialVersionUID = -7214362358523195493L;

    public enum Status { SUCCESS, FAILURE, IGNORE }

    private final long id;
    private final String data;
    private final Status status;

    /**
     * Class constructor
     *
     * @param id item identifier, must be larger than {@value dk.dbc.dataio.commons.types.Constants#CHUNK_ITEM_ID_LOWER_BOUND}
     * @param data item data, can be empty
     * @param status item status
     *
     * @throws NullPointerException when given null valued argument
     * @throws IllegalArgumentException when given id value of {@value dk.dbc.dataio.commons.types.Constants#CHUNK_ITEM_ID_LOWER_BOUND} or less
     */
    @JsonCreator
    public ChunkItem(
            @JsonProperty("id") long id,
            @JsonProperty("data") String data,
            @JsonProperty("status") Status status) {
        this.id = InvariantUtil.checkLowerBoundOrThrow(id, "id", Constants.CHUNK_ITEM_ID_LOWER_BOUND);
        this.data = InvariantUtil.checkNotNullOrThrow(data, "data");
        this.status = InvariantUtil.checkNotNullOrThrow(status, "status");
    }

    public long getId() {
        return id;
    }

    public String getData() {
        return data;
    }

    public Status getStatus() {
        return status;
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
        if (!data.equals(chunkItem.data)) {
            return false;
        }
        if (status != chunkItem.status) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = (int) (id ^ (id >>> 32));
        result = 31 * result + data.hashCode();
        result = 31 * result + status.hashCode();
        return result;
    }
}
