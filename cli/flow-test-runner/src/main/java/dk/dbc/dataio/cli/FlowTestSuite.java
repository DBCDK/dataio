package dk.dbc.dataio.cli;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Scanner;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Represents a flow test suite
 */
public class FlowTestSuite {

    private final Path featureFile;
    private final Path featureDir;
    private final Path actualStateDir;
    private final Path expectedStateDir;
    private final Path inputDir;
    private final Path logsDir;

    private final String featureName;
    private final List<Scenario> scenarios;
    private String featureDescription;

    private FlowTestSuite(Path featureFile) {
        this.featureFile = featureFile.toAbsolutePath();
        this.featureDir = this.featureFile.getParent();
        this.featureName = this.featureDir.getFileName().toString();
        this.actualStateDir = this.featureDir.resolve("actual_state");
        this.expectedStateDir = this.featureDir.resolve("expected_state");
        this.inputDir = this.featureDir.resolve("input");
        this.logsDir = this.featureDir.resolve("logs");
        final FeatureFileParser parser = new FeatureFileParser(featureFile).parse();
        this.featureDescription = parser.getFeatureDescription();
        this.scenarios = parser.getScenarios();
    }

    public static List<FlowTestSuite> findFlowTestSuites(Path startPath) throws IOException {
        return findFeatureFiles(startPath).stream().map(FlowTestSuite::new).toList();
    }

    public Path getFeatureFile() {
        return featureFile;
    }

    public Path getFeatureDir() {
        return featureDir;
    }

    public Path getActualStateDir() {
        return actualStateDir;
    }

    public Path getExpectedStateDir() {
        return expectedStateDir;
    }

    public Path getInputDir() {
        return inputDir;
    }

    public Path getLogsDir() {
        return logsDir;
    }

    public String getFeatureName() {
        return featureName;
    }

    public String getFeatureDescription() {
        return featureDescription;
    }

    public List<Scenario> getScenarios() {
        return scenarios;
    }

    @Override
    public String toString() {
        return "FlowTestSuite{" +
                "featureFile=" + featureFile +
                ", featureDir=" + featureDir +
                ", actualStateDir=" + actualStateDir +
                ", expectedStateDir=" + expectedStateDir +
                ", inputDir=" + inputDir +
                ", logsDir=" + logsDir +
                ", featureName='" + featureName + '\'' +
                ", scenarios=" + scenarios +
                '}';
    }

    /*
     * Class to parse a feature file and extract test scenarios,
     * see src/test/resources/suite1|suite2 for examples of feature files.
     *
     * The feature file format is inspired by the Gherkin language,
     * and must begin with a "Egenskab:" line followed by a description of the feature.
     * Each scenario must begin with a "Scenarie:" line followed by a description of the scenario.
     * The scenario must contain a "Når ... fil <input file from 'input' dir> ... agency <agency> ... format <format> ..." line
     * followed by a "Så ... fil <expected output file from 'expected_state' dir> ..." line.
     */
    private static class FeatureFileParser {
        private static final String FEATURE_LINE = "egenskab:";
        private static final String SCENARIO_LINE = "scenarie:";
        private static final String WHEN_LINE = "når ";
        private static final String THEN_LINE = "så ";
        private static final String COMMENT_LINE = "# ";

        private final Path featureFile;
        private final List<Scenario> scenarios = new ArrayList<>();
        private String featureDescription;

        public FeatureFileParser(Path featureFile) {
            this.featureFile = featureFile;
        }

        public FeatureFileParser parse() {
            try (final Scanner scanner = new Scanner(featureFile.toFile())) {
                String line = getNextLine(scanner);
                while (line != null) {
                    if (line.toLowerCase().startsWith(SCENARIO_LINE)) {
                        line = parseScenario(scanner, line);
                    } else if (line.toLowerCase().startsWith(FEATURE_LINE)) {
                        line = parseFeature(scanner, line);
                    } else {
                        line = getNextLine(scanner);
                    }
                }
            } catch (FileNotFoundException e) {
                throw new UncheckedIOException(e);
            }
            return this;
        }

        public List<Scenario> getScenarios() {
            return scenarios;
        }

        public String getFeatureDescription() {
            return featureDescription;
        }

        private String parseFeature(Scanner scanner, String line) {
            final StringBuilder featureDescriptionBuilder = new StringBuilder(line.substring(FEATURE_LINE.length()).trim());
            String nextLine = getNextLine(scanner);
            while (nextLine != null) {
                if (nextLine.toLowerCase().startsWith(SCENARIO_LINE)) {
                    break;
                }
                featureDescriptionBuilder.append("\n").append(nextLine);
                nextLine = getNextLine(scanner);
            }
            featureDescription = featureDescriptionBuilder.toString();
            return nextLine;
        }

