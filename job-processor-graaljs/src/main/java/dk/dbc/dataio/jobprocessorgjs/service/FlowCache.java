package dk.dbc.dataio.jobprocessorgjs.service;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheStats;
import com.google.common.cache.RemovalNotification;
import dk.dbc.dataio.commons.types.Flow;
import dk.dbc.dataio.commons.types.FlowContent;
import dk.dbc.dataio.jobprocessorgjs.Metric;
import dk.dbc.dataio.jobprocessorgjs.javascript.GraalJsScript;
import org.graalvm.polyglot.Engine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Collection;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

public class FlowCache {
    private static final Logger LOGGER = LoggerFactory.getLogger(FlowCache.class);

    private final Cache<String, FlowCacheEntry> cache;
    private final Engine engine;

    public FlowCache(int maxSize, Duration expiry, Engine engine) {
        this.engine = engine;
        cache = CacheBuilder.newBuilder()
                .maximumSize(maxSize)
                .expireAfterAccess(expiry)
                .removalListener(FlowCache::onRemoval)
                .recordStats()
                .build();
    }

    /**
     * Registers the flow-cache gauges once, aggregating across all per-thread caches.
     * Each consumer thread owns its own {@link FlowCache}, so a gauge registered per instance
     * would be silently deduplicated by metric name and report only the first cache. Registering
     * here over the whole collection yields totals (or, for hit rate, a pooled ratio) across threads.
     */
    public static void registerMetrics(Collection<FlowCache> caches) {
        Metric.dataio_flow_cache_hit_rate.gauge(() -> aggregateHitRate(caches));
        Metric.dataio_flow_cache_size.gauge(() -> caches.stream().mapToLong(c -> c.cache.size()).sum());
        Metric.dataio_flow_cache_fetch.gauge(() -> caches.stream().mapToLong(c -> c.cache.stats().loadCount()).sum());
        Metric.dataio_flow_cache_fetch_time.gauge(
                () -> caches.stream().mapToLong(c -> c.cache.stats().totalLoadTime()).sum());
    }

    private static double aggregateHitRate(Collection<FlowCache> caches) {
        long hits = 0;
        long requests = 0;
        for (FlowCache flowCache : caches) {
            CacheStats stats = flowCache.cache.stats();
            hits += stats.hitCount();
            requests += stats.requestCount();
        }
        // Mirror Guava's CacheStats.hitRate(): a cache with no requests is reported as 100% hits.
        return requests == 0 ? 1.0 : (double) hits / requests;
    }

    public FlowCacheEntry get(String key, Callable<Flow> loader) throws ExecutionException {
        return cache.get(key, () -> new FlowCacheEntry(loader.call(), engine));
    }

    public void clear() {
        cache.invalidateAll();
    }

    private static void onRemoval(RemovalNotification<String, FlowCacheEntry> notification) {
        FlowCacheEntry entry = notification.getValue();
        if (entry != null) {
            entry.script.close();
        }
    }

    private static GraalJsScript createScript(Flow flow, Engine engine) {
        long start = System.currentTimeMillis();
        FlowContent content = flow.getContent();
        byte[] jsar = content.getJsar();
        if (jsar == null) {
            throw new IllegalArgumentException(
                    "Flow '" + content.getName() + "' (id=" + flow.getId() + ") has no embedded JSAR");
        }
        try {
            return new GraalJsScript(content.getEntrypointScript(), content.getEntrypointFunction(), jsar, engine);
        } finally {
            LOGGER.info("Created GraalJS script for flow '{}' id={} version={} in {} ms",
                    content.getName(), flow.getId(), flow.getVersion(), System.currentTimeMillis() - start);
        }
    }

    public static class FlowCacheEntry {
        public final Flow flow;
        public final GraalJsScript script;

        FlowCacheEntry(Flow flow, Engine engine) {
            this.flow = Objects.requireNonNull(flow);
            this.script = createScript(flow, engine);
        }
    }
}
