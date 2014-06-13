package dk.dbc.dataio.commons.types;

import org.junit.Ignore;
import org.junit.Test;

import java.util.Map;

import static junit.framework.TestCase.assertTrue;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * JobState unit tests
 *
 * The test methods of this class uses the following naming convention:
 *$
 *  unitOfWork_stateUnderTest_expectedBehavior
 */
@Ignore
public class JobStateTest {

    @Test
    public void constructor_valid_assertValid() {
        JobState jobState = new JobState();
        Map<JobState.OperationalState, JobState.LifeCycleState> states = jobState.getStates();
        for (JobState.LifeCycleState stateEnum: states.values()) {
            assertTrue(stateEnum.equals(JobState.LifeCycleState.PENDING));
        }
    }

    @Test(expected = NullPointerException.class)
    public void setLifeCycleStateFor_invalidOperationalState_throws() {
        JobState jobState = new JobState();
        jobState.setLifeCycleStateFor(null, JobState.LifeCycleState.ACTIVE);
    }

    @Test(expected = NullPointerException.class)
    public void setLifeCycleStateFor_invalidLifeCycleState_throws() {
        JobState jobState = new JobState();
        jobState.setLifeCycleStateFor(JobState.OperationalState.CHUNKIFYING, null);
    }

    @Test(expected = IllegalStateException.class)
    public void setLifeCycleStateFor_invalidLifeCycleStateRegression_throws() {
        JobState jobState = new JobState();
        jobState.setLifeCycleStateFor(JobState.OperationalState.CHUNKIFYING, JobState.LifeCycleState.ACTIVE);
        jobState.setLifeCycleStateFor(JobState.OperationalState.CHUNKIFYING, JobState.LifeCycleState.PENDING);
    }

    @Test
    public void setLifeCycleStateFor_validInput_validResult() {
        JobState jobState = new JobState();
        jobState.setLifeCycleStateFor(JobState.OperationalState.CHUNKIFYING, JobState.LifeCycleState.ACTIVE);
        assertThat(jobState.getLifeCycleStateFor(JobState.OperationalState.CHUNKIFYING), is(JobState.LifeCycleState.ACTIVE));
    }

    @Test(expected = NullPointerException.class)
    public void getLifeCycleStateFor_InvalidInput_throws() {
        JobState jobState = new JobState();
        jobState.setLifeCycleStateFor(JobState.OperationalState.CHUNKIFYING, JobState.LifeCycleState.ACTIVE);
        assertThat(jobState.getLifeCycleStateFor(null), is(JobState.LifeCycleState.ACTIVE));
    }

    @Test
    public void getStates_validInput_validResult() {
        JobState jobState = new JobState();
        jobState.setLifeCycleStateFor(JobState.OperationalState.CHUNKIFYING, JobState.LifeCycleState.ACTIVE);
        jobState.setLifeCycleStateFor(JobState.OperationalState.DELIVERING, JobState.LifeCycleState.PENDING);
        jobState.setLifeCycleStateFor(JobState.OperationalState.PROCESSING, JobState.LifeCycleState.DONE);
        Map<JobState.OperationalState, JobState.LifeCycleState> result = jobState.getStates();
        assertThat(result.get(JobState.OperationalState.CHUNKIFYING), is(JobState.LifeCycleState.ACTIVE));
        assertThat(result.get(JobState.OperationalState.DELIVERING), is(JobState.LifeCycleState.PENDING));
        assertThat(result.get(JobState.OperationalState.PROCESSING), is(JobState.LifeCycleState.DONE));
    }

//    @Test
//    public void isAllDone_allIsDone_verifyAllIsDone() {
//        JobState jobState = new JobState();
//        jobState.setLifeCycleStateFor(JobState.OperationalState.CHUNKIFYING, JobState.LifeCycleState.DONE);
//        jobState.setLifeCycleStateFor(JobState.OperationalState.DELIVERING, JobState.LifeCycleState.DONE);
//        jobState.setLifeCycleStateFor(JobState.OperationalState.PROCESSING, JobState.LifeCycleState.DONE);
//        assertTrue(jobState.isAllDone());
//    }
//
//    @Test
//    public void isAllDone_allIsNotDone_verifyAllIsNotDone() {
//        JobState jobState = new JobState();
//        jobState.setLifeCycleStateFor(JobState.OperationalState.CHUNKIFYING, JobState.LifeCycleState.DONE);
//        jobState.setLifeCycleStateFor(JobState.OperationalState.DELIVERING, JobState.LifeCycleState.PENDING);
//        jobState.setLifeCycleStateFor(JobState.OperationalState.PROCESSING, JobState.LifeCycleState.DONE);
//        assertFalse(jobState.isAllDone());
//    }

}
