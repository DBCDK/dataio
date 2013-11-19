package dk.dbc.dataio.commons.types.json.mixins;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * This class is a companion to the FlowBinderContent DTO class.
 *
 * Think of this as a way to keep the DTO class "jackson-free" by mixing in annotations
 * to the DTO class during runtime.
 *
 * Method implementations of a MixIn class are ignored.
 */
@SuppressWarnings("unused")
public class FlowBinderContentJsonMixIn {
    /**
     * Makes jackson runtime aware of non-default FlowBinderContent constructor.
     */
    @JsonCreator
    public FlowBinderContentJsonMixIn(@JsonProperty("name") String name,
                                      @JsonProperty("description") String description,
                                      @JsonProperty("packaging") String packaging,
                                      @JsonProperty("format") String format,
                                      @JsonProperty("charset") String charset,
                                      @JsonProperty("destination") String destination,
                                      @JsonProperty("recordSplitter") String recordSplitter,
                                      @JsonProperty("flowId") long flowId,
                                      @JsonProperty("submitterIds") List<Long> submitterIds,
                                      @JsonProperty("sinkId") long sinkId) { }
}
