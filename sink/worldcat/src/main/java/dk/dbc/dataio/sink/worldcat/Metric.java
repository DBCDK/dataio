package dk.dbc.dataio.sink.worldcat;

import dk.dbc.dataio.registry.PrometheusMetricMixin;

public enum Metric implements PrometheusMetricMixin {
    WCIRU_UPDATE,
    WCIRU_SERVICE_REQUESTS;


    @Override
    public String getName() {
        return "dataio_" + name().toLowerCase();
    }
}
