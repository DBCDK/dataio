package dk.dbc.dataio.commons.types;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import dk.dbc.invariant.InvariantUtil;

/**
 * Class representing a flow store error
 */
public class FlowStoreError {
    public enum Code {
        NONEXISTING_SUBMITTER,
        EXISTING_SUBMITTER_NONEXISTING_DESTINATION,
        EXISTING_SUBMITTER_EXISTING_DESTINATION_NONEXISTING_TOC,
        INVALID_QUERY
    }

    private final Code code;
    private final String description;
    private final String stacktrace;

    /**
     * Class constructor
     *
     * @param code        error code
     * @param description error description
     * @param stacktrace  error stacktrace or empty string if given as null or empty string
     * @throws NullPointerException     if given null-valued code or description argument
     * @throws IllegalArgumentException if given empty valued description argument
     */
    @JsonCreator
    public FlowStoreError(
            @JsonProperty("code") Code code,
            @JsonProperty("description") String description,
            @JsonProperty("stacktrace") String stacktrace) throws NullPointerException, IllegalArgumentException {
        this.code = InvariantUtil.checkNotNullOrThrow(code, "code");
        this.description = InvariantUtil.checkNotNullNotEmptyOrThrow(description, "description");
        this.stacktrace = stacktrace == null ? "" : stacktrace;
    }

    /**
     * Gets the error code
     *
     * @return error code
     */
    public Code getCode() {
        return code;
    }

    /**
     * Gets the description
     *
     * @return description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Gets the stacktrace
     *
     * @return stacktrace
     */
    public String getStacktrace() {
        return stacktrace;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FlowStoreError)) return false;

        FlowStoreError that = (FlowStoreError) o;

        if (code != that.code) return false;
        if (description != null ? !description.equals(that.description) : that.description != null) return false;
        return !(stacktrace != null ? !stacktrace.equals(that.stacktrace) : that.stacktrace != null);

    }

    @Override
    public int hashCode() {
        int result = code != null ? code.hashCode() : 0;
        result = 31 * result + (description != null ? description.hashCode() : 0);
        result = 31 * result + (stacktrace != null ? stacktrace.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "FlowStoreError{" +
                "code=" + code +
                ", description='" + description + '\'' +
                ", stacktrace='" + stacktrace + '\'' +
                '}';
    }

}
