package dk.dbc.dataio.cli;

import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.types.Diagnostic;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlValue;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@XmlRootElement(name = "testsuite")
public class Testsuite {
    @XmlAttribute
    private String hostname = hostname();
    @XmlAttribute
    private String name;
    @XmlAttribute
    private String timestamp = Instant.now().toString();
    @XmlAttribute
    private String time;
    @XmlElement(name = "testcase")
    private List<TestCase> testCases;

    public Testsuite() {
    }

    public Testsuite(String name, List<TestCase> testCases) {
        this.name = name;
        this.testCases = testCases;
    }

    @XmlAttribute
    public long getErrors() {
        return testCases.stream().filter(tc -> tc.status == Status.error).count();
    }

    @XmlAttribute
    public long getFailures() {
        return testCases.stream().filter(tc -> tc.status == Status.failed).count();
    }

    public String getHostname() {
        return hostname;
    }

    public String getName() {
        return name;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public String getTime() {
        return time;
    }

    @XmlAttribute
    public int getTests() {
        return testCases.size();
    }

    public List<TestCase> getTestCases() {
        if(testCases == null) testCases = List.of();
        return testCases;
    }

    private static String hostname() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
    }

    public static class TestCase {
        @XmlAttribute
        private String classname;
        @XmlAttribute
        private Status status;
        @XmlAttribute
        private double time;
        @XmlAttribute
        private String message;
        @XmlElement(name = "system-out")
        private Output output;

        public TestCase() {
        }

        public TestCase(String classname) {
            this.classname = classname;
        }

        public static TestCase from(ChunkItem ci) {
            TestCase testCase = new TestCase("Post " + ci.getId()).withStatus(Status.from(ci.getStatus())).withOutput(new Output(new String(ci.getData(), ci.getEncoding())));
            Optional<Diagnostic> diag = ci.getDiagnostics().stream().findFirst();
            diag.ifPresent(d -> testCase.withMessage(d.getMessage()));
            return testCase;
        }

        public TestCase withStatus(Status status) {
            this.status = status;
            return this;
        }

        public TestCase withTime(double time) {
            this.time = time;
            return this;
        }

        public TestCase withMessage(String message) {
            this.message = message;
            return this;
        }

        public TestCase withOutput(Output output) {
            this.output = output;
            return this;
        }

        public String getClassname() {
            return classname;
        }

        public Status getStatus() {
            return status;
        }

        public double getTime() {
            return time;
        }

        public Output getOutput() {
            return output;
        }

        public String getMessage() {
            return message;
        }
    }

    public static enum Status {
        passed(ChunkItem.Status.SUCCESS), error(null), failed(ChunkItem.Status.FAILURE), ignored(ChunkItem.Status.IGNORE);

        private final ChunkItem.Status ciStatus;
        private static final Map<ChunkItem.Status, Status> MAPPING = Arrays.stream(values()).filter(e -> e.ciStatus != null).collect(Collectors.toMap(e -> e.ciStatus, e -> e));


        Status(ChunkItem.Status ciStatus) {
            this.ciStatus = ciStatus;
        }

        public static Status from(ChunkItem.Status status) {
            return MAPPING.get(status);
        }
    }

    public static class Output {
        @XmlValue
        private String output;

        public Output() {
        }

        public Output(String output) {
            this.output = output;
        }

        public String getOutput() {
            return output;
        }
    }
}
