package dk.dbc.dataio.harvester.promat;

import org.eclipse.microprofile.metrics.Counter;
import org.eclipse.microprofile.metrics.MetricRegistry;

enum PromatHarvesterMetrics {

    RECORDS_HARVESTED("records_harvested_counter"),
    RECORDS_ADDED("records_added_counter"),
    RECORDS_PROCESSED("records_processed_counter"),
    RECORDS_FAILED("records_failed_counter"),
    EXCEPTIONS("exceptions_counter"),
    UNHANDLED_EXCEPTIONS("unhandled_exceptions_counter");

    private final String metricName;
    private static final String PREFIX = "dataio_harvester_promat_";

    PromatHarvesterMetrics(String metricName) {
        this.metricName = metricName;
    }

    public Counter counter(MetricRegistry registry) {
        return registry.counter("prefix" + name());
    }
}
