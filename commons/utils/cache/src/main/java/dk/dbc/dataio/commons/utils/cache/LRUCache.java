package dk.dbc.dataio.commons.utils.cache;

import dk.dbc.invariant.InvariantUtil;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A LRU (least-recently-used) cache using a LinkedHashMap with access-ordering.
 * <p>
 * This class is not thread safe.
 * </p>
 *
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
