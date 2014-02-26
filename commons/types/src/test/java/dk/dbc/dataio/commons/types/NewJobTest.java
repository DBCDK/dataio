package dk.dbc.dataio.commons.types;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

public class NewJobTest {
    private static final long JOB_ID = 1L;
    private static final long CHUNK_COUNT = 42L;
    private static final Sink SINK = SinkTest.newSinkInstance();

    @Test(expected = IllegalArgumentException.class)
    public void constructor_jobIdArgIsLessThanLowerBound_throws() {
        new NewJob(Constants.JOB_ID_LOWER_BOUND - 1, CHUNK_COUNT, SINK);
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructor_chunkCountArgIsLessThanLowerBound_throws() {
        new NewJob(JOB_ID, Constants.CHUNK_COUNT_LOWER_BOUND - 1, SINK);
    }

    @Test(expected = NullPointerException.class)
    public void constructor_sinkArgIsNull_throws() {
        new NewJob(JOB_ID, CHUNK_COUNT, null);
    }

    @Test
    public void constructor_allArgsAreValid_returnsNewInstance() {
        final NewJob instance = new NewJob(JOB_ID, CHUNK_COUNT, SINK);
        assertThat(instance, is(notNullValue()));
    }

    public static NewJob newNewJobInstance() {
        return new NewJob(JOB_ID, CHUNK_COUNT, SINK);
    }
}
