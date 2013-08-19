package dk.dbc.dataio.engine;

import java.util.List;
import org.codehaus.jackson.annotate.JsonCreator;
import org.codehaus.jackson.annotate.JsonProperty;

/**
 * This class is a companion to the FlowComponentContent DTO class.
 *
 * Think of this as a way to keep the DTO class "jackson-free" by mixing in
 * annotations to the DTO class during runtime.
 *
 * Method implementations of a MixIn class are ignored.
 */
@SuppressWarnings("unused")
public class FlowComponentContentJsonMixIn {

    /**
     * Makes jackson runtime aware of non-default FlowComponentContent
     * constructor.
     */
    @JsonCreator
    public FlowComponentContentJsonMixIn(@JsonProperty("name") String name,
                                         @JsonProperty("javascripts") List<JavaScript> javascripts,
                                         @JsonProperty("invocationMethod") String invocationMethod) {
    }
}
