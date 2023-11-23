package dk.dbc.dataio.jobstore.types.criteria;

import dk.dbc.commons.jsonb.JSONBContext;
import dk.dbc.commons.jsonb.JSONBException;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class ListCriteriaTest {
    @Test
    public void getFiltering_noFiltersAdded_returnsEmptyListOfFilterListGroups() {
        List<ListFilterGroup<ListCriteriaImpl.Field>> filtering = new ListCriteriaImpl().getFiltering();
        assertThat("List of ListFilterGroups is empty", filtering.isEmpty(), is(true));
    }

    @Test
    public void getFiltering_multipleFiltersAdded_returnsListOfFilterListGroups() {
        List<ListFilter<ListCriteriaImpl.Field>> filters = new ArrayList<>();
        filters.add(new ListFilter<>(ListCriteriaImpl.Field.FIELD_1, ListFilter.Op.EQUAL, 1));
        filters.add(new ListFilter<>(ListCriteriaImpl.Field.FIELD_2, ListFilter.Op.GREATER_THAN, 2));
        filters.add(new ListFilter<>(ListCriteriaImpl.Field.FIELD_3, ListFilter.Op.EQUAL, 3));
        filters.add(new ListFilter<>(ListCriteriaImpl.Field.FIELD_4, ListFilter.Op.GREATER_THAN, 4));

        ListCriteriaImpl listCriteria = new ListCriteriaImpl()
                .where(filters.get(0))
                .and(filters.get(1))
                .where(filters.get(2))
                .or(filters.get(3))
                .not();

        List<ListFilterGroup<ListCriteriaImpl.Field>> filtering = listCriteria.getFiltering();
        assertThat("Number of ListFilterGroups", filtering.size(), is(2));

        ListFilterGroup<ListCriteriaImpl.Field> group_1 = filtering.get(0);
        assertThat(group_1.isNot(), is(false));
        assertThat("Size of first ListFilterGroup", group_1.size(), is(2));
        Iterator<ListFilterGroup.Member<ListCriteriaImpl.Field>> groupIterator_1 = group_1.iterator();
        ListFilterGroup.Member<ListCriteriaImpl.Field> member_1_1 = groupIterator_1.next();
        assertThat("First group first member: filter", member_1_1.getFilter(), is(filters.get(0)));
        assertThat("First group first member: logical operator", member_1_1.getLogicalOperator(), is(ListFilterGroup.LOGICAL_OP.AND));
        ListFilterGroup.Member<ListCriteriaImpl.Field> member_1_2 = groupIterator_1.next();
        assertThat("First group second member: filter", member_1_2.getFilter(), is(filters.get(1)));
        assertThat("First group second member: logical operator", member_1_2.getLogicalOperator(), is(ListFilterGroup.LOGICAL_OP.AND));

        ListFilterGroup<ListCriteriaImpl.Field> group_2 = filtering.get(1);
        assertThat(group_2.isNot(), is(true));
        assertThat("Size of second ListFilterGroup", group_2.size(), is(2));
        Iterator<ListFilterGroup.Member<ListCriteriaImpl.Field>> groupIterator_2 = group_2.iterator();
        ListFilterGroup.Member<ListCriteriaImpl.Field> member_2_1 = groupIterator_2.next();
        assertThat("Second group first member: filter", member_2_1.getFilter(), is(filters.get(2)));
        assertThat("Second group first member: logical operator", member_1_1.getLogicalOperator(), is(ListFilterGroup.LOGICAL_OP.AND));
        ListFilterGroup.Member<ListCriteriaImpl.Field> member_2_2 = groupIterator_2.next();
        assertThat("Second group second member: filter", member_2_2.getFilter(), is(filters.get(3)));
        assertThat("Second group second member: logical operator", member_2_2.getLogicalOperator(), is(ListFilterGroup.LOGICAL_OP.OR));
    }

    @Test
    public void whereWithListCriteria() {
        List<ListFilter<ListCriteriaImpl.Field>> filters = new ArrayList<>();
        filters.add(new ListFilter<>(ListCriteriaImpl.Field.FIELD_1, ListFilter.Op.EQUAL, 1));
        filters.add(new ListFilter<>(ListCriteriaImpl.Field.FIELD_2, ListFilter.Op.GREATER_THAN, 2));

        ListCriteriaImpl other = new ListCriteriaImpl()
                .where(filters.get(0))
                .and(filters.get(1));
        ListCriteriaImpl listCriteria = new ListCriteriaImpl().where(other);

        assertThat(listCriteria.getFiltering().get(0).getMembers().get(0).getFilter(), is(filters.get(0)));
        assertThat(listCriteria.getFiltering().get(0).getMembers().get(1).getFilter(), is(filters.get(1)));
    }


    @Test
    public void andWithListCriteria() {
        List<ListFilter<ListCriteriaImpl.Field>> filters = new ArrayList<>();
        filters.add(new ListFilter<>(ListCriteriaImpl.Field.FIELD_1, ListFilter.Op.EQUAL, 1));
        filters.add(new ListFilter<>(ListCriteriaImpl.Field.FIELD_2, ListFilter.Op.GREATER_THAN, 2));
        filters.add(new ListFilter<>(ListCriteriaImpl.Field.FIELD_3, ListFilter.Op.EQUAL, 3));
        filters.add(new ListFilter<>(ListCriteriaImpl.Field.FIELD_4, ListFilter.Op.GREATER_THAN, 4));


        ListCriteriaImpl other = new ListCriteriaImpl()
                .where(filters.get(2))
                .and(filters.get(3));
        ListCriteriaImpl listCriteria = new ListCriteriaImpl()
                .where(filters.get(0))
                .and(filters.get(1))
                .and(other);

        assertThat(listCriteria.getFiltering().get(0).getMembers().get(0).getFilter(), is(filters.get(0)));
        assertThat(listCriteria.getFiltering().get(0).getMembers().get(1).getFilter(), is(filters.get(1)));

        assertThat(listCriteria.getFiltering().get(1).getMembers().get(0).getFilter(), is(filters.get(2)));
        assertThat(listCriteria.getFiltering().get(1).getMembers().get(1).getFilter(), is(filters.get(3)));
    }


    @Test
    public void getFiltering_returnsImmutableListOfListFilterGroup() {
        List<ListFilterGroup<ListCriteriaImpl.Field>> filtering = new ListCriteriaImpl().getFiltering();
        assertThrows(UnsupportedOperationException.class, () -> filtering.add(new ListFilterGroup<>()));
    }

    @Test
    public void getOrdering_noOrderingsAdded_returnsEmptyListOfListOrderBy() {
        List<ListOrderBy<ListCriteriaImpl.Field>> ordering = new ListCriteriaImpl().getOrdering();
        assertThat("List of ListOrderBy is empty", ordering.isEmpty(), is(true));
    }

    @Test
    public void getOrdering_multipleOrderByClausesAdded_returnsListOfListOrderBy() {
        List<ListOrderBy<ListCriteriaImpl.Field>> expectedOrdering = new ArrayList<>(2);
        expectedOrdering.add(new ListOrderBy<>(ListCriteriaImpl.Field.FIELD_1, ListOrderBy.Sort.DESC));
        expectedOrdering.add(new ListOrderBy<>(ListCriteriaImpl.Field.FIELD_2, ListOrderBy.Sort.ASC));

        ListCriteriaImpl listCriteria = new ListCriteriaImpl()
                .orderBy(expectedOrdering.get(0))
                .orderBy(expectedOrdering.get(1));

        List<ListOrderBy<ListCriteriaImpl.Field>> ordering = listCriteria.getOrdering();
        assertThat("Ordering", ordering, is(expectedOrdering));
    }

    @Test
    public void getOrdering_returnsImmutableListOfListOrderBy() {
        List<ListOrderBy<ListCriteriaImpl.Field>> ordering = new ListCriteriaImpl().getOrdering();
        assertThrows(UnsupportedOperationException.class, () -> ordering.add(new ListOrderBy<>(ListCriteriaImpl.Field.FIELD_1, ListOrderBy.Sort.DESC)));
    }

    @Test
    public void instance_whenNonEmpty_canBeMarshalledAndUnmarshalled() throws JSONBException {
        List<ListFilter<ListCriteriaImpl.Field>> filters = new ArrayList<>();
        filters.add(new ListFilter<>(ListCriteriaImpl.Field.FIELD_1, ListFilter.Op.EQUAL, 1));
        filters.add(new ListFilter<>(ListCriteriaImpl.Field.FIELD_2, ListFilter.Op.GREATER_THAN, 2));
        filters.add(new ListFilter<>(ListCriteriaImpl.Field.FIELD_3, ListFilter.Op.EQUAL, 3));
        filters.add(new ListFilter<>(ListCriteriaImpl.Field.FIELD_4));

        ListCriteriaImpl listCriteria = new ListCriteriaImpl()
                .where(filters.get(0))
                .and(filters.get(1))
                .where(filters.get(2))
                .or(filters.get(3))
                .orderBy(new ListOrderBy<>(ListCriteriaImpl.Field.FIELD_1, ListOrderBy.Sort.DESC))
                .limit(10)
                .offset(2);

        JSONBContext jsonbContext = new JSONBContext();
        jsonbContext.unmarshall(jsonbContext.marshall(listCriteria), ListCriteriaImpl.class);
    }

    @Test
    public void instance_whenEmpty_canBeMarshalledAndUnmarshalled() throws JSONBException {
        JSONBContext jsonbContext = new JSONBContext();
        jsonbContext.unmarshall(jsonbContext.marshall(new ListCriteriaImpl()), ListCriteriaImpl.class);
    }

    private static class ListCriteriaImpl extends ListCriteria<ListCriteriaImpl.Field, ListCriteriaImpl> {
        public enum Field implements ListFilterField {
            FIELD_1,
            FIELD_2,
            FIELD_3,
            FIELD_4,
        }
    }
}
