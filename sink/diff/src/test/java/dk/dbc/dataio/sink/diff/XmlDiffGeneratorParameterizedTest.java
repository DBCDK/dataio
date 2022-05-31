package dk.dbc.dataio.sink.diff;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsNot.not;

@RunWith(Parameterized.class)
public class XmlDiffGeneratorParameterizedTest extends AbstractDiffGeneratorTest {

    private static final String CURRENT_XML = "-current.xml";
    private static final String NEXT_XML = "-next.xml";

    @Parameters(name = "xmldiff current {0} - next {1}")
    public static Collection<String[]> testData() throws URISyntaxException, IOException {
        final java.net.URL url = XmlDiffGeneratorParameterizedTest.class.getResource("/");
        final java.nio.file.Path resPath;
        resPath = java.nio.file.Paths.get(url.toURI());

        final List<String[]> result = new LinkedList<>();

        try (DirectoryStream<Path> ds = Files.newDirectoryStream(resPath)) {
            for (final Path fn : ds) {
                if (fn.getFileName().toString().endsWith(CURRENT_XML)) {
                    String[] fileEntry = new String[2];
                    fileEntry[0] = fn.getFileName().toString();
                    fileEntry[1] = fn.getFileName().toString().replace(CURRENT_XML, NEXT_XML);
                    result.add(fileEntry);
                }
            }
        }

        return result;
    }

    @Parameterized.Parameter(value = 0)
    public String currentFileName;
    @Parameterized.Parameter(value = 1)
    public String nextFileName;


    @Test
    public void testName() throws Exception {
        if (canXmlDiff()) {
            final ExternalToolDiffGenerator xmlDiffGenerator = newExternalToolDiffGenerator();
            final String diff = xmlDiffGenerator.getDiff(
                    ExternalToolDiffGenerator.Kind.XML,
                    XmlDiffGeneratorTest.readTestRecord(currentFileName),
                    XmlDiffGeneratorTest.readTestRecord(nextFileName)
            );
            assertThat(diff, not(""));
        }
    }
}
