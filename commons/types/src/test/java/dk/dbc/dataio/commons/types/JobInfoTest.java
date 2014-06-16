package dk.dbc.dataio.commons.types;

import org.junit.Test;

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
    private static final long JOB_CREATION_TIME = System.currentTimeMillis();

    @Test(expected = IllegalArgumentException.class)
    public void constructor_jobIdArgIsLessThanLowerBound_throws() {
        new JobInfo(Constants.JOB_ID_LOWER_BOUND - 1, JOB_SPECIFICATION, JOB_CREATION_TIME);
    }

    @Test(expected = NullPointerException.class)
    public void constructor_jobSpecificationArgIsNull_throws() {
        new JobInfo(JOB_ID, null, JOB_CREATION_TIME);
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructor_jobCreationTimeArgIsNegative_throws() {
        new JobInfo(JOB_ID, JOB_SPECIFICATION, -1L);
    }

    @Test
    public void constructor_allArgsAreValid_returnsNewInstance() {
        final JobInfo instance =  new JobInfo(JOB_ID, JOB_SPECIFICATION, JOB_CREATION_TIME);
        assertThat(instance, is(notNullValue()));
    }

    @Test(expected = NullPointerException.class)
    public void setter_setInvalidJobErrorCode_throws() {
        newJobInfoInstance().setJobErrorCode(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void setter_setInvalidJobRecordCount_throws() {
        newJobInfoInstance().setJobRecordCount(-1L);
    }

    @Test
    public void setter_setValidJobErrorCode_changesValue() {
        JobInfo info = newJobInfoInstance();
        info.setJobErrorCode(JobErrorCode.DATA_FILE_INVALID);
        assertThat(info.getJobErrorCode(), is(JobErrorCode.DATA_FILE_INVALID));
    }

    @Test
    public void setter_setValidJobRecordCount_changesValue() {
        JobInfo info = newJobInfoInstance();
        info.setJobRecordCount(23L);
        assertThat(info.getJobRecordCount(), is(23L));
    }

    public static JobInfo newJobInfoInstance() {
        return new JobInfo(JOB_ID, JOB_SPECIFICATION, JOB_CREATION_TIME);
    }
}
