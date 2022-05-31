package dk.dbc.dataio.cli;

import org.apache.commons.io.FileUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
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
        final File tests = dir.newFolder("test");
        FileUtils.write(new File(tests, "suite.acctest"), "id=suite");
        assertThat(TestSuite.findTestSuite(dir.getRoot().toPath(), "suite"), is(Optional.empty()));
    }

    @Test
    public void findTestSuite() throws IOException {
        final File tests = dir.newFolder("test");
        final File dataFile = new File(tests, "suite.txt");
        FileUtils.write(dataFile, "data");
        FileUtils.write(new File(tests, "suite.acctest"), "id=suite");

        final TestSuite testsuite = TestSuite.findTestSuite(dir.getRoot().toPath(), "suite").orElse(null);
        assertThat("testsuite", testsuite, is(notNullValue()));
        assertThat("testsuite name", testsuite.getName(), is("suite"));
        assertThat("testsuite data file", testsuite.getDataFile(), is(dataFile.toPath()));
        assertThat("testsuite properties", testsuite.getProperties().getProperty("id"), is("suite"));
    }

    @Test
    public void findAllTestSuites_noSuitesFound_returnsEmpty() throws IOException {
        assertThat(TestSuite.findAllTestSuites(dir.getRoot().toPath()), is(Collections.emptyList()));
    }

    @Test
    public void findAllTestSuites() throws IOException {
        final File suites = dir.newFolder("test", "suites");
        final File data = dir.newFolder("test", "data");
        final File suite1DataFile = new File(data, "suite1.txt");
        final File suite2DataFile = new File(data, "suite2.dat");
        FileUtils.write(suite1DataFile, "data");
        FileUtils.write(suite2DataFile, "data");
        FileUtils.write(new File(suites, "suite1.acctest"), "id=suite1");
        FileUtils.write(new File(suites, "suite2.acctest"), "id=suite2");
        FileUtils.write(new File(suites, "suite3.acctest"), "id=suite3");

        final List<TestSuite> testsuites = TestSuite.findAllTestSuites(dir.getRoot().toPath());
        assertThat("number of testsuites", testsuites.size(), is(2));
        Collections.sort(testsuites, (o1, o2) -> o1.getName().compareTo(o2.getName()));
        assertThat("1st testsuite name", testsuites.get(0).getName(), is("suite1"));
        assertThat("1st testsuite data file", testsuites.get(0).getDataFile(), is(suite1DataFile.toPath()));
        assertThat("1st testsuite properties", testsuites.get(0).getProperties().getProperty("id"), is("suite1"));
        assertThat("2nd testsuite name", testsuites.get(1).getName(), is("suite2"));
        assertThat("2nd testsuite data file", testsuites.get(1).getDataFile(), is(suite2DataFile.toPath()));
        assertThat("2nd testsuite properties", testsuites.get(1).getProperties().getProperty("id"), is("suite2"));
    }
}
