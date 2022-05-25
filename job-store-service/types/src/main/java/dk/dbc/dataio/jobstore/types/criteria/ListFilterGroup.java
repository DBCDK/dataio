package dk.dbc.dataio.jobstore.types.criteria;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import dk.dbc.invariant.InvariantUtil;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

/**
 * Class representing a logical grouping of filters
 *
 * @param <T> ListFilterField subtype
 */
public class ListFilterGroup<T extends ListFilterField> implements Iterable<ListFilterGroup.Member<T>>, Serializable {
    private boolean not = false;

    /**
     * Logical operators used to combine filter clauses
     */
    public enum LOGICAL_OP {
        AND,
        OR
    }

    public static class Member<T extends ListFilterField> implements Serializable {
        private final ListFilter<T> filter;
        private final LOGICAL_OP logicalOperator;

        /**
         * Constructor
         *
         * @param filter          filter (not null)
         * @param logicalOperator logical combiner (not null)
         * @throws NullPointerException if given null-valued filter or logicalOperator argument
         */
        @JsonCreator
        public Member(@JsonProperty("filter") ListFilter<T> filter,
                      @JsonProperty("logicalOperator") LOGICAL_OP logicalOperator) throws NullPointerException {
            this.filter = InvariantUtil.checkNotNullOrThrow(filter, "filter");
            this.logicalOperator = InvariantUtil.checkNotNullOrThrow(logicalOperator, "logicalOperator");
        }

        public Member() {
            filter = null;
            logicalOperator = null;
        }

        public ListFilter<T> getFilter() {
            return filter;
        }

        public LOGICAL_OP getLogicalOperator() {
            return logicalOperator;
        }

        @Override
        public String toString() {
            return "Member{" +
                    "filter=" + filter +
                    ", logicalOperator=" + logicalOperator +
                    '}';
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Member)) return false;
            Member<?> member = (Member<?>) o;
            return Objects.equals(filter, member.filter) &&
                    Objects.equals(logicalOperator, member.logicalOperator);
        }

        @Override
        public int hashCode() {
            return Objects.hash(filter, logicalOperator);
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

    public void not() {
        not = !not;
    }

    public boolean isNot() {
        return not;
    }

    @Override
    public Iterator<Member<T>> iterator() {
        return members.iterator();
    }

    // Public to allow for tests
    public List<Member<T>> getMembers() {
        return members;
    }


    @Override
    public String toString() {
        return "ListFilterGroup{" +
                "not=" + not +
                ", members=" + members +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ListFilterGroup)) return false;
        ListFilterGroup<?> that = (ListFilterGroup<?>) o;
        if (not != that.not) return false;
        return members.equals(that.members);
    }

    @Override
    public int hashCode() {
        int result = not ? 1 : 0;
        result = 31 * result + members.hashCode();
        return result;
    }
}
