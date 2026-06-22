package dk.dbc.dataio.jobprocessorgjs;

import dk.dbc.dataio.registry.PrometheusMetricMixin;

public enum Metric implements PrometheusMetricMixin {
    dataio_jobprocessor_chunk_duration_ms,
    dataio_jobprocessor_slow_jobs,
    dataio_flow_cache_hit_rate,
    dataio_flow_cache_size,
    dataio_flow_cache_fetch,
    dataio_flow_cache_fetch_time,
    dataio_jobprocessor_chunk_failed
}
