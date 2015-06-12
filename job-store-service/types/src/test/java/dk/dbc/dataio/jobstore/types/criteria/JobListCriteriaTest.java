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

public class JobListCriteriaTest {
    @Test
    public void getFiltering_noFiltersAdded_returnsEmptyListOfFilterListGroups() {
        final List<ListFilterGroup<JobListCriteria.Field>> filtering = new JobListCriteria().getFiltering();
        assertThat("List of ListFilterGroups is empty", filtering.isEmpty(), is(true));
    }

    @Test
    public void getFiltering_multipleFiltersAdded_returnsListOfFilterListGroups() {
        final List<ListFilter<JobListCriteria.Field>> filters = new ArrayList<>();
        filters.add(new ListFilter<>(JobListCriteria.Field.JOB_ID, ListFilter.Op.EQUAL, 42));
        filters.add(new ListFilter<>(JobListCriteria.Field.TIME_OF_CREATION, ListFilter.Op.GREATER_THAN, 42));
        filters.add(new ListFilter<>(JobListCriteria.Field.JOB_ID, ListFilter.Op.EQUAL, 43));
        filters.add(new ListFilter<>(JobListCriteria.Field.TIME_OF_LAST_MODIFICATION, ListFilter.Op.GREATER_THAN, 43));

        final ListCriteria jobListCriteria = new JobListCriteria()
                .where(filters.get(0))
                .and(filters.get(1))
                .where(filters.get(2))
                .or(filters.get(3));

        final List<ListFilterGroup<JobListCriteria.Field>> filtering = jobListCriteria.getFiltering();
        assertThat("Number of ListFilterGroups", filtering.size(), is(2));

        final ListFilterGroup<JobListCriteria.Field> group_1 = filtering.get(0);
        assertThat("Size of first ListFilterGroup", group_1.size(), is(2));
        final Iterator<ListFilterGroup.Member<JobListCriteria.Field>> groupIterator_1 = group_1.iterator();
        final ListFilterGroup.Member<JobListCriteria.Field> member_1_1 = groupIterator_1.next();
        assertThat("First group first member: filter", member_1_1.getFilter(), is(filters.get(0)));
        assertThat("First group first member: logical operator", member_1_1.getLogicalOperator(), is(ListFilterGroup.LOGICAL_OP.AND));
        final ListFilterGroup.Member<JobListCriteria.Field> member_1_2 = groupIterator_1.next();
        assertThat("First group second member: filter", member_1_2.getFilter(), is(filters.get(1)));
        assertThat("First group second member: logical operator", member_1_2.getLogicalOperator(), is(ListFilterGroup.LOGICAL_OP.AND));

        final ListFilterGroup<JobListCriteria.Field> group_2 = filtering.get(1);
        assertThat("Size of second ListFilterGroup", group_2.size(), is(2));
        final Iterator<ListFilterGroup.Member<JobListCriteria.Field>> groupIterator_2 = group_2.iterator();
        final ListFilterGroup.Member<JobListCriteria.Field> member_2_1 = groupIterator_2.next();
        assertThat("Second group first member: filter", member_2_1.getFilter(), is(filters.get(2)));
        assertThat("Second group first member: logical operator", member_1_1.getLogicalOperator(), is(ListFilterGroup.LOGICAL_OP.AND));
        final ListFilterGroup.Member<JobListCriteria.Field> member_2_2 = groupIterator_2.next();
        assertThat("Second group second member: filter", member_2_2.getFilter(), is(filters.get(3)));
        assertThat("Second group second member: logical operator", member_2_2.getLogicalOperator(), is(ListFilterGroup.LOGICAL_OP.OR));
    }

    @Test
    public void getFiltering_returnsImmutableListOfListFilterGroup() {
        final List<ListFilterGroup<JobListCriteria.Field>> filtering = new JobListCriteria().getFiltering();
        try {
            filtering.add(new ListFilterGroup<JobListCriteria.Field>());
            fail("No exception thrown");
        } catch (UnsupportedOperationException e) {
        }
    }

