package dk.dbc.dataio.registry;

import org.eclipse.microprofile.metrics.Counter;
import org.eclipse.microprofile.metrics.Gauge;
import org.eclipse.microprofile.metrics.Tag;
import org.eclipse.microprofile.metrics.Timer;

import java.util.function.Supplier;

public interface PrometheusMetricMixin {
    default  <T extends Number> Gauge<T> gauge(Supplier<T> supplier, Tag... tags) {
        return PrometheusMetricRegistry.create().gauge(getName(), supplier, tags);
    }

    default Timer timer(Tag... tags) {
        return PrometheusMetricRegistry.create().timer(getName(), tags);
    }

    default Counter counter(Tag... tags) {
        return PrometheusMetricRegistry.create().counter(getName(), tags);
    }

    default String getName() {
        return name();
    }

    String name();
}
