package dk.dbc.dataio.sink.diff;

public class AbstractDiffGeneratorTest {
    static final String NO_DIFF = "";

    private final boolean testEnvironmentHasCat = Boolean.parseBoolean(System.getProperty("testenv.cat"));
    private final boolean testEnvironmentHasDiff = Boolean.parseBoolean(System.getProperty("testenv.diff"));
    private final boolean testEnvironmentHasGrep = Boolean.parseBoolean(System.getProperty("testenv.grep"));
    private final boolean testEnvironmentHasJq = Boolean.parseBoolean(System.getProperty("testenv.jq"));
    private final boolean testEnvironmentHasSed = Boolean.parseBoolean(System.getProperty("testenv.sed"));
    private final boolean testEnvironmentHasXmllint = Boolean.parseBoolean(System.getProperty("testenv.xmllint"));

    ExternalToolDiffGenerator newExternalToolDiffGenerator() {
        ExternalToolDiffGenerator.path = "src/main/docker/script/";
        final ExternalToolDiffGenerator externalToolDiffGenerator = new ExternalToolDiffGenerator();
        return externalToolDiffGenerator;
    }

    boolean canDiff() {
        return testEnvironmentHasDiff && testEnvironmentHasGrep;
    }

    boolean canJsonDiff() {
        return canDiff() && testEnvironmentHasJq;
    }

    boolean canXmlDiff() {
        return canDiff() && testEnvironmentHasCat && testEnvironmentHasSed && testEnvironmentHasXmllint;
    }
}
