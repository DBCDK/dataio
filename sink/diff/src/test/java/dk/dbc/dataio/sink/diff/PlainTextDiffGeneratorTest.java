/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GNU GPLv3
 * See license text in LICENSE.txt
 */

package dk.dbc.dataio.sink.diff;

import org.junit.Test;

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
    public void equality() throws DiffGeneratorException {
        final String diff = diffGenerator.getDiff(ExternalToolDiffGenerator.Kind.PLAINTEXT,
                DOC1, DOC1);
        assertThat(diff, is(""));
    }

    @Test
    public void diff() throws DiffGeneratorException {
        final String diff = diffGenerator.getDiff(ExternalToolDiffGenerator.Kind.PLAINTEXT,
                DOC1, DOC2);
        assertThat(diff, containsString("+second and ½"));
    }
}
