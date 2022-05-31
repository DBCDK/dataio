package dk.dbc.dataio.jobstore.types.criteria;

import com.fasterxml.jackson.annotation.JsonProperty;
import dk.dbc.invariant.InvariantUtil;

/**
 * Class representing a list order-by clause
 *
 * @param <T> ListFilterField subtype
 */
public class ListOrderBy<T extends ListFilterField> {
    public enum Sort {
        ASC,
        DESC
    }

    private final Sort sort;
    private final T field;

    /**
     * constructor
     *
     * @param field order by field (not null)
     * @param sort  sort order (not null)
     * @throws NullPointerException if given any null-valued argument
     */
    public ListOrderBy(@JsonProperty("field") T field,
                       @JsonProperty("sort") Sort sort) throws NullPointerException {
        this.field = InvariantUtil.checkNotNullOrThrow(field, "field");
        this.sort = InvariantUtil.checkNotNullOrThrow(sort, "sort");
    }

    public T getField() {
        return field;
    }

    public Sort getSort() {
        return sort;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        ListOrderBy that = (ListOrderBy) o;

        if (!field.equals(that.field)) {
            return false;
        }
        if (sort != that.sort) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = sort.hashCode();
        result = 31 * result + field.hashCode();
        return result;
    }
}
