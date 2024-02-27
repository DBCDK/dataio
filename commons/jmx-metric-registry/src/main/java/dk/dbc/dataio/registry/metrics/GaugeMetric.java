package dk.dbc.dataio.registry.metrics;

import org.eclipse.microprofile.metrics.Gauge;

import java.util.function.Supplier;

public class GaugeMetric<T extends Number> implements Gauge<T>, GaugeMetricMBean<T> {
    private final Supplier<T> supplier;

    public GaugeMetric(Supplier<T> supplier) {
        this.supplier = supplier;
    }

    @Override
    public T getValue() {
        return supplier.get();
    }
}
