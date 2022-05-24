/*
 *
 *  * DataIO - Data IO
 *  * Copyright (C) 2015 Dansk Bibliotekscenter a/s, Tempovej 7-11, DK-2750 Ballerup,
 *  * Denmark. CVR: 15149043
 *  *
 *  * This file is part of DataIO.
 *  *
 *  * DataIO is free software: you can redistribute it and/or modify
 *  * it under the terms of the GNU General Public License as published by
 *  * the Free Software Foundation, either version 3 of the License, or
 *  * (at your option) any later version.
 *  *
 *  * DataIO is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  * GNU General Public License for more details.
 *  *
 *  * You should have received a copy of the GNU General Public License
 *  * along with DataIO.  If not, see <http://www.gnu.org/licenses/>.
 *
 *
 */

package dk.dbc.dataio.commons.utils.cache;

import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Test;

import javax.naming.NamingException;
import java.util.Arrays;
import java.util.Collections;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;


public class UnboundCacheTest {
    private Cache<Integer, String> cache;
    private int reloadCacheCounter;

    // Manually loaded cache

    @Test(expected = NullPointerException.class)
    public void containsKey_notLoaded_returnsFalse() {
        cache = CacheManager.createUnboundCache();
        assertThat(cache.containsKey(12), is(false));
    }

    @Test(expected = NullPointerException.class)
    public void put_keyParameterNull_throws() {
        cache = CacheManager.createUnboundCache();
        cache.put(null, "Hey");
    }

    @Test(expected = NullPointerException.class)
    public void put_valueParameterNull_throws() {
        cache = CacheManager.createUnboundCache();
        cache.put(12, null);
    }

    @Test
    public void containsKey_keyNotPresent_returnsFalse() {
        cache = CacheManager.createUnboundCache();
        cache.put(123, "Hey");
        assertThat(cache.containsKey(12), is(false));
    }

    @Test
    public void containsKey_keyPresent_returnsTrue() {
        cache = CacheManager.createUnboundCache();
        cache.put(12, "Hey");
        assertThat(cache.containsKey(12), is(true));
    }

    @Test
    public void get_keyNotPresent_returnsNull() {
        cache = CacheManager.createUnboundCache();
        cache.put(123, "Hey");
        assertThat(cache.get(12), is(nullValue()));
    }

    @Test
    public void get_keyPresent_returnsValue() {
        cache = CacheManager.createUnboundCache();
        cache.put(12, "Hey");
        assertThat(cache.get(12), is("Hey"));
    }


    // Automatically loaded cache

    @Test
    public void cache_constructor_ok() {
        CacheManager.createUnboundCache(String::length, () -> Arrays.asList("kurt", "viggo"));
    }

    @Test
    public void get_emptyCache_loadCacheOk() {
        reloadCacheCounter = 0;
        cache = CacheManager.createUnboundCache(
                String::length,
                () -> {reloadCacheCounter++; return Collections.emptyList();}
        );
        Assert.assertThat(cache.get(1), is((String) null));
        Assert.assertThat(reloadCacheCounter, CoreMatchers.is(1));

    }

    @Test
    public void containsKey_notLoaded_loadAndTest() {
        reloadCacheCounter = 0;
        cache = CacheManager.createUnboundCache(
                String::length,
                () -> {reloadCacheCounter++; return Arrays.asList("Ø", "Øl", "Basse", "Fire", "Tre");}
        );
        assertThat(cache.containsKey(1), is(true));
        assertThat(cache.containsKey(2), is(true));
        assertThat(cache.containsKey(3), is(true));
        assertThat(cache.containsKey(4), is(true));
        assertThat(cache.containsKey(5), is(true));
        assertThat(cache.containsKey(6), is(false));
        assertThat(reloadCacheCounter, CoreMatchers.is(1));
    }

    @Test
    public void get_notLoaded_loadAndGet() {
        reloadCacheCounter = 0;
        cache = CacheManager.createUnboundCache(
                String::length,
                () -> {reloadCacheCounter++; return Arrays.asList("Ø", "Øl", "Basse", "Fire", "Tre");}
        );
        assertThat(cache.get(1), is("Ø"));
        assertThat(cache.get(2), is("Øl"));
        assertThat(cache.get(3), is("Tre"));
        assertThat(cache.get(4), is("Fire"));
        assertThat(cache.get(5), is("Basse"));
        assertThat(cache.get(6), is(nullValue()));
        assertThat(reloadCacheCounter, CoreMatchers.is(1));
    }

    @Test
    public void cache_clearCache_ok() throws NamingException {
        reloadCacheCounter = 0;
        cache = CacheManager.createUnboundCache(
                String::length,
                () -> {reloadCacheCounter++; return Arrays.asList("Ø", "Øl", "Basse", "Fire", "Tre");}
        );
        assertThat(reloadCacheCounter, is(0));
        cache.get(1);
        assertThat(reloadCacheCounter, is(1));
        cache.get(2);
        assertThat(reloadCacheCounter, is(1));
        cache.clear();
        assertThat(reloadCacheCounter, is(1));
        cache.get(4);
        assertThat(reloadCacheCounter, is(2));

    }

}
