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

import dk.dbc.dataio.cli.FileManager;
import dk.dbc.dataio.cli.FlowManager;
import dk.dbc.dataio.cli.JobManager;
import dk.dbc.dataio.cli.SubversionManager;
import dk.dbc.dataio.cli.UrlManager;
import dk.dbc.dataio.cli.options.CreateOptions;
import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnectorException;
import dk.dbc.dataio.commons.javascript.JavaScriptProjectException;
import dk.dbc.dataio.commons.types.Flow;
import dk.dbc.dataio.commons.types.JobSpecification;
import dk.dbc.dataio.commons.types.RecordSplitterConstants;
import dk.dbc.dataio.commons.types.jndi.JndiConstants;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnectorException;
import dk.dbc.dataio.filestore.service.connector.FileStoreServiceConnectorException;
import dk.dbc.dataio.jobstore.types.JobInfoSnapshot;
import dk.dbc.dataio.jsonb.JSONBException;
import dk.dbc.dataio.urlresolver.service.connector.UrlResolverServiceConnectorException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Acceptance test command line interface for 'create' sub command
 */
public class CreateCommand extends Command {
    private static final Logger LOGGER = LoggerFactory.getLogger(CreateCommand.class);



    private final CreateOptions options;
    private FlowManager flowManager;
    private SubversionManager subversionManager;
    private FileManager fileManager;
    private JobManager jobManager;

    public CreateCommand(CreateOptions options) {
        this.options = options;
    }

    @Override
    public void execute() throws FlowStoreServiceConnectorException,
            UrlResolverServiceConnectorException, JavaScriptProjectException, IOException,
            JSONBException, FileStoreServiceConnectorException, URISyntaxException, JobStoreServiceConnectorException {

        initializeManagers();
        LOGGER.info("Looking up flow '{}'", options.flowName);
        final Flow flow = flowManager.getFlow(options.flowName, subversionManager, options.revision);
        LOGGER.debug("found flow with id {}", flow.getId());

        LOGGER.info("Searching for files in '{}'", options.testsuite);
        final List<Path> testSuite = fileManager.getTestSuite(options.testsuite);
        LOGGER.debug("found {} files: {}", testSuite.size(), testSuite.toString());

        LOGGER.info("Adding datafile to file-store");
        final String fileId = fileManager.addDataFile(testSuite);
        LOGGER.debug("added file with id {}", fileId);

        LOGGER.info("Retrieving JobProperties from testsuite");
        final Properties jobProperties = fileManager.getJobProperties(testSuite);
        LOGGER.debug("found {}", jobProperties.toString());

        LOGGER.info("Creating JobSpecification");
        final JobSpecification jobSpecification = jobManager.createJobSpecification(jobProperties, fileId);
        LOGGER.debug("JobSpecification: {}", jobSpecification.toString());

        LOGGER.info("Adding job");
        final JobInfoSnapshot jobInfoSnapshot = jobManager.addAccTestJob(jobSpecification, flow, RecordSplitterConstants.RecordSplitter.valueOf((String)jobProperties.get("recordSplitter")));
        LOGGER.debug("added job with id {}", jobInfoSnapshot.getJobId());
    }


    /*
     * Private methods
     */

    private void initializeManagers() throws UrlResolverServiceConnectorException, JSONBException {
        LOGGER.info("initializing UrlManager using endpoint: {}", options.guiUrl);
        final UrlManager urlManager = new UrlManager(options.guiUrl);
        final Map<String, String> urls = urlManager.getUrls();

        LOGGER.info("initializing FlowManager using endpoint: {}", urls.get(JndiConstants.FLOW_STORE_SERVICE_ENDPOINT_RESOURCE));
        flowManager = new FlowManager(urls.get(JndiConstants.FLOW_STORE_SERVICE_ENDPOINT_RESOURCE));

        LOGGER.info("initializing SubversionManager using endpoint: {}", urls.get(JndiConstants.SUBVERSION_SCM_ENDPOINT_RESOURCE));
        subversionManager = new SubversionManager(urls.get(JndiConstants.SUBVERSION_SCM_ENDPOINT_RESOURCE));

        LOGGER.info("initializing FileManager using endpoint: {}", urls.get(JndiConstants.URL_RESOURCE_FILESTORE_RS));
        fileManager = new FileManager(urls.get(JndiConstants.URL_RESOURCE_FILESTORE_RS));

        LOGGER.info("initializing JobManager using endpoint: {}", urls.get(JndiConstants.URL_RESOURCE_JOBSTORE_RS));
        jobManager = new JobManager(urls.get(JndiConstants.URL_RESOURCE_JOBSTORE_RS));
    }
}
