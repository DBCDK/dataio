package dk.dbc.dataio.jobprocessor2;

import dk.dbc.dataio.registry.PrometheusMetricMixin;
import org.eclipse.microprofile.metrics.Tag;

public enum Metric implements PrometheusMetricMixin {
    dataio_jobprocessor_chunk_duration_ms,
    dataio_jobprocessor_slow_jobs,
    dataio_message_count,
    dataio_message_time;

    public enum ATag {
        rollback, rejected;

        public Tag is(String value) {
            return new Tag(name(), value);
        }
    }
}
