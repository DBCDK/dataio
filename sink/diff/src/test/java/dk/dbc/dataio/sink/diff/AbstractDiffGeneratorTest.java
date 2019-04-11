package dk.dbc.dataio.sink.diff;

public class AbstractDiffGeneratorTest {
    public ExternalToolDiffGenerator newExternalToolDiffGenerator() {
        ExternalToolDiffGenerator.path = "src/main/docker/dockerfile/script/";
        final ExternalToolDiffGenerator externalToolDiffGenerator = new ExternalToolDiffGenerator();
        //xmlDiffGenerator.threadFactory = Thread::new;
        return externalToolDiffGenerator;
    }
}
