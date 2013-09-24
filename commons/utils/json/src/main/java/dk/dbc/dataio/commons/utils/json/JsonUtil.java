package dk.dbc.dataio.commons.utils.json;

import dk.dbc.dataio.commons.utils.invariant.InvariantUtil;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Map;

/**
 * This utility class provides convenience methods for JSON document handling.
 */
public class JsonUtil {
    private JsonUtil() { }

    /**
     * Transforms given value type into its corresponding JSON string representation
     *
     * @param object object to transform
     * @return JSON representation
     * @throws JsonException if unable to marshall value type into its JSON representation
     */
    public static String toJson(Object object) throws JsonException {
        InvariantUtil.checkNotNullOrThrow(object, "object");
        final StringWriter stringWriter = new StringWriter();
        final ObjectMapper objectMapper = new ObjectMapper();
        try {
            objectMapper.writeValue(stringWriter, object);
        } catch (IOException e) {
            throw new JsonException(String.format("Exception caught when trying to marshall %s object to JSON", object.getClass().getName()), e);
        }
        return stringWriter.toString();
    }

    /**
     * Transforms JSON string into value type
     *
     * @param json JSON representation of value type
     * @param tClass value type class
     * @param mixIns Map of target class to mixin class. Mixin classes use annotations
     *               to guide the serialization/deserialization process, Can be null
     *               or empty.
     * @param <T> type parameter
     * @return value type instance
     * @throws JsonException if unable to unmarshall JSON representation to value type
     */
    public static <T> T fromJson(String json, Class<T> tClass, Map<Class<?>, Class<?>> mixIns) throws JsonException {
        InvariantUtil.checkNotNullNotEmptyOrThrow(json, "json");
        InvariantUtil.checkNotNullOrThrow(tClass, "tClass");
        T object;
        final ObjectMapper objectMapper = new ObjectMapper();
        try {
            if (mixIns != null) {
                for (Map.Entry<Class<?>, Class<?>> e : mixIns.entrySet()) {
                    objectMapper.getDeserializationConfig().addMixInAnnotations(e.getKey(), e.getValue());
                }
            }
            object = objectMapper.readValue(json, tClass);
        } catch (IOException e) {
            throw new JsonException(String.format("Exception caught when trying to unmarshall JSON %s to %s object", json, tClass.getName()), e);
        }
        return object;
    }

    /**
     * Provides access to a tree based view of the given JSON document similar
     * to DOM nodes in XML DOM trees
     *
     * @param json JSON document
     * @return JsonNode representing the root of the document
     * @throws JsonException when given null-valued, empty-valued or invalid JSON string
     */
    public static JsonNode getJsonRoot(String json) throws JsonException {
        if (json == null) {
            throw new JsonException("json string can not be null");
        }

        final ObjectMapper mapper = new ObjectMapper();
        JsonNode root;
        try {
            root = mapper.readTree(json);
        } catch (IOException e) {
            throw new JsonException(e);
        }
        return root;
    }

    /**
     * Retrieves text value of given document tree node
     *
     * @param jsonNode JSON node
     * @param message message to prepend to exception message in case of error
     * @return text value
     * @throws JsonException when given null-valued jsonNode, when jsonNode represents a
     * non-existing node, when jsonNode represents a non-textual node or when text value
     * is empty
     */
    public static String getNonEmptyTextValueOrThrow(JsonNode jsonNode, String message) throws JsonException {
        if (jsonNode == null) {
            throw new JsonException(String.format("%s - jsonNode was null", message));
        }
        if (jsonNode.isMissingNode()) {
            throw new JsonException(String.format("%s - member was not found", message));
        }
        if (!jsonNode.isTextual()) {
            throw new JsonException(String.format("%s - member is non-textual", message));
        }
        final String value = jsonNode.getTextValue();
        if (value.isEmpty()) {
            throw new JsonException(String.format("%s - member value was empty", message));
        }
        return value;
    }

    /**
     * Retrieves long value of given document tree node
     *
     * @param jsonNode JSON node
     * @param message message to prepend to exception message in case of error
     * @return long value
     * @throws JsonException when given null-valued jsonNode, when jsonNode represents a
     * non-existing node or when jsonNode does not represent a number node
     */
    public static long getLongValueOrThrow(JsonNode jsonNode, String message) throws JsonException {
        if (jsonNode == null) {
            throw new JsonException(String.format("%s - jsonNode was null", message));
        }
        if (jsonNode.isMissingNode()) {
            throw new JsonException(String.format("%s - member was not found", message));
        }
        if (!jsonNode.isNumber()) {
            throw new JsonException(String.format("%s - member is not a number", message));
        }
        return jsonNode.getLongValue();
    }
}
