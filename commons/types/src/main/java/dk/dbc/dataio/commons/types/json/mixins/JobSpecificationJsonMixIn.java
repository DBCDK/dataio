package dk.dbc.dataio.commons.types.json.mixins;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

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
                                     @JsonProperty("mailForNotificationAboutVerification") String mailForNotificationAboutVerification,
                                     @JsonProperty("mailForNotificationAboutProcessing") String mailForNotificationAboutProcessing,
                                     @JsonProperty("resultmailInitials") String resultmailInitials,
                                     @JsonProperty("dataFile") String dataFile) { }
}
