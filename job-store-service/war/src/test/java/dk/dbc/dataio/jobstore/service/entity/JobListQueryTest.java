package dk.dbc.dataio.jobstore.service.entity;

import dk.dbc.dataio.jobstore.types.JobInfoSnapshot;
import dk.dbc.dataio.jobstore.types.criteria.JobListCriteria;
import dk.dbc.dataio.jobstore.types.criteria.ListFilter;
import dk.dbc.dataio.jobstore.types.criteria.ListOrderBy;
import org.junit.Test;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class JobListQueryTest {
    private static final EntityManager ENTITY_MANAGER = mock(EntityManager.class);
    private static final Query QUERY = mock(Query.class);

    @Test(expected = NullPointerException.class)
    public void constructor_entityManagerArgIsNull_throws() {
        new JobListQuery(null);
    }

    @Test
    public void execute_criteriaArgIsNull_throws() {
        final JobListQuery jobListQuery = new JobListQuery(ENTITY_MANAGER);
        try {
            jobListQuery.execute(null);
            fail("No exception thrown");
        } catch (NullPointerException e) {
        }
    }

    @Test
    public void execute_queryReturnsEmptyList_returnsEmptySnapshotList() {
        when(ENTITY_MANAGER.createNativeQuery(JobListQuery.QUERY_BASE, JobEntity.class)).thenReturn(QUERY);
        when(QUERY.getResultList()).thenReturn(Collections.emptyList());

        final JobListQuery jobListQuery = new JobListQuery(ENTITY_MANAGER);
        final List<JobInfoSnapshot> jobInfoSnapshots = jobListQuery.execute(new JobListCriteria());
        assertThat("List of JobInfoSnapshot", jobInfoSnapshots, is(notNullValue()));
        assertThat("List of JobInfoSnapshot is empty", jobInfoSnapshots.isEmpty(), is(true));
    }

    @Test
    public void execute_queryReturnsNonEmptyList_returnsSnapshotList() {
        final JobEntity jobEntity1 = new JobEntity();
        jobEntity1.setNumberOfItems(42);
        final JobEntity jobEntity2 = new JobEntity();
        jobEntity2.setNumberOfItems(4242);
        when(ENTITY_MANAGER.createNativeQuery(JobListQuery.QUERY_BASE, JobEntity.class)).thenReturn(QUERY);
        when(QUERY.getResultList()).thenReturn(Arrays.asList(jobEntity1, jobEntity2));

        final JobListQuery jobListQuery = new JobListQuery(ENTITY_MANAGER);
        final List<JobInfoSnapshot> jobInfoSnapshots = jobListQuery.execute(new JobListCriteria());
        assertThat("List of JobInfoSnapshot", jobInfoSnapshots, is(notNullValue()));
        assertThat("List of JobInfoSnapshot size", jobInfoSnapshots.size(), is(2));
        assertThat("List of JobInfoSnapshot first element numberOfItems",
                jobInfoSnapshots.get(0).getNumberOfItems(), is(jobEntity1.getNumberOfItems()));
        assertThat("List of JobInfoSnapshot second element numberOfItems",
                jobInfoSnapshots.get(1).getNumberOfItems(), is(jobEntity2.getNumberOfItems()));
    }

    @Test
    public void buildQueryString_noCriteria_returnsQueryBase() {
        final JobListQuery jobListQuery = new JobListQuery(ENTITY_MANAGER);
        assertThat(jobListQuery.buildQueryString(JobListQuery.QUERY_BASE, new JobListCriteria()), is(JobListQuery.QUERY_BASE));
    }

    @Test
    public void buildQueryString_singleOrderByClause_returnsQueryString() {
        final String expectedQuery = JobListQuery.QUERY_BASE + " ORDER BY id ASC";
        final JobListQuery jobListQuery = new JobListQuery(ENTITY_MANAGER);
        final JobListCriteria jobListCriteria = new JobListCriteria()
                .orderBy(new ListOrderBy<>(JobListCriteria.Field.JOB_ID, ListOrderBy.Sort.ASC));
        assertThat(jobListQuery.buildQueryString(JobListQuery.QUERY_BASE, jobListCriteria), is(expectedQuery));
    }

    @Test
    public void buildQueryString_multipleOrderByClauses_returnsQueryString() {
        final String expectedQuery = JobListQuery.QUERY_BASE + " ORDER BY id ASC, timeOfCreation DESC";
        final JobListQuery jobListQuery = new JobListQuery(ENTITY_MANAGER);
        final JobListCriteria jobListCriteria = new JobListCriteria()
                .orderBy(new ListOrderBy<>(JobListCriteria.Field.JOB_ID, ListOrderBy.Sort.ASC))
                .orderBy(new ListOrderBy<>(JobListCriteria.Field.TIME_OF_CREATION, ListOrderBy.Sort.DESC));
        assertThat(jobListQuery.buildQueryString(JobListQuery.QUERY_BASE, jobListCriteria), is(expectedQuery));
    }

    @Test
    public void buildQueryString_singleWhereClause_returnsQueryString() {
        final String expectedQuery = JobListQuery.QUERY_BASE + " WHERE id=?1";
        final JobListQuery jobListQuery = new JobListQuery(ENTITY_MANAGER);
        final JobListCriteria jobListCriteria = new JobListCriteria()
                .where(new ListFilter<>(JobListCriteria.Field.JOB_ID, ListFilter.Op.EQUAL, 42));
        assertThat(jobListQuery.buildQueryString(JobListQuery.QUERY_BASE, jobListCriteria), is(expectedQuery));
    }

    @Test
    public void buildQueryString_singleWhereClauseMultipleFilters_returnsQueryString() {
        final String expectedQuery = JobListQuery.QUERY_BASE + " WHERE id=?1 AND timeOfCreation>?2 OR timeOfLastModification<?3";
        final JobListQuery jobListQuery = new JobListQuery(ENTITY_MANAGER);
        final JobListCriteria jobListCriteria = new JobListCriteria()
                .where(new ListFilter<>(JobListCriteria.Field.JOB_ID, ListFilter.Op.EQUAL, 42))
                .and(new ListFilter<>(JobListCriteria.Field.TIME_OF_CREATION, ListFilter.Op.GREATER_THAN, 42))
                .or(new ListFilter<>(JobListCriteria.Field.TIME_OF_LAST_MODIFICATION, ListFilter.Op.LESS_THAN, 42));
        assertThat(jobListQuery.buildQueryString(JobListQuery.QUERY_BASE, jobListCriteria), is(expectedQuery));
    }

    @Test
    public void buildQueryString_multipleWhereClauses_returnsQueryString() {
        final String expectedQuery = JobListQuery.QUERY_BASE + " WHERE ( id=?1 ) AND ( timeOfCreation>?2 )";
        final JobListQuery jobListQuery = new JobListQuery(ENTITY_MANAGER);
        final JobListCriteria jobListCriteria = new JobListCriteria()
                .where(new ListFilter<>(JobListCriteria.Field.JOB_ID, ListFilter.Op.EQUAL, 42))
                .where(new ListFilter<>(JobListCriteria.Field.TIME_OF_CREATION, ListFilter.Op.GREATER_THAN, 42));
        assertThat(jobListQuery.buildQueryString(JobListQuery.QUERY_BASE, jobListCriteria), is(expectedQuery));
    }

    @Test
    public void buildQueryString_multipleWhereClausesMultipleFilters_returnsQueryString() {
        final String expectedQuery = JobListQuery.QUERY_BASE + " WHERE ( id=?1 AND timeOfCreation<=?2 ) AND ( timeOfCreation>?3 OR timeOfLastModification<?4 )";
        final JobListQuery jobListQuery = new JobListQuery(ENTITY_MANAGER);
        final JobListCriteria jobListCriteria = new JobListCriteria()
                .where(new ListFilter<>(JobListCriteria.Field.JOB_ID, ListFilter.Op.EQUAL, 42))
                .and(new ListFilter<>(JobListCriteria.Field.TIME_OF_CREATION, ListFilter.Op.LESS_THAN_OR_EQUAL_TO, 42))
                .where(new ListFilter<>(JobListCriteria.Field.TIME_OF_CREATION, ListFilter.Op.GREATER_THAN, 42))
                .or(new ListFilter<>(JobListCriteria.Field.TIME_OF_LAST_MODIFICATION, ListFilter.Op.LESS_THAN, 42));
        assertThat(jobListQuery.buildQueryString(JobListQuery.QUERY_BASE, jobListCriteria), is(expectedQuery));
    }

    @Test
    public void buildQueryString_SingleWhereClauseLimitClause_returnsQueryString() {
        final String expectedQuery = JobListQuery.QUERY_BASE + " LIMIT 10";
        final JobListQuery jobListQuery = new JobListQuery(ENTITY_MANAGER);
        final JobListCriteria jobListCriteria = new JobListCriteria().limit(10);
        assertThat(jobListQuery.buildQueryString(JobListQuery.QUERY_BASE, jobListCriteria), is(expectedQuery));
    }

    @Test
    public void buildQueryString_SingleWhereClauseLimitClauseZero_returnsQueryString() {
        final String expectedQuery = JobListQuery.QUERY_BASE;
        final JobListQuery jobListQuery = new JobListQuery(ENTITY_MANAGER);
        final JobListCriteria jobListCriteria = new JobListCriteria().limit(0);
        assertThat(jobListQuery.buildQueryString(JobListQuery.QUERY_BASE, jobListCriteria), is(expectedQuery));
    }

    @Test
    public void buildQueryString_SingleWhereClauseOffsetClause_returnsQueryString() {
        final String expectedQuery = JobListQuery.QUERY_BASE + " OFFSET 20";
        final JobListQuery jobListQuery = new JobListQuery(ENTITY_MANAGER);
        final JobListCriteria jobListCriteria = new JobListCriteria().offset(20);
        assertThat(jobListQuery.buildQueryString(JobListQuery.QUERY_BASE, jobListCriteria), is(expectedQuery));
    }

    @Test
    public void buildQueryString_SingleWhereClauseOffsetClauseZero_returnsQueryString() {
        final String expectedQuery = JobListQuery.QUERY_BASE;
        final JobListQuery jobListQuery = new JobListQuery(ENTITY_MANAGER);
        final JobListCriteria jobListCriteria = new JobListCriteria().offset(0);
        assertThat(jobListQuery.buildQueryString(JobListQuery.QUERY_BASE, jobListCriteria), is(expectedQuery));
    }

    @Test
    public void buildQueryString_SingleWhereClauseSingleFiltersSingleOrderByClauses_returnsQueryString() {
        final String expectedQuery = JobListQuery.QUERY_BASE +
                " WHERE timeOfCreation>?1 " +
                "AND (state->'states'->'PROCESSING'->>'failed' != '0') " +
                "OR (state->'states'->'DELIVERING'->>'failed' != '0') " +
                "ORDER BY timeOfCreation DESC";

        final JobListQuery jobListQuery = new JobListQuery(ENTITY_MANAGER);
        final JobListCriteria jobListCriteria = new JobListCriteria()
                .where(new ListFilter<>(JobListCriteria.Field.TIME_OF_CREATION, ListFilter.Op.GREATER_THAN, 42))
                .and(new ListFilter<>(JobListCriteria.Field.STATE_PROCESSING_FAILED))
                .or(new ListFilter<>(JobListCriteria.Field.STATE_DELIVERING_FAILED))
                .orderBy(new ListOrderBy<>(JobListCriteria.Field.TIME_OF_CREATION, ListOrderBy.Sort.DESC));
        assertThat(jobListQuery.buildQueryString(JobListQuery.QUERY_BASE, jobListCriteria), is(expectedQuery));
    }
}