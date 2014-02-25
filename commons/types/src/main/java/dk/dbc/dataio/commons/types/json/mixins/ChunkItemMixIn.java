package dk.dbc.dataio.commons.types.json.mixins;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import dk.dbc.dataio.commons.types.ChunkItem;

/**
 * This class is a companion to the ChunkItem DTO class.
 *
 * Think of this as a way to keep the DTO class "jackson-free" by mixing in
 * annotations to the DTO class during runtime.
 *
 * Method implementations of a MixIn class are ignored.
 */
@SuppressWarnings("unused")
public abstract class ChunkItemMixIn {

    /**
     * Makes jackson runtime aware of non-default ChunkItem constructor.
     */
    @JsonCreator
    public ChunkItemMixIn(@JsonProperty("id") String id,
                          @JsonProperty("data") String data,
                          @JsonProperty("status") ChunkItem.Status status) {
    }
}
