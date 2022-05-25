package dk.dbc.dataio.sink.holdingsitems.metrics;

import org.eclipse.microprofile.metrics.Metadata;
import org.eclipse.microprofile.metrics.MetricType;
import org.eclipse.microprofile.metrics.MetricUnits;

public enum SimpleTimerMetrics implements dk.dbc.commons.metricshandler.SimpleTimerMetric {
    HOLDING_EXISTS_REQUESTS(Metadata.builder()
            .withName("dataio_sink_holdings_items_holdingexists_request_timer")
            .withDescription("Duration of holdingExists requests")
            .withType(MetricType.SIMPLE_TIMER)
            .withUnit(MetricUnits.MILLISECONDS).build()),
    SET_HOLDINGS_REQUESTS(Metadata.builder()
            .withName("dataio_sink_holdings_items_setholdings_request_timer")
            .withDescription("Duration of setHoldings requests")
            .withType(MetricType.SIMPLE_TIMER)
            .withUnit(MetricUnits.MILLISECONDS).build());

    private final Metadata metadata;

    SimpleTimerMetrics(Metadata metadata) {
        this.metadata = validateMetadata(metadata);
    }

    @Override
    public Metadata getMetadata() {
        return metadata;
    }
}
