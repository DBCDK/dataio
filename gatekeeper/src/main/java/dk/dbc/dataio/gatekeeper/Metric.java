package dk.dbc.dataio.gatekeeper;

import dk.dbc.dataio.registry.JMXMetricRegistry;
import org.eclipse.microprofile.metrics.Counter;
import org.eclipse.microprofile.metrics.Gauge;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.Tag;

import java.util.function.Supplier;

public enum Metric {
    DATA_BYTES_UPLOADED,
    DATA_FILES_UPLOADED,
    DATA_FILES_REMOVED,
    CREATE_JOB,
    LIVENESS;

    private static final String PREFIX = "dataio_jobstore_";
    private static final MetricRegistry METRIC_REGISTRY = JMXMetricRegistry.create();
    public static final Tag TAG_FAILED = new Tag("status", "failed");


    public Counter counter(Tag... tags) {
        return METRIC_REGISTRY.counter(getName(), tags);
    }

    public <T extends Number> Gauge<T> gauge(Supplier<T> supplier, Tag... tags) {
        return METRIC_REGISTRY.gauge(getName(), supplier, tags);
    }

    public String getName() {
        return PREFIX + name().toLowerCase();
    }
}
