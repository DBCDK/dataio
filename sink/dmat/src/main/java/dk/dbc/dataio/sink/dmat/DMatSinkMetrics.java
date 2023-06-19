package dk.dbc.dataio.sink.dmat;

import dk.dbc.dataio.registry.PrometheusMetricMixin;

enum DMatSinkMetrics implements PrometheusMetricMixin {
    DMAT_SERVICE_REQUESTS_TIMER("dataio_sink_dmat_dmat_service_requests_timer"),
    DMAT_FAILED_RECORDS("dataio_sink_dmat_dmat_failed_records_counter"),
    SINK_FAILED_RECORDS("dataio_sink_dmat_sink_failed_records_counter"),
    UNEXPECTED_EXCEPTIONS("dataio_sink_dmat_unexpected_exceptions_counter");

    private final String name;

    DMatSinkMetrics(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }
}
