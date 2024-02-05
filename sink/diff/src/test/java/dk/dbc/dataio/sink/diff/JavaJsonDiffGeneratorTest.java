package dk.dbc.dataio.sink.diff;

import dk.dbc.dataio.commons.types.exceptions.InvalidMessageException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

public class JavaJsonDiffGeneratorTest {
    private static final DiffGenerator DIFF_GENERATOR = new JavaDiffGenerator();
    public static final byte[] DOC1 = (
            "{" +
                    "\"name\":\"JsonDiffGeneratorTest\"," +
                    "\"author\":{" +
                    "\"name\":\"John Doe\"," +
                    "\"mail\":\"John@Doe.com\"" +
                    "}," +
                    "\"dependencies\":[\"a.jar\",\"b.jar\"]" +
                    "}").getBytes(StandardCharsets.UTF_8);

    public static final byte[] DOC1_PRETTY_PRINTED = (
            "{\n" +
                    "  \"name\": \"JsonDiffGeneratorTest\",\n" +
                    "  \"author\": {\n" +
                    "    \"name\": \"John Doe\",\n" +
                    "    \"mail\": \"John@Doe.com\"\n" +
                    "  },\n" +
                    "  \"dependencies\": [\"a.jar\", \"b.jar\"]\n" +
                    "}\n").getBytes(StandardCharsets.UTF_8);

    public static final byte[] DOC1_WITH_DIFFERENT_KEY_ORDERING = (
            "{" +
                    "\"name\":\"JsonDiffGeneratorTest\"," +
                    "\"author\":{" +
                    "\"mail\":\"John@Doe.com\"," +
                    "\"name\":\"John Doe\"" +
                    "}," +
                    "\"dependencies\":[\"a.jar\",\"b.jar\"]" +
                    "}").getBytes(StandardCharsets.UTF_8);

    public static final byte[] DOC2 = (
            "{" +
                    "\"name\":\"JsonDiffGeneratorTest\"," +
                    "\"author\":{" +
                    "\"name\":\"Jane Doe\"," +
                    "\"mail\":\"Jane@Doe.com\"" +
                    "}," +
                    "\"dependencies\":[\"a.jar\",\"b.jar\"]" +
                    "}").getBytes(StandardCharsets.UTF_8);

    @Test
    public void exactEquality() throws DiffGeneratorException, InvalidMessageException {
        String diff = DIFF_GENERATOR.getDiff(Kind.JSON, DOC1, DOC1_PRETTY_PRINTED);
        Assertions.assertEquals("", diff);
    }

    @Test
    public void semanticEquality() throws DiffGeneratorException, InvalidMessageException {
        String diff = DIFF_GENERATOR.getDiff(Kind.JSON, DOC1, DOC1_WITH_DIFFERENT_KEY_ORDERING);
        Assertions.assertEquals("", diff);
    }

    @Test
    public void diff() throws DiffGeneratorException, InvalidMessageException {
        String diff = DIFF_GENERATOR.getDiff(Kind.JSON, DOC1, DOC2);
        Assertions.assertTrue(diff.contains("Jane"));
        Assertions.assertTrue(diff.contains("John"));
    }
}
