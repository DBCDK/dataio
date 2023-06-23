package dk.dbc.dataio.sink.ticklerepo;

import dk.dbc.dataio.registry.PrometheusMetricMixin;

public enum Metric implements PrometheusMetricMixin {
    HANDLE_CHUNK_ITEM,
    CHUNK_ITEM_FAILURES;
}
