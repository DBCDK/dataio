package dk.dbc.dataio.cli;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;

/**
 * TestSuite abstraction
 * <p>
 *     A test suite consists of a NAME together with a data file called NAME (extension is optional) and a
 *     properties file called NAME.acctest located somewhere in the local filesystem.
 * </p>
 * <pre>
 *    // find all test suites in current working directory
 *    final List&lt;TestSuite&gt; testSuites = TestSuite.findAllTestSuites(getCurrentWorkingDirectory());
 *    for (TestSuite suite : testSuites) {
 *        System.out.println(suite);
 *    }
 *
 *    // find specific test suite in current working directory
 *    final Optional&lt;TestSuite&gt; suite = TestSuite.findTestSuite(getCurrentWorkingDirectory(), "mysuite");
 *    System.out.println(suite.orElse(null));
 *
 *    private static Path getCurrentWorkingDirectory() {
 *        return Paths.get(".").toAbsolutePath().normalize();
 *    }
 * </pre>
 */
public class TestSuite {
    private final static String TESTSUITE_PROPERTIES_FILE_EXTENSION = ".acctest";

    private final String name;
    private Properties properties;
    private Path dataFile;

    public static Optional<TestSuite> findTestSuite(Path directory, String testSuiteName) throws IOException {
        return findAllTestSuites(directory).stream()
                .filter(suite -> suite.getName().equals(testSuiteName))
                .findFirst();
    }

    public static List<TestSuite> findAllTestSuites(Path directory) throws IOException {
        final TestSuitesFinder testSuitesFinder = new TestSuitesFinder();
        Files.walkFileTree(directory, testSuitesFinder);
        return testSuitesFinder.getTestSuites();
    }

    public TestSuite(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public Properties getProperties() {
        return properties;
    }

    public Path getDataFile() {
        return dataFile;
    }

    @Override
    public String toString() {
        return "TestSuite{" +
                "name='" + name + '\'' +
                ", properties=" + properties +
                ", dataFile=" + dataFile +
                '}';
    }

    TestSuite withPropertiesFile(Path propertiesFile) throws IOException {
        properties = new Properties();
        properties.load(new FileInputStream(propertiesFile.toFile()));
        return this;
    }

    TestSuite withDataFile(Path dataFile) {
        this.dataFile = dataFile;
        return this;
    }

    private static class TestSuitesFinder extends SimpleFileVisitor<Path> {
        private final Map<String, List<Path>> fileIndex;

        TestSuitesFinder() {
            fileIndex = new HashMap<>();
        }

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
            final String fileKey = getFileNameWithoutExtension(file);
            if (!fileIndex.containsKey(fileKey)) {
                fileIndex.put(fileKey, new ArrayList<>());
            }
            fileIndex.get(fileKey).add(file);

            return FileVisitResult.CONTINUE;
        }

        List<TestSuite> getTestSuites() throws IOException {
            final List<TestSuite> testSuites = new ArrayList<>();

            for (Map.Entry<String, List<Path>> indexEntry : fileIndex.entrySet()) {
                final TestSuite testSuite = new TestSuite(indexEntry.getKey());

                final List<Path> fileList = indexEntry.getValue();
                if (fileList.size() == 2) {  // .acctest file + data file
                    for (Path file : fileList) {
                        if (file.toString().toLowerCase().endsWith(TESTSUITE_PROPERTIES_FILE_EXTENSION)) {
                            testSuite.withPropertiesFile(file);
                        } else {
                            testSuite.withDataFile(file);
                        }
                    }
                    if (testSuite.getDataFile() != null && testSuite.getProperties() != null) {
                        testSuites.add(testSuite);
                    }
                }
            }
            return testSuites;
        }

        private String getFileNameWithoutExtension(Path file) {
            final String fileName = file.getFileName().toString();
            int lastDot = fileName.lastIndexOf(".");
            return lastDot >= 1 ? fileName.substring(0, lastDot) : fileName;
        }
    }
}
