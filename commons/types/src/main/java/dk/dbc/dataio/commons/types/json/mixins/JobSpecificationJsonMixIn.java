package dk.dbc.dataio.commons.types.json.mixins;

import org.codehaus.jackson.annotate.JsonCreator;
import org.codehaus.jackson.annotate.JsonProperty;

/**
 * This class is a companion to the JobSpecification DTO class.
 *
 * Think of this as a way to keep the DTO class "jackson-free" by mixing in annotations
 * to the DTO class during runtime.
 *
 * Method implementations of a MixIn class are ignored.
 */
@SuppressWarnings("unused")
public class JobSpecificationJsonMixIn {
    /**
     * Makes jackson runtime aware of non-default JobSpecification constructor.
     */
    @JsonCreator
    public JobSpecificationJsonMixIn(@JsonProperty("packaging") String packaging,
                                     @JsonProperty("format") String format,
                                     @JsonProperty("charset") String charset,
                                     @JsonProperty("destination") String destination,
                                     @JsonProperty("submitterId") long submitterId,
                                     @JsonProperty("dataFile") String dataFile) { }
}
