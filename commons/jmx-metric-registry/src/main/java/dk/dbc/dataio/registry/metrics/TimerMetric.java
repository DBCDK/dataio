package dk.dbc.dataio.registry.metrics;

import dk.dbc.dataio.registry.Resettable;
import org.eclipse.microprofile.metrics.Counting;
import org.eclipse.microprofile.metrics.Snapshot;
import org.eclipse.microprofile.metrics.Timer;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.Callable;

public class TimerMetric implements Counting, Timer, TimerMetricMBean, Resettable {
    private Duration duration = Duration.ZERO;
    private Duration min = null;
    private Duration max = Duration.ZERO;
    private long count = 0;

    @Override
    public void update(Duration duration) {
        updateInternal(duration);
    }

    @Override
    public <T> T time(Callable<T> callable) throws Exception {
        Instant start = Instant.now();
        try {
            return callable.call();
        } finally {
            update(start);
        }
    }

    @Override
    public void time(Runnable runnable) {
        Instant start = Instant.now();
        try {
            runnable.run();
        } finally {
            update(start);
        }
    }

    private synchronized void update(Instant start) {
        Duration elapsed = Duration.between(start, Instant.now());
        updateInternal(elapsed);
    }

    private synchronized void updateInternal(Duration elapsed) {
        count++;
        duration = duration.plus(elapsed);
        if (elapsed.compareTo(duration) > 0) max = elapsed;
        if (min == null || elapsed.compareTo(duration) < 0) min = elapsed;
    }

    @Override
    public Context time() {
        return null;
    }

    @Override
    public Duration getElapsedTime() {
        return duration;
    }

    @Override
    public long getCount() {
        return count;
    }

    @Override
    public Snapshot getSnapshot() {
        return null;
    }

    @Override
    public Duration getMaxTimeDuration() {
        return max;
    }

    @Override
    public Duration getMinTimeDuration() {
        return min;
    }

    @Override
    public synchronized void reset() {
        duration = Duration.ZERO;
        min = null;
        max = Duration.ZERO;
        count = 0;
    }
}
