package dk.dbc.dataio.harvester.dmatdm3;

import org.eclipse.microprofile.metrics.Metadata;

@SuppressWarnings("java:S1192")
enum DmatDm3HarvesterMetrics {
    RECORDS_HARVESTED(Metadata.builder()
            .withName("dataio_harvester_dmat_dm3_records_harvested_counter")
            .withDescription("Number of records harvested from dmat")
            .withUnit("records").build()),
    RECORDS_ADDED(Metadata.builder()
            .withName("dataio_harvester_dmat_dm3_records_added_counter")
            .withDescription("Number of records added to the job")
            .withUnit("records").build()),
    RECORDS_PROCESSED(Metadata.builder()
            .withName("dataio_harvester_dmat_dm3_records_processed_counter")
            .withDescription("Number of records processed")
            .withUnit("records").build()),
    RECORDS_FAILED(Metadata.builder()
            .withName("dataio_harvester_dmat_dm3_records_failed_counter")
            .withDescription("Number of failed records")
            .withUnit("records").build()),
    EXCEPTIONS(Metadata.builder()
            .withName("dataio_harvester_dmat_dm3_exceptions_counter")
            .withDescription("Number of acceptable exceptions caught")
            .withUnit("exceptions").build()),
    UNHANDLED_EXCEPTIONS(Metadata.builder()
            .withName("dataio_harvester_dmat_dm3_unhandled_exceptions_counter")
            .withDescription("Number of unhandled exceptions caught")
            .withUnit("exceptions").build());

    private final Metadata metadata;

    DmatDm3HarvesterMetrics(Metadata metadata) {
        this.metadata = metadata;
    }

    public Metadata getMetadata() {
        return metadata;
    }
}
