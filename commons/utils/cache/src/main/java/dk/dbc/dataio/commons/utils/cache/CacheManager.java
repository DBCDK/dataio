package dk.dbc.dataio.commons.utils.cache;

import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Factory for {@link Cache}s
 */
public final class CacheManager {
    private CacheManager() {
    }

    /**
     * Creates new LRU cache instance
     *
     * @param maximumNumberOfEntries maximum number of entries in cache at any given time
     * @param <K>                    type of cache keys
     * @param <V>                    type of cache values
     * @return new LRUCache instance
     * @throws IllegalArgumentException if given maximumNumberOfEntries is less than zero
     */
    public static <K, V> Cache<K, V> createLRUCache(int maximumNumberOfEntries)
            throws IllegalArgumentException {
        return new LRUCache<>(maximumNumberOfEntries);
    }

    /**
     * Creates an unbound cache with manually loaded cache (using put)
     *
     * @param <K> type of cache keys
     * @param <V> type of cache values
     * @return new UnboundCache instance
     */
    public static <K, V> Cache<K, V> createUnboundCache() {
        return new UnboundCache<>();
    }

    /**
     * Creates an unbound cache with automatically loaded cache (using supplied functional interfaces)
     *
     * @param getKey    A Function for getting the key of an entry
     * @param fetchData A Supplier for loading a list of entries in the cache
     * @param <K>       type of cache keys
     * @param <V>       type of cache values
     * @return new UnboundCache instance
     */
    public static <K, V> Cache<K, V> createUnboundCache(Function<V, K> getKey, Supplier<List<V>> fetchData) {
        return new UnboundCache<>(getKey, fetchData);
    }

}
