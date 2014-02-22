package dk.dbc.dataio.commons.types.json.mixins;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * This class is a companion to the SupplementaryProcessData DTO class.
 *
 * Think of this as a way to keep the DTO class "jackson-free" by mixing in
 * annotations to the DTO class during runtime.
 *
 * Method implementations of a MixIn class are ignored.
 */
@SuppressWarnings("unused")
public abstract class SupplementaryProcessDataJsonMixIn {

    /**
     * Makes jackson runtime aware of non-default SupplementaryProcessData constructor.
     */
    @JsonCreator
    public SupplementaryProcessDataJsonMixIn(@JsonProperty("submitter") long submitter,
                                             @JsonProperty("format") String format) {}
}
