package dk.dbc.dataio.jobstore.service.util;

import dk.dbc.dataio.commons.utils.test.model.JobSpecificationBuilder;
import dk.dbc.dataio.jobstore.service.entity.JobEntity;
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

        // It is not possible to compare the date and the timestamp directly since Date
        // does not operate with factional seconds - Hence the full comparison below.
        assertThat(jobInfoSnapshot.getTimeOfCompletion().getTime(),
                is(jobEntity.getTimeOfCompletion().getTime() + jobEntity.getTimeOfCompletion().getNanos() / 1000000));
    }

    /*
     * Private methods
     */

    private JobEntity getJobEntity() {
        JobEntity jobEntity = new JobEntity();
        jobEntity.setState(new State());
        jobEntity.setEoj(false);
        jobEntity.setPartNumber(1234);
        jobEntity.setFlowName("flowName");
        jobEntity.setSinkName("sinkName");
        jobEntity.setSpecification(new JobSpecificationBuilder().build());
        jobEntity.setNumberOfChunks(10);
        jobEntity.setNumberOfItems(5);
        jobEntity.setTimeOfCompletion(new Timestamp(System.currentTimeMillis()));
        return jobEntity;
    }

    private void assertJobInfoSnapshotEquals(JobInfoSnapshot jobInfoSnapshot, JobEntity jobEntity) {
        assertThat(jobInfoSnapshot.getState(), is(jobEntity.getState()));
        assertThat(jobInfoSnapshot.isEoj(), is (jobEntity.isEoj()));
        assertThat(jobInfoSnapshot.getPartNumber(), is(jobEntity.getPartNumber()));
        assertThat(jobInfoSnapshot.getFlowName(), is(jobEntity.getFlowName()));
        assertThat(jobInfoSnapshot.getSinkName(), is(jobEntity.getSinkName()));
        assertThat(jobInfoSnapshot.getSpecification(), is(jobEntity.getSpecification()));
        assertThat(jobInfoSnapshot.getNumberOfChunks(), is(jobEntity.getNumberOfChunks()));
        assertThat(jobInfoSnapshot.getNumberOfItems(), is(jobEntity.getNumberOfItems()));

        // It is not possible to compare the date and the timestamp directly since Date
        // does not operate with factional seconds - Hence the full comparison below.
        assertThat(jobInfoSnapshot.getTimeOfCompletion().getTime(),
                is(jobEntity.getTimeOfCompletion().getTime() + jobEntity.getTimeOfCompletion().getNanos() / 1000000));
    }

}
