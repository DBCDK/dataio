package dk.dbc.dataio.jobprocessor2;

import dk.dbc.dataio.registry.PrometheusMetricMixin;
import org.eclipse.microprofile.metrics.Tag;

public enum Metric implements PrometheusMetricMixin {
    dataio_jobprocessor_chunk_duration_ms,
    dataio_jobprocessor_slow_jobs,
    dataio_message_count,
    dataio_message_time,
    dataio_flow_cache_hit_rate,
    dataio_flow_cache_get;

    public enum ATag {
        rollback, rejected;

        public Tag is(String value) {
            return new Tag(name(), value);
        }
    }
}
