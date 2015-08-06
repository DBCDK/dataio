package dk.dbc.dataio.commons.utils.test.json;

import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.utils.lang.StringUtil;

public class ChunkItemJsonBuilder extends JsonBuilder {
    private long id = 0L;
    private String data = "data";
    private ChunkItem.Status status = ChunkItem.Status.SUCCESS;

    public ChunkItemJsonBuilder setId(long id) {
        this.id = id;
        return this;
    }

    public ChunkItemJsonBuilder setData(String data) {
        this.data = data;
        return this;
    }

    public ChunkItemJsonBuilder setStatus(ChunkItem.Status status) {
        this.status = status;
        return this;
    }

    public String build() {
        return
                START_OBJECT +
                    asLongMember("id", id) + MEMBER_DELIMITER +
                    asTextMember("data", StringUtil.base64encode(data)) + MEMBER_DELIMITER +
                    asTextMember("status", status.name()) +
                END_OBJECT;
    }
}
