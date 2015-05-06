package dk.dbc.dataio.jobstore.types.criteria;

import dk.dbc.dataio.commons.utils.invariant.InvariantUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Chunk listing ListCriteria implementation
 */
public class ChunkListCriteria implements ListCriteria<ChunkListCriteria.Field> {
    /**
     * Available criteria fields
     */
    public enum Field implements ListFilterField {
        /**
         * chunk creation time
         */
        TIME_OF_CREATION,
        /**
         * chunk completion time
         */
        TIME_OF_COMPLETION
    }

    private LinkedList<ListFilterGroup<Field>> filtering;
    private List<ListOrderBy<Field>> ordering;
    private int limit;
    private int offset;

    public ChunkListCriteria() {
        filtering = new LinkedList<>();
        ordering = new ArrayList<>();
    }

    @Override
    public ChunkListCriteria where(ListFilter<Field> filter) throws NullPointerException {
        filtering.add(new ListFilterGroup<Field>().addMember(new ListFilterGroup.Member(filter, ListFilterGroup.LOGICAL_OP.AND)));
        return this;
    }

    @Override
    public ChunkListCriteria and(ListFilter<Field> filter) throws NullPointerException {
        filtering.getLast().addMember(new ListFilterGroup.Member(filter, ListFilterGroup.LOGICAL_OP.AND));
        return this;
    }

    @Override
    public ChunkListCriteria or(ListFilter<Field> filter) throws NullPointerException {
        filtering.getLast().addMember(new ListFilterGroup.Member(filter, ListFilterGroup.LOGICAL_OP.OR));
        return this;
    }

    @Override
    public ChunkListCriteria limit(int limit) {
        this.limit = limit;
        return this;
    }

    @Override
    public ChunkListCriteria offset(int offset) {
        this.offset = offset;
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
    public ChunkListCriteria orderBy(ListOrderBy<Field> orderBy) throws NullPointerException {
        ordering.add(InvariantUtil.checkNotNullOrThrow(orderBy, "orderBy"));
        return this;
    }

    @Override
    public int getLimit() {
        return limit;
    }

    @Override
    public int getOffset() {
        return offset;
    }

}
