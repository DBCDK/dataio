package dk.dbc.dataio.jobstore.types;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import dk.dbc.dataio.commons.utils.invariant.InvariantUtil;

/**
 * Class representing a job error
 */
public class JobError {
    public static enum Code {
        INVALID_DATA,
        INVALID_DATAFILE,
        INVALID_FLOW_BINDER_IDENTIFIER,
        INVALID_URI_SYNTAX,
        INVALID_ITEM_IDENTIFIER,
        ILLEGAL_CHUNK,
        INVALID_JSON,
        INVALID_JOB_ID,
        INVALID_CHUNK_ID,
        INVALID_CHUNK_TYPE

    }

    private final Code code;
    private final String description;
    private final String stacktrace;

    /**
     * Class constructor
     * @param code error code
     * @param description error description
     * @param stacktrace error stacktrace or empty string if given as null or empty string
     * @throws NullPointerException if given null-valued code or description argument
     * @throws IllegalArgumentException if given empty valued description argument
     */
    @JsonCreator
    public JobError(
            @JsonProperty("code") Code code,
            @JsonProperty("description") String description,
            @JsonProperty("stacktrace") String stacktrace) throws NullPointerException, IllegalArgumentException {
        this.code = InvariantUtil.checkNotNullOrThrow(code, "code");
        this.description = InvariantUtil.checkNotNullNotEmptyOrThrow(description, "description");
        this.stacktrace = stacktrace == null ? "" : stacktrace;
    }

    public Code getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public String getStacktrace() {
        return stacktrace;
    }
}
