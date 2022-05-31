package dk.dbc.dataio.jobstore.types.criteria;

import dk.dbc.invariant.InvariantUtil;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

/**
 * Abstract base class for listings criteria
 *
 * <pre>
 * <code>
 *
 * final ListCriteriaImpl listCriteria = new ListCriteriaImpl()
 *      .where(new ListFilterImpl(...))
 *          .and(new ListFilterImpl(...))
 *      .where(new ListFilterImpl(...))
 *          .or(new ListFilterImpl(...))
 *      .orderBy(new ListOrderByImpl(...))
 *      .orderBy(new ListOrderByImpl(...));
 *      .limit(int)
 *      .offset(int)
 *
 * for (ListFilterGroup&lt;ListCriteriaImplField&gt; filters : listCriteria.getFiltering()) {
 *     ...
 * }
 *
 * for (ListOrderBy&lt;ListCriteriaImplField&gt; orderBy : listCriteria.getOrdering()) {
 *     ...
 * }
 * </code>
 * </pre>
 * <p>
 * Upcoming features:
 * <pre>
 *   andWhere(ListFilter&lt;T&gt; filter)
 *   orWhere(ListFilter&lt;T&gt; filter)
 *   whereIn(T, List&lt;Object&gt;)
 * </pre>
 */
public abstract class ListCriteria<T extends ListFilterField, U extends ListCriteria<T, U>> implements Serializable {
    private LinkedList<ListFilterGroup<T>> filtering;
    private ArrayList<ListOrderBy<T>> ordering;
    private int limit;
    private int offset;

    public ListCriteria() {
        filtering = new LinkedList<>();
        ordering = new ArrayList<>();
    }

    @SuppressWarnings("unchecked")
    public U where(ListFilter<T> filter) throws NullPointerException {
        filtering.add(new ListFilterGroup<T>().addMember(new ListFilterGroup.Member<>(filter, ListFilterGroup.LOGICAL_OP.AND)));
        return (U) this;
    }

    @SuppressWarnings("unchecked")
    public U and(ListFilter<T> filter) throws NullPointerException {
        filtering.getLast().addMember(new ListFilterGroup.Member<>(filter, ListFilterGroup.LOGICAL_OP.AND));
        return (U) this;
    }

    @SuppressWarnings("unchecked")
    public U where(ListCriteria<T, U> other) {
        filtering.addAll(other.filtering);
        return (U) this;
    }

    @SuppressWarnings("unchecked")
    public U and(ListCriteria<T, U> other) {
        filtering.addAll(other.filtering);
        return (U) this;
    }

    @SuppressWarnings("unchecked")
    public U or(ListFilter<T> filter) throws NullPointerException {
        filtering.getLast().addMember(new ListFilterGroup.Member<>(filter, ListFilterGroup.LOGICAL_OP.OR));
        return (U) this;
    }

    @SuppressWarnings("unchecked")
    public U not() {
        filtering.getLast().not();
        return (U) this;
    }

    @SuppressWarnings("unchecked")
    public U limit(int limit) {
        this.limit = limit;
        return (U) this;
    }

    @SuppressWarnings("unchecked")
    public U offset(int offset) {
        this.offset = offset;
        return (U) this;
    }

    public List<ListFilterGroup<T>> getFiltering() {
        return Collections.unmodifiableList(filtering);
    }

    public List<ListOrderBy<T>> getOrdering() {
        return Collections.unmodifiableList(ordering);
    }

    @SuppressWarnings("unchecked")
    public U orderBy(ListOrderBy<T> orderBy) throws NullPointerException {
        ordering.add(InvariantUtil.checkNotNullOrThrow(orderBy, "orderBy"));
        return (U) this;
    }

    public void removeOrderBy() {
        ordering = new ArrayList<>();
    }

    public int getLimit() {
        return limit;
    }

    public int getOffset() {
        return offset;
    }

    @Override
    public String toString() {
        return "ListCriteria{" +
                "filtering=" + filtering +
                ", ordering=" + ordering +
                ", limit=" + limit +
                ", offset=" + offset +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ListCriteria)) return false;
        ListCriteria<?, ?> that = (ListCriteria<?, ?>) o;
        return Objects.equals(limit, that.limit) &&
                Objects.equals(offset, that.offset) &&
                Objects.equals(filtering, that.filtering) &&
                Objects.equals(ordering, that.ordering);
    }

    @Override
    public int hashCode() {
        return Objects.hash(filtering, ordering, limit, offset);
    }
}
