package dk.dbc.dataio.commons.types;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import dk.dbc.dataio.commons.utils.invariant.InvariantUtil;

import java.io.Serializable;

/**
 * SubmitterContent DTO class.
 */
public class SubmitterContent implements Serializable {
    static /* final */ long NUMBER_LOWER_BOUND = 1L;

    private static final long serialVersionUID = -2754982619041504537L;

    private final long number;
    private final String name;
    private final String description;

    /**
     * Class constructor
     *
     * @param number submitter number (> {@value #NUMBER_LOWER_BOUND})
     * @param name submitter name
     * @param description submitter description
     *
     * @throws NullPointerException if given null-valued name or description argument
     * @throws IllegalArgumentException if given empty-valued name or description argument, or if
     * value of number is not above {@value #NUMBER_LOWER_BOUND}
     */
    @JsonCreator
    public SubmitterContent(@JsonProperty("number") long number,
                            @JsonProperty("name") String name,
                            @JsonProperty("description") String description) {

        this.number = InvariantUtil.checkLowerBoundOrThrow(number, "number", NUMBER_LOWER_BOUND);
        this.name = InvariantUtil.checkNotNullNotEmptyOrThrow(name, "name");
        this.description = InvariantUtil.checkNotNullNotEmptyOrThrow(description, "description");
    }

    public String getDescription() {
        return description;
    }

    public String getName() {
        return name;
    }

    public long getNumber() {
        return number;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SubmitterContent)) return false;

        SubmitterContent that = (SubmitterContent) o;

        if (number != that.number) return false;
        if (!name.equals(that.name)) return false;
        return description.equals(that.description);

    }

    @Override
    public int hashCode() {
        int result = (int) (number ^ (number >>> 32));
        result = 31 * result + name.hashCode();
        result = 31 * result + description.hashCode();
        return result;
    }
}
