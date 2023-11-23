package dk.dbc.dataio.commons.utils.cache;


import dk.dbc.invariant.InvariantUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * An unbound cache class using a HashMap
 * <p>
 * Apart from manually loading the cache by using put(k,v),
 * the class can load the cache automatically when needed (when the cache has been cleared).
 * This is done by supplying two functional interfaces for loading the cache:
 * </p>
 * <p>
 * This class is not thread safe.
 * </p>
 * <ul>
 *     <li>Function: getKey - for getting the key of an entry</li>
 *     <li>Supplier: fetchData - for loading a list of entries in the cache</li>
 * </ul>
 *
 * @param <K> Type for the Key og the Hashmap
 * @param <V> Type for the Value of each entry in the Hashmap
 */
public class UnboundCache<K, V> implements Cache<K, V> {
    private static final Logger log = LoggerFactory.getLogger(UnboundCache.class);
    private final Function<V, K> getKey;
    private final Supplier<List<V>> fetchData;
    private Map<K, V> map;

    /**
     * Constructor accepting a key-fetcher and a data-loader for automatically loading the cache when needed
     *
     * @param getKey    A Function for getting the key of an entry
     * @param fetchData A Supplier for loading a list of entries in the cache
     */
    public UnboundCache(Function<V, K> getKey, Supplier<List<V>> fetchData) {
        this.getKey = getKey;
        this.fetchData = fetchData;
        this.map = null;
    }

    /**
     * Constructor intended for manually loading the cache
     */
    public UnboundCache() {
        this(null, null);
    }

    /**
     * Get a value from the cache
     *
     * @param key the key whose associated value is to be returned
     * @return The entry from the cache
     */
    @Override
    public V get(K key) {
        InvariantUtil.checkNotNullOrThrow(key, "key");
        if (map == null) {
            loadCache();
        }
        return map.get(key);
    }

    /**
     * Test whether a key exist in the cache
     *
     * @param key key whose presence in this cache is to be tested.
     * @return True if present, false if not
     */
    @Override
    public boolean containsKey(K key) {
        InvariantUtil.checkNotNullOrThrow(key, "key");
        if (map == null) {
            loadCache();
        }
        return map.containsKey(key);
    }

    /**
     * Puts an entry into the cache
     *
     * @param key   key with which the specified value is to be associated
     * @param value value to be associated with the specified key
     */
    @Override
    public void put(K key, V value) {
        InvariantUtil.checkNotNullOrThrow(key, "key");
        InvariantUtil.checkNotNullOrThrow(value, "value");
        if (map == null) {
            map = new HashMap<>();
        }
        map.put(key, value);
    }

    /**
     * Clears the cache
     */
    @Override
    public void clear() {
        log.trace("clear();");
        map = null;
    }

    /**
     * Loads the cache, using the functional interfaces given in the constructor if present,
     * if not - nothing is loaded
     */
    private void loadCache() {
        if (map != null) {
            log.trace("loadCache() - already loaded");
        } else if (getKey == null || fetchData == null) {
            log.trace("loadCache() - could not load (functional interfaces not setup)");
        } else {
            log.trace("loadCache() - loading");
            map = new HashMap<>();
            List<V> data = fetchData.get();
            if (data != null) {
                for (V entry : data) {
                    map.put(getKey.apply(entry), entry);
                }
            }
        }
    }

}
