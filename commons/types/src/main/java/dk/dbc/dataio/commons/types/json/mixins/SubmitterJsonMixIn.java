package dk.dbc.dataio.commons.types.json.mixins;

import dk.dbc.dataio.commons.types.SubmitterContent;
import org.codehaus.jackson.annotate.JsonCreator;
import org.codehaus.jackson.annotate.JsonProperty;

/**
 * This class is a companion to the Submitter DTO class.
 *
 * Think of this as a way to keep the DTO class "jackson-free" by mixing in annotations
 * to the DTO class during runtime.
 *
 * Method implementations of a MixIn class are ignored.
 */
@SuppressWarnings("unused")
abstract class SubmitterJsonMixIn {
    /**
     * Makes jackson runtime aware of non-default Submitter constructor.
     */
    @JsonCreator
    public SubmitterJsonMixIn(@JsonProperty("id") long id,
                              @JsonProperty("version") long version,
                              @JsonProperty("content") SubmitterContent content) { }
}
