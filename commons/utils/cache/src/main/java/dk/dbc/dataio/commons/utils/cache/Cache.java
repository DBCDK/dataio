package dk.dbc.dataio.commons.utils.cache;

/**
 * A {@link Cache} is a Map-like data structure that provides temporary storage
 * of application data.
 * <p>
 * A simple example of how to use a cache is:
 * <pre><code>
 * Cache&lt;Integer, Date&gt; cache = CacheManager.createLRUCache(Integer.class, Date.class, maxEntries);
 * Date value1 = new Date();
 * Integer key = 1;
 * cache.put(key, value1);
 * Date value2 = cache.get(key);
 * </code></pre>
 *
 * @param <K> the type of key
 * @param <V> the type of value
 */
public interface Cache<K, V> {
    /**
     * Gets an entry from the cache.
     *
     * @param key the key whose associated value is to be returned
     * @return the element, or null, if it does not exist.
     * @throws NullPointerException if the key is null
     */
    V get(K key);

    /**
     * Determines if the {@link Cache} contains an entry for the specified key.
     * <p>
     * More formally, returns true if and only if this cache contains a
     * mapping for a key k such that key.equals(k).
     * (There can be at most one such mapping.)</p>
     *
     * @param key key whose presence in this cache is to be tested.
     * @return true if this map contains a mapping for the specified key
     * @throws NullPointerException if key is null
     */
    boolean containsKey(K key);

    /**
     * Associates the specified value with the specified key in the cache.
     * <p>
     * If the {@link Cache} previously contained a mapping for the key, the old
     * value is replaced by the specified value.  (A cache c is said to
     * contain a mapping for a key k if and only if {@link
     * #containsKey(Object) c.containsKey(k)} would return true.)
     *
     * @param key   key with which the specified value is to be associated
     * @param value value to be associated with the specified key
     * @throws NullPointerException if key is null or if value is null
     */
    void put(K key, V value);

    /**
     * Clears the contents of the cache
     */
    void clear();
}
