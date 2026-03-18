package dk.dbc.dataio.commons.retriever.connector.model;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.core.type.TypeReference;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * This class provides a dynamic property container that allows arbitrary fields to be stored
 * and retrieved without requiring a predefined schema. It uses Jackson annotations to support
 * JSON serialization and deserialization of unknown properties, making it suitable for handling
 * article data with varying or unknown structures.
 * <p>
 * All fields are stored in an internal map and can be accessed via type-safe getter methods.
 * The class supports both simple type casting and complex type references for retrieval.
 * <p>
 * Thread-safety: This class is mutable and not thread-safe. External synchronization may be
 * required if instances are accessed concurrently by multiple threads.
 */
public class Article {
    private final Map<String, Object> fields = new LinkedHashMap<>();

    @JsonAnySetter
    public void set(String name, Object value) {
        fields.put(name, value);
    }

    @JsonAnyGetter
    public Map<String, Object> getFields() {
        return fields;
    }

    /**
     * Retrieves a field value from this Article's internal field map and casts it to the specified type.
     *
     * @param <T> the type to cast the field value to
     * @param name the name of the field to retrieve
     * @param type the Class object representing the target type for casting
     * @return the field value cast to the specified type, or null if the field does not exist
     * @throws ClassCastException if the field value cannot be cast to the specified type
     */
    public <T> T get(String name, Class<T> type) {
        return type.cast(fields.get(name));
    }

    /**
     * Retrieves a field value from this Article's internal field map and casts it to the type
     * specified by the TypeReference parameter.
     *
     * @param <T> the type to cast the field value to
     * @param name the name of the field to retrieve
     * @param type the TypeReference object representing the target type for casting
     * @return the field value cast to the specified type, or null if the field does not exist
     * @throws ClassCastException if the field value cannot be cast to the specified type
     */
    @SuppressWarnings("unchecked")
    public <T> T get(String name, TypeReference<T> type) {
        return (T) fields.get(name);
    }
}
