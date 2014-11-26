package dk.dbc.dataio.jobstore.types;

import dk.dbc.dataio.commons.types.JobSpecification;
import dk.dbc.dataio.commons.utils.test.model.JobSpecificationBuilder;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;

public class JobInputStreamTest {

    private static final long PART_NUMBER = 12345678;

    @Test(expected = NullPointerException.class)
    public void constructor_jobSpecificationArgIsNull_throws() {
        new JobInputStream(null, false, PART_NUMBER);
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructor_jobSpecificationContainsEmptyField_throws() {
        JobSpecification jobSpecification = new JobSpecificationBuilder().setFormat("").build();
        new JobInputStream(jobSpecification, false, PART_NUMBER);
    }

    @Test
    public void setJobSpecification_inputIsValid_jobSpecificationCreated() {
        final String FORMAT = "thisIsATestFormat";
        JobSpecification jobSpecification = new JobSpecificationBuilder()
                .setFormat(FORMAT).build();
        JobInputStream jobInputStream = new JobInputStream(jobSpecification, false, PART_NUMBER);

        assertThat(jobInputStream.getPartNumber(), not(nullValue()));
        assertThat(jobInputStream.getPartNumber(), is(PART_NUMBER));
        assertThat(jobInputStream.getIsEndOfJob(), is(false));
        assertThat(jobInputStream.getJobSpecification(), not(nullValue()));
        assertThat(jobInputStream.getJobSpecification().getFormat(), is(FORMAT));
    }

}
