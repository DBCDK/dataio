package dk.dbc.dataio.jobstore.types.monitoring;

import java.beans.ConstructorProperties;
import java.util.Date;

/**
 * Sequence analyser JMX monitoring sample
 */
public class SequenceAnalyserMonitorSample {
    final long queued;
    final long headOfQueueMonitoringStartTime;

    @ConstructorProperties({"queued", "headOfQueueMonitoringStartTime"})
    public SequenceAnalyserMonitorSample(long queued, long headOfQueueMonitoringStartTime) {
        this.queued = queued;
        this.headOfQueueMonitoringStartTime = headOfQueueMonitoringStartTime;
    }

    public long getQueued() {
        return queued;
    }

    public long getHeadOfQueueMonitoringStartTime() {
        return headOfQueueMonitoringStartTime;
    }

    public long getHeadOfQueueWaitTimeInMs() {
        return new Date().getTime() - headOfQueueMonitoringStartTime;
    }
}
