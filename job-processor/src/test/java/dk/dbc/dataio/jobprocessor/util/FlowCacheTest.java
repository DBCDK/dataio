package dk.dbc.dataio.jobprocessor.util;

import dk.dbc.dataio.commons.types.Flow;
import dk.dbc.dataio.commons.types.FlowComponent;
import dk.dbc.dataio.commons.utils.test.model.FlowBuilder;
import dk.dbc.dataio.commons.utils.test.model.FlowComponentBuilder;
import dk.dbc.dataio.commons.utils.test.model.FlowContentBuilder;
import dk.dbc.dataio.jobprocessor.ejb.ChunkProcessorBeanTest;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;

public class FlowCacheTest {
    private static final String KEY_FOUND = "found";
    private static final String KEY_NOT_FOUND = "not-found";
    private static final Flow FLOW;
    private static final FlowComponent FLOW_COMPONENT;

    static {
        try {
            FLOW_COMPONENT = new FlowComponentBuilder()
                    .setContent(ChunkProcessorBeanTest.getFlowComponentContent(
                            new ChunkProcessorBeanTest.ScriptWrapper("test", ChunkProcessorBeanTest.getJavaScript(
                                    ChunkProcessorBeanTest.getJavaScriptReturnUpperCaseFunction()))))
                    .build();
            FLOW = new FlowBuilder()
                    .setContent(new FlowContentBuilder()
                            .setComponents(Collections.singletonList(FLOW_COMPONENT))
                            .build())
                    .build();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private FlowCache cache;

    @Before
    public void newCache() {
        cache = new FlowCache();
    }

    @Test
    public void put_flowArgContainsNoComponents_throws() throws Throwable {
        final Flow emptyFlow = new FlowBuilder()
                .setContent(new FlowContentBuilder()
                        .setComponents(Collections.<FlowComponent>emptyList())
                        .build())
                .build();
        try {
            cache.put(KEY_FOUND, emptyFlow);
            fail("No exception thrown");
        } catch (IllegalStateException e) {
        }
    }

    @Test
    public void put_flowArgHasNoNextComponents_addsAndReturnsCacheEntry() throws Throwable {
        final FlowCache.FlowCacheEntry entry = cache.put(KEY_FOUND, FLOW);
        assertThat("entry", entry, is(notNullValue()));
        assertThat("entry has scripts", entry.scripts.isEmpty(), is(false));
        assertThat("entry has no next", entry.next.isEmpty(), is(true));
    }

    @Test
    public void put_flowArgHasNextComponents_addsAndReturnsCacheEntry() throws Throwable {
        final FlowComponent flowComponent = new FlowComponentBuilder()
                .setContent(ChunkProcessorBeanTest.getFlowComponentContent(
                        new ChunkProcessorBeanTest.ScriptWrapper("test", ChunkProcessorBeanTest.getJavaScript(
                                ChunkProcessorBeanTest.getJavaScriptReturnUpperCaseFunction()))))
                .setNext(ChunkProcessorBeanTest.getFlowComponentContent(
                        new ChunkProcessorBeanTest.ScriptWrapper("test", ChunkProcessorBeanTest.getJavaScript(
                                ChunkProcessorBeanTest.getJavaScriptReturnUpperCaseFunction()))))
                .build();
        final Flow flow = new FlowBuilder()
                .setContent(new FlowContentBuilder()
                        .setComponents(Collections.singletonList(flowComponent))
                        .build())
                .build();
        final FlowCache.FlowCacheEntry entry = cache.put(KEY_FOUND, flow);
        assertThat("entry", entry, is(notNullValue()));
        assertThat("entry has scripts", entry.scripts.isEmpty(), is(false));
        assertThat("entry has next", entry.next.isEmpty(), is(false));
    }

    @Test
    public void cacheSchemeIsLRU() throws Throwable {
        for (int i = 0; i <= FlowCache.CACHE_MAX_ENTRIES; i++) {
            cache.put(String.valueOf(i), FLOW);
        }
        assertThat("Cache contains key 0", cache.containsKey("0"), is(false));
        for (int i = 1; i <= FlowCache.CACHE_MAX_ENTRIES; i++) {
            assertThat("Cache contains key " + i, cache.containsKey(String.valueOf(i)), is(true));
        }
        cache.get("1");  // manipulate access order
        cache.put(String.valueOf(FlowCache.CACHE_MAX_ENTRIES + 1), FLOW);
        assertThat("Cache contains key 1", cache.containsKey("1"), is(true));
        assertThat("Cache contains key 2", cache.containsKey("2"), is(false));
    }

    @Test
    public void keyContains_keyNotInCache_returnsFalse() {
        assertThat(cache.containsKey(KEY_NOT_FOUND), is(false));
    }

    @Test
    public void keyContains_keyInCache_returnsTrue() throws Throwable {
        cache.put(KEY_FOUND, FLOW);
        assertThat(cache.containsKey(KEY_FOUND), is(true));
    }

    @Test
    public void get_keyNotInCache_returnsNull() {
        assertThat(cache.get(KEY_NOT_FOUND), is(nullValue()));
    }

    @Test
    public void get_keyInCache_returnsEntry() throws Throwable {
        cache.put(KEY_FOUND, FLOW);
        assertThat(cache.get(KEY_FOUND), is(notNullValue()));
    }
}
