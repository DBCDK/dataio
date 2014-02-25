package dk.dbc.dataio.commons.utils.test.model;

import dk.dbc.dataio.commons.types.ChunkItem;

public class ChunkItemBuilder {
    private String id = "id";
    private String data = "data";
    private ChunkItem.Status status = ChunkItem.Status.SUCCESS;

    public ChunkItemBuilder setId(String id) {
        this.id = id;
        return this;
    }

    public ChunkItemBuilder setData(String data) {
        this.data = data;
        return this;
    }

    public ChunkItemBuilder setStatus(ChunkItem.Status status) {
        this.status = status;
        return this;
    }

    public ChunkItem build() {
        return new ChunkItem(id, data, status);
    }
}
