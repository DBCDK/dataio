package dk.dbc.dataio.jobstore.service.util;

import dk.dbc.dataio.commons.types.Constants;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;

public class JsonValueTemplateEngineTest {
    final JsonValueTemplateEngine templateEngine = new JsonValueTemplateEngine();
    final String template = "key=${ %s }";
    final String expectedEmptyScalarOutput = "key=";
    final String expectedScalarOutput = "key=text";
    final String expectedArrayOutput = "key=text1\ntext2";
    final String expectedOverwriteOutput = "key=overwriteText";
    final Map<String, String> overwrites = new HashMap<>();

    @Test
    public void apply_templateArgIsNull_throws() {
        try {
            templateEngine.apply(null, "{}");
            fail("No NullPointerException thrown");
        } catch (NullPointerException e) {

        }
    }

    @Test
    public void apply_templateArgIsEmpty() {
        assertThat(templateEngine.apply("", "{}"), is(""));
    }

    @Test
    public void apply_jsonArgIsNull_throws() {
        try {
            templateEngine.apply(template, null);
            fail("No NullPointerException thrown");
        } catch (IllegalArgumentException e) {
        }
    }

    @Test
    public void apply_jsonArgIsEmpty_throws() {
        try {
            templateEngine.apply(template, " ");
            fail("No IllegalArgumentException thrown");
        } catch (IllegalArgumentException e) {
        }
    }

    @Test
    public void apply_jsonArgIsInvalidJson_throws() {
        try {
            templateEngine.apply(template, "<>");
            fail("No IllegalArgumentException thrown");
        } catch (IllegalArgumentException e) {
        }
    }

    @Test
    public void apply_templateWithoutProperties() {
        final String json = "{\"field\": \"text\"}";
        final String expectedOutput = "unchanged";
        final String output = templateEngine.apply(expectedOutput, json);
        assertThat(output, is(expectedOutput));
    }

    @Test
    public void apply_templateWithEmptyProperty() {
        final String path = " ";
        final String json = "{\"field\": \"text\"}";
        final String output = templateEngine.apply(String.format(template, path), json);
        assertThat(output, is(expectedEmptyScalarOutput));
    }

    @Test
    public void apply_templateWithEmptyPathProperty() {
        final String path = "...";
        final String json = "{\"field\": \"text\"}";
        final String output = templateEngine.apply(String.format(template, path), json);
        assertThat(output, is(expectedEmptyScalarOutput));
    }

    @Test
    public void apply_templateWithPropertySelectingValueAtZeroDepth() {
        final String path = "field";
        final String json = "{\"field\": \"text\"}";
        final String output = templateEngine.apply(String.format(template, path), json);
        assertThat(output, is(expectedScalarOutput));
    }

    @Test
    public void apply_templateWithPropertyOverwritesValueAtZeroDepth() {
        final String path = "field";
        final String json = "{\"field\": \"text\"}";
        overwrites.put("field", "overwriteText");
        final String output = templateEngine.apply(String.format(template, path), json, overwrites);
        assertThat(output, is(expectedOverwriteOutput));
    }

    @Test
    public void apply_templateWithPropertySelectingValueAtDepth() {
        final String path = "super.sub.field";
        final String json = "{\"super\": {\"sub\": {\"field\": \"text\"}}}";
        final String output = templateEngine.apply(String.format(template, path), json);
        assertThat(output, is(expectedScalarOutput));
    }

    @Test
    public void apply_templateWithPropertyOverwritesValueAtDepth() {
        final String path = "super.sub.field";
        final String json = "{\"super\": {\"sub\": {\"field\": \"text\"}}}";
        overwrites.put("super.sub.field", "overwriteText");
        final String output = templateEngine.apply(String.format(template, path), json, overwrites);
        assertThat(output, is(expectedOverwriteOutput));
    }

    @Test
    public void apply_templateWithNonMatchingProperty() {
        final String path = "super.sub.field";
        final String json = "{\"field\": \"text\"}";
        final String output = templateEngine.apply(String.format(template, path), json);
        assertThat(output, is(expectedEmptyScalarOutput));
    }

