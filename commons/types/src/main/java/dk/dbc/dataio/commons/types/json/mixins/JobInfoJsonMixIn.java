package dk.dbc.dataio.commons.types.json.mixins;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import dk.dbc.dataio.commons.types.JobErrorCode;
import dk.dbc.dataio.commons.types.JobSpecification;

/**
 * This class is a companion to the JobInfo DTO class.
 *
 * Think of this as a way to keep the DTO class "jackson-free" by mixing in annotations
 * to the DTO class during runtime.
 *
 * Method implementations of a MixIn class are ignored.
 */
@SuppressWarnings("unused")
public class JobInfoJsonMixIn {
    /**
     * Makes jackson runtime aware of non-default JobInfo constructor.
     */
    @JsonCreator
    public JobInfoJsonMixIn(@JsonProperty("jobId") long jobId,
                            @JsonProperty("jobSpecification") JobSpecification jobSpecification,
                            @JsonProperty("jobCreationTime") long jobCreationTime,
                            @JsonProperty("jobErrorCode") JobErrorCode jobErrorCode,
                            @JsonProperty("jobRecordCount") long jobRecordCount) { }
}
