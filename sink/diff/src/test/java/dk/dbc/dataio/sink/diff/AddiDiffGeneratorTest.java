package dk.dbc.dataio.sink.diff;

import dk.dbc.dataio.commons.types.exceptions.InvalidMessageException;
import org.junit.Test;

import java.nio.charset.StandardCharsets;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;

public class AddiDiffGeneratorTest extends AbstractDiffGeneratorTest {
    private final AddiDiffGenerator addiDiffGenerator = new AddiDiffGenerator(newExternalToolDiffGenerator());

    @Test
    public void noDiff() throws DiffGeneratorException, InvalidMessageException {
        final byte[] addiBytes = "9\nmetadata1\n8\ncontent1\n9\nmetadata2\n8\ncontent2\n".getBytes(StandardCharsets.UTF_8);
        assertThat(addiDiffGenerator.getDiff(addiBytes, addiBytes), is(NO_DIFF));
    }

    @Test
    public void plaintextDiff() throws DiffGeneratorException, InvalidMessageException {
        if (canDiff()) {
            final byte[] currentAddiBytes = "9\nmetadata1\n8\ncontent1\n9\nmetadata2\n8\ncontent2\n".getBytes(StandardCharsets.UTF_8);
            final byte[] nextAddiBytes = "9\nmetadataA\n8\ncontentA\n9\nmetadata2\n8\ncontent2\n".getBytes(StandardCharsets.UTF_8);
            final String diff = addiDiffGenerator.getDiff(currentAddiBytes, nextAddiBytes);
            assertThat(diff, containsString("-metadata1"));
            assertThat(diff, containsString("+metadataA"));
            assertThat(diff, containsString("-content1"));
            assertThat(diff, containsString("+contentA"));
        }
    }

    @Test
    public void jsonDiff() throws DiffGeneratorException, InvalidMessageException {
        if (canJsonDiff()) {
            final byte[] currentAddiBytes = "14\n{\"metadata\":1}\n13\n{\"content\":1}\n14\n{\"metadata\":2}\n13\n{\"content\":2}\n".getBytes(StandardCharsets.UTF_8);
            final byte[] nextAddiBytes = "14\n{\"metadata\":1}\n13\n{\"content\":1}\n16\n{\"metadata\":\"B\"}\n15\n{\"content\":\"B\"}\n".getBytes(StandardCharsets.UTF_8);
            final String diff = addiDiffGenerator.getDiff(currentAddiBytes, nextAddiBytes);
            assertThat(diff, containsString("-  \"metadata\": 2"));
            assertThat(diff, containsString("+  \"metadata\": \"B\""));
            assertThat(diff, containsString("-  \"content\": 2"));
            assertThat(diff, containsString("+  \"content\": \"B\""));
        }
    }

    @Test
    public void xmlDiff() throws DiffGeneratorException, InvalidMessageException {
        if (canXmlDiff()) {
            final byte[] currentAddiBytes = "22\n<metadata>1</metadata>\n20\n<content>1</content>\n22\n<metadata>2</metadata>\n20\n<content>2</content>\n".getBytes(StandardCharsets.UTF_8);
            final byte[] nextAddiBytes = "22\n<metadata>1</metadata>\n20\n<content>1</content>\n22\n<metadata>B</metadata>\n20\n<content>B</content>\n".getBytes(StandardCharsets.UTF_8);
            final String diff = addiDiffGenerator.getDiff(currentAddiBytes, nextAddiBytes);
            assertThat(diff, containsString("-<metadata>2</metadata>"));
            assertThat(diff, containsString("+<metadata>B</metadata>"));
            assertThat(diff, containsString("-<content>2</content>"));
            assertThat(diff, containsString("+<content>B</content>"));
        }
    }

    @Test
    public void multipleDocTypesDiff() throws DiffGeneratorException, InvalidMessageException {
        if (canJsonDiff() && canXmlDiff()) {
            final byte[] currentAddiBytes = "14\n{\"metadata\":1}\n20\n<content>1</content>\n14\n{\"metadata\":2}\n20\n<content>2</content>\n".getBytes(StandardCharsets.UTF_8);
            final byte[] nextAddiBytes = "14\n{\"metadata\":1}\n20\n<content>1</content>\n16\n{\"metadata\":\"B\"}\n20\n<content>B</content>\n".getBytes(StandardCharsets.UTF_8);
            final String diff = addiDiffGenerator.getDiff(currentAddiBytes, nextAddiBytes);
            assertThat(diff, containsString("-  \"metadata\": 2"));
            assertThat(diff, containsString("+  \"metadata\": \"B\""));
            assertThat(diff, containsString("-<content>2</content>"));
            assertThat(diff, containsString("+<content>B</content>"));
        }
    }

    @Test
    public void multipleRecordsDiff() throws DiffGeneratorException, InvalidMessageException {
        if (canJsonDiff() && canXmlDiff()) {
            final byte[] currentAddiBytes = "14\n{\"metadata\":1}\n20\n<content>1</content>\n14\n{\"metadata\":2}\n20\n<content>2</content>\n".getBytes(StandardCharsets.UTF_8);
            final byte[] nextAddiBytes = "16\n{\"metadata\":\"A\"}\n20\n<content>A</content>\n16\n{\"metadata\":\"B\"}\n20\n<content>B</content>\n".getBytes(StandardCharsets.UTF_8);
            final String diff = addiDiffGenerator.getDiff(currentAddiBytes, nextAddiBytes);
            assertThat(diff, containsString("-  \"metadata\": 1"));
            assertThat(diff, containsString("+  \"metadata\": \"A\""));
            assertThat(diff, containsString("-<content>1</content>"));
            assertThat(diff, containsString("+<content>A</content>"));
            assertThat(diff, containsString("-  \"metadata\": 2"));
            assertThat(diff, containsString("+  \"metadata\": \"B\""));
            assertThat(diff, containsString("-<content>2</content>"));
            assertThat(diff, containsString("+<content>B</content>"));
        }
    }

    @Test
    public void currentRecordMissingFromNextDiff() throws DiffGeneratorException, InvalidMessageException {
        if (canDiff()) {
            final byte[] currentAddiBytes = "14\n{\"metadata\":1}\n20\n<content>1</content>\n14\n{\"metadata\":2}\n20\n<content>2</content>\n".getBytes(StandardCharsets.UTF_8);
            final byte[] nextAddiBytes = "14\n{\"metadata\":1}\n20\n<content>1</content>\n".getBytes(StandardCharsets.UTF_8);
            final String diff = addiDiffGenerator.getDiff(currentAddiBytes, nextAddiBytes);
            assertThat(diff, containsString("-{\"metadata\":2}"));
            assertThat(diff, containsString("-<content>2</content>"));
        }
    }

    @Test
    public void nextRecordNotInCurrentDiff() throws DiffGeneratorException, InvalidMessageException {
        if (canDiff()) {
            final byte[] currentAddiBytes = "14\n{\"metadata\":1}\n20\n<content>1</content>\n".getBytes(StandardCharsets.UTF_8);
            final byte[] nextAddiBytes = "14\n{\"metadata\":1}\n20\n<content>1</content>\n14\n{\"metadata\":2}\n20\n<content>2</content>\n".getBytes(StandardCharsets.UTF_8);
            final String diff = addiDiffGenerator.getDiff(currentAddiBytes, nextAddiBytes);
            assertThat(diff, containsString("+{\"metadata\":2}"));
            assertThat(diff, containsString("+<content>2</content>"));
        }
    }
}
