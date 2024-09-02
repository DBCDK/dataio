package dk.dbc.dataio.cli;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

class FlowTestRunnerTest {

   /* To update the 'simple.jsar' archive, simply change directory into the
      'src/test/resources/jsar/simple.files' directory, edit the necessary files
      and run 'zip -r ../simple.jsar *' */

    @TempDir
    Path tempDir;

    private final Path jsar = Path.of("src", "test", "resources", "jsar", "simple.jsar");
    private final Path suite1 = Path.of("src", "test", "resources", "suite1");
    private final Path suite2 = Path.of("src", "test", "resources", "suite2");

    @Test
    void noJsarFound() {
        int exitCode = FlowTestRunner.runWith(FlowTestRunner::new, Path.of("no-such-path").toString(), suite1.toString());
        assertThat("exit code", exitCode, is(-255));
    }

    @Test
    void noTestSuitesFound() {
        int exitCode = FlowTestRunner.runWith(FlowTestRunner::new, jsar.toString(), Path.of("no-such-path").toString());
        assertThat("exit code", exitCode, is(0));
    }

    @Test
    void noInputFileFound() {
        int exitCode = FlowTestRunner.runWith(FlowTestRunner::new, jsar.toString(), suite2.toString());
        assertThat("exit code", exitCode, is(-255));
    }

    @Test
    void compare() {
        int exitCode = FlowTestRunner.runWith(FlowTestRunner::new, jsar.toString(), suite1.toString(),
                "-rp="+tempDir.toAbsolutePath());
        assertThat("exit code", exitCode, is(-1));
        assertThat("actual_state/150041.credo.delete.tickle.addi exists",
                Files.exists(suite1.resolve(Path.of("actual_state", "150041.credo.delete.tickle.addi"))),
                is(true));
        assertThat("actual_state/150041.credo.enpost.tickle.addi exists",
                Files.exists(suite1.resolve(Path.of("actual_state", "150041.credo.enpost.tickle.addi"))),
                is(true));
        assertThat("actual_state/150041.credo.topost.tickle.addi exists",
                Files.exists(suite1.resolve(Path.of("actual_state", "150041.credo.topost.tickle.addi"))),
                is(true));
        assertThat("logs/suite1.log exists",
                Files.exists(suite1.resolve(Path.of("logs", "suite1.log"))),
                is(true));
    }
}