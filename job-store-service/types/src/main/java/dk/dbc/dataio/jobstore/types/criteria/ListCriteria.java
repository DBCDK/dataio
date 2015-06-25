package dk.dbc.dataio.jobstore.types.criteria;

import dk.dbc.dataio.commons.utils.invariant.InvariantUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

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
 * for (ListFilterGroup<ListCriteriaImplField> filters : listCriteria.getFiltering()) {
 *     ...
 * }
 *
 * for (ListOrderBy<ListCriteriaImplField> orderBy : listCriteria.getOrdering()) {
 *     ...
 * }
 * </code>
 * </pre>
 *
 * Upcoming features:
 * <pre>
 *   andWhere(ListFilter<T> filter)
 *   orWhere(ListFilter<T> filter)
 *   whereIn(T, List<Object>)
 * </pre>
 */
public abstract class ListCriteria<T extends ListFilterField, U extends ListCriteria<T,U>> {
    private LinkedList<ListFilterGroup<T>> filtering;
    private List<ListOrderBy<T>> ordering;
    private int limit;
    private int offset;

    public ListCriteria() {
        filtering = new LinkedList<>();
        ordering = new ArrayList<>();
    }

    @SuppressWarnings("unchecked")
    public U where(ListFilter<T> filter) throws NullPointerException {
        filtering.add(new ListFilterGroup<T>().addMember(new ListFilterGroup.Member<>(filter, ListFilterGroup.LOGICAL_OP.AND)));
        return (U)this;
    }

    @SuppressWarnings("unchecked")
    public U and(ListFilter<T> filter) throws NullPointerException {
        filtering.getLast().addMember(new ListFilterGroup.Member<>(filter, ListFilterGroup.LOGICAL_OP.AND));
        return (U)this;
    }

    @SuppressWarnings("unchecked")
    public U or(ListFilter<T> filter) throws NullPointerException {
        filtering.getLast().addMember(new ListFilterGroup.Member<>(filter, ListFilterGroup.LOGICAL_OP.OR));
        return (U)this;
    }

    @SuppressWarnings("unchecked")
    public U limit(int limit) {
        this.limit = limit;
        return (U)this;
    }

    @SuppressWarnings("unchecked")
    public U offset(int offset) {
        this.offset = offset;
        return (U)this;
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
        return (U)this;
    }

    public int getLimit() {
        return limit;
    }

    public int getOffset() {
        return offset;
    }
}
