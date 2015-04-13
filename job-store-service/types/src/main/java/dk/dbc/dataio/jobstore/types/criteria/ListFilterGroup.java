package dk.dbc.dataio.jobstore.types.criteria;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import dk.dbc.dataio.commons.utils.invariant.InvariantUtil;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Class representing a logical grouping of filters
 * @param <T> ListFilterField subtype
 */
public class ListFilterGroup<T extends ListFilterField> implements Iterable<ListFilterGroup.Member<T>> {
    /**
     * Logical operators used to combine filter clauses
     */
    public static enum LOGICAL_OP {
        AND,
        OR
    }

    public static class Member<T extends ListFilterField> {
        private final ListFilter<T> filter;
        private final LOGICAL_OP logicalOperator;

        /**
         * Constructor
         * @param filter filter (not null)
         * @param logicalOperator logical combiner (not null)
         * @throws NullPointerException if given null-valued filter or logicalOperator argument
         */
        @JsonCreator
        public Member(@JsonProperty("filter") ListFilter<T> filter,
                      @JsonProperty("logicalOperator") LOGICAL_OP logicalOperator) throws NullPointerException {
            this.filter = InvariantUtil.checkNotNullOrThrow(filter, "filter");
            this.logicalOperator = InvariantUtil.checkNotNullOrThrow(logicalOperator, "logicalOperator");
        }

        public ListFilter<T> getFilter() {
            return filter;
        }

        public LOGICAL_OP getLogicalOperator() {
            return logicalOperator;
        }
    }

    @JsonProperty
    private final List<Member<T>> members;

    public ListFilterGroup() {
        members = new ArrayList<>();
    }

    public ListFilterGroup<T> addMember(Member<T> member) throws NullPointerException {
        members.add(InvariantUtil.checkNotNullOrThrow(member, "member"));
        return this;
    }

    public int size() {
        return members.size();
    }

    @Override
    public Iterator<Member<T>> iterator() {
        return members.iterator();
    }
}
