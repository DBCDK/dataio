package dk.dbc.dataio.sink.diff;

import dk.dbc.dataio.commons.types.exceptions.InvalidMessageException;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class PlainTextDiffGeneratorTest extends AbstractDiffGeneratorTest {
    private static final byte[] DOC1 = (
            "first\n" +
                    "second\n" +
                    "third\n").getBytes(StandardCharsets.UTF_8);

    private static final byte[] DOC2 = (
            "first\n" +
                    "second\n" +
                    "second and ½\n" +
                    "third\n").getBytes(StandardCharsets.UTF_8);

    private final ExternalToolDiffGenerator diffGenerator = newExternalToolDiffGenerator();

    @Test
    public void equality() throws DiffGeneratorException, InvalidMessageException {
        if (canDiff()) {
            String diff = diffGenerator.getDiff(ExternalToolDiffGenerator.Kind.PLAINTEXT, DOC1, DOC1);
            assertThat(diff, is(""));
        }
    }

    @Test
    public void diff() throws DiffGeneratorException, InvalidMessageException {
        if (canDiff()) {
            String diff = diffGenerator.getDiff(ExternalToolDiffGenerator.Kind.PLAINTEXT, DOC1, DOC2);
            assertThat(diff, containsString("+second and ½"));
        }
    }
}
