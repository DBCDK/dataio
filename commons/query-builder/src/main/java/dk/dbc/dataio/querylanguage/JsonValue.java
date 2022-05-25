package dk.dbc.dataio.querylanguage;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Value class to be used in conjunction with the
 * {@link BiClause.Operator#JSON_LEFT_CONTAINS}
 * operator
 */
public class JsonValue {
    private final Map<String, Object> content;

    public JsonValue() {
        this.content = new HashMap<>();
    }

    /**
     * Creates property in this {@link JsonValue} and associates it with the specified value.
     * If this {@link JsonValue} previously contained a mapping for the property,
     * the old value is replaced by the specified value.
     *
     * @param property name of JSON property
     * @param value    value of JSON property
     * @return this {@link JsonValue}
     */
    public JsonValue put(String property, Object value) {
        content.put(property, value);
        return this;
    }

    /**
     * Adds value to the JSON ARRAY pointed to by property
     *
     * @param property name of JSON ARRAY property
     * @param value    value to insert into JSON array
     * @return this {@link JsonValue}
     * @throws IllegalArgumentException if property exists and is not associated
     *                                  with a JSON ARRAY value
     */
    @SuppressWarnings("unchecked")
    public JsonValue add(String property, Object value) throws IllegalArgumentException {
        final Object entry = content.get(property);
        if (entry == null) {
            final ArrayList<Object> list = new ArrayList<>();
            list.add(value);
            content.put(property, list);
        } else {
            if (!(entry instanceof List)) {
                throw new IllegalArgumentException(property + " is not an ARRAY property");
            }
            ((List) entry).add(value);
        }
        return this;
    }

    /**
     * Removes the named JSON property from this {@link JsonValue} if it exists.
     *
     * @param property name of JSON property to remove
     * @return the value which was previously associated with the property,
     * or null if this {@link JsonValue} contained no such property
     */
    public Object remove(String property) {
        return content.remove(property);
    }

    /**
     * Removes the first occurrence of value from the JSON ARRAY pointed to by property
     *
     * @param property name of JSON ARRAY property
     * @param value    value of ARRAY element to remove
     * @return true if element was removed, false if not
     * @throws IllegalArgumentException if property exists and is not associated
     *                                  with a JSON ARRAY value
     */
    public Object removeArrayElement(String property, Object value) {
        final Object entry = content.get(property);
        if (entry != null) {
            if (!(entry instanceof List)) {
                throw new IllegalArgumentException(property + " is not an ARRAY property");
            }
            return ((List) entry).remove(value);
        }
        return false;
    }

    @JsonAnyGetter
    public Map<String, Object> getContent() {
        return content;
    }

    public boolean hasProperty(String property) {
        return content.containsKey(property);
    }

    @JsonIgnore
    public boolean isEmpty() {
        return content.isEmpty();
    }
}
