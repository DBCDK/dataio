package dk.dbc.dataio.cli;

import dk.dbc.dataio.commons.types.JobSpecification;
import dk.dbc.dataio.commons.types.RecordSplitter;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Map;
import java.util.Properties;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class AccTestSuite {
    public static final String ACC_TEXT_EXT = "acctest";
    public static final Pattern ACC_TEXT_EXT_PATTERN = Pattern.compile("\\." + ACC_TEXT_EXT + "$");
    private String name;
    private Path testSpec;
    private final Path dataFile;
    private final Path reportPath;
    private final JobSpecification jobSpecification;
    private RecordSplitter recordSplitter;

    public AccTestSuite(Path testSpec, Path reportDir) {
        this.testSpec = testSpec;
        name = ACC_TEXT_EXT_PATTERN.matcher(testSpec.getFileName().toString()).replaceFirst("");
        jobSpecification = readTestSpec(testSpec);
        reportPath = reportDir == null ? null : reportDir.resolve("/TESTS-dbc_" + getName() + ".xml");
        try(Stream<Path> dataStream = Files.find(testSpec.getParent(), 1, this::isAccTestDataFile)) {
            dataFile = dataStream.findFirst().orElseThrow(() -> new IllegalArgumentException("Unable to find datafile for acc-test " + getName()));
        } catch (IOException e) {
            throw new IllegalStateException("Something foul happened while searching for the data file for acc-test " + getName(), e);
        }
    }

    public AccTestSuite(JobSpecification jobSpecification, RecordSplitter recordSplitter) {
        this.jobSpecification = jobSpecification;
        dataFile = Path.of(jobSpecification.getDataFile());
        this.recordSplitter = recordSplitter;
        reportPath = null;
    }

    private JobSpecification readTestSpec(Path testSpec) {
        Properties p = new Properties();
        try (Reader reader = Files.newBufferedReader(testSpec)) {
            p.load(reader);
        } catch (IOException e) {
            throw new RuntimeException("Unable to read acc-test spec " + testSpec, e);
        }
        JobSpecification js = new JobSpecification();
        Map<Object, Consumer<String>> map = Map.of("submitterId", v -> js.withSubmitterId(Long.parseLong(v)),
                "packaging", js::withPackaging,
                "format", js::withFormat,
                "destination", js::withDestination,
                "charset", js::withCharset,
                "recordSplitter", v -> recordSplitter = RecordSplitter.valueOf(v));
        p.forEach((k, v) -> map.get(k).accept((String) v));
        return js;
    }

    public String getName() {
        return name;
    }

    public Path getDataFile() {
        return dataFile;
    }

    public JobSpecification getJobSpecification() {
        return jobSpecification;
    }

    public RecordSplitter getRecordSplitter() {
        return recordSplitter;
    }

    public static boolean isAccTestSpec(Path path, BasicFileAttributes fa) {
        return fa.isRegularFile() && path.getFileName().toString().endsWith("." + ACC_TEXT_EXT);
    }

    public boolean isAccTestDataFile(Path path, BasicFileAttributes fa) {
        return fa.isRegularFile() && !path.equals(testSpec) && path.getFileName().toString().startsWith(getName());
    }

    public Path getReportPath() {
        return reportPath;
    }
}
