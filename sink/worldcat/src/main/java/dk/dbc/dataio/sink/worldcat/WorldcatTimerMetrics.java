package dk.dbc.dataio.sink.worldcat;

import dk.dbc.commons.metricshandler.SimpleTimerMetric;
import org.eclipse.microprofile.metrics.Metadata;
import org.eclipse.microprofile.metrics.MetricType;
import org.eclipse.microprofile.metrics.MetricUnits;

enum WorldcatTimerMetrics implements SimpleTimerMetric {

    WCIRU_SERVICE_REQUESTS(Metadata.builder()
            .withName("dataio_sink_worldcat_handle_chunk_item_timer")
            .withDescription("Duration of handling a chunk item")
            .withType(MetricType.SIMPLE_TIMER)
            .withUnit(MetricUnits.MILLISECONDS).build()),

    WCIRU_CHUNK_UPDATE(Metadata.builder()
            .withName("dataio_sink_worldcat_handle_chunk_timer")
            .withDescription("Elapsed time of chunks sent to wciru")
            .withType(MetricType.COUNTER)
            .withUnit(MetricUnits.MILLISECONDS).build());

    private final Metadata metadata;

    WorldcatTimerMetrics(Metadata metadata) {
        this.metadata = validateMetadata(metadata);
    }

    @Override
    public Metadata getMetadata() {
        return metadata;
    }
}
