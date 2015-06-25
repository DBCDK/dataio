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

public class ItemListCriteriaTest {

    @Test
    public void getFiltering_noFiltersAdded_returnsEmptyListOfFilterListGroups() {
        final List<ListFilterGroup<ItemListCriteria.Field>> filtering = new ItemListCriteria().getFiltering();
        assertThat("List of ListFilterGroups is empty", filtering.isEmpty(), is(true));
    }

    @Test
    public void getFiltering_multipleFiltersAdded_returnsListOfFilterListGroups() {
        final List<ListFilter<ItemListCriteria.Field>> filters = new ArrayList<>();
        filters.add(new ListFilter<>(ItemListCriteria.Field.ITEM_ID, ListFilter.Op.EQUAL, 42));
        filters.add(new ListFilter<>(ItemListCriteria.Field.CHUNK_ID, ListFilter.Op.EQUAL, 43));
        filters.add(new ListFilter<>(ItemListCriteria.Field.JOB_ID, ListFilter.Op.EQUAL, 42));
        filters.add(new ListFilter<>(ItemListCriteria.Field.TIME_OF_CREATION, ListFilter.Op.GREATER_THAN, 42));

        final ItemListCriteria itemListCriteria = new ItemListCriteria()
                .where(filters.get(0))
                .and(filters.get(1))
                .where(filters.get(2))
                .or(filters.get(3));

        final List<ListFilterGroup<ItemListCriteria.Field>> filtering = itemListCriteria.getFiltering();
        assertThat("Number of ListFilterGroups", filtering.size(), is(2));

        final ListFilterGroup<ItemListCriteria.Field> group_1 = filtering.get(0);
        assertThat("Size of first ListFilterGroup", group_1.size(), is(2));
        final Iterator<ListFilterGroup.Member<ItemListCriteria.Field>> groupIterator_1 = group_1.iterator();
        final ListFilterGroup.Member<ItemListCriteria.Field> member_1_1 = groupIterator_1.next();
        assertThat("First group first member: filter", member_1_1.getFilter(), is(filters.get(0)));
        assertThat("First group first member: logical operator", member_1_1.getLogicalOperator(), is(ListFilterGroup.LOGICAL_OP.AND));
        final ListFilterGroup.Member<ItemListCriteria.Field> member_1_2 = groupIterator_1.next();
        assertThat("First group second member: filter", member_1_2.getFilter(), is(filters.get(1)));
        assertThat("First group second member: logical operator", member_1_2.getLogicalOperator(), is(ListFilterGroup.LOGICAL_OP.AND));

        final ListFilterGroup<ItemListCriteria.Field> group_2 = filtering.get(1);
        assertThat("Size of second ListFilterGroup", group_2.size(), is(2));
        final Iterator<ListFilterGroup.Member<ItemListCriteria.Field>> groupIterator_2 = group_2.iterator();
        final ListFilterGroup.Member<ItemListCriteria.Field> member_2_1 = groupIterator_2.next();
        assertThat("Second group first member: filter", member_2_1.getFilter(), is(filters.get(2)));
        assertThat("Second group first member: logical operator", member_1_1.getLogicalOperator(), is(ListFilterGroup.LOGICAL_OP.AND));
        final ListFilterGroup.Member<ItemListCriteria.Field> member_2_2 = groupIterator_2.next();
        assertThat("Second group second member: filter", member_2_2.getFilter(), is(filters.get(3)));
        assertThat("Second group second member: logical operator", member_2_2.getLogicalOperator(), is(ListFilterGroup.LOGICAL_OP.OR));
    }

    @Test
    public void getFiltering_returnsImmutableListOfListFilterGroup() {
        final List<ListFilterGroup<ItemListCriteria.Field>> filtering = new ItemListCriteria().getFiltering();
        try {
            filtering.add(new ListFilterGroup<ItemListCriteria.Field>());
            fail("No exception thrown");
        } catch (UnsupportedOperationException e) {
        }
    }

