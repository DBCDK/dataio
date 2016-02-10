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

/**
 *  Factory for {@link Cache}s
 */
public final class CacheManager {
    private CacheManager() { }

    /**
     * Creates new LRU cache instance
     * @param kClass type of cache keys
     * @param vClass type of cache values
     * @param maximumNumberOfEntries maximum number of entries in cache at any given time
     * @param <K> type of cache keys
     * @param <V> type of cache values
     * @return new LRUCache instance
     * @throws IllegalArgumentException if given maximumNumberOfEntries is less than zero
     */
    public static <K, V> Cache<K, V> createLRUCache(Class<K> kClass, Class<V> vClass, int maximumNumberOfEntries)
            throws IllegalArgumentException {
        return new LRUCache<>(maximumNumberOfEntries);
    }
}
