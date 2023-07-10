package dk.dbc.dataio.sink.worldcat;

import dk.dbc.dataio.registry.PrometheusMetricMixin;

public enum Metric implements PrometheusMetricMixin {
    WCIRU_UPDATE,
    WCIRU_CHUNK_UPDATE,
    WCIRU_SERVICE_REQUESTS,
    UNHANDLED_EXCEPTIONS;


    @Override
    public String getName() {
        return "dataio_" + name().toLowerCase();
    }
}
