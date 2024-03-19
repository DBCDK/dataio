package dk.dbc.dataio.harvester.dmat;

import dk.dbc.commons.metricshandler.CounterMetric;
import org.eclipse.microprofile.metrics.Metadata;

enum DmatHarvesterMetrics implements CounterMetric {

    RECORDS_HARVESTED(Metadata.builder()
            .withName("dataio_harvester_dmat_records_harvested_counter")
            .withDescription("Number of records harvested from dmat")
            .withUnit("records").build()),
    RECORDS_ADDED(Metadata.builder()
            .withName("dataio_harvester_dmat_records_added_counter")
            .withDescription("Number of records added to the job")
            .withUnit("records").build()),
    RECORDS_PROCESSED(Metadata.builder()
            .withName("dataio_harvester_dmat_records_processed_counter")
            .withDescription("Number of records processed")
            .withUnit("records").build()),
    RECORDS_FAILED(Metadata.builder()
            .withName("dataio_harvester_dmat_records_failed_counter")
            .withDescription("Number of failed records")
            .withUnit("records").build()),
    EXCEPTIONS(Metadata.builder()
            .withName("dataio_harvester_dmat_exceptions_counter")
            .withDescription("Number of acceptable exceptions caught")
            .withUnit("exceptions").build()),
    UNHANDLED_EXCEPTIONS(Metadata.builder()
            .withName("dataio_harvester_dmat_unhandled_exceptions_counter")
            .withDescription("Number of unhandled exceptions caught")
            .withUnit("exceptions").build());

    private final Metadata metadata;

    DmatHarvesterMetrics(Metadata metadata) {
        this.metadata = metadata;
    }

    @Override
    public Metadata getMetadata() {
        return metadata;
    }
}
