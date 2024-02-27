package dk.dbc.dataio.registry.metrics;

import java.time.Duration;

public interface TimerMetricMBean {
    Duration getElapsedTime();

    long getCount();

    Duration getMaxTimeDuration();

    Duration getMinTimeDuration();
}
