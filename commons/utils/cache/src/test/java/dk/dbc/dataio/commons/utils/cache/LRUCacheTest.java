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

package dk.dbc.dataio.commons.utils.cache;

import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

public class LRUCacheTest {
    private static final Integer VALUE = 42;
    private static final String KEY = "key";
    private static final String KEY_NOT_FOUND = "no-such-key";

    private final Cache<String, Integer> cache = CacheManager.createLRUCache(3);

    @Before
    public void clearCache() {
        cache.clear();
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructor_maximumNumberOfEntriesIsZero_throws() {
        new LRUCache<String, Integer>(0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructor_maximumNumberOfEntriesIsLessThanZero_throws() {
        new LRUCache<String, Integer>(-1);
    }

    @Test(expected = NullPointerException.class)
    public void containsKey_keyArgIsNull_throws() {
        cache.containsKey(null);
    }

    @Test
    public void containsKey_notInCache_returnsFalse() {
        assertThat(cache.containsKey(KEY_NOT_FOUND), is(false));
    }

    @Test
    public void containsKey_inCache_returnsTrue() throws Throwable {
        cache.put(KEY, 42);
        assertThat(cache.containsKey(KEY), is(true));
    }

    @Test(expected = NullPointerException.class)
    public void get_keyArgIsNull_throws() {
        cache.get(null);
    }

    @Test
    public void get_notInCache_returnsNull() {
        assertThat(cache.get(KEY_NOT_FOUND), is(nullValue()));
    }

    @Test
    public void get_inCache_returnsEntry() throws Throwable {
        cache.put(KEY, VALUE);
        assertThat(cache.get(KEY), is(VALUE));
    }

    @Test(expected = NullPointerException.class)
    public void put_keyArgIsNull_throws() {
        cache.put(null, VALUE);
    }

    @Test(expected = NullPointerException.class)
    public void put_valueArgIsNull_throws() {
        cache.put(KEY, null);
    }

    @Test
    public void put_keyExists_replacesValue() {
        cache.put(KEY, 1);
        cache.put(KEY, VALUE);
        assertThat(cache.get(KEY), is(VALUE));
    }

    @Test
    public void clear() {
        cache.put(KEY, VALUE);
        cache.clear();
        assertThat(cache.containsKey(KEY), is(false));
    }

    @Test
    public void cache_whenMaximumNumberOfEntriesIsExceeded_thenLeastRecentlyUsedEntryIsDiscarded() {
        cache.put(KEY, VALUE);
        assertThat(cache.containsKey(KEY), is(true));
        cache.put(KEY + 1, VALUE);
        cache.put(KEY + 2, VALUE);
        cache.put(KEY + 3, VALUE);
        assertThat(cache.containsKey(KEY), is(false));
        assertThat(cache.containsKey(KEY + 1), is(true));
        assertThat(cache.containsKey(KEY + 2), is(true));
        assertThat(cache.containsKey(KEY + 3), is(true));
    }
}
