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

package dk.dbc.dataio.jobstore.service.ejb;

import dk.dbc.dataio.commons.types.JobSpecification;
import dk.dbc.dataio.commons.utils.test.model.JobSpecificationBuilder;
import dk.dbc.dataio.jobstore.service.entity.JobEntity;
import dk.dbc.dataio.jobstore.types.State;
import org.junit.Test;

import javax.persistence.LockModeType;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

public class PgJobStoreRepositoryTest extends PgJobStoreBaseTest {
    @Test
    public void resetJob_whenJobExists_returnsResetEntity() {
        final int jobId = 1;
        final JobSpecification specification = new JobSpecificationBuilder().build();
        final State state = new State();
        final JobEntity jobEntity = new JobEntity();
        jobEntity.setSpecification(specification);
        jobEntity.setState(state);
        jobEntity.setNumberOfChunks(42);
        jobEntity.setNumberOfItems(420);

        whenCreateQueryThenReturn();
        when(entityManager.find(JobEntity.class, jobId, LockModeType.PESSIMISTIC_WRITE)).thenReturn(jobEntity);

        final PgJobStoreRepository pgJobStoreRepository = newPgJobStoreReposity();
        final JobEntity resetJob = pgJobStoreRepository.resetJob(jobId);

        assertThat("numberOfChunks reset", resetJob.getNumberOfChunks(), is(0));
        assertThat("numberOfItems reset", resetJob.getNumberOfItems(), is(0));
        assertThat("state reset", resetJob.getState() == state, is(false));
        assertThat("specification unchanged", resetJob.getSpecification() == specification, is(true));
    }

    @Test
    public void resetJob_whenJobDoesNotExist_returnsNull() {
        final int jobId = 1;
        when(entityManager.find(JobEntity.class, jobId, LockModeType.PESSIMISTIC_WRITE)).thenReturn(null);

        final PgJobStoreRepository pgJobStoreRepository = newPgJobStoreReposity();
        assertThat(pgJobStoreRepository.resetJob(jobId), is(nullValue()));
    }
}