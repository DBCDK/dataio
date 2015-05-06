package dk.dbc.dataio.jobstore.types.criteria;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import dk.dbc.dataio.commons.utils.invariant.InvariantUtil;

/**
 * Class representing a listings filter clause
 * @param <T> ListFilterField subtype
 */
public class ListFilter<T extends ListFilterField> {
    /**
     * Filter operators
     */
    public enum Op {
        LESS_THAN,
        GREATER_THAN,
        LESS_THAN_OR_EQUAL_TO,
        GREATER_THAN_OR_EQUAL_TO,
        EQUAL,
        NOT_EQUAL,
        NOOP,
        IS_NULL,
        IS_NOT_NULL
    }

    private final T field;
    private final Op operator;
    private final Object value;

    /**
     * Constructor
     * @param field filter field (not null)
     * @param operator filter operator (not null)
     * @param value filter value
     * @throws NullPointerException if given null-valued field or operator argument
     */
    public ListFilter(@JsonProperty("field") T field,
                      @JsonProperty("operator") Op operator,
                      @JsonProperty("value") Object value) throws NullPointerException {
        this.field = InvariantUtil.checkNotNullOrThrow(field, "field");
        this.operator = InvariantUtil.checkNotNullOrThrow(operator, "operator");
        this.value = value;
    }

    @JsonIgnore
    public ListFilter(@JsonProperty("field") T field) {
        this(field, Op.NOOP, null);
    }

    @JsonIgnore
    public ListFilter(@JsonProperty("field") T field, @JsonProperty("operator") Op operator) {
        this(field, operator, null);
    }

    public T getField() {
        return field;
    }

    public Op getOperator() {
        return operator;
    }

    public Object getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        ListFilter that = (ListFilter) o;

        if (!field.equals(that.field)) {
            return false;
        }
        if (operator != that.operator) {
            return false;
        }
        if (value != null ? !value.equals(that.value) : that.value != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = field.hashCode();
        result = 31 * result + operator.hashCode();
        result = 31 * result + (value != null ? value.hashCode() : 0);
        return result;
    }
}
