package dk.dbc.dataio.commons.utils.json;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

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

    @Test(expected = NullPointerException.class)
    public void toJson_objectArgIsNull_throws() throws Exception {
        JsonUtil.toJson(null);
    }

    @Test(expected = JsonException.class)
    public void toJson_objectArgCanNotBeMarshalled_throws() throws Exception {
       JsonUtil.toJson(new Object());
    }

    @Test
    public void toJson_objectArgCanBeMarshalled_returnsRepresentation() throws Exception {
        final int value = 42;
        final String expectedStringRepresentation = String.format("{\"value\":%d}", value);
        final SimpleBean object = new SimpleBean();
        object.setValue(value);
        final String output = JsonUtil.toJson(object);
        assertThat(output, is(expectedStringRepresentation));
    }

    @Test(expected = NullPointerException.class)
    public void fromJson_jsonArgIsNull_throws() throws Exception {
        JsonUtil.fromJson(null, SimpleBean.class);
    }

    @Test(expected = IllegalArgumentException.class)
    public void fromJson_jsonArgIsEmpty_throws() throws Exception {
        JsonUtil.fromJson("", SimpleBean.class);
    }

    @Test(expected = NullPointerException.class)
    public void fromJson_tClassArgIsNull_throws() throws Exception {
        JsonUtil.fromJson("{}", null);
    }

    @Test(expected = JsonException.class)
    public void fromJson_jsonArgCanNotBeUnmarshalled_throws() throws Exception {
        JsonUtil.fromJson("{}", SimpleBeanWithoutDefaultConstructor.class);
    }

    @Test
    public void fromJson_jsonArgCanBeUnmarshalled_returnsInstance() throws Exception {
        final int value = 42;
        final String jsonRepresentation = String.format("{\"value\":%d}", value);
        final SimpleBean instance = JsonUtil.fromJson(jsonRepresentation, SimpleBean.class);
        assertThat(instance, is(notNullValue()));
        assertThat(instance.getValue(), is(value));
    }

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
        assertThat(root.path(simpleObjectKey).textValue(), is(simpleObjectValue));
    }

    @Test
    public void getJsonRoot_jsonArgIsValidJsonArray_returnsJsonNode() throws Exception {
        final JsonNode root = JsonUtil.getJsonRoot(validJsonSimpleArrayString);
        assertThat(root, is(notNullValue()));
        assertThat(root.size(), is(simpleArraySize));
        assertThat(root.get(0).intValue(), is(simpleArrayValue));
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

    private static class SimpleBean {
        private int value;

        public int getValue() {
            return value;
        }
        public void setValue(int value) {
            this.value = value;
        }
    }

    private static class SimpleBeanWithoutDefaultConstructor {
        private int value;

        public SimpleBeanWithoutDefaultConstructor(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }
    }

    @SuppressWarnings("unused")
    public static abstract class SimpleBeanWithoutDefaultConstructorJsonMixin {
        @JsonCreator
        public SimpleBeanWithoutDefaultConstructorJsonMixin(@JsonProperty("value") int value) { }
    }
}
