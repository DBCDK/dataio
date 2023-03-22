package dk.dbc.dataio.registry;

import org.eclipse.microprofile.metrics.Counter;
import org.eclipse.microprofile.metrics.Gauge;
import org.eclipse.microprofile.metrics.SimpleTimer;
import org.eclipse.microprofile.metrics.Tag;

import java.util.function.Supplier;

public interface PrometheusMetricMixin {
    default  <T extends Number> Gauge<T> gauge(Supplier<T> supplier, Tag... tags) {
        return PrometheusMetricRegistry.create().gauge(name(), supplier, tags);
    }

    default SimpleTimer simpleTimer(Tag... tags) {
        return PrometheusMetricRegistry.create().simpleTimer(name(), tags);
    }

    default Counter counter(Tag... tags) {
        return PrometheusMetricRegistry.create().counter(name(), tags);
    }

    default String getName() {
        return name();
    }

    String name();
}