    @Test
    public void getOrdering_noOrderingsAdded_returnsEmptyListOfListOrderBy() {
        final List<ListOrderBy<JobListCriteria.Field>> ordering = new JobListCriteria().getOrdering();
        assertThat("List of ListOrderBy is empty", ordering.isEmpty(), is(true));
    }

    @Test
    public void getOrdering_multipleOrderByClausesAdded_returnsListOfListOrderBy() {
        final List<ListOrderBy<JobListCriteria.Field>> expectedOrdering = new ArrayList<>(2);
        expectedOrdering.add(new ListOrderBy<>(JobListCriteria.Field.JOB_ID, ListOrderBy.Sort.DESC));
        expectedOrdering.add(new ListOrderBy<>(JobListCriteria.Field.TIME_OF_CREATION, ListOrderBy.Sort.ASC));

        final ListCriteria jobListCriteria = new JobListCriteria()
                .orderBy(expectedOrdering.get(0))
                .orderBy(expectedOrdering.get(1));

        final List<ListOrderBy<JobListCriteria.Field>> ordering = jobListCriteria.getOrdering();
        assertThat("Ordering", ordering, is(expectedOrdering));
    }

    @Test
    public void getOrdering_returnsImmutableListOfListOrderBy() {
        final List<ListOrderBy<JobListCriteria.Field>> ordering = new JobListCriteria().getOrdering();
        try {
            ordering.add(new ListOrderBy<>(JobListCriteria.Field.JOB_ID, ListOrderBy.Sort.DESC));
            fail("No exception thrown");
        } catch (UnsupportedOperationException e) {
        }
    }

    @Test
    public void instance_whenNonEmpty_canBeMarshallingAndUnmarshalled() throws JSONBException {
        final List<ListFilter<JobListCriteria.Field>> filters = new ArrayList<>();
        filters.add(new ListFilter<>(JobListCriteria.Field.JOB_ID, ListFilter.Op.EQUAL, 42));
        filters.add(new ListFilter<>(JobListCriteria.Field.TIME_OF_CREATION, ListFilter.Op.GREATER_THAN, 42));
        filters.add(new ListFilter<>(JobListCriteria.Field.JOB_ID, ListFilter.Op.EQUAL, 43));
        filters.add(new ListFilter<>(JobListCriteria.Field.TIME_OF_LAST_MODIFICATION, ListFilter.Op.GREATER_THAN, 43));
        filters.add(new ListFilter<>(JobListCriteria.Field.SINK_ID, ListFilter.Op.EQUAL, 42));

        final ListCriteria jobListCriteria = new JobListCriteria()
                .where(filters.get(0))
                .and(filters.get(1))
                .where(filters.get(2))
                .or(filters.get(3))
                .and(filters.get(4))
                .orderBy(new ListOrderBy<>(JobListCriteria.Field.JOB_ID, ListOrderBy.Sort.DESC))
                .limit(10)
                .offset(2);


        final JSONBContext jsonbContext = new JSONBContext();
        jsonbContext.unmarshall(jsonbContext.marshall(jobListCriteria), JobListCriteria.class);
    }

    @Test
    public void instance_whenEmpty_canBeMarshallingAndUnmarshalled() throws JSONBException {
        final JSONBContext jsonbContext = new JSONBContext();
        jsonbContext.unmarshall(jsonbContext.marshall(new JobListCriteria()), JobListCriteria.class);
    }

    @Test
    public void instance_whenVerbatimFieldConstruct_canBeMarshalAndUnmarshalled() throws JSONBException {
        final List<ListFilter<JobListCriteria.Field>> filters = new ArrayList<>();
        filters.add(new ListFilter<>(JobListCriteria.Field.STATE_PROCESSING_FAILED));
        filters.add(new ListFilter<>(JobListCriteria.Field.STATE_DELIVERING_FAILED));

        final ListCriteria jobListCriteria = new JobListCriteria()
                .where(filters.get(0))
                .and(filters.get(1))
                .orderBy(new ListOrderBy<>(JobListCriteria.Field.TIME_OF_CREATION, ListOrderBy.Sort.DESC));

        final JSONBContext jsonbContext = new JSONBContext();
        jsonbContext.unmarshall(jsonbContext.marshall(jobListCriteria), JobListCriteria.class);
    }
}