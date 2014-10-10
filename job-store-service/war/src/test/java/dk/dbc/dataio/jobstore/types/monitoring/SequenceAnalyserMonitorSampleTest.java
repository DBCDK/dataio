package dk.dbc.dataio.jobstore.types.monitoring;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

public class SequenceAnalyserMonitorSampleTest {
    @Test
    public void constructor_returnsNewInstance() {
        final long queued = 42;
        final long headOfQueueMonitoringStartTime = 424242;
        final SequenceAnalyserMonitorSample sequenceAnalyserMonitorSample =
                new SequenceAnalyserMonitorSample(queued, headOfQueueMonitoringStartTime);
        assertThat(sequenceAnalyserMonitorSample, is(notNullValue()));
        assertThat(sequenceAnalyserMonitorSample.getQueued(), is(queued));
        assertThat(sequenceAnalyserMonitorSample.getHeadOfQueueMonitoringStartTime(), is(headOfQueueMonitoringStartTime));
        assertThat(sequenceAnalyserMonitorSample.getHeadOfQueueWaitTimeInMs(), is(not(0L)));
    }

    @Test
    public void getHeadOfQueueWaitTimeInMs_queuedIsZero_returnsZero() {
        final long queued = 0;
        final long headOfQueueMonitoringStartTime = 424242;
        final SequenceAnalyserMonitorSample sequenceAnalyserMonitorSample =
                new SequenceAnalyserMonitorSample(queued, headOfQueueMonitoringStartTime);
        assertThat(sequenceAnalyserMonitorSample.getHeadOfQueueWaitTimeInMs(), is(0L));
    }
}