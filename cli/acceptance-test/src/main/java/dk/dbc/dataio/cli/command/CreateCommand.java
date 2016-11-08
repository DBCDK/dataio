/*
 * DataIO - Data IO
 * Copyright (C) 2015 Dansk Bibliotekscenter a/s, Tempovej 7-11, DK-2750 Ballerup,
 * Denmark. CVR: 15149043
 *
 * This file is part of DataIO.
 *
 * DataIO is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * DataIO is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with DataIO.  If not, see <http://www.gnu.org/licenses/>.
 */

package dk.dbc.dataio.cli.command;

import dk.dbc.dataio.cli.FlowManager;
import dk.dbc.dataio.cli.JobManager;
import dk.dbc.dataio.cli.TestSuite;
import dk.dbc.dataio.cli.options.CreateOptions;
import dk.dbc.dataio.commons.types.Flow;
import dk.dbc.dataio.commons.types.jndi.JndiConstants;
import dk.dbc.dataio.commons.utils.httpclient.HttpClient;
import dk.dbc.dataio.jobstore.types.JobInfoSnapshot;
import dk.dbc.dataio.jobstore.types.State;
import dk.dbc.dataio.jsonb.JSONBException;
import dk.dbc.dataio.urlresolver.service.connector.UrlResolverServiceConnector;
import dk.dbc.dataio.urlresolver.service.connector.UrlResolverServiceConnectorException;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.client.Client;
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
public class CreateCommand extends Command {
    private static final Logger LOGGER = LoggerFactory.getLogger(CreateCommand.class);
    private final CreateOptions options;
    private FlowManager flowManager;
    private JobManager jobManager;

    public CreateCommand(CreateOptions options) {
        this.options = options;
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
            LOGGER.info("job {} finished. {}/{} items failed", jobInfoSnapshot.getJobId(), getFailed(jobInfoSnapshot.getState()), jobInfoSnapshot.getNumberOfItems());
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

        LOGGER.info("initializing FlowManager");
        flowManager = new FlowManager(
                endpoints.get(JndiConstants.FLOW_STORE_SERVICE_ENDPOINT_RESOURCE),
                endpoints.get(JndiConstants.SUBVERSION_SCM_ENDPOINT_RESOURCE));

        LOGGER.info("initializing JobManager");
        jobManager = new JobManager(
                endpoints.get(JndiConstants.URL_RESOURCE_JOBSTORE_RS),
                endpoints.get(JndiConstants.URL_RESOURCE_FILESTORE_RS));
    }

    private static Path getCurrentWorkingDirectory() {
        return Paths.get(".").toAbsolutePath().normalize();
    }

    private Map<String, String> getEndpoints() throws UrlResolverServiceConnectorException {
        final Client client = HttpClient.newClient(new ClientConfig()
                .register(new JacksonFeature()));
        final UrlResolverServiceConnector urlResolverServiceConnector = new UrlResolverServiceConnector(client, options.guiUrl);
        return urlResolverServiceConnector.getUrls();
    }

    private static int getFailed(State state) {
        return state.getPhase(State.Phase.PARTITIONING).getFailed() +
                state.getPhase(State.Phase.PROCESSING).getFailed() +
                state.getPhase(State.Phase.DELIVERING).getFailed();
    }
}
