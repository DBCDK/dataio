package dk.dbc.dataio.jse.artemis.common;

import dk.dbc.dataio.registry.PrometheusMetricMixin;
import org.eclipse.microprofile.metrics.Tag;

public enum Metric implements PrometheusMetricMixin {
    dataio_message_count,
    dataio_message_time,
    dataio_tx_elapsed;

    public enum ATag {
        rollback, rejected, redelivery, destination;

        public Tag is(String value) {
            return new Tag(name(), value);
        }
    }
}
