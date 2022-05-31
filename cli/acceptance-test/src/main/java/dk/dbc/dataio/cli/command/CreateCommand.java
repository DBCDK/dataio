package dk.dbc.dataio.cli.command;

import dk.dbc.commons.jsonb.JSONBException;
import dk.dbc.dataio.cli.FlowManager;
import dk.dbc.dataio.cli.JobManager;
import dk.dbc.dataio.cli.TestSuite;
import dk.dbc.dataio.cli.options.CreateOptions;
import dk.dbc.dataio.commons.types.Flow;
import dk.dbc.dataio.jobstore.types.JobInfoSnapshot;
import dk.dbc.dataio.urlresolver.service.connector.UrlResolverServiceConnectorException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Acceptance test command line interface for 'create' sub command
 */
public class CreateCommand extends Command<CreateOptions> {
    private static final Logger LOGGER = LoggerFactory.getLogger(CreateCommand.class);
    private FlowManager flowManager;
    private JobManager jobManager;

    public CreateCommand(CreateOptions options) {
        super(options);
    }

    @Override
    public void execute() throws Exception {

        initializeManagers();
        LOGGER.info("Looking up flow '{}'", options.flowName);
        final Flow flow = flowManager.getFlow(options.flowName, options.revision);
        LOGGER.debug("found flow with id {}", flow.getId());

        LOGGER.info("retrieving test suite");
        final List<TestSuite> testSuites = getTestSuites();
        if (testSuites.isEmpty()) {
            throw new IllegalStateException("No test suites found");
        }

        for (TestSuite testSuite : testSuites) {
            LOGGER.info("Running test suite: {}", testSuite.getName());
            LOGGER.info("Adding job");
            final JobInfoSnapshot jobInfoSnapshot = jobManager.addAccTestJob(testSuite, flow);
            LOGGER.info("job {} finished. {}/{} items failed", jobInfoSnapshot.getJobId(), JobManager.failedItems(jobInfoSnapshot.getState()), jobInfoSnapshot.getNumberOfItems());
            LOGGER.debug("created " + testSuite.getName() + "JUnit.xml");
        }
    }

    private List<TestSuite> getTestSuites() throws IOException {
        final List<TestSuite> testSuites = new ArrayList<>();
        if (options.testsuite == null) {
            testSuites.addAll(TestSuite.findAllTestSuites(getCurrentWorkingDirectory()));
        } else {
            final Optional<TestSuite> testSuite = TestSuite.findTestSuite(getCurrentWorkingDirectory(), options.testsuite);
            if (testSuite.isPresent()) {
                testSuites.add(testSuite.get());
            }
        }
        return testSuites;
    }

    private void initializeManagers() throws UrlResolverServiceConnectorException, JSONBException {
        LOGGER.info("Retrieving endpoints using {}", options.guiUrl);
        final Map<String, String> endpoints = getEndpoints();
        for (Map.Entry<String, String> entry : options.overriddenEndpoints.entrySet()) {
            endpoints.put(entry.getKey(), entry.getValue());
        }

        LOGGER.info("initializing FlowManager");
        flowManager = new FlowManager(
                endpoints.get("FLOWSTORE_URL"),
                endpoints.get("SUBVERSION_URL"));

        LOGGER.info("initializing JobManager");
        jobManager = new JobManager(
                endpoints.get("JOBSTORE_URL"),
                endpoints.get("FILESTORE_URL"));
    }

    private static Path getCurrentWorkingDirectory() {
        return Paths.get(".").toAbsolutePath().normalize();
    }
}
