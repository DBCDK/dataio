package dk.dbc.dataio.sink.worldcat;

import dk.dbc.commons.metricshandler.CounterMetric;
import org.eclipse.microprofile.metrics.Metadata;
import org.eclipse.microprofile.metrics.MetricType;

enum WorldcatCounterMetrics implements CounterMetric {

    CHUNK_ITEM_FAILURES(Metadata.builder()
            .withName("dataio_sink_worldcat_chunk_item_failures_counter")
            .withDescription("Number of failures (exceptions) while handling a single chunk item")
            .withType(MetricType.COUNTER)
            .withUnit("chunkitems").build()),

    UNHANDLED_EXCEPTIONS(Metadata.builder()
            .withName("dataio_sink_worldcat_unhandled_exceptions_counter")
            .withDescription("Number of unhandled exceptions caught")
            .withType(MetricType.COUNTER)
            .withUnit("exceptions").build());

    private final Metadata metadata;

    WorldcatCounterMetrics(Metadata metadata) {
        this.metadata = validateMetadata(metadata);
    }

    @Override
    public Metadata getMetadata() {
        return metadata;
    }
}
