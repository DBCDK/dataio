package dk.dbc.dataio.flowstore.util.json;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;

import java.io.IOException;

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
     * Provides access to text value of given document tree node
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
}
