package dk.dbc.dataio.harvester.types;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serial;
import java.io.Serializable;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;

/**
 * A filter for submitters.
 * <p>
 * Filters can be used to implement two different strategies:
 * 1. Accept all except a list of submitters.
 * 2. Skip all except a list of submitters.
 */
public class SubmitterFilter implements Serializable {
    @Serial
    private static final long serialVersionUID = 1876663346935499374L;

    public enum Type {
        ACCEPT_ALL_EXCEPT,
        SKIP_ALL_EXCEPT
    }

    private final Type type;
    private final Set<Integer> submitterNumbers;

    /**
     * @param type The type of filter.
     * @param submitterNumbers The list of submitters to filter.
     */
    @JsonCreator
    public SubmitterFilter(
            @JsonProperty("type") Type type,
            @JsonProperty("submitterNumber") List<Integer> submitterNumbers) {
        
        if (type == null) {
            throw new IllegalArgumentException("type cannot be null");
        }
        if (submitterNumbers == null) {
            throw new IllegalArgumentException("submitterNumbers cannot be null");
        }
        if (submitterNumbers.isEmpty()) {
            throw new IllegalArgumentException("submitterNumbers cannot be empty");
        }
        this.type = type;
        this.submitterNumbers = new HashSet<>(submitterNumbers);
    }

    public Type getType() {
        return type;
    }

    public Set<Integer> getSubmitterNumbers() {
        return Collections.unmodifiableSet(submitterNumbers);
    }

    /**
     * Tests if the submitter should be skipped.
     * @param submitterNumber The submitter number.
     * @return True if the submitter should be skipped.
     */
    public boolean shouldSkip(Integer submitterNumber) {
        if (type == Type.ACCEPT_ALL_EXCEPT) {
            return submitterNumbers.contains(submitterNumber);
        }
        if (type == Type.SKIP_ALL_EXCEPT) {
            return !submitterNumbers.contains(submitterNumber);
        }
        throw new IllegalStateException("Unknown type: " + type);
    }

    /**
     * @return a Predicate that returns true when the submitter should be skipped.
     */
    public Predicate<Integer> skipPredicate() {
        return this::shouldSkip;
    }

    /**
     * Tests if the submitter should be accepted.
     * @param submitterNumber The submitter number.
     * @return True if the submitter should be accepted.
     */
    public boolean shouldAccept(Integer submitterNumber) {
        if (type == Type.ACCEPT_ALL_EXCEPT) {
            return !submitterNumbers.contains(submitterNumber);
        }
        if (type == Type.SKIP_ALL_EXCEPT) {
            return submitterNumbers.contains(submitterNumber);
        }
        throw new IllegalStateException("Unknown type: " + type);
    }

    /**
     * @return a Predicate that returns true when the submitter should be accepted.
     */
    public Predicate<Integer> acceptPredicate() {
        return this::shouldAccept;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        SubmitterFilter that = (SubmitterFilter) o;
        return type == that.type && Objects.equals(submitterNumbers, that.submitterNumbers);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, submitterNumbers);
    }

    @Override
    public String toString() {
        return "SubmitterFilter{" +
                "type=" + type +
                ", submitterNumbers=" + submitterNumbers +
                '}';
    }
}
