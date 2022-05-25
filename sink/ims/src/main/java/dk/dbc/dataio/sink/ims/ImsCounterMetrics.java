package dk.dbc.dataio.sink.ims;

import dk.dbc.commons.metricshandler.CounterMetric;
import org.eclipse.microprofile.metrics.Metadata;
import org.eclipse.microprofile.metrics.MetricType;

enum ImsCounterMetrics implements CounterMetric {

    IMS_FAILURES(Metadata.builder()
            .withName("dataio_sink_ims_failures_counter")
            .withDescription("Number of failures (exceptions) while making requests to IMS")
            .withType(MetricType.COUNTER)
            .withUnit("failures").build()),

    UNHANDLED_EXCEPTIONS(Metadata.builder()
            .withName("dataio_sink_ims_unhandled_exceptions_counter")
            .withDescription("Number of unhandled exceptions caught")
            .withType(MetricType.COUNTER)
            .withUnit("exceptions").build());

    private final Metadata metadata;

    ImsCounterMetrics(Metadata metadata) {
        this.metadata = validateMetadata(metadata);
    }

    @Override
    public Metadata getMetadata() {
        return metadata;
    }
}
