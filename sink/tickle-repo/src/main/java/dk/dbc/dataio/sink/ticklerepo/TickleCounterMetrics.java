package dk.dbc.dataio.sink.ticklerepo;

import dk.dbc.commons.metricshandler.CounterMetric;
import org.eclipse.microprofile.metrics.Metadata;
import org.eclipse.microprofile.metrics.MetricType;

enum TickleCounterMetrics implements CounterMetric {

    CHUNK_ITEM_FAILURES(Metadata.builder()
            .withName("dataio_sink_tickle_repo_chunk_item_failures_counter")
            .withDescription("Number of failures (exceptions) while handling a single chunk item")
            .withType(MetricType.COUNTER)
            .withUnit("chunkitems").build()),

    UNHANDLED_EXCEPTIONS(Metadata.builder()
            .withName("dataio_sink_tickle_repo_unhandled_exceptions_counter")
            .withDescription("Number of unhandled exceptions caught")
            .withType(MetricType.COUNTER)
            .withUnit("exceptions").build());

    private final Metadata metadata;

    TickleCounterMetrics(Metadata metadata) {
        this.metadata = validateMetadata(metadata);
    }

    @Override
    public Metadata getMetadata() {
        return metadata;
    }
}
