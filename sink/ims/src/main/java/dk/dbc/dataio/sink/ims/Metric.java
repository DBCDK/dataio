package dk.dbc.dataio.sink.ims;

import dk.dbc.dataio.registry.PrometheusMetricMixin;

public enum Metric implements PrometheusMetricMixin {
    IMS_FAILURES,
    REQUEST_DURATION,
    UNHANDLED_EXCEPTIONS
}
