package dk.dbc.dataio.flowstore.util.json;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;

import java.io.IOException;
import java.io.StringWriter;

/**
 * This utility class provides convenience methods for JSON document handling.
 */
public class JsonUtil {
    private JsonUtil() { }

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

    public static String toJson(Object object) throws JsonException {
        final StringWriter stringWriter = new StringWriter();
        final ObjectMapper objectMapper = new ObjectMapper();
        try {
            objectMapper.writeValue(stringWriter, object);
        } catch (IOException e) {
            throw new JsonException(String.format("Exception caught when trying to marshall %s object to JSON", object.getClass().getName()), e);
        }
        return stringWriter.toString();
    }
}
