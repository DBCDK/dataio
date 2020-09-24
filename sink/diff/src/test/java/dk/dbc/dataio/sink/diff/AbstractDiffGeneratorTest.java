package dk.dbc.dataio.sink.diff;

public class AbstractDiffGeneratorTest {
    static final String NO_DIFF = "";

    ExternalToolDiffGenerator newExternalToolDiffGenerator() {
        ExternalToolDiffGenerator.path = "src/main/docker/script/";
        final ExternalToolDiffGenerator externalToolDiffGenerator = new ExternalToolDiffGenerator();
        return externalToolDiffGenerator;
    }
}
