package dk.dbc.dataio.registry;

import org.eclipse.microprofile.metrics.Counter;
import org.eclipse.microprofile.metrics.Gauge;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.SimpleTimer;
import org.eclipse.microprofile.metrics.Tag;

import java.util.function.Supplier;

public interface JMXMetricMixin {
    default  <T extends Number> Gauge<T> gauge(Supplier<T> supplier, Tag... tags) {
        return JMXMetricRegistry.create().gauge(name(), supplier, tags);
    }

    default SimpleTimer simpleTimer(MetricRegistry metricRegistry, Tag... tags) {
        return JMXMetricRegistry.create().simpleTimer(name(), tags);
    }

    default Counter counter(MetricRegistry metricRegistry, Tag... tags) {
        return JMXMetricRegistry.create().counter(name(), tags);
    }

    default String getName() {
        return name();
    }

    String name();
}