        private String parseScenario(Scanner scanner, String line) {
            final Scenario scenario = new Scenario();
            final StringBuilder scenarioDescription = new StringBuilder();
            scenario.setName(line.substring(SCENARIO_LINE.length()).trim().replace(" ", "_"));

            String nextLine = getNextLine(scanner);
            while (nextLine != null) {
                if (nextLine.toLowerCase().startsWith(SCENARIO_LINE)) {
                    break;
                } else if (nextLine.toLowerCase().startsWith(COMMENT_LINE)) {
                    scenarioDescription.append(nextLine).append("\n");
                } else if (nextLine.toLowerCase().startsWith(WHEN_LINE)) {
                    parseWhen(nextLine, scenario);
                } else if (nextLine.toLowerCase().startsWith(THEN_LINE)) {
                    parseThen(nextLine, scenario);
                }
                nextLine = getNextLine(scanner);
            }

            scenario.setDescription(scenarioDescription.toString());
            scenarios.add(scenario);

            return nextLine;
        }

        private void parseWhen(String line, Scenario scenario) {
            try (Scanner whenScanner = new Scanner(new ByteArrayInputStream(line.getBytes(StandardCharsets.UTF_8)))) {
                while (whenScanner.hasNext()) {
                    final String nextToken = whenScanner.next();
                    if (nextToken.equalsIgnoreCase("fil")) {
                        scenario.setInputFile(whenScanner.next());
                    } else if (nextToken.equalsIgnoreCase("agency")) {
                        scenario.setAgency(whenScanner.next());
                    } else if (nextToken.equalsIgnoreCase("format")) {
                        scenario.setFormat(whenScanner.next());
                    }
                }
            }
        }

        private void parseThen(String line, Scenario scenario) {
            try (Scanner thenScanner = new Scanner(new ByteArrayInputStream(line.getBytes(StandardCharsets.UTF_8)))) {
                while (thenScanner.hasNext()) {
                    final String nextToken = thenScanner.next();
                    if (nextToken.equalsIgnoreCase("fil")) {
                        scenario.setOutputFile(thenScanner.next());
                    }
                }
            }
        }

        private String getNextLine(Scanner scanner) {
            if (scanner.hasNextLine()) {
                return scanner.nextLine().trim();
            }
            return null;
        }
    }

    /**
     * Represents a scenario in a feature file
     */
    public static class Scenario {
        private String name;
        private String description;
        private String inputFile;
        private String outputFile;
        private String agency;
        private String format;

        public void setName(String name) {
            this.name = name;
        }

        public Scenario withName(String name) {
            this.name = name;
            return this;
        }

        public String getName() {
            return name;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public Scenario withDescription(String description) {
            this.description = description;
            return this;
        }

        public String getInputFile() {
            return inputFile;
        }

        public void setInputFile(String inputFile) {
            this.inputFile = inputFile;
        }

        public Scenario withInputFile(String inputFile) {
            this.inputFile = inputFile;
            return this;
        }

        public String getOutputFile() {
            return outputFile;
        }

        public void setOutputFile(String outputFile) {
            this.outputFile = outputFile;
        }

        public Scenario withOutputFile(String outputFile) {
            this.outputFile = outputFile;
            return this;
        }

        public String getAgency() {
            return agency;
        }

        public void setAgency(String agency) {
            this.agency = agency;
        }

        public Scenario withAgency(String agency) {
            this.agency = agency;
            return this;
        }

        public String getFormat() {
            return format;
        }

        public void setFormat(String format) {
            this.format = format;
        }

        public Scenario withFormat(String format) {
            this.format = format;
            return this;
        }

        @Override
        public String toString() {
            return "Scenario{" +
                    "name='" + name + '\'' +
                    ", inputFile='" + inputFile + '\'' +
                    ", outputFile='" + outputFile + '\'' +
                    ", agency='" + agency + '\'' +
                    ", format='" + format + '\'' +
                    '}';
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            Scenario scenario = (Scenario) o;
            return Objects.equals(name, scenario.name) && Objects.equals(inputFile, scenario.inputFile) && Objects.equals(outputFile, scenario.outputFile) && Objects.equals(agency, scenario.agency) && Objects.equals(format, scenario.format);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, inputFile, outputFile, agency, format);
        }
    }

    private static List<Path> findFeatureFiles(Path startPath) throws IOException {
        final ArrayList<Path> featureFiles = new ArrayList<>();
        Files.walkFileTree(startPath, new FeatureFileVisitor(featureFiles::add));
        return featureFiles;
    }

    /*
     * Class to visit all files in a directory and filter out files with the extension ".feature"
     */
    private static class FeatureFileVisitor extends SimpleFileVisitor<Path> {

        private final Predicate<Path> filter;
        private final Consumer<Path> consumer;

        public FeatureFileVisitor(Consumer<Path> consumer) {
            this.filter = new MatchFeatureExtensionPredicate();
            this.consumer = consumer;
        }

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attributes) {
            if (filter.test(file)) {
                consumer.accept(file);
            }
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult visitFileFailed(Path file, IOException e){
            return FileVisitResult.CONTINUE;
        }
    }

    /*
     * Class to filter out files with the extension ".feature"
     */
    private static class MatchFeatureExtensionPredicate implements Predicate<Path> {

        private static final String extension = ".feature";

        @Override
        public boolean test(Path path) {
            if (path == null) {
                return false;
            }
            return path.getFileName()
                    .toString()
                    .toLowerCase()
                    .endsWith(extension);
        }
    }
}
