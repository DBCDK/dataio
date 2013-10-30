package dk.dbc.dataio.commons.types.json.mixins;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import dk.dbc.dataio.commons.types.PingResponse;

import java.util.List;

/**
 * This class is a companion to the PingResponse DTO class.
 *
 * Think of this as a way to keep the DTO class "jackson-free" by mixing in annotations
 * to the DTO class during runtime.
 *
 * Method implementations of a MixIn class are ignored.
 */
@SuppressWarnings("unused")
public class PingResponseJsonMixIn {
    /**
     * Makes jackson runtime aware of non-default PingResponse constructor.
     */
    @JsonCreator
    public PingResponseJsonMixIn(@JsonProperty("status") PingResponse.Status status,
                                 @JsonProperty("log") List<String> log) { }
}
