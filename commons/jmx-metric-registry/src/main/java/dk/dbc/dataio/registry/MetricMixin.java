package dk.dbc.dataio.registry;

import dk.dbc.dataio.registry.metrics.CounterMetric;
import dk.dbc.dataio.registry.metrics.GaugeMetric;
import dk.dbc.dataio.registry.metrics.SimpleTimerMetric;
import org.eclipse.microprofile.metrics.Counter;
import org.eclipse.microprofile.metrics.Gauge;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.SimpleTimer;
import org.eclipse.microprofile.metrics.Tag;

import java.util.function.Supplier;

public interface MetricMixin {
    default  <T extends Number> Gauge<T> gauge(MetricRegistry metricRegistry, Supplier<T> supplier, Tag... tags) {
        if(metricRegistry == null) return new GaugeMetric<>(supplier);
        return metricRegistry.gauge(name(), supplier, tags);
    }

    default SimpleTimer simpleTimer(MetricRegistry metricRegistry, Tag... tags) {
        if(metricRegistry == null) return new SimpleTimerMetric();
        return metricRegistry.simpleTimer(name(), tags);
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
