package dk.dbc.dataio.jobprocessor.util;

import org.junit.Ignore;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

public class FlowCacheTest {
    @Test
    public void keyContains_keyNotInCache_returnsFalse() {
        final FlowCache cache = new FlowCache();
        assertThat(cache.containsKey("no-such-key"), is(false));
    }

    @Ignore
    @Test
    public void keyContains_keyInCache_returnsTrue() {
        final FlowCache cache = new FlowCache();
        assertThat(cache.containsKey("key-exists"), is(true));
    }

    @Test
    public void get_keyNotInCache_returnsNull() {
        final FlowCache cache = new FlowCache();
        assertThat(cache.get("no-such-key"), is(nullValue()));
    }

    @Ignore
    @Test
    public void get_keyInCache_returnsEntry() {
        final FlowCache cache = new FlowCache();
        assertThat(cache.get("key-exists"), is(notNullValue()));
    }


}