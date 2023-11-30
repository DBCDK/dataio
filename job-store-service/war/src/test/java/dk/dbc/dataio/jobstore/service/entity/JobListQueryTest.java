package dk.dbc.dataio.jobstore.service.entity;

import dk.dbc.dataio.jobstore.types.JobInfoSnapshot;
import dk.dbc.dataio.jobstore.types.criteria.JobListCriteria;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class JobListQueryTest {
    private static final EntityManager ENTITY_MANAGER = mock(EntityManager.class);
    private static final Query QUERY = mock(Query.class);

    @Test
    public void constructor_entityManagerArgIsNull_throws() {
        assertThrows(NullPointerException.class, () -> new JobListQuery(null));
    }

    @Test
    public void execute_criteriaArgIsNull_throws() {
        JobListQuery jobListQuery = new JobListQuery(ENTITY_MANAGER);
        assertThrows(NullPointerException.class, () -> jobListQuery.execute((JobListCriteria) null));
    }

    @Test
    public void execute_queryReturnsEmptyList_returnsEmptySnapshotList() {
        when(ENTITY_MANAGER.createNativeQuery(JobListQuery.QUERY_BASE, JobEntity.class)).thenReturn(QUERY);
        when(QUERY.getResultList()).thenReturn(Collections.emptyList());

        JobListQuery jobListQuery = new JobListQuery(ENTITY_MANAGER);
        List<JobInfoSnapshot> jobInfoSnapshots = jobListQuery.execute(new JobListCriteria());
        assertThat("List of JobInfoSnapshot", jobInfoSnapshots, is(notNullValue()));
        assertThat("List of JobInfoSnapshot is empty", jobInfoSnapshots.isEmpty(), is(true));
    }

    @Test
    public void execute_queryReturnsNonEmptyList_returnsSnapshotList() {
        JobEntity jobEntity1 = new JobEntity();
        jobEntity1.setNumberOfItems(42);
        JobEntity jobEntity2 = new JobEntity();
        jobEntity2.setNumberOfItems(4242);
        when(ENTITY_MANAGER.createNativeQuery(JobListQuery.QUERY_BASE, JobEntity.class)).thenReturn(QUERY);
        when(QUERY.getResultList()).thenReturn(Arrays.asList(jobEntity1, jobEntity2));

        JobListQuery jobListQuery = new JobListQuery(ENTITY_MANAGER);
        List<JobInfoSnapshot> jobInfoSnapshots = jobListQuery.execute(new JobListCriteria());
        assertThat("List of JobInfoSnapshot", jobInfoSnapshots, is(notNullValue()));
        assertThat("List of JobInfoSnapshot size", jobInfoSnapshots.size(), is(2));
        assertThat("List of JobInfoSnapshot first element numberOfItems",
                jobInfoSnapshots.get(0).getNumberOfItems(), is(jobEntity1.getNumberOfItems()));
        assertThat("List of JobInfoSnapshot second element numberOfItems",
                jobInfoSnapshots.get(1).getNumberOfItems(), is(jobEntity2.getNumberOfItems()));
    }
}