    @Test
    public void getOrdering_noOrderingsAdded_returnsEmptyListOfListOrderBy() {
        final List<ListOrderBy<ItemListCriteria.Field>> ordering = new ItemListCriteria().getOrdering();
        assertThat("List of ListOrderBy is empty", ordering.isEmpty(), is(true));
    }

    @Test
    public void getOrdering_multipleOrderByClausesAdded_returnsListOfListOrderBy() {
        final List<ListOrderBy<ItemListCriteria.Field>> expectedOrdering = new ArrayList<>(2);
        expectedOrdering.add(new ListOrderBy<>(ItemListCriteria.Field.ITEM_ID, ListOrderBy.Sort.DESC));
        expectedOrdering.add(new ListOrderBy<>(ItemListCriteria.Field.TIME_OF_CREATION, ListOrderBy.Sort.ASC));

        final ItemListCriteria itemListCriteria = new ItemListCriteria()
                .orderBy(expectedOrdering.get(0))
                .orderBy(expectedOrdering.get(1));

        final List<ListOrderBy<ItemListCriteria.Field>> ordering = itemListCriteria.getOrdering();
        assertThat("Ordering", ordering, is(expectedOrdering));
    }

    @Test
    public void getOrdering_returnsImmutableListOfListOrderBy() {
        final List<ListOrderBy<ItemListCriteria.Field>> ordering = new ItemListCriteria().getOrdering();
        try {
            ordering.add(new ListOrderBy<>(ItemListCriteria.Field.ITEM_ID, ListOrderBy.Sort.DESC));
            fail("No exception thrown");
        } catch (UnsupportedOperationException e) {
        }
    }

    @Test
    public void instance_whenNonEmpty_canBeMarshallingAndUnmarshalled() throws JSONBException {
        final List<ListFilter<ItemListCriteria.Field>> filters = new ArrayList<>();
        filters.add(new ListFilter<>(ItemListCriteria.Field.ITEM_ID, ListFilter.Op.EQUAL, 42));
        filters.add(new ListFilter<>(ItemListCriteria.Field.CHUNK_ID, ListFilter.Op.EQUAL, 42));
        filters.add(new ListFilter<>(ItemListCriteria.Field.JOB_ID, ListFilter.Op.EQUAL, 42));
        filters.add(new ListFilter<>(ItemListCriteria.Field.TIME_OF_CREATION, ListFilter.Op.GREATER_THAN, 42));

        final ItemListCriteria itemListCriteria = new ItemListCriteria()
                .where(filters.get(0))
                .and(filters.get(1))
                .where(filters.get(2))
                .or(filters.get(3))
                .orderBy(new ListOrderBy<>(ItemListCriteria.Field.ITEM_ID, ListOrderBy.Sort.DESC))
                .limit(10)
                .offset(2);

        final JSONBContext jsonbContext = new JSONBContext();
        jsonbContext.unmarshall(jsonbContext.marshall(itemListCriteria), ItemListCriteria.class);
    }

    @Test
    public void instance_whenEmpty_canBeMarshallingAndUnmarshalled() throws JSONBException {
        final JSONBContext jsonbContext = new JSONBContext();
        jsonbContext.unmarshall(jsonbContext.marshall(new ItemListCriteria()), ItemListCriteria.class);
    }

    @Test
    public void instance_whenVerbatimFieldConstruct_canBeMarshalAndUnmarshalled() throws JSONBException {
        final List<ListFilter<ItemListCriteria.Field>> filters = new ArrayList<>();
        filters.add(new ListFilter<>(ItemListCriteria.Field.STATE_FAILED));
        filters.add(new ListFilter<>(ItemListCriteria.Field.STATE_IGNORED));

        final ListCriteria itemListCriteria = new ItemListCriteria()
                .where(filters.get(0))
                .and(filters.get(1))
                .orderBy(new ListOrderBy<>(ItemListCriteria.Field.ITEM_ID, ListOrderBy.Sort.DESC));

        final JSONBContext jsonbContext = new JSONBContext();
        jsonbContext.unmarshall(jsonbContext.marshall(itemListCriteria), ItemListCriteria.class);
    }
}
