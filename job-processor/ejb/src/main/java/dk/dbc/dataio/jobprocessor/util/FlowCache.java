package dk.dbc.dataio.jobprocessor.util;

import dk.dbc.dataio.commons.types.Flow;
import dk.dbc.dataio.commons.types.FlowComponent;
import dk.dbc.dataio.commons.types.FlowComponentContent;
import dk.dbc.dataio.commons.types.JavaScript;
import dk.dbc.dataio.commons.utils.service.Base64Util;
import dk.dbc.dataio.jobprocessor.javascript.JSWrapperSingleScript;
import dk.dbc.dataio.jobprocessor.javascript.StringSourceSchemeHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * This class implements a LRU Flow cache with a simple get(key), put(key, flow) and containsKey(key)
 * API.
 */
public class FlowCache {
    private static final Logger LOGGER = LoggerFactory.getLogger(FlowCache.class);
    private static final int CACHE_MAX_ENTRIES = 5;

    // A LRU cache using a LinkedHashMap with access-ordering
    private final LinkedHashMap<String, FlowCacheEntry> flowCache;

    public FlowCache() {
        flowCache = new LinkedHashMap(CACHE_MAX_ENTRIES + 1, .75F, true) {
            @Override
            public boolean removeEldestEntry(Map.Entry eldest) {
                return size() > CACHE_MAX_ENTRIES;
            }};
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
        return flowCache.get(key);
    }

    /**
     * Creates script environment for the given flow and associates it with the specified key in this cache.
     * If this cache previously contained a mapping for the key, the old value is replaced by the new entry.
     * @param key key with which the create script environment is to be associated in this cache
     * @param flow flow from which a scripting environment is created
     * @return script environment as FlowCacheEntry
     * @throws IllegalStateException if given flow contains no script
     * @throws Throwable on general script environment creation failure
     */
    public FlowCacheEntry put(String key, Flow flow) throws Throwable {
        LOGGER.info("Setting up javascript environments for flow '{}'", flow.getContent().getName());
        final FlowCacheEntry cacheEntry = new FlowCacheEntry();
        for (FlowComponent flowComponent : flow.getContent().getComponents()) {
            cacheEntry.scripts.add(createWrappedScript(flowComponent.getContent()));
            final FlowComponentContent flowComponentNextContent = flowComponent.getNext();
            if (flowComponentNextContent != FlowComponent.UNDEFINED_NEXT) {
                cacheEntry.next.add(createWrappedScript(flowComponentNextContent));
            }
        }
        if (cacheEntry.scripts.isEmpty()) {
            throw new IllegalStateException(String.format("No javascript found in flow '%s'", flow.getContent().getName()));
        }
        flowCache.put(key, cacheEntry);
        return cacheEntry;
    }

    private JSWrapperSingleScript createWrappedScript(FlowComponentContent componentContent) throws Throwable {
        final List<JavaScript> javaScriptsBase64 = componentContent.getJavascripts();
        final List<StringSourceSchemeHandler.Script> javaScripts = new ArrayList<>(javaScriptsBase64.size());
        for (JavaScript javascriptBase64 : javaScriptsBase64) {
            javaScripts.add(new StringSourceSchemeHandler.Script(javascriptBase64.getModuleName(),
                    Base64Util.base64decode(javascriptBase64.getJavascript())));
        }
        String requireCacheJson = null;
        if (componentContent.getRequireCache() != null) {
            requireCacheJson = Base64Util.base64decode(componentContent.getRequireCache());
        }
        return new JSWrapperSingleScript(componentContent.getName(), componentContent.getInvocationMethod(),
                javaScripts, requireCacheJson);
    }

    /**
     * FlowCache entry abstraction giving direct access to 'script' and 'next' environments.
     */
    public static class FlowCacheEntry {
        public List<JSWrapperSingleScript> scripts = new ArrayList<>();
        public List<JSWrapperSingleScript> next = new ArrayList<>();
    }
}
