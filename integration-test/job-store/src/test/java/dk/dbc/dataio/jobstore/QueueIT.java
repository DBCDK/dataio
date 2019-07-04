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

package dk.dbc.dataio.jobstore;

import dk.dbc.dataio.commons.types.Chunk;
import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.types.FileStoreUrn;
import dk.dbc.dataio.commons.types.JobSpecification;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnectorException;
import dk.dbc.dataio.commons.utils.test.jms.MockedJmsTextMessage;
import dk.dbc.dataio.commons.utils.test.model.ChunkBuilder;
import dk.dbc.dataio.commons.utils.test.model.ChunkItemBuilder;
import dk.dbc.dataio.integrationtest.JmsQueueConnector;
import dk.dbc.dataio.jobstore.types.JobInfoSnapshot;
import dk.dbc.dataio.jobstore.types.State;
import dk.dbc.dataio.jobstore.types.criteria.JobListCriteria;
import dk.dbc.dataio.jobstore.types.criteria.ListFilter;
import dk.dbc.dataio.jsonb.JSONBException;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.rules.TestName;

import javax.jms.JMSContext;
import javax.jms.JMSException;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Ignore("See explanation in AbstractJobStoreTest class")
public class QueueIT extends AbstractJobStoreTest {
    @Rule
    public TemporaryFolder tmpFolder = new TemporaryFolder();

    @Rule
    public TestName test = new TestName();

    private static final long MAX_QUEUE_WAIT_IN_MS = 5000;

    private JMSContext jmsContext;

    @Before
    public void setupMocks() {
        jmsContext = mock(JMSContext.class);
        when(jmsContext.createTextMessage(any(String.class))).thenReturn(new MockedJmsTextMessage());
    }

    @Test
    public void addChunk_jobStateUpdatedAndWorkloadPublished()
            throws IOException, JobStoreServiceConnectorException, JSONBException, JMSException {
        final int expectedNumberOfRecords = 11;
        final String fileId = createLineFormatDataFile();
        final JobSpecification jobSpecification = new JobSpecification()
                    .withPackaging("lin")
                    .withFormat("basis")
                    .withCharset("latin1")
                    .withDestination(test.getMethodName())
                    .withSubmitterId(870970)
                    .withMailForNotificationAboutVerification("")
                .withMailForNotificationAboutProcessing("")
                .withResultmailInitials("")
                .withType(JobSpecification.Type.TEST)
                    .withDataFile(FileStoreUrn.create(fileId).toString());
        createFlowStoreEnvironmentMatchingJobSpecification(jobSpecification);

        JobInfoSnapshot jobInfoSnapshot = jobStoreServiceConnector.addJob(getJobInputStream(jobSpecification));

        // Swallow 1st Chunk message
        JmsQueueConnector.awaitQueueSize(JmsQueueConnector.PROCESSOR_QUEUE_NAME, 2, MAX_QUEUE_WAIT_IN_MS);
        JmsQueueConnector.emptyQueue(JmsQueueConnector.PROCESSOR_QUEUE_NAME);

        jobInfoSnapshot = getJob(jobInfoSnapshot.getJobId());
        assertThat("Partitioning phase complete", jobInfoSnapshot.getState().phaseIsDone(State.Phase.PARTITIONING), is(true));
        assertThat("Processing phase complete", jobInfoSnapshot.getState().phaseIsDone(State.Phase.PROCESSING), is(false));
        assertThat("Delivering phase complete", jobInfoSnapshot.getState().phaseIsDone(State.Phase.DELIVERING), is(false));

        final List<ChunkItem> chunkItems = Arrays.asList(
                new ChunkItemBuilder().setId(0).build(),
                new ChunkItemBuilder().setId(1).build(),
                new ChunkItemBuilder().setId(2).build(),
                new ChunkItemBuilder().setId(3).build(),
                new ChunkItemBuilder().setId(4).build(),
                new ChunkItemBuilder().setId(5).build(),
                new ChunkItemBuilder().setId(6).build(),
                new ChunkItemBuilder().setId(7).build(),
                new ChunkItemBuilder().setId(8).build(),
                new ChunkItemBuilder().setId(9).build());

        Chunk processedChunk = new ChunkBuilder(Chunk.Type.PROCESSED)
                .setJobId(jobInfoSnapshot.getJobId())
                .setChunkId(0L)
                .setItems(chunkItems)
                .build();
        jobStoreServiceConnector.addChunk(processedChunk, jobInfoSnapshot.getJobId(), 0);

        Chunk deliveredChunk = new ChunkBuilder(Chunk.Type.DELIVERED)
                .setJobId(jobInfoSnapshot.getJobId())
                .setChunkId(0L)
                .setItems(chunkItems)
                .build();
        jobStoreServiceConnector.addChunk(deliveredChunk, jobInfoSnapshot.getJobId(), 0);


        jobInfoSnapshot = getJob(jobInfoSnapshot.getJobId());
        assertThat("Processing phase complete", jobInfoSnapshot.getState().phaseIsDone(State.Phase.PROCESSING), is(false));
        assertThat("Processing phase succeeded", jobInfoSnapshot.getState().getPhase(State.Phase.PROCESSING).getSucceeded(), is(processedChunk.size()));
        assertThat("Delivering phase complete", jobInfoSnapshot.getState().phaseIsDone(State.Phase.DELIVERING), is(false));
        assertThat("Delivering phase succeeded", jobInfoSnapshot.getState().getPhase(State.Phase.DELIVERING).getSucceeded(), is(deliveredChunk.size()));

        processedChunk = new ChunkBuilder(Chunk.Type.PROCESSED)
                .setJobId(jobInfoSnapshot.getJobId())
                .setChunkId(1L)
                .build();
        jobStoreServiceConnector.addChunk(processedChunk, jobInfoSnapshot.getJobId(), 1);

        deliveredChunk = new ChunkBuilder(Chunk.Type.DELIVERED)
                .setJobId(jobInfoSnapshot.getJobId())
                .setChunkId(1L)
                .build();
        jobStoreServiceConnector.addChunk(deliveredChunk, jobInfoSnapshot.getJobId(), 1);



        jobInfoSnapshot = getJob(jobInfoSnapshot.getJobId());
        assertThat("Processing phase complete", jobInfoSnapshot.getState().phaseIsDone(State.Phase.PROCESSING), is(true));
        assertThat("Processing phase succeeded", jobInfoSnapshot.getState().getPhase(State.Phase.PROCESSING).getSucceeded(), is(expectedNumberOfRecords));
        assertThat("Delivering phase complete", jobInfoSnapshot.getState().phaseIsDone(State.Phase.DELIVERING), is(true));
        assertThat("Delivering phase succeeded", jobInfoSnapshot.getState().getPhase(State.Phase.DELIVERING).getSucceeded(), is(expectedNumberOfRecords));
    }

    private JobInfoSnapshot getJob(int jobId) throws JobStoreServiceConnectorException {
        final JobListCriteria criteria = new JobListCriteria()
                .where(new ListFilter<>(JobListCriteria.Field.JOB_ID, ListFilter.Op.EQUAL, jobId));
        return jobStoreServiceConnector.listJobs(criteria).get(0);
    }
}
