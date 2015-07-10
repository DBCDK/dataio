package dk.dbc.dataio.jobstore.types;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import dk.dbc.dataio.commons.utils.invariant.InvariantUtil;

/**
 * Class representing a job error
 */
public class JobError {

    public static final String NO_STACKTRACE = null;
    public enum Code {
        INVALID_DATA,
        INVALID_ITEM_IDENTIFIER,
        INVALID_JOB_IDENTIFIER,
        ILLEGAL_CHUNK,
        INVALID_JSON,
        INVALID_CHUNK_IDENTIFIER,
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

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        JobError jobError = (JobError) o;

        if (code != jobError.code) {
            return false;
        }
        if (!description.equals(jobError.description)) {
            return false;
        }
        if (stacktrace != null ? !stacktrace.equals(jobError.stacktrace) : jobError.stacktrace != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = code.hashCode();
        result = 31 * result + description.hashCode();
        result = 31 * result + (stacktrace != null ? stacktrace.hashCode() : 0);
        return result;
    }
}
