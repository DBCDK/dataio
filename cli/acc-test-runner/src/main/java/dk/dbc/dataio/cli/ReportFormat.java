package dk.dbc.dataio.cli;

import dk.dbc.dataio.commons.types.Chunk;
import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.types.Flow;
import jakarta.xml.bind.JAXB;
import junit.testsuite.Testcase;
import junit.testsuite.Testsuite;
import junit.testsuite.Testsuites;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

public enum ReportFormat {
    TEXT {
        public void printDiff(AccTestSuite suite, Flow flow, Chunk chunk, Long revision, String packageName) {
            chunk.getItems().stream().filter(ci -> ci.getStatus() != ChunkItem.Status.SUCCESS).forEach(ci -> {
                System.out.println(ci.getStatus() + " - ChunkItem: " + ci.getId());
                System.out.println(new String(ci.getData(), ci.getEncoding()));
            });
        }
    },
    XML {
        public void printDiff(AccTestSuite suite, Flow flow, Chunk chunk, Long revision, String packageName) {
            String flowName = flow.getContent().getName().replace('.', '_');
            String classnameSuffix = suite.getName().replace('.', '_');
            List<Object> cases = chunk.getItems().stream().map(chunkItem -> {
                Testcase testcase = Testcase.from(chunkItem);
                if (packageName != null) {
                    // Jenkins uses the classname to group test results,
                    // so we need to be able to set this, especially in a monorepo.
                    testcase.setClassname(packageName + ".acctest." + classnameSuffix);
                }
                return testcase;
            }).collect(Collectors.toList());
            Testsuite testsuite = new Testsuite().withName(suite.getName()).withGroup(flowName).withVersion(revision == null ? null : revision.toString()).withHostname(HOSTNAME)
                    .withTests(Integer.toString(chunk.getItems().size())).withTestsuiteOrPropertiesOrTestcase(cases);
            Testsuites testsuites = new Testsuites().withTestsuite(List.of(testsuite));
            Path reportsDir = suite.getReportPath().getParent();
            try {
                Files.createDirectories(reportsDir);
            } catch (IOException e) {
                throw new RuntimeException("Unable to create report output directory " + reportsDir, e);
            }
            File file = suite.getReportPath().toFile();
            JAXB.marshal(testsuites, file);
            System.out.println("Wrote report to " + file.getAbsolutePath());
        }
    };

    public static final String HOSTNAME = hostname();

    private static String hostname() {
        return System.getenv("HOSTNAME");
    }

    public abstract void printDiff(AccTestSuite suite, Flow flow, Chunk chunk, Long revision, String packageName);
}
