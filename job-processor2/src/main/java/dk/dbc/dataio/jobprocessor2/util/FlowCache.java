package dk.dbc.dataio.jobprocessor2.util;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import dk.dbc.dataio.commons.time.StopWatch;
import dk.dbc.dataio.commons.types.Flow;
import dk.dbc.dataio.commons.types.FlowComponent;
import dk.dbc.dataio.commons.types.FlowComponentContent;
import dk.dbc.dataio.commons.types.JavaScript;
import dk.dbc.dataio.commons.utils.lang.StringUtil;
import dk.dbc.dataio.jobprocessor2.Metric;
import dk.dbc.dataio.jobprocessor2.ProcessorConfig;
import dk.dbc.dataio.jobprocessor2.javascript.Script;
import dk.dbc.dataio.jobprocessor2.javascript.StringSourceSchemeHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

public class FlowCache {
    private static final Logger LOGGER = LoggerFactory.getLogger(FlowCache.class);

    private final Cache<String, FlowCacheEntry> flowCache = CacheBuilder.newBuilder()
            .maximumSize(ProcessorConfig.FLOW_CACHE_SIZE.asInteger())
            .expireAfterAccess(ProcessorConfig.FLOW_CACHE_EXPIRY.asDuration())
            .recordStats()
            .build();

    public FlowCache() {
        Metric.dataio_flow_cache_hit_rate.gauge(() -> flowCache.stats().hitRate());
        Metric.dataio_flow_cache_size.gauge(flowCache::size);
        Metric.dataio_flow_cache_fetch.gauge(() -> flowCache.stats().loadCount());
        Metric.dataio_flow_cache_fetch_time.gauge(() -> flowCache.stats().totalLoadTime());
    }

    public void clear() {
        flowCache.invalidateAll();
    }

    public Map<String, FlowCacheEntry> getView() {
        return Collections.unmodifiableMap(flowCache.asMap());
    }

    /**
     * @param key key whose associated value in this cache is to be returned
     * @return the value to which the specified key is mapped, or null if this cache contains no mapping for the key
     */
    public FlowCacheEntry get(String key, Callable<Flow> loader) throws ExecutionException {
        return flowCache.get(key, () -> new FlowCacheEntry(loader.call()));
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
        public final List<Script> scripts;
        public final List<Script> next;

        public FlowCacheEntry(Flow flow) throws Exception {
            List<Script> scripts = new ArrayList<>();
            List<Script> next = new ArrayList<>();
            this.flow = Objects.requireNonNull(flow);
            for (FlowComponent flowComponent : flow.getContent().getComponents()) {
                scripts.add(createScript(flowComponent.getContent()));
                FlowComponentContent flowComponentNextContent = flowComponent.getNext();
                if (flowComponentNextContent != FlowComponent.UNDEFINED_NEXT) {
                    next.add(createScript(flowComponentNextContent));
                }
            }
            this.scripts = Collections.unmodifiableList(scripts);
            this.next = Collections.unmodifiableList(next);
            if (scripts.isEmpty()) {
                throw new IllegalStateException(String.format("No javascript found in flow '%s'", flow.getContent().getName()));
            }
        }
    }
}
