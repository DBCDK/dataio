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

package dk.dbc.dataio.jobstore.service.util;

import dk.dbc.dataio.commons.types.JobSpecification;
import dk.dbc.dataio.jobstore.service.entity.JobEntity;
import dk.dbc.dataio.jobstore.test.types.WorkflowNoteBuilder;
import dk.dbc.dataio.jobstore.types.JobInfoSnapshot;
import dk.dbc.dataio.jobstore.types.State;
import org.junit.Test;

import java.sql.Timestamp;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;

public class JobInfoSnapshotConverterTest {

    @Test
    public void toJobInfoSnapShot_jobEntityInput_jobInfoSnapshotReturned() {
        JobEntity jobEntity = getJobEntity();

        // Convert to JobInfoSnapshot to view information regarding the job from one exact moment in time (now)
        JobInfoSnapshot jobInfoSnapshot = JobInfoSnapshotConverter.toJobInfoSnapshot(jobEntity);

        assertThat(jobInfoSnapshot, not(nullValue()));
        assertJobInfoSnapshotEquals(jobInfoSnapshot, jobEntity);
    }

    /*
     * Private methods
     */

    private JobEntity getJobEntity() {
        JobEntity jobEntity = new JobEntity();
        jobEntity.setState(new State());
        jobEntity.setEoj(false);
        jobEntity.setPartNumber(1234);
        jobEntity.setSpecification(new JobSpecification());
        jobEntity.setNumberOfChunks(10);
        jobEntity.setNumberOfItems(5);
        jobEntity.setTimeOfCompletion(new Timestamp(System.currentTimeMillis()));
        jobEntity.setWorkflowNote(new WorkflowNoteBuilder().build());
        return jobEntity;
    }

    private void assertJobInfoSnapshotEquals(JobInfoSnapshot jobInfoSnapshot, JobEntity jobEntity) {
        assertThat(jobInfoSnapshot.getState(), is(jobEntity.getState()));
        assertThat(jobInfoSnapshot.isEoj(), is (jobEntity.isEoj()));
        assertThat(jobInfoSnapshot.getPartNumber(), is(jobEntity.getPartNumber()));
        assertThat(jobInfoSnapshot.getSpecification(), is(jobEntity.getSpecification()));
        assertThat(jobInfoSnapshot.getNumberOfChunks(), is(jobEntity.getNumberOfChunks()));
        assertThat(jobInfoSnapshot.getNumberOfItems(), is(jobEntity.getNumberOfItems()));
        assertThat(jobInfoSnapshot.getTimeOfCompletion().getTime(), is(jobEntity.getTimeOfCompletion().getTime()));
        assertThat(jobInfoSnapshot.getWorkflowNote(), is(jobEntity.getWorkflowNote()));
    }

}
