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

import dk.dbc.dataio.commons.types.FileStoreUrn;
import dk.dbc.dataio.commons.types.JobSpecification;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnectorException;
import dk.dbc.dataio.jobstore.types.JobInfoSnapshot;
import dk.dbc.dataio.jobstore.types.State;
import dk.dbc.dataio.jobstore.types.criteria.JobListCriteria;
import dk.dbc.dataio.jobstore.types.criteria.ListFilter;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.rules.TestName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

@Ignore("See explanation in AbstractJobStoreTest class")
public class AddJobIT extends AbstractJobStoreTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(AddJobIT.class);

    private static final long SLEEP_INTERVAL_IN_MS = 1000;
    private static final long MAX_WAIT_IN_MS = 10000;

    @Rule
    public TemporaryFolder tmpFolder = new TemporaryFolder();

    @Rule
    public TestName test = new TestName();

    /**
     * Given: an empty job-store <br/>
     * When: a job specification is posted, which references a line format data file
     * containing a number of records exceeding the capacity of a single chunk <br/>
     * Then: a new job is created without error <br/>
     * And: the proper number of chunks is created <br/>
     */
    @Test
    public void createJob_jobSpecificationReferencesLineFormatDataFile_newJobIsCreated()
            throws IOException, JobStoreServiceConnectorException {
        final int recordCount = 11;
        final String fileId = createLineFormatDataFile();
        final JobSpecification jobSpecification = new JobSpecification()
                .withPackaging("lin")
                .withFormat("katalog")
                .withCharset("latin1")
                .withDestination(test.getMethodName())
                .withSubmitterId(700000)
                .withMailForNotificationAboutVerification(JobSpecification.EMPTY_MAIL_FOR_NOTIFICATION_ABOUT_VERIFICATION)
                .withMailForNotificationAboutProcessing(JobSpecification.EMPTY_MAIL_FOR_NOTIFICATION_ABOUT_PROCESSING)
                .withResultmailInitials(JobSpecification.EMPTY_RESULT_MAIL_INITIALS)
                .withType(JobSpecification.Type.TEST)
                .withDataFile(FileStoreUrn.create(fileId).toString());
        createFlowStoreEnvironmentMatchingJobSpecification(jobSpecification);

        // When...
        final JobInfoSnapshot jobInfoSnapshot = jobStoreServiceConnector.addJob(getJobInputStream(jobSpecification));

        final JobInfoSnapshot jobInfoSnapshotAfterWait = this.waitForJobCompletion(jobInfoSnapshot.getJobId());

        // Then...
        final State jobState = jobInfoSnapshotAfterWait.getState();
        assertThat("Partitioning phase complete", jobState.phaseIsDone(State.Phase.PARTITIONING), is(true));
        assertThat("Partitioning phase failures", jobState.getPhase(State.Phase.PARTITIONING).getFailed(), is(0));

        // And...
        assertThat("Number of items", jobInfoSnapshotAfterWait.getNumberOfItems(), is(recordCount));
        assertThat("Number of chunks", jobInfoSnapshotAfterWait.getNumberOfChunks(), is(2));
    }

    private JobInfoSnapshot waitForJobCompletion(long jobId) throws JobStoreServiceConnectorException {
        final JobListCriteria criteria = new JobListCriteria().where(new ListFilter<>(JobListCriteria.Field.JOB_ID, ListFilter.Op.EQUAL, jobId));
        JobInfoSnapshot jobInfoSnapshot = null;
        // Wait for Job-completion
        long remainingWaitInMs = MAX_WAIT_IN_MS;

        LOGGER.info("AddJobIT.createJob_jobSpecificationReferencesHarvesterMarcxchangeDataFile_newJobIsCreated - waiting in MAX milliseconds: " + MAX_WAIT_IN_MS);
        LOGGER.info("AddJobIT.createJob_jobSpecificationReferencesHarvesterMarcxchangeDataFile_newJobIsCreated - sleeping in milliseconds: " + SLEEP_INTERVAL_IN_MS);


        while ( remainingWaitInMs > 0 ) {
            LOGGER.info("AddJobIT.createJob_jobSpecificationReferencesHarvesterMarcxchangeDataFile_newJobIsCreated - remaining wait in milliseconds: " + remainingWaitInMs);

            jobInfoSnapshot = jobStoreServiceConnector.listJobs(criteria).get(0);
            if (phasePartitioningDoneSuccessfully(jobInfoSnapshot)) {
                break;
            } else {
                try {
                    Thread.sleep(SLEEP_INTERVAL_IN_MS);
                    remainingWaitInMs -= SLEEP_INTERVAL_IN_MS;
                } catch (InterruptedException ex) {
                    break;
                }
            }
        }
        if (!phasePartitioningDoneSuccessfully(jobInfoSnapshot)) {
            throw new IllegalStateException(String.format("Job %d did not complete successfully in time",
                    jobInfoSnapshot.getJobId()));
        }

        return jobInfoSnapshot;
    }

    private boolean phasePartitioningDoneSuccessfully(JobInfoSnapshot jobInfoSnapshot) {
        final State state = jobInfoSnapshot.getState();
        return state.phaseIsDone(State.Phase.PARTITIONING);
    }
}