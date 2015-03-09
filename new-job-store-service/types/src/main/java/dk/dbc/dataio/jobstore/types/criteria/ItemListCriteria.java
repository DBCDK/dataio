package dk.dbc.dataio.jobstore.types.criteria;

import dk.dbc.dataio.commons.utils.invariant.InvariantUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class ItemListCriteria implements ListCriteria<ItemListCriteria.Field>{

    /**
     * Available criteria fields
     */
    public enum Field implements ListFilterField {
        /**
         * job id
         */
        JOB_ID,
        /**
         * chunk id
         */
        CHUNK_ID,
        /**
         * item id
         */
        ITEM_ID,
        /**
         * item creation time
         */
        TIME_OF_CREATION,
        /*
         * failed items
         */
        STATE_FAILED
    }

    private LinkedList<ListFilterGroup<ItemListCriteria.Field>> filtering;
    private List<ListOrderBy<ItemListCriteria.Field>> ordering;

    public ItemListCriteria() {
        filtering = new LinkedList<>();
        ordering = new ArrayList<>();
    }

    @Override
    public ItemListCriteria where(ListFilter<Field> filter) throws NullPointerException {
        filtering.add(new ListFilterGroup<Field>().addMember(new ListFilterGroup.Member(filter, ListFilterGroup.LOGICAL_OP.AND)));
        return this;
    }

    @Override
    public ItemListCriteria and(ListFilter<Field> filter) throws NullPointerException {
        filtering.getLast().addMember(new ListFilterGroup.Member(filter, ListFilterGroup.LOGICAL_OP.AND));
        return this;
    }

    @Override
    public ItemListCriteria or(ListFilter<Field> filter) throws NullPointerException {
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
    public ItemListCriteria orderBy(ListOrderBy<Field> orderBy) throws NullPointerException {
        ordering.add(InvariantUtil.checkNotNullOrThrow(orderBy, "orderBy"));
        return this;
    }
}
