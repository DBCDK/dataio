package dk.dbc.dataio.cli;

import com.fasterxml.jackson.databind.ObjectMapper;
import dk.dbc.dataio.commons.types.Flow;
import dk.dbc.dataio.commons.types.FlowContent;
import dk.dbc.dataio.commons.types.JobSpecification;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

class AccTestRunnerTest {

   /* To update the 'simple.jsar' archive, simply change directory into the
      'src/test/resources/jsar/simple.files' directory, edit the necessary files
      and run 'zip -r ../simple.jsar * && cp ../simple.jsar ../simple.old.jsar && zip -j ../simple.old.jsar ../simple.old.files/entrypoint.js' */

    @TempDir
    Path tempDir;

    private final Path simpleJsar = Path.of("src", "test", "resources", "jsar", "simple.jsar");
    private final Path simpleOldJsar = Path.of("src", "test", "resources", "jsar", "simple.old.jsar");
    private final Path dataPath = Path.of("src", "test", "resources", "data");

    private static FlowManagerMock flowManagerMock;

    @BeforeEach
    void resetFlowManagerMock() {
        flowManagerMock = new FlowManagerMock();
    }

    @Test
    void test_noTestSuitesFound() {
        int exitCode = AccTestRunner.runWith(AccTestRunnerTest::flowManagerMock,
                "TEST", simpleJsar.toString(), simpleJsar.getParent().toString(),
                arg("cp", tempDir), arg("v", "1"));
        assertThat("exit code", exitCode, is(-255));
        assertThat("commit file found", Files.exists(tempDir.resolve(FlowManager.FLOW_COMMIT_TMP)), is(false));
    }

    @Test
    void test_flowNotFoundRemotely() throws IOException {
        FlowContent flowContent = flowManagerMock.getFlowContent(simpleJsar);
        flowManagerMock.setFlowByJsar(new Flow(1, 1, flowContent));

        // Not resolving flow by name or specification, assumes a new flow is about to be created

        int exitCode = AccTestRunner.runWith(AccTestRunnerTest::flowManagerMock,
                "TEST", simpleJsar.toString(), dataPath.toString(),
                arg("cp", tempDir), arg("rs", "JSON"), arg("v", "1"));
        assertThat("exit code", exitCode, is(0));

        Path commitFile = tempDir.resolve(FlowManager.FLOW_COMMIT_TMP);
        assertThat("commit file found", Files.exists(commitFile), is(true));
        FlowManager.CommitTempFile commitTempFile = getCommitTempFile(commitFile);
        assertThat("commit action", commitTempFile.action, is(FlowManager.CommitTempFile.Action.CREATE));
        assertThat("commit flow content", commitTempFile.flow.getContent(), is(flowContent));
    }

    @Test
    void test_flowFoundRemotelyByName() throws IOException {
        flowManagerMock.setFlowByJsar(new Flow(42, 100, flowManagerMock.getFlowContent(simpleJsar)));
        flowManagerMock.setFoundFlowByNameFlag(true);

        // Flow resolved by name, but test suite failed to resolve to a flow

        int exitCode = AccTestRunner.runWith(AccTestRunnerTest::flowManagerMock, "TEST", dataPath.toString(),
                arg("jsar", simpleJsar), arg("cp", tempDir), arg("rs", "JSON"), arg("v", "1"));
        assertThat("exit code", exitCode, is(-255));
        assertThat("commit file found", Files.exists(tempDir.resolve(FlowManager.FLOW_COMMIT_TMP)), is(false));
    }

    @Test
    void test_flowFoundRemotelyByJobSpecification() throws IOException {
        FlowContent flowContent = flowManagerMock.getFlowContent(simpleJsar);
        flowManagerMock.setFlowByJsar(new Flow(1, 1, flowContent));
        flowManagerMock.setFlowByJobSpecification(new Flow(42, 100, flowContent));
        flowManagerMock.setFoundFlowByNameFlag(false);

        // Flow failed to be resolved by name, but test suite resolved to a flow

        int exitCode = AccTestRunner.runWith(AccTestRunnerTest::flowManagerMock,
                "TEST", simpleJsar.toString(), dataPath.toString(),
                arg("cp", tempDir), arg("rs", "JSON"), arg("v", "1"));
        assertThat("exit code", exitCode, is(0));

        Path commitFile = tempDir.resolve(FlowManager.FLOW_COMMIT_TMP);
        assertThat("commit file found", Files.exists(commitFile), is(true));
        FlowManager.CommitTempFile commitTempFile = getCommitTempFile(commitFile);
        assertThat("commit action", commitTempFile.action, is(FlowManager.CommitTempFile.Action.UPDATE));
        assertThat("commit flow ID", commitTempFile.flow.getId(), is(42L));
        assertThat("commit flow version", commitTempFile.flow.getVersion(), is(100L));
        assertThat("commit flow content", commitTempFile.flow.getContent(), is(flowContent));
    }