    @Test
    public void apply_templateWithPropertySelectingArrayOfSimpleValues() {
        final String path = "field";
        final String json = "{\"field\": [\"text1\", \"text2\"]}";
        final String output = templateEngine.apply(String.format(template, path), json);
        assertThat(output, is(expectedArrayOutput));
    }

    @Test
    public void apply_templateWithPropertySelectingArrayOfComplexValues() {
        final String path = "field.value";
        final String json = "{\"field\": [{\"value\": \"text1\"}, {\"value\": \"text2\"}]}";
        final String output = templateEngine.apply(String.format(template, path), json);
        assertThat(output, is(expectedArrayOutput));
    }

    @Test
    public void apply_templateWithPropertySelectingValueWithMissingFieldValueConstant() {
        final String path = "field";
        final String json = "{\"field\": \"" + Constants.MISSING_FIELD_VALUE + "\"}";
        final String output = templateEngine.apply(String.format(template, path), json);
        assertThat(output, is(expectedEmptyScalarOutput));
    }

    @Test
    public void apply_templateWithMultipleProperties() {
        final String template = "scalar=${fieldScalar}\narray=${fieldArray.value}";
        final String json = "{\"fieldScalar\": \"text\", \"fieldArray\": [{\"value\": \"text1\"}, {\"value\": \"" + Constants.MISSING_FIELD_VALUE + "\"}, {\"value\": \"text2\"}]}";
        final String output = templateEngine.apply(template, json);
        assertThat(output, is("scalar=text\narray=text1\n\ntext2"));
    }

    @Test
    public void apply_jsonValueContainsTemplateSpecialCharacters() {
        final String template = "scalar=${field}";
        final String json = "{\"field\": \"${text}\"}";
        final String output = templateEngine.apply(template, json);
        assertThat(output, is("scalar=${text}"));
    }

    @Test
    public void apply_templateContainsDateMacro() {
        final long dateTime = 1442554927498L;
        final String dateString = "Fri Sep 18 07:42:07 CEST 2015";

        final String template = "dateMacro=__DATE__{field}";
        final String json = "{\"field\": " + dateTime + "}";
        final String output = templateEngine.apply(template, json);
        assertThat(output, is("dateMacro=" + dateString));
    }

    @Test
    public void apply_templateContainsDateMacroSelectingNonNumberField() {
        final String template = "dateMacro=__DATE__{field}";
        final String json = "{\"field\": \"NaN\"}";
        final String output = templateEngine.apply(template, json);
        assertThat(output, is("dateMacro="));
    }

    @Test
    public void apply_templateContainsSumMacro() {
        final String template = "sumMacro=__SUM__{field1, field2}";
        final String json = "{\"field1\": " + 42 + ", \"field2\": " + 8 + "}";
        final String output = templateEngine.apply(template, json);
        assertThat(output, is("sumMacro=50"));
    }

    @Test
    public void apply_templateContainsSumMacroSelectingNonExistingField() {
        final String template = "sumMacro=__SUM__{field}";
        final String json = "{}";
        final String output = templateEngine.apply(template, json);
        assertThat(output, is("sumMacro="));
    }

    @Test
    public void apply_templateContainsSumMacroSelectingNonNumberField() {
        final String template = "sumMacro=__SUM__{field}";
        final String json = "{\"field\": \"NaN\"}";
        final String output = templateEngine.apply(template, json);
        assertThat(output, is("sumMacro="));
    }

    @Test
    public void apply_templateWithPropertySelectingInteger() {
        final String template = "integer=${field}";
        final String json = "{\"field\": 42}";
        final String output = templateEngine.apply(template, json);
        assertThat(output, is("integer=42"));
    }

    @Test
    public void apply_templateWithPropertySelectingDouble() {
        final String template = "double=${field}";
        final String json = "{\"field\": 3.14}";
        final String output = templateEngine.apply(template, json);
        assertThat(output, is("double=3.14"));
    }
}
