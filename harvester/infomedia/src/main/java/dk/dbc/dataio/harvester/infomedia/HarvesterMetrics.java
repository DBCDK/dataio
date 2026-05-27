package dk.dbc.dataio.harvester.infomedia;

import org.eclipse.microprofile.metrics.Metadata;

/**
 * Enumeration of metrics specific to the Infomedia harvester.
 * <p>
 * Each metric constant provides metadata describing a measurable aspect
 * of the harvester's operation, including the metric name, description,
 * and unit of measurement. These metrics are intended for monitoring
 * and observability purposes.
 * <p>
 * The metrics use a standardized metadata format that can be consumed
 * by metric collection and monitoring systems.
 */
public enum HarvesterMetrics {
    UNEXPECTED_HARVESTER_EXCEPTIONS(Metadata.builder()
            .withName("dataio_harvester_infomedia_unexpected_harvester_exceptions_counter")
            .withDescription("Number of unexpected HarvesterException instances thrown")
            .withUnit("exceptions")
            .build()),
    CREATOR_NAME_SUGGESTIONS_TIMER(Metadata.builder()
            .withName("dataio_harvester_infomedia_creator_name_suggestions_timer")
            .withDescription("Time spent fetching creator name suggestions from creator-detector service")
            .build()),
    TAGS_TIMER(Metadata.builder()
            .withName("dataio_harvester_infomedia_tags_timer")
            .withDescription("Time spent fetching tags from tag-stack service")
            .build());

    private final Metadata metadata;

    HarvesterMetrics(Metadata metadata) {
        this.metadata = metadata;
    }

    public Metadata getMetadata() {
        return metadata;
    }
}
