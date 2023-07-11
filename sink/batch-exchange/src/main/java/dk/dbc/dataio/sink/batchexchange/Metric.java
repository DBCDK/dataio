package dk.dbc.dataio.sink.batchexchange;

import dk.dbc.dataio.registry.PrometheusMetricMixin;

public enum Metric implements PrometheusMetricMixin {
    dataio_entry_timer,
    dataio_error_counter;
}
