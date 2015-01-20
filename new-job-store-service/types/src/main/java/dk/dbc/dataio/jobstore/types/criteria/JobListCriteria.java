package dk.dbc.dataio.jobstore.types.criteria;

import dk.dbc.dataio.commons.utils.invariant.InvariantUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Job listing ListCriteria implementation
 */
public class JobListCriteria implements ListCriteria<JobListCriteria.Field> {
    /**
     * Available criteria fields
     */
    public enum Field implements ListFilterField {
        /**
         * job id
         */
        JOB_ID,
        /**
         * job creation time
         */
        TIME_OF_CREATION,
        /**
         * job last modification time
         */
        TIME_OF_LAST_MODIFICATION
    }

    private LinkedList<ListFilterGroup<JobListCriteria.Field>> filtering;
    private List<ListOrderBy<JobListCriteria.Field>> ordering;

    public JobListCriteria() {
        filtering = new LinkedList<>();
        ordering = new ArrayList<>();
    }

    @Override
    public JobListCriteria where(ListFilter<Field> filter) throws NullPointerException {
        filtering.add(new ListFilterGroup<Field>().addMember(new ListFilterGroup.Member(filter, ListFilterGroup.LOGICAL_OP.AND)));
        return this;
    }

    @Override
    public JobListCriteria and(ListFilter<Field> filter) throws NullPointerException {
        filtering.getLast().addMember(new ListFilterGroup.Member(filter, ListFilterGroup.LOGICAL_OP.AND));
        return this;
    }

    @Override
    public JobListCriteria or(ListFilter<Field> filter) throws NullPointerException {
        filtering.getLast().addMember(new ListFilterGroup.Member(filter, ListFilterGroup.LOGICAL_OP.OR));
        return this;
    }

    @Override
    public List<ListFilterGroup<Field>> getFiltering() {
        return Collections.unmodifiableList(filtering);
    }

    @Override
    public List<ListOrderBy<Field>> getOrdering() {
        return Collections.unmodifiableList(ordering);
    }

    @Override
    public JobListCriteria orderBy(ListOrderBy<Field> orderBy) throws NullPointerException {
        ordering.add(InvariantUtil.checkNotNullOrThrow(orderBy, "orderBy"));
        return this;
    }
}
