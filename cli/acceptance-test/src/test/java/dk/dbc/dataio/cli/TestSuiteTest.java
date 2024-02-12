package dk.dbc.dataio.cli;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

public class TestSuiteTest {
    @Rule
    public TemporaryFolder dir = new TemporaryFolder();

    @Test
    public void findTestSuite_suiteNotFound_returnsEmpty() throws IOException {
        assertThat(TestSuite.findTestSuite(dir.getRoot().toPath(), "suite"), is(Optional.empty()));
    }

    @Test
    public void findTestSuite_suiteIsIncomplete_returnsEmpty() throws IOException {
        Path tests = createTestDir();
        Files.writeString(tests.resolve("suite.acctest"), "id=suite");
        assertThat(TestSuite.findTestSuite(dir.getRoot().toPath(), "suite"), is(Optional.empty()));
    }

    private Path createTestDir() throws IOException {
        Path tests = dir.getRoot().toPath().resolve("tests");
        return Files.createDirectories(tests);
    }

    @Test
    public void findTestSuite() throws IOException {
        Path tests = createTestDir();
        Path dataFile = tests.resolve("suite.txt");
        Files.writeString(dataFile, "data");
        Files.writeString(tests.resolve("suite.acctest"), "id=suite");

        TestSuite testsuite = TestSuite.findTestSuite(dir.getRoot().toPath(), "suite").orElse(null);
        assertThat("testsuite", testsuite, is(notNullValue()));
        assertThat("testsuite name", testsuite.getName(), is("suite"));
        assertThat("testsuite data file", testsuite.getDataFile(), is(dataFile));
        assertThat("testsuite properties", testsuite.getProperties().getProperty("id"), is("suite"));
    }

    @Test
    public void findAllTestSuites_noSuitesFound_returnsEmpty() throws IOException {
        assertThat(TestSuite.findAllTestSuites(dir.getRoot().toPath()), is(Collections.emptyList()));
    }

    @Test
    public void findAllTestSuites() throws IOException {
        Path suites = dir.getRoot().toPath().resolve("test").resolve("suites");
        Files.createDirectories(suites);
        Path data = dir.getRoot().toPath().resolve("test").resolve("data");
        Files.createDirectories(data);
        Path suite1DataFile = data.resolve("suite1.txt");
        Path suite2DataFile = data.resolve("suite2.dat");
        Files.writeString(suite1DataFile, "data");
        Files.writeString(suite2DataFile, "data");
        Files.writeString(suites.resolve("suite1.acctest"), "id=suite1");
        Files.writeString(suites.resolve("suite2.acctest"), "id=suite2");
        Files.writeString(suites.resolve("suite3.acctest"), "id=suite3");

        final List<TestSuite> testsuites = TestSuite.findAllTestSuites(dir.getRoot().toPath());
        assertThat("number of testsuites", testsuites.size(), is(2));
        testsuites.sort(Comparator.comparing(TestSuite::getName));
        assertThat("1st testsuite name", testsuites.get(0).getName(), is("suite1"));
        assertThat("1st testsuite data file", testsuites.get(0).getDataFile(), is(suite1DataFile));
        assertThat("1st testsuite properties", testsuites.get(0).getProperties().getProperty("id"), is("suite1"));
        assertThat("2nd testsuite name", testsuites.get(1).getName(), is("suite2"));
        assertThat("2nd testsuite data file", testsuites.get(1).getDataFile(), is(suite2DataFile));
        assertThat("2nd testsuite properties", testsuites.get(1).getProperties().getProperty("id"), is("suite2"));
    }
}
