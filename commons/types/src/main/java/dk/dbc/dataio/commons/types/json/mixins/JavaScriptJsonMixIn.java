package dk.dbc.dataio.commons.types.json.mixins;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * This class is a companion to the JavaScript DTO class.
 *
 * Think of this as a way to keep the DTO class "jackson-free" by mixing in annotations
 * to the DTO class during runtime.
 *
 * Method implementations of a MixIn class are ignored.
 */
@SuppressWarnings("unused")
public class JavaScriptJsonMixIn {
    /**
     * Makes jackson runtime aware of non-default JavaScript constructor.
     */
    @JsonCreator
    public JavaScriptJsonMixIn(@JsonProperty("javascript") String javascript,
                               @JsonProperty("moduleName") String moduleName) { }
}
