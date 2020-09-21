package dk.dbc.dataio.sink.ims;

import dk.dbc.commons.metricshandler.SimpleTimerMetric;

import org.eclipse.microprofile.metrics.Metadata;
import org.eclipse.microprofile.metrics.MetricType;
import org.eclipse.microprofile.metrics.MetricUnits;

enum ImsTimerMetrics implements SimpleTimerMetric {

    REQUEST_DURATION(Metadata.builder()
            .withName("dataio_sink_ims_request_duration_timer")
            .withDescription("Time used on handling IMS requests")
            .withType(MetricType.SIMPLE_TIMER)
            .withUnit(MetricUnits.MILLISECONDS).build());

    private final Metadata metadata;

    ImsTimerMetrics(Metadata metadata) {
        this.metadata = validateMetadata(metadata);
    }

    @Override
    public Metadata getMetadata() {
        return metadata;
    }
}
