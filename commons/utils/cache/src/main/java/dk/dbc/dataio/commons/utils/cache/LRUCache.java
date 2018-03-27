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

import dk.dbc.invariant.InvariantUtil;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A LRU (least-recently-used) cache using a LinkedHashMap with access-ordering.
 * <p>
 * This class is not thread safe.
 * </p>
 * @param <K> the type of key
 * @param <V> the type of value
 */
public class LRUCache<K, V> implements Cache<K, V> {
    private final Map<K, V> map;

    public LRUCache(int maximumNumberOfEntries) throws IllegalArgumentException {
        if (maximumNumberOfEntries <= 0) {
            throw new IllegalArgumentException("maximumNumberOfEntries must be larger than zero");
        }
        map = new LinkedHashMap<K, V>(maximumNumberOfEntries + 1, .75F, true) {
            @Override
            public boolean removeEldestEntry(Map.Entry eldest) {
                return size() > maximumNumberOfEntries;
            }
        };
    }

    @Override
    public V get(K key) throws NullPointerException {
        InvariantUtil.checkNotNullOrThrow(key, "key");
        return map.get(key);
    }

    @Override
    public boolean containsKey(K key) throws NullPointerException {
        InvariantUtil.checkNotNullOrThrow(key, "key");
        return map.containsKey(key);
    }

    @Override
    public void put(K key, V value) throws NullPointerException {
        InvariantUtil.checkNotNullOrThrow(key, "key");
        InvariantUtil.checkNotNullOrThrow(value, "value");
        map.put(key, value);
    }

    @Override
    public void clear() {
        map.clear();
    }
}
