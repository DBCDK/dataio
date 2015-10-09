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

import dk.dbc.dataio.commons.types.Flow;
import dk.dbc.dataio.commons.types.JobSpecification;
import dk.dbc.dataio.commons.types.Sink;
import dk.dbc.dataio.commons.utils.test.model.FlowBuilder;
import dk.dbc.dataio.commons.utils.test.model.JobSpecificationBuilder;
import dk.dbc.dataio.commons.utils.test.model.SinkBuilder;
import dk.dbc.dataio.jobstore.service.entity.JobEntity;
import dk.dbc.dataio.jobstore.types.JobStoreException;
import dk.dbc.dataio.jobstore.types.ResourceBundle;
import dk.dbc.dataio.jobstore.types.State;
import org.junit.Test;

import javax.persistence.LockModeType;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

public class PgJobStoreRepositoryTest extends PgJobStoreBaseTest {
    @Test
    public void resetJob_whenJobExists_returnsResetEntity() {
        final JobSpecification specification = new JobSpecificationBuilder().build();
        final State state = new State();
        final JobEntity jobEntity = new JobEntity();
        jobEntity.setSpecification(specification);
        jobEntity.setState(state);
        jobEntity.setNumberOfChunks(42);
        jobEntity.setNumberOfItems(420);

        whenCreateQueryThenReturn();
        when(entityManager.find(JobEntity.class, DEFAULT_JOB_ID, LockModeType.PESSIMISTIC_WRITE)).thenReturn(jobEntity);

        final PgJobStoreRepository pgJobStoreRepository = newPgJobStoreReposity();
        final JobEntity resetJob = pgJobStoreRepository.resetJob(DEFAULT_JOB_ID);

        assertThat("numberOfChunks reset", resetJob.getNumberOfChunks(), is(0));
        assertThat("numberOfItems reset", resetJob.getNumberOfItems(), is(0));
        assertThat("state reset", resetJob.getState() == state, is(false));
        assertThat("specification unchanged", resetJob.getSpecification() == specification, is(true));
    }

    @Test
    public void resetJob_whenJobDoesNotExist_returnsNull() {
        when(entityManager.find(JobEntity.class, DEFAULT_JOB_ID, LockModeType.PESSIMISTIC_WRITE)).thenReturn(null);

        final PgJobStoreRepository pgJobStoreRepository = newPgJobStoreReposity();
        assertThat(pgJobStoreRepository.resetJob(DEFAULT_JOB_ID), is(nullValue()));
    }

    @Test
    public void getResourceBundle_jobEntityNotFound_throws() throws JobStoreException {
        final PgJobStoreRepository pgJobStoreRepository = newPgJobStoreReposity();
        when(entityManager.find(eq(JobEntity.class), anyInt())).thenReturn(null);
        try {
            pgJobStoreRepository.getResourceBundle(DEFAULT_JOB_ID);
            fail("No exception thrown");
        } catch (JobStoreException e) {}
    }

    @Test
    public void getResourceBundle_flowIsNull_throws() throws JobStoreException {
        final Sink sink = new SinkBuilder().build();

        final JobEntity jobEntity = getJobEntity(DEFAULT_JOB_ID);
        when(jobEntity.getCachedFlow().getFlow()).thenReturn(null);
        when(jobEntity.getCachedSink().getSink()).thenReturn(sink);

        final PgJobStoreRepository pgJobStoreRepository = newPgJobStoreReposity();
        try {
            pgJobStoreRepository.getResourceBundle(DEFAULT_JOB_ID);
            fail("No exception thrown");
        } catch (NullPointerException e) {}
    }

    @Test
    public void getResourceBundle_sinkIsNull_throws() throws JobStoreException {
        final Flow flow = new FlowBuilder().build();

        final JobEntity jobEntity = getJobEntity(DEFAULT_JOB_ID);
        when(jobEntity.getCachedFlow().getFlow()).thenReturn(flow);
        when(jobEntity.getCachedSink().getSink()).thenReturn(null);

        final PgJobStoreRepository pgJobStoreRepository = newPgJobStoreReposity();
        try {
            pgJobStoreRepository.getResourceBundle(DEFAULT_JOB_ID);
            fail("No exception thrown");
        } catch (NullPointerException e) {}
    }

    @Test
    public void getResourceBundle_resourcesAddedToBundle_returns() throws JobStoreException{
        final Flow flow = new FlowBuilder().build();
        final Sink sink = new SinkBuilder().build();

        final JobEntity jobEntity = getJobEntity(DEFAULT_JOB_ID);
        when(jobEntity.getCachedFlow().getFlow()).thenReturn(flow);
        when(jobEntity.getCachedSink().getSink()).thenReturn(sink);

        final PgJobStoreRepository pgJobStoreRepository = newPgJobStoreReposity();
        final ResourceBundle resourceBundle = pgJobStoreRepository.getResourceBundle(DEFAULT_JOB_ID);
        assertThat("ResourceBundle not null", resourceBundle, not(nullValue()));
        assertThat(String.format("ResourceBundle.flow: %s expected to match: %s", resourceBundle.getFlow(), flow), resourceBundle.getFlow(), is(flow));
        assertThat(String.format("ResourceBundle.sink: %s expected to match: %s", resourceBundle.getSink(), sink), resourceBundle.getSink(), is(sink));

        assertThat(String.format("ResourceBundle.supplementaryProcessData.format: %s expected to match: %s:",
                        resourceBundle.getSupplementaryProcessData().getFormat(),
                        jobEntity.getSpecification().getFormat()),
                resourceBundle.getSupplementaryProcessData().getFormat(), is(jobEntity.getSpecification().getFormat()));

        assertThat(String.format("ResourceBundle.supplementaryProcessData.submitter: %s expected to match: %s:",
                        resourceBundle.getSupplementaryProcessData().getSubmitter(),
                        jobEntity.getSpecification().getSubmitterId()),
                resourceBundle.getSupplementaryProcessData().getSubmitter(), is(jobEntity.getSpecification().getSubmitterId()));
    }
}