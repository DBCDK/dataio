package dk.dbc.dataio.registry.metrics;

import dk.dbc.dataio.registry.Resettable;
import org.eclipse.microprofile.metrics.Counter;

import java.util.concurrent.atomic.AtomicLong;

public class CounterMetric implements Counter, CounterMetricMBean, Resettable {
    private final AtomicLong value = new AtomicLong(0);

    @Override
    public void inc() {
        value.incrementAndGet();
    }

    @Override
    public void inc(long l) {
        value.addAndGet(l);
    }

    @Override
    public long getCount() {
        return value.get();
    }

    @Override
    public void reset() {
        value.set(0);
    }
}
