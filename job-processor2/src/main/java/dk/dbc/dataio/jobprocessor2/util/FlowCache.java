package dk.dbc.dataio.jobprocessor2.util;

import dk.dbc.dataio.commons.time.StopWatch;
import dk.dbc.dataio.commons.types.Flow;
import dk.dbc.dataio.commons.types.FlowComponent;
import dk.dbc.dataio.commons.types.FlowComponentContent;
import dk.dbc.dataio.commons.types.JavaScript;
import dk.dbc.dataio.commons.utils.lang.StringUtil;
import dk.dbc.dataio.jobprocessor2.Metric;
import dk.dbc.dataio.jobprocessor2.javascript.Script;
import dk.dbc.dataio.jobprocessor2.javascript.StringSourceSchemeHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * This class implements a LRU Flow cache with a simple get(key), put(key, flow) and containsKey(key)
 * API.
 */
public class FlowCache {
    public static final int CACHE_MAX_ENTRIES = 50;
    private static final Logger LOGGER = LoggerFactory.getLogger(FlowCache.class);

    // A LRU cache using a LinkedHashMap with access-ordering
    private final LinkedHashMap<String, FlowCacheEntry> flowCache;
    private static final AtomicLong CACHE_HITS = new AtomicLong(0);
    private static final AtomicLong CACHE_MISS = new AtomicLong(0);

    public FlowCache() {
        flowCache = new LinkedHashMap<>(CACHE_MAX_ENTRIES + 1, .75F, true) {
            @Override
            public boolean removeEldestEntry(Map.Entry eldest) {
                return size() > CACHE_MAX_ENTRIES;
            }
        };
        Metric.dataio_flow_cache_hit_rate.gauge(this::getHitRatePercentage);
    }

    private double getHitRatePercentage() {
        long hits = CACHE_HITS.get();
        long total = hits + CACHE_MISS.get();
        if(total == 0) return -1;
        return 100.0 * hits / total;
    }

    /**
     * @param key key whose presence in this cache is to be tested
     * @return true if this cache contains an entry for the specified key, otherwise false
     */
    public boolean containsKey(String key) {
        return flowCache.containsKey(key);
    }

    /**
     * @param key key whose associated value in this cache is to be returned
     * @return the value to which the specified key is mapped, or null if this cache contains no mapping for the key
     */
    public FlowCacheEntry get(String key) {
        FlowCacheEntry entry = flowCache.get(key);
        if(entry == null) CACHE_MISS.incrementAndGet();
        else CACHE_HITS.incrementAndGet();
        return entry;
    }

    /**
     * Creates script environment for the given flow and associates it with the specified key in this cache.
     * If this cache previously contained a mapping for the key, the old value is replaced by the new entry.
     *
     * @param key  key with which the create script environment is to be associated in this cache
     * @param flow flow from which a scripting environment is created
     * @return script environment as FlowCacheEntry
     * @throws IllegalStateException if given flow contains no script
     * @throws Exception             on general script environment creation failure
     */
    public FlowCacheEntry put(String key, Flow flow) throws Exception {
        LOGGER.info("Setting up javascript environment for flow '{}'", flow.getContent().getName());
        StopWatch stopWatch = new StopWatch();
        try {
            FlowCacheEntry cacheEntry = new FlowCacheEntry(flow);
            flowCache.put(key, cacheEntry);
            return cacheEntry;
        } finally {
            LOGGER.info("Setting up javascript environment for flow '{}' took {} ms",
                    flow.getContent().getName(), stopWatch.getElapsedTime());
        }
    }

    private static Script createScript(FlowComponentContent componentContent) throws Exception {
        StopWatch stopWatch = new StopWatch();
        try {
            List<JavaScript> javaScriptsBase64 = componentContent.getJavascripts();
            List<StringSourceSchemeHandler.Script> javaScripts = new ArrayList<>(javaScriptsBase64.size());
            for (JavaScript javascriptBase64 : javaScriptsBase64) {
                javaScripts.add(new StringSourceSchemeHandler.Script(javascriptBase64.getModuleName(),
                        StringUtil.base64decode(javascriptBase64.getJavascript())));
            }
            String requireCacheJson = null;
            if (componentContent.getRequireCache() != null) {
                requireCacheJson = StringUtil.base64decode(componentContent.getRequireCache());
            }
            return new Script(componentContent.getName(), componentContent.getInvocationMethod(), javaScripts, requireCacheJson);
        } finally {
            LOGGER.info("Creating javascript for flow component '{}' took {} ms",
                    componentContent.getName(), stopWatch.getElapsedTime());
        }
    }

    /**
     * FlowCache entry abstraction giving direct access to the 'flow' itself and 'script' and 'next' environments.
     */
    public static class FlowCacheEntry {
        public final Flow flow;
        public final List<Script> scripts = new ArrayList<>();
        public final List<Script> next = new ArrayList<>();

        public FlowCacheEntry(Flow flow) throws Exception {
            this.flow = flow;
            for (FlowComponent flowComponent : flow.getContent().getComponents()) {
                scripts.add(createScript(flowComponent.getContent()));
                FlowComponentContent flowComponentNextContent = flowComponent.getNext();
                if (flowComponentNextContent != FlowComponent.UNDEFINED_NEXT) {
                    next.add(createScript(flowComponentNextContent));
                }
            }
            if (scripts.isEmpty()) {
                throw new IllegalStateException(String.format("No javascript found in flow '%s'", flow.getContent().getName()));
            }
        }
    }
}
