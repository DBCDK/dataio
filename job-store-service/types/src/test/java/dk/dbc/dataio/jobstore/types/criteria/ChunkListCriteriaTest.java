package dk.dbc.dataio.jobstore.types.criteria;

import dk.dbc.dataio.jsonb.JSONBContext;
import dk.dbc.dataio.jsonb.JSONBException;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

public class ChunkListCriteriaTest {

    @Test
    public void getFiltering_noFiltersAdded_returnsEmptyListOfFilterListGroups() {
        final List<ListFilterGroup<ChunkListCriteria.Field>> filtering = new ChunkListCriteria().getFiltering();
        assertThat("List of ListFilterGroups is empty", filtering.isEmpty(), is(true));
    }

    @Test
    public void getFiltering_multipleFiltersAdded_returnsListOfFilterListGroups() {
        final List<ListFilter<ChunkListCriteria.Field>> filters = new ArrayList<>();
        filters.add(new ListFilter<>(ChunkListCriteria.Field.TIME_OF_COMPLETION, ListFilter.Op.IS_NULL));
        filters.add(new ListFilter<>(ChunkListCriteria.Field.TIME_OF_COMPLETION, ListFilter.Op.EQUAL, 42));

        final ChunkListCriteria chunkListCriteria = new ChunkListCriteria()
                .where(filters.get(0))
                .and(filters.get(1));

        final List<ListFilterGroup<ChunkListCriteria.Field>> filtering = chunkListCriteria.getFiltering();
        assertThat("Number of ListFilterGroups", filtering.size(), is(1));

        final ListFilterGroup<ChunkListCriteria.Field> group_1 = filtering.get(0);
        assertThat("Size of first ListFilterGroup", group_1.size(), is(2));
        final Iterator<ListFilterGroup.Member<ChunkListCriteria.Field>> groupIterator_1 = group_1.iterator();
        final ListFilterGroup.Member<ChunkListCriteria.Field> member_1_1 = groupIterator_1.next();
        assertThat("First group first member: filter", member_1_1.getFilter(), is(filters.get(0)));
        assertThat("First group first member: logical operator", member_1_1.getLogicalOperator(), is(ListFilterGroup.LOGICAL_OP.AND));
        final ListFilterGroup.Member<ChunkListCriteria.Field> member_1_2 = groupIterator_1.next();
        assertThat("First group second member: filter", member_1_2.getFilter(), is(filters.get(1)));
        assertThat("First group second member: logical operator", member_1_2.getLogicalOperator(), is(ListFilterGroup.LOGICAL_OP.AND));
    }

    @Test
    public void getFiltering_returnsImmutableListOfListFilterGroup() {
        final List<ListFilterGroup<ChunkListCriteria.Field>> filtering = new ChunkListCriteria().getFiltering();
        try {
            filtering.add(new ListFilterGroup<ChunkListCriteria.Field>());
            fail("No exception thrown");
        } catch (UnsupportedOperationException e) {
        }
    }

    @Test
    public void getOrdering_noOrderingsAdded_returnsEmptyListOfListOrderBy() {
        final List<ListOrderBy<ChunkListCriteria.Field>> ordering = new ChunkListCriteria().getOrdering();
        assertThat("List of ListOrderBy is empty", ordering.isEmpty(), is(true));
    }

    @Test
    public void getOrdering_multipleOrderByClausesAdded_returnsListOfListOrderBy() {
        final List<ListOrderBy<ChunkListCriteria.Field>> expectedOrdering = new ArrayList<>(2);
        expectedOrdering.add(new ListOrderBy<>(ChunkListCriteria.Field.TIME_OF_COMPLETION, ListOrderBy.Sort.DESC));
        expectedOrdering.add(new ListOrderBy<>(ChunkListCriteria.Field.TIME_OF_CREATION, ListOrderBy.Sort.ASC));

        final ChunkListCriteria chunkListCriteria = new ChunkListCriteria()
                .orderBy(expectedOrdering.get(0))
                .orderBy(expectedOrdering.get(1));

        final List<ListOrderBy<ChunkListCriteria.Field>> ordering = chunkListCriteria.getOrdering();
        assertThat("Ordering", ordering, is(expectedOrdering));
    }

    @Test
    public void getOrdering_returnsImmutableListOfListOrderBy() {
        final List<ListOrderBy<ChunkListCriteria.Field>> ordering = new ChunkListCriteria().getOrdering();
        try {
            ordering.add(new ListOrderBy<>(ChunkListCriteria.Field.TIME_OF_CREATION, ListOrderBy.Sort.ASC));
            fail("No exception thrown");
        } catch (UnsupportedOperationException e) {
        }
    }

    @Test
    public void instance_whenNonEmpty_canBeMarshallingAndUnmarshalled() throws JSONBException {
        final List<ListFilter<ChunkListCriteria.Field>> filters = new ArrayList<>();
        filters.add(new ListFilter<>(ChunkListCriteria.Field.TIME_OF_CREATION, ListFilter.Op.IS_NOT_NULL));
        filters.add(new ListFilter<>(ChunkListCriteria.Field.TIME_OF_COMPLETION, ListFilter.Op.IS_NULL));
        filters.add(new ListFilter<>(ChunkListCriteria.Field.TIME_OF_CREATION, ListFilter.Op.EQUAL, 42));

        final ChunkListCriteria chunkListCriteria = new ChunkListCriteria()
                .where(filters.get(0))
                .and(filters.get(1))
                .and(filters.get(2))
                .orderBy(new ListOrderBy<>(ChunkListCriteria.Field.TIME_OF_CREATION, ListOrderBy.Sort.ASC))
                .limit(10)
                .offset(2);

        final JSONBContext jsonbContext = new JSONBContext();
        jsonbContext.unmarshall(jsonbContext.marshall(chunkListCriteria), ChunkListCriteria.class);
    }

    @Test
    public void instance_whenEmpty_canBeMarshallingAndUnmarshalled() throws JSONBException {
        final JSONBContext jsonbContext = new JSONBContext();
        jsonbContext.unmarshall(jsonbContext.marshall(new ChunkListCriteria()), ChunkListCriteria.class);
    }

}

