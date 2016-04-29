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
import dk.dbc.dataio.commons.types.Sink;
import dk.dbc.dataio.commons.utils.test.model.JobSpecificationBuilder;
import dk.dbc.dataio.commons.utils.test.model.SinkBuilder;
import dk.dbc.dataio.filestore.service.connector.FileStoreServiceConnectorException;
import dk.dbc.dataio.jobstore.service.entity.JobEntity;
import dk.dbc.dataio.jobstore.service.entity.JobQueueEntity;
import dk.dbc.dataio.jobstore.service.partitioner.DataPartitioner;
import dk.dbc.dataio.jobstore.types.JobInfoSnapshot;
import dk.dbc.dataio.jobstore.types.JobInputStream;
import dk.dbc.dataio.jobstore.types.JobStoreException;
import org.junit.Test;
import types.TestableJobEntityBuilder;

import javax.persistence.LockModeType;
import javax.persistence.Query;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class PgJobStore_AddAndScheduleJobTest extends PgJobStoreBaseTest {

    @Test
    public void addAndScheduleJob_nullArgument_throws() throws Exception {
        final PgJobStore pgJobStore = newPgJobStore();
        try {
            pgJobStore.addAndScheduleJob(null);
            fail("No NullPointerException Thrown");
        } catch(NullPointerException e) {}
    }

    @Test
    public void addAndScheduleJob_jobAdded_returnsJobInfoSnapshot() throws Exception {
        final PgJobStore pgJobStore = newPgJobStore(newPgJobStoreReposity());
        final JobSpecification jobSpecification = new JobSpecificationBuilder()
                .setDataFile(FILE_STORE_URN.toString())
                .setAncestry(new JobSpecificationBuilder.AncestryBuilder().build())
                .build();

        final JobEntity jobEntity = getJobEntity(0);
        final Sink sink = new SinkBuilder().build();
        when(jobEntity.getCachedSink().getSink()).thenReturn(sink);

        final JobInputStream jobInputStream = getJobInputStream(jobSpecification.getDataFile());
        final String xml = getXml();
        final ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));

        setupSuccessfulMockedReturnsFromFlowStore(jobInputStream.getJobSpecification());

        when(entityManager.find(eq(JobEntity.class), anyInt(), eq(LockModeType.PESSIMISTIC_WRITE))).thenReturn(new TestableJobEntityBuilder().setJobSpecification(jobInputStream.getJobSpecification()).build());
        when(mockedFileStoreServiceConnector.getFile(anyString())).thenReturn(byteArrayInputStream);
        when(mockedFileStoreServiceConnector.getByteSize(anyString())).thenReturn((long) xml.getBytes().length);

        Query mockedNamedQueryFindJobsBySink = mock(Query.class);
        when(entityManager.createNamedQuery(JobQueueEntity.NQ_FIND_NUMBER_OF_JOBS_BY_SINK)).thenReturn(mockedNamedQueryFindJobsBySink);
        when(mockedNamedQueryFindJobsBySink.setParameter(eq(JobQueueEntity.FIELD_SINK_ID), anyLong())).thenReturn(mockedNamedQueryFindJobsBySink);
        when(mockedNamedQueryFindJobsBySink.getSingleResult()).thenReturn(0);

        try {
            final JobInfoSnapshot jobInfoSnapshotReturned = pgJobStore.addAndScheduleJob(jobInputStream);
            // Verify that the method getByteSize (used in compareByteSize) was invoked.
            verify(mockedFileStoreServiceConnector).getByteSize(anyString());
            assertThat(jobInfoSnapshotReturned, is(notNullValue()));
        } catch(JobStoreException e) {
            fail("Exception thrown by addAndScheduleJob()");
        }
    }

    @Test
    public void compareByteSize_byteSizesIdentical() throws IOException, FileStoreServiceConnectorException, JobStoreException {
        final PgJobStore pgJobStore = newPgJobStore();
        final DataPartitioner mockedDataPartitioner = mock(DataPartitioner.class);
        final String xml = getXml();

        when(mockedFileStoreServiceConnector.getByteSize(anyString())).thenReturn((long) xml.getBytes().length);
        when(mockedDataPartitioner.getBytesRead()).thenReturn((long) xml.getBytes().length);

        final String fileId = "42";
        pgJobStore.compareByteSize(fileId, mockedDataPartitioner);
    }

    @Test
    public void addJob_addJobParamArgIsNull_throws() throws JobStoreException {
        final PgJobStore pgJobStore = newPgJobStore();
        try {
            pgJobStore.addJob(null);
            fail("No exception thrown");
        } catch (NullPointerException e) {}
    }
}