/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GNU GPLv3
 * See license text in LICENSE.txt
 */

package dk.dbc.dataio.sink.openupdate.metrics;

import org.eclipse.microprofile.metrics.Metadata;
import org.eclipse.microprofile.metrics.MetricType;

public enum CounterMetrics implements dk.dbc.commons.metricshandler.CounterMetric {
    CHUNK_ITEMS(Metadata.builder()
            .withName("dataio_sink_openupdate_chunk_item_counter")
            .withDescription("Number of chunk items processed")
            .withType(MetricType.COUNTER)
            .withUnit("chunkitems").build()),
    UNHANDLED_EXCEPTIONS(Metadata.builder()
            .withName("dataio_sink_openupdate_exception_counter")
            .withDescription("Number of unhandled exceptions caught")
            .withType(MetricType.COUNTER)
            .withUnit("exceptions").build());

    private final Metadata metadata;

    CounterMetrics(Metadata metadata) {
        this.metadata = validateMetadata(metadata);
    }

    @Override
    public Metadata getMetadata() {
        return metadata;
    }
}