    @Test
    void test_flowFoundRemotelyByNameAndJobSpecification() throws IOException {
        FlowContent flowContent = flowManagerMock.getFlowContent(simpleJsar);
        flowManagerMock.setFlowByJsar(new Flow(42, 100, flowContent));
        flowManagerMock.setFlowByJobSpecification(new Flow(42, 100, flowContent));
        flowManagerMock.setFoundFlowByNameFlag(true);

        // Flow resolved both by name and test suite

        int exitCode = AccTestRunner.runWith(AccTestRunnerTest::flowManagerMock,
                "TEST", simpleJsar.toString(), dataPath.toString(),
                arg("cp", tempDir), arg("rs", "JSON"), arg("v", "1"));
        assertThat("exit code", exitCode, is(0));

        Path commitFile = tempDir.resolve(FlowManager.FLOW_COMMIT_TMP);
        assertThat("commit file found", Files.exists(commitFile), is(true));
        FlowManager.CommitTempFile commitTempFile = getCommitTempFile(commitFile);
        assertThat("commit action", commitTempFile.action, is(FlowManager.CommitTempFile.Action.UPDATE));
        assertThat("commit flow ID", commitTempFile.flow.getId(), is(42L));
        assertThat("commit flow version", commitTempFile.flow.getVersion(), is(100L));
        assertThat("commit flow content", commitTempFile.flow.getContent(), is(flowContent));
    }

    @Test
    void test_differentFlowsFoundRemotelyByNameAndJobSpecification() throws IOException {
        flowManagerMock.setFlowByJsar(new Flow(42, 100, flowManagerMock.getFlowContent(simpleJsar)));
        flowManagerMock.setFlowByJobSpecification(new Flow(43, 100, flowManagerMock.getFlowContent(simpleJsar)));
        flowManagerMock.setFoundFlowByNameFlag(true);

        // Different flows resolved both by name and test suite

        int exitCode = AccTestRunner.runWith(AccTestRunnerTest::flowManagerMock,
                "TEST", simpleJsar.toString(), dataPath.toString(),
                arg("cp", tempDir), arg("rs", "JSON"), arg("v", "1"));
        assertThat("exit code", exitCode, is(-255));
        assertThat("commit file found", Files.exists(tempDir.resolve(FlowManager.FLOW_COMMIT_TMP)), is(false));
    }

    @Test
    void test_producesDiff() throws IOException {
        FlowContent flowContent = flowManagerMock.getFlowContent(simpleJsar);
        flowManagerMock.setFlowByJsar(new Flow(42, 100, flowContent));
        flowManagerMock.setFlowByJobSpecification(new Flow(42, 100, flowManagerMock.getFlowContent(simpleOldJsar)));
        flowManagerMock.setFoundFlowByNameFlag(true);

        int exitCode = AccTestRunner.runWith(AccTestRunnerTest::flowManagerMock,
                "TEST", simpleJsar.toString(), dataPath.toString(),
                arg("cp", tempDir), arg("rs", "JSON"), arg("v", "1"));
        assertThat("exit code", exitCode, is(1));

        Path commitFile = tempDir.resolve(FlowManager.FLOW_COMMIT_TMP);
        assertThat("commit file found", Files.exists(commitFile), is(true));
        FlowManager.CommitTempFile commitTempFile = getCommitTempFile(commitFile);
        assertThat("commit action", commitTempFile.action, is(FlowManager.CommitTempFile.Action.UPDATE));
        assertThat("commit flow ID", commitTempFile.flow.getId(), is(42L));
        assertThat("commit flow version", commitTempFile.flow.getVersion(), is(100L));
        assertThat("commit flow content", commitTempFile.flow.getContent(), is(flowContent));
    }

    private String arg(String name, Path value) {
        return arg(name, value.toAbsolutePath().toString());
    }

    private String arg(String name, String value) {
        return String.format("-%s=%s", name, value);
    }

    private static FlowManager.CommitTempFile getCommitTempFile(Path commitFile) {
        try {
            return new ObjectMapper().readValue(commitFile.toFile(), FlowManager.CommitTempFile.class);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static FlowManager flowManagerMock(String serviceUrl) {
        return flowManagerMock;
    }

    private static class FlowManagerMock extends FlowManager {

        private Flow flowByJsar = null;
        private Flow flowByJobSpecification = null;

        public FlowManagerMock() {
            super(null);
        }

        public void setFlowByJsar(Flow flow) {
            this.flowByJsar = flow;
        }

        public void setFlowByJobSpecification(Flow flow) {
            this.flowByJobSpecification = flow;
        }

        public void setFoundFlowByNameFlag(boolean flag) {
            foundFlowByName = flag;
        }

        @Override
        public Flow getFlow(Path jsar) {
            return flowByJsar;
        }

        @Override
        public Flow getFlow(JobSpecification jobSpecification) {
            return flowByJobSpecification;
        }
    }
}