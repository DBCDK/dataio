package dk.dbc.dataio.commons.types;

import dk.dbc.dataio.commons.utils.invariant.InvariantUtil;

import java.io.Serializable;

/**
 * Chunk item DTO class.
 */
public class ChunkItem implements Serializable {
    private static final long serialVersionUID = -7214362358523195493L;

    public enum Status { SUCCESS, FAILURE, IGNORE }

    private /* final */ String id;
    private /* final */ String data;
    private /* final */ Status status;

    /**
     * Class constructor
     *
     * @param id item identifier, can be empty
     * @param data item data, can be empty
     * @param status item status
     *
     * @throws NullPointerException when given null valued argument
     */
    public ChunkItem(String id, String data, Status status) throws NullPointerException {
        this.id = InvariantUtil.checkNotNullOrThrow(id, "id");
        this.data = InvariantUtil.checkNotNullOrThrow(data, "data");
        this.status = InvariantUtil.checkNotNullOrThrow(status, "status");
    }

    private ChunkItem() { }

    public String getId() {
        return id;
    }

    public String getData() {
        return data;
    }

    public Status getStatus() {
        return status;
    }
}
