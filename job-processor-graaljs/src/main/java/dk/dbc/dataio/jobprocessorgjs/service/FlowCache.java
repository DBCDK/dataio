package dk.dbc.dataio.jobprocessorgjs.service;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalNotification;
import dk.dbc.dataio.commons.types.Flow;
import dk.dbc.dataio.commons.types.FlowContent;
import dk.dbc.dataio.jobprocessorgjs.Metric;
import dk.dbc.dataio.jobprocessorgjs.javascript.GraalJsScript;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

public class FlowCache {
    private static final Logger LOGGER = LoggerFactory.getLogger(FlowCache.class);

    private final Cache<String, FlowCacheEntry> cache;

    public FlowCache(int maxSize, Duration expiry) {
        cache = CacheBuilder.newBuilder()
                .maximumSize(maxSize)
                .expireAfterAccess(expiry)
                .removalListener(FlowCache::onRemoval)
                .recordStats()
                .build();
        Metric.dataio_flow_cache_hit_rate.gauge(() -> cache.stats().hitRate());
        Metric.dataio_flow_cache_size.gauge(cache::size);
        Metric.dataio_flow_cache_fetch.gauge(() -> cache.stats().loadCount());
        Metric.dataio_flow_cache_fetch_time.gauge(() -> cache.stats().totalLoadTime());
    }

    public FlowCacheEntry get(String key, Callable<Flow> loader) throws ExecutionException {
        return cache.get(key, () -> new FlowCacheEntry(loader.call()));
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

    private static GraalJsScript createScript(Flow flow) {
        long start = System.currentTimeMillis();
        FlowContent content = flow.getContent();
        byte[] jsar = content.getJsar();
        if (jsar == null) {
            throw new IllegalArgumentException(
                    "Flow '" + content.getName() + "' (id=" + flow.getId() + ") has no embedded JSAR");
        }
        try {
            return new GraalJsScript(content.getEntrypointScript(), content.getEntrypointFunction(), jsar);
        } finally {
            LOGGER.info("Created GraalJS script for flow '{}' id={} version={} in {} ms",
                    content.getName(), flow.getId(), flow.getVersion(), System.currentTimeMillis() - start);
        }
    }

    public static class FlowCacheEntry {
        public final Flow flow;
        public final GraalJsScript script;

        FlowCacheEntry(Flow flow) {
            this.flow = Objects.requireNonNull(flow);
            this.script = createScript(flow);
        }
    }
}
