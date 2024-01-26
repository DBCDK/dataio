package dk.dbc.dataio.sink.diff;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsNot.not;

public class XmlDiffGeneratorParameterizedTest extends AbstractDiffGeneratorTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(XmlDiffGeneratorParameterizedTest.class);
    private static final String CURRENT_XML = "-current.xml";
    private static final String NEXT_XML = "-next.xml";

    public static Stream<Arguments> testData() throws URISyntaxException, IOException {
        URL url = XmlDiffGeneratorParameterizedTest.class.getResource("/");
        Path resPath;
        resPath = Paths.get(url.toURI());
        List<String[]> result = new LinkedList<>();
        String filter = "-(current|next).xml";
        Map<String, List<Path>> tests = Files.list(resPath).filter(f -> f.toString().matches(".*" + filter)).collect(Collectors.groupingBy(f -> f.toString().replaceAll(filter, "")));
        return tests.values().stream().filter(l -> l.size() > 1).map(l -> Arguments.of(getTestData("current", l), getTestData("next", l)));
    }

    @ParameterizedTest
    @MethodSource("testData")
    public void testName(Path currentFile, Path nextFile) throws Exception {
        if (canXmlDiff()) {
            ExternalToolDiffGenerator xmlDiffGenerator = newExternalToolDiffGenerator();
            String diff = xmlDiffGenerator.getDiff(
                    Kind.XML,
                    XmlDiffGeneratorTest.readTestRecord(currentFile),
                    XmlDiffGeneratorTest.readTestRecord(nextFile)
            );
            assertThat(diff, not(""));
        }
    }

    private static Path getTestData(String id, List<Path> paths) {
        return paths.stream().filter(s -> s.toString().matches(".*/.*" + id + "\\.xml"))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No path found for " + id + " in " + paths));
    }
}
