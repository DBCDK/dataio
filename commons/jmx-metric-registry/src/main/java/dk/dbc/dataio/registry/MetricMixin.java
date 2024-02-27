package dk.dbc.dataio.registry;

import dk.dbc.dataio.registry.metrics.CounterMetric;
import dk.dbc.dataio.registry.metrics.GaugeMetric;
import dk.dbc.dataio.registry.metrics.TimerMetric;
import org.eclipse.microprofile.metrics.Counter;
import org.eclipse.microprofile.metrics.Gauge;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.Tag;
import org.eclipse.microprofile.metrics.Timer;

import java.util.function.Supplier;

public interface MetricMixin {
    default  <T extends Number> Gauge<T> gauge(MetricRegistry metricRegistry, Supplier<T> supplier, Tag... tags) {
        if(metricRegistry == null) return new GaugeMetric<>(supplier);
        return metricRegistry.gauge(name(), supplier, tags);
    }

    default Timer timer(MetricRegistry metricRegistry, Tag... tags) {
        if(metricRegistry == null) return new TimerMetric();
        return metricRegistry.timer(name(), tags);
    }

    default Counter counter(MetricRegistry metricRegistry, Tag... tags) {
        if(metricRegistry == null) return new CounterMetric();
        return metricRegistry.counter(name(), tags);
    }

    default String getName() {
        return name();
    }

    String name();
}
