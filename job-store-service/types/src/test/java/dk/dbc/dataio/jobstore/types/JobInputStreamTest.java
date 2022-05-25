package dk.dbc.dataio.jobstore.types;

import dk.dbc.dataio.commons.types.JobSpecification;
import org.junit.Test;

import static dk.dbc.commons.testutil.Assert.assertThat;
import static dk.dbc.commons.testutil.Assert.isThrowing;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class JobInputStreamTest {

    private static final int PART_NUMBER = 12345678;
    private final JobSpecification jobSpecification = new JobSpecification();

    @Test
    public void constructor_jobSpecificationArgIsNull_throws() {
        assertThat(() -> new JobInputStream(null, false, PART_NUMBER), isThrowing(NullPointerException.class));
    }

    @Test
    public void constructor_partNumberLessThanZero_throws() {
        assertThat(() -> new JobInputStream(jobSpecification, false, -1), isThrowing(IllegalArgumentException.class));
    }

    @Test
    public void constructor_allArgsAreValid_returnsJobInputStream() {
        JobInputStream jobInputStream = new JobInputStream(jobSpecification, true, PART_NUMBER);

        assertThat(jobInputStream.getPartNumber(), is(PART_NUMBER));
        assertThat(jobInputStream.getIsEndOfJob(), is(true));
        assertThat(jobInputStream.getJobSpecification(), is(jobSpecification));
    }

    @Test
    public void constructorWithOneArg_argIsValid_returnsJobInputStream() {
        JobInputStream jobInputStream = new JobInputStream(jobSpecification);

        assertThat(jobInputStream.getPartNumber(), is(0));      // default value
        assertThat(jobInputStream.getIsEndOfJob(), is(false));  // default value
        assertThat(jobInputStream.getJobSpecification(), is(jobSpecification));
    }
 }
