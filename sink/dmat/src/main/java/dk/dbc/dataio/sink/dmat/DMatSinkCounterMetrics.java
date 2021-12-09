package dk.dbc.dataio.sink.dmat;

import dk.dbc.commons.metricshandler.CounterMetric;
import org.eclipse.microprofile.metrics.Metadata;
import org.eclipse.microprofile.metrics.MetricType;

enum DMatSinkCounterMetrics implements CounterMetric {

    DMAT_FAILED_RECORDS(Metadata.builder()
            .withName("dataio_sink_dmat_dmat_failed_records_counter")
            .withDescription("Number of records that was failed by DMat")
            .withType(MetricType.COUNTER)
            .withUnit("chunkitems").build()),
    SINK_FAILED_RECORDS(Metadata.builder()
            .withName("dataio_sink_dmat_sink_failed_records_counter")
            .withDescription("Number of records that was failed by the sink before posting to DMat")
            .withType(MetricType.COUNTER)
            .withUnit("chunkitems").build()),
    // We do not need a '...SUCCESSFULL_RECORDS...' since the DMAT_SERVICE_REQUESTS timer
    // has an implicit counter, thus the number of requests can be extracted from that metric
    UNEXPECTED_EXCEPTIONS(Metadata.builder()
            .withName("dataio_sink_dmat_unexpected_exceptions_counter")
            .withDescription("Number of unexpected exceptions caught")
            .withType(MetricType.COUNTER)
            .withUnit("exceptions").build());

    private final Metadata metadata;

    DMatSinkCounterMetrics(Metadata metadata) {
        this.metadata = validateMetadata(metadata);
    }

    @Override
    public Metadata getMetadata() {
        return metadata;
    }
}
