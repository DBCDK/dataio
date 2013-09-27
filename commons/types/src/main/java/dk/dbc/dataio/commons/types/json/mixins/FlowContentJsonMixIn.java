package dk.dbc.dataio.commons.types.json.mixins;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import dk.dbc.dataio.commons.types.FlowComponent;

import java.util.List;

/**
 * This class is a companion to the FlowContent DTO class.
 *
 * Think of this as a way to keep the DTO class "jackson-free" by mixing in annotations
 * to the DTO class during runtime.
 *
 * Method implementations of a MixIn class are ignored.
 */
@SuppressWarnings("unused")
public class FlowContentJsonMixIn {
    /**
     * Makes jackson runtime aware of non-default FlowContent constructor.
     */
    @JsonCreator
    public FlowContentJsonMixIn(@JsonProperty("name") String name,
                                @JsonProperty("description") String description,
                                @JsonProperty("components") List<FlowComponent> components) { }
}
