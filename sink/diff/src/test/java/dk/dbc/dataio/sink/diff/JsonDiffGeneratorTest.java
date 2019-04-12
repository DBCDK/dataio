/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GNU GPLv3
 * See license text in LICENSE.txt
 */

package dk.dbc.dataio.sink.diff;

import org.junit.Ignore;
import org.junit.Test;

import java.nio.charset.StandardCharsets;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;

@Ignore("Since the tests are not run in a docker container" +
        " where we ca be sure that the binaries called by " +
        "the external tool exist.")
public class JsonDiffGeneratorTest extends AbstractDiffGeneratorTest {
    private static final byte[] DOC1 = (
            "{" +
              "\"name\":\"JsonDiffGeneratorTest\"," +
              "\"author\":{" +
                "\"name\":\"John Doe\"," +
                "\"mail\":\"John@Doe.com\"" +
              "}," +
              "\"dependencies\":[\"a.jar\",\"b.jar\"]" +
            "}").getBytes(StandardCharsets.UTF_8);

    private static final byte[] DOC1_PRETTY_PRINTED = (
            "{\n" +
            "  \"name\": \"JsonDiffGeneratorTest\",\n" +
            "  \"author\": {\n" +
            "    \"name\": \"John Doe\",\n" +
            "    \"mail\": \"John@Doe.com\"\n" +
            "  },\n" +
            "  \"dependencies\": [\"a.jar\", \"b.jar\"]\n" +
            "}\n").getBytes(StandardCharsets.UTF_8);

    private static final byte[] DOC1_WITH_DIFFERENT_KEY_ORDERING = (
            "{" +
              "\"name\":\"JsonDiffGeneratorTest\"," +
              "\"author\":{" +
                "\"mail\":\"John@Doe.com\"," +
                "\"name\":\"John Doe\"" +
              "}," +
              "\"dependencies\":[\"a.jar\",\"b.jar\"]" +
            "}").getBytes(StandardCharsets.UTF_8);

    private static final byte[] DOC2 = (
            "{" +
              "\"name\":\"JsonDiffGeneratorTest\"," +
              "\"author\":{" +
                "\"name\":\"Jane Doe\"," +
                "\"mail\":\"Jane@Doe.com\"" +
              "}," +
              "\"dependencies\":[\"a.jar\",\"b.jar\"]" +
            "}").getBytes(StandardCharsets.UTF_8);

    private final ExternalToolDiffGenerator diffGenerator = newExternalToolDiffGenerator();

    @Test
    public void exactEquality() throws DiffGeneratorException {
        final String diff = diffGenerator.getDiff(ExternalToolDiffGenerator.Kind.JSON,
                DOC1, DOC1);
        assertThat(diff, is(""));
    }

    @Test
    public void normalizedEquality() throws DiffGeneratorException {
        final String diff = diffGenerator.getDiff(ExternalToolDiffGenerator.Kind.JSON,
                DOC1, DOC1_PRETTY_PRINTED);
        assertThat(diff, is(""));
    }

    @Test
    public void semanticEquality() throws DiffGeneratorException {
        final String diff = diffGenerator.getDiff(ExternalToolDiffGenerator.Kind.JSON,
                DOC1, DOC1_WITH_DIFFERENT_KEY_ORDERING);
        assertThat(diff, is(""));
    }

    @Test
    public void diff() throws DiffGeneratorException {
        final String diff = diffGenerator.getDiff(ExternalToolDiffGenerator.Kind.JSON,
                DOC1, DOC2);
        assertThat(diff, containsString("-    \"mail\": \"John@Doe.com\","));
        assertThat(diff, containsString("-    \"name\": \"John Doe\""));
        assertThat(diff, containsString("+    \"mail\": \"Jane@Doe.com\","));
        assertThat(diff, containsString("+    \"name\": \"Jane Doe\""));
    }

    @Test
    public void invalidJson() {
        try {
            diffGenerator.getDiff(ExternalToolDiffGenerator.Kind.JSON,
                    DOC1, "{invalid".getBytes(StandardCharsets.UTF_8));
            fail("No DiffGeneratorException thrown");
        } catch (DiffGeneratorException e) {
        }
    }
}
