package dk.dbc.dataio.harvester.promat;

import dk.dbc.commons.metricshandler.CounterMetric;
import org.eclipse.microprofile.metrics.Metadata;
import org.eclipse.microprofile.metrics.MetricType;

enum PromatHarvesterMetrics implements CounterMetric {

    RECORDS_HARVESTED(Metadata.builder()
            .withName("dataio_harvester_promatmat_records_harvested_counter")
            .withDescription("Number of records harvested from promat")
            .withType(MetricType.COUNTER)
            .withUnit("records").build()),
    RECORDS_ADDED(Metadata.builder()
            .withName("dataio_harvester_promat_records_added_counter")
            .withDescription("Number of records added to the job")
            .withType(MetricType.COUNTER)
            .withUnit("records").build()),
    RECORDS_PROCESSED(Metadata.builder()
            .withName("dataio_harvester_promat_records_processed_counter")
            .withDescription("Number of records processed")
            .withType(MetricType.COUNTER)
            .withUnit("records").build()),
    RECORDS_FAILED(Metadata.builder()
            .withName("dataio_harvester_promat_records_failed_counter")
            .withDescription("Number of failed records")
            .withType(MetricType.COUNTER)
            .withUnit("records").build()),
    EXCEPTIONS(Metadata.builder()
            .withName("dataio_harvester_promat_exceptions_counter")
            .withDescription("Number of acceptable exceptions caught")
            .withType(MetricType.COUNTER)
            .withUnit("exceptions").build()),
    UNHANDLED_EXCEPTIONS(Metadata.builder()
            .withName("dataio_harvester_promat_unhandled_exceptions_counter")
            .withDescription("Number of unhandled exceptions caught")
            .withType(MetricType.COUNTER)
            .withUnit("exceptions").build());

    private final Metadata metadata;

    PromatHarvesterMetrics(Metadata metadata) {
        this.metadata = validateMetadata(metadata);
    }

    @Override
    public Metadata getMetadata() {
        return metadata;
    }
}
