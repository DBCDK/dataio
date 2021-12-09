package dk.dbc.dataio.sink.dmat;

import dk.dbc.commons.metricshandler.SimpleTimerMetric;
import org.eclipse.microprofile.metrics.Metadata;
import org.eclipse.microprofile.metrics.MetricType;
import org.eclipse.microprofile.metrics.MetricUnits;

enum DMatSinkTimerMetrics implements SimpleTimerMetric {

    DMAT_SERVICE_REQUESTS(Metadata.builder()
            .withName("dataio_sink_dmat_dmat_service_requests_timer")
            .withDescription("Duration of successfully handling a DMat record when posted to DMat")
            .withType(MetricType.SIMPLE_TIMER)
            .withUnit(MetricUnits.MILLISECONDS).build());

    private final Metadata metadata;

    DMatSinkTimerMetrics(Metadata metadata) {
        this.metadata = validateMetadata(metadata);
    }

    @Override
    public Metadata getMetadata() {
        return metadata;
    }
}
