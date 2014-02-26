package dk.dbc.dataio.commons.types;

import org.junit.Test;

import java.util.Date;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

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
    private static final JobErrorCode JOB_ERROR_CODE = JobErrorCode.NO_ERROR;
    private static final long JOB_RECORD_COUNT = 0;

    @Test(expected = IllegalArgumentException.class)
    public void constructor_jobIdArgIsBelowThreshold_throws() {
        new JobInfo(Constants.JOB_ID_LOWER_BOUND, JOB_SPECIFICATION, JOB_CREATION_TIME, JOB_ERROR_CODE, JOB_RECORD_COUNT);
    }

    @Test(expected = NullPointerException.class)
    public void constructor_jobSpecificationArgIsNull_throws() {
        new JobInfo(JOB_ID, null, JOB_CREATION_TIME, JOB_ERROR_CODE, JOB_RECORD_COUNT);
    }

    @Test(expected = NullPointerException.class)
    public void constructor_jobCreationTimeArgIsNull_throws() {
        new JobInfo(JOB_ID, JOB_SPECIFICATION, null, JOB_ERROR_CODE, JOB_RECORD_COUNT);
    }

    @Test(expected = NullPointerException.class)
    public void constructor_jobErrorCodeArgIsNull_throws() {
        new JobInfo(JOB_ID, JOB_SPECIFICATION, JOB_CREATION_TIME, null, JOB_RECORD_COUNT);
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructor_jobRecordCountArgIsBelowThreshold_throws() {
        new JobInfo(JOB_ID, JOB_SPECIFICATION, JOB_CREATION_TIME, JOB_ERROR_CODE, -1);
    }

    @Test
    public void constructor_allArgsAreValid_returnsNewInstance() {
        final JobInfo instance =  new JobInfo(JOB_ID, JOB_SPECIFICATION, JOB_CREATION_TIME, JOB_ERROR_CODE, JOB_RECORD_COUNT);
        assertThat(instance, is(notNullValue()));
    }

    public static JobInfo newJobInfoInstance() {
        return new JobInfo(JOB_ID, JOB_SPECIFICATION, JOB_CREATION_TIME, JOB_ERROR_CODE, JOB_RECORD_COUNT);
    }
}
