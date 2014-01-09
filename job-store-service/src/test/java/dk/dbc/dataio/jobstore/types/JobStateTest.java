package dk.dbc.dataio.jobstore.types;

import dk.dbc.dataio.commons.utils.json.JsonException;
import dk.dbc.dataio.commons.utils.json.JsonUtil;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class JobStateTest {
    @Test
    public void constructor_newInstance_allStateEntriesInitializedToPending() {
        final JobState jobState = new JobState();
        for (JobState.OperationalState operationalState : JobState.OperationalState.values()) {
            assertThat(jobState.getLifeCycleStateFor(operationalState), is(JobState.LifeCycleState.PENDING));
        }
    }

    @Test(expected = NullPointerException.class)
    public void setLifeCycleStateFor_operationalStateArgIsNull_throws() {
        final JobState jobState = new JobState();
        jobState.setLifeCycleStateFor(null, JobState.LifeCycleState.ACTIVE);
    }

    @Test(expected = NullPointerException.class)
    public void setLifeCycleStateFor_lifeCycleStateArgIsNull_throws() {
        final JobState jobState = new JobState();
        jobState.setLifeCycleStateFor(JobState.OperationalState.PROCESSING, null);
    }

    @Test(expected = IllegalStateException.class)
    public void setLifeCycleStateFor_lifeCycleStateRegression_throws() {
        final JobState jobState = new JobState();
        jobState.setLifeCycleStateFor(JobState.OperationalState.PROCESSING, JobState.LifeCycleState.DONE);
        jobState.setLifeCycleStateFor(JobState.OperationalState.PROCESSING, JobState.LifeCycleState.DONE);
        jobState.setLifeCycleStateFor(JobState.OperationalState.PROCESSING, JobState.LifeCycleState.PENDING);
    }

    @Test
    public void test() throws JsonException {
        final JobState jobState = new JobState();
        System.out.println(JsonUtil.toJson(jobState));
    }

    @Test(expected = NullPointerException.class)
    public void getLifeCycleStateFor_operationalStateArgIsNull_throws() {
        final JobState jobState = new JobState();
        jobState.getLifeCycleStateFor(null);
    }
}
