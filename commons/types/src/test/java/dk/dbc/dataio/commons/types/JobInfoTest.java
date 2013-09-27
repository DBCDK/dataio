package dk.dbc.dataio.commons.types;

import org.junit.Test;

import java.util.Date;

/**
 * JobInfo unit tests
 *
 * The test methods of this class uses the following naming convention:
 *
 *  unitOfWork_stateUnderTest_expectedBehavior
 */
public class JobInfoTest {
    private static final long JOB_ID = 42L;
    private static final JobSpecification JOB_SPECIFICATION = JobSpecificationTest.newJobSpecificationInstance();
    private static final Date JOB_CREATION_TIME = new Date();
    private static final JobState JOB_STATE = JobState.RUNNING;
    private static final String JOB_RESULT_DATA_FILE = "uri";

    @Test(expected = IllegalArgumentException.class)
    public void constructor_jobIdArgIsBelowThreshold_throws() {
        new JobInfo(JobInfo.JOB_ID_LOWER_THRESHOLD, JOB_SPECIFICATION, JOB_CREATION_TIME, JOB_STATE, JOB_RESULT_DATA_FILE);
    }

    @Test(expected = NullPointerException.class)
    public void constructor_jobSpecificationArgIsNull_throws() {
        new JobInfo(JOB_ID, null, JOB_CREATION_TIME, JOB_STATE, JOB_RESULT_DATA_FILE);
    }

    @Test(expected = NullPointerException.class)
    public void constructor_jobCreationTimeArgIsNull_throws() {
        new JobInfo(JOB_ID, JOB_SPECIFICATION, null, JOB_STATE, JOB_RESULT_DATA_FILE);
    }

    @Test(expected = NullPointerException.class)
    public void constructor_jobStateArgIsNull_throws() {
        new JobInfo(JOB_ID, JOB_SPECIFICATION, JOB_CREATION_TIME, null, JOB_RESULT_DATA_FILE);
    }

    public static JobInfo newJobInfoInstance() {
        return new JobInfo(JOB_ID, JOB_SPECIFICATION, JOB_CREATION_TIME, JOB_STATE, JOB_RESULT_DATA_FILE);
    }
}
