package dk.dbc.dataio.commons.types;

import dk.dbc.dataio.commons.utils.invariant.InvariantUtil;

import java.io.Serializable;

/**
 * SubmitterContent DTO class.
 *
 * In all essence objects of this class are immutable, but due to GWT serialization
 * issues we cannot have final fields and need a default no-arg constructor.
 */
public class SubmitterContent implements Serializable {
    static /* final */ long NUMBER_LOWER_THRESHOLD = 0;

    private static final long serialVersionUID = -2754982619041504537L;

    private /* final */ long number;
    private /* final */ String name;
    private /* final */ String description;

    private SubmitterContent() { }

    /**
     * Class constructor
     *
     * Attention: when changing the signature of this constructor
     * remember to also change the signature in the corresponding *JsonMixIn class.
     *
     * @param number submitter number (> {@value #NUMBER_LOWER_THRESHOLD})
     * @param name submitter name
     * @param description submitter description
     *
     * @throws NullPointerException if given null-valued name or description argument
     * @throws IllegalArgumentException if given empty-valued name or description argument, or if
     * value of number is not above {@value #NUMBER_LOWER_THRESHOLD}
     */
    public SubmitterContent(long number, String name, String description) {
        this.number = InvariantUtil.checkAboveThresholdOrThrow(number, "number", NUMBER_LOWER_THRESHOLD);
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
}
