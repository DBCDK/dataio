package dk.dbc.dataio.flowstore.util.json;

import org.codehaus.jackson.JsonNode;
import org.junit.Test;
import static org.junit.Assert.assertThat;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;

/**
 * JsonUtil unit tests
 * <p>
 * The test methods of this class uses the following naming convention:
 *
 *  unitOfWork_stateUnderTest_expectedBehavior
 */
public class JsonUtilTest {
    private final String simpleObjectKey = "key";
    private final String simpleObjectValue = "value";
    private final String validJsonSimpleObjectString = String.format("{\"%s\": \"%s\"}", simpleObjectKey, simpleObjectValue);
    private final int simpleArraySize = 4;
    private final int simpleArrayValue = 0;
    private final String validJsonSimpleArrayString = String.format("[%d, 1, 2, 3]", simpleArrayValue);
    private final String errMessage = "message";

    @Test(expected = JsonException.class)
    public void getJsonRoot_jsonArgIsNull_throws() throws Exception {
        JsonUtil.getJsonRoot(null);
    }

    @Test(expected = JsonException.class)
    public void getJsonRoot_jsonArgIsEmpty_throws() throws Exception {
        JsonUtil.getJsonRoot("");
    }

    @Test(expected = JsonException.class)
    public void getJsonRoot_jsonArgIsInvalidJson_throws() throws Exception {
        JsonUtil.getJsonRoot("<not_json/>");
    }

    @Test
    public void getJsonRoot_jsonArgIsValidJsonObject_returnsJsonNode() throws Exception {
        final JsonNode root = JsonUtil.getJsonRoot(validJsonSimpleObjectString);
        assertThat(root, is(notNullValue()));
        assertThat(root.path(simpleObjectKey).getTextValue(), is(simpleObjectValue));
    }

    @Test
    public void getJsonRoot_jsonArgIsValidJsonArray_returnsJsonNode() throws Exception {
        final JsonNode root = JsonUtil.getJsonRoot(validJsonSimpleArrayString);
        assertThat(root, is(notNullValue()));
        assertThat(root.size(), is(simpleArraySize));
        assertThat(root.get(0).getIntValue(), is(simpleArrayValue));
    }

    @Test(expected = JsonException.class)
    public void getNonEmptyTextValueOrThrow_jsonNodeArgIsNull_throws() throws Exception {
        JsonUtil.getNonEmptyTextValueOrThrow(null, errMessage);
    }

    @Test(expected = JsonException.class)
    public void getNonEmptyTextValueOrThrow_jsonNodeArgRepresentsMissingNode_throws() throws Exception {
        final JsonNode root = JsonUtil.getJsonRoot(validJsonSimpleObjectString);
        JsonUtil.getNonEmptyTextValueOrThrow(root.path("non-existing"), errMessage);
    }

    @Test(expected = JsonException.class)
    public void getNonEmptyTextValueOrThrow_jsonNodeArgIsNonTextual_throws() throws Exception {
        final JsonNode root = JsonUtil.getJsonRoot(validJsonSimpleObjectString);
        JsonUtil.getNonEmptyTextValueOrThrow(root, errMessage);
    }

    @Test(expected = JsonException.class)
    public void getNonEmptyTextValueOrThrow_jsonNodeArgHasNullTextValue_throws() throws Exception {
        final String objectString = String.format("{\"%s\": null}", simpleObjectKey);
        final JsonNode root = JsonUtil.getJsonRoot(objectString);
        JsonUtil.getNonEmptyTextValueOrThrow(root.path(simpleObjectKey), errMessage);
    }

    @Test(expected = JsonException.class)
    public void getNonEmptyTextValueOrThrow_jsonNodeArgHasEmptyTextValue_throws() throws Exception {
        final String objectString = String.format("{\"%s\": \"\"}", simpleObjectKey);
        final JsonNode root = JsonUtil.getJsonRoot(objectString);
        JsonUtil.getNonEmptyTextValueOrThrow(root.path(simpleObjectKey), errMessage);
    }

    @Test
    public void getNonEmptyTextValueOrThrow_jsonNodeArgHasNonEmptyTextValue_returnsTextValue() throws Exception {
        final JsonNode root = JsonUtil.getJsonRoot(validJsonSimpleObjectString);
        assertThat(JsonUtil.getNonEmptyTextValueOrThrow(root.path(simpleObjectKey), errMessage), is(simpleObjectValue));
    }

    @Test(expected = JsonException.class)
    public void getLongValueOrThrow_jsonNodeArgIsNull_throws() throws Exception {
        JsonUtil.getLongValueOrThrow(null, errMessage);
    }

    @Test(expected = JsonException.class)
    public void getLongValueOrThrow_jsonNodeArgRepresentsMissingNode_throws() throws Exception {
        final JsonNode root = JsonUtil.getJsonRoot(validJsonSimpleObjectString);
        JsonUtil.getLongValueOrThrow(root.path("non-existing"), errMessage);
    }

    @Test(expected = JsonException.class)
    public void getLongValueOrThrow_jsonNodeArgIsNotALong_throws() throws Exception {
        final JsonNode root = JsonUtil.getJsonRoot(validJsonSimpleObjectString);
        JsonUtil.getLongValueOrThrow(root.path(simpleObjectKey), errMessage);
    }

    @Test(expected = JsonException.class)
    public void getLongValueOrThrow_jsonNodeArgHasNullLongValue_throws() throws Exception {
        final String objectString = String.format("{\"%s\": null}", simpleObjectKey);
        final JsonNode root = JsonUtil.getJsonRoot(objectString);
        JsonUtil.getLongValueOrThrow(root.path(simpleObjectKey), errMessage);
    }

    @Test
    public void getLongValueOrThrow_jsonNodeArgHasLongValue_returnsLongValue() throws Exception {
        final long expectedValue = 42;
        final String objectString = String.format("{\"%s\": %d}", simpleObjectKey, expectedValue);
        final JsonNode root = JsonUtil.getJsonRoot(objectString);
        final long value = JsonUtil.getLongValueOrThrow(root.path(simpleObjectKey), errMessage);
        assertThat(value, is(expectedValue));
    }
}
