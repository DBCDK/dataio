/*
 * DataIO - Data IO
 * Copyright (C) 2015 Dansk Bibliotekscenter a/s, Tempovej 7-11, DK-2750 Ballerup,
 * Denmark. CVR: 15149043
 *
 * This file is part of DataIO.
 *
 * DataIO is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * DataIO is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with DataIO.  If not, see <http://www.gnu.org/licenses/>.
 */

package dk.dbc.dataio.jobprocessor.util;

import dk.dbc.dataio.commons.time.StopWatch;
import dk.dbc.dataio.commons.types.Flow;
import dk.dbc.dataio.commons.types.FlowComponent;
import dk.dbc.dataio.commons.types.FlowComponentContent;
import dk.dbc.dataio.commons.types.JavaScript;
import dk.dbc.dataio.commons.utils.lang.StringUtil;
import dk.dbc.dataio.jobprocessor.javascript.Script;
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
    public static final int CACHE_MAX_ENTRIES = 50;
    private static final Logger LOGGER = LoggerFactory.getLogger(FlowCache.class);

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
        LOGGER.info("Setting up javascript environment for flow '{}'", flow.getContent().getName());
        final StopWatch stopWatch = new StopWatch();
        try {
            final FlowCacheEntry cacheEntry = new FlowCacheEntry();
            for (FlowComponent flowComponent : flow.getContent().getComponents()) {
                cacheEntry.scripts.add(createScript(flowComponent.getContent()));
                final FlowComponentContent flowComponentNextContent = flowComponent.getNext();
                if (flowComponentNextContent != FlowComponent.UNDEFINED_NEXT) {
                    cacheEntry.next.add(createScript(flowComponentNextContent));
                }
            }
            if (cacheEntry.scripts.isEmpty()) {
                throw new IllegalStateException(String.format("No javascript found in flow '%s'", flow.getContent().getName()));
            }
            flowCache.put(key, cacheEntry);
            return cacheEntry;
        } finally {
            LOGGER.info("Setting up javascript environment for flow '{}' took {} ms",
                    flow.getContent().getName(), stopWatch.getElapsedTime());
        }
    }

    private static Script createScript(FlowComponentContent componentContent) throws Throwable {
        final StopWatch stopWatch = new StopWatch();
        try {
            final List<JavaScript> javaScriptsBase64 = componentContent.getJavascripts();
            final List<StringSourceSchemeHandler.Script> javaScripts = new ArrayList<>(javaScriptsBase64.size());
            for (JavaScript javascriptBase64 : javaScriptsBase64) {
                javaScripts.add(new StringSourceSchemeHandler.Script(javascriptBase64.getModuleName(),
                        StringUtil.base64decode(javascriptBase64.getJavascript())));
            }
            String requireCacheJson = null;
            if (componentContent.getRequireCache() != null) {
                requireCacheJson = StringUtil.base64decode(componentContent.getRequireCache());
            }
            return new Script(componentContent.getName(), componentContent.getInvocationMethod(),
                    javaScripts, requireCacheJson);
        } finally {
            LOGGER.info("Creating javascript for flow component '{}' took {} ms",
                    componentContent.getName(), stopWatch.getElapsedTime());
        }
    }

    /**
     * FlowCache entry abstraction giving direct access to 'script' and 'next' environments.
     */
    public static class FlowCacheEntry {
        public List<Script> scripts = new ArrayList<>();
        public List<Script> next = new ArrayList<>();
    }
}
