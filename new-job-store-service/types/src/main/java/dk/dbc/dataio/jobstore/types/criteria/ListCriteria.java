package dk.dbc.dataio.jobstore.types.criteria;

import java.util.List;

/**
 * Interface for listings criteria
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
public interface ListCriteria<T extends ListFilterField> {
    ListCriteria<T> where(ListFilter<T> filter);
    ListCriteria<T> and(ListFilter<T> filter);
    ListCriteria<T> or(ListFilter<T> filter);
    List<ListFilterGroup<T>> getFiltering();
    ListCriteria<T> orderBy(ListOrderBy<T> orderBy);
    List<ListOrderBy<T>> getOrdering();
}
