/*
 * DataIO - Data IO
 * Copyright (C) 2015 Dansk Bibliotekscenter a/s, Tempovej 7-11, DK-2750 Ballerup,
 * Denmark. CVR: 15149043
 *
 * This file is part of DataIO.
 *
 * DataIO is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * DataIO is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with DataIO.  If not, see <http://www.gnu.org/licenses/>.
 */

package dk.dbc.dataio.jobstore.service.entity;

import dk.dbc.dataio.jobstore.types.JobInfoSnapshot;
import dk.dbc.dataio.jobstore.types.criteria.JobListCriteria;
import org.junit.Test;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
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
            jobListQuery.execute((JobListCriteria) null);
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
}
